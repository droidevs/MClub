package io.droidevs.mclub.ai.web.dto;

import java.util.List;

public record ChatHistoryResponse(List<ChatHistoryMessage> messages) {
    public record ChatHistoryMessage(String role, String content, String at) {}
}

