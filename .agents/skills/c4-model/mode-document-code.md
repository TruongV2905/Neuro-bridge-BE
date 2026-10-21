# Mode: Document-code — retro-documentation from a repo

> Produce a C4 from an existing codebase. The skill scans, presents findings, then dialogues to fill in the ambiguities.

## When to use

- The user points to a repo path (local or GitHub), pastes code, or says *"document my codebase"*
- Concrete artifacts exist to analyze (source code, config files, manifests…)
- The goal is to reflect reality, not to imagine a possible future

## Step 1 — Quick framing

Max 3 questions if information is missing:

1. Scope of the repo: whole monorepo, or a subfolder?
2. Expected levels (Context + Container usually suffice — Component only if the user asks)?
3. Output format and destination — see [`SKILL.md`](SKILL.md) *Common contract*.

## Step 2 — Codebase scan

**Evidence-based scanning workflow**: Use Antigravity tools (`list_dir`, `view_file`, `grep_search`, `run_command`) systematically across file groups. Do not guess or fabricate components; every element must have evidence from manifests or source code.

### Scanning procedure:

1. **Top-level structure & Orchestration** (`list_dir`, `view_file`):
   - Inspect repository root to identify service folders and mono-repo boundaries.
   - Examine orchestration files (`docker-compose.yml`, Kubernetes manifests, `Procfile`, Helm charts).
   - Identify candidate standalone runtime services (`Container`), datastores (`ContainerDb`), and queues/brokers (`ContainerQueue`).
2. **Build manifests & Dependencies** (`view_file`, `grep_search`):
   - Check build descriptors (`pom.xml`, `package.json`, `go.mod`, `Cargo.toml`, `requirements.txt`).
   - Extract exact technology stack (framework, database drivers, client libraries, cloud SDKs).
3. **Configuration & Data stores** (`view_file`, `grep_search`):
   - Check `.env.example`, `application.yml`, `config/` (mask secrets).
   - Trace configured database connections, message brokers, topics, and external APIs.
4. **APIs, Entry points & Inter-service Communication** (`grep_search`, `view_file`):
   - Trace controllers/routers (`@RestController`, route declarations, HTTP handlers).
   - Trace inter-service clients (`@FeignClient`, HTTP client instances, gRPC clients, message publishers/listeners).
   - Record protocol (HTTPS/JSON, gRPC, AMQP, Kafka, JDBC) and target endpoint.
5. **Evidence vs Assumption Tracking**:
   - Classify findings into: **Verified facts** (direct code/config proof), **Deductions** (strong structural hint), and **Assumptions** (unverified hypotheses).

### Typical sources to read

| Type | Files to examine |
|---|---|
| Overview | `README*`, `ARCHITECTURE.md`, `docs/` |
| Orchestration | `docker-compose.yml`, `Procfile`, k8s/Helm manifests, `serverless.yml` |
| Build/languages | `package.json`, `pom.xml`, `pyproject.toml`, `requirements.txt`, `go.mod`, `Gemfile`, `Cargo.toml` |
| Runtime | `Dockerfile`(s), startup scripts (`start.sh`, `entrypoint.sh`) |
| Data | Migrations (`db/migrations/`, `alembic/`, Prisma schema…), SQL schemas |
| Messaging | Kafka/Rabbit/Pub-Sub config, topic/queue declarations |
| Secrets/config | `.env.example`, `config/` (secrets masked) |
| Integrations | Search for HTTP clients, third-party SDKs (`stripe`, `twilio`, `sendgrid`, `auth0`…) |

### Identification rules

- Service deployed independently → a `Container`
- DB / cache → `ContainerDb`; brokers/queues → `ContainerQueue`
- External dependency (SaaS, third-party API) → `System_Ext`
- Major subpackages/modules of a container → candidate `Component` (level 3, on demand)

## Step 3 — Present findings for explicit user validation

**Mandatory checkpoint**: Present findings in chat and get user confirmation BEFORE creating any architecture documents or diagrams:
- **Verified Containers & Data stores** (with exact technology and evidence path)
- **Verified External dependencies** (with client/SDK/config evidence)
- **Verified Communication flows** (with protocol and intent verified from code)
- **Suspected flows & Ambiguities** (implicit or uncertain connections)
- **Assumptions ledger** (anything inferred without explicit code proof)

Ask: *"Does this breakdown match the actual architecture? Are any containers, datastores, or flows missing or incorrect?"*
Wait for explicit confirmation before proceeding to generate documents.

## Step 4 — Dialogue-driven enrichment

For each unknown or ambiguity, ask by batches of 3-5 (common dialogue rule). Answers feed the model. If the user doesn't know, it stays in **Assumptions**, not in the diagram.

## Step 5 — Draft, iterate, finalize

Produce Context + Container in the chosen format (default: local filesystem in `docs/architecture/` using `write_to_file`). Iterate on user feedback. Run the [`review-checklist.md`](review-checklist.md). Deliver to the chosen destination.

**Assumptions**: anything inferred from code without explicit confirmation (*"we deduce PostgreSQL from migrations in `db/migrations/`"*) goes into the *Assumptions* section of the document, never silently into the diagram.

## Tools to use per step

| Step | Tool | Usage |
|---|---|---|
| 1 | — | Dialogue |
| 2 Codebase scan | `list_dir`, `view_file`, `grep_search`, `run_command` | Systematic multi-phase code and manifest inspection |
| 3-4 | — | Dialogue & verification checkpoint |
| 5 delivery (local) | `write_to_file` | Create architecture documents in `docs/architecture/` |
| 5 delivery (MCP) | `mcp__*` | Destination MCP tools (only when available in the session) |

## Links

- ← Back: [`SKILL.md`](SKILL.md)
- Deliverable template: [`level-template.md`](level-template.md)
- Filled-out examples: [`examples/`](examples/)
- Detailed Mermaid syntax: [`mermaid-c4-syntax.md`](mermaid-c4-syntax.md)
- Review checklist: [`review-checklist.md`](review-checklist.md)
- If the user then wants to evolve the produced C4: [`mode-update.md`](mode-update.md)
