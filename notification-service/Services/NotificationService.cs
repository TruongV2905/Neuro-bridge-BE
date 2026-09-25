using NotificationService.Models.DTOs.Events;
using NotificationService.Models.DTOs.Responses;
using NotificationService.Models.Entities;
using NotificationService.Models.Enums;
using NotificationService.Repositories;
using NotificationService.WebSocket;

namespace NotificationService.Services;

public class NotificationService : INotificationService
{
    private readonly INotificationRepository _notificationRepository;
    private readonly INotificationCacheService _cacheService;
    private readonly IWebSocketNotificationSender _webSocketSender;
    private readonly ILogger<NotificationService> _logger;

    public NotificationService(
        INotificationRepository notificationRepository,
        INotificationCacheService cacheService,
        IWebSocketNotificationSender webSocketSender,
        ILogger<NotificationService> logger)
    {
        _notificationRepository = notificationRepository;
        _cacheService = cacheService;
        _webSocketSender = webSocketSender;
        _logger = logger;
    }

    public async Task SendNotificationAsync(NotificationEvent @event)
    {
        var payload = BuildPayload(@event);

        var notification = new Notification
        {
            RecipientUserId = @event.RecipientUserId,
            Type = payload.Type ?? string.Empty,
            Title = payload.Title ?? string.Empty,
            Message = payload.Message ?? string.Empty,
            ResourceId = @event.ResourceId,
            WorkspaceId = @event.WorkspaceId,
            Deeplink = payload.Deeplink,
            IsRead = false,
            CreatedAt = DateTime.UtcNow
        };

        var saved = await _notificationRepository.SaveAsync(notification);
        var response = ToResponse(saved);

        await _cacheService.AddToNotificationListAsync(@event.RecipientUserId, response);
        await _cacheService.IncrementUnreadCountAsync(@event.RecipientUserId);

        await _webSocketSender.SendNotificationToUserAsync(
            @event.RecipientUserId,
            "/queue/notifications",
            response
        );

        _logger.LogInformation("[Neural Bridge] Saved + Cached + Sent notification {EventType} to user {RecipientUserId}",
            @event.EventType, @event.RecipientUserId);
    }

    public async Task<List<NotificationResponse>> GetNotificationsAsync(Guid userId)
    {
        // 1. Thử lấy từ Redis
        var cached = await _cacheService.GetNotificationListAsync(userId);
        if (cached != null)
        {
            _logger.LogInformation("[Redis] Cache hit - notifications for user {UserId}", userId);
            return cached;
        }

        // 2. Cache miss -> query DB
        _logger.LogInformation("[Redis] Cache miss - query DB for user {UserId}", userId);
        var fromDb = (await _notificationRepository.FindByRecipientUserIdOrderByCreatedAtDescAsync(userId))
            .Select(ToResponse)
            .ToList();

        // 3. Lưu vào Redis
        await _cacheService.SetNotificationListAsync(userId, fromDb);

        return fromDb;
    }

    public async Task<long> CountUnreadAsync(Guid userId)
    {
        // 1. Thử lấy từ Redis
        long cached = await _cacheService.GetUnreadCountAsync(userId);
        if (cached != -1L)
        {
            _logger.LogInformation("[Redis] Cache hit - unread count for user {UserId}", userId);
            return cached;
        }

        // 2. Cache miss -> query DB
        _logger.LogInformation("[Redis] Cache miss - query DB unread for user {UserId}", userId);
        long count = await _notificationRepository.CountByRecipientUserIdAndIsReadAsync(userId, false);

        // 3. Lưu vào Redis
        await _cacheService.SetUnreadCountAsync(userId, count);

        return count;
    }

    public async Task MarkAllAsReadAsync(Guid userId)
    {
        // 1. Cập nhật DB
        await _notificationRepository.MarkAllAsReadAsync(userId);

        // 2. Reset Redis
        await _cacheService.ResetUnreadCountAsync(userId);
        await _cacheService.InvalidateUserAsync(userId);
    }

    public async Task MarkAsReadAsync(Guid notificationId)
    {
        var notification = await _notificationRepository.FindByIdAsync(notificationId);
        if (notification != null)
        {
            var userId = notification.RecipientUserId;

            // 1. Cập nhật DB
            notification.IsRead = true;
            await _notificationRepository.SaveAsync(notification);

            // 2. Cập nhật Redis
            await _cacheService.InvalidateUserAsync(userId);
        }
    }

    private static NotificationResponse ToResponse(Notification n)
    {
        return new NotificationResponse
        {
            Id = n.Id,
            Type = n.Type,
            Title = n.Title,
            Message = n.Message,
            ResourceId = n.ResourceId,
            WorkspaceId = n.WorkspaceId,
            Deeplink = n.Deeplink,
            IsRead = n.IsRead,
            CreatedAt = n.CreatedAt
        };
    }

    private static NotificationPayload BuildPayload(NotificationEvent @event)
    {
        if (@event.CustomTitle != null && @event.CustomMessage != null)
        {
            var template = NotificationTemplate.FromEventType(@event.EventType);
            return new NotificationPayload
            {
                Type = @event.EventType,
                Title = @event.CustomTitle,
                Message = @event.CustomMessage,
                ResourceId = @event.ResourceId,
                WorkspaceId = @event.WorkspaceId,
                Deeplink = @event.CustomDeeplink ?? template.BuildDeeplink(@event),
                Timestamp = DateTime.UtcNow
            };
        }

        var defaultTemplate = NotificationTemplate.FromEventType(@event.EventType);

        return new NotificationPayload
        {
            Type = @event.EventType,
            Title = defaultTemplate.Title,
            Message = defaultTemplate.BuildMessage(@event),
            ResourceId = @event.ResourceId,
            WorkspaceId = @event.WorkspaceId,
            Deeplink = defaultTemplate.BuildDeeplink(@event),
            Timestamp = DateTime.UtcNow
        };
    }
}
