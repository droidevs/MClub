package io.droidevs.mclub.bootstrap;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    // add repositories needed for seeding comments/ratings
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final EventRatingRepository eventRatingRepository;

    @Override
    public void run(String... args) {
        if (!isSchemaReady()) {
            log.warn("Skipping DataSeeder: DB schema isn't ready yet (missing required tables). Ensure Flyway migrations were applied.");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Database already seeded");
            return;
        }

        log.info("Seeding database with test users...");

        // Create Platform Admin User
        User admin = User.builder()
                .fullName("System Administrator")
                .email("admin@mclub.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.PLATFORM_ADMIN)
                .build();
        userRepository.save(admin);

        // Create Student User
        User member = User.builder()
                .fullName("John Student")
                .email("member@mclub.com")
                .password(passwordEncoder.encode("member123"))
                .role(Role.STUDENT)
                .build();
        userRepository.save(member);

        // Create Second Student User
        User member2 = User.builder()
                .fullName("Sara Student")
                .email("student2@mclub.com")
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT)
                .build();
        userRepository.save(member2);

        // Create Manager User (still a STUDENT globally; gets club ADMIN via membership)
        User clubAdmin = User.builder()
                .fullName("Club Manager")
                .email("manager@mclub.com")
                .password(passwordEncoder.encode("manager123"))
                .role(Role.STUDENT)
                .build();
        userRepository.save(clubAdmin);

        log.info("Seeding clubs...");

        Club photographyClub = Club.builder()
                .name("Photography Club")
                .description("A community for photography enthusiasts to share tips and organize photowalks.")
                .createdBy(admin)
                .build();
        clubRepository.save(photographyClub);

        Club codingClub = Club.builder()
                .name("Coding Club")
                .description("Learn to code together and build awesome projects.")
                .createdBy(admin)
                .build();
        clubRepository.save(codingClub);

        // Assign manager as ADMIN of Coding Club (club-scoped)
        membershipRepository.save(Membership.builder()
                .user(clubAdmin)
                .club(codingClub)
                .role(ClubRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .build());

        log.info("Seeding events...");

        Event photoWalk = Event.builder()
                .title("Downtown Photowalk")
                .description("Join us for a morning photowalk exploring the historic downtown architecture.")
                .location("City Center Plaza")
                .startDate(LocalDateTime.now().plusDays(2).withHour(9).withMinute(0))
                .endDate(LocalDateTime.now().plusDays(2).withHour(12).withMinute(0))
                .club(photographyClub)
                .createdBy(admin)
                .build();
        eventRepository.save(photoWalk);

        Event hackathon = Event.builder()
                .title("Weekend Hackathon")
                .description("48-hour coding marathon. Food and drinks provided!")
                .location("University Tech Hub")
                .startDate(LocalDateTime.now().plusDays(5).withHour(18).withMinute(0))
                .endDate(LocalDateTime.now().plusDays(7).withHour(18).withMinute(0))
                .club(codingClub)
                .createdBy(admin)
                .build();
        eventRepository.save(hackathon);

        // Seed comment threads + likes + ratings
        seedCommentsAndRatings(List.of(photoWalk, hackathon), member, member2);

        log.info("Database seeding complete!");
        log.info("Test Accounts:");
        log.info("Platform Admin: admin@mclub.com / admin123");
        log.info("Manager (student + club ADMIN): manager@mclub.com / manager123");
        log.info("Student: member@mclub.com / member123");
        log.info("Student 2: student2@mclub.com / student123");
    }

    private boolean isSchemaReady() {
        // We check for a couple of core tables. If these aren't present, any repository call will crash.
        return tableExists("users") && tableExists("clubs") && tableExists("events");
    }

    private boolean tableExists(String tableName) {
        Integer c = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                Integer.class,
                tableName
        );
        return c != null && c > 0;
    }

    private void seedCommentsAndRatings(List<Event> events, User s1, User s2) {
        for (Event event : events) {
            // Comments
            Comment c1 = commentRepository.save(Comment.builder()
                    .targetType(CommentTargetType.EVENT)
                    .targetId(event.getId())
                    .parentId(null)
                    .author(s1)
                    .content("Really looking forward to this!")
                    .deleted(false)
                    .build());

            Comment c2 = commentRepository.save(Comment.builder()
                    .targetType(CommentTargetType.EVENT)
                    .targetId(event.getId())
                    .parentId(null)
                    .author(s2)
                    .content("Will there be beginner-friendly guidance?")
                    .deleted(false)
                    .build());

            // Replies
            Comment r1 = commentRepository.save(Comment.builder()
                    .targetType(CommentTargetType.EVENT)
                    .targetId(event.getId())
                    .parentId(c2.getId())
                    .author(s1)
                    .content("Yes — organizers usually share tips at the start.")
                    .deleted(false)
                    .build());

            // Likes
            commentLikeRepository.save(CommentLike.builder().comment(c2).user(s1).build());
            commentLikeRepository.save(CommentLike.builder().comment(c1).user(s2).build());
            commentLikeRepository.save(CommentLike.builder().comment(r1).user(s2).build());

            // Ratings: only allowed after event ended + registered + attended
            // We set the event ended for seeded sample so ratings display immediately.
            event.setStartDate(LocalDateTime.now().minusHours(2));
            event.setEndDate(LocalDateTime.now().minusHours(1));
            eventRepository.save(event);

            // register + attend
            eventRegistrationRepository.save(EventRegistration.builder().event(event).user(s1).build());
            eventRegistrationRepository.save(EventRegistration.builder().event(event).user(s2).build());

            eventAttendanceRepository.save(EventAttendance.builder()
                    .event(event)
                    .user(s1)
                    .method(AttendanceMethod.STUDENT_SCANNED_EVENT_QR)
                    .build());
            eventAttendanceRepository.save(EventAttendance.builder()
                    .event(event)
                    .user(s2)
                    .method(AttendanceMethod.STUDENT_SCANNED_EVENT_QR)
                    .build());

            // ratings
            eventRatingRepository.save(EventRating.builder().event(event).student(s1).rating(5).comment("Great event!").build());
            eventRatingRepository.save(EventRating.builder().event(event).student(s2).rating(4).comment("Well organized.").build());

            log.info("Seeded comments+ratings for event {}", event.getTitle());
        }
    }
}
