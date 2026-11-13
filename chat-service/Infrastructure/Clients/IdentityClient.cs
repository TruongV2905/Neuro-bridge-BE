using ChatService.DTOs.Responses;
using Com.Identity.Grpc;
using Grpc.Core;

namespace ChatService.Infrastructure.Clients;

public class IdentityClient : IIdentityClient
{
    private readonly UserService.UserServiceClient _userServiceClient;
    private readonly ILogger<IdentityClient> _logger;

    public IdentityClient(UserService.UserServiceClient userServiceClient, ILogger<IdentityClient> logger)
    {
        _userServiceClient = userServiceClient;
        _logger = logger;
    }

    public async Task<ParticipantDetail?> GetUserByCodeAsync(string userCode, string? bearerToken = null)
    {
        if (string.IsNullOrWhiteSpace(userCode))
        {
            return null;
        }

        var headers = CreateHeaders(bearerToken);

        // If userCode is a UUID (e.g. from Workspace gRPC or JWT sub), try GetUser by userId first
        if (Guid.TryParse(userCode, out _))
        {
            try
            {
                var byIdResponse = await _userServiceClient.GetUserAsync(
                    new UserRequest { UserId = userCode },
                    headers: headers
                );

                if (byIdResponse != null && !string.IsNullOrWhiteSpace(byIdResponse.Id))
                {
                    return new ParticipantDetail
                    {
                        Id = byIdResponse.Id,
                        Code = string.IsNullOrWhiteSpace(byIdResponse.Code) ? byIdResponse.Id : byIdResponse.Code,
                        FullName = string.IsNullOrWhiteSpace(byIdResponse.FullName) ? byIdResponse.Code : byIdResponse.FullName,
                        AvatarUrl = string.IsNullOrWhiteSpace(byIdResponse.AvatarUrl) ? null : byIdResponse.AvatarUrl,
                        Role = byIdResponse.Role
                    };
                }
            }
            catch (Exception ex)
            {
                _logger.LogDebug(ex, "[IdentityClient] GetUserAsync by UUID {UserId} failed, falling back to getUserByCodeAsync", userCode);
            }
        }

        try
        {
            var response = await _userServiceClient.getUserByCodeAsync(
                new UserCodeRequest { Code = userCode },
                headers: headers
            );

            return new ParticipantDetail
            {
                Id = response.Id,
                Code = string.IsNullOrWhiteSpace(response.Code) ? userCode : response.Code,
                FullName = string.IsNullOrWhiteSpace(response.FullName) ? userCode : response.FullName,
                AvatarUrl = string.IsNullOrWhiteSpace(response.AvatarUrl) ? null : response.AvatarUrl,
                Role = response.Role
            };
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[IdentityClient] Failed to get user by code {Code}", userCode);
            return new ParticipantDetail
            {
                Id = userCode,
                Code = userCode,
                FullName = userCode
            };
        }
    }

    public async Task<ParticipantDetail?> GetCurrentUserAsync(string? bearerToken = null)
    {
        try
        {
            var headers = CreateHeaders(bearerToken);
            var response = await _userServiceClient.GetCurrentUserAsync(
                new GetCurrentUserRequest(),
                headers: headers
            );

            return new ParticipantDetail
            {
                Id = response.Id,
                Code = response.Code,
                FullName = response.FullName,
                AvatarUrl = string.IsNullOrWhiteSpace(response.AvatarUrl) ? null : response.AvatarUrl,
                Role = response.Role
            };
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "[IdentityClient] Failed to get current user");
            return null;
        }
    }

    private static Metadata CreateHeaders(string? bearerToken)
    {
        var headers = new Metadata();
        if (!string.IsNullOrWhiteSpace(bearerToken))
        {
            if (!bearerToken.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
            {
                bearerToken = "Bearer " + bearerToken;
            }
            headers.Add("authorization", bearerToken);
        }
        return headers;
    }
}
