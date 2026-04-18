package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.AuthRequest;
import io.droidevs.mclub.dto.AuthResponse;
import io.droidevs.mclub.dto.RegisterRequest;
import io.droidevs.mclub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebAuthController {

    private static final String JWT_COOKIE_NAME = "jwtToken";
    private static final long JWT_COOKIE_MAX_AGE_SECONDS = 24L * 60L * 60L;

    private final AuthService authService;

    @GetMapping("/login")
    public String showLoginForm(Model model, Authentication auth, HttpServletRequest request) {
        log.info("HIT WebAuthController#showLoginForm GET /login");
        boolean reallyLoggedIn = isReallyLoggedIn(auth, request);
        log.debug("GET /login - authPresent={}, authName={}, reallyLoggedIn={}, hasJwtCookie={}",
                auth != null,
                auth != null ? auth.getName() : null,
                reallyLoggedIn,
                hasNonEmptyJwtCookie(request));

        if (reallyLoggedIn) {
            return "redirect:/";
        }
        model.addAttribute("loginRequest", new AuthRequest());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginRequest") AuthRequest req,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model) {
        log.info("HIT WebAuthController#processLogin POST /login");
        String client = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        log.info("POST /login attempt email={} client={} ua={}", req.getEmail(), client, ua);

        try {
            AuthResponse authResponse = authService.authenticateUser(req);

            boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

            ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, authResponse.getToken())
                    .httpOnly(true)
                    .secure(secure)
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(JWT_COOKIE_MAX_AGE_SECONDS)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            log.info("POST /login success email={} setCookie[name={}, httpOnly={}, secure={}, sameSite={}, path=/, maxAgeSeconds={}]",
                    req.getEmail(), JWT_COOKIE_NAME, true, secure, "Lax", JWT_COOKIE_MAX_AGE_SECONDS);
            log.debug("POST /login redirecting to '/' for email={}", req.getEmail());

            return "redirect:/";
        } catch (Exception e) {
            log.warn("POST /login failed email={} client={} reason={}", req.getEmail(), client, e.getMessage());
            log.debug("POST /login failure stacktrace", e);
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, Authentication auth, HttpServletRequest request) {
        log.info("HIT WebAuthController#showRegisterForm GET /register");
        boolean reallyLoggedIn = isReallyLoggedIn(auth, request);
        log.debug("GET /register - authPresent={}, authName={}, reallyLoggedIn={}, hasJwtCookie={}",
                auth != null,
                auth != null ? auth.getName() : null,
                reallyLoggedIn,
                hasNonEmptyJwtCookie(request));

        if (reallyLoggedIn) {
            return "redirect:/";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest req, Model model) {
        log.info("HIT WebAuthController#processRegister POST /register");
        log.info("POST /register attempt email={} role={}", req.getEmail(), req.getRole());
        try {
            authService.registerUser(req);
            log.info("POST /register success email={} -> redirect /login?registered", req.getEmail());
            return "redirect:/login?registered";
        } catch (Exception e) {
            log.warn("POST /register failed email={} reason={}", req.getEmail(), e.getMessage());
            log.debug("POST /register failure stacktrace", e);
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("HIT WebAuthController#logout POST /logout");
        boolean secure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        log.info("POST /logout client={} secureRequest={} hasJwtCookieBefore={}",
                request.getRemoteAddr(), secure, hasNonEmptyJwtCookie(request));

        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("POST /logout setCookie[name={}, maxAgeSeconds=0, secure={}] -> redirect /login?logout", JWT_COOKIE_NAME, secure);
        return "redirect:/login?logout";
    }

    private boolean isReallyLoggedIn(Authentication auth, HttpServletRequest request) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        // Must also have a non-empty JWT cookie; this prevents redirecting away from /login
        // when the SecurityContext is stale after logout.
        if (request.getCookies() == null) {
            return false;
        }
        for (jakarta.servlet.http.Cookie c : request.getCookies()) {
            if (JWT_COOKIE_NAME.equals(c.getName())) {
                return c.getValue() != null && !c.getValue().isBlank();
            }
        }
        return false;
    }

    private boolean hasNonEmptyJwtCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (jakarta.servlet.http.Cookie c : request.getCookies()) {
            if (JWT_COOKIE_NAME.equals(c.getName())) {
                return c.getValue() != null && !c.getValue().isBlank();
            }
        }
        return false;
    }
}
