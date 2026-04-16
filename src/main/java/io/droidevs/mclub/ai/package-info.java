/**
 * AI / Conversational assistant module.
 *
 * <p>Layers:
 * <ul>
 *   <li>webhook: inbound/outbound adapters (WhatsApp providers)</li>
 *   <li>conversation: state + session management</li>
 *   <li>rag: orchestration (prompt building, routing, model calls)</li>
 *   <li>retrieval: knowledge + data retrieval (db + vector search)</li>
 *   <li>tools: executable actions mapped to existing services</li>
 *   <li>audit: logging, safety, idempotency</li>
 * </ul>
 */
package io.droidevs.mclub.ai;

