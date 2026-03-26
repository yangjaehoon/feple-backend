package com.feple.feple_backend.timetable.repository;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByFestivalIdOrderByFestivalDateAscStartTimeAsc(Long festivalId);
}
