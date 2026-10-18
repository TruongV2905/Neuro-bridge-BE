using AiAvailableService.Domain.Enums;

namespace AiAvailableService.Domain.Entities;

public sealed class ProductAsset
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid ProductId { get; set; }
    public ProductAssetType AssetType { get; set; }
    public string StorageBucket { get; set; } = string.Empty;
    public string StorageObjectKey { get; set; } = string.Empty;
    public string OriginalFilename { get; set; } = string.Empty;
    public string ContentType { get; set; } = string.Empty;
    public long ByteSize { get; set; }
    public DateTimeOffset UploadedAt { get; set; } = DateTimeOffset.UtcNow;

    public AiProduct Product { get; set; } = null!;
}
