package io.droidevs.mclub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectProvider<JwtTokenProvider> tokenProvider;
    private final ObjectProvider<UserDetailsService> customUserDetailsService;

    public JwtAuthenticationFilter(ObjectProvider<JwtTokenProvider> tokenProvider,
                                   ObjectProvider<UserDetailsService> customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        JwtTokenProvider provider = tokenProvider.getIfAvailable();
        UserDetailsService uds = customUserDetailsService.getIfAvailable();

        if (provider == null || uds == null) {
            log.debug("JWT filter no-op (missing beans) path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        try {
            String jwt = getJwtFromRequest(request);
            boolean hasJwt = jwt != null && !jwt.isBlank();
            if (log.isDebugEnabled()) {
                log.debug("JWT filter path={} method={} hasJwtCookieOrHeader={} authBefore={}",
                        path,
                        request.getMethod(),
                        hasJwt,
                        SecurityContextHolder.getContext().getAuthentication() != null ? SecurityContextHolder.getContext().getAuthentication().getName() : null);
            }

            if (jwt != null && provider.validateToken(jwt)) {
                String username = provider.getUsernameFromJWT(jwt);
                UserDetails userDetails = uds.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT filter authenticated path={} username={} authorities={}",
                        path, username, userDetails.getAuthorities());
            } else if (hasJwt) {
                log.debug("JWT filter found token but it is invalid/expired path={}", path);
            }
        } catch (UsernameNotFoundException ex) {
            log.info("JWT filter user not found for token subject; clearing context path={} msg={}", path, ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.error("JWT filter exception path={} msg={}", path, ex.getMessage(), ex);
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Try getting token from cookie first
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwtToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback to Header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
