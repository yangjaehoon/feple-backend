package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 하드 삭제 시 FK 제약을 지키는 자식 레코드 삭제 순서(CommentLike → CommentReport → Comment)를
 * 한 곳에서 관리한다. CommentServiceImpl(게시글 삭제에 딸린 일괄 삭제)과
 * CommentReportService(신고 처리로 인한 단건 삭제) 양쪽에서 동일한 순서가 필요해 통합했다.
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
        commentRepository.deleteById(commentId);
    }
}
