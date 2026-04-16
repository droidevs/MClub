# MClub AI RAG System Analysis

This document provides a comprehensive analysis of the RAG (Retrieval-Augmented Generation) AI assistant system integrated into the MClub Spring Boot enterprise application.

## 1. High-Level Architecture
The RAG system acts as an intelligent orchestration layer between natural language inputs (specifically via WhatsApp webhooks) and MClub's backend services. It is responsible for session management, context enrichment, context retrieval, prompt generation, LLM communication, and tool execution.

### Flow of Data
1. **Inbound Webhook**: A POST request arrives at `WhatsAppWebhookController`.
2. **Session & Context**: `ConversationService` retrieves or creates a session from the `ConversationStore` (e.g., `InMemoryConversationStore`) and fetches user context via `WhatsappIdentityService`.
3. **Retrieval**: `RagService` delegates to `RetrievalService` (`StructuredRetrievalService` for now) to append factual contextual data (e.g., recent events, global context).
4. **Prompt Construction**: `PromptBuilder` merges historical session messages, retrieved facts, system rules, and user identity info into an LLM prompt.
5. **LLM Decision**: The LLM (`LlmClient` interface, currently implemented as `StubLlmClient` for deterministic dev testing) makes a decision: either `answer(text)` or trigger a tool `tool(ToolCall)`.
6. **Tool Execution**: `RagService` verifies if the user is linked (`ctx.linked()`). If linked, it executes the relevant tool via `ToolRegistry`.
7. **Outbound Message**: A `RagResponse` is routed back through `WhatsAppSender` (currently `LoggingWhatsAppSender`) to the user.

## 2. Core Components & Packages

### `io.droidevs.mclub.ai.conversation`
Handles state and identity.
*   **`ConversationService`**: Entry point orchestrator for incoming Webhook messages. Updates history and sends output.
*   **`ConversationStore`**: Maintains `ConversationSession` instances to keep track of messaging history.
*   **`WhatsappIdentityService`**: Responsible for determining if a WhatsApp number matches an MClub user identity.

### `io.droidevs.mclub.ai.rag`
Handles the orchestration of RAG.
*   **`RagService`**: Core coordinator calling Retrieval, PromptBuilder, LlmClient, and Tool Execution.
*   **`PromptBuilder`**: Constructs the final payload to be sent to the LLM (inserts system rules, context, and chat history).
*   **`LlmClient`**: The abstraction to communicate with the LLM API. The application uses `StubLlmClient` which handles heuristic keyword detection to trigger tools for development.
*   **`LlmDecision` & `ToolCall`**: Models representing deciding either to respond via text or execute a function block.

### `io.droidevs.mclub.ai.retrieval`
Handles context injection.
*   **`StructuredRetrievalService`**: Currently performs structured DB retrieval (e.g., fetching a snapshot of 5 recent events via Pagination) instead of relying entirely on vector embeddings.

### `io.droidevs.mclub.ai.tools`
Implements the Strategy/Command pattern for all actionable tools. 
The AI assistant can execute state mutations strictly through these securely registered tools. 
**Current Tools Available:**
*   `RegisterToEventTool`: Automatically registers a linked user to an event using `RegistrationService`.
*   `RateEventTool`: Allows rating an event securely.
*   `CheckInEventTool`: Manages attendance QR logic mappings.
*   `AddCommentTool` / `LikeCommentTool` / `ReplyToCommentTool`: Engages with the threading mechanism within events and club activities.

## 3. Identity and Security (Crucial Constraint)
The RAG agent is strictly bound to user security constraints:
*   Users must link their WhatsApp number to an MClub profile via `WhatsappIdentityService`.
*   If `ctx.linked()` returns false, the agent will gracefully fail any write operations (tool executions) and instruct the user to register/login and link their account. 

## 4. Current Limitations & Next Steps
*   **Vector Search & Semantic Retrieval**: Retrieval is currently strictly structured via `StructuredRetrievalService`. Integrating `pgvector` for true semantic matching will significantly broaden knowledge matching accuracy.
*   **Actual LLM Client Implementation**: `StubLlmClient` needs to be swapped out for a full implementation utilizing OpenAI/Azure APIs with strict JSON function calling capabilities to properly replace heuristic routing.
*   **Persistence**: Implement a production-grade external persistent `ConversationStore` (Redis/PostgreSQL) instead of `InMemoryConversationStore`.
*   **WhatsApp Account Linking Flow**: The process of generating an OTP and logging via a web UI to link `phone_e164` to `user_id` needs complete implementation for end-to-end testing.
