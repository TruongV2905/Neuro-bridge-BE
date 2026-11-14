using Microsoft.AspNetCore.Mvc;
using NotificationService.Models.DTOs.Events;
using NotificationService.Models.DTOs.Responses;
using NotificationService.Services;

namespace NotificationService.Controllers;

public class TestNotificationRequest
{
    public Guid? RecipientUserId { get; set; }
    public string? Title { get; set; }
    public string? Message { get; set; }
    public string? Type { get; set; }
    public string? Deeplink { get; set; }
    public Guid? WorkspaceId { get; set; }
    public Guid? ResourceId { get; set; }
}

[ApiController]
[Route("api/v1/notifications")]
[Route("v1/notifications")]
[Route("api/notifications")]
public class NotificationController : ControllerBase
{
    private readonly INotificationService _notificationService;
    private readonly ILogger<NotificationController> _logger;

    public NotificationController(
        INotificationService notificationService,
        ILogger<NotificationController> logger)
    {
        _notificationService = notificationService;
        _logger = logger;
    }

    [HttpGet("user/{userId:guid}")]
    [ProducesResponseType(typeof(List<NotificationResponse>), StatusCodes.Status200OK)]
    public async Task<ActionResult<List<NotificationResponse>>> GetNotifications(Guid userId)
    {
        _logger.LogInformation("GET notifications for user {UserId}", userId);
        var notifications = await _notificationService.GetNotificationsAsync(userId);
        return Ok(notifications);
    }

    [HttpGet("user/{userId:guid}/unread-count")]
    [ProducesResponseType(typeof(long), StatusCodes.Status200OK)]
    public async Task<ActionResult<long>> CountUnread(Guid userId)
    {
        _logger.LogInformation("GET unread-count for user {UserId}", userId);
        long count = await _notificationService.CountUnreadAsync(userId);
        return Ok(count);
    }

    [HttpPut("user/{userId:guid}/read-all")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> MarkAllAsRead(Guid userId)
    {
        _logger.LogInformation("PUT read-all for user {UserId}", userId);
        await _notificationService.MarkAllAsReadAsync(userId);
        return Ok(new { message = "Marked all notifications as read." });
    }

    [HttpPut("{notificationId:guid}/read")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> MarkAsRead(Guid notificationId)
    {
        _logger.LogInformation("PUT read for notification {NotificationId}", notificationId);
        await _notificationService.MarkAsReadAsync(notificationId);
        return Ok(new { message = "Marked notification as read." });
    }

    [HttpPost("test")]
    [HttpPost("user/{userId:guid}/test")]
    public async Task<IActionResult> SendTestNotification(Guid? userId, [FromBody] TestNotificationRequest? request)
    {
        Guid targetUserId = userId ?? request?.RecipientUserId ?? Guid.Empty;
        if (targetUserId == Guid.Empty)
        {
            return BadRequest(new { error = "recipientUserId is required." });
        }

        var testEvent = new NotificationEvent
        {
            EventType = request?.Type ?? "TEST_NOTIFICATION",
            RecipientUserId = targetUserId,
            CustomTitle = request?.Title ?? "Thông báo thử nghiệm Real-time",
            CustomMessage = request?.Message ?? "Hệ thống thông báo WebSocket/STOMP/SignalR đang hoạt động ổn định!",
            CustomDeeplink = request?.Deeplink ?? "/notifications",
            WorkspaceId = request?.WorkspaceId,
            ResourceId = request?.ResourceId
        };

        await _notificationService.SendNotificationAsync(testEvent);
        _logger.LogInformation("Test real-time notification sent to user {UserId}", targetUserId);

        return Ok(new
        {
            success = true,
            message = "Real-time notification dispatched via WebSocket and SignalR.",
            recipientUserId = targetUserId,
            title = testEvent.CustomTitle,
            content = testEvent.CustomMessage
        });
    }
}
