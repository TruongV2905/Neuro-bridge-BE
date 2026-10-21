# 1. Executive Summary

This is a source-only audit conducted after Phase 6. The requested `PHASE6_CORE_MVP_IMPLEMENTATION_REPORT.md` is not present under that exact name; the repository contains `PHASE6_CORE_MVP_IMPLEMENTATION_REPORT_FINAL.md`, which was read as the current Phase 6 closure report.

The approved AI Place four-table schema already has the storage identity and stable Purchase intent needed for Phase 7. **No `ai-available-service` schema or migration change is required.** The nine routes are structurally feasible, but Purchase creation/retry is blocked until Payment adds a secure, durable, idempotent AI Product settlement/reconciliation capability and extends the authoritative proto. Storage requires deployment of a usable Supabase server credential and bucket policies. Kafka notification is non-blocking and feasible once the producer and Notification subscription are added.

Key findings:

- `common-starter/src/main/proto/payment.proto` is authoritative. It exposes only project escrow RPCs today.
- The current Payment service can move money through `PaymentTransactionService.payment(...)`, but has no AI Product reference/type, no durable idempotency/correlation record, no settlement lookup, and no locking/versioning of wallet rows.
- The existing proposal path demonstrates a 90/10-style split, but it is not a safe reusable AI Product contract: it is caller-identity based, not correlation-idempotent, and its wallet read/update operations are not pessimistically locked.
- AI Available has bucket names/configuration and an `IObjectStorageService` boundary, but no Supabase SDK/package, adapter, controller route, storage credential deployment, or frontend in this repository.
- Notification consumes a fixed list of ten topics using `Confluent.Kafka` 2.4.0. It accepts the Phase 4 JSON payload fields it recognizes and ignores unknown fields, but it does not subscribe to the AI topic and does not persist/deduplicate `eventId`.

# 2. Current Payment Architecture

## Authoritative proto and consumers

The authoritative definition is [payment.proto](D:/FA2026/PRN232/BE/common-starter/src/main/proto/payment.proto). `common-starter` compiles proto definitions into its Java artifact; `payment-service`, `jobPost-service`, and `workspace-service` use the resulting `com.payment.grpc` generated classes through their dependency on `common-starter`. `ai-available-service` links the same source file directly in its project file and generates a .NET client; its Dockerfile explicitly copies the file into the build context. No other direct `payment.proto` copy was found.

The current `PaymentService` RPC inventory is exactly:

| RPC | Current use |
|---|---|
| `PayProjectEscrow` | creates/funds a project escrow or returns a shortfall/pending result |
| `ReleaseEscrow` | releases an existing escrow to its expert |

Neither RPC is suitable for an immediate AI Product purchase: both are project/escrow semantics, neither accepts a permanent purchase correlation key, and neither returns a reusable 90/10 immediate-settlement result.

## Wallet and transaction model

`UserWallet` uses an internal UUID primary key, but all business lookup and money-transfer methods identify wallets by `userCode`. The platform wallet is found as the first wallet whose `userCode` begins with `ADM`; it is not represented by a dedicated platform-wallet identifier/configuration. `PaymentTransaction` has its own generated UUID primary key and string `referenceId`, but has no uniqueness constraint/index for a reference or idempotency key.

Current values are:

| Domain | Values |
|---|---|
| `TransactionType` | `WALLET_DEPOSIT`, `WALLET_WITHDRAW`, `PROPOSAL_PAYMENT`, `PROJECT_ESCROW_DEPOSIT`, `PROJECT_RELEASE`, `PROJECT_REFUND`, `REFUND`, `SYSTEM_FEE`, `ADMIN_ADD`, `ADMIN_DEDUCT` |
| `PaymentReferenceType` | `PROPOSAL`, `PROJECT`, `JOB_POST`, `WITHDRAW_REQUEST`, `DEPOSIT_REQUEST`, `ADMIN_ACTION` |
| `TransactionStatus` | `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED` |
| `WalletStatus` | `ACTIVE`, `INACTIVE`, `LOCKED`, `PENDING_VERIFICATION` |

`PaymentTransactionService.create(...)` only maps and saves a transaction row. It does **not** debit or credit a wallet. The method which actually combines movement and a transaction record is `PaymentTransactionService.payment(...)`:

1. `withdraw(fromUserCode, deductibleAmount)` debits an active source wallet and increments its `totalWithdrawn`.
2. It either deposits to an active destination wallet, or deposits to project escrow when `referenceType == PROJECT`.
3. It creates a `SUCCESS` transaction with a generated UUID when `transactionId` is null, or marks an existing transaction `SUCCESS` when an ID is supplied.

Missing wallets throw `GlobalException("Wallet not found...")`; any non-`ACTIVE` wallet throws `GlobalException("Wallet is not active")`; and a debit whose balance is lower than the amount throws `GlobalException("Wallet balance is not enough")`. The `PayProjectEscrow` RPC has an explicit pre-check that converts insufficient balance to `PENDING` plus a shortfall; generic wallet movement throws instead.

The current 90/10 example is `transactionProposalUnlockByWallet(...)`: it calculates a 10% `HALF_UP` fee, debits the client once for the gross amount while crediting the expert net amount, then records a `SYSTEM_FEE` transaction and credits the `ADM*` wallet while debiting zero. It returns the generated UUID of the main proposal transaction; the fee transaction has its own generated UUID. This is a useful behavioral reference only, not a product-purchase contract.

For ID clarity: JPA/Hibernate generates `PaymentTransaction.id` and `UserWallet.id` UUIDs. `payment(...)` returns the new main transaction UUID when it creates one; its existing main-row `amount` is the received amount when present (therefore the proposal main row records the expert net, not the gross). `PayProjectEscrow` returns an escrow UUID, not a transaction UUID, and `ReleaseEscrow` returns its generated release-transaction UUID.

## Transaction, concurrency, and gRPC security observations

The money methods are annotated `@Transactional`, so the debit, credit(s), and rows within a successful service invocation share one local database transaction. A later exception normally rolls those changes back. However, `UserWalletRepo.findByUserCode(...)` is an ordinary repository read, `UserWallet` has no `@Version`, and no `PESSIMISTIC_WRITE`/`SELECT FOR UPDATE` repository query was found. Concurrent debits to the same wallet therefore lack an explicit database locking control and should not be relied on for financial correctness.

There is no reusable idempotency table/key. `PaymentTransaction.referenceId` is non-unique and generic transaction creation is not a settlement ledger. The escrow record supplies a project-specific duplicate guard, but cannot model current-price retryable product outcomes or settlement lookup.

The common starter installs a gRPC JWT authentication reader and supports `ROLE_SERVICE`; `WalletGrpcServiceImpl.createWallet` explicitly requires that role. In contrast, `PaymentGrpcServer`'s `PayProjectEscrow` and `ReleaseEscrow` methods have no `@PreAuthorize` restriction. The current AI Payment client registration also attaches no authorization metadata. Phase 7 must make the new Payment RPC service-only and supply the corresponding service-auth mechanism; a browser JWT must not be treated as authority to settle an arbitrary wallet request.

# 3. Payment Gap Analysis

The required additions are `AI_PRODUCT_PAYMENT` in `TransactionType` and `AI_PRODUCT` in `PaymentReferenceType`. The reference ID should be the `ProductPurchase.Id` UUID string. These do not exist today.

The Phase 4 conceptual `SettleAiProductPurchase` and `GetAiProductPurchaseSettlement` RPCs fit the current Java/gRPC architecture cleanly, but not as a thin call to the existing generic `payment(...)` method. Payment needs a durable Payment-owned settlement record keyed uniquely by `product_purchase_id`, holding:

- status (`SUCCESS`, durable `IN_PROGRESS`, explicit no-money-moving outcomes, or not found);
- immutable material input once in progress/success (product, client/expert codes, gross amount, snapshot context);
- main and fee transaction UUIDs, authoritative gross/fee/net amounts, and `settledAtUtc` on success;
- enough outcome/error data for safe reconciliation.

This is a **Payment-side** persistence change, not a fifth AI table. It is required because a `payment_transaction` row cannot distinguish an unattempted Purchase from a confirmed no-movement result, cannot enforce one settlement per Purchase, and cannot reconstruct the frozen success context after local AI persistence fails.

New settlement code must lock source, expert, and platform wallet rows in a canonical order, validate all three are active and the source has funds before changing any balance, calculate `ROUND(gross * 0.10, 0)` with positive half ties up, debit gross once, credit net and fee, persist transaction rows/settlement result, and commit atomically. It must never use escrow. A unique settlement key plus row locking must make concurrent same-Purchase requests return/reconcile the same authoritative record rather than double debit.

# 4. Recommended Payment Phase 7 Change

1. Extend the authoritative proto `PaymentService` with the two Phase 4 RPCs and messages/statuses. Regenerate the Java server and .NET client from that one source.
2. Add a Payment-owned AI Product settlement entity/table with unique `product_purchase_id`; add repository locking queries and a settlement service.
3. Add the two enum values and a migration/controlled schema update for Payment (its current `ddl-auto: update` does not replace a reviewed financial migration strategy).
4. Apply service-only authorization to the new gRPC methods and add a .NET gRPC client interceptor/credential mechanism for it.
5. Implement a transactional immediate 90/10 transfer using locked wallets and deterministic transaction records, returning the main transaction UUID.
6. Implement lookup from the durable settlement record. `SUCCESS` and `IN_PROGRESS` freeze material input; explicit `INSUFFICIENT_BALANCE`, `WALLET_NOT_ACTIVE`, `NOT_FOUND`, and an explicitly guaranteed no-movement `PAYMENT_FAILED` do not.

# 5. Current Supabase / Storage Architecture

There is no frontend project, `package.json`, JavaScript/TypeScript Supabase client, or browser upload helper in the supplied repository. Consequently, no installed Supabase JS SDK/version, current frontend bucket usage, `getPublicUrl`, signed upload/download helper, delete/move helper, or frontend file-metadata type can be verified from current source. The existing Workspace frontend pattern described in authoritative documents is not present here for source inspection.

The AI service contains only Phase 5/6 foundations:

- `SupabaseOptions` has `Url`, `ServiceRoleKey`, `PreviewBucket`, and `ProtectedAssetBucket`.
- `appsettings.json` supplies placeholder empty URL/key values and the planned bucket names `ai-place-previews` and `ai-place-protected`.
- `IObjectStorageService` declares future direct-upload authorization, trusted object metadata lookup, temporary read URL, and delete operations.
- No implementation is registered in DI, no Supabase package is referenced by the project, and no route calls the interface.
- `AiProduct` already persists nullable preview `storageBucket`/`storageObjectKey` plus safe metadata. `ProductAsset` already persists required protected `storageBucket`/`storageObjectKey` plus metadata, with a unique bucket/key constraint.

The names imply separate public-preview and protected-asset buckets, but source cannot establish that either actual Supabase bucket exists, is public/private, or has an RLS/storage policy. No permanent public URL is stored in the entity model.

Only the `Supabase:ServiceRoleKey` configuration name exists in the AI service; it is blank in checked-in `appsettings.json`, no `SUPABASE*` entry exists in the repository `.env`, and no other server credential configuration was found. Its value is intentionally not reported.

# 6. Storage Gap Analysis

The approved columns are sufficient; no schema change is needed to separate preview and protected assets. Use `PreviewBucket`/`ProtectedAssetBucket` or, if infrastructure requires one bucket, distinct validated object prefixes. The durable identity is exactly the authorized `storageBucket` + `storageObjectKey` persisted in the existing preview columns or `product_asset` row, never an upload URL or a public URL.

There is currently no signed upload or signed download implementation. An appropriate server-capable Supabase client/Storage REST adapter can support browser-direct upload by obtaining a short-lived signed upload authorization, provided the backend creates the key and authorization and the bucket policy permits only that operation. This is not supplied by the present code or any installed package.

The server needs a Supabase service-role/server credential (or a narrowly scoped server credential with equivalent Storage Admin abilities) to create signed upload authorization, list/stat/verify exact objects and ETags, issue protected temporary reads, and remove unassociated/replaced objects. Browser credentials cannot safely perform those authority functions.

Trusted preview-video duration inspection is also absent. The client-provided duration cannot satisfy the approved contract; Phase 7 needs a server-side inspector or approved worker/tool that operates on the exact authorized object.

# 7. Recommended Supabase Phase 7 Design

Implement one `SupabaseObjectStorageService` behind the existing interface, without proxying file bytes through ASP.NET Core:

1. Validate active Expert ownership/state; backend chooses the configured bucket and an unpredictable key such as `ai-place/{expertId}/{productId}/preview/{nonce}` or `.../assets/{assetType}/{nonce}`.
2. Sign a short-lived upload authorization and a tamper-evident upload token containing caller, Product, slot/type, bucket/key, expected content type/size, expiry, and nonce. Return the bucket/key only to that owner during authorization.
3. On confirmation, validate the token then stat the exact object through Supabase. Persist only trusted metadata and durable object identity in a locally locked Product transaction. Never accept browser-supplied bucket/key.
4. Issue Preview display access at read time; issue protected access only after buyer/Purchase/Product/type/SUCCESS/strict-expiry checks, expiring at `min(now + 5 minutes, accessExpiresAt)`.
5. Keep preview separate from the protected `VIDEO` asset. Enforce Draft/never-published asset mutation and rely on the existing asset-lock trigger after first publish. Delete/replacement cleanup is best effort after metadata changes; abandoned objects require lifecycle/operational cleanup.

# 8. Current Kafka / Notification Architecture

`notification-service` uses `Confluent.Kafka` **2.4.0**. Its `BackgroundService` subscribes to a fixed in-code list, not to an appsettings topic list:

1. `neural-bridge.package.submitted`
2. `neural-bridge.package.resubmitted`
3. `neural-bridge.package.approved`
4. `neural-bridge.package.rejected`
5. `neural-bridge.workspace.invited`
6. `neural-bridge.jobpost.applied`
7. `neural-bridge.jobpost.accepted`
8. `neural-bridge.contract.sign-required`
9. `neural-bridge.contract.fully-signed`
10. `neural-bridge.project.completed-payment-released`

Bootstrap configuration is `KAFKA_SERVERS` when set, otherwise `Kafka:BootstrapServers`, otherwise `localhost:9092`. Although `Kafka:GroupId` exists in settings, the code hard-codes `notification-service`. The consumer uses `ConsumerBuilder<string,string>`, `PropertyNameCaseInsensitive = true`, `AutoOffsetReset = Earliest`, `EnableAutoCommit = true`, and `AllowAutoCreateTopics = true`. It also attempts to create each fixed topic with one partition/replica through the Admin client.

`NotificationEvent` understands `eventType`, `domain`, recipient/sender IDs and names, `resourceId`, `workspaceId`, `resourceVersion`, custom title/message/deeplink, feedback, and `occurredAt`. JSON unknown fields are tolerated by `System.Text.Json` by default, so Phase 4's additional `eventId`, `purchaseId`, `productId`, and `productName` fields are harmless. Case-insensitive names are explicitly enabled.

The recipient is `recipientUserId`; sender is available as `senderUserId`/`senderName`; title/message/deeplink use complete custom fields when both custom title and message are supplied, otherwise an event-type template. The Phase 4 payload supplies all three custom display fields, so it does not require a new template to display correctly.

On a successful consume, the service persists a notification, then updates Redis and sends a WebSocket message. Processing exceptions are logged and the loop waits one second; consumer-level failures recreate the consumer after five seconds. There is no explicit retry policy, manual commit-after-persistence, dead-letter topic, retry topic, or event deduplication. With auto commit, persistence/cache/WebSocket failure can result in loss or replay depending on timing. `Notification` has no `eventId` column and `eventId` is not persisted.

# 9. Notification Gap Analysis

Notification must add `neural-bridge.ai-product.purchased` to its fixed `Topics` array; this automatically includes it in its Admin-client create and subscription behavior. No DTO change is required for basic display of the approved Phase 4 payload, because recognized fields already map correctly and unknown fields are ignored. A template entry for `AI_PRODUCT_PURCHASED` is optional only if custom title/message are ever omitted.

`ai-available-service` currently has no Kafka package/reference or producer. To match the current .NET ecosystem it should use **`Confluent.Kafka` 2.4.0**, serialize lower-camel JSON, and produce a string key of seller `recipientUserId`. It should publish best effort only after local Purchase success commit, log failure, and never reverse Purchase success. The approved four-table model intentionally has no outbox, so at-least-once/possibly-lost notification delivery remains an accepted MVP limitation.

The existing Java producer convention is `common-starter`'s `NotificationEventPublisher`: `KafkaTemplate<String,Object>`, string recipient-ID key, JSON serializer, and asynchronous failure logging. AI Available should preserve the observable JSON/key convention while using the matching .NET package above; it cannot reuse the Java publisher.

For host-local runs use `localhost:9092`. For Docker-networked services use `kafka:29092`, matching `docker-compose.yml` advertised listeners and its other service `KAFKA_SERVERS` values.

# 10. Recommended Kafka Phase 7 Design

- Add a small producer abstraction and `Confluent.Kafka` 2.4.0 to AI Available.
- Produce JSON to `neural-bridge.ai-product.purchased`, key `recipientUserId`, after the local `SUCCESS` commit only.
- Emit the approved Phase 4 payload: its core fields are compatible with the current consumer; no storage path, signed URL, payment amount, or wallet data belongs in it.
- Add the topic in `NotificationKafkaConsumer.Topics`. Optionally add a template for fallback display.
- Do not claim deduplication. If desired later, add `eventId` persistence plus a unique constraint to Notification in a separately approved change; it is not necessary to make purchase settlement correct.

# 11. Nine Endpoint Feasibility Matrix

| Route | External dependency | Current support | Required Phase 7 change | Blocker | AI DB change? |
|---|---|---|---|---|---|
| `POST .../preview/upload-authorizations` | Supabase signed upload | Options/interface only | storage adapter, signed token/key generator, controller/service/DTO | server credential + bucket policy | No |
| `PUT .../preview` | Supabase stat + duration inspection | preview columns/constraints | confirmation/token validation, metadata lookup, trusted duration, locked update | server credential + duration tool | No |
| `DELETE .../preview` | Supabase cleanup | nullable preview columns | owner/state command, best-effort object deletion | none after storage configured | No |
| `POST .../assets/{assetType}/upload-authorizations` | Supabase signed upload | asset table/lock trigger/interface | slot validation, generated identity/token, storage adapter | server credential + bucket policy | No |
| `PUT .../assets/{assetType}` | Supabase stat | asset persistence/unique constraints | confirmation, metadata validation, locked replace/idempotency | server credential | No |
| `DELETE .../assets/{assetType}` | optional Supabase delete | asset lock trigger | locked Draft-only removal and cleanup | none after storage configured | No |
| `GET .../download-access` | Supabase temporary protected URL | Purchase/asset metadata and expiry columns | entitlement checks plus storage temporary read | server credential/private policy | No |
| `POST .../products/{productId}/purchases` | Identity + Payment + Kafka | stable Purchase entity/constraints and Payment client foundation | buy service/controller/DTO, Payment settlement/reconciliation, optional post-commit event | Payment proto + durable settlement service | No |
| `POST .../{purchaseId}/retry-payment` | Identity + Payment | pending Purchase model only | reconciliation-first retry service/controller | Payment lookup/durable outcomes | No |

All nine are feasible within the approved AI data model. The first seven await storage operational configuration; the last two cannot be safely implemented until the Payment extension is complete.

# 12. Payment Retry / Recovery Verification

`ProductPurchase.Id` is already application-generated before remote settlement and the database already enforces one `(product_id, buyer_client_id)` row. It can therefore be the permanent correlation key without an AI `PaymentAttempt`/Saga table. The approved `PENDING_PAYMENT` shape deliberately leaves financial fields null and the `SUCCESS` check/unique payment transaction ID protects the one local terminal transition.

Current Payment cannot satisfy recovery by itself. It has no durable record keyed by Purchase ID, no lookup RPC, and no distinction between known non-money-moving failures and unknown after-call state. Phase 7 requires the Payment-side durable settlement record described in section 3. After an uncertain result, AI must lookup first: recover local success from Payment `SUCCESS`; only retry current Product price/context after an explicitly confirmed no-settlement/no-movement outcome; and keep pending/not send materially changed input after `IN_PROGRESS`, timeout, unavailable, or unknown. This meets the approved rule without adding an AI service table.

# 13. Security / Data Exposure Verification

The approved design remains achievable without weakening any requirement:

- browser-direct uploads use backend-generated object identity and short-lived authorization, never arbitrary browser bucket/key;
- Preview is a distinct optional public-marketing slot, separate from protected `VIDEO`;
- Product/asset rows already store durable identity only, not permanent public URLs;
- protected reads require entitlement validation and return temporary access only;
- existing first-publication marker plus asset trigger preserves protected-asset immutability;
- the four-table model remains intact;
- no protected bucket/key, service credential, or signed URL may be placed in general Product, history, or Kafka responses.

The implementation must additionally prevent the existing unguarded Payment gRPC pattern from becoming an AI settlement privilege escalation: Payment must authenticate/authorize service callers, and AI must not forward a client-controlled wallet identity as financial authority.

# 14. Files Expected to Change in Phase 7

Expected AI Available changes include the project file (Supabase/Kafka packages), `Program.cs` registrations/configuration, payment gRPC registration/interceptor and client wrapper, storage adapter, product/purchase application services, contracts, and Expert/Client controllers. Existing `IObjectStorageService`, `SupabaseOptions`, entity mappings, and the current `ProductPurchase` model should be extended through behavior, not an AI migration.

Expected Payment changes include `common-starter/src/main/proto/payment.proto`, `TransactionType`, `PaymentReferenceType`, a new Payment-side settlement entity/repository/migration, locking queries in wallet repository/service, settlement/reconciliation service, and `PaymentGrpcServer` security/implementation. Consumers that compile the shared proto—Payment, JobPost, Workspace, and AI Available—must regenerate/rebuild.

Expected Notification changes are `NotificationKafkaConsumer.cs` (topic list; optional consumer reliability improvements) and optionally `NotificationTemplate.cs`. AI Available needs new Kafka producer files. No notification schema change is necessary for baseline best-effort delivery; persisting/deduplicating `eventId` would be a separate Notification schema enhancement.

# 15. Database Impact

**`ai-available-service`: no migration or schema change is required.** The approved four tables already contain preview and protected-object identity, pending Purchase intent, local success snapshot, entitlement, and uniqueness controls required by Phase 7.

**Payment service:** a durable AI Product settlement/idempotency/reconciliation record and financial enum/schema updates are required. This is the correct location for the necessary new state.

**Notification service:** no schema change is required for the approved best-effort event. Adding `eventId` durability/deduplication would require a separate notification schema change and is not part of the approved four-table AI model.

# 16. Genuine Blockers

1. Payment has no AI Product settlement or lookup RPC and no durable correlation/idempotency record. Implementing buy/retry before this would risk double debit or unrecoverable ambiguity.
2. Payment wallet mutation lacks explicit locking/versioning and the Payment gRPC server lacks explicit service authorization on its current escrow RPCs. New financial RPCs must correct both before being exposed.
3. No usable Supabase server credential, bucket policy confirmation, or trusted preview-duration mechanism is present in source/configuration. Storage route implementation must wait for those deployment inputs.

Kafka/Notification is not a settlement blocker: Phase 4 intentionally makes it post-commit best effort. The missing frontend source prevents verification of its current helper/package, but does not prevent backend Phase 7 design.

# 17. Phase 7 Implementation Recommendation

1. Implement and test the Payment proto extension, secure service authentication, Payment-owned durable settlement record, canonical wallet locks, atomic 90/10 settlement, and reconciliation lookup.
2. Add AI Available Payment client/service and buy/retry reconciliation workflow; test uncertain-call recovery and same-Purchase current-price retry rules.
3. Provision Supabase credentials/policies and trusted media inspection; implement the storage adapter and then all preview/protected upload, confirmation, removal, and temporary-download routes.
4. Add the AI Kafka producer and Notification topic subscription; publish only after local success commit and verify it cannot affect successful purchase outcome.
5. Run end-to-end security/concurrency/recovery tests across all nine routes, including post-first-publish asset immutability and expired entitlement access denial.
