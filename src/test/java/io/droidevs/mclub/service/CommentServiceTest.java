package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.mapper.CommentFactoryMapper;
import io.droidevs.mclub.mapper.CommentLikeFactoryMapper;
import io.droidevs.mclub.mapper.CommentMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private CommentFactoryMapper commentFactoryMapper;
    @Mock private CommentLikeFactoryMapper commentLikeFactoryMapper;

    @InjectMocks
    private CommentService service;

    private User student;
    private UUID targetId;

    @BeforeEach
    void init() {
        student = User.builder().id(UUID.randomUUID()).email("s@example.com").role(Role.STUDENT).fullName("Student").build();
        targetId = UUID.randomUUID();
    }

    @Test
    void addComment_shouldForbid_whenNotStudent() {
        User admin = User.builder().id(UUID.randomUUID()).email("a").role(Role.PLATFORM_ADMIN).build();
        when(userRepository.findByEmail("a")).thenReturn(Optional.of(admin));

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent("hi");

        assertThrows(ForbiddenException.class, () -> service.addComment(CommentTargetType.EVENT, targetId, req, "a"));
    }

    @Test
    void addComment_shouldValidateParentBelongsToSameTarget() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(eventRepository.findById(targetId)).thenReturn(Optional.of(Event.builder().id(targetId).build()));

        UUID parentId = UUID.randomUUID();
        Comment otherParent = Comment.builder()
                .id(parentId)
                .targetType(CommentTargetType.EVENT)
                .targetId(UUID.randomUUID())
                .author(student)
                .content("p")
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(otherParent));

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent("reply");
        req.setParentId(parentId);

        assertThrows(ForbiddenException.class, () -> service.addComment(CommentTargetType.EVENT, targetId, req, student.getEmail()));
    }

    @Test
    void getThread_shouldBuildTree_andNotThrow_whenRepliesAdded() {
        // This protects against the classic MapStruct replies=Collections.emptyList() bug.
        UUID rootId = UUID.randomUUID();
        UUID replyId = UUID.randomUUID();

        Comment root = Comment.builder().id(rootId).targetType(CommentTargetType.EVENT).targetId(targetId).parentId(null)
                .author(student).content("root").deleted(false).createdAt(LocalDateTime.now()).build();
        Comment reply = Comment.builder().id(replyId).targetType(CommentTargetType.EVENT).targetId(targetId).parentId(rootId)
                .author(student).content("reply").deleted(false).createdAt(LocalDateTime.now()).build();

        when(commentRepository.findThreadWithAuthor(CommentTargetType.EVENT, targetId)).thenReturn(List.of(root, reply));
        when(commentLikeRepository.countByCommentId(any())).thenReturn(0L);

        CommentDto rootDto = new CommentDto();
        rootDto.setId(rootId);
        rootDto.setReplies(new ArrayList<>()); // must be mutable
        CommentDto replyDto = new CommentDto();
        replyDto.setId(replyId);
        replyDto.setReplies(new ArrayList<>());

        when(commentMapper.toDto(eq(root), anyLong(), anyBoolean())).thenReturn(rootDto);
        when(commentMapper.toDto(eq(reply), anyLong(), anyBoolean())).thenReturn(replyDto);

        assertDoesNotThrow(() -> {
            List<CommentDto> out = service.getThread(CommentTargetType.EVENT, targetId, null);
            assertEquals(1, out.size());
            assertEquals(1, out.getFirst().getReplies().size());
        });
    }

    @Test
    void toggleLike_shouldForbid_whenNotStudent() {
        User admin = User.builder().id(UUID.randomUUID()).email("a").role(Role.PLATFORM_ADMIN).build();
        when(userRepository.findByEmail("a")).thenReturn(Optional.of(admin));

        assertThrows(ForbiddenException.class, () -> service.toggleLike(UUID.randomUUID(), "a"));
    }

    @Test
    void toggleLike_shouldDeleteExistingLike_whenAlreadyLiked() {
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        Comment c = Comment.builder().id(UUID.randomUUID()).author(student).targetType(CommentTargetType.EVENT).targetId(targetId).content("x").deleted(false).build();
        when(commentRepository.findById(c.getId())).thenReturn(Optional.of(c));

        CommentLike existing = CommentLike.builder().comment(c).user(student).build();
        when(commentLikeRepository.findByCommentIdAndUserId(c.getId(), student.getId())).thenReturn(Optional.of(existing));

        service.toggleLike(c.getId(), student.getEmail());
        verify(commentLikeRepository).delete(existing);
        verify(commentLikeRepository, never()).save(any());
    }
}

