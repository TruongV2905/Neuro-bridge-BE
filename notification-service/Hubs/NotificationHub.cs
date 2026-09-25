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
        string? userId = Context.UserIdentifier
                         ?? Context.GetHttpContext()?.Request.Headers["X-User-Id"].FirstOrDefault()
                         ?? Context.GetHttpContext()?.Request.Query["userId"].FirstOrDefault();

        _logger.LogInformation("[SignalR] User connected. ConnectionId: {ConnectionId}, UserId: {UserId}",
            Context.ConnectionId, userId);

        if (!string.IsNullOrEmpty(userId))
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, $"user_{userId}");
        }

        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        _logger.LogInformation("[SignalR] User disconnected. ConnectionId: {ConnectionId}", Context.ConnectionId);
        await base.OnDisconnectedAsync(exception);
    }
}
