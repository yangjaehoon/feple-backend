package com.feple.feple_backend.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class TimetableEntryRequest {
    @NotBlank(message = "스테이지 이름은 필수입니다.")
    private String stageName;
    @NotBlank(message = "아티스트 이름은 필수입니다.")
    private String artistName;

    @NotNull(message = "페스티벌 날짜는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate festivalDate;

    @NotNull(message = "시작 시간은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotNull(message = "종료 시간은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;
}
