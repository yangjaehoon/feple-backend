package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.LikeToggler;
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

        // 추가 경로에서 매칭 없는 DELETE의 갭 락을 잡지 않도록 존재 조회로 분기한다 —
        // 같은 유저가 여러 페스티벌을 동시에 좋아요할 때(온보딩) 빈 인덱스 구간에
        // 겹쳐 잡히는 갭 락 데드락을 피하기 위함.
        return LikeToggler.toggleByExistence(
                () -> festivalLikeRepository.existsByUserIdAndFestivalId(userId, festivalId),
                () -> {
                    if (festivalLikeRepository.deleteByUserIdAndFestivalId(userId, festivalId) > 0) {
                        festivalRepository.decrementLikeCount(festivalId);
                    }
                },
                () -> {
                    festivalLikeRepository.saveAndFlush(FestivalLike.of(user, festival));
                    festivalRepository.incrementLikeCount(festivalId);
                });
    }

    /** 회원 탈퇴 시 해당 유저의 페스티벌 좋아요 데이터 일괄 제거 */
    @Transactional
    public void removeAllByUser(Long userId) {
        festivalLikeRepository.decrementFestivalLikeCountByUserId(userId);
        festivalLikeRepository.deleteByUserId(userId);
    }

    /** 페스티벌 삭제 시 좋아요 데이터 일괄 제거 — festival 자체가 삭제되므로 카운터 감소 불필요 */
    @Transactional
    public void removeAllByFestival(Long festivalId) {
        festivalLikeRepository.deleteByFestivalId(festivalId);
    }
}
