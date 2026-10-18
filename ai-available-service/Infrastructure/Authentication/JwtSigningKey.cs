namespace AiAvailableService.Infrastructure.Authentication;

public static class JwtSigningKey
{
    public static byte[] DecodeBase64(string? configuredValue, string configurationName = "Jwt:Base64SigningKey")
    {
        if (string.IsNullOrWhiteSpace(configuredValue))
        {
            throw new InvalidOperationException(
                $"{configurationName} is required. Configure it through an environment variable, user secrets, or a deployment secret manager.");
        }

        try
        {
            var key = Convert.FromBase64String(configuredValue);
            if (key.Length < 64)
            {
                throw new InvalidOperationException(
                    $"{configurationName} must decode to at least 64 bytes for HS512.");
            }

            return key;
        }
        catch (FormatException exception)
        {
            throw new InvalidOperationException(
                $"{configurationName} must be a valid Base64 value.", exception);
        }
    }
}
