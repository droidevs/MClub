# MClub RAG System — Gap Analysis & Required Next Steps (April 2026)

This document re-analyzes the **current implemented** RAG system in MClub (WhatsApp + RAG + tools + pgvector extensions) and lists:

- what is working now
- what is missing for production
- key problems / risks
- a prioritized implementation roadmap

> Scope: This analysis is based on the code in `io.droidevs.mclub.ai.*` plus the recent additions: agent loop, OpenAI tool calling, pgvector table, vector retrieval/search services.

---

## 1) Current End-to-End Flow (as implemented)

### Inbound
- `POST /webhooks/whatsapp` → `WhatsAppWebhookController.receive()`
- Delegates to `ConversationService.handleIncomingMessage(...)` using `@Async`

### Session + context
- `ConversationStore` is currently `InMemoryConversationStore`
- `WhatsappIdentityService.buildContext(fromPhoneE164)` currently always returns:
  - `linked=false`
  - `userId/email=empty`

### RAG orchestration
- `RagService.handle(session, ctx)` delegates to `AgentLoopExecutor.run(session, ctx)`

### Agent loop
- Up to 5 steps:
  1) Retrieval via `RetrievalService` (wired to `HybridRetrievalService`)
  2) Prompt build via `PromptBuilder`
  3) LLM decision via `LlmClient`
  4) If tool call: execute tool through `ToolRegistry`
  5) Tool result is fed back into session as an assistant message

### Retrieval
- `HybridRetrievalService` always executes structured retrieval (`StructuredRetrievalService`)
- If vector beans are available, it also executes vector search and appends results to `RetrievalContext.recentEvents`

### LLM
- Tool calling client exists: `OpenAiLlmClient` (OpenAI-compatible `/v1/chat/completions`)
- Uses `ToolSchemaProvider` to provide tool schemas for existing tools

### Outbound
- Response is sent via `WhatsAppSender` (currently logging adapter)

---

## 2) What Works Now

### ✅ Stable wiring
- WhatsApp webhook → async conversation handling → agent loop → response

### ✅ Tool execution mechanism
- Tool invocation is centralized via `ToolRegistry`
- Current tools include:
  - `register_event`
  - `checkin_event`
  - `rate_event`
  - `add_comment`

### ✅ Agent loop foundation
- Multi-step loop exists with:
  - max step limit
  - loop guard against repeating identical tool calls

### ✅ pgvector schema is present
- Flyway migration `V2__ai_pgvector.sql` defines:
  - `ai_embedding_document`
  - entity references via FKs (`event_id`, `club_id`, `activity_id`, `comment_id`)
  - HNSW index for cosine search

### ✅ Vector search capability is implemented (query-time)
- `VectorIndexRepository` supports:
  - cosine similarity search
  - optional filters (entity_type, club_id, event_id)
- `VectorSearchService` embeds query and runs vector search

### ✅ Embeddings provider adapter exists
- `EmbeddingService`
- `OpenAiEmbeddingService` (calls `/v1/embeddings` using `text-embedding-3-small`, dims=1536)

---

## 3) Critical Problems / Gaps

### P0 — Write actions are effectively disabled
**Problem:** `WhatsappIdentityService` always produces `linked=false`.

**Impact:** Every tool call is blocked by the agent loop’s linked-user guard:
> “please link your WhatsApp number to your MClub account first.”

**What you need:** implement WhatsApp linking (OTP) + persistence table + lookup.

---

### P0 — Vector search will return nothing unless you ingest
**Problem:** `VectorIngestionService` exists but nothing calls it.

**Impact:** `ai_embedding_document` will be empty → semantic search yields no hits.

**What you need:** ingestion triggers:
- initial backfill job
- incremental reindexing when entities change

---

### P0 — Embeddings provider enabled flag is overloaded
**Problem:** `mclub.ai.openai.enabled=true` currently controls BOTH:
- `OpenAiLlmClient`
- `OpenAiEmbeddingService`

**Impact:** You cannot independently enable embeddings vs chat model. Cost control and operational toggles are weak.

**What you need:** separate toggles:
- `mclub.ai.llm.openai.enabled`
- `mclub.ai.embedding.openai.enabled`

---

### P0 — Tool schemas do not align with current tool code in all cases
Example:
- `RegisterToEventTool` expects `eventId`
- But the legacy stub earlier used `eventQuery`

**Impact:** With real LLM usage, it must always produce the exact args. If retrieval does not surface IDs clearly, tool will fail.

**What you need:**
- ensure RetrievalContext includes explicit IDs for candidate entities
- enforce JSON schema validation and strong argument parsing

---

### P1 — RetrievalContext structure is semantically confusing
`RetrievalContext` currently has:
- `factualSnippets`
- `recentEvents`

But hybrid retrieval appends vector results into `recentEvents`.

**Impact:** prompt semantics degrade (vector hits ≠ “recent events”).

**What you need:** evolve RetrievalContext carefully without breaking:
- add `semanticHits` or `relevantItems` field
- keep `recentEvents` for actual recent events

---

### P1 — Agent loop does not persist intermediate state / pending intent
**Problem:** no pending action store.

**Impact:**
- disambiguation ("which event?") cannot resume after user responds

**What you need:**
- `PendingIntent` store keyed by conversationId
- resume logic in ConversationService / AgentLoopExecutor

---

### P1 — Conversation storage is not production-safe

**Problem:** `InMemoryConversationStore`:
- loses data on restart
- not horizontally scalable

**Impact:** breaks real WhatsApp conversation continuity.

**What you need:** Redis or PostgreSQL backed store + TTL.

---

### P1 — Tool execution lacks a professional validation/error boundary
**Problem:** each tool manually checks arguments.

**Impact:**
- inconsistent validation responses
- weak security posture if new tools are added

**What you need:** `ToolExecutionService`:
- parse args into DTOs (Jackson)
- validate with Bean Validation
- normalize exceptions into safe messages

---

### P2 — Observability is missing

**Problem:** no structured logs, metrics, traces for:
- LLM calls
- retrieval latency
- tool success/failure

**Impact:** production debugging and cost control are hard.

**What you need:** micrometer metrics + tracing + structured logs.

---

### P2 — Vector index tuning / model mismatch risk

**Problem:** Vector dimension is fixed to 1536 in schema.

**Impact:** Changing embedding model/dims later needs migration.

**What you need:**
- lock the embedding model
- or create a new table per dimension/model

---

## 4) What Your System Needs Next (Prioritized)

### Phase 1 (P0 — make it usable)
1. WhatsApp linking end-to-end
   - table `user_whatsapp_link`
   - OTP generation + verification
   - identity lookup in `WhatsappIdentityService`

2. Vector ingestion backfill
   - scheduled job indexing recent events/clubs/activities + non-deleted comments

3. Incremental reindexing hooks
   - on create/update of event/club/activity/comment

### Phase 2 (P1 — correctness & UX)
4. Disambiguation system
   - pending intent storage
   - numbered candidate selection

5. RetrievalContext improvement
   - separate fields for structured lists vs semantic hits

6. ToolExecutionService
   - DTO parsing + Bean Validation + safe error mapping

### Phase 3 (P2 — production hardening)
7. Persistent conversation store
   - Redis (TTL) or Postgres

8. Observability
   - metrics / tracing / tool audit logs

9. Vector tuning
   - HNSW parameters or switch to IVFFlat for large datasets

---

## 5) Concrete Risks to Address

- **Security risk:** if/when you implement linking, ensure:
  - verification is strong (OTP expiry, rate limit)
  - phone numbers are normalized (E.164)

- **Cost risk:** embeddings per message and per ingestion can be expensive.
  - batching embeddings
  - caching repeated queries

- **Data risk:** comments might contain sensitive data.
  - MUST exclude deleted comments
  - consider moderation / redaction pipeline

- **Reliability risk:** external LLM/embeddings failures.
  - timeouts + retries + fallbacks

---

## 6) Summary

### Your system is currently:
- architecturally sound (clean seams exist: retrieval, LLM, tools)
- has a working agent loop foundation
- has pgvector schema + search code

### Your system still needs:
- linking (otherwise tools never run)
- ingestion (otherwise vector search returns no results)
- disambiguation persistence
- production-grade storage and observability

---

## 7) Recommended Next Implementation

If you want the most immediate impact:
1) Implement WhatsApp linking
2) Implement a one-time backfill index job
3) Implement incremental reindex on entity update

Then the vectorized semantic retrieval becomes useful in the real world.

