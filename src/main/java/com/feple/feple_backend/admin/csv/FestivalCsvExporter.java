package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FestivalCsvExporter {

    private final FestivalAdminService festivalAdminService;

    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,제목,영어제목,지역,장소,시작일,종료일,좋아요,참석의사\n");
        for (FestivalResponseDto f : festivalAdminService.getAllFestivalsForAdmin()) {
            sb.append(CsvExporter.row(
                    f.getId(),
                    f.getTitle(),
                    f.getTitleEn(),
                    f.getRegion() != null ? f.getRegion().getDisplayName() : "",
                    f.getLocation(),
                    f.getStartDateIso(),
                    f.getEndDateIso(),
                    f.getLikeCount(),
                    f.getAttendingCount()));
        }
        return sb.toString();
    }
}
