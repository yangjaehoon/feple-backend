package com.feple.feple_backend.user.service;

import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.user.repository.UserPointLogRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserPointLogRepository pointLogRepository;

    @InjectMocks PointService pointService;

    @Test
    void 게시글_작성_시_원자적_UPDATE로_포인트_적립() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        pointService.onPostCreated(new PostCreatedEvent(1L, 10L));

        verify(userRepository).addPointAtomically(1L, 5);
        ArgumentCaptor<com.feple.feple_backend.user.entity.UserPointLog> captor =
                ArgumentCaptor.forClass(com.feple.feple_backend.user.entity.UserPointLog.class);
        verify(pointLogRepository).save(captor.capture());
    }

    @Test
    void 존재하지_않는_유저는_포인트_적립_스킵() {
        given(userRepository.existsById(999L)).willReturn(false);

        pointService.onPostCreated(new PostCreatedEvent(999L, 10L));

        verify(userRepository, never()).addPointAtomically(eq(999L), org.mockito.ArgumentMatchers.anyInt());
        verify(pointLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 댓글_작성자_없으면_포인트_적립_스킵() {
        pointService.onCommentCreated(new CommentCreatedEvent(1L, "닉네임", "제목", 10L, null, null));

        verify(userRepository, never()).existsById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void 게시글_좋아요_수신_시_포인트_적립() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        pointService.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 10L, 2L));

        verify(userRepository).addPointAtomically(1L, 1);
    }

    @Test
    void 관리자_삭제_시_포인트_차감() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        pointService.onPostDeletedByAdmin(new PostDeletedByAdminEvent(1L, "제목"));

        verify(userRepository).addPointAtomically(1L, -5);
    }

    @Test
    void 인증_승인_시_포인트_적립() {
        given(userRepository.existsById(1L)).willReturn(true);
        given(userRepository.getReferenceById(1L)).willReturn(user(1L));

        pointService.addCertApprovedPoint(1L, 5L);

        verify(userRepository).addPointAtomically(1L, 10);
    }
}
