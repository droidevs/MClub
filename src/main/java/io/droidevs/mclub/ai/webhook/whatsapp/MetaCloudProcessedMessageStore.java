package io.droidevs.mclub.ai.webhook.whatsapp;

/**
 * Idempotency store for processed inbound Meta messages.
 *
 * <p>Meta can retry deliveries; we must avoid processing the same message twice.
 */
public interface MetaCloudProcessedMessageStore {

    /** Returns true if added (not processed before). False if already processed. */
    boolean markProcessedIfNew(String messageId);
}

