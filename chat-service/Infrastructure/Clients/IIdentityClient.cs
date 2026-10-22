using ChatService.DTOs.Responses;

namespace ChatService.Infrastructure.Clients;

public interface IIdentityClient
{
    Task<ParticipantDetail?> GetUserByCodeAsync(string userCode, string? bearerToken = null);
    Task<ParticipantDetail?> GetCurrentUserAsync(string? bearerToken = null);
}
