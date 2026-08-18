package com.feple.feple_backend.notice.service;

import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeService {
    Page<NoticeResponseDto> getNotices(Pageable pageable);
    NoticeResponseDto getNotice(Long id);
}
