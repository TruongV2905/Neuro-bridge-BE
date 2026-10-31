using Microsoft.AspNetCore.Mvc;

namespace ChatService.Controllers;

[ApiController]
[Route("api/chat/health")]
[Route("chat/health")]
[Route("health")]
public class HealthController : ControllerBase
{
    [HttpGet]
    public IActionResult HealthCheck()
    {
        return Ok(new
        {
            status = "UP",
            service = "chat-service",
            version = "1.0.0-dotnet8",
            timestamp = DateTime.UtcNow
        });
    }
}
