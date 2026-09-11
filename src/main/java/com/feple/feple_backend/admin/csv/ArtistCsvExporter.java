package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.artist.service.ArtistAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistCsvExporter {

    private final ArtistAdminService artistAdminService;

    public String buildCsv() {
        return CsvExporter.buildCsv("ID,이름,영어이름,카테고리,팔로워수,곡수\n",
                artistAdminService.getArtistsForExport(),
                a -> new Object[]{ a.getId(), a.getName(), a.getNameEn(), a.getGenre(), a.getFollowerCount(), a.getSongCount() });
    }
}
