using AiAvailableService.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace AiAvailableService.Infrastructure.Persistence.Configurations;

public sealed class ProductAssetConfiguration : IEntityTypeConfiguration<ProductAsset>
{
    public void Configure(EntityTypeBuilder<ProductAsset> builder)
    {
        builder.ToTable("product_asset", table =>
        {
            table.HasCheckConstraint("ck_product_asset_byte_size_nonnegative", "byte_size >= 0");
        });

        builder.HasKey(asset => asset.Id).HasName("pk_product_asset");

        builder.Property(asset => asset.Id)
            .HasColumnName("id")
            .HasColumnType("uuid")
            .ValueGeneratedNever();
        builder.Property(asset => asset.ProductId).HasColumnName("product_id").HasColumnType("uuid");
        builder.Property(asset => asset.AssetType).HasColumnName("asset_type").HasColumnType("product_asset_type");
        builder.Property(asset => asset.StorageBucket).HasColumnName("storage_bucket").HasColumnType("text").IsRequired();
        builder.Property(asset => asset.StorageObjectKey).HasColumnName("storage_object_key").HasColumnType("text").IsRequired();
        builder.Property(asset => asset.OriginalFilename).HasColumnName("original_filename").HasColumnType("text").IsRequired();
        builder.Property(asset => asset.ContentType).HasColumnName("content_type").HasColumnType("text").IsRequired();
        builder.Property(asset => asset.ByteSize).HasColumnName("byte_size").HasColumnType("bigint");
        builder.Property(asset => asset.UploadedAt).HasColumnName("uploaded_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");

        builder.HasOne(asset => asset.Product)
            .WithMany(product => product.Assets)
            .HasForeignKey(asset => asset.ProductId)
            .OnDelete(DeleteBehavior.Cascade)
            .HasConstraintName("fk_product_asset_product_id");

        builder.HasIndex(asset => new { asset.ProductId, asset.AssetType })
            .IsUnique()
            .HasDatabaseName("uq_product_asset_product_type");
        builder.HasIndex(asset => new { asset.StorageBucket, asset.StorageObjectKey })
            .IsUnique()
            .HasDatabaseName("uq_product_asset_storage_object");
    }
}
