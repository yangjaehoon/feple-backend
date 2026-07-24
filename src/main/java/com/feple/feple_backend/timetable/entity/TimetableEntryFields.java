package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.stage.entity.Stage;

import java.time.LocalDate;
import java.time.LocalTime;

/** TimetableEntry.update()에 넘기는 수정 가능 필드 묶음 — 7개 개별 인수 대신 사용 */
public record TimetableEntryFields(
        String artistName,
        String stageName,
        Stage stage,
        LocalDate festivalDate,
        LocalTime startTime,
        LocalTime endTime,
        String color
) {}
