# 1. Implementation Summary

Phase 7C adds the seven approved Supabase-backed AI Place storage routes without changing the four-table AI schema or touching Payment, Kafka, or Notification code.

The implementation keeps browser-direct upload: the API creates a Supabase signed upload URL and a separate application-signed confirmation token, while the browser sends the file bytes directly to Supabase. The backend chooses the durable bucket/key, verifies the exact object metadata after upload, and persists only the existing Product Preview or ProductAsset identity and trusted metadata.

Preview IMAGE, protected deliverable, replacement/removal, entitlement-gated download, and derived Preview access workflows are implemented. Preview VIDEO confirmation deliberately returns `MEDIA_INSPECTION_UNAVAILABLE` until a trusted server-side media inspector is deployed; it never accepts the browser-observed duration.

# 2. Files Changed

Modified:

- `ai-available-service/Program.cs`
- `ai-available-service/appsettings.json`
- `ai-available-service/Contracts/ProductContracts.cs`
- `ai-available-service/Application/ProductApplicationService.cs`
- `ai-available-service/Infrastructure/Authentication/JwtSigningKey.cs`

Added:

- `ai-available-service/Configuration/StorageUploadTokenOptions.cs`
- `ai-available-service/Application/ProductStorageApplicationService.cs`
- `ai-available-service/Controllers/ExpertProductStorageController.cs`
- `ai-available-service/Controllers/ClientProductAssetDownloadsController.cs`
- `ai-available-service/Infrastructure/Storage/SupabaseObjectStorageService.cs`
- `ai-available-service/Infrastructure/Storage/StorageUploadTokenService.cs`
- `ai-available-service/Infrastructure/Storage/IMediaDurationInspector.cs`
- `ai-available-service/Infrastructure/Storage/StorageExceptions.cs`

The pre-existing `IObjectStorageService`, `SupabaseOptions`, Product/ProductAsset entities, mappings, and migrations were retained. No Payment, Kafka, Notification, or EF migration source was changed for Phase 7C.

# 3. Storage Configuration

`SupabaseOptions` remains the storage provider configuration:

- `Supabase:Url` (`Supabase__Url`)
- `Supabase:ServiceRoleKey` (`Supabase__ServiceRoleKey`)
- `Supabase:PreviewBucket` (`Supabase__PreviewBucket`)
- `Supabase:ProtectedAssetBucket` (`Supabase__ProtectedAssetBucket`)

The new purpose-specific confirmation-token configuration is:

- `StorageUploadToken:Base64SigningKey` (`StorageUploadToken__Base64SigningKey`)
- `StorageUploadToken:LifetimeMinutes` (default `10`, accepted range `1..60`)

The token key must be a distinct 64-byte-or-longer Base64 HS512 key. It is not a Supabase credential and is not emitted or logged. Storage methods return the safe `STORAGE_UNAVAILABLE` error when this key, Supabase URL, or service-role key is unavailable at runtime.

The service requires configured Preview and protected-asset buckets to be different. This preserves a hard separation between Preview media and the protected `VIDEO` deliverable instead of relying on an accidental shared-bucket policy. Bucket existence, privacy/publicity, allowed MIME/size limits, RLS, and lifecycle policies remain deployment prerequisites.

# 4. Supabase Adapter

`SupabaseObjectStorageService` is a small typed `HttpClient` adapter registered behind `IObjectStorageService`; no Supabase client framework/package was added.

It uses documented Storage REST operations with service-only `Authorization: Bearer` and `apikey` headers:

- `POST /storage/v1/object/upload/sign/{bucket}/{key}` for a browser upload URL;
- `GET /storage/v1/object/info/{bucket}/{key}` for canonical object metadata;
- `POST /storage/v1/object/sign/{bucket}/{key}` for a temporary read URL;
- `DELETE /storage/v1/object/{bucket}` with `prefixes` for cleanup.

The adapter transfers JSON/metadata only. It never accepts a file stream and never relays file bytes through ASP.NET Core. It converts provider/network/malformed-response failures to a provider-neutral `StorageUnavailableException` and intentionally does not include raw response bodies in exceptions or logs.

Supabase documents signed upload URLs as valid for two hours. The application confirmation token is still limited to the configured ten-minute default; the response expiry reflects the shorter confirmation window. An upload after that window cannot be attached and becomes an orphan subject to storage lifecycle/operational cleanup.

# 5. Upload Authorization Security

For each authorization, the service:

1. validates the active Expert through Identity and locks/rechecks Product ownership/state locally;
2. generates a nonce and opaque durable key:
   - Preview: `ai-place/{expertId}/{productId}/preview/{nonce}`
   - protected asset: `ai-place/{expertId}/{productId}/assets/{assetType}/{nonce}`;
3. creates an HS512, purpose-bound application token containing Expert/Product IDs, slot, Preview type where applicable, configured bucket/key, original filename, expected content type/byte size, nonce, and expiry;
4. requests Supabase signed browser-upload authority for exactly that key; and
5. returns direct-upload material only to the authorized Expert.

Confirmation trusts neither a browser bucket/key nor a browser filename/content type/byte size. It validates token signature/lifetime/purpose/current Expert/Product/slot binding, then obtains the canonical metadata for the token-bound object. Optional client ETag is only a match check; it is never evidence of object existence by itself.

# 6. Preview Workflow

The Preview routes are implemented for Expert owners only.

- Authorization accepts `IMAGE` or `VIDEO`, a nonblank filename, valid matching image/video MIME type, and nonnegative byte size. The Product must be `DRAFT` or `UNPUBLISHED`.
- Confirmation verifies token claims and exact Supabase metadata, then locks the Product and updates only its already-approved Preview columns. Replacing a Preview atomically commits new metadata before attempting old-object cleanup. Replay with the same valid token/object is safe; arbitrary or stale objects cannot be attached.
- Removal requires owner plus `DRAFT`/`UNPUBLISHED`, clears all Preview columns atomically, and returns `204`. Missing Preview removal is idempotent. Object cleanup occurs best effort after commit.

Marketplace Browse and Product Detail now derive a five-minute signed Preview access URL at read time. Preview durable bucket/key identity remains excluded from normal owner, marketplace, and Purchase response DTOs. If signing fails, the response receives safe `STORAGE_UNAVAILABLE`; no synthetic URL is returned.

# 7. Trusted Video Duration

`IMediaDurationInspector` is registered with `UnavailableMediaDurationInspector` because no verified server-side media tool, worker, or approved object-inspection runtime exists in the repository/environment.

For a VIDEO Preview, confirmation calls this abstraction for the exact token-bound bucket/key. The current implementation returns `MEDIA_INSPECTION_UNAVAILABLE` (503); it never persists `clientObservedVideoDurationSeconds`. A future configured inspector must return a trusted exact-object duration in `(0, 240]`; a duration outside the range returns `PREVIEW_VIDEO_TOO_LONG` (422).

IMAGE Preview confirmation is fully implemented and does not require duration inspection.

# 8. Protected Asset Workflow

The four allowed asset slots are exactly `SOURCE_CODE`, `VIDEO`, `DATASET`, and `DOCUMENT`.

- Authorization and confirmation require an active owning Expert, `status == DRAFT`, and `firstPublishedAt == null`.
- Confirmation persists or replaces exactly one existing `ProductAsset` row for the Product/type slot after provider metadata verification.
- Existing unique Product/type and bucket/key constraints remain in effect. The existing first-publish asset-lock trigger remains enabled and is retained as database defense in depth.
- After a first publication, every protected authorization, confirmation, and delete returns `DELIVERABLE_LOCKED`, including when the Product has subsequently been unpublished.
- Delete is idempotent for a missing Draft asset, removes metadata atomically, and performs provider cleanup best effort after commit.

No publish rule was changed. The existing four-asset persistence check therefore naturally succeeds only after all four confirmed asset rows exist.

# 9. Download Entitlement

`GET /v1/ai-place/client/purchases/{purchaseId}/products/{productId}/assets/{assetType}/download-access` validates:

1. current Client ownership of the Purchase;
2. `SUCCESS` status;
3. path Product identity matches the Purchase;
4. strict `now UTC < accessExpiresAt` entitlement activity; and
5. existence of the requested allowed Product asset type.

It deliberately does not inspect the Product's current Published/Unpublished state, preserving a successful buyer's remaining entitlement after unpublish. The returned response has only safe filename/content type/byte size and a temporary signed URL. It never returns bucket, object key, service credentials, or a permanent URL.

Read expiry is calculated as `min(now + 5 minutes, purchase.accessExpiresAt)`. The adapter floors provider seconds so it cannot issue a URL beyond the supplied entitlement boundary. At/after expiry the endpoint returns `ENTITLEMENT_EXPIRED` while Purchase History remains available.

# 10. Preview Access

Preview data is durable only as the existing Product Preview metadata. It has no stored public URL.

The marketplace mappers verify that complete Preview identity exists, then ask the storage adapter for a temporary signed display URL. Confirmation returns the same display-safe `PublicPreviewResponse` form after the Product metadata commit. Preview signing failures map to the existing safe API envelope with `STORAGE_UNAVAILABLE` and never expose provider diagnostics or object identity.

# 11. Error Mapping

Storage operations use the existing `ApiEnvelope<ApiError>` model and provide these stable safe errors:

| Error code | HTTP | Condition |
|---|---:|---|
| `STORAGE_UNAVAILABLE` | 503 | Missing/unreachable/malformed Supabase or token-signing configuration/provider failure |
| `UPLOAD_AUTHORIZATION_EXPIRED` | 409 | Expired application confirmation token |
| `INVALID_UPLOAD_TOKEN` | 400 | Invalid/tampered/cross-Expert/cross-Product/cross-slot token |
| `UPLOAD_OBJECT_NOT_FOUND` | 404 | Token-bound object absent from Supabase metadata |
| `UPLOAD_METADATA_MISMATCH` | 422 | Canonical type, size, identity, or supplied ETag does not match authorization |
| `PREVIEW_VIDEO_TOO_LONG` | 422 | Trusted duration is outside the approved maximum |
| `MEDIA_INSPECTION_UNAVAILABLE` | 503 | No trusted VIDEO inspector configured |
| `PRODUCT_ASSET_NOT_FOUND` | 404 | Requested protected type does not exist for the Purchase Product |
| `ENTITLEMENT_EXPIRED` | 403 | Purchase entitlement is no longer active |

Existing ownership/lifecycle errors remain unchanged (`PRODUCT_NOT_FOUND`, `PRODUCT_NOT_OWNED`, `INVALID_PRODUCT_STATE`, `DELIVERABLE_LOCKED`, `PURCHASE_NOT_FOUND`, and `FORBIDDEN`). No raw Supabase body, provider URL in an error, credential, object key, SQL, or stack trace is sent to callers.

# 12. Database Impact

**AI database: unchanged.** No entity, column, enum, mapping, trigger, migration, or table was added or modified.

The same approved four business tables remain:

- `ai_product`
- `product_asset`
- `product_purchase`
- `product_rating`

The implementation reuses existing Preview columns, ProductAsset unique constraints, ProductPurchase success/entitlement fields, and first-publish locking behavior. No AI EF migration is required.

# 13. Route Inventory

Exactly seven storage-dependent routes are registered:

1. `POST /v1/ai-place/expert/products/{productId}/preview/upload-authorizations`
2. `PUT /v1/ai-place/expert/products/{productId}/preview`
3. `DELETE /v1/ai-place/expert/products/{productId}/preview`
4. `POST /v1/ai-place/expert/products/{productId}/assets/{assetType}/upload-authorizations`
5. `PUT /v1/ai-place/expert/products/{productId}/assets/{assetType}`
6. `DELETE /v1/ai-place/expert/products/{productId}/assets/{assetType}`
7. `GET /v1/ai-place/client/purchases/{purchaseId}/products/{productId}/assets/{assetType}/download-access`

The existing Buy/Retry routes remain untouched. No Kafka, Notification, or new storage-independent route was added.

# 14. Build Results

Command:

```text
dotnet build ai-available-service\ai-available-service.csproj --no-restore
```

Result: **passed — 0 warnings, 0 errors.**

Static checks also confirmed:

- exactly seven new storage HTTP actions;
- the existing three AI migration files only, with no migration changes;
- no `Confluent.Kafka`, `AI_PRODUCT_PURCHASED`, or AI product Kafka topic source in `ai-available-service`.

No package reference was changed, so a restore was not required.

# 15. Runtime Results

No real Supabase object was uploaded, downloaded, or deleted.

The checked-in configuration intentionally has blank `Supabase:Url`, `Supabase:ServiceRoleKey`, and `StorageUploadToken:Base64SigningKey` placeholders. Consequently, real provider runtime tests A–F could not be performed safely and the code correctly reports `STORAGE_UNAVAILABLE` when storage configuration is missing.

Build/static validation is complete. Operational runtime verification remains pending provision of the prerequisites below, particularly a trusted VIDEO duration inspector.

# 16. Operational Prerequisites

Before enabling these endpoints in a deployed environment, provide and verify:

1. a real Supabase project URL and server-only service-role key through deployment secret configuration;
2. two real distinct buckets matching `PreviewBucket` and `ProtectedAssetBucket`;
3. storage/RLS/bucket policy that allows server service-role signing/stat/delete and accepts only the intended signed browser-upload flow;
4. a private protected-asset bucket and no permanent protected URL path;
5. upload limits/MIME policy aligned with the API's accepted types and deployment limits;
6. lifecycle/orphan-object handling for expired authorization uploads and cleanup failure;
7. a separate Base64 HS512 `StorageUploadToken` signing key; and
8. a trusted exact-object VIDEO duration inspector (for example, an approved media worker/tool with controlled access to the token-bound object).

The frontend source is not present in this repository. Its upload code must use the returned `uploadUrl`, `uploadMethod`, and `requiredHeaders`, then call confirmation using only `uploadToken` and optional ETag; it must not construct its own durable bucket/key.

# 17. Deferred Phase 7D Work

Phase 7D remains untouched:

- no Kafka producer/package;
- no `AI_PRODUCT_PURCHASED` event;
- no `neural-bridge.ai-product.purchased` topic change;
- no Notification consumer/persistence change;
- no outbox or notification deduplication.

Payment source and the Phase 7B settlement/reconciliation design were also not modified.

# 18. Genuine Blockers

There are no source-code blockers for IMAGE Preview, protected asset, or entitlement-gated signed-download support.

There are two genuine **operational** blockers to full runtime closure:

1. The repository contains no usable Supabase URL/service-role secret, confirmed bucket deployment, or bucket-policy verification. Runtime signing, metadata verification, cleanup, and download cannot be verified until those secrets/infrastructure are supplied.
2. There is no verified trusted server-side VIDEO duration capability. VIDEO Preview confirmation intentionally remains unavailable rather than trusting browser duration.

These blockers do not require an AI database schema change. They must be resolved before declaring full Phase 7C runtime verification complete.
