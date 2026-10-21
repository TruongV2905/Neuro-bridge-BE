using AiAvailableService.Api;
using AiAvailableService.Contracts;
using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using AiAvailableService.Infrastructure.Authentication;
using AiAvailableService.Infrastructure.Identity;
using AiAvailableService.Infrastructure.Kafka;
using AiAvailableService.Infrastructure.Payment;
using AiAvailableService.Infrastructure.Persistence;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Npgsql;

namespace AiAvailableService.Application;

public interface IPurchasePaymentApplicationService
{
    Task<PurchaseSettlementResponse> PurchaseAsync(Guid productId, CancellationToken cancellationToken);
    Task<PurchaseSettlementResponse> RetryAsync(Guid purchaseId, CancellationToken cancellationToken);
}

/// <summary>
/// Owns only AI Available's durable Purchase intent and terminal snapshot. The
/// Payment service owns money movement and its correlation ledger. No EF
/// transaction remains open while Identity or Payment gRPC is awaited.
/// </summary>
public sealed class PurchasePaymentApplicationService(
    AiAvailableDbContext db,
    ICurrentUser currentUser,
    IIdentityDirectory identityDirectory,
    IPaymentSettlementGateway paymentGateway,
    IAiProductPurchaseEventPublisher purchaseEventPublisher,
    ILogger<PurchasePaymentApplicationService> logger) : IPurchasePaymentApplicationService
{
    public async Task<PurchaseSettlementResponse> PurchaseAsync(
        Guid productId,
        CancellationToken cancellationToken)
    {
        var buyer = await RequireActiveClientAsync(cancellationToken);
        var intent = await GetOrCreateIntentAsync(productId, buyer, cancellationToken);
        if (intent.IsSuccess)
        {
            throw PurchaseAlreadyOwned(intent.PurchaseId);
        }

        try
        {
            if (intent.WasExistingIntent)
            {
                return await ReconcileThenSettleAsync(intent.PurchaseId, buyer, true, cancellationToken);
            }

            return await PrepareAndSettleAsync(intent.PurchaseId, buyer, false, cancellationToken);
        }
        catch (PaymentServiceUnavailableException)
        {
            throw PaymentUnavailable();
        }
    }

    public async Task<PurchaseSettlementResponse> RetryAsync(
        Guid purchaseId,
        CancellationToken cancellationToken)
    {
        var buyer = await RequireActiveClientAsync(cancellationToken);
        var purchase = await GetOwnedPurchaseAsync(purchaseId, buyer.Id, cancellationToken);
        if (purchase.Status == PurchaseStatus.Success)
        {
            throw PurchaseAlreadyOwned(purchase.Id);
        }

        try
        {
            return await ReconcileThenSettleAsync(purchase.Id, buyer, true, cancellationToken);
        }
        catch (PaymentServiceUnavailableException)
        {
            throw PaymentUnavailable();
        }
    }

    private async Task<PurchaseSettlementResponse> ReconcileThenSettleAsync(
        Guid purchaseId,
        IdentityUser buyer,
        bool wasExistingIntent,
        CancellationToken cancellationToken)
    {
        var reconciliation = await paymentGateway.LookupAsync(purchaseId, cancellationToken);
        EnsureCorrelation(reconciliation, purchaseId);

        return reconciliation.Status switch
        {
            PaymentSettlementStatus.Success => await CompleteSuccessAsync(
                purchaseId, reconciliation, buyer, wasExistingIntent, cancellationToken),
            PaymentSettlementStatus.InProgress => throw new ApiException(
                StatusCodes.Status409Conflict, "PAYMENT_IN_PROGRESS",
                "Payment settlement is still being processed."),
            PaymentSettlementStatus.NotFound
                or PaymentSettlementStatus.InsufficientBalance
                or PaymentSettlementStatus.WalletNotActive
                or PaymentSettlementStatus.PaymentFailed => await PrepareAndSettleAsync(
                    purchaseId, buyer, wasExistingIntent, cancellationToken),
            PaymentSettlementStatus.IdempotencyConflict => throw new ApiException(
                StatusCodes.Status409Conflict, "PAYMENT_IDEMPOTENCY_CONFLICT",
                "Payment settlement conflicts with an authoritative settlement."),
            _ => throw PaymentUnavailable()
        };
    }

    private async Task<PurchaseSettlementResponse> PrepareAndSettleAsync(
        Guid purchaseId,
        IdentityUser buyer,
        bool wasExistingIntent,
        CancellationToken cancellationToken)
    {
        // This short transaction revalidates the purchasable Product and reads
        // its current price. It is committed before the Identity/Payment calls.
        var preparation = await PrepareAttemptAsync(purchaseId, buyer.Id, cancellationToken);
        var seller = await RequireCurrentSellerAsync(preparation, cancellationToken);
        var remote = await paymentGateway.SettleAsync(new PaymentSettlementRequest(
            preparation.PurchaseId,
            preparation.ProductId,
            buyer.Code,
            seller.Code,
            preparation.GrossPriceVnd,
            new PaymentSnapshotContext(
                preparation.ProductName,
                seller.Id,
                seller.Code,
                seller.FullName)), cancellationToken);
        EnsureCorrelation(remote, purchaseId);
        return await HandleSettlementResultAsync(remote, buyer, wasExistingIntent, cancellationToken);
    }

    private async Task<PurchaseSettlementResponse> HandleSettlementResultAsync(
        PaymentSettlementResult remote,
        IdentityUser buyer,
        bool wasExistingIntent,
        CancellationToken cancellationToken)
    {
        return remote.Status switch
        {
            PaymentSettlementStatus.Success => await CompleteSuccessAsync(
                remote.ProductPurchaseId, remote, buyer, wasExistingIntent, cancellationToken),
            PaymentSettlementStatus.InsufficientBalance => throw InsufficientBalance(remote),
            PaymentSettlementStatus.WalletNotActive => throw new ApiException(
                StatusCodes.Status409Conflict, "WALLET_NOT_ACTIVE",
                "A required wallet is not active. The Purchase remains pending."),
            PaymentSettlementStatus.InProgress => throw new ApiException(
                StatusCodes.Status409Conflict, "PAYMENT_IN_PROGRESS",
                "Payment settlement is still being processed."),
            PaymentSettlementStatus.IdempotencyConflict => throw new ApiException(
                StatusCodes.Status409Conflict, "PAYMENT_IDEMPOTENCY_CONFLICT",
                "Payment settlement conflicts with an authoritative settlement."),
            PaymentSettlementStatus.PaymentFailed => throw new ApiException(
                StatusCodes.Status502BadGateway, "PAYMENT_FAILED",
                "Payment did not complete. The Purchase remains pending and may be retried."),
            PaymentSettlementStatus.NotFound => throw PaymentUnavailable(),
            _ => throw PaymentUnavailable()
        };
    }

    private async Task<PurchaseSettlementResponse> CompleteSuccessAsync(
        Guid purchaseId,
        PaymentSettlementResult remote,
        IdentityUser buyer,
        bool wasExistingIntent,
        CancellationToken cancellationToken)
    {
        ValidateSuccessResult(remote, purchaseId);

        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var purchase = await GetPurchaseForUpdateAsync(purchaseId, cancellationToken)
            ?? throw PurchaseNotFound();
        if (purchase.Status == PurchaseStatus.Success)
        {
            await transaction.CommitAsync(cancellationToken);
            return MapSuccess(purchase, wasExistingIntent);
        }

        if (remote.ProductId != purchase.ProductId)
        {
            throw PaymentUnavailable();
        }

        var snapshot = remote.SnapshotContext!;
        var purchasedAt = remote.SettledAtUtc!.Value;
        purchase.Status = PurchaseStatus.Success;
        purchase.PaymentTransactionId = remote.PaymentTransactionId;
        purchase.ProductNameAtPurchase = snapshot.ProductName;
        purchase.SellerExpertIdAtPurchase = snapshot.SellerExpertId;
        purchase.SellerExpertCodeAtPurchase = snapshot.SellerExpertCode;
        purchase.SellerDisplayNameAtPurchase = snapshot.SellerDisplayName;
        purchase.PriceVndAtPurchase = remote.GrossAmountVnd;
        purchase.PlatformFeeVndAtPurchase = remote.PlatformFeeAmountVnd;
        purchase.ExpertNetVndAtPurchase = remote.ExpertNetAmountVnd;
        purchase.PurchasedAt = purchasedAt;
        purchase.AccessExpiresAt = purchasedAt.AddDays(30);
        purchase.UpdatedAt = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);

        // Kafka is deliberately outside the local DB transaction. A transport
        // failure cannot undo an authoritative Payment/Purchase success.
        await PublishPurchaseSuccessBestEffortAsync(purchase, buyer, CancellationToken.None);
        return MapSuccess(purchase, wasExistingIntent);
    }

    private async Task PublishPurchaseSuccessBestEffortAsync(
        ProductPurchase purchase,
        IdentityUser buyer,
        CancellationToken cancellationToken)
    {
        var @event = new AiProductPurchasedEvent(
            AiProductPurchasedEvent.CreateDeterministicEventId(purchase.Id),
            AiProductPurchasedEvent.Type,
            AiProductPurchasedEvent.Version,
            DateTimeOffset.UtcNow,
            purchase.Id,
            purchase.ProductId,
            purchase.ProductNameAtPurchase!,
            purchase.PurchasedAt!.Value.ToUniversalTime(),
            purchase.AccessExpiresAt!.Value.ToUniversalTime(),
            purchase.BuyerClientId,
            purchase.BuyerClientCode,
            buyer.Email,
            purchase.SellerExpertIdAtPurchase!.Value,
            purchase.SellerExpertCodeAtPurchase!,
            purchase.SellerDisplayNameAtPurchase!,
            purchase.PriceVndAtPurchase!.Value,
            purchase.PlatformFeeVndAtPurchase!.Value,
            purchase.ExpertNetVndAtPurchase!.Value);

        try
        {
            await purchaseEventPublisher.PublishAsync(@event, cancellationToken);
        }
        catch (Exception ex)
        {
            // The publisher is required to absorb errors itself. This second
            // guard preserves the post-commit success response if a future
            // implementation violates that contract.
            logger.LogWarning(ex,
                "AI Product purchase event publish failed best-effort after committed Purchase: EventId={EventId} PurchaseId={PurchaseId} ProductId={ProductId}",
                @event.EventId, @event.PurchaseId, @event.ProductId);
        }
    }

    private async Task<PurchaseIntent> GetOrCreateIntentAsync(
        Guid productId,
        IdentityUser buyer,
        CancellationToken cancellationToken)
    {
        // The unique database constraint is final protection. A narrow retry
        // reloads the winning intent if two initial browser requests race.
        for (var attempt = 0; attempt < 2; attempt++)
        {
            try
            {
                await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
                var product = await GetProductForUpdateAsync(productId, cancellationToken)
                    ?? throw ProductNotFound();
                var purchase = await db.ProductPurchases
                    .FromSqlInterpolated($"SELECT * FROM product_purchase WHERE product_id = {productId} AND buyer_client_id = {buyer.Id} FOR UPDATE")
                    .SingleOrDefaultAsync(cancellationToken);
                if (purchase is not null)
                {
                    if (purchase.Status != PurchaseStatus.Success)
                    {
                        EnsurePurchasable(product, buyer.Id);
                    }
                    await transaction.CommitAsync(cancellationToken);
                    return new PurchaseIntent(purchase.Id, true, purchase.Status == PurchaseStatus.Success);
                }

                EnsurePurchasable(product, buyer.Id);
                var now = DateTimeOffset.UtcNow;
                purchase = new ProductPurchase
                {
                    Id = Guid.NewGuid(),
                    ProductId = product.Id,
                    BuyerClientId = buyer.Id,
                    BuyerClientCode = buyer.Code,
                    Status = PurchaseStatus.PendingPayment,
                    CreatedAt = now,
                    UpdatedAt = now
                };
                db.ProductPurchases.Add(purchase);
                await db.SaveChangesAsync(cancellationToken);
                await transaction.CommitAsync(cancellationToken);
                return new PurchaseIntent(purchase.Id, false, false);
            }
            catch (DbUpdateException exception) when (IsPurchaseUniqueViolation(exception) && attempt == 0)
            {
                db.ChangeTracker.Clear();
            }
        }

        throw new ApiException(StatusCodes.Status409Conflict, "PURCHASE_ALREADY_EXISTS",
            "A Purchase intent already exists for this Product.");
    }

    private async Task<SettlementPreparation> PrepareAttemptAsync(
        Guid purchaseId,
        Guid buyerId,
        CancellationToken cancellationToken)
    {
        // Product first, then Purchase is the deterministic local lock order.
        var existing = await db.ProductPurchases.AsNoTracking()
            .SingleOrDefaultAsync(item => item.Id == purchaseId, cancellationToken)
            ?? throw PurchaseNotFound();
        if (existing.BuyerClientId != buyerId)
        {
            throw ApiException.Forbidden();
        }

        await using var transaction = await db.Database.BeginTransactionAsync(cancellationToken);
        var product = await GetProductForUpdateAsync(existing.ProductId, cancellationToken)
            ?? throw ProductNotFound();
        var purchase = await GetPurchaseForUpdateAsync(purchaseId, cancellationToken)
            ?? throw PurchaseNotFound();
        if (purchase.BuyerClientId != buyerId)
        {
            throw ApiException.Forbidden();
        }
        if (purchase.Status == PurchaseStatus.Success)
        {
            throw PurchaseAlreadyOwned(purchase.Id);
        }

        EnsurePurchasable(product, buyerId);
        var preparation = new SettlementPreparation(
            purchase.Id,
            product.Id,
            product.Name,
            product.PriceVnd,
            product.SellerExpertId,
            product.SellerExpertCode);
        await transaction.CommitAsync(cancellationToken);
        return preparation;
    }

    private async Task<IdentityUser> RequireActiveClientAsync(CancellationToken cancellationToken)
    {
        var identity = await identityDirectory.GetUserAsync(currentUser.UserId, cancellationToken);
        if (!string.Equals(identity.Role, "CLIENT", StringComparison.Ordinal)
            || !string.Equals(identity.Status, "ACTIVE", StringComparison.Ordinal)
            || string.IsNullOrWhiteSpace(identity.Code))
        {
            throw ApiException.Forbidden("An active Client identity is required.");
        }
        return identity;
    }

    private async Task<IdentityUser> RequireCurrentSellerAsync(
        SettlementPreparation preparation,
        CancellationToken cancellationToken)
    {
        var seller = await identityDirectory.GetUserAsync(preparation.SellerExpertId, cancellationToken);
        if (seller.Id != preparation.SellerExpertId
            || !string.Equals(seller.Role, "EXPERT", StringComparison.Ordinal)
            || string.IsNullOrWhiteSpace(seller.Code)
            || !string.Equals(seller.Code, preparation.SellerExpertCode, StringComparison.Ordinal)
            || string.IsNullOrWhiteSpace(seller.FullName))
        {
            throw new ApiException(StatusCodes.Status409Conflict, "SELLER_IDENTITY_INVALID",
                "The Product seller is not currently eligible for settlement.");
        }
        return seller;
    }

    private async Task<ProductPurchase> GetOwnedPurchaseAsync(
        Guid purchaseId,
        Guid buyerId,
        CancellationToken cancellationToken)
    {
        var purchase = await db.ProductPurchases.AsNoTracking()
            .SingleOrDefaultAsync(item => item.Id == purchaseId, cancellationToken)
            ?? throw PurchaseNotFound();
        if (purchase.BuyerClientId != buyerId)
        {
            throw ApiException.Forbidden();
        }
        return purchase;
    }

    private Task<AiProduct?> GetProductForUpdateAsync(Guid productId, CancellationToken cancellationToken) =>
        db.AiProducts.FromSqlInterpolated($"SELECT * FROM ai_product WHERE id = {productId} FOR UPDATE")
            .SingleOrDefaultAsync(cancellationToken);

    private Task<ProductPurchase?> GetPurchaseForUpdateAsync(Guid purchaseId, CancellationToken cancellationToken) =>
        db.ProductPurchases.FromSqlInterpolated($"SELECT * FROM product_purchase WHERE id = {purchaseId} FOR UPDATE")
            .SingleOrDefaultAsync(cancellationToken);

    private static void EnsurePurchasable(AiProduct product, Guid buyerId)
    {
        if (product.Status != ProductStatus.Published)
        {
            throw ProductNotPublished();
        }
        if (product.SellerExpertId == buyerId)
        {
            throw new ApiException(StatusCodes.Status409Conflict, "SELF_PURCHASE_NOT_ALLOWED",
                "A Client cannot purchase their own Product.");
        }
    }

    private static void EnsureCorrelation(PaymentSettlementResult result, Guid purchaseId)
    {
        if (result.ProductPurchaseId != purchaseId)
        {
            throw PaymentUnavailable();
        }
    }

    private static void ValidateSuccessResult(PaymentSettlementResult remote, Guid purchaseId)
    {
        if (remote.Status != PaymentSettlementStatus.Success
            || remote.ProductPurchaseId != purchaseId
            || remote.ProductId is null
            || remote.PaymentTransactionId is null
            || remote.GrossAmountVnd is null
            || remote.PlatformFeeAmountVnd is null
            || remote.ExpertNetAmountVnd is null
            || remote.SettledAtUtc is null
            || remote.SnapshotContext is null
            || remote.GrossAmountVnd <= 0
            || decimal.Truncate(remote.GrossAmountVnd.Value) != remote.GrossAmountVnd
            || decimal.Truncate(remote.PlatformFeeAmountVnd.Value) != remote.PlatformFeeAmountVnd
            || decimal.Truncate(remote.ExpertNetAmountVnd.Value) != remote.ExpertNetAmountVnd)
        {
            throw PaymentUnavailable();
        }

        var expectedFee = decimal.Round(remote.GrossAmountVnd.Value * 0.10m, 0, MidpointRounding.AwayFromZero);
        if (remote.PlatformFeeAmountVnd != expectedFee
            || remote.ExpertNetAmountVnd != remote.GrossAmountVnd - remote.PlatformFeeAmountVnd
            || string.IsNullOrWhiteSpace(remote.SnapshotContext.ProductName)
            || string.IsNullOrWhiteSpace(remote.SnapshotContext.SellerExpertCode)
            || string.IsNullOrWhiteSpace(remote.SnapshotContext.SellerDisplayName))
        {
            throw PaymentUnavailable();
        }
    }

    private static ApiException InsufficientBalance(PaymentSettlementResult result) =>
        new(StatusCodes.Status409Conflict, "PAYMENT_INSUFFICIENT_BALANCE",
            "Wallet balance is insufficient. The Purchase remains pending.", details: new
            {
                purchaseId = result.ProductPurchaseId,
                productId = result.ProductId,
                purchaseStatus = "PENDING_PAYMENT",
                grossAmountVnd = result.GrossAmountVnd,
                shortfallAmountVnd = result.ShortfallAmountVnd,
                topUpRequired = true,
                retryAllowed = true
            });

    private static ApiException ProductNotFound() =>
        new(StatusCodes.Status404NotFound, "PRODUCT_NOT_FOUND", "Product was not found.");

    private static ApiException ProductNotPublished() =>
        new(StatusCodes.Status409Conflict, "PRODUCT_NOT_PUBLISHED", "Product is not published.");

    private static ApiException PurchaseNotFound() =>
        new(StatusCodes.Status404NotFound, "PURCHASE_NOT_FOUND", "Purchase was not found.");

    private static ApiException PurchaseAlreadyOwned(Guid purchaseId) =>
        new(StatusCodes.Status409Conflict, "PURCHASE_ALREADY_OWNED",
            "This Product has already been purchased.", details: new { purchaseId });

    private static ApiException PaymentUnavailable() =>
        new(StatusCodes.Status503ServiceUnavailable, "PAYMENT_SERVICE_UNAVAILABLE",
            "Payment service is temporarily unavailable.");

    private static PurchaseSettlementResponse MapSuccess(ProductPurchase purchase, bool wasExistingIntent) =>
        new(
            purchase.Id,
            purchase.ProductId,
            "SUCCESS",
            wasExistingIntent,
            purchase.PaymentTransactionId!.Value,
            purchase.ProductNameAtPurchase!,
            purchase.SellerExpertIdAtPurchase!.Value,
            purchase.SellerExpertCodeAtPurchase!,
            purchase.SellerDisplayNameAtPurchase!,
            purchase.PriceVndAtPurchase!.Value,
            purchase.PlatformFeeVndAtPurchase!.Value,
            purchase.ExpertNetVndAtPurchase!.Value,
            purchase.PurchasedAt!.Value,
            purchase.AccessExpiresAt!.Value);

    private static bool IsPurchaseUniqueViolation(DbUpdateException exception) =>
        exception.GetBaseException() is PostgresException { SqlState: PostgresErrorCodes.UniqueViolation };

    private sealed record PurchaseIntent(Guid PurchaseId, bool WasExistingIntent, bool IsSuccess);

    private sealed record SettlementPreparation(
        Guid PurchaseId,
        Guid ProductId,
        string ProductName,
        decimal GrossPriceVnd,
        Guid SellerExpertId,
        string SellerExpertCode);
}
