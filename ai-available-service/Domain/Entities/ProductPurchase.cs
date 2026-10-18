using AiAvailableService.Domain.Enums;

namespace AiAvailableService.Domain.Entities;

public sealed class ProductPurchase
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid ProductId { get; set; }
    public Guid BuyerClientId { get; set; }
    public string BuyerClientCode { get; set; } = string.Empty;
    public PurchaseStatus Status { get; set; } = PurchaseStatus.PendingPayment;
    public Guid? PaymentTransactionId { get; set; }
    public string? ProductNameAtPurchase { get; set; }
    public Guid? SellerExpertIdAtPurchase { get; set; }
    public string? SellerExpertCodeAtPurchase { get; set; }
    public string? SellerDisplayNameAtPurchase { get; set; }
    public decimal? PriceVndAtPurchase { get; set; }
    public decimal? PlatformFeeVndAtPurchase { get; set; }
    public decimal? ExpertNetVndAtPurchase { get; set; }
    public DateTimeOffset? PurchasedAt { get; set; }
    public DateTimeOffset? AccessExpiresAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public AiProduct Product { get; set; } = null!;
    public ProductRating? Rating { get; set; }
}
