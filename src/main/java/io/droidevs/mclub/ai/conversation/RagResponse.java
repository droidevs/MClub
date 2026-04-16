package io.droidevs.mclub.ai.conversation;

/** Response to send back to the chat channel. */
public record RagResponse(String message) {
    public static RagResponse of(String msg) {return new RagResponse(msg);}
}

