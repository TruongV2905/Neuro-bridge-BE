using NotificationService.Models.DTOs.Responses;

namespace NotificationService.Services;

public interface INotificationCacheService
{
    Task<long> GetUnreadCountAsync(Guid userId);
    Task SetUnreadCountAsync(Guid userId, long count);
    Task IncrementUnreadCountAsync(Guid userId);
    Task ResetUnreadCountAsync(Guid userId);
    Task<List<NotificationResponse>?> GetNotificationListAsync(Guid userId);
    Task SetNotificationListAsync(Guid userId, List<NotificationResponse> list);
    Task AddToNotificationListAsync(Guid userId, NotificationResponse notification);
    Task InvalidateUserAsync(Guid userId);
}
