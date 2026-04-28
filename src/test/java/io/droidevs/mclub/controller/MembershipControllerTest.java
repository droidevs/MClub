package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.dto.MembershipDto;
import io.droidevs.mclub.service.MembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MembershipController.class)
@AutoConfigureMockMvc // enable Spring Security filters
@Import({io.droidevs.mclub.security.SecurityConfig.class, TestControllerAdviceMocks.class})
class MembershipControllerTest {

    @Autowired MockMvc mvc;

    @MockBean MembershipService membershipService;

    @Test
    void joinClub_shouldReturn401_whenAnonymous() throws Exception {
        mvc.perform(post("/api/memberships/club/{clubId}/join", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void joinClub_shouldReturn200_whenAuthenticated() throws Exception {
        UUID clubId = UUID.randomUUID();
        MembershipDto dto = new MembershipDto();
        dto.setClubId(clubId);
        when(membershipService.joinClub(clubId, "s@example.com")).thenReturn(dto);

        mvc.perform(post("/api/memberships/club/{clubId}/join", clubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubId").value(clubId.toString()));
    }

    @Test
    @WithMockUser(username = "a@example.com", roles = "PLATFORM_ADMIN")
    void updateStatus_shouldAllowPlatformAdmin() throws Exception {
        UUID mid = UUID.randomUUID();
        when(membershipService.updateStatus(mid, MembershipStatus.APPROVED,"a@example.com")).thenReturn(new MembershipDto());

        mvc.perform(put("/api/memberships/{id}/status", mid)
                        .queryParam("status", "APPROVED")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void updateRole_shouldForbidNonPlatformAdmin() throws Exception {
        mvc.perform(put("/api/memberships/{id}/role", UUID.randomUUID())
                        .queryParam("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMembers_shouldRequireAuth_dueToSecurityConfigAnyRequestAuthenticated() throws Exception {
        mvc.perform(get("/api/memberships/club/{clubId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
