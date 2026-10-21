using AiAvailableService.Api;
using AiAvailableService.Contracts;
using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using AiAvailableService.Infrastructure.Authentication;
using AiAvailableService.Infrastructure.Persistence;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Npgsql;

namespace AiAvailableService.Application;

public interface IProductRatingApplicationService
{
    Task<PageResponse<ProductRatingResponse>> ListAsync(Guid productId, int page, int size, CancellationToken cancellationToken);
    Task<ProductRatingSummaryResponse> GetSummaryAsync(Guid productId, CancellationToken cancellationToken);
    Task<ProductRatingResponse> CreateAsync(Guid purchaseId, CreateProductRatingRequest request, CancellationToken cancellationToken);
    Task<ProductRatingResponse> UpdateAsync(Guid purchaseId, UpdateProductRatingRequest request, CancellationToken cancellationToken);
    Task DeleteAsync(Guid purchaseId, CancellationToken cancellationToken);
}

public sealed class ProductRatingApplicationService(
    AiAvailableDbContext db,
    ICurrentUser currentUser) : IProductRatingApplicationService
{
    public async Task<PageResponse<ProductRatingResponse>> ListAsync(
        Guid productId,
        int page,
        int size,
        CancellationToken cancellationToken)
    {
        var pagination = Pagination.Validate(page, size);
        await EnsurePublishedProductAsync(productId, cancellationToken);
        var query = db.ProductRatings.AsNoTracking()
            .Where(rating => rating.Purchase.ProductId == productId && rating.Purchase.Status == PurchaseStatus.Success)
            .OrderByDescending(rating => rating.CreatedAt)
            .ThenByDescending(rating => rating.Id);
        var total = await query.LongCountAsync(cancellationToken);
        var ratings = await query.Skip(pagination.Skip).Take(pagination.Size).ToListAsync(cancellationToken);
        return Pagination.Create(ratings.Select(Map).ToList(), pagination, total);
    }

    public async Task<ProductRatingSummaryResponse> GetSummaryAsync(Guid productId, CancellationToken cancellationToken)
    {
        await EnsurePublishedProductAsync(productId, cancellationToken);
        var query = db.ProductRatings.AsNoTracking()
            .Where(rating => rating.Purchase.ProductId == productId && rating.Purchase.Status == PurchaseStatus.Success);
        var count = await query.LongCountAsync(cancellationToken);
        var average = await query.Select(rating => (decimal?)rating.Score).AverageAsync(cancellationToken);
        return new ProductRatingSummaryResponse(productId, count, average);
    }

    public async Task<ProductRatingResponse> CreateAsync(
        Guid purchaseId,
        CreateProductRatingRequest request,
        CancellationToken cancellationToken)
    {
        var input = Validate(request.Score, request.ReviewText);
        var buyerId = currentUser.UserId;
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var purchase = await GetOwnedSuccessfulPurchaseForUpdateAsync(purchaseId, buyerId, cancellationToken);
        var existing = await db.ProductRatings.SingleOrDefaultAsync(rating => rating.PurchaseId == purchase.Id, cancellationToken);
        if (existing is not null)
        {
            throw RatingAlreadyExists();
        }

        var now = DateTimeOffset.UtcNow;
        var rating = new ProductRating
        {
            Id = Guid.NewGuid(),
            PurchaseId = purchase.Id,
            Score = input.Score,
            ReviewText = input.ReviewText,
            CreatedAt = now,
            UpdatedAt = now
        };
        db.ProductRatings.Add(rating);
        try
        {
            await db.SaveChangesAsync(cancellationToken);
        }
        catch (DbUpdateException exception) when (IsUniqueViolation(exception))
        {
            throw RatingAlreadyExists();
        }

        await transaction.CommitAsync(cancellationToken);
        return Map(rating);
    }

    public async Task<ProductRatingResponse> UpdateAsync(
        Guid purchaseId,
        UpdateProductRatingRequest request,
        CancellationToken cancellationToken)
    {
        var input = Validate(request.Score, request.ReviewText);
        var buyerId = currentUser.UserId;
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var purchase = await GetOwnedSuccessfulPurchaseForUpdateAsync(purchaseId, buyerId, cancellationToken);
        var rating = await db.ProductRatings.SingleOrDefaultAsync(item => item.PurchaseId == purchase.Id, cancellationToken);
        if (rating is null)
        {
            throw RatingNotAllowed("No active rating exists for this Purchase.");
        }

        rating.Score = input.Score;
        rating.ReviewText = input.ReviewText;
        rating.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return Map(rating);
    }

    public async Task DeleteAsync(Guid purchaseId, CancellationToken cancellationToken)
    {
        var buyerId = currentUser.UserId;
        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var purchase = await GetOwnedSuccessfulPurchaseForUpdateAsync(purchaseId, buyerId, cancellationToken);
        var rating = await db.ProductRatings.SingleOrDefaultAsync(item => item.PurchaseId == purchase.Id, cancellationToken);
        if (rating is null)
        {
            throw RatingNotAllowed("No active rating exists for this Purchase.");
        }

        db.ProductRatings.Remove(rating);
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
    }

    private async Task EnsurePublishedProductAsync(Guid productId, CancellationToken cancellationToken)
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
    }

    private async Task<ProductPurchase> GetOwnedSuccessfulPurchaseForUpdateAsync(
        Guid purchaseId,
        Guid buyerId,
        CancellationToken cancellationToken)
    {
        var purchase = await db.ProductPurchases
            .FromSqlInterpolated($"SELECT * FROM product_purchase WHERE id = {purchaseId} FOR UPDATE")
            .SingleOrDefaultAsync(cancellationToken);
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
            throw RatingNotAllowed("Only a successful Purchase may be rated.");
        }

        return purchase;
    }

    private static ValidatedRating Validate(short score, string? reviewText)
    {
        var errors = new Dictionary<string, string[]>();
        if (score is < 1 or > 5)
        {
            errors["score"] = ["score must be an integer between 1 and 5."];
        }

        string? normalizedReview = null;
        if (reviewText is not null)
        {
            normalizedReview = reviewText.Trim();
            if (normalizedReview.Length == 0)
            {
                errors["reviewText"] = ["reviewText cannot be blank when supplied."];
            }
        }

        if (errors.Count > 0)
        {
            throw ApiException.Validation(errors);
        }

        return new ValidatedRating(score, normalizedReview);
    }

    private static ProductRatingResponse Map(ProductRating rating) =>
        new(rating.Id, rating.Score, rating.ReviewText, rating.CreatedAt, rating.UpdatedAt);

    private static bool IsUniqueViolation(DbUpdateException exception) =>
        exception.InnerException is PostgresException { SqlState: PostgresErrorCodes.UniqueViolation };

    private static ApiException PurchaseNotFound() =>
        new(StatusCodes.Status404NotFound, "PURCHASE_NOT_FOUND", "Purchase was not found.");

    private static ApiException RatingNotAllowed(string message) =>
        new(StatusCodes.Status403Forbidden, "RATING_NOT_ALLOWED", message);

    private static ApiException RatingAlreadyExists() =>
        new(StatusCodes.Status409Conflict, "RATING_ALREADY_EXISTS", "A rating already exists for this Purchase.");

    private sealed record ValidatedRating(short Score, string? ReviewText);
}
