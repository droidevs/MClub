package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds "current user" fields to the model for all Thymeleaf controllers.
 *
 * <p>Spring Security's Thymeleaf extras can already do authorize checks, but we also
 * want to display user's name/email/role in the navbar reliably.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class CurrentUserModelAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("currentUserName")
    public String currentUserName(Authentication auth) {
        User u = currentUser(auth);
        if (u != null && u.getFullName() != null && !u.getFullName().isBlank()) {
            return u.getFullName();
        }
        return auth != null && auth.isAuthenticated() && auth.getName() != null ? auth.getName() : null;
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Authentication auth) {
        User u = currentUser(auth);
        if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
            return u.getEmail();
        }
        return auth != null && auth.isAuthenticated() && auth.getName() != null ? auth.getName() : null;
    }

    @ModelAttribute("currentUserRole")
    public String currentUserRole(Authentication auth) {
        User u = currentUser(auth);
        if (u != null && u.getRole() != null) {
            return u.getRole().name();
        }

        Set<String> roles = authRoles(auth);
        if (roles.contains("ROLE_PLATFORM_ADMIN")) {
            return "PLATFORM_ADMIN";
        }
        if (roles.contains("ROLE_STUDENT")) {
            return "STUDENT";
        }
        return null;
    }

    private Set<String> authRoles(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
