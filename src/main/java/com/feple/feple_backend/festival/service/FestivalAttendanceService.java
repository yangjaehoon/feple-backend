package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalAttendance;
import com.feple.feple_backend.festival.repository.FestivalAttendanceRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalAttendanceService {

    private final FestivalAttendanceRepository attendanceRepository;
    private final FestivalRepository festivalRepository;
    private final UserRepository userRepository;

    public boolean isAttending(Long festivalId, Long userId) {
        return attendanceRepository.existsByUserIdAndFestivalId(userId, festivalId);
    }

    @Transactional
    public boolean toggleAttending(Long festivalId, Long userId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = attendanceRepository.deleteByUserIdAndFestivalId(userId, festivalId);
        if (deleted > 0) {
            festival.decrementAttendingCount();
            return false;
        }
        attendanceRepository.save(FestivalAttendance.of(user, festival));
        festival.incrementAttendingCount();
        return true;
    }
}
