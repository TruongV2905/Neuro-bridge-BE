using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;

namespace AiAvailableService.Infrastructure.Persistence;

/// <summary>
/// Used only by EF tooling. It does not connect to PostgreSQL or apply migrations.
/// </summary>
public sealed class AiAvailableDbContextFactory : IDesignTimeDbContextFactory<AiAvailableDbContext>
{
    public AiAvailableDbContext CreateDbContext(string[] args)
    {
        var configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: false)
            .AddJsonFile("appsettings.Development.json", optional: true)
            .AddEnvironmentVariables()
            .Build();

        var connectionString = configuration.GetConnectionString("AiAvailableDatabase");
        if (string.IsNullOrWhiteSpace(connectionString))
        {
            throw new InvalidOperationException(
                "ConnectionStrings:AiAvailableDatabase is required to create the EF Core model.");
        }

        var dataSource = AiAvailableDataSourceFactory.Create(connectionString);
        var options = new DbContextOptionsBuilder<AiAvailableDbContext>()
            .UseNpgsql(dataSource)
            .Options;

        return new AiAvailableDbContext(options);
    }
}
