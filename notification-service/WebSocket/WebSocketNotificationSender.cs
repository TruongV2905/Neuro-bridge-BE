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
    public ConcurrentDictionary<string, string> Subscriptions { get; } = new(StringComparer.OrdinalIgnoreCase);

    public StompSession(System.Net.WebSockets.WebSocket webSocket, string userId)
    {
        WebSocket = webSocket;
        UserId = userId;
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
        _logger.LogInformation("[Neural Bridge] Stomp session registered: {ConnectionId} for user {UserId}",
            session.ConnectionId, session.UserId);
    }

    public void UnregisterSession(string connectionId)
    {
        if (_sessions.TryRemove(connectionId, out var session))
        {
            _logger.LogInformation("[Neural Bridge] Stomp session unregistered: {ConnectionId}", connectionId);
        }
    }

    public async Task SendNotificationToUserAsync<T>(Guid userId, string destination, T payload)
    {
        string userIdStr = userId.ToString().ToLowerInvariant();
        string json = JsonSerializer.Serialize(payload, JsonOptions);

        // 1. Send via STOMP WebSocket
        var targetSessions = _sessions.Values
            .Where(s => s.UserId.Equals(userIdStr, StringComparison.OrdinalIgnoreCase) &&
                        s.WebSocket.State == WebSocketState.Open)
            .ToList();

        foreach (var session in targetSessions)
        {
            try
            {
                // Find subscription matching /queue/notifications or /user/queue/notifications or fallback to first
                string subId = "sub-0";
                foreach (var (subDest, id) in session.Subscriptions)
                {
                    if (subDest.EndsWith(destination, StringComparison.OrdinalIgnoreCase) ||
                        destination.EndsWith(subDest, StringComparison.OrdinalIgnoreCase))
                    {
                        subId = id;
                        break;
                    }
                }

                string messageFrame =
                    $"MESSAGE\n" +
                    $"subscription:{subId}\n" +
                    $"message-id:{Guid.NewGuid()}\n" +
                    $"destination:/user/{userIdStr}{destination}\n" +
                    $"content-type:application/json\n" +
                    $"\n" +
                    $"{json}\0";

                byte[] bytes = Encoding.UTF8.GetBytes(messageFrame);
                await session.WebSocket.SendAsync(
                    new ArraySegment<byte>(bytes),
                    WebSocketMessageType.Text,
                    true,
                    CancellationToken.None
                );

                _logger.LogInformation("[Neural Bridge] Sent STOMP frame to user {UserId} (conn: {ConnId})",
                    userId, session.ConnectionId);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "[Neural Bridge] Failed to send STOMP frame to connection {ConnId}",
                    session.ConnectionId);
            }
        }

        // 2. Send via SignalR
        try
        {
            await _hubContext.Clients.User(userId.ToString()).SendAsync("ReceiveNotification", payload);
            await _hubContext.Clients.Group($"user_{userIdStr}").SendAsync("ReceiveNotification", payload);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Neural Bridge] Failed to broadcast notification via SignalR for user {UserId}", userId);
        }
    }
}
