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
 * deleteByPostIds는 벌크 DELETE라 @SQLDelete를 우회한 하드 삭제이고, deleteSingle은 deleteById
 * (@SQLDelete가 deleted_at을 세팅)로 소프트 삭제한다 — comment.parent_id가 자기참조 FK(RESTRICT)라
 * 대댓글이 달린 댓글을 하드 삭제하면 FK 위반이 나기 때문에 소프트 삭제가 의도된 동작이다.
 * CommentServiceImpl(게시글 삭제에 딸린 일괄 삭제)과 CommentReportService(신고 처리로 인한 단건
 * 삭제) 양쪽에서 동일한 정리 순서가 필요해 통합했다. 호출부의 트랜잭션 유무와 무관하게 삭제 순서의
 * 원자성을 보장하기 위해 클래스 레벨로 트랜잭션을 건다.
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
        // @SQLDelete가 걸려 있어 deleteById가 deleted_at을 세팅하는 소프트 삭제로 동작한다.
        commentRepository.deleteById(commentId);
    }
}
