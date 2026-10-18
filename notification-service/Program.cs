using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi.Models;
using NotificationService.Consumers;
using NotificationService.Data;
using NotificationService.Hubs;
using NotificationService.Repositories;
using NotificationService.Services;
using NotificationService.WebSocket;
using Npgsql;
using StackExchange.Redis;
using Steeltoe.Discovery.Eureka;

var builder = WebApplication.CreateBuilder(args);

// Configure listening URL/port (default port: 8086)
if (string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("ASPNETCORE_URLS")))
{
    builder.WebHost.UseUrls("http://0.0.0.0:8086");
}

// 1. Eureka Discovery Client (Steeltoe)
string? eurekaServer = Environment.GetEnvironmentVariable("EUREKA_SERVER")
    ?? Environment.GetEnvironmentVariable("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE");
if (!string.IsNullOrWhiteSpace(eurekaServer))
{
    builder.Configuration["eureka:client:serviceUrl"] = eurekaServer;
}
builder.Services.AddEurekaDiscoveryClient();

// 2. Add Controllers & JSON Options
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
    });

builder.Services.AddEndpointsApiExplorer();

// 3. Swagger / OpenAPI Configuration
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Notification Service API",
        Description = "API quản lý thông báo real-time (WebSocket + REST)",
        Version = "1.0.0"
    });

    var securityScheme = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Description = "Enter JWT Bearer token **_only_**",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        Reference = new OpenApiReference
        {
            Id = "bearerAuth",
            Type = ReferenceType.SecurityScheme
        }
    };

    c.AddSecurityDefinition("bearerAuth", securityScheme);
    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        { securityScheme, Array.Empty<string>() }
    });
});

// 4. PostgreSQL & EF Core Configuration
string pgConnectionString = BuildPostgresConnectionString(builder.Configuration);
builder.Services.AddDbContext<NotificationDbContext>(options =>
{
    options.UseNpgsql(pgConnectionString);
});

// 5. Redis Configuration
var redisOptions = BuildRedisOptions(builder.Configuration);
builder.Services.AddSingleton<IConnectionMultiplexer>(_ =>
{
    try
    {
        return ConnectionMultiplexer.Connect(redisOptions);
    }
    catch (Exception ex)
    {
        var logger = LoggerFactory.Create(b => b.AddConsole()).CreateLogger("Redis");
        logger.LogWarning(ex, "[Redis] Connection error during startup. Cache service will fall back to DB.");
        return ConnectionMultiplexer.Connect(new ConfigurationOptions
        {
            EndPoints = { { "127.0.0.1", 6379 } },
            AbortOnConnectFail = false
        });
    }
});

// 6. Application Repositories & Services
builder.Services.AddScoped<INotificationRepository, NotificationRepository>();
builder.Services.AddScoped<INotificationCacheService, NotificationCacheService>();
builder.Services.AddSingleton<WebSocketNotificationSender>();
builder.Services.AddSingleton<IWebSocketNotificationSender>(sp => sp.GetRequiredService<WebSocketNotificationSender>());
builder.Services.AddScoped<INotificationService, NotificationService.Services.NotificationService>();

// 7. Kafka Consumer Background Service
builder.Services.AddHostedService<NotificationKafkaConsumer>();

// 8. Real-time WebSockets & SignalR
builder.Services.AddSignalR();

// 9. CORS Policy (matching Spring application.yaml)
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.SetIsOriginAllowed(_ => true)
              .AllowAnyMethod()
              .AllowAnyHeader()
              .AllowCredentials();
    });
});

var app = builder.Build();

// Auto-migrate or ensure database table exists on startup
try
{
    using var scope = app.Services.CreateScope();
    var dbContext = scope.ServiceProvider.GetRequiredService<NotificationDbContext>();
    dbContext.Database.EnsureCreated();
    await EnsurePurchaseNotificationSchemaAsync(dbContext);
}
catch (Exception ex)
{
    app.Logger.LogWarning(ex, "Could not ensure database created at startup. Make sure Postgres is running.");
}

// Configure HTTP request pipeline
app.UseCors("AllowAll");

app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "Notification Service API v1");
    c.RoutePrefix = "swagger";
});

// WebSockets & STOMP Middleware
app.UseWebSockets(new WebSocketOptions
{
    KeepAliveInterval = TimeSpan.FromSeconds(30)
});
app.UseMiddleware<StompWebSocketMiddleware>();

app.UseRouting();

app.MapControllers();
app.MapHub<NotificationHub>("/hubs/notifications");

app.Run();

// ── Connection Helper Functions ──────────────────────────────────────────

static string BuildPostgresConnectionString(IConfiguration config)
{
    string? dbUrl = Environment.GetEnvironmentVariable("DB_URL");
    string? host = Environment.GetEnvironmentVariable("DB_HOST");
    string? port = Environment.GetEnvironmentVariable("DB_PORT") ?? "5432";
    string? user = Environment.GetEnvironmentVariable("DB_USERNAME");
    string? pass = Environment.GetEnvironmentVariable("DB_PASSWORD");
    string? name = Environment.GetEnvironmentVariable("DB_NAME") ?? "notification";

    if (!string.IsNullOrWhiteSpace(dbUrl))
    {
        // Example: jdbc:postgresql://103.180.138.143:5432/notification
        if (dbUrl.StartsWith("jdbc:postgresql://", StringComparison.OrdinalIgnoreCase))
        {
            string urlWithoutPrefix = dbUrl.Substring("jdbc:postgresql://".Length);
            int slashIndex = urlWithoutPrefix.IndexOf('/');
            string hostPort = slashIndex > 0 ? urlWithoutPrefix.Substring(0, slashIndex) : urlWithoutPrefix;
            string dbName = slashIndex > 0 ? urlWithoutPrefix.Substring(slashIndex + 1) : name;
            int colonIndex = hostPort.IndexOf(':');
            host = colonIndex > 0 ? hostPort.Substring(0, colonIndex) : hostPort;
            port = colonIndex > 0 ? hostPort.Substring(colonIndex + 1) : "5432";
            name = dbName;
        }
    }

    if (!string.IsNullOrWhiteSpace(host))
    {
        var builder = new NpgsqlConnectionStringBuilder
        {
            Host = host,
            Port = int.TryParse(port, out int p) ? p : 5432,
            Database = name,
            Username = user ?? "postgres",
            Password = pass ?? "SWD392@server",
            Pooling = true
        };
        return builder.ConnectionString;
    }

    return config.GetConnectionString("DefaultConnection")
        ?? "Host=103.180.138.143;Port=5432;Database=notification;Username=postgres;Password=SWD392@server;";
}

static ConfigurationOptions BuildRedisOptions(IConfiguration config)
{
    string? host = Environment.GetEnvironmentVariable("REDIS_HOST");
    string? port = Environment.GetEnvironmentVariable("REDIS_PORT") ?? "6379";
    string? pass = Environment.GetEnvironmentVariable("REDIS_PASSWORD");

    if (!string.IsNullOrWhiteSpace(host))
    {
        var options = new ConfigurationOptions
        {
            EndPoints = { { host, int.TryParse(port, out int p) ? p : 6379 } },
            AbortOnConnectFail = false,
            ConnectTimeout = 5000
        };
        if (!string.IsNullOrWhiteSpace(pass))
        {
            options.Password = pass;
        }
        return options;
    }

    string configStr = config["Redis:Configuration"] ?? "103.180.138.143:6379,abortConnect=false,connectTimeout=5000";
    return ConfigurationOptions.Parse(configStr);
}

static async Task EnsurePurchaseNotificationSchemaAsync(NotificationDbContext dbContext)
{
    // This service currently uses EnsureCreated rather than EF migrations.
    // Keep the Phase 7D change Notification-owned and safe for both a new and
    // already-created database while preserving all prior notifications.
    await dbContext.Database.ExecuteSqlRawAsync("""
        ALTER TABLE notifications ADD COLUMN IF NOT EXISTS event_id text NULL;
        ALTER TABLE notifications ADD COLUMN IF NOT EXISTS purchase_id uuid NULL;
        CREATE UNIQUE INDEX IF NOT EXISTS ux_notifications_event_recipient
            ON notifications (event_id, recipient_user_id)
            WHERE event_id IS NOT NULL;
        CREATE UNIQUE INDEX IF NOT EXISTS ux_notifications_purchase_recipient
            ON notifications (type, purchase_id, recipient_user_id)
            WHERE purchase_id IS NOT NULL;
        """);
}
