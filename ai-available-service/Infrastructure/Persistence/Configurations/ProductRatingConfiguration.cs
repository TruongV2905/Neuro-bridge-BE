using AiAvailableService.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace AiAvailableService.Infrastructure.Persistence.Configurations;

public sealed class ProductRatingConfiguration : IEntityTypeConfiguration<ProductRating>
{
    public void Configure(EntityTypeBuilder<ProductRating> builder)
    {
        builder.ToTable("product_rating", table =>
        {
            table.HasCheckConstraint("ck_product_rating_score_range", "score BETWEEN 1 AND 5");
            table.HasCheckConstraint(
                "ck_product_rating_review_text_nonblank_if_present",
                "review_text IS NULL OR btrim(review_text) <> ''");
        });

        builder.HasKey(rating => rating.Id).HasName("pk_product_rating");

        builder.Property(rating => rating.Id)
            .HasColumnName("id")
            .HasColumnType("uuid")
            .ValueGeneratedNever();
        builder.Property(rating => rating.PurchaseId).HasColumnName("purchase_id").HasColumnType("uuid");
        builder.Property(rating => rating.Score).HasColumnName("score").HasColumnType("smallint");
        builder.Property(rating => rating.ReviewText).HasColumnName("review_text").HasColumnType("text");
        builder.Property(rating => rating.CreatedAt).HasColumnName("created_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");
        builder.Property(rating => rating.UpdatedAt).HasColumnName("updated_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");

        builder.HasOne(rating => rating.Purchase)
            .WithOne(purchase => purchase.Rating)
            .HasForeignKey<ProductRating>(rating => rating.PurchaseId)
            .OnDelete(DeleteBehavior.Restrict)
            .HasConstraintName("fk_product_rating_purchase_id");

        builder.HasIndex(rating => rating.PurchaseId)
            .IsUnique()
            .HasDatabaseName("uq_product_rating_purchase");
    }
}
