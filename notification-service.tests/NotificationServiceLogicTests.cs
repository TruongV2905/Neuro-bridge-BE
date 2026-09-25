using Microsoft.Extensions.Logging;
using Moq;
using NotificationService.Models.DTOs.Events;
using NotificationService.Models.DTOs.Responses;
using NotificationService.Models.Entities;
using NotificationService.Repositories;
using NotificationService.Services;
using NotificationService.WebSocket;
using Xunit;

namespace NotificationService.Tests;

public class NotificationServiceLogicTests
{
    private readonly Mock<INotificationRepository> _mockRepo;
    private readonly Mock<INotificationCacheService> _mockCache;
    private readonly Mock<IWebSocketNotificationSender> _mockWsSender;
    private readonly Mock<ILogger<Services.NotificationService>> _mockLogger;
    private readonly Services.NotificationService _service;

    public NotificationServiceLogicTests()
    {
        _mockRepo = new Mock<INotificationRepository>();
        _mockCache = new Mock<INotificationCacheService>();
        _mockWsSender = new Mock<IWebSocketNotificationSender>();
        _mockLogger = new Mock<ILogger<Services.NotificationService>>();

        _service = new Services.NotificationService(
            _mockRepo.Object,
            _mockCache.Object,
            _mockWsSender.Object,
            _mockLogger.Object
        );
    }

    [Fact]
    public async Task SendNotificationAsync_SavesEntity_Caches_AndBroadcastsWebSocket()
    {
        var recipientId = Guid.NewGuid();
        var @event = new NotificationEvent
        {
            EventType = "PACKAGE_SUBMITTED",
            RecipientUserId = recipientId,
            SenderName = "Alice",
            ResourceVersion = "1",
            WorkspaceId = Guid.NewGuid(),
            ResourceId = Guid.NewGuid()
        };

        Notification? savedNotification = null;
        _mockRepo.Setup(r => r.SaveAsync(It.IsAny<Notification>()))
            .Callback<Notification>(n => savedNotification = n)
            .ReturnsAsync((Notification n) => n);

        await _service.SendNotificationAsync(@event);

        _mockRepo.Verify(r => r.SaveAsync(It.IsAny<Notification>()), Times.Once);
        Assert.NotNull(savedNotification);
        Assert.Equal(recipientId, savedNotification.RecipientUserId);
        Assert.Equal("Gói bàn giao mới", savedNotification.Title);
        Assert.Equal("Alice đã nộp gói bàn giao v1", savedNotification.Message);

        _mockCache.Verify(c => c.AddToNotificationListAsync(recipientId, It.IsAny<NotificationResponse>()), Times.Once);
        _mockCache.Verify(c => c.IncrementUnreadCountAsync(recipientId), Times.Once);
        _mockWsSender.Verify(w => w.SendNotificationToUserAsync(recipientId, "/queue/notifications", It.IsAny<NotificationResponse>()), Times.Once);
    }

    [Fact]
    public async Task SendNotificationAsync_WithCustomTitleAndMessage_UsesCustomData()
    {
        var recipientId = Guid.NewGuid();
        var @event = new NotificationEvent
        {
            EventType = "CUSTOM_EVENT",
            RecipientUserId = recipientId,
            CustomTitle = "Custom Notice",
            CustomMessage = "This is a custom message",
            CustomDeeplink = "/custom/link"
        };

        Notification? savedNotification = null;
        _mockRepo.Setup(r => r.SaveAsync(It.IsAny<Notification>()))
            .Callback<Notification>(n => savedNotification = n)
            .ReturnsAsync((Notification n) => n);

        await _service.SendNotificationAsync(@event);

        Assert.NotNull(savedNotification);
        Assert.Equal("Custom Notice", savedNotification.Title);
        Assert.Equal("This is a custom message", savedNotification.Message);
        Assert.Equal("/custom/link", savedNotification.Deeplink);
    }

    [Fact]
    public async Task GetNotificationsAsync_WhenCacheHit_ReturnsFromCache()
    {
        var userId = Guid.NewGuid();
        var cachedList = new List<NotificationResponse>
        {
            new() { Id = Guid.NewGuid(), Title = "Cached 1" }
        };

        _mockCache.Setup(c => c.GetNotificationListAsync(userId))
            .ReturnsAsync(cachedList);

        var result = await _service.GetNotificationsAsync(userId);

        Assert.Single(result);
        Assert.Equal("Cached 1", result[0].Title);
        _mockRepo.Verify(r => r.FindByRecipientUserIdOrderByCreatedAtDescAsync(It.IsAny<Guid>()), Times.Never);
    }

    [Fact]
    public async Task GetNotificationsAsync_WhenCacheMiss_QueriesDbAndCaches()
    {
        var userId = Guid.NewGuid();
        _mockCache.Setup(c => c.GetNotificationListAsync(userId))
            .ReturnsAsync((List<NotificationResponse>?)null);

        var dbList = new List<Notification>
        {
            new() { Id = Guid.NewGuid(), RecipientUserId = userId, Title = "DB Notice", Type = "TEST", Message = "Msg" }
        };

        _mockRepo.Setup(r => r.FindByRecipientUserIdOrderByCreatedAtDescAsync(userId))
            .ReturnsAsync(dbList);

        var result = await _service.GetNotificationsAsync(userId);

        Assert.Single(result);
        Assert.Equal("DB Notice", result[0].Title);
        _mockCache.Verify(c => c.SetNotificationListAsync(userId, It.IsAny<List<NotificationResponse>>()), Times.Once);
    }

    [Fact]
    public async Task CountUnreadAsync_WhenCacheHit_ReturnsFromCache()
    {
        var userId = Guid.NewGuid();
        _mockCache.Setup(c => c.GetUnreadCountAsync(userId))
            .ReturnsAsync(7L);

        var count = await _service.CountUnreadAsync(userId);

        Assert.Equal(7L, count);
        _mockRepo.Verify(r => r.CountByRecipientUserIdAndIsReadAsync(It.IsAny<Guid>(), It.IsAny<bool>()), Times.Never);
    }

    [Fact]
    public async Task CountUnreadAsync_WhenCacheMiss_QueriesDbAndCaches()
    {
        var userId = Guid.NewGuid();
        _mockCache.Setup(c => c.GetUnreadCountAsync(userId))
            .ReturnsAsync(-1L);

        _mockRepo.Setup(r => r.CountByRecipientUserIdAndIsReadAsync(userId, false))
            .ReturnsAsync(3L);

        var count = await _service.CountUnreadAsync(userId);

        Assert.Equal(3L, count);
        _mockCache.Verify(c => c.SetUnreadCountAsync(userId, 3L), Times.Once);
    }

    [Fact]
    public async Task MarkAllAsReadAsync_UpdatesDb_ResetsCache_AndInvalidates()
    {
        var userId = Guid.NewGuid();

        await _service.MarkAllAsReadAsync(userId);

        _mockRepo.Verify(r => r.MarkAllAsReadAsync(userId), Times.Once);
        _mockCache.Verify(c => c.ResetUnreadCountAsync(userId), Times.Once);
        _mockCache.Verify(c => c.InvalidateUserAsync(userId), Times.Once);
    }

    [Fact]
    public async Task MarkAsReadAsync_UpdatesDb_AndInvalidatesCache()
    {
        var notifId = Guid.NewGuid();
        var userId = Guid.NewGuid();
        var notification = new Notification { Id = notifId, RecipientUserId = userId, IsRead = false };

        _mockRepo.Setup(r => r.FindByIdAsync(notifId))
            .ReturnsAsync(notification);

        await _service.MarkAsReadAsync(notifId);

        Assert.True(notification.IsRead);
        _mockRepo.Verify(r => r.SaveAsync(notification), Times.Once);
        _mockCache.Verify(c => c.InvalidateUserAsync(userId), Times.Once);
    }
}
