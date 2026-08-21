package com.feple.feple_backend.ticketlink.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.ticketlink.dto.TicketLinkRequestDto;
import com.feple.feple_backend.ticketlink.dto.TicketLinkResponseDto;
import com.feple.feple_backend.ticketlink.entity.FestivalTicketLink;
import com.feple.feple_backend.ticketlink.repository.FestivalTicketLinkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalTicketLinkService {

    private final FestivalTicketLinkRepository ticketLinkRepository;
    private final FestivalRepository festivalRepository;

    @Transactional(readOnly = true)
    public List<TicketLinkResponseDto> getTicketLinks(Long festivalId) {
        return ticketLinkRepository.findByFestivalId(festivalId).stream()
                .map(TicketLinkResponseDto::from)
                .toList();
    }

    @Transactional
    public Long createTicketLink(Long festivalId, TicketLinkRequestDto dto) {
        // 삭제된(휴지통) 페스티벌에는 새 링크를 만들 수 없다 — 다른 관리자가 방금
        // 삭제한 페스티벌의 등록 폼이 아직 열려 있는 경우를 막는다
        Festival festival = EntityLoader.getOrThrow(
                festivalRepository::findByIdAndDeletedAtIsNull, festivalId, "페스티벌");
        FestivalTicketLink link = FestivalTicketLink.builder()
                .festival(festival)
                .label(dto.getLabel())
                .url(dto.getUrl())
                .build();
        return ticketLinkRepository.save(link).getId();
    }

    @Transactional
    public void deleteTicketLink(Long festivalId, Long ticketLinkId) {
        FestivalTicketLink link = EntityLoader.getOrThrow(ticketLinkRepository::findById, ticketLinkId, "티켓 링크");
        EntityLoader.requireBelongsToFestival(festivalId, link.getFestivalId(), "티켓 링크가");
        ticketLinkRepository.delete(link);
    }
}
