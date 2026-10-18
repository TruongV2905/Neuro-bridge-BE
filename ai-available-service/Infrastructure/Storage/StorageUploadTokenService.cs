using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using AiAvailableService.Configuration;
using AiAvailableService.Infrastructure.Authentication;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace AiAvailableService.Infrastructure.Storage;

public interface IStorageUploadTokenService
{
    string Create(StorageUploadTokenClaims claims);
    StorageUploadTokenClaims Validate(string? token);
}

public sealed record StorageUploadTokenClaims(
    Guid ExpertId,
    Guid ProductId,
    string Slot,
    string? PreviewType,
    string StorageBucket,
    string StorageObjectKey,
    string ExpectedContentType,
    long ExpectedByteSize,
    string OriginalFilename,
    string Nonce,
    DateTimeOffset ExpiresAt);

/// <summary>
/// Signs and validates a purpose-bound token used only to confirm a direct
/// upload. It is not an authentication JWT and is never forwarded to Supabase.
/// </summary>
public sealed class StorageUploadTokenService(
    IOptions<StorageUploadTokenOptions> optionsAccessor) : IStorageUploadTokenService
{
    private const string Issuer = "ai-available-service";
    private const string Audience = "ai-place-storage-upload";
    private const string Purpose = "AI_PLACE_STORAGE_UPLOAD";
    private readonly JwtSecurityTokenHandler _handler = new();

    public string Create(StorageUploadTokenClaims claims)
    {
        var key = GetSigningKey();
        var identity = new ClaimsIdentity(new[]
        {
            new Claim("purpose", Purpose),
            new Claim("expertId", claims.ExpertId.ToString()),
            new Claim("productId", claims.ProductId.ToString()),
            new Claim("slot", claims.Slot),
            new Claim("storageBucket", claims.StorageBucket),
            new Claim("storageObjectKey", claims.StorageObjectKey),
            new Claim("contentType", claims.ExpectedContentType),
            new Claim("byteSize", claims.ExpectedByteSize.ToString(System.Globalization.CultureInfo.InvariantCulture)),
            new Claim("originalFilename", claims.OriginalFilename),
            new Claim(JwtRegisteredClaimNames.Jti, claims.Nonce)
        });
        if (!string.IsNullOrWhiteSpace(claims.PreviewType))
        {
            identity.AddClaim(new Claim("previewType", claims.PreviewType));
        }

        var descriptor = new SecurityTokenDescriptor
        {
            Subject = identity,
            Issuer = Issuer,
            Audience = Audience,
            NotBefore = DateTime.UtcNow,
            Expires = claims.ExpiresAt.UtcDateTime,
            SigningCredentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha512)
        };
        return _handler.WriteToken(_handler.CreateToken(descriptor));
    }

    public StorageUploadTokenClaims Validate(string? token)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            throw new InvalidStorageUploadTokenException();
        }

        try
        {
            var principal = _handler.ValidateToken(token, new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = GetSigningKey(),
                ValidateIssuer = true,
                ValidIssuer = Issuer,
                ValidateAudience = true,
                ValidAudience = Audience,
                ValidateLifetime = true,
                RequireExpirationTime = true,
                RequireSignedTokens = true,
                ValidAlgorithms = [SecurityAlgorithms.HmacSha512],
                ClockSkew = TimeSpan.Zero
            }, out var validatedToken);

            if (validatedToken is not JwtSecurityToken jwtToken || jwtToken.ValidTo <= DateTime.UtcNow ||
                !string.Equals(principal.FindFirstValue("purpose"), Purpose, StringComparison.Ordinal) ||
                !Guid.TryParse(principal.FindFirstValue("expertId"), out var expertId) ||
                !Guid.TryParse(principal.FindFirstValue("productId"), out var productId) ||
                !long.TryParse(principal.FindFirstValue("byteSize"), System.Globalization.NumberStyles.None,
                    System.Globalization.CultureInfo.InvariantCulture, out var byteSize) ||
                byteSize < 0 ||
                string.IsNullOrWhiteSpace(principal.FindFirstValue("slot")) ||
                string.IsNullOrWhiteSpace(principal.FindFirstValue("storageBucket")) ||
                string.IsNullOrWhiteSpace(principal.FindFirstValue("storageObjectKey")) ||
                string.IsNullOrWhiteSpace(principal.FindFirstValue("contentType")) ||
                string.IsNullOrWhiteSpace(principal.FindFirstValue("originalFilename")) ||
                string.IsNullOrWhiteSpace(principal.FindFirstValue(JwtRegisteredClaimNames.Jti)))
            {
                throw new InvalidStorageUploadTokenException();
            }

            return new StorageUploadTokenClaims(
                expertId,
                productId,
                principal.FindFirstValue("slot")!,
                principal.FindFirstValue("previewType"),
                principal.FindFirstValue("storageBucket")!,
                principal.FindFirstValue("storageObjectKey")!,
                principal.FindFirstValue("contentType")!,
                byteSize,
                principal.FindFirstValue("originalFilename")!,
                principal.FindFirstValue(JwtRegisteredClaimNames.Jti)!,
                new DateTimeOffset(jwtToken.ValidTo, TimeSpan.Zero));
        }
        catch (SecurityTokenExpiredException)
        {
            throw new StorageUploadTokenExpiredException();
        }
        catch (StorageUnavailableException)
        {
            throw;
        }
        catch (InvalidStorageUploadTokenException)
        {
            throw;
        }
        catch (SecurityTokenException)
        {
            throw new InvalidStorageUploadTokenException();
        }
        catch (ArgumentException)
        {
            throw new InvalidStorageUploadTokenException();
        }
    }

    private SymmetricSecurityKey GetSigningKey()
    {
        try
        {
            return new SymmetricSecurityKey(JwtSigningKey.DecodeBase64(
                optionsAccessor.Value.Base64SigningKey,
                "StorageUploadToken:Base64SigningKey"));
        }
        catch (InvalidOperationException)
        {
            throw new StorageUnavailableException();
        }
    }
}
