package com.feple.feple_backend.timetable.dto;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.entity.TimetableEntryMember;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private String color;
    private List<String> memberArtistNames;
    private List<Long> memberArtistIds;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static TimetableEntryResponse from(TimetableEntry e) {
        int order = e.getStage() != null ? e.getStage().getDisplayOrder() : Integer.MAX_VALUE;
        List<TimetableEntryMember> members = e.getMembers();
        return TimetableEntryResponse.builder()
                .id(e.getId())
                .stageName(e.getStageName())
                .stageOrder(order)
                .artistName(e.getArtistName())
                .festivalDate(e.getFestivalDate().toString())
                .startTime(e.getStartTime().format(HH_MM))
                .endTime(e.getEndTime().format(HH_MM))
                .color(e.getColor())
                .memberArtistNames(members.stream().map(TimetableEntryMember::getArtistName).toList())
                .memberArtistIds(members.stream().map(TimetableEntryMember::getArtistId).filter(Objects::nonNull).toList())
                .build();
    }

    public String getMemberArtistIdsStr() {
        if (memberArtistIds == null || memberArtistIds.isEmpty()) return "";
        return memberArtistIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
