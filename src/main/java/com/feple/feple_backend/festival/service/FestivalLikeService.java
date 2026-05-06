package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

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
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 페스티벌입니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 사용자입니다."));

        return festivalLikeRepository.findByUserIdAndFestivalId(userId, festivalId)
                .map(like -> {
                    festivalLikeRepository.delete(like);
                    festivalRepository.decrementLikeCount(festivalId);
                    return false;
                })
                .orElseGet(() -> {
                    festivalLikeRepository.save(FestivalLike.of(user, festival));
                    festivalRepository.incrementLikeCount(festivalId);
                    return true;
                });
    }
}
