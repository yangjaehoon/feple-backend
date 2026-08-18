package com.feple.feple_backend.notice.service;

import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import com.feple.feple_backend.notice.entity.Notice;
import com.feple.feple_backend.notice.repository.NoticeRepository;
import java.util.NoSuchElementException;
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
    public Page<NoticeResponseDto> getNotices(Pageable pageable) {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable)
                .map(NoticeResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public NoticeResponseDto getNotice(Long id) {
        return NoticeResponseDto.from(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NoticeResponseDto> getAdminNotices(Pageable pageable) {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable)
                .map(NoticeResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public NoticeResponseDto getNoticeForEdit(Long id) {
        return NoticeResponseDto.from(findById(id));
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
        if (!noticeRepository.existsById(id)) {
            throw new NoSuchElementException("존재하지 않는 공지사항입니다.");
        }
        noticeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void togglePin(Long id) {
        findById(id).togglePin();
    }

    private Notice findById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 공지사항입니다."));
    }
}
