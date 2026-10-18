namespace AiAvailableService.Configuration;

/// <summary>
/// Separate signing material for short-lived, application-owned upload
/// confirmation tokens. This is intentionally not a browser or Supabase key.
/// </summary>
public sealed class StorageUploadTokenOptions
{
    public const string SectionName = "StorageUploadToken";

    public string Base64SigningKey { get; init; } = string.Empty;
    public int LifetimeMinutes { get; init; } = 10;
}
