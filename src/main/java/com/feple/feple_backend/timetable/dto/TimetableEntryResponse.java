package com.feple.feple_backend.timetable.dto;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static TimetableEntryResponse from(TimetableEntry e) {
        int order = e.getStage() != null ? e.getStage().getDisplayOrder() : Integer.MAX_VALUE;
        return TimetableEntryResponse.builder()
                .id(e.getId())
                .stageName(e.getStageName())
                .stageOrder(order)
                .artistName(e.getArtistName())
                .festivalDate(e.getFestivalDate().toString())
                .startTime(e.getStartTime().format(HH_MM))
                .endTime(e.getEndTime().format(HH_MM))
                .build();
    }
}
