# 1. Implementation Summary

Created the standalone ASP.NET Core 8 `ai-available-service` technical and persistence skeleton. It contains the approved four-entity EF Core model, PostgreSQL native enum mappings, the initial migration (including the single approved asset-lock trigger), strict JWT bearer setup, Eureka registration configuration, generated Identity/Payment gRPC client foundations, OpenAPI/Swagger, a lightweight health endpoint, Supabase configuration/abstraction groundwork, and a multi-stage Dockerfile.

No Phase 4 business endpoint, product workflow, payment settlement, Supabase business workflow, Kafka producer, controller, or database update was implemented.

# 2. Files Created / Modified

Created:

- `ai-available-service/ai-available-service.csproj`
- `ai-available-service/Program.cs`
- `ai-available-service/appsettings.json`
- `ai-available-service/appsettings.Development.json`
- `ai-available-service/Dockerfile`
- `ai-available-service/Domain/Entities/AiProduct.cs`
- `ai-available-service/Domain/Entities/ProductAsset.cs`
- `ai-available-service/Domain/Entities/ProductPurchase.cs`
- `ai-available-service/Domain/Entities/ProductRating.cs`
- `ai-available-service/Domain/Enums/ProductStatus.cs`
- `ai-available-service/Domain/Enums/ProductAssetType.cs`
- `ai-available-service/Domain/Enums/PreviewType.cs`
- `ai-available-service/Domain/Enums/PurchaseStatus.cs`
- `ai-available-service/Configuration/JwtOptions.cs`
- `ai-available-service/Configuration/GrpcServiceOptions.cs`
- `ai-available-service/Configuration/SupabaseOptions.cs`
- `ai-available-service/Infrastructure/Authentication/JwtSigningKey.cs`
- `ai-available-service/Infrastructure/Identity/IdentityGrpcClientRegistration.cs`
- `ai-available-service/Infrastructure/Payment/PaymentGrpcClientRegistration.cs`
- `ai-available-service/Infrastructure/Storage/IObjectStorageService.cs`
- `ai-available-service/Infrastructure/Persistence/AiAvailableDbContext.cs`
- `ai-available-service/Infrastructure/Persistence/AiAvailableDataSourceFactory.cs`
- `ai-available-service/Infrastructure/Persistence/AiAvailableDbContextFactory.cs`
- `ai-available-service/Infrastructure/Persistence/Configurations/AiProductConfiguration.cs`
- `ai-available-service/Infrastructure/Persistence/Configurations/ProductAssetConfiguration.cs`
- `ai-available-service/Infrastructure/Persistence/Configurations/ProductPurchaseConfiguration.cs`
- `ai-available-service/Infrastructure/Persistence/Configurations/ProductRatingConfiguration.cs`
- `ai-available-service/Infrastructure/Persistence/Migrations/20261002141943_InitialAiAvailable.cs`
- `ai-available-service/Infrastructure/Persistence/Migrations/20261002141943_InitialAiAvailable.Designer.cs`
- `ai-available-service/Infrastructure/Persistence/Migrations/AiAvailableDbContextModelSnapshot.cs`
- `PHASE5_TECHNICAL_SKELETON_REPORT.md`

No existing Java service, API Gateway, root Maven configuration, Docker Compose file, shared proto, or existing service source was modified.

# 3. Project / Package Versions

| Component | Version / choice |
| --- | --- |
| Target framework | `net8.0` |
| JWT bearer | `Microsoft.AspNetCore.Authentication.JwtBearer` 8.0.4 |
| EF design-time tooling | `Microsoft.EntityFrameworkCore.Design` 8.0.4 |
| PostgreSQL EF provider | `Npgsql.EntityFrameworkCore.PostgreSQL` 8.0.4 |
| Eureka discovery | `Steeltoe.Discovery.Eureka` 4.3.0 |
| Swagger / OpenAPI | `Swashbuckle.AspNetCore` 6.5.0 |
| gRPC client factory | `Grpc.Net.ClientFactory` 2.71.0 |
| Protobuf runtime / tools | `Google.Protobuf` 3.33.0 / `Grpc.Tools` 2.71.0 |

Physical snake_case mapping is explicit, so `EFCore.NamingConventions` is intentionally not required. No Supabase or Kafka package was added because this phase has no storage SDK implementation or event producer.

# 4. Service Startup Pipeline

1. ASP.NET configuration loads JSON settings and environment overrides. `EUREKA_SERVER` and `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` can supply the Eureka server URL without hard-coding it.
2. Required connection and JWT configuration is validated. The database connection comes only from `ConnectionStrings:AiAvailableDatabase` / `ConnectionStrings__AiAvailableDatabase`; the JWT key is decoded and validated before startup.
3. Options are bound for JWT, gRPC endpoints, and Supabase settings.
4. A mapped Npgsql data source and `AiAvailableDbContext` are registered. The application does not call `EnsureCreated` or `Database.Migrate`.
5. JWT bearer authentication and `ClientOnly` / `ExpertOnly` role policies are registered, followed by authorization middleware.
6. Steeltoe Eureka discovery and generated Identity/Payment gRPC clients are registered from configuration. No remote call is made during startup.
7. Swagger is registered with bearer security metadata. The generated JSON is served at `/v3/api-docs`; Swagger UI is served at `/swagger`.
8. The application maps only `/health`, `/v3/api-docs`, Swagger UI, and the controller infrastructure for future work. No product or purchase controller exists.

# 5. Persistence Implementation

- `AiProduct`, `ProductAsset`, `ProductPurchase`, and `ProductRating` are the only persistence entities and the only `DbSet`s.
- `ProductStatus`, `ProductAssetType`, `PreviewType`, and `PurchaseStatus` use `NpgsqlTypes.PgNameAttribute` plus Npgsql 8 data-source enum mapping. PostgreSQL labels are exactly `DRAFT`, `PUBLISHED`, `UNPUBLISHED`, `SOURCE_CODE`, `VIDEO`, `DATASET`, `DOCUMENT`, `IMAGE`, `PENDING_PAYMENT`, and `SUCCESS` as applicable to each type.
- Application code assigns every primary key with `Guid.NewGuid()`; configurations use `ValueGeneratedNever()` and no PostgreSQL UUID-generation default or `pgcrypto` extension is introduced.
- Explicit configurations apply the approved snake_case columns, nullable/required rules, `text[]` metadata, numeric precision, checks, unique/partial indexes, and internal-only foreign keys.
- Relationships are `AiProduct -> ProductAsset` (cascade), `AiProduct -> ProductPurchase` (restrict), and `ProductPurchase -> ProductRating` (restrict). Identity, payment, user, and wallet identifiers remain scalar values.
- The initial migration creates `fn_reject_product_asset_mutation_after_first_publish()` and `trg_product_asset_lock_after_first_publish`. The trigger rejects `INSERT`, `UPDATE`, and `DELETE` for an asset whose old or new product has `first_published_at IS NOT NULL`.

# 6. Authentication Configuration

The service validates the established Identity token contract strictly:

- HMAC SHA-512 only (`HS512`), with signature and expiration validation.
- Issuer `nqt` and audience `nqt-app` are both required and validated.
- `Jwt:Base64SigningKey` is Base64-decoded before creating the symmetric key; startup rejects a missing, invalid, or too-short key. No real key is committed.
- The UUID in `sub` and `userId` must both parse and match. `sub` is the name identity, an explicit `NameIdentifier` claim is ensured, and `role` is the role claim type.
- Future business endpoints can use the registered `ClientOnly` and `ExpertOnly` policies. No query string or header identity shortcut exists.

# 7. Eureka / Gateway Compatibility

The configured Eureka application name is exactly `ai-available-service`. It is compatible with the existing Gateway dynamic discovery rule: its expected external prefix is `/api/ai-available/**`, which the Gateway rewrites to the internal service path.

No Gateway modification was required. Runtime Eureka server URLs remain configuration/environment driven; deployment must provide a reachable Eureka configuration.

# 8. Swagger Compatibility

The exact OpenAPI JSON endpoint is:

`/v3/api-docs`

This satisfies the Gateway discovery expectation. Swagger UI is available internally at `/swagger` and references that JSON route. The OpenAPI document declares JWT bearer security for future protected APIs and contains no secrets.

# 9. gRPC Foundations

Identity:

- The authoritative `common-starter/src/main/proto/user.proto` is linked into the project at build time and generates the existing `UserService` client.
- `GrpcServices:Identity:Address` configures its address, with standard environment override support.
- No user lookup is invoked before Phase 6 business work.

Payment:

- The current authoritative `common-starter/src/main/proto/payment.proto` is linked and generates the current `PaymentService` client.
- `GrpcServices:Payment:Address` configures its address.
- No Payment call is made. In particular, `SettleAiProductPurchase` and `GetAiProductPurchaseSettlement` are not added or implemented; the Phase 4 AI Product payment contract remains deferred to Phase 7.

The Dockerfile builds from repository-root context and explicitly copies both source proto files, so the linked-proto arrangement is reproducible in container builds.

# 10. Supabase Foundation

`Supabase` options define the URL, server-side service-role credential source, preview bucket, and protected-asset bucket without embedding a real credential. `IObjectStorageService` defines only product-agnostic future capabilities for short-lived upload authorization, object metadata/existence verification, temporary protected reads, and deletion/cleanup.

No SDK implementation, product authorization, public URL persistence, or file-byte proxying is present.

# 11. Docker Readiness

`ai-available-service/Dockerfile` is a multi-stage .NET 8 build:

1. Restore from the repository-root build context.
2. Copy the linked authoritative proto files and service source.
3. Publish in an SDK image.
4. Run `AiAvailableService.dll` in the ASP.NET Core 8 runtime image.

It exposes port `8089`, sets `ASPNETCORE_URLS=http://+:8089`, and does not contain passwords, JWT keys, Supabase keys, or environment-specific discovery URLs.

# 12. Migration Review

Migration: `InitialAiAvailable` at `ai-available-service/Infrastructure/Persistence/Migrations/20261002141943_InitialAiAvailable.cs`.

It creates:

- Native enum types: `product_status`, `product_asset_type`, `preview_type`, and `purchase_status`, using the exact approved uppercase labels.
- Tables: `ai_product`, `product_asset`, `product_purchase`, and `product_rating`.
- Internal foreign keys: asset-to-product cascade; purchase-to-product restrict; rating-to-purchase restrict.
- Unique indexes: product/asset type, asset storage object, purchase/product buyer, purchase payment transaction, and rating purchase.
- Marketplace/history partial indexes: published category/price, published price, published recent, seller updated, and successful buyer history.
- Checks covering nonblank data, preview shape and 240-second video limit, positive whole-VND price, publication timestamp shape, nonnegative asset size, pending/success purchase snapshot shape, rounded ten-percent fee plus 30-day entitlement consistency, and integer score 1 through 5.
- Only the approved `fn_reject_product_asset_mutation_after_first_publish()` function and `trg_product_asset_lock_after_first_publish` trigger.

The migration SQL was generated and reviewed. No `database update` command was run, and no shared, production, or disposable database was changed.

The `Down` path now first drops the asset-lock trigger and function, then drops `product_rating`, `product_purchase`, `product_asset`, and `ai_product`. After those dependent tables are gone, it executes deterministic `DROP TYPE IF EXISTS` statements for `public.preview_type`, `public.product_asset_type`, `public.product_status`, and `public.purchase_status`.

# 13. Build / Validation Results

| Command / check | Outcome |
| --- | --- |
| `dotnet restore ai-available-service\\ai-available-service.csproj` | Succeeded. The sandbox required elevated access to read the standard local NuGet configuration; this was not a project or dependency failure. |
| `dotnet build ai-available-service\\ai-available-service.csproj --no-restore` | Succeeded with 0 warnings and 0 errors. |
| `dotnet ef migrations add InitialAiAvailable --no-build --output-dir Infrastructure/Persistence/Migrations` | Generated the reviewed initial migration. |
| `dotnet ef migrations script --no-build --project ai-available-service\\ai-available-service.csproj --startup-project ai-available-service\\ai-available-service.csproj` | Generated SQL showing all four types/tables, approved indexes, function, and trigger. |
| `dotnet ef migrations script InitialAiAvailable 0 --no-build --project ai-available-service\\ai-available-service.csproj --startup-project ai-available-service\\ai-available-service.csproj` | Generated rollback SQL that drops the trigger, function, four tables, and all four `public` enum types after their dependencies. |
| Source scan for `EnsureCreated`, `Database.Migrate`, and `.Migrate(` | No matches. |
| Controller-file scan | No controller files created. |
| `docker build -f ai-available-service/Dockerfile -t ai-available-service:phase5-cleanup .` from repository root | Not completed: Docker Desktop Linux engine is unavailable (`//./pipe/dockerDesktopLinuxEngine` was not found). |
| Local smoke test with ephemeral JWT/connection values and Eureka registration/fetch disabled | `/health` = 200 and `/v3/api-docs` = 200 (`application/json`). No external DB, Eureka, gRPC, Payment, or Supabase connectivity was asserted. |

# 14. Deferred to Phase 6

Core AI Place business implementation: the approved product-management, marketplace, preview/asset, purchase-history, entitlement-download, and product-rating REST surface; authorization/ownership rules; Identity lookup usage; and application services.

# 15. Deferred to Phase 7

- Payment-service AI Product settlement extension and the `SettleAiProductPurchase` / `GetAiProductPurchaseSettlement` client use.
- Supabase business upload confirmation, verification, cleanup, and protected temporary-download flows.
- Kafka producer and best-effort `neural-bridge.ai-product.purchased` notification event.

# 16. Blockers / Risks

The requested Docker image build is blocked only by this machine's unavailable Docker Desktop Linux engine; the Dockerfile itself was not changed and could not be exercised locally.

Runtime deployment must supply a valid Base64 HS512 JWT signing key, `ConnectionStrings__AiAvailableDatabase`, Eureka configuration when discovery is enabled, and Supabase credentials before the corresponding integrations can operate. Payment AI Product settlement RPCs do not yet exist in the shared Payment contract by approved Phase 5 scope. No external infrastructure connectivity was claimed or required for this skeleton validation.
