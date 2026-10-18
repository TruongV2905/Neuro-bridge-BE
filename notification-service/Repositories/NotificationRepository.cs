using Microsoft.EntityFrameworkCore;
using NotificationService.Data;
using NotificationService.Models.Entities;

namespace NotificationService.Repositories;

public class NotificationRepository : INotificationRepository
{
    private readonly NotificationDbContext _context;

    public NotificationRepository(NotificationDbContext context)
    {
        _context = context;
    }

    public async Task<List<Notification>> FindByRecipientUserIdOrderByCreatedAtDescAsync(Guid recipientUserId)
    {
        return await _context.Notifications
            .AsNoTracking()
            .Where(n => n.RecipientUserId == recipientUserId)
            .OrderByDescending(n => n.CreatedAt)
            .ToListAsync();
    }

    public async Task<long> CountByRecipientUserIdAndIsReadAsync(Guid recipientUserId, bool isRead)
    {
        return await _context.Notifications
            .LongCountAsync(n => n.RecipientUserId == recipientUserId && n.IsRead == isRead);
    }

    public async Task MarkAllAsReadAsync(Guid userId)
    {
        await _context.Notifications
            .Where(n => n.RecipientUserId == userId && !n.IsRead)
            .ExecuteUpdateAsync(s => s.SetProperty(n => n.IsRead, true));
    }

    public async Task<Notification?> FindByIdAsync(Guid notificationId)
    {
        return await _context.Notifications.FindAsync(notificationId);
    }

    public Task<bool> ExistsForPurchaseNotificationAsync(string eventType, Guid purchaseId, Guid recipientUserId)
    {
        return _context.Notifications.AsNoTracking().AnyAsync(notification =>
            notification.Type == eventType
            && notification.PurchaseId == purchaseId
            && notification.RecipientUserId == recipientUserId);
    }

    public async Task<Notification> SaveAsync(Notification notification)
    {
        if (notification.Id == Guid.Empty)
        {
            notification.Id = Guid.NewGuid();
        }

        var entry = _context.Entry(notification);
        if (entry.State == EntityState.Detached)
        {
            var existing = await _context.Notifications.FindAsync(notification.Id);
            if (existing != null)
            {
                _context.Entry(existing).CurrentValues.SetValues(notification);
            }
            else
            {
                await _context.Notifications.AddAsync(notification);
            }
        }

        await _context.SaveChangesAsync();
        return notification;
    }
}
