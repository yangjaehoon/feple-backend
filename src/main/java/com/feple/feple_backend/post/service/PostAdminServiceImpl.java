package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.post.repository.PostRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostAdminServiceImpl implements PostAdminService {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, PostRelationFilterStrategy> relationFilterStrategies;

    public PostAdminServiceImpl(PostRepository postRepository,
                                ApplicationEventPublisher eventPublisher,
                                List<PostRelationFilterStrategy> relationFilterStrategies) {
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
        this.relationFilterStrategies = relationFilterStrategies.stream()
                .collect(Collectors.toMap(PostRelationFilterStrategy::filterKey, s -> s));
    }

    @Override
    public Page<PostResponseDto> getPostsForAdmin(PostAdminFilterDto params) {
        PageRequest pageable = PageRequest.of(params.page(), params.size());
        boolean hasKeyword = params.keyword() != null && !params.keyword().isBlank();
        String kw = JpqlLikeEscaper.escapeOrEmpty(params.keyword());

        // BoardType enum에 해당하는 필터(FREE, MATE, FESTIVAL_COMPANION 등)는 enum이 직접 처리.
        // 관계 필터(ARTIST, FESTIVAL 등)는 PostRelationFilterStrategy 빈으로 위임.
        // 새 BoardType/관계 필터를 추가해도 이 메서드를 수정할 필요 없음.
        return BoardType.fromAdminFilter(params.filter())
                .map(boardType -> hasKeyword
                        ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(boardType, kw, pageable)
                        : postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable))
                .orElseGet(() -> resolveRelationFilter(params, hasKeyword, kw, pageable))
                .map(PostResponseDto::from);
    }

    private Page<Post> resolveRelationFilter(PostAdminFilterDto params, boolean hasKeyword, String kw, PageRequest pageable) {
        PostRelationFilterStrategy strategy = relationFilterStrategies.get(params.filter());
        if (strategy != null) {
            return strategy.findPosts(params, hasKeyword, kw, pageable);
        }
        return hasKeyword
                ? postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(kw, pageable)
                : postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'totalPosts'")
    public long getTotalPostCount() {
        return postRepository.count();
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'recentPosts_' + #days")
    public long countRecentPosts(int days) {
        return postRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(days));
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'adminHotPosts_' + #limit")
    public List<PostResponseDto> getAdminHotPosts(int limit) {
        return postRepository.findPopularPosts(LocalDateTime.now().minusWeeks(1), PageRequest.of(0, limit))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        eventPublisher.publishEvent(new PostDeletedByAdminEvent(post.getUserId(), post.getTitle()));
        // bulkDeletePosts와 동일하게 소프트 삭제 — 휴지통(getDeletedPosts/restorePost)에서
        // 복구 가능해야 하므로 단건 삭제만 하드 삭제하면 안 됨
        postRepository.softDeleteByIds(List.of(postId));
    }

    @Override
    @Transactional
    public void bulkDeletePosts(List<Long> ids) {
        if (ids.isEmpty()) return;
        postRepository.softDeleteByIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPostsContaining(String word) {
        return postRepository.countByTitleOrContentContaining(JpqlLikeEscaper.escape(word.toLowerCase()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getPostCountsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return QueryResultMapper.toLongMap(postRepository.countGroupByUserId(userIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDto> getDeletedPosts(int limit) {
        return postRepository.findSoftDeleted(limit).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void restorePost(Long postId) {
        postRepository.restore(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDto> getRecentPostsByUser(Long userId, int limit) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream().map(PostResponseDto::from).toList();
    }
}
