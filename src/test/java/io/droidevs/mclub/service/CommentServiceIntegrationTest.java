package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.dto.CommentPreviewDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.repository.EventRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for CommentService.
 *
 * WHY each @Nested class has its own @BeforeEach:
 *   JUnit 5 creates a separate instance for each @Nested inner class.
 *   A @BeforeEach on the OUTER class runs for the outer instance but does
 *   NOT automatically populate the outer-class fields that the inner class
 *   instance references. The fix is to have each @Nested class call the
 *   shared initFixtures() method from its own @BeforeEach so that the
 *   outer-class fields (student, event, …) are always non-null when any
 *   test body executes.
 */
@SpringBootTest
@Transactional
@DisplayName("CommentService Integration Tests")
class CommentServiceIntegrationTest {


    // -----------------------------------------------------------------------
    // Injected beans
    // -----------------------------------------------------------------------

    @Autowired private CommentService commentService;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // -----------------------------------------------------------------------
    // Shared fixtures — populated by initFixtures()
    // -----------------------------------------------------------------------

    User student;
    User anotherStudent;
    User nonStudent;
    Event event;

    // -----------------------------------------------------------------------
    // Shared setup — called explicitly from every @Nested @BeforeEach
    // -----------------------------------------------------------------------

    void initFixtures() {
        System.out.println("test");
        student = userRepository.save(User.builder()
                .fullName("John Student")
                .email("student@test.com")
                .password(passwordEncoder.encode("student123"))
                .role(Role.STUDENT)
                .build());

        System.out.println(student != null);

        anotherStudent = userRepository.save(User.builder()
                .fullName("John Student")
                .email("another@test.com")
                .password(passwordEncoder.encode("another123"))
                .role(Role.STUDENT)
                .build());

        // Replace Role.ADMIN with whatever non-STUDENT role your enum exposes
        nonStudent = userRepository.save(User.builder()
                .fullName("System Administrator")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.PLATFORM_ADMIN)
                .build());

        event = eventRepository.save(Event.builder()
                .title("Test Event")
                .build());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    CommentDto postComment(String content, String email) {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(content);
        return commentService.addComment(CommentTargetType.EVENT, event.getId(), req, email);
    }

    CommentDto postReply(String content, UUID parentId, String email) {
        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(content);
        req.setParentId(parentId);
        return commentService.addComment(CommentTargetType.EVENT, event.getId(), req, email);
    }

    // =======================================================================
    // 1. addComment()
    // =======================================================================

    @Nested
    @DisplayName("addComment()")
    class AddComment {

        @BeforeEach
        void setUp() { initFixtures(); }

        @Test
        @DisplayName("Student posts a root comment → returns a DTO with a valid ID and correct content")
        void student_postsRootComment_success() {
            CommentDto created = postComment("hello world", student.getEmail());

            assertThat(created.getId()).isNotNull();
            assertThat(created.getContent()).isEqualTo("hello world");
            assertThat(created.getParentId()).isNull();
        }

        @Test
        @DisplayName("Content is trimmed before persisting")
        void content_isTrimmed() {
            CommentDto created = postComment("  spaces around  ", student.getEmail());

            assertThat(created.getContent()).isEqualTo("spaces around");
        }

        @Test
        @DisplayName("Non-student posting a comment throws ForbiddenException")
        void nonStudent_throwsForbidden() {
            assertThatThrownBy(() -> postComment("hi", nonStudent.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Posting to a non-existent event throws a RuntimeException")
        void nonExistentTarget_throws() {
            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("hi");

            // The service calls findById().orElseThrow() — we assert RuntimeException
            // rather than a specific subtype to stay robust to implementation changes.
            assertThatThrownBy(() ->
                    commentService.addComment(
                            CommentTargetType.EVENT, UUID.randomUUID(), req, student.getEmail()))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Reply with a parentId that belongs to a different target throws ForbiddenException")
        void reply_parentBelongsToDifferentTarget_throwsForbidden() {
            Event otherEvent = eventRepository.save(Event.builder().title("Other Event").build());

            CommentCreateRequest rootReq = new CommentCreateRequest();
            rootReq.setContent("root on other event");
            CommentDto foreignRoot = commentService.addComment(
                    CommentTargetType.EVENT, otherEvent.getId(), rootReq, student.getEmail());

            CommentCreateRequest replyReq = new CommentCreateRequest();
            replyReq.setContent("sneaky reply");
            replyReq.setParentId(foreignRoot.getId());

            assertThatThrownBy(() ->
                    commentService.addComment(
                            CommentTargetType.EVENT, event.getId(), replyReq, student.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // =======================================================================
    // 2. reply()
    // =======================================================================

    @Nested
    @DisplayName("reply()")
    class Reply {

        @BeforeEach
        void setUp() { initFixtures(); }

        @Test
        @DisplayName("Valid reply sets parentId and inherits the parent's target")
        void validReply_setsParentId() {
            CommentDto root = postComment("root comment", student.getEmail());

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("my reply");
            CommentDto reply = commentService.reply(root.getId(), req, anotherStudent.getEmail());

            assertThat(reply.getId()).isNotNull();
            assertThat(reply.getParentId()).isEqualTo(root.getId());
            assertThat(reply.getContent()).isEqualTo("my reply");
        }

        @Test
        @DisplayName("Replying to a non-existent parent throws a RuntimeException")
        void reply_nonExistentParent_throws() {
            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("orphan reply");

            assertThatThrownBy(() ->
                    commentService.reply(UUID.randomUUID(), req, student.getEmail()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // =======================================================================
    // 3. getThread()
    // =======================================================================

    @Nested
    @DisplayName("getThread()")
    class GetThread {

        @BeforeEach
        void setUp() { initFixtures(); }

        @Test
        @DisplayName("Empty target returns an empty page")
        void emptyTarget_returnsEmptyPage() {
            Page<CommentDto> page = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), null);

            assertThat(page.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Root comments appear in the thread")
        void rootComments_appearInThread() {
            postComment("first",  student.getEmail());
            postComment("second", student.getEmail());

            Page<CommentDto> page = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), student.getEmail());

            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Replies are nested under their parent (repliesPreview), not returned as root comments")
        void replies_areNestedUnderParent() {
            CommentDto root = postComment("root", student.getEmail());
            postReply("reply 1", root.getId(), anotherStudent.getEmail());
            postReply("reply 2", root.getId(), anotherStudent.getEmail());

            Page<CommentDto> page = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), student.getEmail());

            // Only the root appears at page level — replies are nested inside it
            assertThat(page.getTotalElements()).isEqualTo(1);

            CommentDto rootDto = page.getContent().get(0);
            assertThat(rootDto.getReplyCount()).isEqualTo(2);
            assertThat(rootDto.getRepliesPreview()).hasSize(2);
        }

        @Test
        @DisplayName("Only the first 3 replies appear in repliesPreview; hasMoreReplies is true when there are >3")
        void repliesPreview_limitedToThree_hasMoreRepliesFlag() {
            CommentDto root = postComment("root", student.getEmail());
            for (int i = 1; i <= 4; i++) {
                postReply("reply " + i, root.getId(), anotherStudent.getEmail());
            }

            Page<CommentDto> page = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), student.getEmail());

            CommentDto rootDto = page.getContent().get(0);
            assertThat(rootDto.getReplyCount()).isEqualTo(4);
            assertThat(rootDto.getRepliesPreview()).hasSize(3);
            assertThat(rootDto.isHasMoreReplies()).isTrue();
        }

        @Test
        @DisplayName("hasMoreReplies is false when reply count is ≤ 3")
        void repliesPreview_noMoreFlag_whenThreeOrFewer() {
            CommentDto root = postComment("root", student.getEmail());
            postReply("only reply", root.getId(), anotherStudent.getEmail());

            Page<CommentDto> page = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), student.getEmail());

            assertThat(page.getContent().get(0).isHasMoreReplies()).isFalse();
        }

        @Test
        @DisplayName("likedByMe is true only for the user who liked the comment")
        void likedByMe_reflectsCurrentUser() {
            CommentDto comment = postComment("likeable", student.getEmail());
            commentService.toggleLike(comment.getId(), anotherStudent.getEmail());

            Page<CommentDto> asLiker = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), anotherStudent.getEmail());
            assertThat(asLiker.getContent().get(0).isLikedByMe()).isTrue();

            Page<CommentDto> asAuthor = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), student.getEmail());
            assertThat(asAuthor.getContent().get(0).isLikedByMe()).isFalse();
        }

        @Test
        @DisplayName("Unauthenticated user (null email) can read the thread; likedByMe is always false")
        void anonymousUser_canReadThread_likedByMeAlwaysFalse() {
            postComment("public comment", student.getEmail());

            Page<CommentDto> page = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 10), null);

            assertThat(page.isEmpty()).isFalse();
            assertThat(page.getContent().get(0).isLikedByMe()).isFalse();
        }

        @Test
        @DisplayName("Pagination splits root comments across pages correctly")
        void pagination_works() {
            for (int i = 1; i <= 5; i++) postComment("comment " + i, student.getEmail());

            Page<CommentDto> page0 = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(0, 3), null);
            Page<CommentDto> page1 = commentService.getThread(
                    CommentTargetType.EVENT, event.getId(), PageRequest.of(1, 3), null);

            assertThat(page0.getTotalElements()).isEqualTo(5);
            assertThat(page0.getContent()).hasSize(3);
            assertThat(page1.getContent()).hasSize(2);
        }
    }

    // =======================================================================
    // 4. getDirectReplies()
    // =======================================================================

    @Nested
    @DisplayName("getDirectReplies()")
    class GetDirectReplies {

        @BeforeEach
        void setUp() { initFixtures(); }

        @Test
        @DisplayName("Returns all direct replies for a given parent comment")
        void returnsDirectReplies() {
            CommentDto root = postComment("root", student.getEmail());
            postReply("r1", root.getId(), anotherStudent.getEmail());
            postReply("r2", root.getId(), anotherStudent.getEmail());

            Page<CommentDto> replies = commentService.getDirectReplies(
                    root.getId(), PageRequest.of(0, 10), student.getEmail());

            assertThat(replies.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Returns an empty page when a root comment has no replies")
        void noReplies_returnsEmptyPage() {
            CommentDto root = postComment("lonely root", student.getEmail());

            Page<CommentDto> replies = commentService.getDirectReplies(
                    root.getId(), PageRequest.of(0, 10), student.getEmail());

            assertThat(replies.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Paginating replies respects page size")
        void pagination_works() {
            CommentDto root = postComment("root", student.getEmail());
            for (int i = 1; i <= 5; i++) postReply("r" + i, root.getId(), anotherStudent.getEmail());

            Page<CommentDto> page0 = commentService.getDirectReplies(
                    root.getId(), PageRequest.of(0, 3), student.getEmail());

            assertThat(page0.getTotalElements()).isEqualTo(5);
            assertThat(page0.getContent()).hasSize(3);
        }
    }

    // =======================================================================
    // 5. toggleLike()
    // =======================================================================

    @Nested
    @DisplayName("toggleLike()")
    class ToggleLike {

        @BeforeEach
        void setUp() { initFixtures(); }

        @Test
        @DisplayName("First toggle adds a like; like count becomes 1")
        void firstToggle_addsLike() {
            CommentDto comment = postComment("likeable", student.getEmail());
            commentService.toggleLike(comment.getId(), anotherStudent.getEmail());

            assertThat(commentService.getComment(comment.getId()).getLikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Second toggle removes the like; like count returns to 0")
        void secondToggle_removesLike() {
            CommentDto comment = postComment("likeable", student.getEmail());
            commentService.toggleLike(comment.getId(), anotherStudent.getEmail());
            commentService.toggleLike(comment.getId(), anotherStudent.getEmail());

            assertThat(commentService.getComment(comment.getId()).getLikeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Multiple students liking the same comment accumulates correctly")
        void multipleLikers_accumulate() {
            CommentDto comment = postComment("popular", student.getEmail());
            commentService.toggleLike(comment.getId(), student.getEmail());
            commentService.toggleLike(comment.getId(), anotherStudent.getEmail());

            assertThat(commentService.getComment(comment.getId()).getLikeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Non-student attempting to like throws ForbiddenException")
        void nonStudent_throwsForbidden() {
            CommentDto comment = postComment("likeable", student.getEmail());

            assertThatThrownBy(() ->
                    commentService.toggleLike(comment.getId(), nonStudent.getEmail()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("Liking a non-existent comment throws a RuntimeException")
        void nonExistentComment_throws() {
            // student is valid — only the comment lookup will fail
            assertThatThrownBy(() ->
                    commentService.toggleLike(UUID.randomUUID(), student.getEmail()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // =======================================================================
    // 6. getPreview()
    // =======================================================================

    @Nested
    @DisplayName("getPreview()")
    class GetPreview {

        @BeforeEach
        void setUp() { initFixtures(); }

        @Test
        @DisplayName("Preview on empty target returns all-zero counts and empty lists")
        void emptyTarget_returnsZeroCounts() {
            CommentPreviewDto preview = commentService.getPreview(
                    CommentTargetType.EVENT, event.getId(), null);

            assertThat(preview.totalComments()).isZero();
            assertThat(preview.totalReplies()).isZero();
            assertThat(preview.totalLikes()).isZero();
            assertThat(preview.topComments()).isEmpty();
            assertThat(preview.latestComment()).isNull();
        }

        @Test
        @DisplayName("totalComments counts root comments only; totalReplies counts replies only")
        void counts_rootsAndRepliesSeparately() {
            CommentDto root1 = postComment("r1", student.getEmail());
            postComment("r2", student.getEmail());
            postReply("rep1", root1.getId(), anotherStudent.getEmail());

            CommentPreviewDto preview = commentService.getPreview(
                    CommentTargetType.EVENT, event.getId(), student.getEmail());

            assertThat(preview.totalComments()).isEqualTo(2);
            assertThat(preview.totalReplies()).isEqualTo(1);
        }

        @Test
        @DisplayName("topComments is capped at 3 entries")
        void topComments_cappedAtThree() {
            for (int i = 1; i <= 5; i++) postComment("c" + i, student.getEmail());

            CommentPreviewDto preview = commentService.getPreview(
                    CommentTargetType.EVENT, event.getId(), null);

            assertThat(preview.topComments()).hasSize(3);
        }

        @Test
        @DisplayName("latestComment is the most recent root comment")
        void latestComment_isMostRecent() {
            postComment("first", student.getEmail());
            CommentDto last = postComment("last", anotherStudent.getEmail());

            CommentPreviewDto preview = commentService.getPreview(
                    CommentTargetType.EVENT, event.getId(), null);

            assertThat(preview.latestComment().getId()).isEqualTo(last.getId());
        }

        @Test
        @DisplayName("totalLikes sums likes across the preview comments correctly")
        void totalLikes_sumsCorrectly() {
            CommentDto c1 = postComment("c1", student.getEmail());
            CommentDto c2 = postComment("c2", student.getEmail());

            commentService.toggleLike(c1.getId(), anotherStudent.getEmail()); // +1
            commentService.toggleLike(c2.getId(), anotherStudent.getEmail()); // +1
            commentService.toggleLike(c2.getId(), student.getEmail());        // +1 → total 3

            CommentPreviewDto preview = commentService.getPreview(
                    CommentTargetType.EVENT, event.getId(), null);

            assertThat(preview.totalLikes()).isEqualTo(3);
        }
    }
}