package com.feple.feple_backend.notice.service;

import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeAdminService {
    Page<NoticeResponseDto> getAdminNotices(Pageable pageable);
    NoticeResponseDto getNoticeForEdit(Long id);
    Long createNotice(NoticeRequestDto dto);
    void updateNotice(Long id, NoticeRequestDto dto);
    void deleteNotice(Long id);
    void togglePin(Long id);
}
