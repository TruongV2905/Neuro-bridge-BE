using System.Globalization;
using Com.Payment.Grpc;
using Grpc.Core;

namespace AiAvailableService.Infrastructure.Payment;

public interface IPaymentSettlementGateway
{
    Task<PaymentSettlementResult> SettleAsync(
        PaymentSettlementRequest request,
        CancellationToken cancellationToken = default);

    Task<PaymentSettlementResult> LookupAsync(
        Guid productPurchaseId,
        CancellationToken cancellationToken = default);
}

public sealed record PaymentSettlementRequest(
    Guid ProductPurchaseId,
    Guid ProductId,
    string ClientCode,
    string ExpertCode,
    decimal GrossAmountVnd,
    PaymentSnapshotContext SnapshotContext);

public sealed record PaymentSnapshotContext(
    string ProductName,
    Guid SellerExpertId,
    string SellerExpertCode,
    string SellerDisplayName);

public enum PaymentSettlementStatus
{
    Success,
    InProgress,
    InsufficientBalance,
    WalletNotActive,
    PaymentFailed,
    NotFound,
    IdempotencyConflict
}

public sealed record PaymentSettlementResult(
    PaymentSettlementStatus Status,
    Guid ProductPurchaseId,
    Guid? ProductId,
    Guid? PaymentTransactionId,
    decimal? GrossAmountVnd,
    decimal? PlatformFeeAmountVnd,
    decimal? ExpertNetAmountVnd,
    decimal? ShortfallAmountVnd,
    DateTimeOffset? SettledAtUtc,
    PaymentSnapshotContext? SnapshotContext);

public sealed class PaymentServiceUnavailableException : Exception
{
    public PaymentServiceUnavailableException() : base("Payment service is temporarily unavailable.") { }
}

public sealed class PaymentSettlementGateway(
    PaymentService.PaymentServiceClient client,
    IInternalServiceTokenProvider serviceTokenProvider) : IPaymentSettlementGateway
{
    public async Task<PaymentSettlementResult> SettleAsync(
        PaymentSettlementRequest request,
        CancellationToken cancellationToken = default)
    {
        try
        {
            var response = await client.SettleAiProductPurchaseAsync(
                new AiProductSettlementRequest
                {
                    ProductPurchaseId = request.ProductPurchaseId.ToString(),
                    ProductId = request.ProductId.ToString(),
                    ClientCode = request.ClientCode,
                    ExpertCode = request.ExpertCode,
                    GrossAmountVnd = request.GrossAmountVnd.ToString("0", CultureInfo.InvariantCulture),
                    SnapshotContext = new AiProductSnapshotContext
                    {
                        ProductName = request.SnapshotContext.ProductName,
                        SellerExpertId = request.SnapshotContext.SellerExpertId.ToString(),
                        SellerExpertCode = request.SnapshotContext.SellerExpertCode,
                        SellerDisplayName = request.SnapshotContext.SellerDisplayName
                    }
                },
                headers: ServiceHeaders(),
                cancellationToken: cancellationToken).ResponseAsync;
            return Map(response);
        }
        catch (RpcException)
        {
            throw new PaymentServiceUnavailableException();
        }
    }

    public async Task<PaymentSettlementResult> LookupAsync(
        Guid productPurchaseId,
        CancellationToken cancellationToken = default)
    {
        try
        {
            var response = await client.GetAiProductPurchaseSettlementAsync(
                new AiProductSettlementLookupRequest { ProductPurchaseId = productPurchaseId.ToString() },
                headers: ServiceHeaders(),
                cancellationToken: cancellationToken).ResponseAsync;
            return Map(response);
        }
        catch (RpcException)
        {
            throw new PaymentServiceUnavailableException();
        }
    }

    private Metadata ServiceHeaders() => new()
    {
        { "authorization", $"Bearer {serviceTokenProvider.CreatePaymentServiceToken()}" }
    };

    private static PaymentSettlementResult Map(AiProductSettlementResponse response)
    {
        if (!Guid.TryParse(response.ProductPurchaseId, out var purchaseId))
        {
            throw new PaymentServiceUnavailableException();
        }

        var status = response.Status switch
        {
            AiProductSettlementStatus.AiProductSettlementSuccess => PaymentSettlementStatus.Success,
            AiProductSettlementStatus.AiProductSettlementInProgress => PaymentSettlementStatus.InProgress,
            AiProductSettlementStatus.AiProductSettlementInsufficientBalance => PaymentSettlementStatus.InsufficientBalance,
            AiProductSettlementStatus.AiProductSettlementWalletNotActive => PaymentSettlementStatus.WalletNotActive,
            AiProductSettlementStatus.AiProductSettlementPaymentFailed => PaymentSettlementStatus.PaymentFailed,
            AiProductSettlementStatus.AiProductSettlementNotFound => PaymentSettlementStatus.NotFound,
            AiProductSettlementStatus.AiProductSettlementIdempotencyConflict => PaymentSettlementStatus.IdempotencyConflict,
            _ => throw new PaymentServiceUnavailableException()
        };

        return new PaymentSettlementResult(
            status,
            purchaseId,
            ParseGuid(response.ProductId),
            ParseGuid(response.PaymentTransactionId),
            ParseDecimal(response.GrossAmountVnd),
            ParseDecimal(response.PlatformFeeAmountVnd),
            ParseDecimal(response.ExpertNetAmountVnd),
            ParseDecimal(response.ShortfallAmountVnd),
            ParseDateTimeOffset(response.SettledAtUtc),
            ParseSnapshot(response.SnapshotContext));
    }

    private static Guid? ParseGuid(string value) => string.IsNullOrWhiteSpace(value)
        ? null
        : Guid.TryParse(value, out var parsed) ? parsed : throw new PaymentServiceUnavailableException();

    private static decimal? ParseDecimal(string value) => string.IsNullOrWhiteSpace(value)
        ? null
        : decimal.TryParse(value, NumberStyles.Number, CultureInfo.InvariantCulture, out var parsed)
            ? parsed
            : throw new PaymentServiceUnavailableException();

    private static DateTimeOffset? ParseDateTimeOffset(string value) => string.IsNullOrWhiteSpace(value)
        ? null
        : DateTimeOffset.TryParse(value, CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out var parsed)
            ? parsed.ToUniversalTime()
            : throw new PaymentServiceUnavailableException();

    private static PaymentSnapshotContext? ParseSnapshot(AiProductSnapshotContext context)
    {
        if (string.IsNullOrWhiteSpace(context.ProductName)
            && string.IsNullOrWhiteSpace(context.SellerExpertId)
            && string.IsNullOrWhiteSpace(context.SellerExpertCode)
            && string.IsNullOrWhiteSpace(context.SellerDisplayName))
        {
            return null;
        }

        if (!Guid.TryParse(context.SellerExpertId, out var sellerId)
            || string.IsNullOrWhiteSpace(context.ProductName)
            || string.IsNullOrWhiteSpace(context.SellerExpertCode)
            || string.IsNullOrWhiteSpace(context.SellerDisplayName))
        {
            throw new PaymentServiceUnavailableException();
        }

        return new PaymentSnapshotContext(
            context.ProductName,
            sellerId,
            context.SellerExpertCode,
            context.SellerDisplayName);
    }
}
