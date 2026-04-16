package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.service.CommentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(io.droidevs.mclub.security.SecurityConfig.class)
class CommentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean CommentService commentService;

    @Test
    void getComments_shouldPermitAnonymous() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(commentService.getThread(CommentTargetType.EVENT, targetId, null)).thenReturn(List.of());

        mvc.perform(get("/api/comments/EVENT/{id}", targetId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void addComment_shouldAllowStudent() throws Exception {
        UUID targetId = UUID.randomUUID();
        CommentDto dto = new CommentDto();
        dto.setId(UUID.randomUUID());

        when(commentService.addComment(eq(CommentTargetType.EVENT), eq(targetId), any(CommentCreateRequest.class), eq("s@example.com")))
                .thenReturn(dto);

        mvc.perform(post("/api/comments/EVENT/{id}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hi\",\"parentId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void addComment_shouldReturn401_whenAnonymous() throws Exception {
        UUID targetId = UUID.randomUUID();
        mvc.perform(post("/api/comments/EVENT/{id}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hi\",\"parentId\":null}"))
                .andExpect(status().isUnauthorized());
    }
}
