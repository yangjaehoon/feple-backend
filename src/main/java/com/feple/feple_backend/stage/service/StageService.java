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
        return stageRepository.findByFestivalIdOrderByName(festivalId);
    }

    public Stage createStage(Long festivalId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("스테이지 이름을 입력해주세요.");
        }
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌을 찾을 수 없습니다."));
        Stage stage = Stage.builder()
                .festival(festival)
                .name(name.trim())
                .build();
        return stageRepository.save(stage);
    }

    public void deleteStage(Long stageId) {
        stageRepository.deleteById(stageId);
    }
}
