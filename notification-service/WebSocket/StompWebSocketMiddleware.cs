using System.Net.WebSockets;
using System.Text;

namespace NotificationService.WebSocket;

public class StompWebSocketMiddleware
{
    private readonly RequestDelegate _next;
    private readonly WebSocketNotificationSender _sender;
    private readonly ILogger<StompWebSocketMiddleware> _logger;

    public StompWebSocketMiddleware(
        RequestDelegate next,
        WebSocketNotificationSender sender,
        ILogger<StompWebSocketMiddleware> logger)
    {
        _next = next;
        _sender = sender;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        string path = context.Request.Path.Value ?? string.Empty;

        // 1. Handle SockJS /info endpoint
        if (path.StartsWith("/ws/notifications/info", StringComparison.OrdinalIgnoreCase))
        {
            context.Response.ContentType = "application/json";
            context.Response.Headers.Append("Access-Control-Allow-Origin", "*");
            await context.Response.WriteAsync("{\"websocket\":true,\"origins\":[\"*:*\"],\"cookie_needed\":false,\"entropy\":21548624}");
            return;
        }

        // 2. Check if WebSocket request matches /ws/notifications
        if (path.StartsWith("/ws/notifications", StringComparison.OrdinalIgnoreCase))
        {
            if (context.WebSockets.IsWebSocketRequest)
            {
                // Extract userId from header or query param
                string? userId = context.Request.Headers["X-User-Id"].FirstOrDefault();
                if (string.IsNullOrWhiteSpace(userId))
                {
                    userId = context.Request.Query["userId"].FirstOrDefault();
                }

                if (!string.IsNullOrWhiteSpace(userId))
                {
                    _logger.LogInformation("[Neural Bridge] WS handshake userId={UserId}", userId);
                }
                else
                {
                    _logger.LogWarning("[Neural Bridge] WS handshake missing X-User-Id header and userId query param");
                    userId = "anonymous";
                }

                using var webSocket = await context.WebSockets.AcceptWebSocketAsync();
                var session = new StompSession(webSocket, userId.ToLowerInvariant());
                _sender.RegisterSession(session);

                try
                {
                    await ProcessStompConnectionAsync(session);
                }
                finally
                {
                    _sender.UnregisterSession(session.ConnectionId);
                }

                return;
            }
        }

        await _next(context);
    }

    private async Task ProcessStompConnectionAsync(StompSession session)
    {
        byte[] buffer = new byte[4096];

        while (session.WebSocket.State == WebSocketState.Open)
        {
            var result = await session.WebSocket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);

            if (result.MessageType == WebSocketMessageType.Close)
            {
                await session.WebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closing", CancellationToken.None);
                break;
            }

            if (result.MessageType == WebSocketMessageType.Text)
            {
                string text = Encoding.UTF8.GetString(buffer, 0, result.Count);
                await HandleStompMessageAsync(session, text);
            }
        }
    }

    private async Task HandleStompMessageAsync(StompSession session, string message)
    {
        // STOMP frame may contain multiple null-separated messages
        string[] frames = message.Split('\0', StringSplitOptions.RemoveEmptyEntries);

        foreach (var frame in frames)
        {
            string trimmed = frame.Trim();
            if (string.IsNullOrEmpty(trimmed))
            {
                // Heartbeat ping -> pong
                byte[] pong = Encoding.UTF8.GetBytes("\n");
                await session.WebSocket.SendAsync(new ArraySegment<byte>(pong), WebSocketMessageType.Text, true, CancellationToken.None);
                continue;
            }

            using var reader = new StringReader(trimmed);
            string? command = await reader.ReadLineAsync();

            if (string.IsNullOrEmpty(command)) continue;
            command = command.Trim().ToUpperInvariant();

            var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            string? line;
            while (!string.IsNullOrEmpty(line = await reader.ReadLineAsync()))
            {
                int colonIndex = line.IndexOf(':');
                if (colonIndex > 0)
                {
                    string key = line.Substring(0, colonIndex).Trim();
                    string value = line.Substring(colonIndex + 1).Trim();
                    headers[key] = value;
                }
            }

            switch (command)
            {
                case "CONNECT":
                case "STOMP":
                    string connectedFrame = "CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0";
                    byte[] connectedBytes = Encoding.UTF8.GetBytes(connectedFrame);
                    await session.WebSocket.SendAsync(new ArraySegment<byte>(connectedBytes), WebSocketMessageType.Text, true, CancellationToken.None);
                    _logger.LogInformation("[Neural Bridge] STOMP connected for session {ConnId}", session.ConnectionId);
                    break;

                case "SUBSCRIBE":
                    if (headers.TryGetValue("destination", out var destination) &&
                        headers.TryGetValue("id", out var subId))
                    {
                        session.Subscriptions[destination] = subId;
                        _logger.LogInformation("[Neural Bridge] Session {ConnId} subscribed to {Destination} with id {SubId}",
                            session.ConnectionId, destination, subId);
                    }
                    break;

                case "UNSUBSCRIBE":
                    if (headers.TryGetValue("id", out var unsubId))
                    {
                        var keyToRemove = session.Subscriptions.FirstOrDefault(x => x.Value == unsubId).Key;
                        if (keyToRemove != null)
                        {
                            session.Subscriptions.TryRemove(keyToRemove, out _);
                        }
                    }
                    break;

                case "DISCONNECT":
                    if (headers.TryGetValue("receipt", out var receiptId))
                    {
                        string receiptFrame = $"RECEIPT\nreceipt-id:{receiptId}\n\n\0";
                        byte[] receiptBytes = Encoding.UTF8.GetBytes(receiptFrame);
                        await session.WebSocket.SendAsync(new ArraySegment<byte>(receiptBytes), WebSocketMessageType.Text, true, CancellationToken.None);
                    }
                    await session.WebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Disconnected by client", CancellationToken.None);
                    break;

                default:
                    // Other messages
                    break;
            }
        }
    }
}
