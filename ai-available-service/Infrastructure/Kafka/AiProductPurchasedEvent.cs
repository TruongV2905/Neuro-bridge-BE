using System.Security.Cryptography;
using System.Text;
using System.Text.Json.Serialization;

namespace AiAvailableService.Infrastructure.Kafka;

/// <summary>
/// Version 1 JSON contract for neural-bridge.ai-product.purchased. It contains
/// the immutable local Purchase success snapshot, never an EF entity.
/// </summary>
public sealed record AiProductPurchasedEvent(
    [property: JsonPropertyName("eventId")] string EventId,
    [property: JsonPropertyName("eventType")] string EventType,
    [property: JsonPropertyName("eventVersion")] int EventVersion,
    [property: JsonPropertyName("occurredAtUtc")] DateTimeOffset OccurredAtUtc,
    [property: JsonPropertyName("purchaseId")] Guid PurchaseId,
    [property: JsonPropertyName("productId")] Guid ProductId,
    [property: JsonPropertyName("productName")] string ProductName,
    [property: JsonPropertyName("purchasedAt")] DateTimeOffset PurchasedAt,
    [property: JsonPropertyName("accessExpiresAt")] DateTimeOffset AccessExpiresAt,
    [property: JsonPropertyName("buyerId")] Guid BuyerId,
    [property: JsonPropertyName("buyerCode")] string BuyerCode,
    [property: JsonPropertyName("buyerEmail")] string? BuyerEmail,
    [property: JsonPropertyName("sellerExpertId")] Guid SellerExpertId,
    [property: JsonPropertyName("sellerExpertCode")] string SellerExpertCode,
    [property: JsonPropertyName("sellerDisplayName")] string SellerDisplayName,
    [property: JsonPropertyName("priceVnd")] decimal PriceVnd,
    [property: JsonPropertyName("platformFeeVnd")] decimal PlatformFeeVnd,
    [property: JsonPropertyName("expertNetVnd")] decimal ExpertNetVnd)
{
    public const string Type = "AI_PRODUCT_PURCHASED";
    public const int Version = 1;

    public static string CreateDeterministicEventId(Guid purchaseId)
    {
        // This stable v5-like identifier is derived only from the business
        // purchase identity and event type; it avoids a new AI tracking table.
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes($"{Type}:{purchaseId:D}"));
        return $"ai-product-purchased:{Convert.ToHexString(bytes.AsSpan(0, 16)).ToLowerInvariant()}";
    }
}
