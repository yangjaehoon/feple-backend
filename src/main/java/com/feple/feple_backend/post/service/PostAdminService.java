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
    /** 회원 상세 모더레이션 요약 — 이 유저의 현재 블라인드 상태 게시글 수(삭제 제외). */
    long countBlindedPostsByUser(Long userId);
    List<PostResponseDto> getDeletedPosts(int limit);
    void restorePost(Long postId);
    /** 신고 누적으로 자동 블라인드된 게시글을 관리자가 되돌린다. */
    void unblindPost(Long postId);
    List<PostResponseDto> getRecentPostsByUser(Long userId, int limit);
    /** @return 토글 후 고정 여부 — 컨트롤러가 감사 로그 기록에 사용 */
    boolean togglePin(Long postId);
    /** 블라인드된 글도 관리자는 검토할 수 있어야 하므로 기본 findById로 모든 행을 조회한다. */
    PostResponseDto getPostForAdmin(Long postId);
    /** 블라인드된 글은 일반 목록/검색에 노출되지 않으므로 전용 목록으로 제공한다. */
    List<PostResponseDto> getBlindedPosts(int limit);
}
