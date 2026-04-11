package io.droidevs.mclub.bootstrap;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
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

        log.info("Database seeding complete!");
        log.info("Test Accounts:");
        log.info("Platform Admin: admin@mclub.com / admin123");
        log.info("Manager (student + club ADMIN): manager@mclub.com / manager123");
        log.info("Student: member@mclub.com / member123");
    }
}
