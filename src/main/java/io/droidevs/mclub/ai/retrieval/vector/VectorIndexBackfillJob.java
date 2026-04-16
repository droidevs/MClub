package io.droidevs.mclub.ai.retrieval.vector;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.Comment;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.CommentRepository;
import io.droidevs.mclub.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background backfill to populate the vector index table.
 *
 * <p>Non-breaking: safe to run repeatedly; ingestion deletes and re-inserts per entity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mclub.ai.vector", name = "backfill-enabled", havingValue = "true")
public class VectorIndexBackfillJob {

    private final VectorIngestionService ingestionService;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;

    private final TextNormalizer normalizer;

    @Scheduled(initialDelayString = "${mclub.ai.vector.backfill.initial-delay-ms:30000}",
            fixedDelayString = "${mclub.ai.vector.backfill.fixed-delay-ms:3600000}")
    @Transactional(readOnly = true)
    public void runBackfill() {
        log.info("Vector backfill started");

        List<Club> clubs = clubRepository.findAll();
        for (Club c : clubs) {
            String text = normalizer.normalize("Club: " + safe(c.getName()) + "\n" + safe(c.getDescription()));
            ingestionService.reindexClub(c.getId(), text, "{}");
        }

        List<Event> events = eventRepository.findAll();
        for (Event e : events) {
            String text = normalizer.normalize("Event: " + safe(e.getTitle()) + "\n" + safe(e.getDescription()) + "\nLocation: " + safe(e.getLocation()));
            ingestionService.reindexEvent(e.getId(), text, "{}");
        }

        List<Comment> comments = commentRepository.findAll();
        for (Comment c : comments) {
            if (c.isDeleted()) continue;
            String text = normalizer.normalize("Comment: " + safe(c.getContent()));
            ingestionService.reindexComment(c.getId(), text, "{}");
        }

        log.info("Vector backfill completed");
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}


