using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using AiAvailableService.Configuration;
using AiAvailableService.Infrastructure.Authentication;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace AiAvailableService.Infrastructure.Payment;

public interface IInternalServiceTokenProvider
{
    string CreatePaymentServiceToken();
}

/// <summary>
/// Reuses the repository's existing HS512 ROLE_SERVICE convention used by the
/// Java common-starter. This token is only sent to Payment gRPC and is never
/// derived from, or replaced by, a browser JWT.
/// </summary>
public sealed class InternalServiceTokenProvider(IOptions<JwtOptions> jwtOptions) : IInternalServiceTokenProvider
{
    private readonly byte[] signingKey = JwtSigningKey.DecodeBase64(jwtOptions.Value.Base64SigningKey);

    public string CreatePaymentServiceToken()
    {
        var now = DateTime.UtcNow;
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, "ai-available-service"),
            new Claim("userId", "ai-available-service"),
            new Claim("role", "SERVICE")
        };
        var token = new JwtSecurityToken(
            claims: claims,
            notBefore: now,
            expires: now.AddMinutes(1),
            signingCredentials: new SigningCredentials(
                new SymmetricSecurityKey(signingKey),
                SecurityAlgorithms.HmacSha512));
        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
