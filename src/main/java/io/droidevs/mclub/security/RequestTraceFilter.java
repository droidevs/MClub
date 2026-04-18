package io.droidevs.mclub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Low-level request tracing to debug auth/login/logout flow.
 *
 * <p>This logs for every request whether the jwtToken cookie is present and what Spring Security
 * thinks the current Authentication is, plus the final HTTP status.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        boolean hasJwtCookie = false;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("jwtToken".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    hasJwtCookie = true;
                    break;
                }
            }
        }

        Authentication before = SecurityContextHolder.getContext().getAuthentication();
        String beforeName = before != null ? before.getName() : null;

        // Only log auth-relevant paths at INFO to avoid noise.
        boolean interesting = uri.startsWith("/login") || uri.startsWith("/register") || uri.startsWith("/logout") || uri.startsWith("/api/me") || "/".equals(uri);
        if (interesting) {
            log.info("TRACE -> {} {} hasJwtCookie={} authBefore={}", method, uri, hasJwtCookie, beforeName);
        } else if (log.isDebugEnabled()) {
            log.debug("TRACE -> {} {} hasJwtCookie={} authBefore={}", method, uri, hasJwtCookie, beforeName);
        }

        filterChain.doFilter(request, response);

        // Log immediately after controller/filters have run (before SecurityContext is cleared).
        Authentication after = SecurityContextHolder.getContext().getAuthentication();
        String afterName = after != null ? after.getName() : null;
        if (interesting) {
            log.info("TRACE <- {} {} status={} authAfter={}", method, uri, response.getStatus(), afterName);
        } else if (log.isDebugEnabled()) {
            log.debug("TRACE <- {} {} status={} authAfter={}", method, uri, response.getStatus(), afterName);
        }
    }
}
