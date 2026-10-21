namespace AiAvailableService.Infrastructure.Identity;

public interface IIdentityDirectory
{
    Task<IdentityUser> GetUserAsync(Guid userId, CancellationToken cancellationToken = default);
}
