using AiAvailableService.Domain.Enums;
using Npgsql;

namespace AiAvailableService.Infrastructure.Persistence;

public static class AiAvailableDataSourceFactory
{
    public static NpgsqlDataSource Create(string connectionString)
    {
        var builder = new NpgsqlDataSourceBuilder(connectionString);
        builder.MapEnum<ProductStatus>("product_status");
        builder.MapEnum<ProductAssetType>("product_asset_type");
        builder.MapEnum<PreviewType>("preview_type");
        builder.MapEnum<PurchaseStatus>("purchase_status");
        return builder.Build();
    }
}
