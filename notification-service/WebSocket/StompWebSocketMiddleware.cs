using System.Net.WebSockets;
using System.Text;
using System.Text.Json;

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

        // 1. Handle SockJS /info endpoint (CORS preflight & GET)
        if (IsSockJsInfoRequest(path))
        {
            ApplyCorsHeaders(context);

            if (HttpMethods.IsOptions(context.Request.Method))
            {
                context.Response.StatusCode = StatusCodes.Status204NoContent;
                return;
            }

            context.Response.ContentType = "application/json; charset=UTF-8";
            context.Response.Headers.Append("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            int entropy = new Random().Next(10000000, 99999999);
            await context.Response.WriteAsync($"{{\"websocket\":true,\"origins\":[\"*:*\"],\"cookie_needed\":false,\"entropy\":{entropy}}}");
            return;
        }

        // 2. Check if WebSocket request matches notifications endpoint
        if (IsWebSocketNotificationRequest(path))
        {
            if (context.WebSockets.IsWebSocketRequest)
            {
                bool isSockJs = path.Contains("/websocket", StringComparison.OrdinalIgnoreCase) ||
                                context.Request.Query.ContainsKey("transport");

                string? userId = ExtractUserIdFromRequest(context);

                if (!string.IsNullOrWhiteSpace(userId))
                {
                    _logger.LogInformation("[RealTime] WS handshake accepted. UserId={UserId}, IsSockJs={IsSockJs}, Path={Path}",
                        userId, isSockJs, path);
                }
                else
                {
                    _logger.LogInformation("[RealTime] WS handshake accepted without explicit user. Awaiting STOMP CONNECT. IsSockJs={IsSockJs}, Path={Path}",
                        isSockJs, path);
                    userId = "anonymous";
                }

                using var webSocket = await context.WebSockets.AcceptWebSocketAsync();
                var session = new StompSession(webSocket, userId.ToLowerInvariant(), isSockJs);
                _sender.RegisterSession(session);

                // SockJS protocol requirement: server must send 'o' frame on connection open
                if (isSockJs)
                {
                    await session.SendSockJsOpenAsync();
                    _logger.LogInformation("[RealTime] Sent SockJS open frame 'o' to ConnId={ConnId}", session.ConnectionId);
                }

                using var cts = new CancellationTokenSource();
                var heartbeatTask = StartHeartbeatTimerAsync(session, cts.Token);

                try
                {
                    await ProcessStompConnectionAsync(session, cts.Token);
                }
                finally
                {
                    cts.Cancel();
                    try { await heartbeatTask; } catch { }
                    _sender.UnregisterSession(session.ConnectionId);
                }

                return;
            }
        }

        await _next(context);
    }

    private static bool IsSockJsInfoRequest(string path)
    {
        return path.EndsWith("/info", StringComparison.OrdinalIgnoreCase) &&
               (path.Contains("/ws/notifications", StringComparison.OrdinalIgnoreCase) ||
                path.Contains("/notifications/ws", StringComparison.OrdinalIgnoreCase) ||
                path.Contains("/ws", StringComparison.OrdinalIgnoreCase));
    }

    private static bool IsWebSocketNotificationRequest(string path)
    {
        return path.Contains("/ws/notifications", StringComparison.OrdinalIgnoreCase) ||
               path.Contains("/notifications/ws", StringComparison.OrdinalIgnoreCase) ||
               path.EndsWith("/ws", StringComparison.OrdinalIgnoreCase);
    }

    private static void ApplyCorsHeaders(HttpContext context)
    {
        string? origin = context.Request.Headers["Origin"].FirstOrDefault();
        context.Response.Headers["Access-Control-Allow-Origin"] = string.IsNullOrWhiteSpace(origin) ? "*" : origin;
        context.Response.Headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS, PUT, DELETE";
        context.Response.Headers["Access-Control-Allow-Headers"] = "*";
        context.Response.Headers["Access-Control-Allow-Credentials"] = "true";
    }

    private string? ExtractUserIdFromRequest(HttpContext context)
    {
        // 1. Header X-User-Id (forwarded by Gateway)
        string? userId = context.Request.Headers["X-User-Id"].FirstOrDefault();
        if (!string.IsNullOrWhiteSpace(userId)) return userId;

        // 2. Query param: userId, user_id, userid
        userId = context.Request.Query["userId"].FirstOrDefault()
                 ?? context.Request.Query["user_id"].FirstOrDefault()
                 ?? context.Request.Query["userid"].FirstOrDefault();
        if (!string.IsNullOrWhiteSpace(userId)) return userId;

        // 3. Query param: token or access_token (JWT)
        string? token = context.Request.Query["token"].FirstOrDefault()
                        ?? context.Request.Query["access_token"].FirstOrDefault();
        if (!string.IsNullOrWhiteSpace(token))
        {
            string? jwtUserId = ExtractUserIdFromToken(token);
            if (!string.IsNullOrWhiteSpace(jwtUserId)) return jwtUserId;
        }

        return null;
    }

    private async Task StartHeartbeatTimerAsync(StompSession session, CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested && session.WebSocket.State == WebSocketState.Open)
            {
                await Task.Delay(TimeSpan.FromSeconds(25), ct);
                if (session.WebSocket.State == WebSocketState.Open)
                {
                    await session.SendHeartbeatAsync(ct);
                }
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            _logger.LogDebug("[RealTime] Heartbeat ended for ConnId={ConnId}: {Message}", session.ConnectionId, ex.Message);
        }
    }

    private async Task ProcessStompConnectionAsync(StompSession session, CancellationToken ct)
    {
        byte[] buffer = new byte[8192];
        var ms = new MemoryStream();

        while (!ct.IsCancellationRequested && session.WebSocket.State == WebSocketState.Open)
        {
            WebSocketReceiveResult result;
            try
            {
                result = await session.WebSocket.ReceiveAsync(new ArraySegment<byte>(buffer), ct);
            }
            catch (OperationCanceledException) { break; }
            catch (WebSocketException) { break; }

            if (result.MessageType == WebSocketMessageType.Close)
            {
                await session.WebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closing", ct);
                break;
            }

            ms.Write(buffer, 0, result.Count);

            if (result.EndOfMessage)
            {
                string text = Encoding.UTF8.GetString(ms.ToArray());
                ms.SetLength(0);

                await HandleIncomingTextAsync(session, text);
            }
        }
    }

    private async Task HandleIncomingTextAsync(StompSession session, string text)
    {
        string trimmed = text.Trim();
        if (string.IsNullOrEmpty(trimmed)) return;

        // SockJS wraps frames in JSON string array: ["CONNECT\n..."]
        if (trimmed.StartsWith("[") && trimmed.EndsWith("]"))
        {
            try
            {
                var messages = JsonSerializer.Deserialize<string[]>(trimmed);
                if (messages != null)
                {
                    foreach (var msg in messages)
                    {
                        await HandleStompMessageAsync(session, msg);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "[RealTime] Failed to parse SockJS frame: {Text}", trimmed);
            }
        }
        else
        {
            await HandleStompMessageAsync(session, trimmed);
        }
    }

    private async Task HandleStompMessageAsync(StompSession session, string message)
    {
        // STOMP frames may contain null-terminated commands
        string[] frames = message.Split('\0', StringSplitOptions.RemoveEmptyEntries);

        foreach (var frame in frames)
        {
            string trimmed = frame.Trim();
            if (string.IsNullOrEmpty(trimmed))
            {
                // Heartbeat ping -> pong
                await session.SendHeartbeatAsync();
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
                    // Check headers for user identity
                    string? extractedUserId = null;
                    if (headers.TryGetValue("userId", out var u1)) extractedUserId = u1;
                    else if (headers.TryGetValue("user-id", out var u2)) extractedUserId = u2;
                    else if (headers.TryGetValue("x-user-id", out var u3)) extractedUserId = u3;
                    else if (headers.TryGetValue("login", out var u4)) extractedUserId = u4;

                    if (string.IsNullOrWhiteSpace(extractedUserId))
                    {
                        string? authHeader = headers.GetValueOrDefault("Authorization")
                                             ?? headers.GetValueOrDefault("token")
                                             ?? headers.GetValueOrDefault("passcode");
                        if (!string.IsNullOrWhiteSpace(authHeader))
                        {
                            extractedUserId = ExtractUserIdFromToken(authHeader);
                        }
                    }

                    if (!string.IsNullOrWhiteSpace(extractedUserId))
                    {
                        session.UserId = extractedUserId.ToLowerInvariant();
                        _logger.LogInformation("[RealTime] Session {ConnId} bound to UserId={UserId} via STOMP {Command}",
                            session.ConnectionId, session.UserId, command);
                    }

                    string connectedFrame = $"CONNECTED\nversion:1.2\nheart-beat:0,0\nuser-name:{session.UserId}\n\n\0";
                    await session.SendRawAsync(connectedFrame);
                    _logger.LogInformation("[RealTime] STOMP connected for session {ConnId} (User={UserId})",
                        session.ConnectionId, session.UserId);
                    break;

                case "SUBSCRIBE":
                    if (headers.TryGetValue("destination", out var destination) &&
                        headers.TryGetValue("id", out var subId))
                    {
                        session.Subscriptions[destination] = subId;
                        _logger.LogInformation("[RealTime] Session {ConnId} (User={UserId}) subscribed to {Destination} with id {SubId}",
                            session.ConnectionId, session.UserId, destination, subId);
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
                        await session.SendRawAsync(receiptFrame);
                    }
                    try
                    {
                        await session.WebSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Disconnected by client", CancellationToken.None);
                    }
                    catch { }
                    break;

                default:
                    // Heartbeat or unsupported frame
                    break;
            }
        }
    }

    private static string? ExtractUserIdFromToken(string token)
    {
        if (string.IsNullOrWhiteSpace(token)) return null;
        if (token.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
            token = token.Substring(7).Trim();

        var parts = token.Split('.');
        if (parts.Length < 2) return null;

        try
        {
            string payload = parts[1];
            payload = payload.Replace('-', '+').Replace('_', '/');
            switch (payload.Length % 4)
            {
                case 2: payload += "=="; break;
                case 3: payload += "="; break;
            }
            var bytes = Convert.FromBase64String(payload);
            using var doc = JsonDocument.Parse(bytes);
            var root = doc.RootElement;
            if (root.TryGetProperty("userId", out var u) && u.GetString() is { Length: > 0 } uid) return uid;
            if (root.TryGetProperty("sub", out var s) && s.GetString() is { Length: > 0 } sub) return sub;
            if (root.TryGetProperty("user_id", out var ui) && ui.GetString() is { Length: > 0 } uid2) return uid2;
        }
        catch { }
        return null;
    }
}
