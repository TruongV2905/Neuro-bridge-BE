using AiAvailableService.Api;
using AiAvailableService.Contracts;
using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using AiAvailableService.Infrastructure.Authentication;
using AiAvailableService.Infrastructure.Persistence;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;

namespace AiAvailableService.Application;

public interface IPurchaseReadApplicationService
{
    Task<PageResponse<PurchaseHistoryItemResponse>> ListAsync(int page, int size, CancellationToken cancellationToken);
    Task<PurchaseDetailResponse> GetDetailAsync(Guid purchaseId, CancellationToken cancellationToken);
}

public sealed class PurchaseReadApplicationService(
    AiAvailableDbContext db,
    ICurrentUser currentUser) : IPurchaseReadApplicationService
{
    public async Task<PageResponse<PurchaseHistoryItemResponse>> ListAsync(
        int page,
        int size,
        CancellationToken cancellationToken)
    {
        var pagination = Pagination.Validate(page, size);
        var buyerId = currentUser.UserId;
        var query = db.ProductPurchases.AsNoTracking()
            .Where(purchase => purchase.BuyerClientId == buyerId && purchase.Status == PurchaseStatus.Success)
            .OrderByDescending(purchase => purchase.PurchasedAt)
            .ThenByDescending(purchase => purchase.Id);
        var total = await query.LongCountAsync(cancellationToken);
        var purchases = await query.Skip(pagination.Skip).Take(pagination.Size).ToListAsync(cancellationToken);
        var now = DateTimeOffset.UtcNow;
        return Pagination.Create(purchases.Select(purchase => MapHistory(purchase, now)).ToList(), pagination, total);
    }

    public async Task<PurchaseDetailResponse> GetDetailAsync(Guid purchaseId, CancellationToken cancellationToken)
    {
        var buyerId = currentUser.UserId;
        var purchase = await db.ProductPurchases.AsNoTracking()
            .Include(item => item.Product)
                .ThenInclude(product => product.Assets)
            .Include(item => item.Rating)
            .SingleOrDefaultAsync(item => item.Id == purchaseId, cancellationToken);
        if (purchase is null)
        {
            throw PurchaseNotFound();
        }

        if (purchase.BuyerClientId != buyerId)
        {
            throw ApiException.Forbidden();
        }

        if (purchase.Status != PurchaseStatus.Success)
        {
            throw PurchaseNotFound();
        }

        var now = DateTimeOffset.UtcNow;
        var isActive = IsEntitlementActive(purchase, now);
        return new PurchaseDetailResponse(
            purchase.Id,
            purchase.ProductId,
            purchase.ProductNameAtPurchase!,
            purchase.SellerExpertIdAtPurchase!.Value,
            purchase.SellerExpertCodeAtPurchase!,
            purchase.SellerDisplayNameAtPurchase!,
            purchase.PriceVndAtPurchase!.Value,
            purchase.PlatformFeeVndAtPurchase!.Value,
            purchase.ExpertNetVndAtPurchase!.Value,
            purchase.PaymentTransactionId!.Value,
            purchase.PurchasedAt!.Value,
            purchase.AccessExpiresAt!.Value,
            isActive ? "ACTIVE" : "EXPIRED",
            isActive,
            isActive
                ? purchase.Product.Assets.OrderBy(asset => asset.AssetType).Select(asset => ToApiValue(asset.AssetType)).ToList()
                : Array.Empty<string>(),
            purchase.Rating is null ? null : MapRating(purchase.Rating));
    }

    private static PurchaseHistoryItemResponse MapHistory(ProductPurchase purchase, DateTimeOffset now) =>
        new(
            purchase.Id,
            purchase.ProductId,
            purchase.ProductNameAtPurchase!,
            purchase.SellerExpertIdAtPurchase!.Value,
            purchase.SellerExpertCodeAtPurchase!,
            purchase.SellerDisplayNameAtPurchase!,
            purchase.PriceVndAtPurchase!.Value,
            purchase.PlatformFeeVndAtPurchase!.Value,
            purchase.ExpertNetVndAtPurchase!.Value,
            purchase.PurchasedAt!.Value,
            purchase.AccessExpiresAt!.Value,
            IsEntitlementActive(purchase, now) ? "ACTIVE" : "EXPIRED");

    private static bool IsEntitlementActive(ProductPurchase purchase, DateTimeOffset now) =>
        purchase.AccessExpiresAt is not null && now < purchase.AccessExpiresAt.Value;

    private static ProductRatingResponse MapRating(ProductRating rating) =>
        new(rating.Id, rating.Score, rating.ReviewText, rating.CreatedAt, rating.UpdatedAt);

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
}
