using System.Text.Json.Serialization;

namespace NotificationService.Models.DTOs.Events;

/// <summary>
/// Version 1 contract from ai-available-service on
/// neural-bridge.ai-product.purchased. BuyerEmail is intentionally not used
/// for notification content or logs.
/// </summary>
public sealed class AiProductPurchasedEvent
{
    [JsonPropertyName("eventId")]
    public string? EventId { get; set; }

    [JsonPropertyName("eventType")]
    public string? EventType { get; set; }

    [JsonPropertyName("eventVersion")]
    public int EventVersion { get; set; }

    [JsonPropertyName("occurredAtUtc")]
    public DateTimeOffset? OccurredAtUtc { get; set; }

    [JsonPropertyName("purchaseId")]
    public Guid PurchaseId { get; set; }

    [JsonPropertyName("productId")]
    public Guid ProductId { get; set; }

    [JsonPropertyName("productName")]
    public string? ProductName { get; set; }

    [JsonPropertyName("purchasedAt")]
    public DateTimeOffset? PurchasedAt { get; set; }

    [JsonPropertyName("accessExpiresAt")]
    public DateTimeOffset? AccessExpiresAt { get; set; }

    [JsonPropertyName("buyerId")]
    public Guid BuyerId { get; set; }

    [JsonPropertyName("buyerCode")]
    public string? BuyerCode { get; set; }

    [JsonPropertyName("buyerEmail")]
    public string? BuyerEmail { get; set; }

    [JsonPropertyName("sellerExpertId")]
    public Guid SellerExpertId { get; set; }

    [JsonPropertyName("sellerExpertCode")]
    public string? SellerExpertCode { get; set; }

    [JsonPropertyName("sellerDisplayName")]
    public string? SellerDisplayName { get; set; }

    [JsonPropertyName("priceVnd")]
    public decimal PriceVnd { get; set; }

    [JsonPropertyName("platformFeeVnd")]
    public decimal PlatformFeeVnd { get; set; }

    [JsonPropertyName("expertNetVnd")]
    public decimal ExpertNetVnd { get; set; }

    public bool IsSupported(out string reason)
    {
        if (!string.Equals(EventType, "AI_PRODUCT_PURCHASED", StringComparison.Ordinal))
        {
            reason = "unsupported eventType";
            return false;
        }

        if (EventVersion != 1)
        {
            reason = "unsupported eventVersion";
            return false;
        }

        if (string.IsNullOrWhiteSpace(EventId) || PurchaseId == Guid.Empty || ProductId == Guid.Empty || BuyerId == Guid.Empty
            || SellerExpertId == Guid.Empty || string.IsNullOrWhiteSpace(ProductName) || string.IsNullOrWhiteSpace(BuyerCode)
            || string.IsNullOrWhiteSpace(SellerExpertCode) || string.IsNullOrWhiteSpace(SellerDisplayName)
            || OccurredAtUtc is null || PurchasedAt is null || AccessExpiresAt is null)
        {
            reason = "missing required event fields";
            return false;
        }

        if (PriceVnd <= 0 || PlatformFeeVnd < 0 || ExpertNetVnd < 0
            || decimal.Truncate(PriceVnd) != PriceVnd
            || decimal.Truncate(PlatformFeeVnd) != PlatformFeeVnd
            || decimal.Truncate(ExpertNetVnd) != ExpertNetVnd
            || PriceVnd != PlatformFeeVnd + ExpertNetVnd)
        {
            reason = "invalid monetary snapshot";
            return false;
        }

        if (AccessExpiresAt <= PurchasedAt)
        {
            reason = "invalid access expiry";
            return false;
        }

        reason = string.Empty;
        return true;
    }
}
