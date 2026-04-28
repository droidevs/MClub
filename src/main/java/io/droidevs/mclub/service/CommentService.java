package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.dto.CommentDto;
import io.droidevs.mclub.dto.CommentPreviewDto;
import io.droidevs.mclub.exception.ForbiddenException;
import io.droidevs.mclub.mapper.CommentMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    private final EventRepository eventRepository;
    private final ActivityRepository activityRepository;

    private final CommentMapper commentMapper;
    private final io.droidevs.mclub.mapper.CommentFactoryMapper commentFactoryMapper;
    private final io.droidevs.mclub.mapper.CommentLikeFactoryMapper commentLikeFactoryMapper;


    @Transactional(readOnly = true)
    public Comment getCommentEntity(UUID commentId) {
        return commentRepository.findById(commentId).orElseThrow();
    }

    public CommentDto getComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(c -> {
                    long likeCount = commentLikeRepository.countByCommentId(c.getId());
                    boolean likedByMe = false; // This method doesn't know the user, so we can't determine this
                    int replyCount = commentRepository.countCommentByParentId(c.getId());
                    return commentMapper.toDto(c, likeCount, likedByMe, replyCount, false);
                })
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getThread(
            CommentTargetType targetType,
            UUID targetId,
            Pageable pageable,
            String currentUserEmail
    ) {

        User me = currentUserEmail != null
                ? userRepository.findByEmail(currentUserEmail).orElse(null)
                : null;

        UUID myId = me != null ? me.getId() : null;

        // 1. ROOT COMMENTS (paged)
        Page<Comment> rootPage = commentRepository.findLatestComments(targetType, targetId, pageable);
        List<Comment> roots = rootPage.getContent();

        if (roots.isEmpty()) {
            return rootPage.map(c -> commentMapper.toDto(c, 0L, false, 0, false));
        }

        List<UUID> rootIds = roots.stream().map(Comment::getId).toList();

        // 2. REPLIES (only for roots)
        List<Comment> replies = commentRepository.findRepliesByParentIds(rootIds);

        // 3. FLAT LIST FOR LIKE CALCULATION
        List<Comment> all = new ArrayList<>();
        all.addAll(roots);
        all.addAll(replies);

        // 4. BULK LIKES (replace N+1)
        Map<UUID, Long> likeCounts = new HashMap<>();
        if (!all.isEmpty()) {
            List<UUID> allIds = all.stream().map(Comment::getId).toList();

            for (Object[] row : commentLikeRepository.countLikesBulk(allIds)) {
                likeCounts.put((UUID) row[0], (Long) row[1]);
            }
        }

        Set<UUID> likedByMe = commentLikeRepository.findLikedCommentIds(myId, likeCounts.keySet());

        // 5. DTO MAP
        Map<UUID, CommentDto> dtoMap = new HashMap<>();

        for (Comment c : all) {
            dtoMap.put(c.getId(),
                    commentMapper.toDto(
                            c,
                            likeCounts.getOrDefault(c.getId(), 0L),
                            likedByMe.contains(c.getId()), 0, false
                    )
            );
        }

        // 6. GROUP REPLIES
        Map<UUID, List<CommentDto>> repliesByParent = replies.stream()
                .map(r -> dtoMap.get(r.getId()))
                .collect(Collectors.groupingBy(dto -> dto.getParentId()));

        // 7. BUILD FINAL TREE (ONLY ROOTS + 3 REPLIES)
        List<CommentDto> result = new ArrayList<>();

        for (Comment root : roots) {

            CommentDto rootDto = dtoMap.get(root.getId());

            List<CommentDto> children = repliesByParent.getOrDefault(root.getId(), List.of());

            rootDto.setReplyCount(children.size());
            rootDto.setHasMoreReplies(children.size() > 3);

            // 🔥 LIMIT TO 3 ONLY
            rootDto.setRepliesPreview(children.stream()
                    .limit(3)
                    .toList());

            result.add(rootDto);
        }

        return new PageImpl<>(result, pageable, rootPage.getTotalElements());
    }


    @Transactional(readOnly = true)
    public CommentPreviewDto getPreview(
            CommentTargetType targetType,
            UUID targetId,
            String currentUserEmail
    ) {

        User me = currentUserEmail != null
                ? userRepository.findByEmail(currentUserEmail).orElse(null)
                : null;

        UUID myId = me != null ? me.getId() : null;

        // 1. COUNTS
        long totalComments = commentRepository.countAllByTarget(targetType, targetId);
        long totalReplies = commentRepository.countRepliesByTarget(targetType, targetId);

        // 2. LATEST COMMENTS (limit 3)
        Page<Comment> latestPage = commentRepository.findLatestComments(
                targetType,
                targetId,
                PageRequest.of(0, 3)
        );
        List<Comment> latest = latestPage.getContent();

        Set<UUID> ids = latest.stream().map(Comment::getId).collect(Collectors.toSet());

        Map<UUID, Long> likeCounts = new HashMap<>();
        Set<UUID> likedByMe;

        if (!ids.isEmpty()) {
            for (Object[] row : commentLikeRepository.countLikesBulk(ids.stream().toList())) {
                likeCounts.put((UUID) row[0], (Long) row[1]);
            }

            if (myId != null) {
                likedByMe = commentLikeRepository.findLikedCommentIds(myId, ids);
            } else {
                likedByMe = new HashSet<>();
            }
        } else {
            likedByMe = new HashSet<>();
        }

        List<CommentDto> latestDtos = latest.stream()
                .map(c -> commentMapper.toDto(
                        c,
                        likeCounts.getOrDefault(c.getId(), 0L),
                        likedByMe.contains(c.getId()), 0, false
                ))
                .toList();

        long totalLikes = likeCounts.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        CommentPreviewDto dto = new CommentPreviewDto(
                totalComments,
                totalReplies,
                totalLikes,
                latestDtos,
                latestDtos.isEmpty() ? null : latestDtos.get(0)
        );

        return dto;
    }


    /**
     * Load ALL direct replies for a given parent comment (children only; no deep nesting).
     * This powers the "See more replies" expansion on the comments page.
     */
    @Transactional(readOnly = true)
    public Page<CommentDto> getDirectReplies(
            UUID parentCommentId,
            Pageable pageable,
            String currentUserEmail
    ) {

        User me = currentUserEmail != null
                ? userRepository.findByEmail(currentUserEmail).orElse(null)
                : null;

        UUID myId = me != null ? me.getId() : null;

        // 1. PAGINATED DIRECT REPLIES
        Page<Comment> replyPage =
                commentRepository.findReplies(parentCommentId, pageable);

        List<Comment> replies = replyPage.getContent();

        if (replies.isEmpty()) {
            return replyPage.map(c -> commentMapper.toDto(c, 0L, false, 0, false));
        }

        // 2. SUB REPLIES (ONLY FOR THESE REPLIES)
        List<UUID> replyIds = replies.stream()
                .map(Comment::getId)
                .toList();

        List<Comment> subReplies =
                commentRepository.findRepliesByParentIds(replyIds);

        // 3. FLAT LIST
        List<Comment> all = new ArrayList<>();
        all.addAll(replies);
        all.addAll(subReplies);

        // 4. BULK LIKES (NO N+1)
        Map<UUID, Long> likeCounts = new HashMap<>();
        Set<UUID> likedByMe = new HashSet<>();

        if (!all.isEmpty()) {
            List<UUID> ids = all.stream().map(Comment::getId).toList();

            for (Object[] row : commentLikeRepository.countLikesBulk(ids)) {
                likeCounts.put((UUID) row[0], (Long) row[1]);
            }

            if (myId != null) {
                for (UUID id : ids) {
                    if (commentLikeRepository.findByCommentIdAndUserId(id, myId).isPresent()) {
                        likedByMe.add(id);
                    }
                }
            }
        }

        // 5. DTO MAP
        Map<UUID, CommentDto> dtoMap = new HashMap<>();

        for (Comment c : all) {
            dtoMap.put(c.getId(),
                    commentMapper.toDto(
                            c,
                            likeCounts.getOrDefault(c.getId(), 0L),
                            likedByMe.contains(c.getId()), 0, false
                    )
            );
        }

        // 6. GROUP SUB REPLIES
        Map<UUID, List<CommentDto>> subByParent = subReplies.stream()
                .map(c -> dtoMap.get(c.getId()))
                .collect(Collectors.groupingBy(CommentDto::getParentId));

        // 7. BUILD RESPONSE
        List<CommentDto> result = new ArrayList<>();

        for (Comment reply : replies) {

            CommentDto dto = dtoMap.get(reply.getId());

            List<CommentDto> children =
                    subByParent.getOrDefault(reply.getId(), List.of());

            dto.setReplyCount(children.size());
            dto.setHasMoreReplies(children.size() > 3);

            dto.setRepliesPreview(children.stream()
                    .limit(3)
                    .toList());

            result.add(dto);
        }

        return new PageImpl<>(result, pageable, replyPage.getTotalElements());
    }

    @Transactional
    public CommentDto addComment(CommentTargetType targetType, UUID targetId, CommentCreateRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();

        // Minimal guard: only STUDENT should post comments (controller also enforces)
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can comment");
        }

        // Validate target exists
        if (targetType == CommentTargetType.EVENT) {
            eventRepository.findById(targetId).orElseThrow();
        } else {
            activityRepository.findById(targetId).orElseThrow();
        }

        UUID parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId).orElseThrow();
            if (parent.getTargetType() != targetType || !parent.getTargetId().equals(targetId)) {
                throw new ForbiddenException("Parent comment does not belong to this target");
            }
        }

        Comment comment = commentFactoryMapper.create(targetType, targetId, parentId, request.getContent().trim());
        comment.setAuthor(student);
        comment.setDeleted(false);

        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved, 0L, false, 0, false);
    }

    @Transactional
    public void toggleLike(UUID commentId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow();
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can like comments");
        }

        Comment comment = commentRepository.findById(commentId).orElseThrow();

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndUserId(commentId, student.getId());
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
        } else {
            commentLikeRepository.save(commentLikeFactoryMapper.create(comment, student));
        }
    }

    @Transactional
    public CommentDto reply(UUID parentCommentId, CommentCreateRequest request, String studentEmail) {
        Comment parent = commentRepository.findById(parentCommentId).orElseThrow();

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(request.getContent());
        req.setParentId(parent.getId());

        return addComment(parent.getTargetType(), parent.getTargetId(), req, studentEmail);
    }
}
