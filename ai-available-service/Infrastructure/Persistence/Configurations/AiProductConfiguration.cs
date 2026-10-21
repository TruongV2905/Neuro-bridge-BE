using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace AiAvailableService.Infrastructure.Persistence.Configurations;

public sealed class AiProductConfiguration : IEntityTypeConfiguration<AiProduct>
{
    public void Configure(EntityTypeBuilder<AiProduct> builder)
    {
        builder.ToTable("ai_product", table =>
        {
            table.HasCheckConstraint("ck_ai_product_name_nonblank", "btrim(name) <> ''");
            table.HasCheckConstraint("ck_ai_product_category_code_nonblank", "btrim(category_code) <> ''");
            table.HasCheckConstraint(
                "ck_ai_product_description_nonblank_if_present",
                "description IS NULL OR btrim(description) <> ''");
            table.HasCheckConstraint(
                "ck_ai_product_price_vnd_positive_whole",
                "price_vnd > 0 AND price_vnd = trunc(price_vnd)");
            table.HasCheckConstraint(
                "ck_ai_product_preview_shape",
                """
                (
                    (preview_type IS NULL
                        AND preview_storage_bucket IS NULL
                        AND preview_storage_object_key IS NULL
                        AND preview_original_filename IS NULL
                        AND preview_content_type IS NULL
                        AND preview_byte_size IS NULL
                        AND preview_video_duration_seconds IS NULL
                        AND preview_uploaded_at IS NULL)
                    OR
                    (preview_type IS NOT NULL
                        AND preview_storage_bucket IS NOT NULL
                        AND preview_storage_object_key IS NOT NULL
                        AND preview_original_filename IS NOT NULL
                        AND preview_content_type IS NOT NULL
                        AND preview_byte_size IS NOT NULL
                        AND preview_uploaded_at IS NOT NULL
                        AND preview_byte_size >= 0
                        AND (
                            (preview_type = 'IMAGE' AND preview_video_duration_seconds IS NULL)
                            OR
                            (preview_type = 'VIDEO'
                                AND preview_video_duration_seconds > 0
                                AND preview_video_duration_seconds <= 240.000)
                        ))
                )
                """);
            table.HasCheckConstraint(
                "ck_ai_product_publication_timestamp_shape",
                """
                (
                    (status = 'DRAFT' AND first_published_at IS NULL AND last_published_at IS NULL)
                    OR
                    (status IN ('PUBLISHED', 'UNPUBLISHED')
                        AND first_published_at IS NOT NULL
                        AND last_published_at IS NOT NULL
                        AND last_published_at >= first_published_at)
                )
                """);
        });

        builder.HasKey(product => product.Id).HasName("pk_ai_product");

        builder.Property(product => product.Id)
            .HasColumnName("id")
            .HasColumnType("uuid")
            .ValueGeneratedNever();
        builder.Property(product => product.SellerExpertId).HasColumnName("seller_expert_id").HasColumnType("uuid");
        builder.Property(product => product.SellerExpertCode).HasColumnName("seller_expert_code").HasColumnType("text").IsRequired();
        builder.Property(product => product.Name).HasColumnName("name").HasColumnType("text").IsRequired();
        builder.Property(product => product.Description).HasColumnName("description").HasColumnType("text");
        builder.Property(product => product.CategoryCode).HasColumnName("category_code").HasColumnType("text").IsRequired();
        builder.Property(product => product.Technologies)
            .HasColumnName("technologies")
            .HasColumnType("text[]")
            .HasDefaultValueSql("ARRAY[]::text[]")
            .IsRequired();
        builder.Property(product => product.AiCapabilities)
            .HasColumnName("ai_capabilities")
            .HasColumnType("text[]")
            .HasDefaultValueSql("ARRAY[]::text[]")
            .IsRequired();
        builder.Property(product => product.PriceVnd)
            .HasColumnName("price_vnd")
            .HasColumnType("numeric(19,2)")
            .HasPrecision(19, 2);
        builder.Property(product => product.Status)
            .HasColumnName("status")
            .HasColumnType("product_status")
            .HasDefaultValueSql("'DRAFT'::product_status")
            .HasSentinel((ProductStatus)(-1));
        builder.Property(product => product.FirstPublishedAt).HasColumnName("first_published_at").HasColumnType("timestamptz");
        builder.Property(product => product.LastPublishedAt).HasColumnName("last_published_at").HasColumnType("timestamptz");
        builder.Property(product => product.PreviewType).HasColumnName("preview_type").HasColumnType("preview_type");
        builder.Property(product => product.PreviewStorageBucket).HasColumnName("preview_storage_bucket").HasColumnType("text");
        builder.Property(product => product.PreviewStorageObjectKey).HasColumnName("preview_storage_object_key").HasColumnType("text");
        builder.Property(product => product.PreviewOriginalFilename).HasColumnName("preview_original_filename").HasColumnType("text");
        builder.Property(product => product.PreviewContentType).HasColumnName("preview_content_type").HasColumnType("text");
        builder.Property(product => product.PreviewByteSize).HasColumnName("preview_byte_size").HasColumnType("bigint");
        builder.Property(product => product.PreviewVideoDurationSeconds)
            .HasColumnName("preview_video_duration_seconds")
            .HasColumnType("numeric(8,3)")
            .HasPrecision(8, 3);
        builder.Property(product => product.PreviewUploadedAt).HasColumnName("preview_uploaded_at").HasColumnType("timestamptz");
        builder.Property(product => product.CreatedAt).HasColumnName("created_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");
        builder.Property(product => product.UpdatedAt).HasColumnName("updated_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");

        builder.HasIndex(product => new { product.LastPublishedAt, product.Id })
            .HasDatabaseName("ix_ai_product_published_recent")
            .IsDescending(true, false)
            .HasFilter("\"status\" = 'PUBLISHED'");
        builder.HasIndex(product => new { product.CategoryCode, product.PriceVnd, product.Id })
            .HasDatabaseName("ix_ai_product_published_category_price")
            .HasFilter("\"status\" = 'PUBLISHED'");
        builder.HasIndex(product => new { product.PriceVnd, product.Id })
            .HasDatabaseName("ix_ai_product_published_price")
            .HasFilter("\"status\" = 'PUBLISHED'");
        builder.HasIndex(product => new { product.SellerExpertId, product.UpdatedAt, product.Id })
            .HasDatabaseName("ix_ai_product_seller_updated")
            .IsDescending(false, true, false);
    }
}
