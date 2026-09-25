namespace NotificationService.WebSocket;

public interface IWebSocketNotificationSender
{
    Task SendNotificationToUserAsync<T>(Guid userId, string destination, T payload);
}
