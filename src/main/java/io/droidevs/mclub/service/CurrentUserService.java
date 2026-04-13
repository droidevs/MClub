package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Small helper to centralize "current user" lookup logic and reduce repeated repository calls. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User requireUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}

