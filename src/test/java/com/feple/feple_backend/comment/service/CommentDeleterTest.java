package com.feple.feple_backend.comment.service;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentDeleterTest {

    @Mock CommentLikeRepository commentLikeRepository;
    @Mock CommentReportRepository commentReportRepository;
    @Mock CommentRepository commentRepository;

    @InjectMocks CommentDeleter commentDeleter;

    @Test
    void deleteByPostIds_빈목록이면_아무것도_안함() {
        commentDeleter.deleteByPostIds(List.of());

        then(commentLikeRepository).shouldHaveNoInteractions();
        then(commentReportRepository).shouldHaveNoInteractions();
        then(commentRepository).shouldHaveNoInteractions();
    }

    @Test
    void deleteByPostIds_FK순서대로_삭제() {
        commentDeleter.deleteByPostIds(List.of(1L, 2L));

        InOrder order = inOrder(commentLikeRepository, commentReportRepository, commentRepository);
        order.verify(commentLikeRepository).deleteByPostIds(List.of(1L, 2L));
        order.verify(commentReportRepository).deleteByPostIds(List.of(1L, 2L));
        order.verify(commentRepository).deleteByPostIds(List.of(1L, 2L));
    }

    @Test
    void deleteSingle_FK순서대로_삭제() {
        commentDeleter.deleteSingle(10L);

        InOrder order = inOrder(commentLikeRepository, commentReportRepository, commentRepository);
        order.verify(commentLikeRepository).deleteByCommentId(10L);
        order.verify(commentReportRepository).deleteByCommentId(10L);
        order.verify(commentRepository).softDeleteById(10L);
    }
}
