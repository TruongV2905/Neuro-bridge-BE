using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using ChatService.Hubs;
using Microsoft.AspNetCore.SignalR;

namespace ChatService.WebSocket;

public class StompChatSession
{
    public string ConnectionId { get; } = Guid.NewGuid().ToString();
    public System.Net.WebSockets.WebSocket WebSocket { get; }
    public ConcurrentDictionary<string, string> Subscriptions { get; } = new(StringComparer.OrdinalIgnoreCase);

    public StompChatSession(System.Net.WebSockets.WebSocket webSocket)
    {
        WebSocket = webSocket;
    }
}

public interface IWebSocketChatSender
{
    void RegisterSession(StompChatSession session);
    void UnregisterSession(string connectionId);
    Task BroadcastMessageToSessionAsync<T>(Guid chatSessionId, T payload);
}

public class WebSocketChatSender : IWebSocketChatSender
{
    private readonly ConcurrentDictionary<string, StompChatSession> _sessions = new();
    private readonly IHubContext<ChatHub> _hubContext;
    private readonly ILogger<WebSocketChatSender> _logger;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter() }
    };

    public WebSocketChatSender(
        IHubContext<ChatHub> hubContext,
        ILogger<WebSocketChatSender> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public void RegisterSession(StompChatSession session)
    {
        _sessions.TryAdd(session.ConnectionId, session);
        _logger.LogInformation("[Chat WS] STOMP session registered: {ConnectionId}", session.ConnectionId);
    }

    public void UnregisterSession(string connectionId)
    {
        if (_sessions.TryRemove(connectionId, out _))
        {
            _logger.LogInformation("[Chat WS] STOMP session unregistered: {ConnectionId}", connectionId);
        }
    }

    public async Task BroadcastMessageToSessionAsync<T>(Guid chatSessionId, T payload)
    {
        string sessionIdStr = chatSessionId.ToString().ToLowerInvariant();
        string topicDestination = $"/topic/chat/{sessionIdStr}";
        string json = JsonSerializer.Serialize(payload, JsonOptions);

        // 1. Broadcast via STOMP WebSocket
        var openSessions = _sessions.Values
            .Where(s => s.WebSocket.State == WebSocketState.Open)
            .ToList();

        foreach (var session in openSessions)
        {
            try
            {
                string? matchedSubId = null;
                string matchedDest = topicDestination;

                foreach (var (subDest, subId) in session.Subscriptions)
                {
                    if (subDest.EndsWith(sessionIdStr, StringComparison.OrdinalIgnoreCase) ||
                        subDest.Equals(topicDestination, StringComparison.OrdinalIgnoreCase))
                    {
                        matchedSubId = subId;
                        matchedDest = subDest;
                        break;
                    }
                }

                if (matchedSubId == null)
                {
                    continue;
                }

                string messageFrame =
                    $"MESSAGE\n" +
                    $"subscription:{matchedSubId}\n" +
                    $"message-id:{Guid.NewGuid()}\n" +
                    $"destination:{matchedDest}\n" +
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
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "[Chat WS] Failed to send STOMP frame to connection {ConnId}", session.ConnectionId);
            }
        }

        // 2. Broadcast via SignalR Hub as well
        try
        {
            await _hubContext.Clients.Group(chatSessionId.ToString()).SendAsync("ReceiveMessage", payload);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[Chat WS] Failed to broadcast via SignalR for session {SessionId}", chatSessionId);
        }
    }
}

