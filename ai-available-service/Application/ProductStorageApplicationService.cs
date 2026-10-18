using System.Net.Http.Headers;
using AiAvailableService.Api;
using AiAvailableService.Configuration;
using AiAvailableService.Contracts;
using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using AiAvailableService.Infrastructure.Authentication;
using AiAvailableService.Infrastructure.Identity;
using AiAvailableService.Infrastructure.Persistence;
using AiAvailableService.Infrastructure.Storage;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace AiAvailableService.Application;

public interface IProductStorageApplicationService
{
    Task<SupabaseUploadAuthorizationResponse> CreatePreviewUploadAuthorizationAsync(
        Guid productId,
        PreviewUploadAuthorizationRequest request,
        CancellationToken cancellationToken);

    Task<PublicPreviewResponse> ConfirmPreviewAsync(
        Guid productId,
        ConfirmPreviewUploadRequest request,
        CancellationToken cancellationToken);

    Task DeletePreviewAsync(Guid productId, CancellationToken cancellationToken);

    Task<SupabaseUploadAuthorizationResponse> CreateAssetUploadAuthorizationAsync(
        Guid productId,
        string assetType,
        ProtectedAssetUploadAuthorizationRequest request,
        CancellationToken cancellationToken);

    Task<ProtectedAssetResponse> ConfirmAssetAsync(
        Guid productId,
        string assetType,
        ConfirmProtectedAssetUploadRequest request,
        CancellationToken cancellationToken);

    Task DeleteAssetAsync(Guid productId, string assetType, CancellationToken cancellationToken);

    Task<ProtectedDownloadAccessResponse> GetDownloadAccessAsync(
        Guid purchaseId,
        Guid productId,
        string assetType,
        CancellationToken cancellationToken);
}

/// <summary>
/// Owns AI Place's storage authorization, exact-object verification, Product
/// metadata persistence, and buyer entitlement checks. Supabase owns bytes;
/// no method here accepts a file stream or returns a durable protected path.
/// </summary>
public sealed class ProductStorageApplicationService(
    AiAvailableDbContext db,
    ICurrentUser currentUser,
    IIdentityDirectory identityDirectory,
    IObjectStorageService objectStorage,
    IStorageUploadTokenService uploadTokens,
    IMediaDurationInspector mediaDurationInspector,
    IOptions<SupabaseOptions> supabaseOptions,
    IOptions<StorageUploadTokenOptions> uploadTokenOptions,
    ILogger<ProductStorageApplicationService> logger) : IProductStorageApplicationService
{
    private const string PreviewSlot = "PREVIEW";
    private static readonly TimeSpan PreviewReadLifetime = TimeSpan.FromMinutes(5);
    private static readonly TimeSpan ProtectedReadLifetime = TimeSpan.FromMinutes(5);

    public async Task<SupabaseUploadAuthorizationResponse> CreatePreviewUploadAuthorizationAsync(
        Guid productId,
        PreviewUploadAuthorizationRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var input = ValidatePreviewUploadRequest(request);
            var expert = await RequireActiveExpertAsync(cancellationToken);
            var plan = await CreatePreviewPlanAsync(productId, expert.Id, input, cancellationToken);
            return await IssueUploadAuthorizationAsync(plan, cancellationToken);
        }
        catch (StorageUnavailableException)
        {
            throw StorageUnavailable();
        }
    }

    public async Task<PublicPreviewResponse> ConfirmPreviewAsync(
        Guid productId,
        ConfirmPreviewUploadRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var expert = await RequireActiveExpertAsync(cancellationToken);
            var claims = ValidateUploadToken(request.UploadToken);
            EnsurePreviewClaims(claims, expert.Id, productId);
            var metadata = await GetAndValidateMetadataAsync(claims, request.ObjectETag, cancellationToken);
            var previewType = ParsePreviewType(claims.PreviewType);
            decimal? videoDuration = null;
            if (previewType == PreviewType.Video)
            {
                var inspectedDuration = await mediaDurationInspector.GetDurationSecondsAsync(
                    claims.StorageBucket, claims.StorageObjectKey, metadata.ContentType, cancellationToken);
                if (inspectedDuration <= 0 || inspectedDuration > 240m)
                {
                    throw new ApiException(StatusCodes.Status422UnprocessableEntity, "PREVIEW_VIDEO_TOO_LONG",
                        "The trusted Preview video duration must be greater than zero and no more than 240 seconds.");
                }

                videoDuration = decimal.Round(inspectedDuration, 3, MidpointRounding.AwayFromZero);
                if (videoDuration <= 0)
                {
                    throw UploadMetadataMismatch();
                }
            }

            var persisted = await PersistPreviewAsync(productId, expert.Id, claims, metadata, previewType,
                videoDuration, cancellationToken);
            await TryDeleteAfterCommitAsync(persisted.OldObject, productId, cancellationToken);
            return await CreatePreviewResponseAsync(previewType, claims.OriginalFilename, metadata.ContentType,
                metadata.ByteSize, videoDuration, claims.StorageBucket, claims.StorageObjectKey, persisted.UploadedAt,
                cancellationToken);
        }
        catch (StorageUploadTokenExpiredException)
        {
            throw UploadAuthorizationExpired();
        }
        catch (InvalidStorageUploadTokenException)
        {
            throw InvalidUploadToken();
        }
        catch (MediaDurationInspectionUnavailableException)
        {
            throw MediaInspectionUnavailable();
        }
        catch (StorageUnavailableException)
        {
            throw StorageUnavailable();
        }
    }

    public async Task DeletePreviewAsync(Guid productId, CancellationToken cancellationToken)
    {
        var expert = await RequireActiveExpertAsync(cancellationToken);
        StorageObjectReference? oldObject;
        await using (var transaction = await db.Database.BeginTransactionAsync(cancellationToken))
        {
            var product = await GetOwnedProductForUpdateAsync(productId, expert.Id, cancellationToken);
            EnsurePreviewEditable(product);
            oldObject = GetPreviewObjectReference(product);
            if (oldObject is not null)
            {
                ClearPreview(product);
                product.UpdatedAt = DateTimeOffset.UtcNow;
                await db.SaveChangesAsync(cancellationToken);
            }

            await transaction.CommitAsync(cancellationToken);
        }

        await TryDeleteAfterCommitAsync(oldObject, productId, cancellationToken);
    }

    public async Task<SupabaseUploadAuthorizationResponse> CreateAssetUploadAuthorizationAsync(
        Guid productId,
        string assetType,
        ProtectedAssetUploadAuthorizationRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var type = ParseAssetType(assetType);
            var input = ValidateProtectedAssetUploadRequest(request);
            var expert = await RequireActiveExpertAsync(cancellationToken);
            var plan = await CreateAssetPlanAsync(productId, expert.Id, type, input, cancellationToken);
            return await IssueUploadAuthorizationAsync(plan, cancellationToken);
        }
        catch (StorageUnavailableException)
        {
            throw StorageUnavailable();
        }
    }

    public async Task<ProtectedAssetResponse> ConfirmAssetAsync(
        Guid productId,
        string assetType,
        ConfirmProtectedAssetUploadRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var type = ParseAssetType(assetType);
            var expert = await RequireActiveExpertAsync(cancellationToken);
            var claims = ValidateUploadToken(request.UploadToken);
            EnsureAssetClaims(claims, expert.Id, productId, type);
            var metadata = await GetAndValidateMetadataAsync(claims, request.ObjectETag, cancellationToken);
            var persisted = await PersistAssetAsync(productId, expert.Id, type, claims, metadata, cancellationToken);
            await TryDeleteAfterCommitAsync(persisted.OldObject, productId, cancellationToken);
            return new ProtectedAssetResponse(ToApiValue(persisted.Asset.AssetType), persisted.Asset.OriginalFilename,
                persisted.Asset.ContentType, persisted.Asset.ByteSize, persisted.Asset.UploadedAt);
        }
        catch (StorageUploadTokenExpiredException)
        {
            throw UploadAuthorizationExpired();
        }
        catch (InvalidStorageUploadTokenException)
        {
            throw InvalidUploadToken();
        }
        catch (StorageUnavailableException)
        {
            throw StorageUnavailable();
        }
    }

    public async Task DeleteAssetAsync(
        Guid productId,
        string assetType,
        CancellationToken cancellationToken)
    {
        var type = ParseAssetType(assetType);
        var expert = await RequireActiveExpertAsync(cancellationToken);
        StorageObjectReference? oldObject = null;
        await using (var transaction = await db.Database.BeginTransactionAsync(cancellationToken))
        {
            var product = await GetOwnedProductForUpdateAsync(productId, expert.Id, cancellationToken);
            EnsureAssetEditable(product);
            var asset = await db.ProductAssets.SingleOrDefaultAsync(item => item.ProductId == productId && item.AssetType == type,
                cancellationToken);
            if (asset is not null)
            {
                oldObject = new StorageObjectReference(asset.StorageBucket, asset.StorageObjectKey);
                db.ProductAssets.Remove(asset);
                product.UpdatedAt = DateTimeOffset.UtcNow;
                await db.SaveChangesAsync(cancellationToken);
            }

            await transaction.CommitAsync(cancellationToken);
        }

        await TryDeleteAfterCommitAsync(oldObject, productId, cancellationToken);
    }

    public async Task<ProtectedDownloadAccessResponse> GetDownloadAccessAsync(
        Guid purchaseId,
        Guid productId,
        string assetType,
        CancellationToken cancellationToken)
    {
        try
        {
            var type = ParseAssetType(assetType);
            var purchase = await db.ProductPurchases.AsNoTracking()
                .SingleOrDefaultAsync(item => item.Id == purchaseId, cancellationToken)
                ?? throw PurchaseNotFound();
            if (purchase.BuyerClientId != currentUser.UserId)
            {
                throw ApiException.Forbidden();
            }

            if (purchase.Status != PurchaseStatus.Success || purchase.ProductId != productId)
            {
                throw PurchaseNotFound();
            }

            var now = DateTimeOffset.UtcNow;
            if (purchase.AccessExpiresAt is null || now >= purchase.AccessExpiresAt.Value)
            {
                throw new ApiException(StatusCodes.Status403Forbidden, "ENTITLEMENT_EXPIRED",
                    "This Purchase entitlement has expired.");
            }

            var asset = await db.ProductAssets.AsNoTracking()
                .SingleOrDefaultAsync(item => item.ProductId == productId && item.AssetType == type, cancellationToken)
                ?? throw new ApiException(StatusCodes.Status404NotFound, "PRODUCT_ASSET_NOT_FOUND",
                    "The requested Product asset was not found.");
            var urlExpiry = Min(now.Add(ProtectedReadLifetime), purchase.AccessExpiresAt.Value);
            var access = await objectStorage.CreateTemporaryReadAccessAsync(asset.StorageBucket, asset.StorageObjectKey,
                urlExpiry, cancellationToken);
            return new ProtectedDownloadAccessResponse(purchase.Id, purchase.ProductId, ToApiValue(type),
                asset.OriginalFilename, asset.ContentType, asset.ByteSize, access.AccessUri.ToString(), access.ExpiresAt);
        }
        catch (StorageUnavailableException)
        {
            throw StorageUnavailable();
        }
    }

    private async Task<SupabaseUploadAuthorizationResponse> IssueUploadAuthorizationAsync(
        StorageUploadPlan plan,
        CancellationToken cancellationToken)
    {
        var tokenClaims = new StorageUploadTokenClaims(plan.ExpertId, plan.ProductId, plan.Slot, plan.PreviewType,
            plan.Bucket, plan.ObjectKey, plan.ContentType, plan.ByteSize, plan.OriginalFilename, plan.Nonce, plan.ExpiresAt);
        var uploadToken = uploadTokens.Create(tokenClaims);
        var storageAuthorization = await objectStorage.CreateUploadAuthorizationAsync(new StorageUploadRequest(
            plan.Bucket, plan.ObjectKey, plan.ContentType, plan.ByteSize, plan.ExpiresAt), cancellationToken);
        var visibleExpiry = Min(plan.ExpiresAt, storageAuthorization.ExpiresAt);
        return new SupabaseUploadAuthorizationResponse(uploadToken, storageAuthorization.UploadUri.ToString(), "PUT",
            storageAuthorization.RequiredHeaders, plan.Bucket, plan.ObjectKey, visibleExpiry);
    }

    private async Task<StorageUploadPlan> CreatePreviewPlanAsync(
        Guid productId,
        Guid expertId,
        ValidatedPreviewUpload input,
        CancellationToken cancellationToken)
    {
        var bucket = GetPreviewBucket();
        var nonce = Guid.NewGuid().ToString("N");
        var objectKey = $"ai-place/{expertId}/{productId}/preview/{nonce}";
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedProductForUpdateAsync(productId, expertId, cancellationToken);
        EnsurePreviewEditable(product);
        await transaction.CommitAsync(cancellationToken);
        return new StorageUploadPlan(productId, expertId, PreviewSlot, ToApiValue(input.PreviewType), bucket, objectKey,
            input.ContentType, input.ByteSize, input.OriginalFilename, nonce, GetUploadExpiry());
    }

    private async Task<StorageUploadPlan> CreateAssetPlanAsync(
        Guid productId,
        Guid expertId,
        ProductAssetType assetType,
        ValidatedUpload input,
        CancellationToken cancellationToken)
    {
        var bucket = GetProtectedAssetBucket();
        var nonce = Guid.NewGuid().ToString("N");
        var slot = ToApiValue(assetType);
        var objectKey = $"ai-place/{expertId}/{productId}/assets/{slot}/{nonce}";
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedProductForUpdateAsync(productId, expertId, cancellationToken);
        EnsureAssetEditable(product);
        await transaction.CommitAsync(cancellationToken);
        return new StorageUploadPlan(productId, expertId, slot, null, bucket, objectKey, input.ContentType,
            input.ByteSize, input.OriginalFilename, nonce, GetUploadExpiry());
    }

    private async Task<StorageObjectMetadata> GetAndValidateMetadataAsync(
        StorageUploadTokenClaims claims,
        string? clientObjectETag,
        CancellationToken cancellationToken)
    {
        var metadata = await objectStorage.GetObjectMetadataAsync(claims.StorageBucket, claims.StorageObjectKey,
            cancellationToken) ?? throw new ApiException(StatusCodes.Status404NotFound, "UPLOAD_OBJECT_NOT_FOUND",
            "The authorized upload object was not found.");
        if (!string.Equals(metadata.Bucket, claims.StorageBucket, StringComparison.Ordinal) ||
            !string.Equals(metadata.ObjectKey, claims.StorageObjectKey, StringComparison.Ordinal) ||
            !string.Equals(metadata.ContentType.Trim(), claims.ExpectedContentType, StringComparison.OrdinalIgnoreCase) ||
            metadata.ByteSize != claims.ExpectedByteSize)
        {
            throw UploadMetadataMismatch();
        }

        var requestedETag = NormalizeOptional(clientObjectETag);
        if (requestedETag is not null &&
            (string.IsNullOrWhiteSpace(metadata.ETag) ||
             !string.Equals(requestedETag, metadata.ETag, StringComparison.Ordinal)))
        {
            throw UploadMetadataMismatch();
        }

        return metadata;
    }

    private async Task<PersistedPreview> PersistPreviewAsync(
        Guid productId,
        Guid expertId,
        StorageUploadTokenClaims claims,
        StorageObjectMetadata metadata,
        PreviewType previewType,
        decimal? videoDuration,
        CancellationToken cancellationToken)
    {
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedProductForUpdateAsync(productId, expertId, cancellationToken);
        EnsurePreviewEditable(product);
        var oldObject = GetPreviewObjectReference(product);
        var now = DateTimeOffset.UtcNow;
        product.PreviewType = previewType;
        product.PreviewStorageBucket = claims.StorageBucket;
        product.PreviewStorageObjectKey = claims.StorageObjectKey;
        product.PreviewOriginalFilename = claims.OriginalFilename;
        product.PreviewContentType = metadata.ContentType;
        product.PreviewByteSize = metadata.ByteSize;
        product.PreviewVideoDurationSeconds = videoDuration;
        product.PreviewUploadedAt = now;
        product.UpdatedAt = now;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return new PersistedPreview(
            IsSameObject(oldObject, claims.StorageBucket, claims.StorageObjectKey) ? null : oldObject,
            now);
    }

    private async Task<PersistedAsset> PersistAssetAsync(
        Guid productId,
        Guid expertId,
        ProductAssetType assetType,
        StorageUploadTokenClaims claims,
        StorageObjectMetadata metadata,
        CancellationToken cancellationToken)
    {
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedProductForUpdateAsync(productId, expertId, cancellationToken);
        EnsureAssetEditable(product);
        var asset = await db.ProductAssets.SingleOrDefaultAsync(item => item.ProductId == productId && item.AssetType == assetType,
            cancellationToken);
        StorageObjectReference? oldObject = null;
        var now = DateTimeOffset.UtcNow;
        if (asset is null)
        {
            asset = new ProductAsset
            {
                Id = Guid.NewGuid(),
                ProductId = productId,
                AssetType = assetType
            };
            db.ProductAssets.Add(asset);
        }
        else
        {
            oldObject = new StorageObjectReference(asset.StorageBucket, asset.StorageObjectKey);
        }

        asset.StorageBucket = claims.StorageBucket;
        asset.StorageObjectKey = claims.StorageObjectKey;
        asset.OriginalFilename = claims.OriginalFilename;
        asset.ContentType = metadata.ContentType;
        asset.ByteSize = metadata.ByteSize;
        asset.UploadedAt = now;
        product.UpdatedAt = now;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return new PersistedAsset(asset, IsSameObject(oldObject, claims.StorageBucket, claims.StorageObjectKey) ? null : oldObject);
    }

    private async Task<PublicPreviewResponse> CreatePreviewResponseAsync(
        PreviewType previewType,
        string originalFilename,
        string contentType,
        long byteSize,
        decimal? videoDuration,
        string bucket,
        string objectKey,
        DateTimeOffset uploadedAt,
        CancellationToken cancellationToken)
    {
        var access = await objectStorage.CreateTemporaryReadAccessAsync(bucket, objectKey,
            DateTimeOffset.UtcNow.Add(PreviewReadLifetime), cancellationToken);
        return new PublicPreviewResponse(ToApiValue(previewType), originalFilename, contentType, byteSize,
            videoDuration, uploadedAt, access.AccessUri.ToString(), access.ExpiresAt);
    }

    private async Task TryDeleteAfterCommitAsync(
        StorageObjectReference? objectReference,
        Guid productId,
        CancellationToken cancellationToken)
    {
        if (objectReference is null)
        {
            return;
        }

        try
        {
            await objectStorage.DeleteObjectAsync(objectReference.Bucket, objectReference.ObjectKey, CancellationToken.None);
        }
        catch (Exception)
        {
            // The new durable metadata has already committed. Log only Product
            // identity, never bucket/key, provider URL, token, or credential.
            logger.LogWarning("Storage cleanup failed after Product {ProductId} metadata commit.", productId);
        }
    }

    private async Task<IdentityUser> RequireActiveExpertAsync(CancellationToken cancellationToken)
    {
        var identity = await identityDirectory.GetUserAsync(currentUser.UserId, cancellationToken);
        if (!string.Equals(identity.Role, "EXPERT", StringComparison.Ordinal) ||
            !string.Equals(identity.Status, "ACTIVE", StringComparison.Ordinal) ||
            string.IsNullOrWhiteSpace(identity.Code))
        {
            throw ApiException.Forbidden("An active Expert identity is required.");
        }

        return identity;
    }

    private async Task<AiProduct> GetOwnedProductForUpdateAsync(
        Guid productId,
        Guid expertId,
        CancellationToken cancellationToken)
    {
        var product = await db.AiProducts
            .FromSqlInterpolated($"SELECT * FROM ai_product WHERE id = {productId} FOR UPDATE")
            .SingleOrDefaultAsync(cancellationToken);
        if (product is not null)
        {
            if (product.SellerExpertId != expertId)
            {
                throw new ApiException(StatusCodes.Status403Forbidden, "PRODUCT_NOT_OWNED",
                    "This Product belongs to a different Expert.");
            }

            return product;
        }

        var exists = await db.AiProducts.AsNoTracking().AnyAsync(item => item.Id == productId, cancellationToken);
        if (exists)
        {
            throw new ApiException(StatusCodes.Status403Forbidden, "PRODUCT_NOT_OWNED",
                "This Product belongs to a different Expert.");
        }

        throw new ApiException(StatusCodes.Status404NotFound, "PRODUCT_NOT_FOUND", "Product was not found.");
    }

    private static void EnsurePreviewEditable(AiProduct product)
    {
        if (product.Status is not (ProductStatus.Draft or ProductStatus.Unpublished))
        {
            throw InvalidProductState("Preview metadata may be changed only while the Product is Draft or Unpublished.");
        }
    }

    private static void EnsureAssetEditable(AiProduct product)
    {
        if (product.FirstPublishedAt is not null)
        {
            throw new ApiException(StatusCodes.Status409Conflict, "DELIVERABLE_LOCKED",
                "Protected Product deliverables are permanently locked after first publication.");
        }

        if (product.Status != ProductStatus.Draft)
        {
            throw InvalidProductState("Protected Product deliverables may be changed only while the Product is Draft.");
        }
    }

    private StorageUploadTokenClaims ValidateUploadToken(string? token) => uploadTokens.Validate(token);

    private static void EnsurePreviewClaims(StorageUploadTokenClaims claims, Guid expertId, Guid productId)
    {
        if (claims.ExpertId != expertId || claims.ProductId != productId ||
            !string.Equals(claims.Slot, PreviewSlot, StringComparison.Ordinal) ||
            claims.PreviewType is null ||
            claims.PreviewType is not ("IMAGE" or "VIDEO"))
        {
            throw new InvalidStorageUploadTokenException();
        }
    }

    private static void EnsureAssetClaims(
        StorageUploadTokenClaims claims,
        Guid expertId,
        Guid productId,
        ProductAssetType assetType)
    {
        if (claims.ExpertId != expertId || claims.ProductId != productId || claims.PreviewType is not null ||
            !string.Equals(claims.Slot, ToApiValue(assetType), StringComparison.Ordinal))
        {
            throw new InvalidStorageUploadTokenException();
        }
    }

    private StorageObjectReference? GetPreviewObjectReference(AiProduct product) =>
        string.IsNullOrWhiteSpace(product.PreviewStorageBucket) || string.IsNullOrWhiteSpace(product.PreviewStorageObjectKey)
            ? null
            : new StorageObjectReference(product.PreviewStorageBucket, product.PreviewStorageObjectKey);

    private static void ClearPreview(AiProduct product)
    {
        product.PreviewType = null;
        product.PreviewStorageBucket = null;
        product.PreviewStorageObjectKey = null;
        product.PreviewOriginalFilename = null;
        product.PreviewContentType = null;
        product.PreviewByteSize = null;
        product.PreviewVideoDurationSeconds = null;
        product.PreviewUploadedAt = null;
    }

    private string GetPreviewBucket()
    {
        var options = supabaseOptions.Value;
        EnsureDistinctConfiguredBuckets(options);
        return RequireBucket(options.PreviewBucket);
    }

    private string GetProtectedAssetBucket()
    {
        var options = supabaseOptions.Value;
        EnsureDistinctConfiguredBuckets(options);
        return RequireBucket(options.ProtectedAssetBucket);
    }

    private DateTimeOffset GetUploadExpiry()
    {
        var lifetime = uploadTokenOptions.Value.LifetimeMinutes;
        if (lifetime is < 1 or > 60)
        {
            throw new StorageUnavailableException();
        }

        return DateTimeOffset.UtcNow.AddMinutes(lifetime);
    }

    private static void EnsureDistinctConfiguredBuckets(SupabaseOptions options)
    {
        if (string.IsNullOrWhiteSpace(options.PreviewBucket) ||
            string.IsNullOrWhiteSpace(options.ProtectedAssetBucket) ||
            string.Equals(options.PreviewBucket.Trim(), options.ProtectedAssetBucket.Trim(), StringComparison.Ordinal))
        {
            throw new StorageUnavailableException();
        }
    }

    private static string RequireBucket(string value)
    {
        var bucket = value.Trim();
        if (bucket.Length == 0 || bucket.Length > 255)
        {
            throw new StorageUnavailableException();
        }

        return bucket;
    }

    private static ValidatedPreviewUpload ValidatePreviewUploadRequest(PreviewUploadAuthorizationRequest request)
    {
        var basic = ValidateUploadRequest(request.OriginalFilename, request.ContentType, request.ByteSize);
        var errors = new Dictionary<string, string[]>();
        var previewType = request.PreviewType?.Trim().ToUpperInvariant() switch
        {
            "IMAGE" => PreviewType.Image,
            "VIDEO" => PreviewType.Video,
            _ => (PreviewType?)null
        };
        if (previewType is null)
        {
            errors["previewType"] = ["previewType must be IMAGE or VIDEO."];
        }

        if (previewType == PreviewType.Image && !IsMediaType(basic.ContentType, "image/"))
        {
            errors["contentType"] = ["IMAGE Preview contentType must be an image media type."];
        }

        if (previewType == PreviewType.Video && !IsMediaType(basic.ContentType, "video/"))
        {
            errors["contentType"] = ["VIDEO Preview contentType must be a video media type."];
        }

        if (errors.Count > 0)
        {
            throw ApiException.Validation(errors);
        }

        return new ValidatedPreviewUpload(previewType!.Value, basic.OriginalFilename, basic.ContentType, basic.ByteSize);
    }

    private static ValidatedUpload ValidateProtectedAssetUploadRequest(ProtectedAssetUploadAuthorizationRequest request) =>
        ValidateUploadRequest(request.OriginalFilename, request.ContentType, request.ByteSize);

    private static ValidatedUpload ValidateUploadRequest(string? originalFilename, string? contentType, long byteSize)
    {
        var errors = new Dictionary<string, string[]>();
        var filename = originalFilename?.Trim();
        if (string.IsNullOrWhiteSpace(filename))
        {
            errors["originalFilename"] = ["originalFilename is required and cannot be blank."];
        }
        else if (filename.Length > 255)
        {
            errors["originalFilename"] = ["originalFilename must be 255 characters or fewer."];
        }

        var normalizedContentType = contentType?.Trim();
        if (string.IsNullOrWhiteSpace(normalizedContentType) ||
            !MediaTypeHeaderValue.TryParse(normalizedContentType, out var parsed) ||
            string.IsNullOrWhiteSpace(parsed.MediaType))
        {
            errors["contentType"] = ["contentType must be a valid media type."];
        }
        else if (normalizedContentType.Length > 255)
        {
            errors["contentType"] = ["contentType must be 255 characters or fewer."];
        }

        if (byteSize < 0)
        {
            errors["byteSize"] = ["byteSize must be nonnegative."];
        }

        if (errors.Count > 0)
        {
            throw ApiException.Validation(errors);
        }

        return new ValidatedUpload(filename!, normalizedContentType!, byteSize);
    }

    private static bool IsMediaType(string contentType, string requiredPrefix) =>
        MediaTypeHeaderValue.TryParse(contentType, out var parsed) &&
        parsed.MediaType is not null && parsed.MediaType.StartsWith(requiredPrefix, StringComparison.OrdinalIgnoreCase);

    private static PreviewType ParsePreviewType(string? value) => value switch
    {
        "IMAGE" => PreviewType.Image,
        "VIDEO" => PreviewType.Video,
        _ => throw new InvalidStorageUploadTokenException()
    };

    private static ProductAssetType ParseAssetType(string? value) => value?.Trim().ToUpperInvariant() switch
    {
        "SOURCE_CODE" => ProductAssetType.SourceCode,
        "VIDEO" => ProductAssetType.Video,
        "DATASET" => ProductAssetType.Dataset,
        "DOCUMENT" => ProductAssetType.Document,
        _ => throw ApiException.Validation(new Dictionary<string, string[]>
        {
            ["assetType"] = ["assetType must be SOURCE_CODE, VIDEO, DATASET, or DOCUMENT."]
        })
    };

    private static string? NormalizeOptional(string? value) => string.IsNullOrWhiteSpace(value) ? null : value.Trim();

    private static bool IsSameObject(StorageObjectReference? reference, string bucket, string objectKey) =>
        reference is not null && string.Equals(reference.Bucket, bucket, StringComparison.Ordinal) &&
        string.Equals(reference.ObjectKey, objectKey, StringComparison.Ordinal);

    private static DateTimeOffset Min(DateTimeOffset left, DateTimeOffset right) => left <= right ? left : right;

    private static ApiException InvalidProductState(string message) =>
        new(StatusCodes.Status409Conflict, "INVALID_PRODUCT_STATE", message);

    private static ApiException UploadAuthorizationExpired() =>
        new(StatusCodes.Status409Conflict, "UPLOAD_AUTHORIZATION_EXPIRED", "The upload authorization has expired.");

    private static ApiException InvalidUploadToken() =>
        new(StatusCodes.Status400BadRequest, "INVALID_UPLOAD_TOKEN", "The upload authorization is invalid.");

    private static ApiException UploadMetadataMismatch() =>
        new(StatusCodes.Status422UnprocessableEntity, "UPLOAD_METADATA_MISMATCH",
            "The uploaded object does not match its authorized metadata.");

    private static ApiException MediaInspectionUnavailable() =>
        new(StatusCodes.Status503ServiceUnavailable, "MEDIA_INSPECTION_UNAVAILABLE",
            "Trusted video-duration inspection is temporarily unavailable.");

    private static ApiException StorageUnavailable() =>
        new(StatusCodes.Status503ServiceUnavailable, "STORAGE_UNAVAILABLE", "Object storage is temporarily unavailable.");

    private static ApiException PurchaseNotFound() =>
        new(StatusCodes.Status404NotFound, "PURCHASE_NOT_FOUND", "Purchase was not found.");

    private static string ToApiValue(ProductAssetType assetType) => assetType switch
    {
        ProductAssetType.SourceCode => "SOURCE_CODE",
        ProductAssetType.Video => "VIDEO",
        ProductAssetType.Dataset => "DATASET",
        ProductAssetType.Document => "DOCUMENT",
        _ => throw new ArgumentOutOfRangeException(nameof(assetType))
    };

    private static string ToApiValue(PreviewType previewType) => previewType switch
    {
        PreviewType.Image => "IMAGE",
        PreviewType.Video => "VIDEO",
        _ => throw new ArgumentOutOfRangeException(nameof(previewType))
    };

    private sealed record ValidatedUpload(string OriginalFilename, string ContentType, long ByteSize);

    private sealed record ValidatedPreviewUpload(PreviewType PreviewType, string OriginalFilename, string ContentType, long ByteSize);

    private sealed record StorageUploadPlan(
        Guid ProductId,
        Guid ExpertId,
        string Slot,
        string? PreviewType,
        string Bucket,
        string ObjectKey,
        string ContentType,
        long ByteSize,
        string OriginalFilename,
        string Nonce,
        DateTimeOffset ExpiresAt);

    private sealed record StorageObjectReference(string Bucket, string ObjectKey);

    private sealed record PersistedAsset(ProductAsset Asset, StorageObjectReference? OldObject);

    private sealed record PersistedPreview(StorageObjectReference? OldObject, DateTimeOffset UploadedAt);
}
