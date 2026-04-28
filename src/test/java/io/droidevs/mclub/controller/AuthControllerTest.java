package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.AuthRequest;
import io.droidevs.mclub.dto.AuthResponse;
import io.droidevs.mclub.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc // enable Spring Security filters
@Import({io.droidevs.mclub.security.SecurityConfig.class, TestControllerAdviceMocks.class})
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean AuthService authService;

    @Test
    void login_shouldPermitAnonymous_andReturnToken() throws Exception {
        when(authService.authenticateUser(any(AuthRequest.class))).thenReturn(new AuthResponse("jwt"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void register_shouldPermitAnonymous() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\",\"email\":\"a@b.com\",\"password\":\"pw\"}"))
                .andExpect(status().isOk());

        verify(authService).registerUser(any());
    }
}
