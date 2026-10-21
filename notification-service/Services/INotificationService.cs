using NotificationService.Models.DTOs.Events;
using NotificationService.Models.DTOs.Responses;

namespace NotificationService.Services;

public interface INotificationService
{
    Task SendNotificationAsync(NotificationEvent @event);
    Task<PurchaseNotificationDeliveryResult> SendAiProductPurchaseNotificationAsync(AiProductPurchasedEvent @event);
    Task<List<NotificationResponse>> GetNotificationsAsync(Guid userId);
    Task<long> CountUnreadAsync(Guid userId);
    Task MarkAllAsReadAsync(Guid userId);
    Task MarkAsReadAsync(Guid notificationId);
}

public enum PurchaseNotificationDeliveryResult
{
    Created,
    Duplicate
}
