using AiAvailableService.Domain.Enums;

namespace AiAvailableService.Domain.Entities;

public sealed class AiProduct
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid SellerExpertId { get; set; }
    public string SellerExpertCode { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string CategoryCode { get; set; } = string.Empty;
    public string[] Technologies { get; set; } = Array.Empty<string>();
    public string[] AiCapabilities { get; set; } = Array.Empty<string>();
    public decimal PriceVnd { get; set; }
    public ProductStatus Status { get; set; } = ProductStatus.Draft;
    public DateTimeOffset? FirstPublishedAt { get; set; }
    public DateTimeOffset? LastPublishedAt { get; set; }
    public PreviewType? PreviewType { get; set; }
    public string? PreviewStorageBucket { get; set; }
    public string? PreviewStorageObjectKey { get; set; }
    public string? PreviewOriginalFilename { get; set; }
    public string? PreviewContentType { get; set; }
    public long? PreviewByteSize { get; set; }
    public decimal? PreviewVideoDurationSeconds { get; set; }
    public DateTimeOffset? PreviewUploadedAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public ICollection<ProductAsset> Assets { get; set; } = new List<ProductAsset>();
    public ICollection<ProductPurchase> Purchases { get; set; } = new List<ProductPurchase>();
}
