# Neuro Bridge — AI Available Service Boundary & Integration Contract

## 1. Purpose

This document is the Phase 2 source of truth for the new `ai-available-service`.

The service powers **AI Place**, where Experts publish ready-made AI products and Clients can browse, purchase, access purchased deliverables for a limited time, and rate the purchased product.

This document defines:
- what `ai-available-service` owns,
- what it only references,
- which existing services it integrates with,
- the integration mechanism for each dependency,
- MVP business rules that directly affect service boundaries,
- what is explicitly out of scope.

This document does **not** define the final database schema, REST endpoint list, DTO classes, or implementation code. Those belong to later phases.

---

## 2. AI Place vs Existing Expert Marketplace

Neuro Bridge has two different marketplace directions:

### Existing Expert Marketplace
- Client creates and publishes a JobPost.
- Expert browses JobPosts.
- Expert applies to a JobPost.
- The project can proceed to contract, workspace, deliverables, escrow/release, and Expert rating.

### AI Place
- Expert creates and publishes a ready-made AI Product.
- Client browses AI Products.
- Client purchases a Product.
- Payment is settled immediately from Client Wallet to Expert/Platform.
- Client receives a time-limited entitlement to the Product deliverables.
- Client may rate the purchased Product.

These are separate business flows and should not share domain ownership simply because both appear under a marketplace concept.

---

## 3. Service Identity

### Feature / UI name
`AI Place`

### Backend service
`ai-available-service`

### Technology baseline
- ASP.NET Core 8
- EF Core
- PostgreSQL
- Eureka registration through Steeltoe
- API Gateway dynamic routing
- JWT validation compatible with the existing Neuro Bridge identity contract

---

## 4. MVP Scope

### In Scope

#### Expert
- Create AI Product as Draft
- Edit Draft
- Upload the four required deliverables while still in Draft
- Replace/remove deliverables before first Publish
- Set a fixed selling price
- Add Product description and public preview video
- Publish Product
- Unpublish Product
- Edit basic Product information while Unpublished
- Republish Product
- View own Products

#### Client
- Browse AI Place
- Search/filter AI Products
- View Product Detail
- View basic Expert information
- Open the existing Expert Profile flow/page
- Purchase Product
- Access purchased deliverables for 30 days
- Download purchased deliverables multiple times during the entitlement period
- View Purchase History after entitlement expiration
- Rate purchased Product
- Edit Product Rating
- Delete Product Rating

### Out of Scope

- Report
- Dispute
- Refund workflow
- Admin moderation / approval
- Product Versioning
- Deliverable replacement after first Publish
- Subscription
- Advanced license marketplace
- Project Escrow
- Expert Rating implementation
- Changes to the existing `rate-service`
- Changes to existing Workspace/JobPost business flows

---

## 5. Product Lifecycle

MVP Product states:

```text
DRAFT
  |
  | Publish
  v
PUBLISHED
  |
  | Unpublish
  v
UNPUBLISHED
  |
  | Republish
  v
PUBLISHED
```

No Admin approval state is required in MVP.

No Product Version state is required in MVP.

---

## 6. Deliverable Lifecycle

Every AI Product requires exactly four protected deliverables:

- `SOURCE_CODE`
- `VIDEO`
- `DATASET`
- `DOCUMENT`

### Draft behavior

While the Product is still `DRAFT`, Expert may:
- upload a deliverable,
- replace a deliverable,
- remove and re-upload a deliverable.

### First Publish rule

After the Product is published for the first time, all four deliverables become immutable for MVP.

```text
SOURCE_CODE  LOCKED
VIDEO        LOCKED
DATASET      LOCKED
DOCUMENT     LOCKED
```

Unpublishing does not unlock the deliverables.

While `UNPUBLISHED`, Expert may still edit basic Product information and price.

Changing deliverables after first Publish requires Product Versioning, which is outside MVP.

---

## 7. Public Preview vs Protected Deliverables

The public Product Detail contains:

- full Product description,
- one short preview/demo video,
- preview video duration under 4 minutes.

The preview video is a **public Product asset** and is separate from the protected `VIDEO` deliverable.

### Public assets
Examples:
- Product thumbnail/image
- Short preview video

These may be visible before purchase.

### Protected assets
- Source Code
- Full Video
- Dataset
- Document

Protected assets require a valid purchaser entitlement.

---

## 8. Ownership Boundary

### `ai-available-service` owns

- AI Product
- Product basic information
- Product price
- Product lifecycle/status
- Product public preview metadata
- Protected deliverable metadata/object references
- Product seller references
- Product Purchase
- Purchase Snapshot
- 30-day Entitlement
- Purchase History
- Product Rating

### `identity-service` owns

- User
- Expert identity
- Expert Profile
- Expert name
- Avatar
- Skills
- Certificates
- Portfolio
- Expert profile statistics
- Expert Rating / completed-project profile data

### `payment-service` owns

- Wallet
- Wallet balance
- Wallet debit/credit
- Platform fee settlement
- PaymentTransaction
- financial transaction state

### Supabase owns

- physical file bytes / storage objects

### Existing `rate-service` owns

- Expert rating based on completed Workspace Project

It does not own AI Product Rating.

---

## 9. Identity Integration Contract

`ai-available-service` should use the existing internal gRPC User lookup from `identity-service`.

Concept:

```text
ai-available-service
        |
        | gRPC GetUser(userId)
        v
identity-service
```

Primary usage:
- validate/reference seller identity when needed,
- obtain basic display information such as:
  - user/expert code,
  - full name,
  - avatar,
  - role/status if required.

`ai-available-service` should not copy the full Expert Profile into its own database.

### Seller references

Conceptually the Product should be able to reference:

- `expertId` — UUID identity reference
- `expertCode` — external Expert code used by existing payment/wallet flows

The exact database representation is deferred to Phase 3.

### Full Expert Profile

AI Place should not compose full Skill/Certificate/Portfolio details for every Product card.

When Client chooses **View Expert Profile**, frontend should use the existing Expert Profile flow/page.

---

## 10. Payment Integration Contract

### Payment policy

AI Product purchase uses the existing Neuro Bridge platform-fee policy:

```text
Product price = 100%

Client Wallet:
  -100%

Settlement:
  +90% Expert Wallet
  +10% Platform/System Wallet
```

### No Escrow

AI Product purchase is an immediate digital purchase.

Project Escrow must not be used.

### Financial ownership

`payment-service` must remain the only owner of money movement.

`ai-available-service` must not:
- modify wallet balances directly,
- access payment tables directly,
- treat a ledger-only transaction record as payment execution.

### Required Payment extension

The existing Payment service has internal wallet debit/credit logic but no suitable public generic Product purchase endpoint.

A small synchronous Payment contract extension is therefore required later.

Concept:

```text
ai-available-service
       |
       | Product settlement request
       v
payment-service
       |
       +-- debit Client Wallet
       +-- credit 90% Expert Wallet
       +-- credit 10% Platform Wallet
       +-- create payment transaction(s)
       +-- return payment transaction reference
```

The exact endpoint/DTO is deferred to Phase 4.

Payment semantic values for AI Product purchase will also need an appropriate transaction/reference type in `payment-service`.

---

## 11. Purchase Ownership and Rules

`PaymentTransaction` and `ProductPurchase` are different concepts.

### `payment-service`
Owns:
- financial transaction.

### `ai-available-service`
Owns:
- business Purchase.

A Product Purchase conceptually preserves:
- Product reference,
- Buyer reference,
- Seller reference,
- payment transaction reference,
- Product snapshot,
- price at purchase,
- platform fee at purchase,
- Expert net amount at purchase,
- purchased time,
- entitlement expiration.

The exact schema is deferred to Phase 3.

### Purchase uniqueness

For MVP:

```text
One Client + One Product = maximum one successful purchase
```

After a Client has purchased the Product once:
- they cannot buy it again,
- this remains true even after the 30-day entitlement expires.

---

## 12. Purchase Snapshot

Purchase History must remain historically stable even if Expert later changes the live Product listing.

The Purchase should preserve a minimal snapshot such as:
- Product name at purchase,
- Product thumbnail/preview identity at purchase,
- seller identity/display information at purchase,
- price at purchase,
- platform fee at purchase,
- Expert net amount at purchase.

The final snapshot fields are decided in Phase 3.

---

## 13. 30-Day Entitlement

After successful payment:

```text
Purchase SUCCESS
        |
        v
purchasedAt = T
accessExpiresAt = T + 30 days
```

During the entitlement window:
- Client may download protected assets multiple times.

After expiration:
- Purchase History remains visible,
- protected download access is denied,
- Client cannot repurchase the same Product.

### Unpublish behavior

If Product is `UNPUBLISHED`:
- new Clients cannot discover it,
- new Clients cannot purchase it,
- existing Buyers retain their remaining 30-day entitlement,
- Purchase History remains.

---

## 14. Supabase Integration Boundary

### Existing frontend pattern

Existing Workspace Submission uploads directly from browser to Supabase.

This direct-upload approach is useful for large files and should remain the general direction for AI Product upload.

### Upload target pattern

```text
Expert FE
   |
   | file bytes
   v
Supabase Storage
   |
   | object reference / metadata
   v
ai-available-service
```

Large Dataset files should not need to pass through ASP.NET Core as a binary proxy.

### Protected download target pattern

Permanent public URLs must not be the purchaser authorization mechanism for protected assets.

Target concept:

```text
Client requests asset access
        |
        v
ai-available-service
        |
        +-- verify current Client
        +-- verify successful Purchase
        +-- verify Purchase belongs to Product
        +-- verify entitlement not expired
        |
        v
temporary/protected asset access
        |
        v
Supabase Storage
```

The exact signed/temporary URL mechanism is deferred to Phase 4/5.

### Metadata principle

The service should treat the Supabase storage object reference/path as the durable file identity.

A permanent public URL should not be the protected entitlement credential.

---

## 15. Product Rating Contract

AI Product Rating belongs to `ai-available-service`.

The existing `rate-service` remains dedicated to Expert rating from completed Workspace Projects.

### MVP Rating rules

- Only a Client with a successful Product Purchase is eligible to rate.
- Rating eligibility starts immediately after successful purchase.
- No download/use verification is required.
- One active Rating per Client per Product.
- Client may edit the Rating.
- Client may delete the Rating.

Report/dispute handling for Ratings is outside MVP.

Frontend may reuse existing Rating UI components, but backend data and business contracts are separate.

---

## 16. Notification Boundary

Notification is optional asynchronous integration.

Concept:

```text
ai-available-service
       |
       | Kafka event
       v
Kafka
       |
       v
notification-service
```

Potential events may include Product purchase or publication events.

Exact event names and payloads are deferred to Phase 4.

A Notification failure must not make a successful Product purchase fail.

---

## 17. Explicit Non-Dependencies

`ai-available-service` should not have a runtime dependency on:

- `jobPost-service`
- `workspace-service`
- `rate-service`
- `report-service`

These existing services may be referenced only for architectural patterns or later optional integration, not core MVP runtime behavior.

---

## 18. Integration Mechanism Summary

| Integration | Mechanism | Status |
|---|---|---|
| Frontend -> ai-available-service | REST through API Gateway | Required |
| ai-available-service -> identity-service | Existing gRPC User lookup | Required |
| ai-available-service -> payment-service | New synchronous Product settlement contract | Required |
| Expert Frontend -> Supabase | Direct file upload pattern | Required |
| ai-available-service -> Supabase | Protected/temporary asset-access authority | Required |
| ai-available-service -> PostgreSQL | EF Core / Npgsql | Required |
| ai-available-service -> Notification | Kafka event | Optional |
| AI Product Rating | Internal to ai-available-service | Required MVP |
| Workspace / JobPost | None | No dependency |
| Report / Dispute | None | Out of MVP |

---

## 19. Integration Map

```text
                               +----------------------+
                               |   identity-service   |
                               | User / Expert source |
                               +----------^-----------+
                                          |
                                         gRPC
                                          |
                                          |
+------------------+             +---------+------------+
| Next.js AI Place| --- REST --->| ai-available-service |
|                  | via Gateway  |                      |
+--------+---------+             | Product              |
         |                       | Purchase             |
         |                       | Entitlement          |
         |                       | Product Rating       |
         |                       +------+--------+------+
         |                              |        |
         | direct upload                |        | settlement
         v                              |        v
+------------------+                    |   +------------------+
|     Supabase     |<-------------------+   | payment-service  |
|                  | protected access       | Wallet / Tx      |
| Public preview   |                        +------------------+
| Protected files  |
+------------------+

                        Optional async
                             |
                             v
                           Kafka
                             |
                             v
                    notification-service
```

---

## 20. Phase 2 Completion Criteria

Phase 2 is considered complete when the following are agreed:

- Service ownership is defined.
- Existing dependencies are identified.
- Identity integration uses existing gRPC lookup.
- Payment remains owned by `payment-service`.
- Product purchase uses 90/10 settlement policy.
- Project Escrow is excluded.
- Supabase stores physical assets.
- Protected downloads require entitlement-aware access.
- Product Rating belongs to `ai-available-service`.
- Report/Dispute is out of MVP.
- Product Versioning is out of MVP.
- Deliverables are immutable after first Publish.
- Purchase History uses historical snapshot semantics.
- Client can purchase each Product at most once.

All criteria above are now agreed.

**Phase 2 status: DONE.**
