package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
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
public class FestivalLikeService {

    private final FestivalLikeRepository festivalLikeRepository;
    private final FestivalRepository festivalRepository;
    private final UserRepository userRepository;

    public boolean isLiked(Long festivalId, Long userId) {
        return festivalLikeRepository.existsByUserIdAndFestivalId(userId, festivalId);
    }

    @Transactional
    public boolean toggleLike(Long festivalId, Long userId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = festivalLikeRepository.deleteByUserIdAndFestivalId(userId, festivalId);
        if (deleted > 0) {
            festivalRepository.decrementLikeCount(festivalId);
            return false;
        }
        festivalLikeRepository.save(FestivalLike.of(user, festival));
        festivalRepository.incrementLikeCount(festivalId);
        return true;
    }
}
