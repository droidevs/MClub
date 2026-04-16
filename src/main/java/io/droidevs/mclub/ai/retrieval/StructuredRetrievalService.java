package io.droidevs.mclub.ai.retrieval;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.RetrievalContext;
import io.droidevs.mclub.dto.EventDto;
import io.droidevs.mclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple structured retrieval (no embeddings yet).
 *
 * <p>Fetches a small list of recent events to ground the assistant.
 */
@Service
@RequiredArgsConstructor
public class StructuredRetrievalService implements RetrievalService {

    private final EventService eventService;

    @Override
    public RetrievalContext retrieve(ConversationContext ctx, String userMessage) {
        // For now, retrieve global events snapshot.
        List<EventDto> events = eventService.getAllEvents(PageRequest.of(0, 5)).getContent();
        List<String> eventLines = events.stream()
                .map(e -> "%s (%s → %s) id=%s".formatted(
                        safe(e.getTitle()),
                        e.getStartDate(),
                        e.getEndDate(),
                        e.getId()
                ))
                .toList();

        List<String> facts = new ArrayList<>();
        facts.add("System: MClub (clubs, events, registration, attendance check-in, ratings, comments)");
        facts.add("User message: \"" + userMessage + "\"");

        return RetrievalContext.of(facts, eventLines);

    }

    private String safe(String v) {
        return v == null ? "(no title)" : v;
    }
}

