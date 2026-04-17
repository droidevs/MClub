package io.droidevs.mclub.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provide JwtAuthenticationFilter if it's available; in slice tests it's often absent,
     * and we still want the SecurityFilterChain bean to be creatable.
     */
    @Bean(name = "jwtAuthenticationFilterForChain")
    public OncePerRequestFilter jwtAuthenticationFilterForChain(ObjectProvider<JwtAuthenticationFilter> provider) {
        JwtAuthenticationFilter filter = provider.getIfAvailable();
        if (filter != null) {
            return filter;
        }
        // no-op fallback
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OncePerRequestFilter jwtAuthenticationFilterForChain) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API should respond with 401 for anonymous and 403 for forbidden, without redirects
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")
                        )
                        .defaultAccessDeniedHandlerFor(
                                (request, response, accessDeniedException) -> response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden"),
                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // Public pages
                        .requestMatchers("/", "/clubs", "/events", "/login", "/register", "/css/**", "/js/**", "/error", "/favicon.ico").permitAll()
                        // Event detail page should be viewable when logged in (and is the redirect target after registration)
                        .requestMatchers("/events/*").authenticated()
                        // Web actions
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/events/*/register").hasRole("STUDENT")
                        .requestMatchers("/api/auth/**").permitAll()
                        // Public read-only APIs used by UI
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/events/*/ratings/summary",
                                "/api/comments/**").permitAll()
                        // club-admin pages are restricted by membership (club-scoped). Require login here; deeper checks happen in controller.
                        .requestMatchers("/club-admin/**").authenticated()
                        // Students can apply to create a club
                        .requestMatchers("/club-applications/apply", "/club-applications/apply-club", "/club-applications/submit").authenticated()
                        .requestMatchers("/club-applications/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(jwtAuthenticationFilterForChain, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
