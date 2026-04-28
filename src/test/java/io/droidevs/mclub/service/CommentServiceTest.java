package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.dto.CommentPreviewDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.mapper.CommentFactoryMapper;
import io.droidevs.mclub.mapper.CommentLikeFactoryMapper;
import io.droidevs.mclub.mapper.CommentMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommentService}.
 *
 * <p>Conventions:
 * <ul>
 *   <li>AssertJ for fluent, readable assertions.</li>
 *   <li>Mockito strict stubs (MockitoExtension) – unused stubs fail fast.</li>
 *   <li>One {@code @Nested} class per public method.</li>
 *   <li>Test names: {@code shouldExpectedBehavior_whenCondition}.</li>
 *   <li>Private factory helpers keep arrange-blocks lean and DRY.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    // -------------------------------------------------------------------------
    // Mocks
    // -------------------------------------------------------------------------

    @Mock private CommentRepository       commentRepository;
    @Mock private CommentLikeRepository   commentLikeRepository;
    @Mock private UserRepository          userRepository;
    @Mock private EventRepository         eventRepository;
    @Mock private ActivityRepository      activityRepository;
    @Mock private CommentMapper           commentMapper;
    @Mock private CommentFactoryMapper    commentFactoryMapper;
    @Mock private CommentLikeFactoryMapper commentLikeFactoryMapper;

    @InjectMocks
    private CommentService service;

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------

    private static final String STUDENT_EMAIL = "student@mclub.io";
    private static final String ADMIN_EMAIL   = "admin@mclub.io";

    private UUID   studentId;
    private UUID   targetId;
    private User   studentUser;
    private User   adminUser;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        targetId  = UUID.randomUUID();

        studentUser = User.builder()
                .id(studentId)
                .email(STUDENT_EMAIL)
                .role(Role.STUDENT)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email(ADMIN_EMAIL)
                .role(Role.PLATFORM_ADMIN)
                .build();
    }

    // =========================================================================
    // getCommentEntity()
    // =========================================================================

    @Nested
    @DisplayName("getCommentEntity()")
    class GetCommentEntity {

        @Test
        @DisplayName("should return the comment entity when it exists")
        void shouldReturnComment_whenExists() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = comment(commentId, targetId, CommentTargetType.EVENT);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // Act
            Comment result = service.getCommentEntity(commentId);

            // Assert
            assertThat(result).isSameAs(comment);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when comment does not exist")
        void shouldThrow_whenCommentNotFound() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.getCommentEntity(commentId))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // =========================================================================
    // getComment()
    // =========================================================================

    @Nested
    @DisplayName("getComment()")
    class GetComment {

        @Test
        @DisplayName("should return a DTO with correct like count and reply count")
        void shouldReturnDto_withCorrectCounts() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = comment(commentId, targetId, CommentTargetType.EVENT);
            CommentDto expected = commentDto(commentId, null);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentLikeRepository.countByCommentId(commentId)).thenReturn(5L);
            when(commentRepository.countCommentByParentId(commentId)).thenReturn(3);
            when(commentMapper.toDto(comment, 5L, false, 3, false)).thenReturn(expected);

            // Act
            CommentDto result = service.getComment(commentId);

            // Assert
            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("should always set likedByMe=false (no user context available)")
        void shouldSetLikedByMeFalse_always() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = comment(commentId, targetId, CommentTargetType.EVENT);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentLikeRepository.countByCommentId(commentId)).thenReturn(0L);
            when(commentRepository.countCommentByParentId(commentId)).thenReturn(0);
            when(commentMapper.toDto(comment, 0L, false, 0, false)).thenReturn(new CommentDto());

            // Act
            service.getComment(commentId);

            // Assert – likedByMe is always false since there is no user context
            verify(commentMapper).toDto(comment, 0L, false, 0, false);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when comment does not exist")
        void shouldThrow_whenCommentNotFound() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.getComment(commentId))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // =========================================================================
    // addComment()
    // =========================================================================

    @Nested
    @DisplayName("addComment()")
    class AddComment {

        @Test
        @DisplayName("should save and return a DTO when request is valid for an EVENT target")
        void shouldSaveAndReturnDto_forEventTarget() {
            // Arrange
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.EVENT);
            CommentDto expected = commentDto(created.getId(), null);
            CommentCreateRequest req = request("hello", null);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentFactoryMapper.create(CommentTargetType.EVENT, targetId, null, "hello"))
                    .thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(created, 0L, false, 0, false)).thenReturn(expected);

            // Act
            CommentDto result = service.addComment(CommentTargetType.EVENT, targetId, req, STUDENT_EMAIL);

            // Assert
            assertThat(result).isSameAs(expected);
            verify(commentRepository).save(created);
        }

        @Test
        @DisplayName("should save and return a DTO when request is valid for an ACTIVITY target")
        void shouldSaveAndReturnDto_forActivityTarget() {
            // Arrange
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.ACTIVITY);
            CommentDto expected = commentDto(created.getId(), null);
            CommentCreateRequest req = request("hello", null);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(activityRepository.findById(targetId))
                    .thenReturn(Optional.of(Activity.builder().id(targetId).build()));
            when(commentFactoryMapper.create(CommentTargetType.ACTIVITY, targetId, null, "hello"))
                    .thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(created, 0L, false, 0, false)).thenReturn(expected);

            // Act
            CommentDto result = service.addComment(CommentTargetType.ACTIVITY, targetId, req, STUDENT_EMAIL);

            // Assert
            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("should trim content whitespace before saving")
        void shouldTrimContent_beforeSaving() {
            // Arrange – note the surrounding spaces; the factory must receive the trimmed version
            CommentCreateRequest req = request("  hello world  ", null);
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.EVENT);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentFactoryMapper.create(CommentTargetType.EVENT, targetId, null, "hello world"))
                    .thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(created, 0L, false, 0, false)).thenReturn(new CommentDto());

            // Act
            service.addComment(CommentTargetType.EVENT, targetId, req, STUDENT_EMAIL);

            // Assert
            verify(commentFactoryMapper).create(CommentTargetType.EVENT, targetId, null, "hello world");
        }

        @Test
        @DisplayName("should set author on the comment entity before saving")
        void shouldSetAuthor_onCommentBeforeSave() {
            // Arrange
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.EVENT);
            CommentCreateRequest req = request("hello", null);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentFactoryMapper.create(any(), any(), any(), any())).thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(any(), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(new CommentDto());

            // Act
            service.addComment(CommentTargetType.EVENT, targetId, req, STUDENT_EMAIL);

            // Assert
            assertThat(created.getAuthor()).isSameAs(studentUser);
            assertThat(created.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("should throw ForbiddenException when caller is not a STUDENT")
        void shouldThrow_whenCallerIsNotStudent() {
            // Arrange
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.addComment(CommentTargetType.EVENT, targetId, request("x", null), ADMIN_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("student");
        }

        @Test
        @DisplayName("should throw NoSuchElementException when EVENT target does not exist")
        void shouldThrow_whenEventTargetNotFound() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    service.addComment(CommentTargetType.EVENT, targetId, request("x", null), STUDENT_EMAIL))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when ACTIVITY target does not exist")
        void shouldThrow_whenActivityTargetNotFound() {
            // Arrange
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(activityRepository.findById(targetId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    service.addComment(CommentTargetType.ACTIVITY, targetId, request("x", null), STUDENT_EMAIL))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when parent comment belongs to a different target")
        void shouldThrow_whenParentBelongsToDifferentTarget() {
            // Arrange
            UUID parentId    = UUID.randomUUID();
            UUID otherTarget = UUID.randomUUID(); // ← different from targetId
            Comment parent = comment(parentId, otherTarget, CommentTargetType.EVENT);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.addComment(CommentTargetType.EVENT, targetId, request("reply", parentId), STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("target");
        }

        @Test
        @DisplayName("should throw ForbiddenException when parent comment has a different target type")
        void shouldThrow_whenParentHasDifferentTargetType() {
            // Arrange – same targetId but ACTIVITY vs EVENT
            UUID parentId = UUID.randomUUID();
            Comment parent = comment(parentId, targetId, CommentTargetType.ACTIVITY);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.addComment(CommentTargetType.EVENT, targetId, request("reply", parentId), STUDENT_EMAIL))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should allow reply when parent comment belongs to the same target")
        void shouldAllow_whenParentBelongsToSameTarget() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            Comment parent = comment(parentId, targetId, CommentTargetType.EVENT);
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.EVENT);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(commentFactoryMapper.create(CommentTargetType.EVENT, targetId, parentId, "reply"))
                    .thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(created, 0L, false, 0, false)).thenReturn(new CommentDto());

            // Act & Assert
            assertThatNoException().isThrownBy(() ->
                    service.addComment(CommentTargetType.EVENT, targetId, request("reply", parentId), STUDENT_EMAIL));
        }

        @Test
        @DisplayName("should never query activityRepository when target type is EVENT")
        void shouldNotQueryActivity_whenTargetIsEvent() {
            // Arrange
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.EVENT);
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentFactoryMapper.create(any(), any(), any(), any())).thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(any(), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(new CommentDto());

            // Act
            service.addComment(CommentTargetType.EVENT, targetId, request("x", null), STUDENT_EMAIL);

            // Assert
            verifyNoInteractions(activityRepository);
        }

        @Test
        @DisplayName("should never query eventRepository when target type is ACTIVITY")
        void shouldNotQueryEvent_whenTargetIsActivity() {
            // Arrange
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.ACTIVITY);
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(activityRepository.findById(targetId))
                    .thenReturn(Optional.of(Activity.builder().id(targetId).build()));
            when(commentFactoryMapper.create(any(), any(), any(), any())).thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(any(), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(new CommentDto());

            // Act
            service.addComment(CommentTargetType.ACTIVITY, targetId, request("x", null), STUDENT_EMAIL);

            // Assert
            verifyNoInteractions(eventRepository);
        }
    }

    // =========================================================================
    // toggleLike()
    // =========================================================================

    @Nested
    @DisplayName("toggleLike()")
    class ToggleLike {

        @Test
        @DisplayName("should save a new like when the user has not yet liked the comment")
        void shouldSaveLike_whenNotLiked() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = comment(commentId, targetId, CommentTargetType.EVENT);
            CommentLike newLike = CommentLike.builder().build();

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentLikeRepository.findByCommentIdAndUserId(commentId, studentId))
                    .thenReturn(Optional.empty());
            when(commentLikeFactoryMapper.create(comment, studentUser)).thenReturn(newLike);

            // Act
            service.toggleLike(commentId, STUDENT_EMAIL);

            // Assert
            verify(commentLikeRepository).save(newLike);
            verify(commentLikeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should delete the existing like when the user has already liked the comment")
        void shouldDeleteLike_whenAlreadyLiked() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = comment(commentId, targetId, CommentTargetType.EVENT);
            CommentLike existing = CommentLike.builder().build();

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentLikeRepository.findByCommentIdAndUserId(commentId, studentId))
                    .thenReturn(Optional.of(existing));

            // Act
            service.toggleLike(commentId, STUDENT_EMAIL);

            // Assert
            verify(commentLikeRepository).delete(existing);
            verify(commentLikeRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ForbiddenException when caller is not a STUDENT")
        void shouldThrow_whenCallerIsNotStudent() {
            // Arrange
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));

            // Act & Assert
            assertThatThrownBy(() -> service.toggleLike(UUID.randomUUID(), ADMIN_EMAIL))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("student");
        }

        @Test
        @DisplayName("should throw NoSuchElementException when comment does not exist")
        void shouldThrow_whenCommentNotFound() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.toggleLike(commentId, STUDENT_EMAIL))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("should use the correct userId when querying for an existing like")
        void shouldQueryLike_withCorrectUserId() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = comment(commentId, targetId, CommentTargetType.EVENT);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentLikeRepository.findByCommentIdAndUserId(commentId, studentId))
                    .thenReturn(Optional.empty());
            when(commentLikeFactoryMapper.create(comment, studentUser)).thenReturn(CommentLike.builder().build());

            // Act
            service.toggleLike(commentId, STUDENT_EMAIL);

            // Assert – exact IDs forwarded, no default/null mix-up
            verify(commentLikeRepository).findByCommentIdAndUserId(commentId, studentId);
        }
    }

    // =========================================================================
    // reply()
    // =========================================================================

    @Nested
    @DisplayName("reply()")
    class Reply {

        @Test
        @DisplayName("should delegate to addComment with parent's target type and target id")
        void shouldDelegateToAddComment_withParentTarget() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            Comment parent = comment(parentId, targetId, CommentTargetType.EVENT);
            Comment created = comment(UUID.randomUUID(), targetId, CommentTargetType.EVENT);
            CommentDto expected = commentDto(created.getId(), parentId);

            when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(eventRepository.findById(targetId))
                    .thenReturn(Optional.of(Event.builder().id(targetId).build()));
            when(commentFactoryMapper.create(CommentTargetType.EVENT, targetId, parentId, "reply"))
                    .thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(created, 0L, false, 0, false)).thenReturn(expected);

            // Act
            CommentDto result = service.reply(parentId, request("reply", null), STUDENT_EMAIL);

            // Assert
            assertThat(result).isSameAs(expected);
            // Verifies the parentId is forwarded correctly into the factory
            verify(commentFactoryMapper).create(CommentTargetType.EVENT, targetId, parentId, "reply");
        }

        @Test
        @DisplayName("should throw NoSuchElementException when parent comment does not exist")
        void shouldThrow_whenParentNotFound() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            when(commentRepository.findById(parentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.reply(parentId, request("reply", null), STUDENT_EMAIL))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("should resolve target from parent – not from caller input")
        void shouldResolveTarget_fromParent() {
            // Arrange – parent lives in an ACTIVITY target
            UUID parentId     = UUID.randomUUID();
            UUID activityTargetId = UUID.randomUUID();
            Comment parent = comment(parentId, activityTargetId, CommentTargetType.ACTIVITY);
            Comment created = comment(UUID.randomUUID(), activityTargetId, CommentTargetType.ACTIVITY);

            when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(activityRepository.findById(activityTargetId))
                    .thenReturn(Optional.of(Activity.builder().id(activityTargetId).build()));
            when(commentFactoryMapper.create(CommentTargetType.ACTIVITY, activityTargetId, parentId, "hi"))
                    .thenReturn(created);
            when(commentRepository.save(created)).thenReturn(created);
            when(commentMapper.toDto(created, 0L, false, 0, false)).thenReturn(new CommentDto());

            // Act & Assert – should NOT throw; target is resolved entirely from parent
            assertThatNoException().isThrownBy(() ->
                    service.reply(parentId, request("hi", null), STUDENT_EMAIL));

            verifyNoInteractions(eventRepository);
        }
    }

    // =========================================================================
    // getThread()
    // =========================================================================

    @Nested
    @DisplayName("getThread()")
    class GetThread {

        @Test
        @DisplayName("should return an empty page without any further queries when there are no root comments")
        void shouldReturnEmptyPage_whenNoRootComments() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(commentRepository.findLatestComments(CommentTargetType.EVENT, targetId, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            Page<CommentDto> result = service.getThread(CommentTargetType.EVENT, targetId, pageable, null);

            // Assert
            assertThat(result).isEmpty();
            verifyNoInteractions(commentLikeRepository);
        }

        @Test
        @DisplayName("should set replyCount on each root comment equal to its reply count")
        void shouldSetReplyCount_onRootDto() {
            // Arrange
            UUID rootId  = UUID.randomUUID();
            UUID replyId = UUID.randomUUID();
            Comment root  = comment(rootId,  targetId, CommentTargetType.EVENT);
            Comment reply = Comment.builder().id(replyId).parentId(rootId)
                    .targetId(targetId).targetType(CommentTargetType.EVENT).build();

            Pageable pageable = PageRequest.of(0, 10);
            stubGetThread(pageable, List.of(root), List.of(reply), STUDENT_EMAIL);

            // Act
            Page<CommentDto> result = service.getThread(CommentTargetType.EVENT, targetId, pageable, STUDENT_EMAIL);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getReplyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should set hasMoreReplies=true when a root has more than 3 replies")
        void shouldSetHasMoreReplies_whenMoreThanThreeReplies() {
            // Arrange
            UUID rootId = UUID.randomUUID();
            Comment root = comment(rootId, targetId, CommentTargetType.EVENT);
            List<Comment> replies = buildReplies(rootId, 4);

            Pageable pageable = PageRequest.of(0, 10);
            stubGetThread(pageable, List.of(root), replies, STUDENT_EMAIL);

            // Act
            Page<CommentDto> result = service.getThread(CommentTargetType.EVENT, targetId, pageable, STUDENT_EMAIL);

            // Assert
            CommentDto rootDto = result.getContent().get(0);
            assertThat(rootDto.isHasMoreReplies()).isTrue();
        }

        @Test
        @DisplayName("should set hasMoreReplies=false when a root has 3 or fewer replies")
        void shouldSetHasMoreReplies_false_whenThreeOrFewerReplies() {
            // Arrange
            UUID rootId = UUID.randomUUID();
            Comment root = comment(rootId, targetId, CommentTargetType.EVENT);
            List<Comment> replies = buildReplies(rootId, 3);

            Pageable pageable = PageRequest.of(0, 10);
            stubGetThread(pageable, List.of(root), replies, STUDENT_EMAIL);

            // Act
            Page<CommentDto> result = service.getThread(CommentTargetType.EVENT, targetId, pageable, STUDENT_EMAIL);

            // Assert
            assertThat(result.getContent().get(0).isHasMoreReplies()).isFalse();
        }

        @Test
        @DisplayName("should limit repliesPreview to at most 3 entries even if more exist")
        void shouldLimitRepliesPreview_toThree() {
            // Arrange
            UUID rootId = UUID.randomUUID();
            Comment root = comment(rootId, targetId, CommentTargetType.EVENT);
            List<Comment> replies = buildReplies(rootId, 5); // 5 replies → only 3 in preview

            Pageable pageable = PageRequest.of(0, 10);
            stubGetThread(pageable, List.of(root), replies, STUDENT_EMAIL);

            // Act
            Page<CommentDto> result = service.getThread(CommentTargetType.EVENT, targetId, pageable, STUDENT_EMAIL);

            // Assert
            assertThat(result.getContent().get(0).getRepliesPreview()).hasSize(3);
        }

        @Test
        @DisplayName("should work when currentUserEmail is null (anonymous viewer)")
        void shouldWork_whenUserIsAnonymous() {
            // Arrange
            UUID rootId = UUID.randomUUID();
            Comment root = comment(rootId, targetId, CommentTargetType.EVENT);
            Pageable pageable = PageRequest.of(0, 10);

            stubGetThread(pageable, List.of(root), List.of(), null);

            // Act & Assert
            assertThatNoException().isThrownBy(() ->
                    service.getThread(CommentTargetType.EVENT, targetId, pageable, null));

            // Anonymous → userRepository must never be queried for non-null email
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("should propagate correct pagination metadata")
        void shouldPreservePaginationMetadata() {
            // Arrange
            UUID rootId = UUID.randomUUID();
            Comment root = comment(rootId, targetId, CommentTargetType.EVENT);
            Pageable pageable = PageRequest.of(0, 5);

            // totalElements = 20 (simulates a larger dataset)
            Page<Comment> rootPage = new PageImpl<>(List.of(root), pageable, 20);
            when(commentRepository.findLatestComments(CommentTargetType.EVENT, targetId, pageable))
                    .thenReturn(rootPage);
            when(commentRepository.findRepliesByParentIds(List.of(rootId))).thenReturn(List.of());
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());
            when(commentLikeRepository.findLikedCommentIds(any(), any())).thenReturn(Set.of());

            CommentDto rootDto = commentDto(rootId, null);
            when(commentMapper.toDto(eq(root), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(rootDto);
            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));

            // Act
            Page<CommentDto> result = service.getThread(CommentTargetType.EVENT, targetId, pageable, STUDENT_EMAIL);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(20);
            assertThat(result.getSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("should mark liked comments correctly when user has liked some")
        void shouldMarkLikedComments_correctly() {
            // Arrange
            UUID rootId  = UUID.randomUUID();
            UUID replyId = UUID.randomUUID();
            Comment root  = comment(rootId,  targetId, CommentTargetType.EVENT);
            Comment reply = Comment.builder().id(replyId).parentId(rootId)
                    .targetId(targetId).targetType(CommentTargetType.EVENT).build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> rootPage = new PageImpl<>(List.of(root), pageable, 1);

            when(userRepository.findByEmail(STUDENT_EMAIL)).thenReturn(Optional.of(studentUser));
            when(commentRepository.findLatestComments(CommentTargetType.EVENT, targetId, pageable))
                    .thenReturn(rootPage);
            when(commentRepository.findRepliesByParentIds(List.of(rootId))).thenReturn(List.of(reply));
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());
            // Student has liked the root but not the reply
            when(commentLikeRepository.findLikedCommentIds(eq(studentId), any()))
                    .thenReturn(Set.of(rootId));

            CommentDto rootDto  = commentDto(rootId, null);
            CommentDto replyDto = commentDto(replyId, rootId);
            when(commentMapper.toDto(eq(root),  anyLong(), eq(true),  anyInt(), anyBoolean())).thenReturn(rootDto);
            when(commentMapper.toDto(eq(reply), anyLong(), eq(false), anyInt(), anyBoolean())).thenReturn(replyDto);

            // Act
            service.getThread(CommentTargetType.EVENT, targetId, pageable, STUDENT_EMAIL);

            // Assert – root liked=true, reply liked=false
            verify(commentMapper).toDto(eq(root),  anyLong(), eq(true),  anyInt(), anyBoolean());
            verify(commentMapper).toDto(eq(reply), anyLong(), eq(false), anyInt(), anyBoolean());
        }
    }

    // =========================================================================
    // getPreview()
    // =========================================================================

    @Nested
    @DisplayName("getPreview()")
    class GetPreview {

        @Test
        @DisplayName("should return correct total counts from repository")
        void shouldReturnCorrectTotals() {
            // Arrange
            when(commentRepository.countAllByTarget(CommentTargetType.EVENT, targetId)).thenReturn(10L);
            when(commentRepository.countRepliesByTarget(CommentTargetType.EVENT, targetId)).thenReturn(4L);
            when(commentRepository.findLatestComments(eq(CommentTargetType.EVENT), eq(targetId), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            CommentPreviewDto result = service.getPreview(CommentTargetType.EVENT, targetId, null);

            // Assert
            assertThat(result.totalComments()).isEqualTo(10L);
            assertThat(result.totalReplies()).isEqualTo(4L);
        }

        @Test
        @DisplayName("should return empty latestDtos and null pinned when there are no comments")
        void shouldReturnEmptyPreview_whenNoComments() {
            // Arrange
            when(commentRepository.countAllByTarget(any(), any())).thenReturn(0L);
            when(commentRepository.countRepliesByTarget(any(), any())).thenReturn(0L);
            when(commentRepository.findLatestComments(eq(CommentTargetType.EVENT), eq(targetId), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            CommentPreviewDto result = service.getPreview(CommentTargetType.EVENT, targetId, null);

            // Assert
            assertThat(result.topComments()).isEmpty();
            assertThat(result.latestComment()).isNull();
        }

        @Test
        @DisplayName("should set pinned to first latest DTO when comments exist")
        void shouldSetPinned_toFirstLatestDto() {
            // Arrange
            UUID cId = UUID.randomUUID();
            Comment c = comment(cId, targetId, CommentTargetType.EVENT);
            CommentDto dto = commentDto(cId, null);

            when(commentRepository.countAllByTarget(any(), any())).thenReturn(1L);
            when(commentRepository.countRepliesByTarget(any(), any())).thenReturn(0L);
            when(commentRepository.findLatestComments(eq(CommentTargetType.EVENT), eq(targetId), any()))
                    .thenReturn(new PageImpl<>(List.of(c)));
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());
            when(commentMapper.toDto(eq(c), anyLong(), anyBoolean(), anyInt(), anyBoolean())).thenReturn(dto);

            // Act
            CommentPreviewDto result = service.getPreview(CommentTargetType.EVENT, targetId, null);

            // Assert
            assertThat(result.latestComment()).isSameAs(dto);
            assertThat(result.topComments()).containsExactly(dto);
        }

        @Test
        @DisplayName("should sum like counts across all latest comments correctly")
        void shouldSumLikeCounts() {
            // Arrange
            UUID c1Id = UUID.randomUUID();
            UUID c2Id = UUID.randomUUID();
            Comment c1 = comment(c1Id, targetId, CommentTargetType.EVENT);
            Comment c2 = comment(c2Id, targetId, CommentTargetType.EVENT);

            when(commentRepository.countAllByTarget(any(), any())).thenReturn(2L);
            when(commentRepository.countRepliesByTarget(any(), any())).thenReturn(0L);
            when(commentRepository.findLatestComments(eq(CommentTargetType.EVENT), eq(targetId), any()))
                    .thenReturn(new PageImpl<>(List.of(c1, c2)));
            // c1 has 3 likes, c2 has 7 likes → total = 10
            when(commentLikeRepository.countLikesBulk(any()))
                    .thenReturn(List.of(new Object[]{c1Id, 3L}, new Object[]{c2Id, 7L}));
            when(commentMapper.toDto(any(), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(new CommentDto());

            // Act
            CommentPreviewDto result = service.getPreview(CommentTargetType.EVENT, targetId, null);

            // Assert
            assertThat(result.totalLikes()).isEqualTo(10L);
        }

        @Test
        @DisplayName("should not query likedCommentIds when user is anonymous")
        void shouldNotQueryLikedIds_whenAnonymous() {
            // Arrange
            UUID cId = UUID.randomUUID();
            Comment c = comment(cId, targetId, CommentTargetType.EVENT);

            when(commentRepository.countAllByTarget(any(), any())).thenReturn(1L);
            when(commentRepository.countRepliesByTarget(any(), any())).thenReturn(0L);
            when(commentRepository.findLatestComments(eq(CommentTargetType.EVENT), eq(targetId), any()))
                    .thenReturn(new PageImpl<>(List.of(c)));
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());
            when(commentMapper.toDto(any(), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(new CommentDto());

            // Act
            service.getPreview(CommentTargetType.EVENT, targetId, null);

            // Assert – anonymous user: findLikedCommentIds must never be called
            verify(commentLikeRepository, never()).findLikedCommentIds(any(), any());
        }
    }

    // =========================================================================
    // getDirectReplies()
    // =========================================================================

    @Nested
    @DisplayName("getDirectReplies()")
    class GetDirectReplies {

        @Test
        @DisplayName("should return an empty page without further queries when there are no replies")
        void shouldReturnEmptyPage_whenNoReplies() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            when(commentRepository.findReplies(parentId, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            Page<CommentDto> result = service.getDirectReplies(parentId, pageable, null);

            // Assert
            assertThat(result).isEmpty();
            verifyNoInteractions(commentLikeRepository);
        }

        @Test
        @DisplayName("should set replyCount on each reply equal to its sub-reply count")
        void shouldSetReplyCount_forEachReply() {
            // Arrange
            UUID parentId  = UUID.randomUUID();
            UUID replyId   = UUID.randomUUID();
            UUID subReplyId = UUID.randomUUID();
            Comment reply    = Comment.builder().id(replyId).parentId(parentId).build();
            Comment subReply = Comment.builder().id(subReplyId).parentId(replyId).build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> replyPage = new PageImpl<>(List.of(reply), pageable, 1);

            when(commentRepository.findReplies(parentId, pageable)).thenReturn(replyPage);
            when(commentRepository.findRepliesByParentIds(List.of(replyId))).thenReturn(List.of(subReply));
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());

            CommentDto replyDto    = commentDto(replyId, parentId);
            CommentDto subReplyDto = commentDto(subReplyId, replyId);
            when(commentMapper.toDto(eq(reply),    anyLong(), anyBoolean(), anyInt(), anyBoolean())).thenReturn(replyDto);
            when(commentMapper.toDto(eq(subReply), anyLong(), anyBoolean(), anyInt(), anyBoolean())).thenReturn(subReplyDto);

            // Act
            Page<CommentDto> result = service.getDirectReplies(parentId, pageable, null);

            // Assert
            assertThat(result.getContent().get(0).getReplyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should limit sub-reply preview to 3 entries")
        void shouldLimitSubReplyPreview_toThree() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            UUID replyId  = UUID.randomUUID();
            Comment reply    = Comment.builder().id(replyId).parentId(parentId).build();
            List<Comment> subReplies = buildReplies(replyId, 5);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> replyPage = new PageImpl<>(List.of(reply), pageable, 1);

            when(commentRepository.findReplies(parentId, pageable)).thenReturn(replyPage);
            when(commentRepository.findRepliesByParentIds(List.of(replyId))).thenReturn(subReplies);
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());

            CommentDto replyDto = commentDto(replyId, parentId);
            when(commentMapper.toDto(eq(reply), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(replyDto);
            for (Comment sr : subReplies) {
                when(commentMapper.toDto(eq(sr), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                        .thenReturn(commentDto(sr.getId(), replyId));
            }

            // Act
            Page<CommentDto> result = service.getDirectReplies(parentId, pageable, null);

            // Assert
            assertThat(result.getContent().get(0).getRepliesPreview()).hasSize(3);
            assertThat(result.getContent().get(0).isHasMoreReplies()).isTrue();
        }

        @Test
        @DisplayName("should work when currentUserEmail is null (anonymous viewer)")
        void shouldWork_whenAnonymous() {
            // Arrange
            UUID parentId = UUID.randomUUID();
            UUID replyId  = UUID.randomUUID();
            Comment reply = Comment.builder().id(replyId).parentId(parentId).build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> replyPage = new PageImpl<>(List.of(reply), pageable, 1);

            when(commentRepository.findReplies(parentId, pageable)).thenReturn(replyPage);
            when(commentRepository.findRepliesByParentIds(List.of(replyId))).thenReturn(List.of());
            when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());
            when(commentMapper.toDto(eq(reply), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(commentDto(replyId, parentId));

            // Act & Assert
            assertThatNoException().isThrownBy(() ->
                    service.getDirectReplies(parentId, pageable, null));

            verifyNoInteractions(userRepository);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** Builds a minimal {@link Comment} with the given id, targetId, and targetType. */
    private static Comment comment(UUID id, UUID tId, CommentTargetType type) {
        return Comment.builder()
                .id(id)
                .targetId(tId)
                .targetType(type)
                .build();
    }

    /** Builds a minimal {@link CommentDto} with id and optional parentId pre-set. */
    private static CommentDto commentDto(UUID id, UUID parentId) {
        CommentDto dto = new CommentDto();
        dto.setId(id);
        dto.setParentId(parentId);
        return dto;
    }

    /** Builds a {@link CommentCreateRequest} with the given content and optional parentId. */
    private static CommentCreateRequest request(String content, UUID parentId) {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(content);
        req.setParentId(parentId);
        return req;
    }

    /**
     * Builds {@code count} reply {@link Comment}s all pointing at the given parentId.
     * Each gets a fresh random UUID.
     */
    private static List<Comment> buildReplies(UUID parentId, int count) {
        List<Comment> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(Comment.builder()
                    .id(UUID.randomUUID())
                    .parentId(parentId)
                    .build());
        }
        return list;
    }

    /**
     * Full stub setup for {@link CommentService#getThread} so individual tests only
     * need to assert on the outcome they care about.
     *
     * <p>All comments are mapped to DTOs; likes are empty; userEmail may be null.
     */
    private void stubGetThread(Pageable pageable, List<Comment> roots,
                               List<Comment> replies, String userEmail) {
        Page<Comment> rootPage = new PageImpl<>(roots, pageable, roots.size());

        if (userEmail != null) {
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(studentUser));
        }

        when(commentRepository.findLatestComments(CommentTargetType.EVENT, targetId, pageable))
                .thenReturn(rootPage);

        List<UUID> rootIds = roots.stream().map(Comment::getId).toList();
        when(commentRepository.findRepliesByParentIds(rootIds)).thenReturn(replies);
        when(commentLikeRepository.countLikesBulk(any())).thenReturn(List.of());
        when(commentLikeRepository.findLikedCommentIds(any(), any())).thenReturn(Set.of());

        // Map every comment (root + reply) to a DTO
        for (Comment root : roots) {
            CommentDto dto = commentDto(root.getId(), null);
            when(commentMapper.toDto(eq(root), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(dto);
        }
        for (Comment reply : replies) {
            CommentDto dto = commentDto(reply.getId(), reply.getParentId());
            when(commentMapper.toDto(eq(reply), anyLong(), anyBoolean(), anyInt(), anyBoolean()))
                    .thenReturn(dto);
        }
    }
}