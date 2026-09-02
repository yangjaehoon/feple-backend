package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * OCR 일괄 등록(60~100건)에서 항목 하나를 독립 트랜잭션으로 저장한다.
 * TimetableEntry의 ID 생성 전략이 IDENTITY라 save()가 즉시 INSERT를 실행하는데,
 * 이 INSERT가 DB 제약 위반 등으로 실패하면 JPA 스펙상 그 영속성 컨텍스트는
 * 더 이상 신뢰할 수 없는 상태가 된다 — TimetableService.createEntriesBatch()가
 * 항목마다 이 예외를 개별 try/catch로 잡아도, 같은 트랜잭션(같은 영속성
 * 컨텍스트)을 계속 쓰면 그 뒤 항목들까지 도미노로 실패할 수 있다.
 * REQUIRES_NEW로 매 항목마다 완전히 새 트랜잭션·영속성 컨텍스트를 열어
 * 한 건의 실패가 다른 건에 전파되지 않도록 격리한다.
 * (같은 클래스 내부 메서드 호출은 AOP 프록시를 우회해 @Transactional이
 * 무시되므로, TimetableService가 아닌 별도 빈으로 분리했다.)
 */
@Component
@RequiredArgsConstructor
class TimetableEntryBatchPersister {

    private final TimetableRepository timetableRepository;
    private final ArtistRepository artistRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TimetableEntry saveIsolated(Festival festival, Stage stage, String stageName, TimetableEntryRequestDto req) {
        String color = (req.getColor() != null && !req.getColor().isBlank()) ? req.getColor().trim() : null;
        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stage(stage)
                .stageName(stage != null ? null : stageName)
                .artistName(req.getArtistName() != null ? req.getArtistName().trim() : "")
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .color(color)
                .build();
        TimetableEntry saved = timetableRepository.save(entry);
        TimetableMemberSync.sync(artistRepository, saved, req.getMemberArtistIds());
        return saved;
    }
}
