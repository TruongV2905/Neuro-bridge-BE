using System.Net.WebSockets;
using System.Text;

namespace ChatService.WebSocket;

public class StompChatWebSocketMiddleware
{
    private readonly RequestDelegate _next;
    private readonly IWebSocketChatSender _sender;
    private readonly ILogger<StompChatWebSocketMiddleware> _logger;

    public StompChatWebSocketMiddleware(
        RequestDelegate next,
        IWebSocketChatSender sender,
        ILogger<StompChatWebSocketMiddleware> logger)
    {
        _next = next;
        _sender = sender;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        string path = context.Request.Path.Value ?? string.Empty;

        // 1. SockJS /info compatibility endpoint
        if (path.StartsWith("/ws/chat/info", StringComparison.OrdinalIgnoreCase))
        {
            context.Response.ContentType = "application/json";
            context.Response.Headers.Append("Access-Control-Allow-Origin", "*");
            await context.Response.WriteAsync("{\"websocket\":true,\"origins\":[\"*:*\"],\"cookie_needed\":false,\"entropy\":21548625}");
            return;
        }

        // 2. Raw WebSocket / STOMP endpoint at /ws/chat
        if (path.StartsWith("/ws/chat", StringComparison.OrdinalIgnoreCase) && context.WebSockets.IsWebSocketRequest)
        {
            using var webSocket = await context.WebSockets.AcceptWebSocketAsync();
            var session = new StompChatSession(webSocket);
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

        await _next(context);
    }

    private async Task ProcessStompConnectionAsync(StompChatSession session)
    {
        byte[] buffer = new byte[8192];

        while (session.WebSocket.State == WebSocketState.Open)
        {
            WebSocketReceiveResult result;
            try
            {
                result = await session.WebSocket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
            }
            catch
            {
                break;
            }

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

    private async Task HandleStompMessageAsync(StompChatSession session, string message)
    {
        string[] frames = message.Split('\0', StringSplitOptions.RemoveEmptyEntries);

        foreach (var frame in frames)
        {
            string trimmed = frame.Trim();
            if (string.IsNullOrEmpty(trimmed))
            {
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
                    break;

                case "SUBSCRIBE":
                    if (headers.TryGetValue("destination", out var destination) &&
                        headers.TryGetValue("id", out var subId))
                    {
                        session.Subscriptions[destination] = subId;
                        _logger.LogInformation("[Chat WS] Session {ConnId} subscribed to {Destination} (id={SubId})",
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
            }
        }
    }
}

