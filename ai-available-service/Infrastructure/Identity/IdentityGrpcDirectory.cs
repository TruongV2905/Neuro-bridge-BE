using AiAvailableService.Api;
using Com.Identity.Grpc;
using Grpc.Core;
using Microsoft.AspNetCore.Http;
using Microsoft.Net.Http.Headers;

namespace AiAvailableService.Infrastructure.Identity;

public sealed class IdentityGrpcDirectory(
    UserService.UserServiceClient userClient,
    IHttpContextAccessor httpContextAccessor) : IIdentityDirectory
{
    public async Task<IdentityUser> GetUserAsync(
        Guid userId,
        CancellationToken cancellationToken = default)
    {
        try
        {
            var authorization =
                httpContextAccessor.HttpContext?
                    .Request.Headers[HeaderNames.Authorization]
                    .ToString();

            if (string.IsNullOrWhiteSpace(authorization))
            {
                throw IdentityUnavailable();
            }

            var headers = new Metadata
            {
                { "Authorization", authorization }
            };

            var response = await userClient.GetUserAsync(
                new UserRequest
                {
                    UserId = userId.ToString()
                },
                headers: headers,
                cancellationToken: cancellationToken
            ).ResponseAsync;

            if (!Guid.TryParse(response.Id, out var responseId) ||
                responseId != userId)
            {
                throw IdentityUnavailable();
            }

            return new IdentityUser(
                responseId,
                response.Code,
                response.FullName,
                string.IsNullOrWhiteSpace(response.AvatarUrl)
                    ? null
                    : response.AvatarUrl,
                response.Role,
                response.Status,
                string.IsNullOrWhiteSpace(response.Email)
                    ? null
                    : response.Email);
        }
        catch (ApiException)
        {
            throw;
        }
        catch (RpcException)
        {
            throw IdentityUnavailable();
        }
    }

    private static ApiException IdentityUnavailable() =>
        new(
            StatusCodes.Status503ServiceUnavailable,
            "IDENTITY_UNAVAILABLE",
            "Identity information is temporarily unavailable.");
}
