using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text.Json;
using System.Text.Json.Serialization;
using AiAvailableService.Api;
using AiAvailableService.Application;
using AiAvailableService.Configuration;
using AiAvailableService.Infrastructure.Authentication;
using AiAvailableService.Infrastructure.Identity;
using AiAvailableService.Infrastructure.Kafka;
using AiAvailableService.Infrastructure.Payment;
using AiAvailableService.Infrastructure.Persistence;
using AiAvailableService.Infrastructure.Storage;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using Microsoft.OpenApi.Writers;
using Steeltoe.Discovery.Eureka;
using Swashbuckle.AspNetCore.Swagger;

var builder = WebApplication.CreateBuilder(args);

ApplyEurekaEnvironmentOverride(builder.Configuration);

var connectionString = builder.Configuration.GetConnectionString("AiAvailableDatabase");
if (string.IsNullOrWhiteSpace(connectionString))
{
    throw new InvalidOperationException(
        "ConnectionStrings:AiAvailableDatabase is required. Supply it through configuration or ConnectionStrings__AiAvailableDatabase.");
}

var jwtOptions = builder.Configuration.GetSection(JwtOptions.SectionName).Get<JwtOptions>()
    ?? throw new InvalidOperationException("Jwt configuration is required.");
var signingKeyBytes = JwtSigningKey.DecodeBase64(jwtOptions.Base64SigningKey);

builder.Services.Configure<JwtOptions>(builder.Configuration.GetSection(JwtOptions.SectionName));
builder.Services.Configure<GrpcServiceOptions>(builder.Configuration.GetSection(GrpcServiceOptions.SectionName));
builder.Services.Configure<SupabaseOptions>(builder.Configuration.GetSection(SupabaseOptions.SectionName));
builder.Services.Configure<StorageUploadTokenOptions>(
    builder.Configuration.GetSection(StorageUploadTokenOptions.SectionName));

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
        options.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
    });
builder.Services.Configure<ApiBehaviorOptions>(options =>
{
    options.InvalidModelStateResponseFactory = context =>
    {
        var fieldErrors = context.ModelState
            .Where(entry => entry.Value?.Errors.Count > 0)
            .ToDictionary(
                entry => string.IsNullOrWhiteSpace(entry.Key) ? "request" : entry.Key,
                entry => entry.Value!.Errors.Select(error => string.IsNullOrWhiteSpace(error.ErrorMessage)
                    ? "The supplied value is invalid."
                    : error.ErrorMessage).ToArray());
        var envelope = new ApiEnvelope<ApiError>(StatusCodes.Status400BadRequest,
            "One or more fields are invalid.",
            new ApiError("VALIDATION_ERROR", fieldErrors, null, context.HttpContext.TraceIdentifier));
        return new BadRequestObjectResult(envelope);
    };
});
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddHealthChecks();
builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<ICurrentUser, CurrentUser>();

var dataSource = AiAvailableDataSourceFactory.Create(connectionString);
builder.Services.AddSingleton(dataSource);
builder.Services.AddDbContext<AiAvailableDbContext>(options => options.UseNpgsql(dataSource));

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(signingKeyBytes),
            ValidateIssuer = true,
            ValidIssuer = jwtOptions.Issuer,
            ValidateAudience = true,
            ValidAudience = jwtOptions.Audience,
            ValidateLifetime = true,
            RequireExpirationTime = true,
            RequireSignedTokens = true,
            ValidAlgorithms = [SecurityAlgorithms.HmacSha512],
            NameClaimType = JwtRegisteredClaimNames.Sub,
            RoleClaimType = "role",
            ClockSkew = TimeSpan.FromMinutes(1)
        };
        options.Events = new JwtBearerEvents
        {
            OnTokenValidated = context =>
            {
                var principal = context.Principal;
                var subject = principal?.FindFirst(JwtRegisteredClaimNames.Sub)?.Value;
                var userId = principal?.FindFirst("userId")?.Value;

                if (!Guid.TryParse(subject, out var subjectId) ||
                    !Guid.TryParse(userId, out var userClaimId) ||
                    subjectId != userClaimId)
                {
                    context.Fail("The token must contain matching UUID sub and userId claims.");
                    return Task.CompletedTask;
                }

                if (principal?.Identity is ClaimsIdentity identity &&
                    !identity.HasClaim(claim => claim.Type == ClaimTypes.NameIdentifier))
                {
                    identity.AddClaim(new Claim(ClaimTypes.NameIdentifier, subjectId.ToString()));
                }

                return Task.CompletedTask;
            },
            OnChallenge = context =>
            {
                context.HandleResponse();
                return ApiExceptionMiddleware.WriteErrorAsync(context.HttpContext,
                    StatusCodes.Status401Unauthorized, "UNAUTHORIZED", "Authentication is required.", null, null);
            },
            OnForbidden = context => ApiExceptionMiddleware.WriteErrorAsync(context.HttpContext,
                StatusCodes.Status403Forbidden, "FORBIDDEN", "You do not have permission to perform this action.", null, null)
        };
    });

builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("ClientOnly", policy => policy.RequireAuthenticatedUser().RequireRole("CLIENT"));
    options.AddPolicy("ExpertOnly", policy => policy.RequireAuthenticatedUser().RequireRole("EXPERT"));
});

builder.Services.AddEurekaDiscoveryClient();
builder.Services.AddIdentityGrpcFoundation(builder.Configuration);
builder.Services.AddPaymentGrpcFoundation(builder.Configuration);
builder.Services.AddAiProductPurchaseKafka(builder.Configuration);
builder.Services.AddHttpClient<IObjectStorageService, SupabaseObjectStorageService>(client =>
{
    client.Timeout = TimeSpan.FromSeconds(20);
});
builder.Services.AddScoped<IIdentityDirectory, IdentityGrpcDirectory>();
builder.Services.AddSingleton<IStorageUploadTokenService, StorageUploadTokenService>();
builder.Services.AddSingleton<IMediaDurationInspector, UnavailableMediaDurationInspector>();
builder.Services.AddScoped<IProductApplicationService, ProductApplicationService>();
builder.Services.AddScoped<IProductStorageApplicationService, ProductStorageApplicationService>();
builder.Services.AddScoped<IPurchaseReadApplicationService, PurchaseReadApplicationService>();
builder.Services.AddScoped<IPurchasePaymentApplicationService, PurchasePaymentApplicationService>();
builder.Services.AddScoped<IProductRatingApplicationService, ProductRatingApplicationService>();

builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "AI Available Service API",
        Version = "v1",
        Description = "Technical skeleton for Neuro Bridge AI Place."
    });

    var bearerScheme = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        Description = "JWT Bearer token.",
        Reference = new OpenApiReference
        {
            Type = ReferenceType.SecurityScheme,
            Id = "Bearer"
        }
    };

    options.AddSecurityDefinition("Bearer", bearerScheme);
    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        [bearerScheme] = Array.Empty<string>()
    });
});

var app = builder.Build();

app.UseSwagger(options => options.RouteTemplate = "v3/api-docs/{documentName}");
app.UseSwaggerUI(options =>
{
    options.RoutePrefix = "swagger";
    options.SwaggerEndpoint("/v3/api-docs", "AI Available Service API v1");
});

app.UseMiddleware<ApiExceptionMiddleware>();
app.UseAuthentication();
app.UseAuthorization();

app.MapHealthChecks("/health").AllowAnonymous();
app.MapGet("/v3/api-docs", (ISwaggerProvider swaggerProvider) =>
{
    var document = swaggerProvider.GetSwagger("v1");
    using var textWriter = new StringWriter();
    var jsonWriter = new OpenApiJsonWriter(textWriter);
    document.SerializeAsV3(jsonWriter);
    jsonWriter.Flush();
    return Results.Text(textWriter.ToString(), "application/json");
}).AllowAnonymous();
app.MapControllers();

app.Run();

static void ApplyEurekaEnvironmentOverride(ConfigurationManager configuration)
{
    var eurekaServer = Environment.GetEnvironmentVariable("EUREKA_SERVER")
        ?? Environment.GetEnvironmentVariable("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE");

    if (!string.IsNullOrWhiteSpace(eurekaServer))
    {
        configuration["eureka:client:serviceUrl"] = eurekaServer;
    }
}
