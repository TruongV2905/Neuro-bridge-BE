namespace AiAvailableService.Contracts;

public sealed record PurchaseSettlementResponse(
    Guid PurchaseId,
    Guid ProductId,
    string PurchaseStatus,
    bool WasExistingIntent,
    Guid PaymentTransactionId,
    string ProductNameAtPurchase,
    Guid SellerExpertId,
    string SellerExpertCode,
    string SellerDisplayName,
    decimal GrossPriceVnd,
    decimal PlatformFeeVnd,
    decimal ExpertNetVnd,
    DateTimeOffset PurchasedAt,
    DateTimeOffset AccessExpiresAt);

public sealed record PurchaseHistoryItemResponse(
    Guid PurchaseId,
    Guid ProductId,
    string ProductNameAtPurchase,
    Guid SellerExpertId,
    string SellerExpertCode,
    string SellerDisplayName,
    decimal GrossPriceVnd,
    decimal PlatformFeeVnd,
    decimal ExpertNetVnd,
    DateTimeOffset PurchasedAt,
    DateTimeOffset AccessExpiresAt,
    string EntitlementStatus);

public sealed record PurchaseDetailResponse(
    Guid PurchaseId,
    Guid ProductId,
    string ProductNameAtPurchase,
    Guid SellerExpertId,
    string SellerExpertCode,
    string SellerDisplayName,
    decimal GrossPriceVnd,
    decimal PlatformFeeVnd,
    decimal ExpertNetVnd,
    Guid PaymentTransactionId,
    DateTimeOffset PurchasedAt,
    DateTimeOffset AccessExpiresAt,
    string EntitlementStatus,
    bool CanDownload,
    IReadOnlyList<string> AvailableAssetTypes,
    ProductRatingResponse? Rating);
