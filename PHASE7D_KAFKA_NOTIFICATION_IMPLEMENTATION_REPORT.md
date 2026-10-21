# 1. Implementation Summary

Phase 7D adds a best-effort, post-commit Kafka notification integration for authoritative AI Product Purchase success.

- `ai-available-service` publishes `AI_PRODUCT_PURCHASED` to `neural-bridge.ai-product.purchased` only after the local `ProductPurchase` success transaction commits.
- The publish path is shared by immediate settlement success and retry/reconciliation success.
- A Purchase already locally marked `SUCCESS` returns its existing result without a new publish attempt.
- Kafka delivery failure is caught and logged without rolling back Payment, ProductPurchase success, entitlement, or the HTTP success result.
- `notification-service` consumes the topic, validates the versioned contract, creates a Client purchase-success notification, and suppresses duplicate delivery.

Payment, Supabase storage workflows, the AI Available four-table schema, and Docker Compose were not changed by this phase.

# 2. Targeted Kafka/Notification Audit

- Kafka was already present in `docker-compose.yml`, using `localhost:9092` for host access and `kafka:29092` for Docker-networked services. No Compose change was required.
- Existing Java producers use JSON with a string recipient-user-id key through `common-starter`'s `NotificationEventPublisher`.
- The active `notification-service` is .NET 8, not the legacy Java `notification-service-backup`. It uses Confluent.Kafka 2.4.0, a fixed topic list, and a JSON string consumer.
- The consumer previously used automatic commits and had no persisted event identity or deduplication. Notification persistence already existed in PostgreSQL through `notifications`.
- Existing retry handling re-created consumers after fatal failures but had no message-level retry/commit boundary. The consumer now commits only after processing (or safe rejection) and seeks the failed record before retrying.

# 3. Files Changed

- `ai-available-service/ai-available-service.csproj`
- `ai-available-service/appsettings.json`
- `ai-available-service/Program.cs`
- `ai-available-service/Application/PurchasePaymentApplicationService.cs`
- `ai-available-service/Infrastructure/Identity/IdentityUser.cs`
- `ai-available-service/Infrastructure/Identity/IdentityGrpcDirectory.cs`
- `ai-available-service/Infrastructure/Kafka/AiProductPurchaseEventPublisher.cs`
- `ai-available-service/Infrastructure/Kafka/AiProductPurchasedEvent.cs`
- `ai-available-service/Infrastructure/Kafka/KafkaOptions.cs`
- `notification-service/Consumers/NotificationKafkaConsumer.cs`
- `notification-service/Models/DTOs/Events/AiProductPurchasedEvent.cs`
- `notification-service/Models/Entities/Notification.cs`
- `notification-service/Data/NotificationDbContext.cs`
- `notification-service/Repositories/INotificationRepository.cs`
- `notification-service/Repositories/NotificationRepository.cs`
- `notification-service/Services/INotificationService.cs`
- `notification-service/Services/NotificationService.cs`
- `notification-service/Program.cs`
- `notification-service.tests/NotificationServiceLogicTests.cs`

# 4. Event Contract

Topic: `neural-bridge.ai-product.purchased`  
Type: `AI_PRODUCT_PURCHASED`  
Version: `1`  
Key: `buyerId` as a string, matching the existing recipient-oriented key convention.

The lower-camel JSON event has:

- Envelope: `eventId`, `eventType`, `eventVersion`, `occurredAtUtc`.
- Purchase: `purchaseId`, `productId`, `productName`, `purchasedAt`, `accessExpiresAt`.
- Buyer: `buyerId`, `buyerCode`, `buyerEmail`.
- Seller: `sellerExpertId`, `sellerExpertCode`, `sellerDisplayName`.
- Snapshot money: `priceVnd`, `platformFeeVnd`, `expertNetVnd`.

`eventId` is a deterministic SHA-256-derived identifier based on `AI_PRODUCT_PURCHASED` plus `purchaseId`; no AI event-tracking table is needed. The payload is a dedicated DTO, never an EF entity. It contains no token, wallet secret, storage key, bucket, or signed URL.

# 5. Producer Design

`IAiProductPurchaseEventPublisher` separates `PurchasePaymentApplicationService` from Confluent.Kafka. `KafkaAiProductPurchaseEventPublisher` is a singleton Confluent.Kafka 2.4.0 producer that serializes the dedicated DTO using camel-case JSON. Kafka configuration is resolved from `KAFKA_SERVERS` first, then `Kafka:BootstrapServers`.

Buyer email is obtained from the existing Identity gRPC response solely to satisfy the event contract. It is never emitted in producer or consumer logs and is not used in notification content.

# 6. Publish Timing

`CompleteSuccessAsync` commits the local ProductPurchase `SUCCESS` transaction before calling the publisher. It publishes only when that method changed the local pending Purchase to `SUCCESS` in that invocation. The existing-local-success branch returns without publishing.

Both a direct settlement `SUCCESS` and a reconciliation/retry `SUCCESS` flow through the same method. Kafka is not involved in Payment settlement or the EF transaction.

# 7. Failure Semantics

The producer logs attempted, succeeded, and failed-best-effort outcomes using only event ID, purchase ID, product ID, topic, and error category. It catches broker, delivery, cancellation, and unexpected producer errors. Therefore Kafka failure cannot change Payment settlement, ProductPurchase status, entitlement, or a successful HTTP response.

There is intentionally no Kafka transaction, distributed transaction, Outbox, Saga, AI event table, or retry-induced financial behavior.

# 8. Consumer Design

The active .NET notification consumer subscribes to the new topic along with its existing topics. For this topic it deserializes the dedicated DTO and rejects malformed, unsupported type, unsupported version, missing required fields, invalid money snapshots, or invalid expiry safely while keeping the consumer alive.

For a valid event, it creates a Client notification containing the product name, confirmation of success, expiry timestamp, and a Purchase deeplink. Seller notification was not added because the existing notification domain only needed the required Client confirmation for this phase.

# 9. Consumer Idempotency

Notification-owned persistence now has nullable `event_id` and `purchase_id` columns. Startup safely adds those columns and creates two partial unique indexes for existing `EnsureCreated` deployments:

- `(event_id, recipient_user_id)` for stable source-event identity.
- `(type, purchase_id, recipient_user_id)` where `purchase_id` is present, which directly protects `AI_PRODUCT_PURCHASED` recipient delivery.

The service first checks the business tuple, then relies on the database uniqueness constraint to cover a race. A uniqueness collision returns `Duplicate`; cache and WebSocket delivery are skipped. This is Notification-local only and creates no cross-service dependency.

# 10. Kafka Configuration

`ai-available-service/appsettings.json` supplies the non-secret defaults:

```json
"Kafka": {
  "BootstrapServers": "localhost:9092",
  "AiProductPurchasedTopic": "neural-bridge.ai-product.purchased"
}
```

`KAFKA_SERVERS` overrides the bootstrap server. Docker deployment should use the existing `kafka:29092`; host-local development uses `localhost:9092`. Topic name validation prevents an accidental producer topic drift. The notification consumer now also respects `KAFKA_GROUP_ID`/`Kafka:GroupId` while retaining `notification-service` as the default.

# 11. Security

- No JWT, service token, broker credential, wallet secret, Supabase bucket/object key, or signed URL is published.
- Structured logs omit buyer email and never print complete event payloads.
- The consumer uses validated contract fields only; `buyerEmail` is not persisted in a notification or used in its display text.

# 12. Database Impact

AI Available: no database schema change and no EF migration.

Notification: small Notification-owned schema extension to the existing `notifications` table: nullable `event_id`, nullable `purchase_id`, and the two idempotency indexes described above. Existing non-purchase notifications remain compatible.

# 13. Build Results

- `dotnet restore` completed for AI Available, Notification Service, and Notification tests after approved NuGet access.
- `dotnet build ai-available-service/ai-available-service.csproj --no-restore` completed successfully with 0 warnings and 0 errors using an isolated output directory because an existing local `AiAvailableService.exe` held the default output executable open.
- `dotnet build notification-service/NotificationService.csproj --no-restore` completed successfully with 0 warnings and 0 errors using the same isolated-output validation approach.
- `dotnet build notification-service.tests/NotificationService.Tests.csproj --no-restore` completed successfully with 0 warnings and 0 errors. The test suite includes added Client-notification creation and duplicate-skip coverage.

# 14. Runtime Results

End-to-end runtime scenarios A-F were not executed. Docker Desktop access confirmed that PostgreSQL, Redis, Identity, Payment, Discovery, and Gateway are running, but Kafka and the active Notification service are not running. AI Available is also not represented as a running Compose service in the current stack.

No containers were started or rebuilt solely for this phase validation. A direct VSTest execution did not complete within the execution window; the affected test project itself compiled successfully.

# 15. Deferred Phase 7E Work

- Durable delivery/replay guarantees via an approved Outbox design.
- Kafka retry/DLQ policy and operational alerting.
- Optional Expert sale notification, if product requirements specify its recipient experience.
- Full Compose/runtime orchestration for AI Available and automated Kafka integration tests.

# 16. Genuine Blockers

There is no source or architecture blocker to Phase 7D implementation.

Runtime verification is environment-blocked: the current Compose stack has no running Kafka, Notification, or AI Available service. Starting/rebuilding those services and creating live settlement data was not performed as part of this implementation task. The VSTest runner also did not complete in the available execution window, although all affected test code builds cleanly.
