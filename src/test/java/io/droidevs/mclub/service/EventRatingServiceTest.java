package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.EventRating;
import io.droidevs.mclub.domain.EventRegistration;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.repository.EventRatingRepository;
import io.droidevs.mclub.repository.EventRegistrationRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventRatingServiceTest {

    @Autowired EventRepository eventRepository;
    @Autowired EventRegistrationRepository eventRegistrationRepository;
    @Autowired EventRatingRepository eventRatingRepository;
    @Autowired UserRepository userRepository;
    @Autowired EventRatingService eventRatingService;

    @Test
    void studentCanRateOnlyIfRegistered_andAfterEventEnds() {
        User student = userRepository.save(User.builder()
                .email("s@test.com")
                .password("x")
                .fullName("Student")
                .role(Role.STUDENT)
                .build());

        Event event = eventRepository.save(Event.builder()
                .title("E")
                .description("D")
                .location("L")
                .startDate(LocalDateTime.now().minusHours(2))
                .endDate(LocalDateTime.now().plusHours(1))
                .createdBy(student)
                .build());

        eventRegistrationRepository.save(EventRegistration.builder().event(event).user(student).build());

        EventRatingRequest req = new EventRatingRequest();
        req.setRating(5);
        req.setComment("great");

        // too early
        assertThrows(RuntimeException.class, () -> eventRatingService.rateEvent(event.getId(), req, student.getEmail()));

        // move end date to past -> now allowed
        event.setEndDate(LocalDateTime.now().minusMinutes(1));
        eventRepository.save(event);

        var dto = eventRatingService.rateEvent(event.getId(), req, student.getEmail());
        assertEquals(5, dto.getRating());
        assertEquals(event.getId(), dto.getEventId());

        // second time updates same row
        req.setRating(3);
        var dto2 = eventRatingService.rateEvent(event.getId(), req, student.getEmail());
        assertEquals(3, dto2.getRating());
        assertEquals(1, eventRatingRepository.findByEventId(event.getId()).size());

        EventRating stored = eventRatingRepository.findByEventIdAndStudentId(event.getId(), student.getId()).orElseThrow();
        assertEquals(3, stored.getRating());
    }

    @Test
    void platformAdminCannotRate() {
        User admin = userRepository.save(User.builder()
                .email("a@test.com")
                .password("x")
                .fullName("Admin")
                .role(Role.PLATFORM_ADMIN)
                .build());

        Event event = eventRepository.save(Event.builder()
                .title("E")
                .description("D")
                .location("L")
                .startDate(LocalDateTime.now().minusHours(2))
                .endDate(LocalDateTime.now().minusHours(1))
                .createdBy(admin)
                .build());

        EventRatingRequest req = new EventRatingRequest();
        req.setRating(4);

        assertThrows(RuntimeException.class, () -> eventRatingService.rateEvent(event.getId(), req, admin.getEmail()));
    }
}
