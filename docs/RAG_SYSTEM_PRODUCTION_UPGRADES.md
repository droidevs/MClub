# MClub RAG System — Production-Grade Upgrade Plan (Design + Implementation)

This document upgrades the **existing** MClub RAG system into a production-grade AI agent architecture while **preserving current abstractions** (`ConversationService`, `RagService`, `RetrievalService`, `ToolRegistry`, `Tool`).

> Scope: extend and improve; do not rewrite domain logic.

---

## 0) Current Implementation Snapshot (as-is)

### Runtime flow (current)
1. `WhatsAppWebhookController.receive()` → async delegation
2. `ConversationService.handleIncomingMessage()`
   - appends user message to `ConversationStore`
   - builds `ConversationContext` via `WhatsappIdentityService`
   - calls `RagService.handle(session, ctx)`
   - stores assistant response, sends via `WhatsAppSender`
3. `RagService.handle()`
   - calls `RetrievalService.retrieve(ctx, userMessage)`
   - builds prompt via `PromptBuilder`
   - calls `LlmClient.decide(prompt)`
   - if tool call → `ToolRegistry.get(name).execute(call, ctx)`

### Gaps today
- `StubLlmClient` is heuristic only; no real tool/function calling
- retrieval is small structured snapshot only
- no agent loop (1 decision per message)
- no disambiguation state machine
- in-memory conversation storage only
- tool input validation is manual and inconsistent
- WhatsApp linking is unimplemented (`linked=false` always)

---

## 1) Updated Architecture Diagram (text)

```
WhatsApp Provider
  -> POST /webhooks/whatsapp
      -> WhatsAppWebhookController
          -> ConversationService (async)
              -> ConversationStore (Redis/Postgres)
              -> WhatsappIdentityService (linking + lookup)
              -> AgentLoopExecutor
                  -> HybridRetrievalService
                      -> StructuredRetrievalService (existing)
                      -> VectorRetrievalService (pgvector)
                  -> PromptBuilder (+ system + tool schemas)
                  -> OpenAiLlmClient (JSON tool calling)
                  -> ToolExecutionService
                      -> ToolRegistry (existing tools)
                      -> ToolArgumentParser + BeanValidation
                      -> error normalization
              -> WhatsAppSender

Observability: structured logs + metrics + tracing around AgentLoopExecutor
```

Key principle: **RagService remains the orchestration entry**, but delegates multi-step logic to `AgentLoopExecutor`.

---

## 2) Updated Package Structure (non-breaking)

Add these packages (keep existing ones):

- `io.droidevs.mclub.ai.agent`
  - `AgentLoopExecutor`
  - `AgentStep`
  - `PendingActionStore` (persist pending clarification)

- `io.droidevs.mclub.ai.llm`
  - `OpenAiLlmClient` (implements `LlmClient`)
  - `ToolSchemaProvider`
  - `LlmJson`
  - `LlmOutputParser`

- `io.droidevs.mclub.ai.retrieval.vector`
  - `VectorEmbeddingService`
  - `VectorIngestionService`
  - `VectorRetrievalService`

- `io.droidevs.mclub.ai.retrieval.hybrid`
  - `HybridRetrievalService` (implements `RetrievalService`)
  - `HybridRanker`

- `io.droidevs.mclub.ai.tools.exec`
  - `ToolExecutionService`
  - `ToolArgumentParser`
  - `ToolExecutionException`

- `io.droidevs.mclub.ai.link`
  - `WhatsAppLinkService`
  - `OtpService`

- `io.droidevs.mclub.ai.store`
  - `RedisConversationStore` or `PostgresConversationStore`

---

## 3) Replace StubLlmClient With a Real LLM (Function Calling)

### Requirements
- tool/function calling
- strict JSON arguments
- robust parsing + validation
- dynamic tool list (derived from ToolRegistry)

### Suggested approach
Use OpenAI-compatible Chat Completions with *tools*.

#### Tool schemas
Provide JSON schemas per tool name. **Do not hardcode business logic** in LLM.

Example tool definition (Register event)
```json
{
  "name": "register_event",
  "description": "Register the linked user to a specific event.",
  "parameters": {
    "type": "object",
    "properties": {
      "eventId": {"type": "string", "description": "UUID of the event"}
    },
    "required": ["eventId"],
    "additionalProperties": false
  }
}
```

> Note: today `RegisterToEventTool` expects `eventId` but `StubLlmClient` passes `eventQuery`. The real LLM must output `eventId` after retrieval.

### Implementation: `OpenAiLlmClient`
- Implement `LlmClient.decide(prompt)`
- Call external API
- Convert API response into:
  - `LlmDecision.answer(text)`, or
  - `LlmDecision.tool(new ToolCall(name, args))`
- Use strict JSON parsing (Jackson) + schema validation

---

## 4) Smart Parameter Extraction Layer

### Goal
Convert ambiguous NL into structured hints *before* the tool call.

### Approach
Introduce `IntentExtractionService` that outputs:
- intent (REGISTER_EVENT / RATE_EVENT / CHECKIN / COMMENT / ASK)
- normalized entities:
  - `EventRef { id?, keyword?, dateRange? }`
  - `RatingRef { stars?, comment? }`
  - `CommentRef { targetType, targetId?, text }`

### Date normalization
- parse “tomorrow”, “next week”, “this Friday” into `LocalDate` range
- implement via `java.time` + a small rules engine (no heavy NLP) or integrate `natty`-style parser

### Fallback strategy
If extraction fails:
- run retrieval → show top matches → ask clarification

---

## 5) Vector Search with pgvector

### Schema (PostgreSQL)
```sql
create extension if not exists vector;

create table ai_embedding_document (
  id uuid primary key,
  doc_type text not null,            -- EVENT | CLUB | COMMENT | DOC
  source_id uuid,                    -- id of event/club/comment
  chunk_index int not null,
  content text not null,
  metadata jsonb not null,
  embedding vector(1536) not null,   -- adjust dimension to model
  created_at timestamptz not null default now()
);

create index ai_embedding_document_doc_type_idx on ai_embedding_document(doc_type);
create index ai_embedding_document_source_id_idx on ai_embedding_document(source_id);

-- HNSW index for ANN search (pgvector)
create index ai_embedding_document_embedding_hnsw
  on ai_embedding_document using hnsw (embedding vector_cosine_ops);
```

### Chunking strategy
- Events: title + description + location + date window as one chunk
- Clubs: name + description + category + status
- Comments: content with target metadata (but be careful: PII + moderation)

### Embedding pipeline
- On write/update of entities → enqueue embedding job
- Batch embeddings
- Store vector + metadata

---

## 6) Hybrid Retrieval Service

Replace current `StructuredRetrievalService` usage by composing it inside `HybridRetrievalService`:

- run structured retrieval (existing) → produces factual snippets + event list
- run vector retrieval → topK relevant chunks
- merge, rank, dedupe

### Ranking
Score =
- structured score (exactness, recency)
- vector similarity score
- boosts:
  - linked user’s clubs/events
  - time proximity for events

Fallback: if vector retrieval fails → structured only

---

## 7) Multi-step Agent Loop (true agent)

Add `AgentLoopExecutor` with:
- maxSteps (e.g., 5)
- loop guard (same tool call repeated → stop)
- iteration:
  1) retrieval
  2) prompt
  3) LLM decision
  4) if tool: execute + append tool result into session context
  5) continue until answer

This enables:
- search → disambiguate → act → confirm

---

## 8) Clarification & Disambiguation

### Problem
When multiple events match, you must ask which one.

### Design
- Maintain `PendingIntent` in store keyed by `conversationId`
- If ambiguous:
  - assistant asks a numbered list
  - store pending state: intent + candidates
- On next user message:
  - if pending state exists, resolve selection and continue agent loop

---

## 9) Persistent Conversation Store

### Option A: Redis (recommended)
- keys:
  - `conv:{conversationId}:session`
  - `conv:{conversationId}:pending_intent`
- TTL: 7–30 days depending on compliance

### Option B: PostgreSQL
```sql
create table ai_conversation_session (
  id uuid primary key,
  conversation_id text unique not null,
  from_phone_e164 text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table ai_conversation_message (
  id uuid primary key,
  session_id uuid not null references ai_conversation_session(id),
  role text not null, -- user|assistant|tool
  content text not null,
  created_at timestamptz not null default now()
);

create index ai_conversation_message_session_id_idx on ai_conversation_message(session_id);
```

---

## 10) Tool Execution Enhancements

Introduce `ToolExecutionService` that wraps `ToolRegistry`:
- parse arguments into DTOs
- validate via Bean Validation
- normalize exceptions into safe user answers
- attach audit logs

Standardize tool results:
- `ToolResult { humanMessage, status, toolName, data, errorCode }`

---

## 11) WhatsApp Linking Flow (End-to-End)

### Flow
1) user: "link my whatsapp"
2) system: generates OTP and instructs user to enter it in the web UI (authenticated)
3) web UI endpoint: `POST /api/whatsapp/link/confirm` (must be authenticated)
   - verifies OTP + phone number
   - stores `user_whatsapp_links(user_id, phone_e164, verified_at)`
4) WhatsApp messages now build `ConversationContext(linked=true, userId, email)`

Schema:
```sql
create table user_whatsapp_link (
  user_id uuid primary key,
  phone_e164 text unique not null,
  verified_at timestamptz not null
);
```

---

## 12) Observability & Monitoring

Add around agent loop + tools:
- **structured logs**: conversationId, userId, toolName, latencyMs, outcome
- **metrics** (Micrometer):
  - `ai.agent.steps`
  - `ai.llm.latency`
  - `ai.tool.success` / `ai.tool.failure`
  - `ai.retrieval.vector.hitRate`
- **tracing** (Micrometer Tracing / OpenTelemetry)
  - spans: webhook → agent loop → retrieval → llm → tool

---

## 13) Example End-to-End Scenario

User: "Register me for the AI workshop tomorrow"

1) Hybrid retrieval returns events with title containing "AI" and date in tomorrow range
2) LLM calls tool `search_events` **(recommended new tool)** OR selects eventId directly
3) if multiple matches:
   - assistant asks: "Which one? 1) ... 2) ..."
4) user repliesPreview: "1"
5) tool: `register_event(eventId=...)`
6) assistant confirms

---

## What I can implement next (incrementally, non-breaking)

1) Add `OpenAiLlmClient` behind `LlmClient` with config-enabled switch
2) Add `HybridRetrievalService` composing current structured retrieval
3) Add `AgentLoopExecutor` and inject into `RagService`
4) Add Postgres schema + repositories for conversation persistence
5) Add baseline observability (logs + micrometer metrics)

