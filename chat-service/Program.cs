using System.Text.Json;
using System.Text.Json.Serialization;
using ChatService.Data;
using ChatService.Hubs;
using ChatService.Infrastructure.Ai;
using ChatService.Infrastructure.Clients;
using ChatService.Services;
using Com.Identity.Grpc;
using Com.Workspace.Grpc;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi.Models;
using Steeltoe.Discovery.Eureka;

var builder = WebApplication.CreateBuilder(args);

// Configure listening URL/port (default port: 4565)
if (string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("ASPNETCORE_URLS")))
{
    builder.WebHost.UseUrls("http://0.0.0.0:4565");
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
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });

builder.Services.AddEndpointsApiExplorer();

// 3. Swagger / OpenAPI
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Chat Service API (.NET 8)",
        Description = "Microservice quản lý tin nhắn thời gian thực và AI Chatbot (Gemini AI)",
        Version = "1.0.0"
    });

    var securityScheme = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Description = "Enter JWT Bearer token: Bearer {token}",
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

// 4. PostgreSQL & EF Core
string pgConnectionString = BuildPostgresConnectionString(builder.Configuration);
builder.Services.AddDbContext<ChatDbContext>(options =>
{
    options.UseNpgsql(pgConnectionString)
           .UseSnakeCaseNamingConvention();
});

// 5. gRPC Clients (Identity & Workspace)
string identityGrpcAddress = builder.Configuration["GrpcServices:Identity:Address"] 
                             ?? (Environment.GetEnvironmentVariable("IDENTITY_GRPC_HOST") != null 
                                 ? $"http://{Environment.GetEnvironmentVariable("IDENTITY_GRPC_HOST")}:9091" 
                                 : "http://localhost:9091");

string workspaceGrpcAddress = builder.Configuration["GrpcServices:Workspace:Address"] 
                              ?? (Environment.GetEnvironmentVariable("WORKSPACE_GRPC_HOST") != null 
                                  ? $"http://{Environment.GetEnvironmentVariable("WORKSPACE_GRPC_HOST")}:9093" 
                                  : "http://localhost:9093");

builder.Services.AddGrpcClient<UserService.UserServiceClient>(options =>
{
    options.Address = new Uri(identityGrpcAddress);
});

builder.Services.AddGrpcClient<WorkspaceService.WorkspaceServiceClient>(options =>
{
    options.Address = new Uri(workspaceGrpcAddress);
});

// 6. SignalR
builder.Services.AddSignalR();

// 7. CORS
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

// 8. Application Services
builder.Services.AddScoped<IIdentityClient, IdentityClient>();
builder.Services.AddScoped<IWorkspaceClient, WorkspaceClient>();
builder.Services.AddHttpClient<IGeminiAiService, GeminiAiService>();
builder.Services.AddScoped<IChatService, ChatServiceImpl>();

var app = builder.Build();

// Auto create tables if not exist
using (var scope = app.Services.CreateScope())
{
    try
    {
        var db = scope.ServiceProvider.GetRequiredService<ChatDbContext>();
        db.Database.EnsureCreated();
    }
    catch (Exception ex)
    {
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();
        logger.LogWarning(ex, "[Database] Could not run EnsureCreated during startup");
    }
}

app.UseCors("AllowAll");

if (app.Environment.IsDevelopment() || true)
{
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "Chat Service API v1");
        c.RoutePrefix = "swagger";
    });
}

app.UseRouting();

app.MapControllers();
app.MapHub<ChatHub>("/hubs/chat");
app.MapHub<ChatHub>("/ws/chat");

app.Run();

// Helper to construct connection string
static string BuildPostgresConnectionString(IConfiguration config)
{
    string? host = Environment.GetEnvironmentVariable("DB_HOST");
    string? port = Environment.GetEnvironmentVariable("DB_PORT");
    string? database = Environment.GetEnvironmentVariable("DB_NAME") ?? "chat";
    string? username = Environment.GetEnvironmentVariable("DB_USERNAME");
    string? password = Environment.GetEnvironmentVariable("DB_PASSWORD");

    if (!string.IsNullOrWhiteSpace(host))
    {
        port ??= "5432";
        username ??= "postgres";
        password ??= "postgres";
        return $"Host={host};Port={port};Database={database};Username={username};Password={password};Include Error Detail=true";
    }

    return config.GetConnectionString("DefaultConnection") 
           ?? "Host=localhost;Port=5432;Database=chat;Username=postgres;Password=postgres;";
}
