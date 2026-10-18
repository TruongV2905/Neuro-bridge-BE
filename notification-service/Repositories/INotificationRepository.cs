using NotificationService.Models.Entities;

namespace NotificationService.Repositories;

public interface INotificationRepository
{
    Task<List<Notification>> FindByRecipientUserIdOrderByCreatedAtDescAsync(Guid recipientUserId);
    Task<long> CountByRecipientUserIdAndIsReadAsync(Guid recipientUserId, bool isRead);
    Task MarkAllAsReadAsync(Guid userId);
    Task<Notification?> FindByIdAsync(Guid notificationId);
    Task<bool> ExistsForPurchaseNotificationAsync(string eventType, Guid purchaseId, Guid recipientUserId);
    Task<Notification> SaveAsync(Notification notification);
}
