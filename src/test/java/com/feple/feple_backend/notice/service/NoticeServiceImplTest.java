package com.feple.feple_backend.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import com.feple.feple_backend.notice.dto.NoticeSummaryDto;
import com.feple.feple_backend.notice.entity.Notice;
import com.feple.feple_backend.notice.repository.NoticeRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock NoticeRepository noticeRepository;

    @InjectMocks NoticeServiceImpl noticeService;

    private Notice notice(String title, String content, boolean pinned) {
        return Notice.builder().title(title).content(content).pinned(pinned).build();
    }

    private NoticeRequestDto requestDto(String title, String content, boolean pinned) {
        return NoticeRequestDto.builder().title(title).content(content).pinned(pinned).build();
    }

    @Test
    void 공지_목록은_요약_DTO로_매핑해_반환한다() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notice> page = new PageImpl<>(List.of(notice("점검 안내", "본문", true)));
        given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable)).willReturn(page);

        Page<NoticeSummaryDto> result = noticeService.getNotices(pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("점검 안내");
        assertThat(result.getContent().get(0).isPinned()).isTrue();
    }

    @Test
    void 관리자_목록은_공개_목록과_동일한_조회_로직을_사용한다() {
        Pageable pageable = PageRequest.of(1, 10);
        given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of()));

        noticeService.getAdminNotices(pageable);

        then(noticeRepository).should().findAllByOrderByPinnedDescCreatedAtDesc(pageable);
    }

    @Test
    void 공지_단건_조회_존재하면_상세_DTO를_반환한다() {
        given(noticeRepository.findById(1L)).willReturn(Optional.of(notice("제목", "내용", false)));

        NoticeResponseDto result = noticeService.getNotice(1L);

        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getContent()).isEqualTo("내용");
    }

    @Test
    void 공지_단건_조회_없으면_NoSuchElementException() {
        given(noticeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNotice(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("공지사항");
    }

    @Test
    void 수정_폼_조회도_상세_조회와_동일하게_동작한다() {
        given(noticeRepository.findById(5L)).willReturn(Optional.of(notice("t", "c", true)));

        NoticeResponseDto result = noticeService.getNoticeForEdit(5L);

        assertThat(result.getContent()).isEqualTo("c");
    }

    @Test
    void 공지_생성시_요청값으로_엔티티를_저장하고_id를_반환한다() {
        Notice saved = notice("새 공지", "새 내용", true);
        given(noticeRepository.save(any(Notice.class))).willReturn(saved);

        noticeService.createNotice(requestDto("새 공지", "새 내용", true));

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        then(noticeRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("새 공지");
        assertThat(captor.getValue().isPinned()).isTrue();
    }

    @Test
    void 공지_수정시_기존_엔티티의_필드를_갱신한다() {
        Notice existing = notice("옛 제목", "옛 내용", false);
        given(noticeRepository.findById(3L)).willReturn(Optional.of(existing));

        noticeService.updateNotice(3L, requestDto("새 제목", "새 내용", true));

        assertThat(existing.getTitle()).isEqualTo("새 제목");
        assertThat(existing.getContent()).isEqualTo("새 내용");
        assertThat(existing.isPinned()).isTrue();
    }

    @Test
    void 공지_삭제시_조회한_엔티티를_삭제한다() {
        Notice existing = notice("삭제 대상", "내용", false);
        given(noticeRepository.findById(7L)).willReturn(Optional.of(existing));

        noticeService.deleteNotice(7L);

        then(noticeRepository).should().delete(existing);
    }

    @Test
    void 고정_토글시_pinned_값이_반전된다() {
        Notice existing = notice("제목", "내용", false);
        given(noticeRepository.findById(2L)).willReturn(Optional.of(existing));

        noticeService.togglePin(2L);

        assertThat(existing.isPinned()).isTrue();
    }
}
