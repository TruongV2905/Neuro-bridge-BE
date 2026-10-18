using AiAvailableService.Api;
using AiAvailableService.Contracts;
using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using AiAvailableService.Infrastructure.Authentication;
using AiAvailableService.Infrastructure.Identity;
using AiAvailableService.Infrastructure.Persistence;
using AiAvailableService.Infrastructure.Storage;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;

namespace AiAvailableService.Application;

public interface IProductApplicationService
{
    Task<OwnerProductDetailResponse> CreateDraftAsync(CreateDraftProductRequest request, CancellationToken cancellationToken);
    Task<PageResponse<OwnerProductListItemResponse>> ListOwnAsync(int page, int size, CancellationToken cancellationToken);
    Task<OwnerProductDetailResponse> GetOwnAsync(Guid productId, CancellationToken cancellationToken);
    Task<OwnerProductDetailResponse> UpdateAsync(Guid productId, UpdateProductRequest request, CancellationToken cancellationToken);
    Task DeleteAsync(Guid productId, CancellationToken cancellationToken);
    Task<LifecycleActionResponse> PublishAsync(Guid productId, CancellationToken cancellationToken);
    Task<LifecycleActionResponse> UnpublishAsync(Guid productId, CancellationToken cancellationToken);
    Task<PageResponse<MarketplaceProductListItemResponse>> BrowseAsync(BrowseProductsQuery request, CancellationToken cancellationToken);
    Task<MarketplaceProductDetailResponse> GetMarketplaceDetailAsync(Guid productId, CancellationToken cancellationToken);
}

public sealed class ProductApplicationService(
    AiAvailableDbContext db,
    ICurrentUser currentUser,
    IIdentityDirectory identityDirectory,
    IObjectStorageService objectStorage) : IProductApplicationService
{
    private static readonly ProductAssetType[] RequiredAssetTypes =
    [
        ProductAssetType.SourceCode,
        ProductAssetType.Video,
        ProductAssetType.Dataset,
        ProductAssetType.Document
    ];

    public async Task<OwnerProductDetailResponse> CreateDraftAsync(
        CreateDraftProductRequest request,
        CancellationToken cancellationToken)
    {
        var validated = ValidateCreate(request);
        var expert = await RequireActiveExpertAsync(cancellationToken);
        var now = DateTimeOffset.UtcNow;
        var product = new AiProduct
        {
            Id = Guid.NewGuid(),
            SellerExpertId = expert.Id,
            SellerExpertCode = expert.Code,
            Name = validated.Name,
            CategoryCode = validated.CategoryCode,
            PriceVnd = validated.PriceVnd,
            Technologies = Array.Empty<string>(),
            AiCapabilities = Array.Empty<string>(),
            Status = ProductStatus.Draft,
            CreatedAt = now,
            UpdatedAt = now
        };

        db.AiProducts.Add(product);
        await db.SaveChangesAsync(cancellationToken);
        return MapOwnerDetail(product);
    }

    public async Task<PageResponse<OwnerProductListItemResponse>> ListOwnAsync(
        int page,
        int size,
        CancellationToken cancellationToken)
    {
        var pagination = Pagination.Validate(page, size);
        var expert = await RequireActiveExpertAsync(cancellationToken);
        var query = db.AiProducts.AsNoTracking()
            .Where(product => product.SellerExpertId == expert.Id)
            .OrderByDescending(product => product.UpdatedAt)
            .ThenByDescending(product => product.Id);
        var total = await query.LongCountAsync(cancellationToken);
        var products = await query.Skip(pagination.Skip).Take(pagination.Size).ToListAsync(cancellationToken);
        return Pagination.Create(products.Select(MapOwnerListItem).ToList(), pagination, total);
    }

    public async Task<OwnerProductDetailResponse> GetOwnAsync(Guid productId, CancellationToken cancellationToken)
    {
        var expert = await RequireActiveExpertAsync(cancellationToken);
        var product = await db.AiProducts.AsNoTracking()
            .Include(item => item.Assets)
            .SingleOrDefaultAsync(item => item.Id == productId, cancellationToken);
        EnsureOwned(product, expert.Id);
        return MapOwnerDetail(product!);
    }

    public async Task<OwnerProductDetailResponse> UpdateAsync(
        Guid productId,
        UpdateProductRequest request,
        CancellationToken cancellationToken)
    {
        var validated = ValidateUpdate(request);
        var expert = await RequireActiveExpertAsync(cancellationToken);

        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedForUpdateAsync(productId, expert.Id, cancellationToken);
        if (product.Status == ProductStatus.Published)
        {
            throw InvalidProductState("Published Products must be unpublished before editing.");
        }

        product.Name = validated.Name;
        product.Description = validated.Description;
        product.CategoryCode = validated.CategoryCode;
        product.Technologies = validated.Technologies;
        product.AiCapabilities = validated.AiCapabilities;
        product.PriceVnd = validated.PriceVnd;
        product.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);

        await db.Entry(product).Collection(item => item.Assets).LoadAsync(cancellationToken);
        return MapOwnerDetail(product);
    }

    public async Task DeleteAsync(Guid productId, CancellationToken cancellationToken)
    {
        var expert = await RequireActiveExpertAsync(cancellationToken);
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedForUpdateAsync(productId, expert.Id, cancellationToken);
        if (product.Status != ProductStatus.Draft || product.FirstPublishedAt is not null)
        {
            throw InvalidProductState("Only a never-published Draft Product may be deleted.");
        }

        db.AiProducts.Remove(product);
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
    }

    public async Task<LifecycleActionResponse> PublishAsync(Guid productId, CancellationToken cancellationToken)
    {
        var expert = await RequireActiveExpertAsync(cancellationToken);
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedForUpdateAsync(productId, expert.Id, cancellationToken);

        if (product.Status == ProductStatus.Published)
        {
            await transaction.CommitAsync(cancellationToken);
            return MapLifecycle(product, true);
        }

        if (product.Status is not (ProductStatus.Draft or ProductStatus.Unpublished))
        {
            throw InvalidProductState("The Product cannot be published from its current state.");
        }

        if (string.IsNullOrWhiteSpace(product.Description))
        {
            throw ApiException.Validation(new Dictionary<string, string[]>
            {
                ["description"] = ["A nonblank description is required before publishing."]
            });
        }

        var assetTypes = await db.ProductAssets.AsNoTracking()
            .Where(asset => asset.ProductId == product.Id)
            .Select(asset => asset.AssetType)
            .ToListAsync(cancellationToken);
        var present = assetTypes.ToHashSet();
        if (assetTypes.Count != RequiredAssetTypes.Length || RequiredAssetTypes.Any(type => !present.Contains(type)))
        {
            throw new ApiException(StatusCodes.Status409Conflict, "REQUIRED_DELIVERABLES_MISSING",
                "All four required protected deliverables must be attached before publishing.");
        }

        var now = DateTimeOffset.UtcNow;
        if (product.FirstPublishedAt is null)
        {
            product.FirstPublishedAt = now;
        }

        product.LastPublishedAt = now;
        product.Status = ProductStatus.Published;
        product.UpdatedAt = now;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return MapLifecycle(product, false);
    }

    public async Task<LifecycleActionResponse> UnpublishAsync(Guid productId, CancellationToken cancellationToken)
    {
        var expert = await RequireActiveExpertAsync(cancellationToken);
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetOwnedForUpdateAsync(productId, expert.Id, cancellationToken);

        if (product.Status == ProductStatus.Unpublished)
        {
            await transaction.CommitAsync(cancellationToken);
            return MapLifecycle(product, true);
        }

        if (product.Status != ProductStatus.Published)
        {
            throw InvalidProductState("Only a Published Product may be unpublished.");
        }

        product.Status = ProductStatus.Unpublished;
        product.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return MapLifecycle(product, false);
    }

    public async Task<PageResponse<MarketplaceProductListItemResponse>> BrowseAsync(
        BrowseProductsQuery request,
        CancellationToken cancellationToken)
    {
        var pagination = Pagination.Validate(request.Page, request.Size);
        var filters = ValidateBrowseFilters(request);
        IQueryable<AiProduct> query = db.AiProducts.AsNoTracking()
            .Where(product => product.Status == ProductStatus.Published);

        if (filters.Keyword is not null)
        {
            query = query.Where(product => EF.Functions.ILike(product.Name, $"%{filters.Keyword}%"));
        }

        if (filters.CategoryCode is not null)
        {
            query = query.Where(product => product.CategoryCode == filters.CategoryCode);
        }

        if (filters.MinPriceVnd is not null)
        {
            query = query.Where(product => product.PriceVnd >= filters.MinPriceVnd);
        }

        if (filters.MaxPriceVnd is not null)
        {
            query = query.Where(product => product.PriceVnd <= filters.MaxPriceVnd);
        }

        query = query.OrderByDescending(product => product.LastPublishedAt).ThenByDescending(product => product.Id);
        var total = await query.LongCountAsync(cancellationToken);
        var products = await query.Skip(pagination.Skip).Take(pagination.Size).ToListAsync(cancellationToken);
        var mapped = await Task.WhenAll(products.Select(product => MapMarketplaceListItemAsync(product, cancellationToken)));
        return Pagination.Create(mapped, pagination, total);
    }

    public async Task<MarketplaceProductDetailResponse> GetMarketplaceDetailAsync(Guid productId, CancellationToken cancellationToken)
    {
        var product = await db.AiProducts.AsNoTracking()
            .SingleOrDefaultAsync(item => item.Id == productId, cancellationToken);
        if (product is null)
        {
            throw new ApiException(StatusCodes.Status404NotFound, "PRODUCT_NOT_FOUND", "Product was not found.");
        }

        if (product.Status != ProductStatus.Published)
        {
            throw new ApiException(StatusCodes.Status409Conflict, "PRODUCT_NOT_PUBLISHED", "Product is not published.");
        }

        var sellerTask = identityDirectory.GetUserAsync(product.SellerExpertId, cancellationToken);
        var previewTask = MapPreviewWithAccessAsync(product, cancellationToken);
        await Task.WhenAll(sellerTask, previewTask);
        var seller = await sellerTask;
        return new MarketplaceProductDetailResponse(
            product.Id,
            product.Name,
            product.Description!,
            product.CategoryCode,
            product.Technologies,
            product.AiCapabilities,
            product.PriceVnd,
            await previewTask,
            product.LastPublishedAt!.Value,
            new BasicSellerSummaryResponse(seller.Id, seller.Code, seller.FullName, seller.AvatarUrl));
    }

    private async Task<IdentityUser> RequireActiveExpertAsync(CancellationToken cancellationToken)
    {
        var userId = currentUser.UserId;
        var identity = await identityDirectory.GetUserAsync(userId, cancellationToken);
        if (!string.Equals(identity.Role, "EXPERT", StringComparison.Ordinal) ||
            !string.Equals(identity.Status, "ACTIVE", StringComparison.Ordinal) ||
            string.IsNullOrWhiteSpace(identity.Code))
        {
            throw ApiException.Forbidden("An active Expert identity is required.");
        }

        return identity;
    }

    private async Task<AiProduct> GetOwnedForUpdateAsync(
        Guid productId,
        Guid expertId,
        CancellationToken cancellationToken)
    {
        var product = await db.AiProducts
            .FromSqlInterpolated($"SELECT * FROM ai_product WHERE id = {productId} FOR UPDATE")
            .SingleOrDefaultAsync(cancellationToken);
        if (product is not null)
        {
            EnsureOwned(product, expertId);
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

    private static void EnsureOwned(AiProduct? product, Guid expertId)
    {
        if (product is null)
        {
            throw new ApiException(StatusCodes.Status404NotFound, "PRODUCT_NOT_FOUND", "Product was not found.");
        }

        if (product.SellerExpertId != expertId)
        {
            throw new ApiException(StatusCodes.Status403Forbidden, "PRODUCT_NOT_OWNED",
                "This Product belongs to a different Expert.");
        }
    }

    private static ApiException InvalidProductState(string message) =>
        new(StatusCodes.Status409Conflict, "INVALID_PRODUCT_STATE", message);

    private static (string Name, string CategoryCode, decimal PriceVnd) ValidateCreate(CreateDraftProductRequest request)
    {
        var errors = new Dictionary<string, string[]>();
        var name = RequireTrimmed(request.Name, "name", errors);
        var category = RequireTrimmed(request.CategoryCode, "categoryCode", errors);
        ValidateWholePositiveVnd(request.PriceVnd, "priceVnd", errors);
        ThrowIfErrors(errors);
        return (name!, category!, request.PriceVnd);
    }

    private static ValidatedUpdate ValidateUpdate(UpdateProductRequest request)
    {
        var errors = new Dictionary<string, string[]>();
        var name = RequireTrimmed(request.Name, "name", errors);
        var category = RequireTrimmed(request.CategoryCode, "categoryCode", errors);
        ValidateWholePositiveVnd(request.PriceVnd, "priceVnd", errors);
        string? description = null;
        if (request.Description is not null)
        {
            description = request.Description.Trim();
            if (description.Length == 0)
            {
                errors["description"] = ["description cannot be blank when supplied."];
            }
        }

        var technologies = NormalizeArray(request.Technologies, "technologies", errors);
        var capabilities = NormalizeArray(request.AiCapabilities, "aiCapabilities", errors);
        ThrowIfErrors(errors);
        return new ValidatedUpdate(name!, description, category!, technologies!, capabilities!, request.PriceVnd);
    }

    private static BrowseFilters ValidateBrowseFilters(BrowseProductsQuery request)
    {
        var errors = new Dictionary<string, string[]>();
        string? keyword = null;
        if (request.Keyword is not null)
        {
            keyword = request.Keyword.Trim();
            if (keyword.Length == 0)
            {
                keyword = null;
            }
        }

        string? category = null;
        if (request.CategoryCode is not null)
        {
            category = request.CategoryCode.Trim();
            if (category.Length == 0)
            {
                errors["categoryCode"] = ["categoryCode cannot be blank when supplied."];
            }
        }

        ValidateWholeNonnegativeVnd(request.MinPriceVnd, "minPriceVnd", errors);
        ValidateWholeNonnegativeVnd(request.MaxPriceVnd, "maxPriceVnd", errors);
        if (request.MinPriceVnd is not null && request.MaxPriceVnd is not null && request.MinPriceVnd > request.MaxPriceVnd)
        {
            errors["minPriceVnd"] = ["minPriceVnd must be less than or equal to maxPriceVnd."];
        }

        ThrowIfErrors(errors);
        return new BrowseFilters(keyword, category, request.MinPriceVnd, request.MaxPriceVnd);
    }

    private static string? RequireTrimmed(string? value, string field, IDictionary<string, string[]> errors)
    {
        var normalized = value?.Trim();
        if (string.IsNullOrEmpty(normalized))
        {
            errors[field] = [$"{field} is required and cannot be blank."];
        }

        return normalized;
    }

    private static string[]? NormalizeArray(string[]? values, string field, IDictionary<string, string[]> errors)
    {
        if (values is null)
        {
            errors[field] = [$"{field} is required; use an empty array when there are no values."];
            return null;
        }

        if (values.Any(value => string.IsNullOrWhiteSpace(value)))
        {
            errors[field] = [$"{field} cannot contain blank values."];
            return null;
        }

        return values.Select(value => value.Trim())
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToArray();
    }

    private static void ValidateWholePositiveVnd(decimal value, string field, IDictionary<string, string[]> errors)
    {
        if (value <= 0 || decimal.Truncate(value) != value)
        {
            errors[field] = [$"{field} must be a positive whole-VND value."];
        }
    }

    private static void ValidateWholeNonnegativeVnd(decimal? value, string field, IDictionary<string, string[]> errors)
    {
        if (value is not null && (value < 0 || decimal.Truncate(value.Value) != value.Value))
        {
            errors[field] = [$"{field} must be a nonnegative whole-VND value."];
        }
    }

    private static void ThrowIfErrors(IReadOnlyDictionary<string, string[]> errors)
    {
        if (errors.Count > 0)
        {
            throw ApiException.Validation(errors);
        }
    }

    private static OwnerProductListItemResponse MapOwnerListItem(AiProduct product) =>
        new(product.Id, product.Name, product.CategoryCode, product.PriceVnd, ToApiValue(product.Status),
            MapPreview(product), product.UpdatedAt, product.LastPublishedAt);

    private static OwnerProductDetailResponse MapOwnerDetail(AiProduct product) =>
        new(product.Id, product.Name, product.Description, product.CategoryCode, product.Technologies,
            product.AiCapabilities, product.PriceVnd, ToApiValue(product.Status), MapPreview(product),
            product.UpdatedAt, product.LastPublishedAt, product.FirstPublishedAt, product.CreatedAt,
            product.Assets.OrderBy(asset => asset.AssetType).Select(MapAsset).ToList());

    private async Task<MarketplaceProductListItemResponse> MapMarketplaceListItemAsync(
        AiProduct product,
        CancellationToken cancellationToken) =>
        new(product.Id, product.Name, product.CategoryCode, product.PriceVnd, product.Technologies,
            product.AiCapabilities, await MapPreviewWithAccessAsync(product, cancellationToken), product.LastPublishedAt!.Value);

    private static LifecycleActionResponse MapLifecycle(AiProduct product, bool alreadyInTargetState) =>
        new(product.Id, ToApiValue(product.Status), product.FirstPublishedAt, product.LastPublishedAt, alreadyInTargetState);

    private static ProtectedAssetResponse MapAsset(ProductAsset asset) =>
        new(ToApiValue(asset.AssetType), asset.OriginalFilename, asset.ContentType, asset.ByteSize, asset.UploadedAt);

    private static PublicPreviewResponse? MapPreview(AiProduct product)
    {
        if (product.PreviewType is null || product.PreviewStorageBucket is null || product.PreviewStorageObjectKey is null ||
            product.PreviewOriginalFilename is null || product.PreviewContentType is null || product.PreviewByteSize is null ||
            product.PreviewUploadedAt is null)
        {
            return null;
        }

        return new PublicPreviewResponse(ToApiValue(product.PreviewType.Value), product.PreviewOriginalFilename,
            product.PreviewContentType, product.PreviewByteSize.Value, product.PreviewVideoDurationSeconds,
            product.PreviewUploadedAt.Value, null, null);
    }

    private async Task<PublicPreviewResponse?> MapPreviewWithAccessAsync(
        AiProduct product,
        CancellationToken cancellationToken)
    {
        var preview = MapPreview(product);
        if (preview is null)
        {
            return null;
        }

        try
        {
            var access = await objectStorage.CreateTemporaryReadAccessAsync(
                product.PreviewStorageBucket!, product.PreviewStorageObjectKey!, DateTimeOffset.UtcNow.AddMinutes(5),
                cancellationToken);
            return preview with { AccessUrl = access.AccessUri.ToString(), AccessUrlExpiresAt = access.ExpiresAt };
        }
        catch (StorageUnavailableException)
        {
            throw new ApiException(StatusCodes.Status503ServiceUnavailable, "STORAGE_UNAVAILABLE",
                "Object storage is temporarily unavailable.");
        }
    }

    private static string ToApiValue(ProductStatus status) => status switch
    {
        ProductStatus.Draft => "DRAFT",
        ProductStatus.Published => "PUBLISHED",
        ProductStatus.Unpublished => "UNPUBLISHED",
        _ => throw new ArgumentOutOfRangeException(nameof(status))
    };

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

    private sealed record ValidatedUpdate(
        string Name,
        string? Description,
        string CategoryCode,
        string[] Technologies,
        string[] AiCapabilities,
        decimal PriceVnd);

    private sealed record BrowseFilters(string? Keyword, string? CategoryCode, decimal? MinPriceVnd, decimal? MaxPriceVnd);
}
