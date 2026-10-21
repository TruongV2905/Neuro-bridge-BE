namespace AiAvailableService.Infrastructure.Identity;

public sealed record IdentityUser(
    Guid Id,
    string Code,
    string FullName,
    string? AvatarUrl,
    string Role,
    string Status,
    string? Email);
