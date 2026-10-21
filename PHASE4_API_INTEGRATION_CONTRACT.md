# 1. Contract Summary

This document is the Phase 4 contract for the future ASP.NET Core 8 service named `ai-available-service`. It is design only; it creates no service, schema, DTO, controller, or integration implementation.

| Decision | Contract |
|---|---|
| Service registration name | `ai-available-service` |
| Internal REST base route | `/v1/ai-place` |
| Gateway REST base route | `/api/ai-available/v1/ai-place` |
| Why the external route differs | The Gateway derives a short name by removing `-service`, creates `Path=/api/{shortName}/**`, and rewrites that prefix away. Thus `ai-available-service` is externally `ai-available`, while the service receives `/v1/ai-place/...`. |
| Versioning | `v1`, matching the existing .NET notification-service's internal `v1/...` route convention. |
| REST envelope | Reuse the dominant Neuro Bridge envelope: `{ "code": <HTTP status>, "message": "...", "result": <payload-or-error> }`. Successful responses put the response DTO in `result`; error responses put `ApiError` in `result`. |
| Pagination | Reuse the existing zero-based `PageResponse`: `result.data` plus `result.metadata` with `page`, `size`, `currentElements`, `totalElements`, `totalPages`, `hasNext`, and `hasPrevious`. |
| Identity integration | Existing `identity-service` `UserService/GetUser(UserRequest)` gRPC lookup; no new Identity API. |
| Payment integration | A narrowly scoped synchronous extension to the existing `payment.proto` gRPC `PaymentService`; decimal money is encoded as plain decimal strings, consistent with the existing payment proto. |
| Supabase model | Browser-direct, short-lived, backend-authorized upload URLs for Expert uploads. The service persists validated object metadata only. Public preview access is derived at read time; protected downloads are entitlement-gated temporary access. |
| Kafka retained | One best-effort post-commit notification event: `AI_PRODUCT_PURCHASED` on `neural-bridge.ai-product.purchased`. |

All JSON property names below are lower camel case. `UUID` means a canonical UUID string. Monetary fields exposed by REST are JSON numbers that must be positive whole VND values where applicable; the internal gRPC Payment contract uses exact decimal strings because that is the established Payment convention.

The contract contains **25 REST endpoints**. It deliberately does not add Admin, report, dispute, refund, versioning, subscription, escrow, expert-rating, JobPost, Workspace, licensing, or download-analytics endpoints.

# 2. Role / Authorization Matrix

The service validates the authenticated subject and role server-side; frontend navigation is not an authorization boundary. For identity-sensitive commands, it additionally confirms the current user role/status through Identity gRPC.

| Endpoint/Action | CLIENT | EXPERT | Ownership Rule |
|---|---:|---:|---|
| Create, list, read, edit, delete, publish, or unpublish own Product | No | Yes | The Product's `sellerExpertId` must equal the authenticated Expert UUID. |
| Request/confirm/remove Preview | No | Yes | Same Product seller ownership; edit state must be `DRAFT` or `UNPUBLISHED`. |
| Request/confirm/remove protected asset | No | Yes | Same Product seller ownership; Product must still be its original `DRAFT` and `firstPublishedAt` must be null. |
| Browse published Products and view published Product Detail | Yes | No | Client role only; only currently `PUBLISHED` Products are visible. |
| Buy, retry payment, list/view Purchases, request protected download | Yes | No | Purchase `buyerClientId` must equal the authenticated Client UUID. |
| List/summary Product ratings | Yes | No | Product must be visible to the Client as `PUBLISHED`. |
| Create, update, delete a rating | Yes | No | The linked Purchase must be `SUCCESS` and belong to the current Client. |

There are no anonymous AI Place REST endpoints in MVP. A request with no valid credential returns `UNAUTHORIZED`; a valid credential with the wrong role returns `FORBIDDEN`.

# 3. Route Map

All Gateway paths below are the externally callable paths. The Gateway removes `/api/ai-available`, so no controller inside the future service should include that prefix.

| Method | Internal Path | Gateway Path | Role | Purpose |
|---|---|---|---|---|
| POST | `/v1/ai-place/expert/products` | `/api/ai-available/v1/ai-place/expert/products` | EXPERT | Create Draft. |
| GET | `/v1/ai-place/expert/products` | `/api/ai-available/v1/ai-place/expert/products` | EXPERT | List the caller's Products. |
| GET | `/v1/ai-place/expert/products/{productId}` | `/api/ai-available/v1/ai-place/expert/products/{productId}` | EXPERT | Get own Product detail. |
| PUT | `/v1/ai-place/expert/products/{productId}` | `/api/ai-available/v1/ai-place/expert/products/{productId}` | EXPERT | Replace editable basic Product information. |
| DELETE | `/v1/ai-place/expert/products/{productId}` | `/api/ai-available/v1/ai-place/expert/products/{productId}` | EXPERT | Hard-delete an eligible Draft. |
| POST | `/v1/ai-place/expert/products/{productId}/publish` | `/api/ai-available/v1/ai-place/expert/products/{productId}/publish` | EXPERT | Publish or idempotently return an already published Product. |
| POST | `/v1/ai-place/expert/products/{productId}/unpublish` | `/api/ai-available/v1/ai-place/expert/products/{productId}/unpublish` | EXPERT | Unpublish or idempotently return an already unpublished Product. |
| GET | `/v1/ai-place/products` | `/api/ai-available/v1/ai-place/products` | CLIENT | Browse published Products. |
| GET | `/v1/ai-place/products/{productId}` | `/api/ai-available/v1/ai-place/products/{productId}` | CLIENT | Read published Product Detail. |
| POST | `/v1/ai-place/expert/products/{productId}/preview/upload-authorizations` | `/api/ai-available/v1/ai-place/expert/products/{productId}/preview/upload-authorizations` | EXPERT | Obtain one Preview direct-upload authorization. |
| PUT | `/v1/ai-place/expert/products/{productId}/preview` | `/api/ai-available/v1/ai-place/expert/products/{productId}/preview` | EXPERT | Confirm/attach or replace a Preview. |
| DELETE | `/v1/ai-place/expert/products/{productId}/preview` | `/api/ai-available/v1/ai-place/expert/products/{productId}/preview` | EXPERT | Remove the Preview. |
| POST | `/v1/ai-place/expert/products/{productId}/assets/{assetType}/upload-authorizations` | `/api/ai-available/v1/ai-place/expert/products/{productId}/assets/{assetType}/upload-authorizations` | EXPERT | Obtain direct-upload authorization for one protected slot. |
| PUT | `/v1/ai-place/expert/products/{productId}/assets/{assetType}` | `/api/ai-available/v1/ai-place/expert/products/{productId}/assets/{assetType}` | EXPERT | Confirm/attach or replace the Draft asset in that slot. |
| DELETE | `/v1/ai-place/expert/products/{productId}/assets/{assetType}` | `/api/ai-available/v1/ai-place/expert/products/{productId}/assets/{assetType}` | EXPERT | Remove the Draft asset in that slot. |
| GET | `/v1/ai-place/client/purchases/{purchaseId}/products/{productId}/assets/{assetType}/download-access` | `/api/ai-available/v1/ai-place/client/purchases/{purchaseId}/products/{productId}/assets/{assetType}/download-access` | CLIENT | Obtain temporary protected-download access. |
| POST | `/v1/ai-place/client/products/{productId}/purchases` | `/api/ai-available/v1/ai-place/client/products/{productId}/purchases` | CLIENT | Create/reuse a stable intent and synchronously attempt settlement. |
| POST | `/v1/ai-place/client/purchases/{purchaseId}/retry-payment` | `/api/ai-available/v1/ai-place/client/purchases/{purchaseId}/retry-payment` | CLIENT | Retry the same pending Purchase after top-up. |
| GET | `/v1/ai-place/client/purchases` | `/api/ai-available/v1/ai-place/client/purchases` | CLIENT | List successful Purchase History. |
| GET | `/v1/ai-place/client/purchases/{purchaseId}` | `/api/ai-available/v1/ai-place/client/purchases/{purchaseId}` | CLIENT | Read own Purchase Detail. |
| GET | `/v1/ai-place/products/{productId}/ratings` | `/api/ai-available/v1/ai-place/products/{productId}/ratings` | CLIENT | List ratings for a published Product. |
| GET | `/v1/ai-place/products/{productId}/ratings/summary` | `/api/ai-available/v1/ai-place/products/{productId}/ratings/summary` | CLIENT | Read rating count and average. |
| POST | `/v1/ai-place/client/purchases/{purchaseId}/rating` | `/api/ai-available/v1/ai-place/client/purchases/{purchaseId}/rating` | CLIENT | Create the current Client's rating. |
| PUT | `/v1/ai-place/client/purchases/{purchaseId}/rating` | `/api/ai-available/v1/ai-place/client/purchases/{purchaseId}/rating` | CLIENT | Update the current Client's rating. |
| DELETE | `/v1/ai-place/client/purchases/{purchaseId}/rating` | `/api/ai-available/v1/ai-place/client/purchases/{purchaseId}/rating` | CLIENT | Hard-delete the current Client's rating. |

# 4. Product Management Contracts

`ProductStatus` values are `DRAFT`, `PUBLISHED`, and `UNPUBLISHED`. `ProductAssetType` values are `SOURCE_CODE`, `VIDEO`, `DATASET`, and `DOCUMENT`. A caller never supplies lifecycle or audit fields.

## Create Draft

`POST /v1/ai-place/expert/products`

Request: `CreateDraftProductRequest`

```json
{
  "name": "Vietnamese Document Classifier",
  "categoryCode": "NLP",
  "priceVnd": 250000
}
```

Response: HTTP `201`, `ApiEnvelope<OwnerProductDetailResponse>`. The resulting Product is `DRAFT`, has empty `technologies` and `aiCapabilities`, and may have null `description`, no Preview, and no assets.

Validation and state behavior:

- `name` and `categoryCode` are trimmed, nonblank, and required. `categoryCode` must be from the future application-owned canonical vocabulary; it is not a database category entity.
- `priceVnd` is required, positive, and a whole VND value. VND is the only currency and no currency field is accepted.
- Identity `GetUser` must identify the current subject as an active `EXPERT`; the service snapshots only the Expert UUID and Expert code needed by its own Product row.
- This endpoint has no durable, cross-restart request-idempotency record under the approved four-table MVP. The client must disable duplicate submissions and must not blindly retry an ambiguous create response. This limitation is explicit rather than hidden behind an unreliable idempotency claim.

## List Own Products

`GET /v1/ai-place/expert/products?page={page}&size={size}`

Response: HTTP `200`, `ApiEnvelope<PageResponse<OwnerProductListItemResponse>>`. It includes all of the Expert's statuses, ordered only by `updatedAt DESC, id DESC`. It has no unapproved status/filter/sort parameters.

The caller must be an active `EXPERT`; only rows whose `sellerExpertId` equals the caller are returned. `page` is zero-based; defaults and bounds are defined in section 15.

## Get Own Product Detail

`GET /v1/ai-place/expert/products/{productId}`

Response: HTTP `200`, `ApiEnvelope<OwnerProductDetailResponse>`. It contains management-safe asset metadata (type, original filename, validated content type, byte size, uploaded time), but does not disclose any storage bucket, object key, permanent URL, or Supabase credential.

`PRODUCT_NOT_FOUND` applies to an unknown ID; `PRODUCT_NOT_OWNED` applies when it exists but is not the caller's Product.

## Replace Editable Product Information

`PUT /v1/ai-place/expert/products/{productId}`

Request: `UpdateProductRequest`

```json
{
  "name": "Vietnamese Document Classifier",
  "description": "A nonblank product description.",
  "categoryCode": "NLP",
  "technologies": ["Python", "PyTorch"],
  "aiCapabilities": ["Document classification"],
  "priceVnd": 250000
}
```

Response: HTTP `200`, `ApiEnvelope<OwnerProductDetailResponse>`.

This is a full replacement of the editable basic-information group. `description` may be `null` only while the Product remains incomplete; a non-null description cannot be blank. Arrays are normalized by trim/de-duplicate and may be empty. Statuses, seller references, timestamps, Preview fields, and asset fields are never accepted here.

The caller must own the Product and it must be `DRAFT` or `UNPUBLISHED`; an edit while `PUBLISHED` returns `INVALID_PRODUCT_STATE`. An `UNPUBLISHED` Product may be made incomplete, but then a subsequent Publish must again meet the publication checks.

## Delete Product

`DELETE /v1/ai-place/expert/products/{productId}`

Response: HTTP `204` with no envelope body.

Deletion requires seller ownership, `status == DRAFT`, and `firstPublishedAt == null`. It hard-deletes the Product and its Draft assets through the approved internal cascade only. Any Product ever published is not deletable, even if currently `UNPUBLISHED`; return `INVALID_PRODUCT_STATE`.

## Publish

`POST /v1/ai-place/expert/products/{productId}/publish`

Request body: none. Response: HTTP `200`, `ApiEnvelope<LifecycleActionResponse>` with `productId`, `status`, `firstPublishedAt`, `lastPublishedAt`, and `alreadyInTargetState`.

The service locks the Product, verifies seller ownership, and validates all of the following atomically:

- source state is `DRAFT` or `UNPUBLISHED`;
- `description` is nonblank;
- exactly one persisted protected asset exists for each of `SOURCE_CODE`, `VIDEO`, `DATASET`, and `DOCUMENT`.

Missing description is `VALIDATION_ERROR`; a missing or incomplete asset set is `REQUIRED_DELIVERABLES_MISSING`. First publish sets both publication timestamps. Republish preserves `firstPublishedAt` and changes only `lastPublishedAt`. Calling Publish on an already `PUBLISHED` Product is idempotent: it returns the current lifecycle response with `alreadyInTargetState: true` and does not mutate timestamps.

## Unpublish

`POST /v1/ai-place/expert/products/{productId}/unpublish`

Request body: none. Response: HTTP `200`, `ApiEnvelope<LifecycleActionResponse>`.

Only the owner can transition `PUBLISHED -> UNPUBLISHED`. Calling it on an already `UNPUBLISHED` Product is idempotent and returns `alreadyInTargetState: true`. Calling it on a `DRAFT` is `INVALID_PRODUCT_STATE`. Unpublishing removes the Product from browse/new-purchase reads only; it never changes protected assets or existing successful Buyer entitlements.

# 5. Marketplace Contracts

## Browse Published Products

`GET /v1/ai-place/products?page={page}&size={size}&keyword={keyword?}&categoryCode={categoryCode?}&minPriceVnd={minPriceVnd?}&maxPriceVnd={maxPriceVnd?}`

Response: HTTP `200`, `ApiEnvelope<PageResponse<MarketplaceProductListItemResponse>>`.

Only currently `PUBLISHED` Products are selected. The list item contains `id`, `name`, `categoryCode`, `priceVnd`, `preview`, `technologies`, `aiCapabilities`, and `lastPublishedAt`. It deliberately does not make an Identity call per item and does not include a full seller profile, Expert rating, protected assets, bucket/key values, or permanent URLs. The single sort is newest publication first (`lastPublishedAt DESC, id DESC`); no client-controlled sort field/direction is accepted.

`preview`, when present, is a `PublicPreviewResponse` containing `type`, `accessUrl`, and `accessUrlExpiresAt`; it has no durable Supabase identity. The URL is derived on read and may be a signed public-read URL. It is not stored as a permanent URL in PostgreSQL.

## Product Detail

`GET /v1/ai-place/products/{productId}`

Response: HTTP `200`, `ApiEnvelope<MarketplaceProductDetailResponse>`.

The response contains the published Product's `id`, `name`, `description`, `categoryCode`, `technologies`, `aiCapabilities`, `priceVnd`, `preview`, `lastPublishedAt`, and:

```json
{
  "seller": {
    "expertId": "UUID",
    "expertCode": "EXP-...",
    "displayName": "Expert display name",
    "avatarUrl": "https://..."
  }
}
```

The basic seller summary is composed from the existing single-user Identity gRPC lookup. It must not aggregate certificates, portfolio, skills, profile statistics, or Expert ratings; the frontend continues to use the existing Expert Profile flow for that data. A non-published Product is returned as `PRODUCT_NOT_PUBLISHED`, even if its UUID exists.

# 6. Preview Upload / Management Contract

Preview is optional, has exactly one slot, and is separate from every protected deliverable. Its accepted types are `IMAGE` and `VIDEO`; a video must be no longer than 240 seconds.

## 1. Request browser-direct upload authorization

`POST /v1/ai-place/expert/products/{productId}/preview/upload-authorizations`

Request: `PreviewUploadAuthorizationRequest`

```json
{
  "previewType": "VIDEO",
  "originalFilename": "demo.mp4",
  "contentType": "video/mp4",
  "byteSize": 10485760
}
```

Response: HTTP `200`, `ApiEnvelope<SupabaseUploadAuthorizationResponse>`. It contains only the short-lived direct-upload material needed by the owning Expert's browser:

```json
{
  "uploadToken": "opaque short-lived signed token",
  "uploadUrl": "temporary Supabase upload URL",
  "uploadMethod": "PUT",
  "requiredHeaders": {"content-type": "video/mp4"},
  "storageBucket": "authorized-preview-bucket",
  "storageObjectKey": "ai-place/{expertId}/{productId}/preview/{nonce}",
  "expiresAt": "2026-10-02T05:30:00Z"
}
```

The backend, not the browser, chooses the bucket and opaque key. The key is scoped to Expert, Product, Preview slot, nonce, type, expected content type/size, and expiration through a tamper-evident upload token. The endpoint checks seller ownership and permits only `DRAFT` or `UNPUBLISHED`; a `PUBLISHED` Product returns `INVALID_PRODUCT_STATE`. Browser-supplied duration is never considered authoritative.

## 2. Confirm/attach or replace

`PUT /v1/ai-place/expert/products/{productId}/preview`

Request: `ConfirmPreviewUploadRequest`

```json
{
  "uploadToken": "opaque short-lived signed token",
  "objectETag": "optional provider object version/etag",
  "clientObservedVideoDurationSeconds": 123.456
}
```

Response: HTTP `200`, `ApiEnvelope<PreviewResponse>`.

The service validates the token claims, current caller, Product, slot, and exact assigned object identity. It obtains canonical object existence, content type, byte size, and version metadata from Supabase rather than accepting arbitrary browser bucket/key input. For video it performs or invokes trusted media-duration inspection and persists a duration only when it is in `(0, 240]`; a client observation can assist diagnostics but cannot override that verification. Exceeding the limit returns `PREVIEW_VIDEO_TOO_LONG`.

The same confirmation endpoint replaces the one existing Preview while the Product is `DRAFT` or `UNPUBLISHED`. The response contains display-safe Preview metadata and a derived `accessUrl`; it never returns bucket/key. Confirmation is idempotent only for the same still-current token/object/metadata tuple; a stale token or a different object is rejected rather than silently reattaching an arbitrary object.

## 3. Remove

`DELETE /v1/ai-place/expert/products/{productId}/preview`

Response: HTTP `204`. Seller ownership and `DRAFT`/`UNPUBLISHED` state are required. Removing a missing Preview is idempotent (`204`); permanent asset credentials are never returned. Physical-object cleanup is a storage operational concern and does not change the one-preview metadata invariant.

# 7. Protected Asset Upload / Management Contract

The `{assetType}` route parameter must be one of `SOURCE_CODE`, `VIDEO`, `DATASET`, or `DOCUMENT`. These are private storage objects, not public preview media.

## 1. Request upload authorization

`POST /v1/ai-place/expert/products/{productId}/assets/{assetType}/upload-authorizations`

Request: `ProtectedAssetUploadAuthorizationRequest`

```json
{
  "originalFilename": "model-source.zip",
  "contentType": "application/zip",
  "byteSize": 20971520
}
```

Response: HTTP `200`, `ApiEnvelope<SupabaseUploadAuthorizationResponse>`. Its exact shape is the Preview authorization shape, except its generated key is limited to the requested protected slot, for example `ai-place/{expertId}/{productId}/assets/SOURCE_CODE/{nonce}`.

The service accepts this only from the Product's owner while `status == DRAFT` and `firstPublishedAt == null`. A Product that was ever published returns `DELIVERABLE_LOCKED`; any other non-Draft state is `INVALID_PRODUCT_STATE`.

## 2. Confirm/attach or replace

`PUT /v1/ai-place/expert/products/{productId}/assets/{assetType}`

Request: `ConfirmProtectedAssetUploadRequest`

```json
{
  "uploadToken": "opaque short-lived signed token",
  "objectETag": "optional provider object version/etag"
}
```

Response: HTTP `200`, `ApiEnvelope<ProtectedAssetResponse>` containing only `assetType`, `originalFilename`, `contentType`, `byteSize`, and `uploadedAt`.

The token is valid only for the caller, Product, asset type, generated key, expected upload metadata, and expiry. The service reads Supabase metadata for that exact object and persists it only after verifying that authorized flow. It does not accept a client-supplied bucket/key, and the unique Product/type and bucket/key rules remain authoritative. Confirmation replaces the one Draft asset in that type slot. A confirmation replay with the same current authorized object is idempotent; a different or expired object/token is rejected.

## 3. Remove

`DELETE /v1/ai-place/expert/products/{productId}/assets/{assetType}`

Response: HTTP `204`. The same seller/Draft/never-published checks apply. Missing Draft assets may be removed idempotently. Once `firstPublishedAt` is non-null, insert, replace, and removal all return `DELIVERABLE_LOCKED`, including after Unpublish.

## No UploadSession table

No UploadSession business table is added. The short-lived, signed `uploadToken` carries the immutable authorization claims, while the generated key contains an unpredictable nonce. On confirmation the backend verifies the token and then queries Supabase for the exact claimed object. The durable association is the existing Preview columns or `product_asset` row only. This rejects arbitrary object adoption without inventing a fifth table. Expired or abandoned uploads may be handled by storage lifecycle/operational cleanup; they are not Product assets until confirmation succeeds.

# 8. Protected Download Contract

`GET /v1/ai-place/client/purchases/{purchaseId}/products/{productId}/assets/{assetType}/download-access`

Response: HTTP `200`, `ApiEnvelope<ProtectedDownloadAccessResponse>`.

```json
{
  "purchaseId": "UUID",
  "productId": "UUID",
  "assetType": "SOURCE_CODE",
  "originalFilename": "model-source.zip",
  "contentType": "application/zip",
  "byteSize": 20971520,
  "accessUrl": "temporary protected Supabase access URL",
  "accessUrlExpiresAt": "2026-10-02T05:25:00Z"
}
```

Before issuing access, the service must verify all of the following:

1. the authenticated caller has role `CLIENT`;
2. the Purchase belongs to that Client;
3. the Purchase's `productId` matches the path `productId`;
4. the Purchase status is `SUCCESS`;
5. current UTC time is strictly before `accessExpiresAt`;
6. the requested allowed asset type belongs to that Product.

The Product's current `PUBLISHED`/`UNPUBLISHED` state is intentionally not checked. Existing Buyers retain entitlement after an unpublish. The service returns a temporary protected URL, preferably expiring at `min(now + 5 minutes, accessExpiresAt)`, never a permanent public URL, bucket, object key, or Supabase service credential. An expired entitlement returns `ENTITLEMENT_EXPIRED` and does not remove Purchase History.

# 9. Purchase Contracts

## Stable intent and current-price rule

One `(Client, Product)` has exactly one `ProductPurchase` row across its whole lifetime. It is created as `PENDING_PAYMENT`; it has no financial/product snapshot or entitlement until settlement succeeds. The row ID is generated before calling Payment and is the immutable Payment correlation/idempotency identity.

Every settlement attempt that is safe to make locks/rechecks the pending Purchase and Product. It requires a current `PUBLISHED` Product, rejects self-purchase, and reads the then-current Product price. A pending intent does not reserve an old price. After a *confirmed non-money-moving* Payment outcome, the same Purchase may be retried with that current price and current approved Product/seller snapshot context. A timeout or unavailable response is not such confirmation: reconciliation must run first, as defined in sections 11 and 17. At success, the service persists the Payment-authoritative gross/fee/net values plus the approved Product/seller snapshot, `purchasedAt`, and `accessExpiresAt = purchasedAt + 30 days` exactly once.

## Buy Product

`POST /v1/ai-place/client/products/{productId}/purchases`

Request body: none. Response when settlement succeeds: HTTP `200`, `ApiEnvelope<PurchaseSettlementResponse>`.

`PurchaseSettlementResponse` contains `purchaseId`, `productId`, `purchaseStatus: "SUCCESS"`, `wasExistingIntent`, `paymentTransactionId`, `productNameAtPurchase`, seller snapshot, `grossPriceVnd`, `platformFeeVnd`, `expertNetVnd`, `purchasedAt`, and `accessExpiresAt`.

The service creates the stable pending row if absent, or reuses its pending row and attempts settlement. If the stable row is already `SUCCESS`, it does not call Payment and returns HTTP `409` with `PURCHASE_ALREADY_OWNED`, including only `purchaseId` and a safe Purchase-detail reference in error details. This prevents a second successful purchase even after entitlement expiry.

## Insufficient balance result

If Payment reports insufficient balance, the stable Purchase remains `PENDING_PAYMENT`; no failed Purchase row is created. The HTTP response is `409 Conflict` and uses the normal envelope with a machine-readable result, for example:

```json
{
  "code": 409,
  "message": "Wallet balance is insufficient.",
  "result": {
    "errorCode": "INSUFFICIENT_BALANCE",
    "details": {
      "purchaseId": "UUID",
      "productId": "UUID",
      "purchaseStatus": "PENDING_PAYMENT",
      "grossAmountVnd": 250000,
      "shortfallAmountVnd": 75000,
      "topUpRequired": true,
      "retryAllowed": true
    }
  }
}
```

The frontend must branch on `result.errorCode == "INSUFFICIENT_BALANCE"`, not message text, redirect to the existing wallet top-up flow, then call the retry endpoint with the returned `purchaseId`.

## Retry same pending Purchase

`POST /v1/ai-place/client/purchases/{purchaseId}/retry-payment`

Request body: none. Success response: HTTP `200`, `ApiEnvelope<PurchaseSettlementResponse>`. Insufficient balance has the error shape above. The path Purchase must belong to the current Client and must still be `PENDING_PAYMENT`; `SUCCESS` returns `PURCHASE_ALREADY_OWNED`; unknown/not-owned rows return `PURCHASE_NOT_FOUND`/`FORBIDDEN` as applicable. The endpoint never creates a new Purchase, never accepts a client price, and uses the same `purchaseId` for every Payment call.

Before retrying a prior uncertain Payment call, the service calls `GetAiProductPurchaseSettlement(purchaseId)`. A confirmed retryable no-settlement outcome permits revalidation and a new same-Purchase call using the current published Product price/context. A `SUCCESS`, authoritative `IN_PROGRESS`, or unavailable/unknown reconciliation outcome does not permit a materially different settlement request; the service recovers or waits for that authoritative outcome instead.

## Purchase History

`GET /v1/ai-place/client/purchases?page={page}&size={size}`

Response: HTTP `200`, `ApiEnvelope<PageResponse<PurchaseHistoryItemResponse>>`. It returns only `SUCCESS` Purchases for the caller, newest `purchasedAt DESC, id DESC`, including expired entitlements. Each item uses immutable snapshot data: `purchaseId`, `productId`, `productNameAtPurchase`, seller snapshot, gross/fee/net VND, `purchasedAt`, `accessExpiresAt`, and `entitlementStatus` (`ACTIVE` or `EXPIRED`). Preview object identity is not snapshotted or exposed.

## Purchase Detail

`GET /v1/ai-place/client/purchases/{purchaseId}`

Response: HTTP `200`, `ApiEnvelope<PurchaseDetailResponse>`. It returns the immutable success snapshot, `paymentTransactionId`, computed entitlement status, and `availableAssetTypes` only when entitlement is currently active. It also returns `rating`, which is `null` when this Purchase has no active Rating and otherwise contains the current Client's `ratingId`, integer `score` (1–5), optional nonblank `reviewText`, `createdAt`, and `updatedAt`. It never returns protected bucket/key or durable access URLs. `PURCHASE_NOT_FOUND` covers an unknown Purchase; an existing Purchase for another Client is `FORBIDDEN`.

# 10. Identity gRPC Usage

The service uses the existing `com.identity.grpc.UserService/GetUser` unary RPC with `UserRequest.user_id`. The response already supplies `id`, `code`, `full_name`, `avatar_url`, `status`, and `role`, which is sufficient for MVP.

| Use | Identity fields required | Contract rule |
|---|---|---|
| Create Draft and every Expert-owned mutation | `id`, `code`, `role`, `status` | Confirm current authenticated subject is an active `EXPERT`; persist Product seller UUID/code. |
| Product Detail seller summary | `id`, `code`, `full_name`, `avatar_url` | Resolve only this one seller and return the basic summary; never aggregate profile content. |
| Buy/retry | `id`, `code`, `role`, `status` | Confirm current caller is an active `CLIENT`; use `code` as the Payment client wallet/user code. |
| Successful Purchase snapshot | Seller `id`, `code`, `full_name` | Use the Product's seller UUID/code and get the display name immediately before the settlement request. |
| Purchase notification display | Buyer `id`, `full_name` | Supply minimal sender display data for the seller notification. |

The existing contract has no batch user lookup. Browse intentionally omits seller display data instead of issuing one Identity call per Product card. Product Detail has one lookup. This is a known MVP N+1 limitation if future browse requirements add seller summaries; it is not a reason to redesign Identity in this phase. `GetCurrentUser` is not used for cross-service actor resolution because the original frontend auth context is not assumed to propagate to a service-to-service gRPC call.

# 11. Payment Settlement Contract

## Chosen mechanism: gRPC

Payment already exposes its service-to-service escrow operations through `payment.proto` gRPC and represents `BigDecimal` amounts as strings. A gRPC extension therefore matches the established internal pattern and avoids routing a financial service call through the public Gateway/REST layer. The existing generic transaction REST creation endpoint and proposal-payment endpoint are explicitly not this contract.

The following is a conceptual future addition to the existing `PaymentService`. It is documentation only; this phase does not create or edit a `.proto` file.

```proto
// Conceptual additions to package com.payment.grpc / service PaymentService.
// All VND amounts are positive whole-VND BigDecimal plain strings.

rpc SettleAiProductPurchase(AiProductSettlementRequest)
    returns (AiProductSettlementResponse);
rpc GetAiProductPurchaseSettlement(AiProductSettlementLookupRequest)
    returns (AiProductSettlementResponse);

message AiProductSettlementRequest {
  string product_purchase_id = 1;       // UUID: mandatory idempotency/correlation key
  string product_id = 2;                // UUID: AI Product reference
  string client_user_code = 3;          // wallet owner to debit
  string expert_user_code = 4;          // wallet owner to credit
  string gross_amount_vnd = 5;          // current locked Product price, BigDecimal string
  AiProductSnapshotContext snapshot_context = 6;
}

message AiProductSnapshotContext {
  string product_name = 1;
  string seller_expert_id = 2;          // UUID
  string seller_expert_code = 3;
  string seller_display_name = 4;
}

message AiProductSettlementLookupRequest {
  string product_purchase_id = 1;       // UUID
}

enum AiProductSettlementStatus {
  AI_PRODUCT_SETTLEMENT_STATUS_UNSPECIFIED = 0;
  AI_PRODUCT_SETTLEMENT_SUCCESS = 1;
  AI_PRODUCT_SETTLEMENT_INSUFFICIENT_BALANCE = 2;
  AI_PRODUCT_SETTLEMENT_WALLET_NOT_ACTIVE = 3;
  AI_PRODUCT_SETTLEMENT_PAYMENT_FAILED = 4;
  AI_PRODUCT_SETTLEMENT_NOT_FOUND = 5;
  AI_PRODUCT_SETTLEMENT_IDEMPOTENCY_CONFLICT = 6;
  AI_PRODUCT_SETTLEMENT_IN_PROGRESS = 7;
}

message AiProductSettlementResponse {
  AiProductSettlementStatus status = 1;
  string product_purchase_id = 2;
  string payment_transaction_id = 3;    // required only for SUCCESS
  string gross_amount_vnd = 4;          // required only for SUCCESS
  string platform_fee_amount_vnd = 5;   // required only for SUCCESS
  string expert_net_amount_vnd = 6;     // required only for SUCCESS
  string shortfall_amount_vnd = 7;      // only for INSUFFICIENT_BALANCE
  string settled_at_utc = 8;            // RFC 3339 UTC instant, only for SUCCESS
  AiProductSnapshotContext snapshot_context = 9; // original accepted context on SUCCESS or IN_PROGRESS
  string message = 10;                  // safe diagnostic text; never parsed by the FE
}
```

`snapshot_context` is not a financial assertion. Once Payment reaches `SUCCESS` or durable `IN_PROGRESS`, it is immutable caller context echoed from Payment's authoritative record so ai-available-service can recover the approved historical listing/seller snapshot if local persistence later fails. A confirmed retryable no-money-moving outcome does not freeze this context; Payment may accept the current context on the next same-Purchase retry. Payment remains authoritative for financial fields and settlement status.

Settlement policy and obligations for the later Payment extension:

1. Use `product_purchase_id` as the permanent correlation/idempotency key. Material input means Product ID, Client code, Expert code, gross amount, and snapshot context.
2. The following are **retryable, non-money-moving outcomes**: `INSUFFICIENT_BALANCE`, `WALLET_NOT_ACTIVE`, `NOT_FOUND`, and `PAYMENT_FAILED` only when Payment explicitly guarantees that it performed no debit or credit. These outcomes do not freeze material input. The same key may be submitted again after ai-available-service revalidates that the Product is currently `PUBLISHED` and reconstructs the current price/context. Payment may accept that changed gross amount/context while still ensuring no double debit.
3. `SUCCESS` and `IN_PROGRESS` are **authoritative/frozen outcomes**. Once Payment has settled money or durably entered an in-flight state, it retains the accepted material input for the key. Repeated same-key requests return/reconcile that original authoritative result; they never create a second settlement.
4. `AI_PRODUCT_SETTLEMENT_IDEMPOTENCY_CONFLICT` means a request is incompatible with the material input of an already frozen `SUCCESS` or `IN_PROGRESS` settlement identity. It must not be returned for a legitimate current-price retry after a confirmed retryable, non-money-moving outcome.
5. Debit Client by the whole gross amount, calculate `platformFee = ROUND(gross * 0.10, nearest whole VND)` with positive half-VND ties rounded up (equivalent to the approved `MidpointRounding.AwayFromZero`/PostgreSQL `round(numeric, 0)` policy), credit Expert by `gross - platformFee`, and credit the platform/system by `platformFee`. The Payment service owns the calculation and returns actual values. There is no escrow.
6. On `SUCCESS`, return the main Payment transaction ID, gross amount, fee, Expert net, and authoritative settlement instant. The main transaction can coexist with an internal system-fee transaction; ai-available-service stores only the returned main transaction ID.
7. Insufficient balance and inactive-wallet results leave the Purchase pending and perform no partial settlement. A `PAYMENT_FAILED` result is retryable only when its contract confirms no movement; a timeout, unavailable response, or other unresolved result must not be represented as that confirmation.
8. The lookup RPC returns the durable outcome for reconciliation. Before any pending retry after an uncertain call, ai-available-service queries it. `SUCCESS` recovers locally; confirmed retryable no-settlement/`NOT_FOUND` allows a current-price retry; `IN_PROGRESS` or an unavailable/unknown result must not be followed by a materially different settlement request.

Required future Payment enum additions are `AI_PRODUCT_PAYMENT` in `TransactionType` and `AI_PRODUCT` in `PaymentReferenceType`; the Payment reference ID is the `ProductPurchase.Id`. These are documented future changes only. They do not reuse `PROPOSAL_PAYMENT`, `PROPOSAL`, `PROJECT`, or any escrow path.

# 12. Product Rating Contracts

The existing `rate-service` is not called. An active rating is uniquely linked to one `ProductPurchase`, so caller, Product, and eligibility derive from that Purchase.

## List ratings

`GET /v1/ai-place/products/{productId}/ratings?page={page}&size={size}`

Response: HTTP `200`, `ApiEnvelope<PageResponse<ProductRatingResponse>>`, ordered `createdAt DESC, id DESC`. The Product must be currently published. Each public rating item exposes only `ratingId`, `score`, `reviewText`, `createdAt`, and `updatedAt`; it does not expose another Buyer's `purchaseId`, client email, wallet code, or protected asset metadata.

## Rating summary

`GET /v1/ai-place/products/{productId}/ratings/summary`

Response: HTTP `200`, `ApiEnvelope<ProductRatingSummaryResponse>` with `productId`, `ratingCount`, and `averageScore`. The average is calculated from active ratings at query time; no denormalized aggregate is persisted.

## Create current Client rating

`POST /v1/ai-place/client/purchases/{purchaseId}/rating`

Request: `CreateProductRatingRequest`

```json
{
  "score": 5,
  "reviewText": "Useful and well structured."
}
```

Response: HTTP `201`, `ApiEnvelope<ProductRatingResponse>`. The Purchase must belong to the caller and be `SUCCESS`. Entitlement expiry and download activity do not affect rating eligibility. `score` is an integer `1..5`; an omitted/null `reviewText` is allowed, but a supplied value cannot be blank. An existing active rating returns `RATING_ALREADY_EXISTS`.

## Update current Client rating

`PUT /v1/ai-place/client/purchases/{purchaseId}/rating`

Request: `UpdateProductRatingRequest` with the same `score` and nullable `reviewText` fields. Response: HTTP `200`, `ApiEnvelope<ProductRatingResponse>`. The caller must own the successful Purchase and its existing active rating. It replaces the score/review only; Product/Purchase linkage is immutable.

## Delete current Client rating

`DELETE /v1/ai-place/client/purchases/{purchaseId}/rating`

Response: HTTP `204`. It hard-deletes the caller-owned active rating only. A later create is permitted because the approved model defines one active rating, not an irreversible one-time rating action.

# 13. Request / Response DTO Catalog

The following names are contract names, not classes to create in this phase.

| DTO | Fields | Used By | Notes |
|---|---|---|---|
| `ApiEnvelope<T>` | `code`, `message`, `result` | Every JSON REST response except `204` | Matches existing `APIResponse` shape. `code` equals the HTTP status. |
| `ApiError` | `errorCode`, `fieldErrors?`, `details?`, `requestId?` | Error envelope `result` | `errorCode` is the stable frontend branch key. |
| `PageResponse<T>` | `data`, `metadata` | Every paged read | Existing PageResponse shape. |
| `PageMetadata` | `page`, `size`, `currentElements`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious` | Every paged read | Zero-based page. |
| `CreateDraftProductRequest` | `name`, `categoryCode`, `priceVnd` | Create Draft | Only Draft-required fields. |
| `UpdateProductRequest` | `name`, `description?`, `categoryCode`, `technologies`, `aiCapabilities`, `priceVnd` | Replace Product basic information | Full editable group; no status/asset/Preview/seller fields. |
| `OwnerProductListItemResponse` | `id`, `name`, `categoryCode`, `priceVnd`, `status`, `preview?`, `updatedAt`, `lastPublishedAt?` | Own list | No storage identity. |
| `OwnerProductDetailResponse` | list-item fields plus `description?`, `technologies`, `aiCapabilities`, `firstPublishedAt?`, `createdAt`, `assets`, `preview?` | Create, own get, update | `assets` are `ProtectedAssetResponse`; Preview has no bucket/key. |
| `LifecycleActionResponse` | `productId`, `status`, `firstPublishedAt?`, `lastPublishedAt?`, `alreadyInTargetState` | Publish/unpublish | No caller-supplied lifecycle timestamps. |
| `MarketplaceProductListItemResponse` | `id`, `name`, `categoryCode`, `priceVnd`, `technologies`, `aiCapabilities`, `preview?`, `lastPublishedAt` | Browse | No seller profile, rating, assets, or storage identity. |
| `MarketplaceProductDetailResponse` | list-item fields plus `description`, `seller` | Product Detail | `seller` is `BasicSellerSummaryResponse`. |
| `BasicSellerSummaryResponse` | `expertId`, `expertCode`, `displayName`, `avatarUrl?` | Product Detail | Identity-derived; not an Expert profile. |
| `PublicPreviewResponse` / `PreviewResponse` | `type`, `originalFilename`, `contentType`, `byteSize`, `videoDurationSeconds?`, `uploadedAt`, `accessUrl`, `accessUrlExpiresAt` | Marketplace/owner preview reads | No bucket/key; `accessUrl` is derived, not persisted as a permanent URL. |
| `PreviewUploadAuthorizationRequest` | `previewType`, `originalFilename`, `contentType`, `byteSize` | Preview authorization | Duration is verified after upload. |
| `ProtectedAssetUploadAuthorizationRequest` | `originalFilename`, `contentType`, `byteSize` | Protected asset authorization | Type is in path. |
| `SupabaseUploadAuthorizationResponse` | `uploadToken`, `uploadUrl`, `uploadMethod`, `requiredHeaders`, `storageBucket`, `storageObjectKey`, `expiresAt` | Both upload authorizations | Storage identity is disclosed only to owner for short-lived direct upload. |
| `ConfirmPreviewUploadRequest` | `uploadToken`, `objectETag?`, `clientObservedVideoDurationSeconds?` | Confirm Preview | Browser facts are revalidated against storage/trusted media inspection. |
| `ConfirmProtectedAssetUploadRequest` | `uploadToken`, `objectETag?` | Confirm asset | Exact authorized object only. |
| `ProtectedAssetResponse` | `assetType`, `originalFilename`, `contentType`, `byteSize`, `uploadedAt` | Owner Product/asset confirmation | Never has bucket/key/URL. |
| `ProtectedDownloadAccessResponse` | `purchaseId`, `productId`, `assetType`, `originalFilename`, `contentType`, `byteSize`, `accessUrl`, `accessUrlExpiresAt` | Protected download access | Temporary read authority only. |
| `PurchaseSettlementResponse` | `purchaseId`, `productId`, `purchaseStatus`, `wasExistingIntent`, `paymentTransactionId`, product/seller snapshot, `grossPriceVnd`, `platformFeeVnd`, `expertNetVnd`, `purchasedAt`, `accessExpiresAt` | Successful Buy/retry | Values originate from final Purchase/Payment result. |
| `PurchaseHistoryItemResponse` | `purchaseId`, `productId`, `productNameAtPurchase`, seller snapshot, money snapshot, `purchasedAt`, `accessExpiresAt`, `entitlementStatus` | Purchase History | Successful Purchases only. |
| `PurchaseDetailResponse` | history fields plus `paymentTransactionId`, `availableAssetTypes`, `canDownload`, `rating: ProductRatingResponse?` | Purchase Detail | `rating` is null or the current Client's active rating; no preview snapshot or protected object identity. |
| `CreateProductRatingRequest` / `UpdateProductRatingRequest` | `score`, `reviewText?` | Rating mutation | `score` integer 1–5; supplied review must be nonblank. |
| `ProductRatingResponse` | `ratingId`, `score`, `reviewText?`, `createdAt`, `updatedAt` | Rating mutation/public list/Purchase Detail | It contains no `purchaseId`; that linkage is internal and mutation routes remain Purchase-based. |
| `ProductRatingSummaryResponse` | `productId`, `ratingCount`, `averageScore` | Rating summary | Computed, not stored. |

# 14. Error Contract

Errors use `ApiEnvelope<ApiError>`. `message` is human-safe and localizable; clients must use `result.errorCode`, never parse `message`. No error exposes a stack trace, SQL, Supabase credential, payment internals, or an object path.

| Error Code | HTTP Status | Trigger | FE Action |
|---|---:|---|---|
| `VALIDATION_ERROR` | 400 | Invalid fields, blank required text, invalid enum/path ID, invalid whole-VND input, invalid filter range | Show `fieldErrors`/safe message and correct input. |
| `UNAUTHORIZED` | 401 | Missing/invalid authentication | Sign in again. |
| `FORBIDDEN` | 403 | Wrong role or a caller attempts another user's Purchase/rating | Do not retry; show access denial. |
| `PRODUCT_NOT_FOUND` | 404 | Product ID does not exist in owner scope | Refresh/list navigation. |
| `PRODUCT_NOT_OWNED` | 403 | Existing Product does not belong to Expert caller | Do not retry. |
| `INVALID_PRODUCT_STATE` | 409 | Disallowed lifecycle action or edit while Published | Refresh Product and follow allowed state flow. |
| `PRODUCT_NOT_PUBLISHED` | 409 | Client browses/details/buys a non-published Product | Return to marketplace; do not settle. |
| `REQUIRED_DELIVERABLES_MISSING` | 409 | Publish lacks one or more required protected asset types | Return to asset management. |
| `DELIVERABLE_LOCKED` | 409 | Asset mutation after first publish | Inform Expert that MVP assets are permanent. |
| `PREVIEW_VIDEO_TOO_LONG` | 422 | Trusted duration is over 240 seconds | Upload a preview no longer than four minutes. |
| `PURCHASE_ALREADY_OWNED` | 409 | Client tries to buy/retry a successful stable Purchase | Open Purchase Detail; no new purchase. |
| `PURCHASE_NOT_FOUND` | 404 | Purchase ID is absent for a caller's route | Refresh Purchase History. |
| `INSUFFICIENT_BALANCE` | 409 | Payment reports a shortfall | Use error details to top up, then retry the same `purchaseId`. |
| `WALLET_NOT_ACTIVE` | 409 | Payment reports inactive/missing usable wallet | Direct user to wallet support/activation; do not create a new Purchase. |
| `PAYMENT_FAILED` | 502 | Payment returned a confirmed atomic non-success outcome with no money movement | Keep pending Purchase; after revalidation it may retry the same Purchase at current price. |
| `PAYMENT_SERVICE_UNAVAILABLE` | 503 | gRPC timeout/unavailability with settlement outcome not yet known | Keep pending Purchase; reconcile first and do not send a materially different retry while outcome is uncertain. |
| `ENTITLEMENT_EXPIRED` | 403 | Successful Purchase exists but `now >= accessExpiresAt` | Keep History visible; disable protected download. |
| `RATING_NOT_ALLOWED` | 403 | Rating caller lacks a successful owned Purchase | Do not show mutation UI. |
| `RATING_ALREADY_EXISTS` | 409 | An active rating already exists for the Purchase | Load/update/delete the existing rating instead. |
| `IDENTITY_UNAVAILABLE` | 503 | Required Identity lookup cannot be completed | Retry later; do not invent seller/client facts. |

# 15. Pagination / Filter / Sort Contract

All paged endpoints use zero-based `page` and `size`. Default `page` is `0`; default `size` is `10`; valid size is `1..50`. Values outside those bounds return `VALIDATION_ERROR`.

| Endpoint | Allowed filters | Sort | Response |
|---|---|---|---|
| Browse Products | `keyword` (trimmed case-insensitive Product-name substring), `categoryCode`, `minPriceVnd`, `maxPriceVnd` | Fixed `lastPublishedAt DESC, id DESC` | `PageResponse<MarketplaceProductListItemResponse>` |
| Own Products | Pagination only | Fixed `updatedAt DESC, id DESC` | `PageResponse<OwnerProductListItemResponse>` |
| Purchase History | Pagination only | Fixed `purchasedAt DESC, id DESC` | `PageResponse<PurchaseHistoryItemResponse>` |
| Product ratings | Pagination only | Fixed `createdAt DESC, id DESC` | `PageResponse<ProductRatingResponse>` |

`keyword` blank after trim is treated as absent. `categoryCode`, price values, and `minPriceVnd <= maxPriceVnd` are validated. There are intentionally no technology, AI-capability, Expert-rating, status, arbitrary sort, full-text, or price-currency filters.

# 16. Kafka Event Contract

Only one event is justified by an MVP consumer: it creates a seller notification after a successful Purchase. Product publication has no approved MVP consumer, so no publication event is added.

## `AI_PRODUCT_PURCHASED`

| Item | Contract |
|---|---|
| Topic | `neural-bridge.ai-product.purchased` |
| Kafka key | `recipientUserId` (the seller Expert UUID), matching the existing notification publisher convention. |
| Producer | `ai-available-service` |
| Intended consumer | `notification-service` |
| Emit point | Only after the local `ProductPurchase` transition to `SUCCESS` has committed. |
| Recipient | Seller Expert UUID. |
| Sender | Buyer Client UUID/display name resolved for this successful Purchase. |

Payload JSON is lower camel case and retains the current notification consumer's recognized fields while adding stable event and purchase references:

```json
{
  "eventId": "UUID",
  "eventType": "AI_PRODUCT_PURCHASED",
  "domain": "ai-place",
  "occurredAt": "2026-10-02T05:20:00Z",
  "recipientUserId": "seller Expert UUID",
  "recipientName": "seller display name",
  "senderUserId": "buyer Client UUID",
  "senderName": "buyer display name",
  "resourceId": "Product UUID",
  "purchaseId": "ProductPurchase UUID",
  "productId": "Product UUID",
  "productName": "snapshot product name",
  "customTitle": "AI Place product purchased",
  "customMessage": "Your AI Place product \"Vietnamese Document Classifier\" was purchased.",
  "customDeeplink": "/ai-place/seller/products/{productId}/purchases/{purchaseId}"
}
```

No storage bucket/key, signed URL, protected asset metadata, wallet balance, or payment amount appears in the event. The current .NET notification consumer recognizes `eventType`, recipient/sender fields, `resourceId`, custom title/message/deeplink, and `occurredAt`; its later change must add this topic to its subscription. `eventId` is included for future consumer deduplication even though the current consumer does not persist it.

Delivery is best-effort in MVP: publish is attempted after commit, and a Kafka/notification failure is logged and does not roll back a successful Purchase. Because the approved database remains four business tables and has no Outbox, a crash between commit and publish can lose a notification and producer/consumer retries can duplicate one. This is explicitly accepted MVP behavior, not hidden distributed-transaction behavior.

# 17. Idempotency & Failure-Recovery Rules

| Operation | Contract expectation |
|---|---|
| Create Draft | No durable idempotency guarantee under the approved four-table model; UI prevents duplicate submit and resolves an ambiguous outcome before retry. Do not claim an in-memory cache is durable idempotency. |
| Publish | Target-state idempotent: already `PUBLISHED` returns the current response without timestamp mutation. |
| Unpublish | Target-state idempotent: already `UNPUBLISHED` returns the current response without mutation. |
| Buy/retry | The unique `(productId, buyerClientId)` Purchase row is the stable intent. `ProductPurchase.Id` is passed as Payment correlation/idempotency key. Confirmed non-money-moving outcomes permit current-price/context retry after Product revalidation; authoritative/uncertain outcomes cannot be overwritten or cause a second debit. |
| Upload confirmation | Same current authorization token/object/version/metadata confirmation returns the existing attachment. Any changed, expired, cross-slot, cross-Product, or arbitrary object fails. |
| Rating create | The unique `purchaseId` rating relation prevents a duplicate. A second create returns `RATING_ALREADY_EXISTS`; update is the explicit mutation operation. |

Commands use normal local transactions, deterministic Product/Purchase row locking, unique constraints, and service-layer state checks. They do not attempt a distributed database/Payment/Supabase transaction.

## Remote Payment succeeds but local SUCCESS persistence fails

1. The pending Purchase row is committed before settlement and supplies `ProductPurchase.Id` to Payment.
2. Payment durably records the idempotency key and returns `SUCCESS` with the main transaction ID, actual financial amounts, settlement instant, and the original immutable snapshot context.
3. If ai-available-service fails before its `PENDING_PAYMENT -> SUCCESS` transaction commits, the row remains pending locally but Payment must not be called with a new identity or materially different request until reconciliation establishes that this is safe.
4. On the next Buy/retry or a later reconciliation job, ai-available-service first calls `GetAiProductPurchaseSettlement(purchaseId)`.
5. If Payment says `SUCCESS`, ai-available-service validates the returned whole-VND fee/net values and persisted snapshot context, then completes the existing local Purchase exactly once using Payment's `settledAtUtc` as `purchasedAt` and derives the 30-day expiry. It does not re-debit or replace the snapshot with a later edited Product.
6. If Payment confirms `NOT_FOUND` or another explicit no-money-moving retryable outcome, ai-available-service revalidates the currently published Product and may make a normal same-key settlement attempt using its current price/context. If lookup reports `IN_PROGRESS`, `SUCCESS`, or remains unavailable/unknown, it must not send a materially different retry; it returns `PAYMENT_SERVICE_UNAVAILABLE` as appropriate and keeps the row pending for later reconciliation.

This recovery obligation is why the proposed Payment lookup RPC and echoed snapshot context are part of the contract.

# 18. Supabase Contract Summary

| Concern | Contract |
|---|---|
| Upload authorization | An owner-only endpoint validates Product state, picks a private object identity with an unpredictable nonce, and returns short-lived browser-direct upload authorization plus a tamper-evident `uploadToken`. The browser never chooses a bucket/key. |
| Metadata confirmation | The browser supplies the token and optional object version after upload. The backend validates token scope/expiry/current owner, queries Supabase for that exact object, validates canonical metadata and trusted Preview duration, then persists only approved Preview/asset metadata. No arbitrary object can be adopted. |
| Marketplace preview access | Product reads return derived `PublicPreviewResponse.accessUrl` and expiry, not stored permanent URLs or raw object identities. Preview is public marketing media but still has no durable database URL requirement. |
| Protected download access | A Client must pass the complete Purchase/Product/asset/entitlement authorization check. The backend returns a short-lived protected read URL that cannot outlive entitlement; no permanent public protected URL exists. |

Supabase owns file bytes. ai-available-service owns only the durable validated references/metadata allowed by the approved Product and ProductAsset model. Browser upload URLs/tokens are not durable Product metadata and must never be put in Kafka, Purchase History, or general Product responses.

# 19. Security / Data Exposure Rules

| Endpoint category | Authentication / role | Owner scope | Storage data that may be returned | Must never be returned |
|---|---|---|---|---|
| Expert Product management | Authenticated active EXPERT | Product seller only | Preview/asset display metadata only | Protected bucket/key, permanent asset URL, Supabase service credential. |
| Expert upload authorization | Authenticated active EXPERT | Product seller only | Short-lived URL/token, generated bucket/key, required headers, expiry, only to the authorized owner browser | Any credential capable of broader bucket access; another Product's key; permanent URL. |
| Expert upload confirmation | Authenticated active EXPERT | Product seller only | Safe Preview/asset metadata and derived Preview access URL | Raw protected key/bucket/credential. |
| Marketplace browse/detail | Authenticated CLIENT | Published listings only | Product marketing data, derived Preview access URL, Product Detail basic seller summary | Protected assets, protected bucket/key, upload URLs/tokens, seller private profile/contact fields. |
| Purchase History/detail | Authenticated CLIENT | Buyer only | Immutable commercial snapshot, entitlement state, asset type availability | Protected storage identity or persistent access URL; seller wallet/internal Payment entries. |
| Protected download | Authenticated CLIENT | Buyer + Product + successful unexpired Purchase | One temporary access URL plus safe file display metadata | Permanent URL, bucket/key, Supabase credential, another asset's access. |
| Ratings | Authenticated CLIENT | Successful purchaser for mutation | Public rating content/aggregate | Client wallet code, email, protected asset data. |
| Kafka purchase event | Internal producer/consumer only | Seller recipient | Minimal notification display data and Product/Purchase UUIDs | Object paths, signed URLs, asset metadata, financial/wallet secrets. |

Every owner-sensitive operation verifies the path resource against the authenticated subject; IDs in a URL are not authorization. The service should return safe business errors and log internally with correlation/request IDs without leaking exception details to clients.

# 20. Contract-to-Domain Traceability

| API / Integration | Entity / Domain Rule | External Dependency |
|---|---|---|
| Draft create/update/delete/publish/unpublish | `ai_product`; required Draft fields; lifecycle; first-publish marker; no delete after publication | Identity `GetUser` for active Expert validation. |
| Preview authorization/confirm/remove | Nullable one-slot Preview columns; IMAGE/VIDEO; video max 240 seconds; no Published edit | Supabase direct upload/metadata and trusted media inspection. |
| Protected asset authorization/confirm/remove | `product_asset`; one of four typed slots; unique object identity; immutable after first Publish | Supabase direct upload/metadata. |
| Browse/Product Detail | Published Product discovery; metadata-only technologies/capabilities; basic seller summary | Identity `GetUser` only for Product Detail seller. |
| Buy/retry | `product_purchase`; stable pending intent; unique Client/Product; current Product price after confirmed no-money-moving outcomes; no self-purchase; frozen authoritative settlement identity | Identity Client/seller lookup; Payment gRPC settlement/reconciliation. |
| Purchase History/Detail | Immutable success snapshot and 30-day entitlement; nullable active Rating on Buyer-owned Detail; preview deliberately not snapshotted | None for snapshot reads. |
| Protected download | `product_purchase SUCCESS` + buyer/product/expiry checks; Product status irrelevant after purchase | Supabase temporary protected read access. |
| Rating APIs | `product_rating` one active rating per successful Purchase; client may edit/hard-delete | None; explicitly not `rate-service`. |
| `AI_PRODUCT_PURCHASED` | Successful Purchase notification after local commit | Kafka and notification-service. |
| Payment reconciliation lookup | `ProductPurchase.Id` as correlation identity; retryable no-money-moving versus frozen authoritative settlement state; exact-once local success transition after remote success | Future Payment gRPC extension. |

# 21. Deferred / Out-of-Scope Contracts

The following contracts are intentionally absent from MVP:

- Admin moderation, approval, suspension, or administration of Product categories.
- Report, dispute, refund, chargeback, or payment reversal flows.
- Product versions, post-first-publish deliverable replacement, licensing tiers, subscriptions, or renewals.
- Project escrow, JobPost, Workspace, proposal payment, and Expert-rating integrations.
- Full Expert Profile aggregation, batch Identity lookup, seller performance/rating filters, or technology/capability marketplace filters.
- Download analytics, download logs, download limits, or a durable download-token table.
- An Outbox table or guaranteed notification delivery.
- A durable general-purpose REST idempotency-key store for Draft creation or rating creation.

# 22. Genuine Open Questions / Risks

| Item | Status / impact |
|---|---|
| Canonical `categoryCode` vocabulary | Required application input is not defined by approved phases. It must be supplied before implementation so validation is consistent; it does not block this contract. |
| Supabase bucket names, storage policies, upload URL primitive, and trusted video-duration inspector | Required infrastructure configuration/implementation detail. The contract fixes authorization, identity validation, and data exposure regardless of provider SDK choice; it does not block API design. |
| Payment extension and its durable idempotency/reconciliation storage | Required before Purchase implementation. The existing Payment proto/service has escrow RPCs but no AI Product settlement. This is the sole implementation dependency for settlement, not a Phase 4 contract blocker. |
| Notification-service subscription/template update | Required later for delivery of the retained topic; current consumer has a fixed topic list. Purchase success remains correct if notification is unavailable. |
| No batch Identity lookup | Known performance risk only if future browse cards require seller data. This contract avoids the N+1 path by returning seller summary on Detail only. |
| Four-table MVP and Draft create idempotency | There is no durable idempotency-request record without changing the approved persistence model. The contract explicitly makes creation non-retry-idempotent rather than adding an unapproved table. |

There is no genuine blocker to approving this API/integration contract. The later Payment extension is a required implementation dependency already identified by Phase 2.
