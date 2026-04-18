package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.mapper.UserEntityMapper;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.JwtTokenProvider;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserEntityMapper userEntityMapper;

    public AuthResponse authenticateUser(AuthRequest request) {
        log.debug("AuthService.authenticateUser start email={}", request.getEmail());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = tokenProvider.generateToken(auth);
        log.debug("AuthService.authenticateUser success email={} authorities={}", auth.getName(), auth.getAuthorities());
        return new AuthResponse(token);
    }

    public void registerUser(RegisterRequest req) {
        log.debug("AuthService.registerUser start email={} roleReq={}", req.getEmail(), req.getRole());
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            log.info("AuthService.registerUser rejected (email exists) email={}", req.getEmail());
            throw new RuntimeException("Email already exists");
        }

        Role r;
        if (req.getRole() != null && req.getRole().equalsIgnoreCase("PLATFORM_ADMIN")) {
            r = Role.PLATFORM_ADMIN;
        } else {
            r = Role.STUDENT;
        }

        User user = userEntityMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(r);
        userRepository.save(user);

        log.info("AuthService.registerUser success email={} assignedRole={}", req.getEmail(), r);
    }
}
