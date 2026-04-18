package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minimal endpoint for the web UI to know who is logged in (email + roles).
 *
 * <p>Uses the same JwtAuthenticationFilter-based authentication as the rest of the app.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    public record MeResponse(boolean authenticated, String email, String fullName, Set<String> roles) {
    }

    @GetMapping("/api/me")
    public ResponseEntity<MeResponse> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            log.debug("GET /api/me -> anonymous");
            return ResponseEntity.ok(new MeResponse(false, null, null, Set.of()));
        }

        String email = auth.getName();
        Set<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        User u = userRepository.findByEmail(email).orElse(null);
        String fullName = u != null ? u.getFullName() : null;

        log.debug("GET /api/me -> authenticated email={} roles={}", email, roles);
        return ResponseEntity.ok(new MeResponse(true, email, fullName, roles));
    }
}
