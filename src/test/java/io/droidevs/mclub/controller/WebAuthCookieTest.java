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
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebAuthController.class)
@AutoConfigureMockMvc
@Import({io.droidevs.mclub.security.SecurityConfig.class, TestControllerAdviceMocks.class})
class WebAuthCookieTest {

    @Autowired MockMvc mvc;

    @MockBean AuthService authService;

    @Test
    void login_setsJwtCookie_withSameSiteAndMaxAge() throws Exception {
        when(authService.authenticateUser(any(AuthRequest.class))).thenReturn(new AuthResponse("token123"));

        mvc.perform(post("/login")
                        .param("email", "a@a.com")
                        .param("password", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwtToken=token123")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=")));
    }

    @Test
    void logout_clearsJwtCookie() throws Exception {
        mvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwtToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }
}

