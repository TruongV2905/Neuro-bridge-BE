namespace AiAvailableService.Contracts;

public sealed class CreateDraftProductRequest
{
    public string? Name { get; init; }
    public string? CategoryCode { get; init; }
    public decimal PriceVnd { get; init; }
}

public sealed class UpdateProductRequest
{
    public string? Name { get; init; }
    public string? Description { get; init; }
    public string? CategoryCode { get; init; }
    public string[]? Technologies { get; init; }
    public string[]? AiCapabilities { get; init; }
    public decimal PriceVnd { get; init; }
}

public sealed record OwnerProductListItemResponse(
    Guid Id,
    string Name,
    string CategoryCode,
    decimal PriceVnd,
    string Status,
    PublicPreviewResponse? Preview,
    DateTimeOffset UpdatedAt,
    DateTimeOffset? LastPublishedAt);

public sealed record OwnerProductDetailResponse(
    Guid Id,
    string Name,
    string? Description,
    string CategoryCode,
    string[] Technologies,
    string[] AiCapabilities,
    decimal PriceVnd,
    string Status,
    PublicPreviewResponse? Preview,
    DateTimeOffset UpdatedAt,
    DateTimeOffset? LastPublishedAt,
    DateTimeOffset? FirstPublishedAt,
    DateTimeOffset CreatedAt,
    IReadOnlyList<ProtectedAssetResponse> Assets);

public sealed record LifecycleActionResponse(
    Guid ProductId,
    string Status,
    DateTimeOffset? FirstPublishedAt,
    DateTimeOffset? LastPublishedAt,
    bool AlreadyInTargetState);

public sealed record MarketplaceProductListItemResponse(
    Guid Id,
    string Name,
    string CategoryCode,
    decimal PriceVnd,
    string[] Technologies,
    string[] AiCapabilities,
    PublicPreviewResponse? Preview,
    DateTimeOffset LastPublishedAt);

public sealed record MarketplaceProductDetailResponse(
    Guid Id,
    string Name,
    string Description,
    string CategoryCode,
    string[] Technologies,
    string[] AiCapabilities,
    decimal PriceVnd,
    PublicPreviewResponse? Preview,
    DateTimeOffset LastPublishedAt,
    BasicSellerSummaryResponse Seller);

public sealed record BasicSellerSummaryResponse(
    Guid ExpertId,
    string ExpertCode,
    string DisplayName,
    string? AvatarUrl);

// Phase 6 has no storage-access integration. Access fields stay null until Phase 7 can
// derive a short-lived URL; no durable bucket/object identity is ever serialized.
public sealed record PublicPreviewResponse(
    string Type,
    string OriginalFilename,
    string ContentType,
    long ByteSize,
    decimal? VideoDurationSeconds,
    DateTimeOffset UploadedAt,
    string? AccessUrl,
    DateTimeOffset? AccessUrlExpiresAt);

public sealed record ProtectedAssetResponse(
    string AssetType,
    string OriginalFilename,
    string ContentType,
    long ByteSize,
    DateTimeOffset UploadedAt);

public sealed class PreviewUploadAuthorizationRequest
{
    public string? PreviewType { get; init; }
    public string? OriginalFilename { get; init; }
    public string? ContentType { get; init; }
    public long ByteSize { get; init; }
}

public sealed class ProtectedAssetUploadAuthorizationRequest
{
    public string? OriginalFilename { get; init; }
    public string? ContentType { get; init; }
    public long ByteSize { get; init; }
}

public sealed class ConfirmPreviewUploadRequest
{
    public string? UploadToken { get; init; }
    public string? ObjectETag { get; init; }

    // Accepted only for diagnostics/client UX. It is never persisted or used
    // to authorize a VIDEO preview; IMediaDurationInspector is authoritative.
    public decimal? ClientObservedVideoDurationSeconds { get; init; }
}

public sealed class ConfirmProtectedAssetUploadRequest
{
    public string? UploadToken { get; init; }
    public string? ObjectETag { get; init; }
}

/// <summary>
/// The bucket/key are intentionally returned only to the authenticated Expert
/// who has just obtained a short-lived browser-direct upload authorization.
/// They are never included in general Product, Purchase, or marketplace DTOs.
/// </summary>
public sealed record SupabaseUploadAuthorizationResponse(
    string UploadToken,
    string UploadUrl,
    string UploadMethod,
    IReadOnlyDictionary<string, string> RequiredHeaders,
    string StorageBucket,
    string StorageObjectKey,
    DateTimeOffset ExpiresAt);

public sealed record ProtectedDownloadAccessResponse(
    Guid PurchaseId,
    Guid ProductId,
    string AssetType,
    string OriginalFilename,
    string ContentType,
    long ByteSize,
    string AccessUrl,
    DateTimeOffset AccessUrlExpiresAt);

public sealed class BrowseProductsQuery
{
    public int Page { get; init; }
    public int Size { get; init; } = 10;
    public string? Keyword { get; init; }
    public string? CategoryCode { get; init; }
    public decimal? MinPriceVnd { get; init; }
    public decimal? MaxPriceVnd { get; init; }
}
