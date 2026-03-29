package com.feple.feple_backend.timetable.dto;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimetableEntryResponse {
    private Long id;
    private String stageName;
    private int stageOrder;
    private String artistName;
    private String festivalDate;
    private String startTime;
    private String endTime;

    public static TimetableEntryResponse from(TimetableEntry e, int stageOrder) {
        return TimetableEntryResponse.builder()
                .id(e.getId())
                .stageName(e.getStageName())
                .stageOrder(stageOrder)
                .artistName(e.getArtistName())
                .festivalDate(e.getFestivalDate().toString())
                .startTime(e.getStartTime().toString())
                .endTime(e.getEndTime().toString())
                .build();
    }
}
