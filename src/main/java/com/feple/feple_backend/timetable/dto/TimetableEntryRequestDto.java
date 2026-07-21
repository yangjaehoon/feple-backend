package com.feple.feple_backend.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TimetableEntryRequestDto {
    @Size(max = 255, message = "무대명은 255자 이내로 입력해주세요.")
    private String stageName;
    @Size(max = 255, message = "아티스트명은 255자 이내로 입력해주세요.")
    private String artistName;
    private List<Long> memberArtistIds;

    @NotNull(message = "페스티벌 날짜는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate festivalDate;

    @NotNull(message = "시작 시간은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "종료 시간은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    @Size(max = 20, message = "색상 값은 20자 이내로 입력해주세요.")
    private String color;
}
