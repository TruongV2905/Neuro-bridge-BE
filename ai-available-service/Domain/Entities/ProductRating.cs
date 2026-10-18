namespace AiAvailableService.Domain.Entities;

public sealed class ProductRating
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid PurchaseId { get; set; }
    public short Score { get; set; }
    public string? ReviewText { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public ProductPurchase Purchase { get; set; } = null!;
}
