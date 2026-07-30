package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.moderation.CsvExporter;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistCsvExporter {

    private final ArtistAdminService artistAdminService;

    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,이름,영어이름,카테고리,팔로워수,곡수\n");
        for (ArtistResponseDto a : artistAdminService.getAllArtistsSortedByName()) {
            sb.append(CsvExporter.row(
                    a.getId(),
                    a.getName(),
                    a.getNameEn(),
                    a.getGenre(),
                    a.getFollowerCount(),
                    a.getSongCount()));
        }
        return sb.toString();
    }
}
