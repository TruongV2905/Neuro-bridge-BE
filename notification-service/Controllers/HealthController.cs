using Microsoft.AspNetCore.Mvc;

namespace NotificationService.Controllers;

[ApiController]
public class HealthController : ControllerBase
{
    [HttpGet("api/notifications/health")]
    [HttpGet("health")]
    [HttpGet("actuator/health")]
    public IActionResult Health()
    {
        return Ok(new
        {
            status = "UP",
            service = "notification-service",
            framework = ".NET 8",
            timestamp = DateTime.UtcNow
        });
    }
}
