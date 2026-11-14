using System.Text.Json;
using Microsoft.AspNetCore.SignalR;

namespace NotificationService.Hubs;

public class NotificationHub : Hub
{
    private readonly ILogger<NotificationHub> _logger;

    public NotificationHub(ILogger<NotificationHub> logger)
    {
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        var httpContext = Context.GetHttpContext();
        string? userId = Context.UserIdentifier
                         ?? httpContext?.Request.Headers["X-User-Id"].FirstOrDefault()
                         ?? httpContext?.Request.Query["userId"].FirstOrDefault()
                         ?? httpContext?.Request.Query["user_id"].FirstOrDefault()
                         ?? httpContext?.Request.Query["userid"].FirstOrDefault();

        if (string.IsNullOrWhiteSpace(userId) && httpContext != null)
        {
            string? token = httpContext.Request.Query["access_token"].FirstOrDefault()
                            ?? httpContext.Request.Query["token"].FirstOrDefault();
            if (!string.IsNullOrWhiteSpace(token))
            {
                userId = ExtractUserIdFromToken(token);
            }
        }

        if (!string.IsNullOrEmpty(userId))
        {
            userId = userId.ToLowerInvariant();
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");
            _logger.LogInformation("[SignalR] User {UserId} joined group user_{UserId} (ConnId: {ConnectionId})",
                userId, userId, Context.ConnectionId);
        }
        else
        {
            _logger.LogInformation("[SignalR] Anonymous connection established (ConnId: {ConnectionId})", Context.ConnectionId);
        }

        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        _logger.LogInformation("[SignalR] User disconnected. ConnectionId: {ConnectionId}", Context.ConnectionId);
        await base.OnDisconnectedAsync(exception);
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
