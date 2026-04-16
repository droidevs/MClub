package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.mapper.EventCreateRequestMapper;
import io.droidevs.mclub.service.EventService;
import io.droidevs.mclub.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(io.droidevs.mclub.security.SecurityConfig.class)
class EventControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    EventService eventService;
    @MockBean
    RegistrationService registrationService;
    @MockBean EventCreateRequestMapper eventCreateRequestMapper;


    @Test
    void getEventsByClub_shouldPermitAnonymous() throws Exception {
        UUID clubId = UUID.randomUUID();
        when(eventService.getEventsByClub(clubId)).thenReturn(List.of());

        mvc.perform(get("/api/events/club/{clubId}", clubId))
                .andExpect(status().isOk());
    }

    @Test
    void createEvent_shouldReturn401_whenAnonymous() throws Exception {
        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "PLATFORM_ADMIN")
    void createEvent_shouldReturn200_whenAuthenticated_andValidBody() throws Exception {
        EventCreateRequest req = new EventCreateRequest();
        req.setClubId(UUID.randomUUID());
        req.setTitle("t");
        req.setDescription("d");
        req.setLocation("loc");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(req.getStartDate().plusHours(1));

        EventDto mapped = new EventDto();
        mapped.setClubId(req.getClubId());

        EventDto out = new EventDto();
        out.setId(UUID.randomUUID());

        when(eventCreateRequestMapper.toDto(any(EventCreateRequest.class))).thenReturn(mapped);
        when(eventService.createEvent(eq(mapped), eq("admin@example.com"))).thenReturn(out);

        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"clubId\":\"" + req.getClubId() + "\"," +
                                "\"title\":\"t\"," +
                                "\"description\":\"d\"," +
                                "\"location\":\"loc\"," +
                                "\"startDate\":\"" + req.getStartDate() + "\"," +
                                "\"endDate\":\"" + req.getEndDate() + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        verify(eventService).createEvent(eq(mapped), eq("admin@example.com"));
    }

    @Test
    void registrationsSummary_shouldPermitAnonymous() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(registrationService.countRegistrations(eventId)).thenReturn(3L);

        mvc.perform(get("/api/events/{eventId}/registrations/summary", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.count").value(3));
    }
}

