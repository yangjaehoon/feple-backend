package com.feple.feple_backend.festival;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalDataMigration implements ApplicationRunner {

    private final FestivalRepository festivalRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Festival> festivals = festivalRepository.findAll();

        int migrated = 0;
        for (Festival festival : festivals) {
            boolean changed = false;

            if (festival.getRegion() == null) {
                festival.setRegion(Region.ETC);
                changed = true;
            }

            if (festival.getGenres().isEmpty()) {
                festival.getGenres().add(Genre.ETC);
                changed = true;
            }

            if (changed) migrated++;
        }

        if (migrated > 0) {
            log.info("Festival migration: {}개 페스티벌에 기본 장르/지역(ETC) 적용 완료", migrated);
        }
    }
}
