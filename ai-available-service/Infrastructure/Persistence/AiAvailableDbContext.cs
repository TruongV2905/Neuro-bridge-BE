using AiAvailableService.Domain.Entities;
using AiAvailableService.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace AiAvailableService.Infrastructure.Persistence;

public sealed class AiAvailableDbContext(DbContextOptions<AiAvailableDbContext> options) : DbContext(options)
{
    public DbSet<AiProduct> AiProducts => Set<AiProduct>();
    public DbSet<ProductAsset> ProductAssets => Set<ProductAsset>();
    public DbSet<ProductPurchase> ProductPurchases => Set<ProductPurchase>();
    public DbSet<ProductRating> ProductRatings => Set<ProductRating>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.HasPostgresEnum<ProductStatus>(schema: "public", name: "product_status");
        modelBuilder.HasPostgresEnum<ProductAssetType>(schema: "public", name: "product_asset_type");
        modelBuilder.HasPostgresEnum<PreviewType>(schema: "public", name: "preview_type");
        modelBuilder.HasPostgresEnum<PurchaseStatus>(schema: "public", name: "purchase_status");

        modelBuilder.ApplyConfigurationsFromAssembly(typeof(AiAvailableDbContext).Assembly);
    }
}
