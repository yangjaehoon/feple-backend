package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 삭제 시 자식 레코드(CommentLike, CommentReport)를 함께 정리하는 순서를 한 곳에서 관리한다.
 * deleteByPostIds는 벌크 DELETE라 @SQLDelete를 우회한 하드 삭제이고, deleteSingle은 softDeleteById
 * (벌크 UPDATE)로 소프트 삭제(deleted_at 세팅)한다 — comment.parent_id가 자기참조 FK(RESTRICT)라
 * 대댓글이 달린 댓글을 하드 삭제하면 FK 위반이 나기 때문에 소프트 삭제가 의도된 동작이고,
 * 블라인드된 댓글(blinded=true)은 @SQLRestriction에 걸려 findById 기반 삭제로는 찾을 수 없어
 * 제약을 우회하는 벌크 쿼리를 쓴다. CommentServiceImpl(게시글 삭제에 딸린 일괄 삭제)과
 * CommentReportService(신고 처리로 인한 단건 삭제) 양쪽에서 동일한 정리 순서가 필요해 통합했다.
 * 호출부의 트랜잭션 유무와 무관하게 삭제 순서의 원자성을 보장하기 위해 클래스 레벨로 트랜잭션을 건다.
 */
@Component
@RequiredArgsConstructor
@Transactional
public class CommentDeleter {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;

    public void deleteByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) return;
        commentLikeRepository.deleteByPostIds(postIds);
        commentReportRepository.deleteByPostIds(postIds);
        commentRepository.deleteByPostIds(postIds);
    }

    public void deleteSingle(Long commentId) {
        commentLikeRepository.deleteByCommentId(commentId);
        commentReportRepository.deleteByCommentId(commentId);
        // deleteById()는 findById()로 먼저 존재를 확인하는데, 블라인드된 댓글은 @SQLRestriction에
        // 걸려 못 찾아 실패한다 — 신고 누적으로 블라인드된 댓글도 삭제할 수 있어야 하므로
        // softDeleteById(벌크 UPDATE, 제약 우회)를 쓴다.
        commentRepository.softDeleteById(commentId);
    }
}
