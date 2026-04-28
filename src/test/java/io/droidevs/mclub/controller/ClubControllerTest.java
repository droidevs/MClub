package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.ClubDto;
import io.droidevs.mclub.service.ClubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
@AutoConfigureMockMvc // enable Spring Security filters
@Import({io.droidevs.mclub.security.SecurityConfig.class, TestControllerAdviceMocks.class})
class ClubControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ClubService clubService;

    @Test
    void getClubs_shouldReturn401_whenAnonymous_dueToAnyRequestAuthenticated() throws Exception {
        mvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "PLATFORM_ADMIN")
    void createClub_shouldAllowPlatformAdmin() throws Exception {
        ClubDto out = new ClubDto();
        out.setId(UUID.randomUUID());
        when(clubService.createClub(any(ClubDto.class), eq("admin@example.com"))).thenReturn(out);

        mvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"C\",\"description\":\"D\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void createClub_shouldForbidNonPlatformAdmin() throws Exception {
        mvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"C\",\"description\":\"D\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "u@example.com", roles = "STUDENT")
    void getClub_shouldReturn200_whenAuthenticated() throws Exception {
        UUID id = UUID.randomUUID();
        ClubDto dto = new ClubDto();
        dto.setId(id);
        when(clubService.getClub(id)).thenReturn(dto);

        mvc.perform(get("/api/clubs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(username = "u@example.com", roles = "STUDENT")
    void getClubs_shouldReturn200_whenAuthenticated() throws Exception {
        Page<ClubDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(clubService.getAllClubs(any())).thenReturn(page);

        mvc.perform(get("/api/clubs"))
                .andExpect(status().isOk());
    }
}
