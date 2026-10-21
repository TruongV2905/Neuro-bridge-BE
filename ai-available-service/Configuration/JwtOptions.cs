namespace AiAvailableService.Configuration;

public sealed class JwtOptions
{
    public const string SectionName = "Jwt";

    public string Base64SigningKey { get; init; } = string.Empty;
    public string Issuer { get; init; } = "nqt";
    public string Audience { get; init; } = "nqt-app";
}
