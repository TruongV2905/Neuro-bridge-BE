namespace AiAvailableService.Configuration;

public sealed class SupabaseOptions
{
    public const string SectionName = "Supabase";

    public string Url { get; init; } = string.Empty;
    public string ServiceRoleKey { get; init; } = string.Empty;
    public string PreviewBucket { get; init; } = "ai-place-previews";
    public string ProtectedAssetBucket { get; init; } = "ai-place-protected";
}
