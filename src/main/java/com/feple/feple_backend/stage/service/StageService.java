package com.feple.feple_backend.stage.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class StageService {

    private final StageRepository stageRepository;
    private final FestivalRepository festivalRepository;

    @Transactional(readOnly = true)
    public List<Stage> getStages(Long festivalId) {
        return stageRepository.findByFestivalIdOrderByDisplayOrder(festivalId);
    }

    public Stage createStage(Long festivalId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("스테이지 이름을 입력해주세요.");
        }
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌을 찾을 수 없습니다."));
        int nextOrder = stageRepository.countByFestivalId(festivalId) + 1;
        Stage stage = Stage.builder()
                .festival(festival)
                .name(name.trim())
                .displayOrder(nextOrder)
                .build();
        return stageRepository.save(stage);
    }

    public void deleteStage(Long stageId) {
        stageRepository.deleteById(stageId);
    }

    /** 위로 이동: 바로 앞 스테이지와 순서를 교환 */
    public void moveUp(Long festivalId, Long stageId) {
        Stage current = stageRepository.findById(stageId)
                .orElseThrow(() -> new NoSuchElementException("스테이지를 찾을 수 없습니다."));
        stageRepository
                .findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(
                        festivalId, current.getDisplayOrder())
                .ifPresent(prev -> {
                    int tmp = prev.getDisplayOrder();
                    prev.setDisplayOrder(current.getDisplayOrder());
                    current.setDisplayOrder(tmp);
                });
    }

    /** 아래로 이동: 바로 뒤 스테이지와 순서를 교환 */
    public void moveDown(Long festivalId, Long stageId) {
        Stage current = stageRepository.findById(stageId)
                .orElseThrow(() -> new NoSuchElementException("스테이지를 찾을 수 없습니다."));
        stageRepository
                .findFirstByFestivalIdAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(
                        festivalId, current.getDisplayOrder())
                .ifPresent(next -> {
                    int tmp = next.getDisplayOrder();
                    next.setDisplayOrder(current.getDisplayOrder());
                    current.setDisplayOrder(tmp);
                });
    }
}
