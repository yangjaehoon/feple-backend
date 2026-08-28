package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;

public interface PostAdminService {
    Page<PostResponseDto> getPostsForAdmin(PostAdminFilterDto params);
    long getTotalPostCount();
    long countRecentPosts(int days);
    List<PostResponseDto> getAdminHotPosts(int limit);
    void deletePost(Long postId);
    void bulkDeletePosts(List<Long> ids);
    long countPostsContaining(String word);
    java.util.Map<Long, Long> getPostCountsByUserIds(java.util.List<Long> userIds);
    List<PostResponseDto> getDeletedPosts(int limit);
    void restorePost(Long postId);
    List<PostResponseDto> getRecentPostsByUser(Long userId, int limit);
    /** @return 토글 후 고정 여부 — 컨트롤러가 감사 로그 기록에 사용 */
    boolean togglePin(Long postId);
    /** 블라인드된 글도 관리자는 검토할 수 있어야 하므로 기본 findById로 모든 행을 조회한다. */
    PostResponseDto getPostForAdmin(Long postId);
    /** 블라인드된 글은 일반 목록/검색에 노출되지 않으므로 전용 목록으로 제공한다. */
    List<PostResponseDto> getBlindedPosts(int limit);
}
