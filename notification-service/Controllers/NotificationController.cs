using Microsoft.AspNetCore.Mvc;
using NotificationService.Models.DTOs.Responses;
using NotificationService.Services;

namespace NotificationService.Controllers;

[ApiController]
[Route("api/v1/notifications")]
[Route("v1/notifications")]
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
        _logger.LogInformation("GET /api/v1/notifications/user/{UserId}", userId);
        var notifications = await _notificationService.GetNotificationsAsync(userId);
        return Ok(notifications);
    }

    [HttpGet("user/{userId:guid}/unread-count")]
    [ProducesResponseType(typeof(long), StatusCodes.Status200OK)]
    public async Task<ActionResult<long>> CountUnread(Guid userId)
    {
        _logger.LogInformation("GET /api/v1/notifications/user/{UserId}/unread-count", userId);
        long count = await _notificationService.CountUnreadAsync(userId);
        return Ok(count);
    }

    [HttpPut("user/{userId:guid}/read-all")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> MarkAllAsRead(Guid userId)
    {
        _logger.LogInformation("PUT /api/v1/notifications/user/{UserId}/read-all", userId);
        await _notificationService.MarkAllAsReadAsync(userId);
        return Ok();
    }

    [HttpPut("{notificationId:guid}/read")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    public async Task<IActionResult> MarkAsRead(Guid notificationId)
    {
        _logger.LogInformation("PUT /api/v1/notifications/{NotificationId}/read", notificationId);
        await _notificationService.MarkAsReadAsync(notificationId);
        return Ok();
    }
}
