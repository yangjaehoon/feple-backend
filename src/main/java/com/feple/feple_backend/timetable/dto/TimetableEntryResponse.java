package com.feple.feple_backend.timetable.dto;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimetableEntryResponse {
    private Long id;
    private String stageName;
    private String artistName;
    private String festivalDate;
    private String startTime;
    private String endTime;

    public static TimetableEntryResponse from(TimetableEntry e) {
        return TimetableEntryResponse.builder()
                .id(e.getId())
                .stageName(e.getStageName())
                .artistName(e.getArtistName())
                .festivalDate(e.getFestivalDate().toString())
                .startTime(e.getStartTime().toString())
                .endTime(e.getEndTime().toString())
                .build();
    }
}
