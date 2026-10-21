# 1. Implementation Summary

Phase 7B implements the Payment foundation for immediate AI Place Product purchases and the two AI Available endpoints:

- `POST /v1/ai-place/client/products/{productId}/purchases`
- `POST /v1/ai-place/client/purchases/{purchaseId}/retry-payment`

The authoritative Payment gRPC contract now provides immediate AI Product settlement and reconciliation lookup. Payment owns a new durable settlement record keyed by `ProductPurchase.Id`; AI Available continues to own only its approved pending/success Purchase state and immutable success snapshot. No Supabase, Kafka, Notification, asset, or download code was added.

# 2. Files Changed

## Shared contract

- `common-starter/src/main/proto/payment.proto`

## Payment service

- `constant/TransactionType.java`
- `constant/PaymentReferenceType.java`
- `constant/AiProductSettlementState.java`
- `entity/AiProductSettlement.java`
- `repository/AiProductSettlementRepo.java`
- `repository/UserWalletRepo.java`
- `service/AiProductSettlementService.java`
- `service/PaymentTransactionService.java`
- `grpc/PaymentGrpcServer.java`
- `resources/db/manual/phase7b_ai_product_settlement.sql`

## AI Available service

- `Contracts/PurchaseContracts.cs`
- `Infrastructure/Payment/InternalServiceTokenProvider.cs`
- `Infrastructure/Payment/PaymentSettlementGateway.cs`
- `Infrastructure/Payment/PaymentGrpcClientRegistration.cs`
- `Application/PurchasePaymentApplicationService.cs`
- `Controllers/ClientPurchasePaymentsController.cs`
- `Program.cs`

# 3. Proto Changes

`PaymentService` now declares:

- `SettleAiProductPurchase(AiProductSettlementRequest)`
- `GetAiProductPurchaseSettlement(AiProductSettlementLookupRequest)`

The settlement request carries the stable `product_purchase_id`, Product ID, client/expert wallet codes, whole-VND decimal-string gross amount, and the opaque recovery snapshot context. The response distinguishes `SUCCESS`, `IN_PROGRESS`, `INSUFFICIENT_BALANCE`, `WALLET_NOT_ACTIVE`, guaranteed-no-movement `PAYMENT_FAILED`, `NOT_FOUND`, and `IDEMPOTENCY_CONFLICT`.

On success it returns the main transaction UUID, gross/fee/net string amounts, UTC settlement instant, Product ID, and the original snapshot context. It does not reuse the Project Escrow RPCs.

# 4. Payment Settlement Persistence

`AiProductSettlement` is a Payment-owned entity/table with a unique `product_purchase_id` constraint. It persists:

- correlation/Product IDs and client/expert codes;
- gross, 10% rounded platform fee, and expert net amounts;
- current durable settlement state and safe outcome code/shortfall;
- main and platform-fee PaymentTransaction UUIDs;
- Product/seller recovery context;
- settlement, creation, and update timestamps.

The reviewed PostgreSQL schema script is [phase7b_ai_product_settlement.sql](D:/FA2026/PRN232/BE/payment-service/src/main/resources/db/manual/phase7b_ai_product_settlement.sql). It includes the unique correlation constraint, whole-VND/fee arithmetic checks, state domain check, and successful-settlement shape check. Payment currently uses Hibernate `ddl-auto: update`; the script is deliberately checked in for reviewed deployment application so financial durability is not silently dependent on an unreviewed production schema mutation.

# 5. Wallet Locking / Atomicity

`AiProductSettlementService` obtains a PostgreSQL transaction-scoped advisory lock for the Purchase correlation ID, then locks all candidate Client, Expert, and `ADM*` platform-wallet rows with `PESSIMISTIC_WRITE` in UUID order.

It requires exactly one wallet for each Client/Expert code and exactly one total `ADM*` wallet; all three must be `ACTIVE`. A missing, inactive, or ambiguous platform wallet returns the confirmed no-movement `WALLET_NOT_ACTIVE` result. No arbitrary platform wallet is selected.

One Payment transaction creates/updates the durable settlement record, validates locks and balances, debits Client gross once, credits Expert net, credits platform fee, inserts the main `AI_PRODUCT_PAYMENT` transaction plus `SYSTEM_FEE` transaction, and writes final `SUCCESS`. An insufficient balance changes no wallet and inserts no success transaction.

# 6. Service Authentication

The new Payment RPC methods are annotated with `@PreAuthorize("hasRole('SERVICE')")`.

AI Available signs a one-minute HS512 service JWT using the same configured signing-key convention already used by common-starter. It contains the established `sub`, `userId`, and `role: SERVICE` claims and is sent as gRPC authorization metadata only for Payment. It is not a forwarded browser token and is never logged.

# 7. Settlement Idempotency

`ProductPurchase.Id` is the permanent Payment correlation key.

- The Payment table unique constraint plus advisory lock serializes concurrent same-Purchase settlement requests.
- `SUCCESS` returns the original result without moving money again; materially different input returns `IDEMPOTENCY_CONFLICT`.
- `IN_PROGRESS` is authoritative/frozen and likewise rejects materially different input.
- Confirmed non-money-moving outcomes (`INSUFFICIENT_BALANCE`, `WALLET_NOT_ACTIVE`, or guaranteed no-movement `PAYMENT_FAILED`) permit the same key to be resubmitted with new current Product price/context.

# 8. Settlement Lookup / Recovery

`GetAiProductPurchaseSettlement` looks up the durable Payment record by `product_purchase_id` and returns `NOT_FOUND` only when no record exists.

AI Available performs lookup first for every Retry and for an existing pending Purchase submitted through Buy. A returned Payment `SUCCESS` completes the existing local pending Purchase from the Payment-authoritative amounts, timestamp, transaction ID, and echoed recovery snapshot; it does not debit again or overwrite snapshot data from the current listing. `IN_PROGRESS` remains pending without a changed settlement request. Only a confirmed no-settlement outcome prepares a new same-Purchase settlement request.

# 9. AI Payment Client

`PaymentSettlementGateway` wraps generated gRPC types behind internal request/result records. Controllers and application services do not use generated DTOs. It maps all declared Payment results explicitly and maps RPC/transport/invalid-response failure to `PaymentServiceUnavailableException`, which becomes a safe API `PAYMENT_SERVICE_UNAVAILABLE` response.

No EF transaction or local row lock is held while Identity or Payment gRPC is awaited.

# 10. Buy Endpoint

The new Client-only Buy endpoint:

1. verifies the current active Client with Identity;
2. locks/rechecks the Product and creates or reuses its unique stable pending Purchase intent;
3. rejects existing successful ownership;
4. commits the pending intent before Payment;
5. captures the current published Product price and current seller identity context in a short local transaction/Identity lookup;
6. calls Payment with the Purchase UUID correlation key; and
7. on Payment success, atomically transitions only the existing AI Purchase to `SUCCESS`, writes its approved snapshot and 30-day entitlement.

The Product must be published, and self-purchase is rejected. No `FAILED` AI Purchase state, AI attempt row, or AI schema change was added.

# 11. Retry Endpoint

The Client-only Retry endpoint verifies Purchase ownership and permits only `PENDING_PAYMENT`.

It always reconciles with Payment first. Payment `SUCCESS` is locally recovered; `IN_PROGRESS` returns `PAYMENT_IN_PROGRESS`; `NOT_FOUND` or an explicit no-movement result revalidates the current Product/seller and makes a same-Purchase settlement request at the current Product price. If the Product is no longer published, it returns `PRODUCT_NOT_PUBLISHED` rather than creating a replacement Purchase.

# 12. Financial Calculation

Payment calculates:

```text
platformFee = gross * 0.10, rounded to scale 0 with HALF_UP
expertNet   = gross - platformFee
```

For `100005`, this produces fee `10001` and expert net `90004`. AI validates the Payment-success amounts using the same whole-VND `AwayFromZero` midpoint behavior before persisting the local success snapshot.

# 13. Error Mapping

The existing API envelope is used. New purchase behavior maps to safe business codes including:

- `PRODUCT_NOT_FOUND`, `PRODUCT_NOT_PUBLISHED`, `SELF_PURCHASE_NOT_ALLOWED`
- `PURCHASE_NOT_FOUND`, `PURCHASE_ALREADY_OWNED`
- `PAYMENT_INSUFFICIENT_BALANCE`, `WALLET_NOT_ACTIVE`
- `PAYMENT_IN_PROGRESS`, `PAYMENT_IDEMPOTENCY_CONFLICT`
- `PAYMENT_FAILED`, `PAYMENT_SERVICE_UNAVAILABLE`

No gRPC diagnostics, Payment exceptions, wallet balances, or SQL errors are returned to the caller.

# 14. Database Impact

## Payment DB changes

Yes.

Phase 7B requires:

1. the Payment-owned `ai_product_settlement` table with a unique `product_purchase_id`, reviewed whole-VND/90-10 arithmetic checks, settlement-state checks, and successful-settlement shape checks; and
2. updates to the existing `payment_transaction` CHECK constraints so the database accepts the new Java enum values:
   - `PaymentReferenceType.AI_PRODUCT`
   - `TransactionType.AI_PRODUCT_PAYMENT`

The second requirement was discovered during real runtime verification. Although `PaymentTransaction.type` and `reference_type` are mapped as strings rather than PostgreSQL enum types, the existing table uses CHECK constraints as an enum-like database guard. Therefore the Phase 7B database change is not complete until those constraints include the two new values.

## AI DB unchanged

No AI entity, column, enum, migration, or table changed. The existing four-table model remains exactly `ai_product`, `product_asset`, `product_purchase`, and `product_rating`.

# 15. Build Results

| Check | Result |
|---|---|
| `dotnet build ai-available-service/ai-available-service.csproj --no-restore` | Passed: 0 warnings, 0 errors. The changed linked Payment proto client compiled successfully. |
| `mvn -pl common-starter clean install -DskipTests -e` | Passed. Protobuf and gRPC Java sources regenerated successfully and `common-starter` installed to the local Maven repository. The earlier host `protoc` `Illegal System DLL Relocation` blocker was no longer reproducible. |
| `mvn -pl payment-service -am clean package -DskipTests` | Passed. Reactor summary reported `common-starter` and `payment-service` SUCCESS. |
| `mvn -pl jobPost-service,workspace-service -am package -DskipTests` | Passed. Shared-proto compatibility rebuild succeeded for `common-starter`, `workspace-service`, and `jobPost-service`. Existing mapper warnings were non-blocking and unrelated to the Payment proto extension. |

# 16. Runtime / Test Results

Phase 7B was validated against the local development stack after Java proto regeneration and Payment schema preparation.

The following runtime behaviors were verified:

| Scenario | Result |
|---|---|
| Payment service container / gRPC port `9095` | Running and reachable |
| AI Available -> Payment reconciliation lookup | Successful gRPC transport and authenticated service call |
| AI Available -> Payment settlement | Successful gRPC transport and authenticated service call |
| Missing required platform wallet | Returned `WALLET_NOT_ACTIVE`; no money moved; AI Purchase remained `PENDING_PAYMENT` |
| Client balance insufficient | Returned `PAYMENT_INSUFFICIENT_BALANCE`; no successful transaction IDs were persisted; AI Purchase remained `PENDING_PAYMENT` |
| Retry reused the same `ProductPurchase.Id` | Passed |
| Payment durable non-money-moving state | Persisted and reused safely for retry |
| Payment `payment_transaction` DB guard | Runtime exposed missing CHECK values for `AI_PRODUCT` / `AI_PRODUCT_PAYMENT`; constraints were corrected |
| Successful retry after prerequisites were fixed | Passed |
| 90/10 settlement | Client debited gross once; Expert credited net; platform wallet credited fee |
| Payment settlement row | Transitioned to `SUCCESS` with main/fee transaction IDs and settlement timestamp |
| AI Purchase row | Transitioned from `PENDING_PAYMENT` to `SUCCESS` with immutable success snapshot and entitlement |
| Repeat/duplicate settlement behavior | Verified no second debit for the same successful Purchase correlation |

A local test fixture issue also exposed that `user_wallet.user_code` has no database UNIQUE constraint: a typo variant using `0` versus `O` could coexist when data was inserted manually outside the service. The incorrect disposable wallet had no transactions and was cleaned up. This is not a Phase 7B settlement-logic defect, but it is a defense-in-depth hardening candidate for Phase 8.

The runtime sequence successfully exercised recovery across confirmed non-money-moving outcomes:

```text
WALLET_NOT_ACTIVE
    -> fix required wallet
INSUFFICIENT_BALANCE
    -> top up correct Client wallet
retry SAME ProductPurchase.Id
    -> SUCCESS
    -> no duplicate debit
```

# 17. Deferred Phase 7C / 7D Work

Explicitly not implemented:

- Supabase adapter, Preview upload, protected asset upload/removal/download, and video-duration inspection.
- Kafka producer, `AI_PRODUCT_PURCHASED`, Notification topic subscription, outbox, or notification deduplication.
- Phase 8 end-to-end, load, and security hardening tests.

# 18. Genuine Blockers

None remain for the approved Phase 7B scope.

The following deployment requirements remain mandatory, but are no longer implementation blockers:

1. Apply the reviewed Payment-side Phase 7B database change before enabling the financial RPCs:
   - create/verify `ai_product_settlement`;
   - update `payment_transaction_reference_type_check` to allow `AI_PRODUCT`;
   - update `payment_transaction_type_check` to allow `AI_PRODUCT_PAYMENT`.
2. Keep the new Payment RPCs service-only and retain service JWT handling without logging credentials.
3. Treat `user_wallet.user_code` database uniqueness as a Phase 8 hardening item unless the team separately approves a Payment schema constraint change.

# 19. Phase 7B Closure

**PHASE 7B — Payment Foundation + AI Product Buy/Retry: PASSED / CLOSED**

Closure criteria satisfied:

- authoritative Payment proto extended and regenerated;
- Java and .NET affected consumers build successfully;
- Payment-owned durable settlement/idempotency state implemented;
- service-only gRPC authorization verified;
- canonical wallet locking / atomic 90-10 settlement implemented;
- reconciliation lookup implemented;
- Buy and Retry endpoints implemented;
- AI database remained unchanged;
- required Payment database changes were identified and applied locally;
- retry from confirmed non-money-moving states was verified;
- successful settlement and immutable local Purchase completion were verified;
- duplicate successful settlement did not debit twice.

Phase 7C may begin.
