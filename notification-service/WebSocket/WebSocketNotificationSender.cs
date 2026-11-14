using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.SignalR;
using NotificationService.Hubs;

namespace NotificationService.WebSocket;

public class StompSession
{
    public string ConnectionId { get; } = Guid.NewGuid().ToString();
    public System.Net.WebSockets.WebSocket WebSocket { get; }
    public string UserId { get; set; }
    public bool IsSockJs { get; set; }
    public ConcurrentDictionary<string, string> Subscriptions { get; } = new(StringComparer.OrdinalIgnoreCase);

    public StompSession(System.Net.WebSockets.WebSocket webSocket, string userId, bool isSockJs = false)
    {
        WebSocket = webSocket;
        UserId = userId;
        IsSockJs = isSockJs;
    }

    public async Task SendRawAsync(string rawMessage, CancellationToken ct = default)
    {
        if (WebSocket.State != WebSocketState.Open) return;

        string finalPayload;
        if (IsSockJs)
        {
            // SockJS protocol requires frames wrapped in a["..."] JSON array
            string escaped = JsonSerializer.Serialize(rawMessage);
            finalPayload = $"a[{escaped}]";
        }
        else
        {
            finalPayload = rawMessage;
        }

        byte[] bytes = Encoding.UTF8.GetBytes(finalPayload);
        await WebSocket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
    }

    public async Task SendSockJsOpenAsync(CancellationToken ct = default)
    {
        if (WebSocket.State != WebSocketState.Open) return;
        byte[] bytes = Encoding.UTF8.GetBytes("o");
        await WebSocket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
    }

    public async Task SendHeartbeatAsync(CancellationToken ct = default)
    {
        if (WebSocket.State != WebSocketState.Open) return;
        byte[] bytes = Encoding.UTF8.GetBytes(IsSockJs ? "h" : "\n");
        await WebSocket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
    }
}

public class WebSocketNotificationSender : IWebSocketNotificationSender
{
    private readonly ConcurrentDictionary<string, StompSession> _sessions = new();
    private readonly IHubContext<NotificationHub> _hubContext;
    private readonly ILogger<WebSocketNotificationSender> _logger;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public WebSocketNotificationSender(
        IHubContext<NotificationHub> hubContext,
        ILogger<WebSocketNotificationSender> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public void RegisterSession(StompSession session)
    {
        _sessions.TryAdd(session.ConnectionId, session);
        _logger.LogInformation("[RealTime] Session registered: ConnId={ConnectionId}, UserId={UserId}, IsSockJs={IsSockJs}",
            session.ConnectionId, session.UserId, session.IsSockJs);
    }

    public void UnregisterSession(string connectionId)
    {
        if (_sessions.TryRemove(connectionId, out var session))
        {
            _logger.LogInformation("[RealTime] Session unregistered: ConnId={ConnectionId}, UserId={UserId}",
                connectionId, session.UserId);
        }
    }

    public async Task SendNotificationToUserAsync<T>(Guid userId, string destination, T payload)
    {
        string userIdStr = userId.ToString().ToLowerInvariant();
        string json = JsonSerializer.Serialize(payload, JsonOptions);

        // 1. Send via STOMP WebSocket (SockJS or native)
        var targetSessions = _sessions.Values
            .Where(s => s.WebSocket.State == WebSocketState.Open &&
                        (string.Equals(s.UserId, userIdStr, StringComparison.OrdinalIgnoreCase) ||
                         string.Equals(s.UserId, userId.ToString(), StringComparison.OrdinalIgnoreCase) ||
                         string.Equals(s.UserId, "all", StringComparison.OrdinalIgnoreCase)))
            .ToList();

        _logger.LogInformation("[RealTime] Dispatching notification to {Count} active STOMP sessions for user {UserId}",
            targetSessions.Count, userId);

        foreach (var session in targetSessions)
        {
            try
            {
                // Find matching subscriptions or send to all relevant notification destinations
                var matchingSubs = session.Subscriptions
                    .Where(kvp => kvp.Key.Contains("notification", StringComparison.OrdinalIgnoreCase) ||
                                 kvp.Key.EndsWith(destination, StringComparison.OrdinalIgnoreCase) ||
                                 destination.EndsWith(kvp.Key, StringComparison.OrdinalIgnoreCase) ||
                                 kvp.Key.StartsWith("/user", StringComparison.OrdinalIgnoreCase) ||
                                 kvp.Key.StartsWith("/queue", StringComparison.OrdinalIgnoreCase) ||
                                 kvp.Key.StartsWith("/topic", StringComparison.OrdinalIgnoreCase))
                    .ToList();

                if (matchingSubs.Count > 0)
                {
                    foreach (var (subDest, subId) in matchingSubs)
                    {
                        string messageFrame =
                            $"MESSAGE\n" +
                            $"subscription:{subId}\n" +
                            $"message-id:{Guid.NewGuid()}\n" +
                            $"destination:{subDest}\n" +
                            $"content-type:application/json\n" +
                            $"\n" +
                            $"{json}\0";

                        await session.SendRawAsync(messageFrame);
                        _logger.LogInformation("[RealTime] Sent STOMP message to user {UserId} (ConnId={ConnId}, Dest={Dest}, SubId={SubId})",
                            userId, session.ConnectionId, subDest, subId);
                    }
                }
                else
                {
                    // Fallback when client has no explicit subscription or subscribed before registering
                    string fallbackDest = $"/user/queue/notifications";
                    string messageFrame =
                        $"MESSAGE\n" +
                        $"subscription:sub-0\n" +
                        $"message-id:{Guid.NewGuid()}\n" +
                        $"destination:{fallbackDest}\n" +
                        $"content-type:application/json\n" +
                        $"\n" +
                        $"{json}\0";

                    await session.SendRawAsync(messageFrame);
                    _logger.LogInformation("[RealTime] Sent fallback STOMP message to user {UserId} (ConnId={ConnId}, Dest={Dest})",
                        userId, session.ConnectionId, fallbackDest);
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "[RealTime] Failed to send STOMP frame to ConnId={ConnId}", session.ConnectionId);
            }
        }

        // 2. Send via SignalR (hub: /hubs/notifications)
        try
        {
            await _hubContext.Clients.Group($"user_{userIdStr}").SendAsync("ReceiveNotification", payload);
            await _hubContext.Clients.User(userId.ToString()).SendAsync("ReceiveNotification", payload);
            await _hubContext.Clients.User(userIdStr).SendAsync("ReceiveNotification", payload);
            _logger.LogInformation("[RealTime] Broadcasted notification via SignalR for user {UserId}", userId);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[RealTime] Failed to broadcast notification via SignalR for user {UserId}", userId);
        }
    }
}
