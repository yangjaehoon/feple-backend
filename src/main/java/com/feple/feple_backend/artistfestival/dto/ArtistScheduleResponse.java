package com.feple.feple_backend.artistfestival.dto;

import com.feple.feple_backend.festival.entity.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ArtistScheduleResponse {
    private Long festivalId;
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private EventType eventType;
    private List<CoArtistInfo> coArtists;

    @Getter
    @Builder
    public static class CoArtistInfo {
        private Long artistId;
        private String artistName;
        private String profileImageUrl;
    }
}
