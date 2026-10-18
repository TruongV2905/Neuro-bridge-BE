# 1. Implementation Summary

Phase 6 turns `ai-available-service` into the core AI Place application service while preserving the approved Phase 5 persistence model and infrastructure. The service now provides the Phase 6 REST foundation, current-user access, Identity `GetUser` composition, Expert Product management/lifecycle, Client marketplace reads, Purchase History/Detail entitlement reads, and Product Rating CRUD/read APIs.

Payment settlement, Supabase upload/download workflows, and Kafka notification production remain intentionally absent for Phase 7.

**Phase 6 final status: PASSED / CLOSED.** The implementation was validated not only by build/static checks but also by local integration testing against the real Neuro Bridge development stack (PostgreSQL, Eureka, Identity, Payment dependency required by registration, API Gateway, and `ai-available-service`).

# 2. Files Created / Modified

Modified:

- `ai-available-service/Program.cs`

Created:

- `ai-available-service/Api/ApiEnvelope.cs`
- `ai-available-service/Api/ApiError.cs`
- `ai-available-service/Api/ApiException.cs`
- `ai-available-service/Api/ApiExceptionMiddleware.cs`
- `ai-available-service/Api/ApiJson.cs`
- `ai-available-service/Api/PageResponse.cs`
- `ai-available-service/Api/Pagination.cs`
- `ai-available-service/Application/ProductApplicationService.cs`
- `ai-available-service/Application/PurchaseReadApplicationService.cs`
- `ai-available-service/Application/ProductRatingApplicationService.cs`
- `ai-available-service/Contracts/ProductContracts.cs`
- `ai-available-service/Contracts/PurchaseContracts.cs`
- `ai-available-service/Contracts/RatingContracts.cs`
- `ai-available-service/Controllers/ExpertProductsController.cs`
- `ai-available-service/Controllers/MarketplaceProductsController.cs`
- `ai-available-service/Controllers/ClientPurchasesController.cs`
- `ai-available-service/Infrastructure/Authentication/CurrentUser.cs`
- `ai-available-service/Infrastructure/Identity/IdentityUser.cs`
- `ai-available-service/Infrastructure/Identity/IIdentityDirectory.cs`
- `ai-available-service/Infrastructure/Identity/IdentityGrpcDirectory.cs`
- `PHASE6_CORE_MVP_IMPLEMENTATION_REPORT.md`

# 3. Architecture

```text
ExpertProductsController / MarketplaceProductsController / ClientPurchasesController
    -> scoped ProductApplicationService / PurchaseReadApplicationService / ProductRatingApplicationService
        -> AiAvailableDbContext
        -> IIdentityDirectory (only where Identity facts are required)
            -> generated UserService.GetUser gRPC client
```

Controllers define routes, policies, and HTTP response statuses. Scoped application services own validation, authorization/ownership decisions, lifecycle changes, transactions, locking, mappings, and EF queries. EF Core remains the direct persistence access layer; no repository, unit-of-work, mapper, CQRS, or mediator layer was added.

# 4. Shared API Foundation

- `ApiEnvelope<T>` follows the Phase 4 shape: `code`, `message`, and `result`.
- Error responses use `ApiEnvelope<ApiError>`. `ApiError.errorCode` is the stable client branch key; `fieldErrors`, safe `details`, and `requestId` are optional.
- `PageResponse<T>` has `data` and `metadata`; metadata contains zero-based `page`, `size`, `currentElements`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`.
- `Pagination.Validate` supplies defaults (`page=0`, `size=10`), accepts size `1..50`, rejects negative/overflowing pages, and returns `VALIDATION_ERROR` for invalid values.
- ASP.NET automatic model-state failures and the global exception middleware use the same envelope/error-code model. Unexpected failures use a safe `INTERNAL_ERROR` response with no stack trace or infrastructure detail.
- JWT challenge/forbid responses are envelope-based `UNAUTHORIZED` (401) and `FORBIDDEN` (403). `204` actions return bodyless responses.
- JSON remains camelCase and omits null optional response fields.
- `ICurrentUser` is a scoped reusable abstraction over the validated JWT principal. It exposes the UUID subject and role without Controllers manually parsing JWTs.

# 5. Identity Integration

`IIdentityDirectory` wraps the generated `UserService.UserServiceClient` and calls only `GetUser(UserRequest)` for this phase. Generated gRPC DTOs do not leak into Controllers or business services; the wrapper maps the minimal internal `IdentityUser` model: ID, code, full name, avatar URL, role, and status.

The current Identity source was verified: `identity-service/.../constant/UserStatus.java` defines the exact active value as `ACTIVE` (alongside `BANNED` and `INACTIVE`). Every Expert Product operation validates the current Identity user as the authenticated UUID, role `EXPERT`, status `ACTIVE`, and a nonblank code before any Product transaction starts. Draft creation stores the Identity-provided seller UUID/code. Marketplace Detail resolves exactly one seller summary through `GetUser`; browse cards intentionally make no Identity calls.

gRPC lookup failure or malformed Identity response maps to `IDENTITY_UNAVAILABLE` (503); the service never invents seller/current-user facts. No DB transaction is kept open while an Identity RPC runs.

During final runtime testing, one integration defect was found and fixed: the incoming REST `Authorization: Bearer <JWT>` header was not being propagated to the authenticated Identity gRPC call. `IdentityGrpcDirectory` now forwards the existing bearer header as gRPC metadata rather than decoding, rebuilding, or re-signing the token. This restored authenticated `GetUser` calls and eliminated the observed `IDENTITY_UNAVAILABLE` response for valid Expert requests. No Identity service, proto, schema, or migration change was required.

# 6. Product Management

1. `POST /v1/ai-place/expert/products` creates an application-UUID Draft after active-Expert verification. Name/category are trimmed and nonblank; price is positive whole VND. Description is null, arrays are empty, and Preview/assets are absent.
2. `GET /v1/ai-place/expert/products` returns only the active caller's Products, paged and ordered `updatedAt DESC, id DESC`.
3. `GET /v1/ai-place/expert/products/{productId}` returns only the caller's Product detail, including safe protected-asset metadata and safe Preview metadata when persisted. Bucket, object key, permanent URL, and credentials are never mapped.
4. `PUT /v1/ai-place/expert/products/{productId}` fully replaces editable basics only while Draft or Unpublished. Arrays are required, trimmed, case-insensitively de-duplicated, and may be empty. A supplied description is trimmed and must not be blank. Published edits return `INVALID_PRODUCT_STATE`.
5. `DELETE /v1/ai-place/expert/products/{productId}` hard-deletes only an owner-owned Draft with null `firstPublishedAt`; the existing FK cascade removes its Draft assets. Ever-published Products return `INVALID_PRODUCT_STATE`.

`categoryCode` is normalized by trimming and required to be nonblank. No guessed allow-list or Category table was introduced because the canonical category vocabulary remains deferred application input.

# 7. Product Lifecycle

- Publish transitions `DRAFT -> PUBLISHED` and `UNPUBLISHED -> PUBLISHED`.
- First publish captures one UTC instant for both `firstPublishedAt` and `lastPublishedAt`.
- Republish preserves `firstPublishedAt`, refreshes `lastPublishedAt`, and restores `PUBLISHED`.
- Publish on an already Published Product is target-state idempotent: timestamps are unchanged and `alreadyInTargetState=true` is returned.
- Publish requires a nonblank description and exactly one persisted asset of each approved type: `SOURCE_CODE`, `VIDEO`, `DATASET`, and `DOCUMENT`. Missing deliverables return `REQUIRED_DELIVERABLES_MISSING`; a missing description returns `VALIDATION_ERROR`.
- Unpublish transitions only `PUBLISHED -> UNPUBLISHED`; an already Unpublished Product is idempotent, while Draft returns `INVALID_PRODUCT_STATE`. It leaves both publication markers, assets, and successful Purchase entitlements intact.

Each lifecycle command starts a local transaction, obtains the Product with PostgreSQL `SELECT ... FOR UPDATE`, rechecks ownership/state inside that transaction, and commits the state mutation atomically. Identity validation occurs before the transaction. The existing asset-lock trigger remains the database defense in depth after first publication.

# 8. Marketplace

`GET /v1/ai-place/products` is Client-only and selects current Published Products only. It implements zero-based pagination and exactly the approved filters: trimmed case-insensitive product-name `keyword`, trimmed `categoryCode`, and nonnegative whole-VND `minPriceVnd`/`maxPriceVnd` with `min <= max`. Results have the fixed sort `lastPublishedAt DESC, id DESC`; there are no technology/capability/rating filters or arbitrary sort fields.

`GET /v1/ai-place/products/{productId}` is Client-only, returns `PRODUCT_NOT_PUBLISHED` for an existing non-Published Product, and composes only the approved seller summary (`expertId`, `expertCode`, `displayName`, `avatarUrl`) through one Identity lookup. It does not aggregate Expert profile content, statistics, or ratings.

Phase 6 does not create a Supabase URL. Preview response mapping exposes safe display metadata only and keeps `accessUrl`/`accessUrlExpiresAt` null when manually seeded Preview metadata exists. Products created through Phase 6 have `preview=null`. Phase 7 storage integration must enrich this response with derived temporary public-preview access without changing Product business rules.

# 9. Purchase Read Model

`GET /v1/ai-place/client/purchases` is Client-only and returns only current-client `SUCCESS` Purchases, ordered `purchasedAt DESC, id DESC`. It maps only the immutable Product/seller/money snapshot and entitlement timestamps.

`GET /v1/ai-place/client/purchases/{purchaseId}` verifies Purchase ownership, returns the immutable snapshot plus `paymentTransactionId`, computed entitlement status, `canDownload`, and the current nullable Rating. When `now UTC < accessExpiresAt`, entitlement is `ACTIVE`, `canDownload=true`, and `availableAssetTypes` is the Product's safe asset-type list. At or after expiry it is `EXPIRED`, downloads are disabled, and history remains visible. The Product's current Published/Unpublished state is deliberately not considered.

No Purchase creation, payment RPC, Supabase call, download URL, or new 30-day calculation is present in this phase.

# 10. Product Rating

- `GET /v1/ai-place/products/{productId}/ratings` lists public ratings for a current Published Product, paged and ordered `createdAt DESC, id DESC`. It exposes only rating ID, integer score, review text, and timestamps.
- `GET /v1/ai-place/products/{productId}/ratings/summary` computes `ratingCount` and `averageScore` at query time. No denormalized aggregate is stored. With no ratings, `averageScore` is `null` and `ratingCount` is zero.
- `POST /v1/ai-place/client/purchases/{purchaseId}/rating` requires a caller-owned successful Purchase and returns 201. Entitlement expiration and download activity do not affect eligibility.
- `PUT /v1/ai-place/client/purchases/{purchaseId}/rating` changes only score/review text of the current active Rating.
- `DELETE /v1/ai-place/client/purchases/{purchaseId}/rating` hard-deletes the active Rating and returns 204; a later create remains possible.

Scores are validated as integer `1..5`; review text is nullable but, if supplied, is trimmed and nonblank. Mutation operations lock the Purchase in a local transaction. The unique `purchase_id` database constraint is retained as the duplicate-create safeguard and expected unique violations become `RATING_ALREADY_EXISTS`. `rate-service` is not called.

# 11. Phase 6 REST Route Inventory

The implemented core total is **16 endpoints**:

1. `POST /v1/ai-place/expert/products`
2. `GET /v1/ai-place/expert/products`
3. `GET /v1/ai-place/expert/products/{productId}`
4. `PUT /v1/ai-place/expert/products/{productId}`
5. `DELETE /v1/ai-place/expert/products/{productId}`
6. `POST /v1/ai-place/expert/products/{productId}/publish`
7. `POST /v1/ai-place/expert/products/{productId}/unpublish`
8. `GET /v1/ai-place/products`
9. `GET /v1/ai-place/products/{productId}`
10. `GET /v1/ai-place/client/purchases`
11. `GET /v1/ai-place/client/purchases/{purchaseId}`
12. `GET /v1/ai-place/products/{productId}/ratings`
13. `GET /v1/ai-place/products/{productId}/ratings/summary`
14. `POST /v1/ai-place/client/purchases/{purchaseId}/rating`
15. `PUT /v1/ai-place/client/purchases/{purchaseId}/rating`
16. `DELETE /v1/ai-place/client/purchases/{purchaseId}/rating`

# 12. Deferred Phase 7 Route Inventory

The following nine integration-dependent routes are intentionally not registered and have no fake/501 implementation:

1. `POST /v1/ai-place/expert/products/{productId}/preview/upload-authorizations`
2. `PUT /v1/ai-place/expert/products/{productId}/preview`
3. `DELETE /v1/ai-place/expert/products/{productId}/preview`
4. `POST /v1/ai-place/expert/products/{productId}/assets/{assetType}/upload-authorizations`
5. `PUT /v1/ai-place/expert/products/{productId}/assets/{assetType}`
6. `DELETE /v1/ai-place/expert/products/{productId}/assets/{assetType}`
7. `GET /v1/ai-place/client/purchases/{purchaseId}/products/{productId}/assets/{assetType}/download-access`
8. `POST /v1/ai-place/client/products/{productId}/purchases`
9. `POST /v1/ai-place/client/purchases/{purchaseId}/retry-payment`

# 13. Database Impact

- No schema changed.
- No EF migration was created.
- The approved four-table model remains unchanged: `ai_product`, `product_asset`, `product_purchase`, and `product_rating`.
- No entities, columns, relationships, indexes, checks, native enums, or triggers were added or modified.
- The only migration files are the existing Phase 5 initial migration, its designer, and its model snapshot.

# 14. Build / Validation Results

| Command / check | Outcome |
| --- | --- |
| `dotnet restore ai-available-service\ai-available-service.csproj` | Succeeded; all projects were up to date. Standard local NuGet configuration required the normal elevated read permission in this sandbox. |
| `dotnet build ai-available-service\ai-available-service.csproj --no-restore` | Succeeded with 0 warnings and 0 errors. |
| Controller attribute inventory | Exactly 16 HTTP actions. |
| Persistence entity scan | Exactly the four approved entity classes. |
| Migration inventory | Only the pre-existing initial migration/designer/snapshot; no Phase 6 migration. |
| Deferred-workflow source scan | No Phase 7 upload/download/buy/retry route implementation, no `SettleAiProductPurchase`/`GetAiProductPurchaseSettlement`, and no Kafka producer/package implementation. |

# 15. Runtime / Integration Validation Results

## Initial isolated smoke validation

The initial Phase 6 implementation was smoke-tested without touching a shared database.

| Request | Result |
| --- | --- |
| `GET /health` | 200 |
| `GET /v3/api-docs` | 200 |
| Unauthenticated `GET /v1/ai-place/products` | 401 with `result.errorCode = UNAUTHORIZED` |

## Final local integration validation

A real local development stack was then started and used for final Phase 6 verification:

- PostgreSQL container with the dedicated `ai_available` database.
- The approved `InitialAiAvailable` migration was applied successfully.
- `__EFMigrationsHistory` plus the four approved business tables were present.
- All four PostgreSQL native enum types were verified.
- Eureka/Discovery was running.
- Identity service was running.
- Payment service was running because Identity registration requires wallet creation.
- API Gateway was running.
- `ai-available-service` was running on port `8089` and registered successfully in Eureka as `AI-AVAILABLE-SERVICE`.
- EXPERT and CLIENT test accounts were registered successfully through the existing Identity flow.
- The .NET service validated the same Identity-issued JWT and forwarded the bearer token to Identity gRPC for authenticated `GetUser` calls.

The following Phase 6 functional tests were executed through Swagger and passed:

| Test | Expected / Observed Result |
| --- | --- |
| `GET /v1/ai-place/expert/products` with valid EXPERT token and no data | 200, empty page |
| `POST /v1/ai-place/expert/products` | 201, Draft created |
| `GET /v1/ai-place/expert/products` after creation | 200, created Product returned |
| `GET /v1/ai-place/expert/products/{productId}` | 200 |
| `PUT /v1/ai-place/expert/products/{productId}` while Draft | 200 |
| `POST .../{productId}/publish` without all four protected assets | 409 `REQUIRED_DELIVERABLES_MISSING` |
| `POST .../{productId}/unpublish` while Draft | 409 `INVALID_PRODUCT_STATE` |
| `GET /v1/ai-place/products` with CLIENT token while Product is Draft | 200, Draft is not exposed |
| `GET /v1/ai-place/client/purchases` with CLIENT token and no successful purchases | 200, empty page |
| CLIENT calling Expert-only Product endpoint | 403 `FORBIDDEN` |
| EXPERT calling Client-only marketplace endpoint | 403 `FORBIDDEN` |
| `DELETE /v1/ai-place/expert/products/{productId}` for never-published Draft | 204 |
| `GET` deleted Product | 404 `PRODUCT_NOT_FOUND` |

All planned Phase 6 runtime test cases above passed.

Positive end-to-end tests for `DRAFT -> PUBLISHED -> UNPUBLISHED -> PUBLISHED`, successful Purchase creation, Purchase Detail with a real successful settlement, and Rating mutation after successful purchase were not forced through manual database fabrication. Those flows depend naturally on the Phase 7 asset-upload and Payment capabilities and will be covered as part of the Phase 7/Phase 8 end-to-end validation.

# 16. Known MVP Limitations

- The canonical `categoryCode` vocabulary is still pending application input; this phase enforces a normalized nonblank code only.
- Preview upload/access and trusted storage metadata/URL enrichment remain Phase 7. No URL is fabricated.
- Protected asset upload/replace/delete and temporary protected download access remain Phase 7.
- Payment Buy/Retry and settlement/reconciliation remain Phase 7; no Payment proto/service was modified.
- Kafka purchase notifications remain Phase 7; no producer/event is added.
- No batch Identity lookup exists. Browse intentionally omits seller summaries rather than creating an N+1 Identity path.

# 17. Genuine Blockers

None for the Phase 6 scope. The deferred Supabase, Payment settlement, protected storage access, and Kafka notification dependencies are intentionally outside this phase and were not treated as blockers.

# 18. Phase 6 Closure

**PHASE 6 — Core AI Place MVP Implementation: PASSED / CLOSED**

Closure criteria satisfied:

- 16 Phase 6 REST endpoints implemented.
- Build succeeds with 0 warnings and 0 errors.
- No schema change and no new EF migration.
- JWT authentication and role policies verified at runtime.
- Identity `GetUser` integration verified at runtime, including bearer-token propagation over gRPC.
- Expert Draft create/read/update/delete verified.
- Product lifecycle negative-state rules verified.
- Marketplace visibility and role isolation verified.
- Purchase History read behavior verified for the no-purchase state.
- `ai-available-service` registered successfully with Eureka.
- Phase 7 integrations remain outside Phase 6.

# 19. Next Roadmap

The roadmap continues, with one wording correction: **Product Rating is already implemented in Phase 6 and should not be listed as a Phase 7 integration.**

```text
PHASE 6
Core AI Product MVP Implementation
PASSED / CLOSED
        ↓
PHASE 7
External Integrations
- Supabase Preview + Protected Asset upload/access
- Payment settlement + retry + reconciliation
- Kafka purchase event + Notification integration
        ↓
PHASE 8
End-to-End Testing / Security / Docker Integration
```

Phase 7 must keep the approved four-table database model unless a genuine blocker is found and explicitly reviewed. Phase 8 remains the final hardening/integration phase for full Docker topology, gateway routing, end-to-end flows, security checks, failure scenarios, and regression testing.
