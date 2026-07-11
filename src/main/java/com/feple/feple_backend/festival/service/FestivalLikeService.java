package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
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
        if (userId == null) return false;
        return festivalLikeRepository.existsByUserIdAndFestivalId(userId, festivalId);
    }

    @Transactional
    public boolean toggleLike(Long festivalId, Long userId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = festivalLikeRepository.deleteByUserIdAndFestivalId(userId, festivalId);
        if (deleted > 0) {
            festivalRepository.decrementLikeCount(festivalId);
            return false;
        }
        festivalLikeRepository.save(FestivalLike.of(user, festival));
        festivalRepository.incrementLikeCount(festivalId);
        return true;
    }

    /** 회원 탈퇴 시 해당 유저의 페스티벌 좋아요 데이터 일괄 제거 */
    @Transactional
    public void removeAllByUser(Long userId) {
        festivalLikeRepository.decrementFestivalLikeCountByUserId(userId);
        festivalLikeRepository.deleteByUserId(userId);
    }
}
