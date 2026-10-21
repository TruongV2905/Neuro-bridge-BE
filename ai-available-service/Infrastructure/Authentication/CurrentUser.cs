using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using AiAvailableService.Api;
using Microsoft.AspNetCore.Http;

namespace AiAvailableService.Infrastructure.Authentication;

public interface ICurrentUser
{
    Guid UserId { get; }
    string Role { get; }
}

public sealed class CurrentUser(IHttpContextAccessor httpContextAccessor) : ICurrentUser
{
    private ClaimsPrincipal Principal => httpContextAccessor.HttpContext?.User
        ?? throw new ApiException(StatusCodes.Status401Unauthorized, "UNAUTHORIZED", "Authentication is required.");

    public Guid UserId
    {
        get
        {
            var subject = Principal.FindFirstValue(JwtRegisteredClaimNames.Sub)
                ?? Principal.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!Guid.TryParse(subject, out var userId))
            {
                throw new ApiException(StatusCodes.Status401Unauthorized, "UNAUTHORIZED", "Authentication is required.");
            }

            return userId;
        }
    }

    public string Role => Principal.FindFirstValue("role") ?? string.Empty;
}
