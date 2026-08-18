package com.feple.feple_backend.notice.service;

import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import com.feple.feple_backend.notice.dto.NoticeSummaryDto;
import com.feple.feple_backend.notice.entity.Notice;
import com.feple.feple_backend.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService, NoticeAdminService {

    private final NoticeRepository noticeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NoticeSummaryDto> getNotices(Pageable pageable) {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable)
                .map(NoticeSummaryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public NoticeResponseDto getNotice(Long id) {
        return NoticeResponseDto.from(findById(id));
    }

    // 관리자 목록도 공개 목록과 동일한 조회·매핑 로직을 쓴다 — 별도 구현을 두면 나중에
    // 한쪽만 고치고 다른 쪽을 잊어 admin/공개 화면이 조용히 갈라지는 문제가 생긴다.
    @Override
    @Transactional(readOnly = true)
    public Page<NoticeSummaryDto> getAdminNotices(Pageable pageable) {
        return getNotices(pageable);
    }

    // 관리자 수정 폼도 공개 상세와 동일하게 content를 포함한 전체 DTO가 필요하다.
    @Override
    @Transactional(readOnly = true)
    public NoticeResponseDto getNoticeForEdit(Long id) {
        return getNotice(id);
    }

    @Override
    @Transactional
    public Long createNotice(NoticeRequestDto dto) {
        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .pinned(dto.isPinned())
                .build();
        return noticeRepository.save(notice).getId();
    }

    @Override
    @Transactional
    public void updateNotice(Long id, NoticeRequestDto dto) {
        findById(id).update(dto.getTitle(), dto.getContent(), dto.isPinned());
    }

    @Override
    @Transactional
    public void deleteNotice(Long id) {
        noticeRepository.delete(findById(id));
    }

    @Override
    @Transactional
    public void togglePin(Long id) {
        findById(id).togglePin();
    }

    private Notice findById(Long id) {
        return EntityLoader.getOrThrow(noticeRepository::findById, id, "공지사항");
    }
}
