using AiAvailableService.Domain.Entities;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace AiAvailableService.Infrastructure.Persistence.Configurations;

public sealed class ProductPurchaseConfiguration : IEntityTypeConfiguration<ProductPurchase>
{
    public void Configure(EntityTypeBuilder<ProductPurchase> builder)
    {
        builder.ToTable("product_purchase", table =>
        {
            table.HasCheckConstraint(
                "ck_product_purchase_status_snapshot_shape",
                """
                (
                    (status = 'PENDING_PAYMENT'
                        AND payment_transaction_id IS NULL
                        AND product_name_at_purchase IS NULL
                        AND seller_expert_id_at_purchase IS NULL
                        AND seller_expert_code_at_purchase IS NULL
                        AND seller_display_name_at_purchase IS NULL
                        AND price_vnd_at_purchase IS NULL
                        AND platform_fee_vnd_at_purchase IS NULL
                        AND expert_net_vnd_at_purchase IS NULL
                        AND purchased_at IS NULL
                        AND access_expires_at IS NULL)
                    OR
                    (status = 'SUCCESS'
                        AND payment_transaction_id IS NOT NULL
                        AND product_name_at_purchase IS NOT NULL
                        AND seller_expert_id_at_purchase IS NOT NULL
                        AND seller_expert_code_at_purchase IS NOT NULL
                        AND seller_display_name_at_purchase IS NOT NULL
                        AND price_vnd_at_purchase IS NOT NULL
                        AND platform_fee_vnd_at_purchase IS NOT NULL
                        AND expert_net_vnd_at_purchase IS NOT NULL
                        AND purchased_at IS NOT NULL
                        AND access_expires_at IS NOT NULL)
                )
                """);
            table.HasCheckConstraint(
                "ck_product_purchase_success_monetary_entitlement",
                """
                status <> 'SUCCESS'
                OR
                (
                    price_vnd_at_purchase = trunc(price_vnd_at_purchase)
                    AND platform_fee_vnd_at_purchase = trunc(platform_fee_vnd_at_purchase)
                    AND expert_net_vnd_at_purchase = trunc(expert_net_vnd_at_purchase)
                    AND platform_fee_vnd_at_purchase = round(price_vnd_at_purchase * 0.10, 0)
                    AND expert_net_vnd_at_purchase = price_vnd_at_purchase - platform_fee_vnd_at_purchase
                    AND access_expires_at = purchased_at + interval '30 days'
                )
                """);
        });

        builder.HasKey(purchase => purchase.Id).HasName("pk_product_purchase");

        builder.Property(purchase => purchase.Id)
            .HasColumnName("id")
            .HasColumnType("uuid")
            .ValueGeneratedNever();
        builder.Property(purchase => purchase.ProductId).HasColumnName("product_id").HasColumnType("uuid");
        builder.Property(purchase => purchase.BuyerClientId).HasColumnName("buyer_client_id").HasColumnType("uuid");
        builder.Property(purchase => purchase.BuyerClientCode).HasColumnName("buyer_client_code").HasColumnType("text").IsRequired();
        builder.Property(purchase => purchase.Status)
            .HasColumnName("status")
            .HasColumnType("purchase_status")
            .HasDefaultValueSql("'PENDING_PAYMENT'::purchase_status")
            .HasSentinel((AiAvailableService.Domain.Enums.PurchaseStatus)(-1));
        builder.Property(purchase => purchase.PaymentTransactionId).HasColumnName("payment_transaction_id").HasColumnType("uuid");
        builder.Property(purchase => purchase.ProductNameAtPurchase).HasColumnName("product_name_at_purchase").HasColumnType("text");
        builder.Property(purchase => purchase.SellerExpertIdAtPurchase).HasColumnName("seller_expert_id_at_purchase").HasColumnType("uuid");
        builder.Property(purchase => purchase.SellerExpertCodeAtPurchase).HasColumnName("seller_expert_code_at_purchase").HasColumnType("text");
        builder.Property(purchase => purchase.SellerDisplayNameAtPurchase).HasColumnName("seller_display_name_at_purchase").HasColumnType("text");
        builder.Property(purchase => purchase.PriceVndAtPurchase).HasColumnName("price_vnd_at_purchase").HasColumnType("numeric(19,2)").HasPrecision(19, 2);
        builder.Property(purchase => purchase.PlatformFeeVndAtPurchase).HasColumnName("platform_fee_vnd_at_purchase").HasColumnType("numeric(19,2)").HasPrecision(19, 2);
        builder.Property(purchase => purchase.ExpertNetVndAtPurchase).HasColumnName("expert_net_vnd_at_purchase").HasColumnType("numeric(19,2)").HasPrecision(19, 2);
        builder.Property(purchase => purchase.PurchasedAt).HasColumnName("purchased_at").HasColumnType("timestamptz");
        builder.Property(purchase => purchase.AccessExpiresAt).HasColumnName("access_expires_at").HasColumnType("timestamptz");
        builder.Property(purchase => purchase.CreatedAt).HasColumnName("created_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");
        builder.Property(purchase => purchase.UpdatedAt).HasColumnName("updated_at").HasColumnType("timestamptz").HasDefaultValueSql("now()");

        builder.HasOne(purchase => purchase.Product)
            .WithMany(product => product.Purchases)
            .HasForeignKey(purchase => purchase.ProductId)
            .OnDelete(DeleteBehavior.Restrict)
            .HasConstraintName("fk_product_purchase_product_id");

        builder.HasIndex(purchase => new { purchase.ProductId, purchase.BuyerClientId })
            .IsUnique()
            .HasDatabaseName("uq_product_purchase_product_buyer");
        builder.HasIndex(purchase => purchase.PaymentTransactionId)
            .IsUnique()
            .HasDatabaseName("uq_product_purchase_payment_transaction");
        builder.HasIndex(purchase => new { purchase.BuyerClientId, purchase.PurchasedAt, purchase.Id })
            .IsDescending(false, true, false)
            .HasDatabaseName("ix_product_purchase_buyer_history")
            .HasFilter("\"status\" = 'SUCCESS'");
    }
}
