package com.feple.feple_backend.stage.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.global.EntityFinder;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StageService {

    private final StageRepository stageRepository;
    private final FestivalRepository festivalRepository;
    private final TimetableRepository timetableRepository;

    @Transactional(readOnly = true)
    public List<Stage> getStages(Long festivalId) {
        return stageRepository.findByFestivalIdOrderByDisplayOrder(festivalId);
    }

    @Transactional(readOnly = true)
    public Optional<Stage> findByFestivalIdAndName(Long festivalId, String name) {
        return stageRepository.findByFestivalIdAndName(festivalId, name);
    }

    public Stage createStage(Long festivalId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("스테이지 이름을 입력해주세요.");
        }
        if (name.trim().length() > 50)
            throw new IllegalArgumentException("스테이지 이름은 50자 이하여야 합니다.");
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        int nextOrder = stageRepository.findMaxDisplayOrderByFestivalId(festivalId) + 1;
        Stage stage = Stage.builder()
                .festival(festival)
                .name(name.trim())
                .displayOrder(nextOrder)
                .build();
        return stageRepository.save(stage);
    }

    public void deleteStage(Long stageId) {
        timetableRepository.nullifyStageId(stageId);
        stageRepository.deleteById(stageId);
    }

    /** 위로 이동: 바로 앞 스테이지와 순서를 교환 */
    public void moveUp(Long festivalId, Long stageId) {
        Stage current = EntityFinder.getOrThrow(stageRepository::findById, stageId, "스테이지");
        stageRepository
                .findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(
                        festivalId, current.getDisplayOrder())
                .ifPresent(prev -> prev.swapDisplayOrder(current));
    }

    /** 아래로 이동: 바로 뒤 스테이지와 순서를 교환 */
    public void moveDown(Long festivalId, Long stageId) {
        Stage current = EntityFinder.getOrThrow(stageRepository::findById, stageId, "스테이지");
        stageRepository
                .findFirstByFestivalIdAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(
                        festivalId, current.getDisplayOrder())
                .ifPresent(next -> next.swapDisplayOrder(current));
    }
}
