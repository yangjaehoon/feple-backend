package com.feple.feple_backend.artist.scheduler;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArtistRankingScheduler {

    private final ArtistRepository artistRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ArtistFollowRepository artistFollowRepository;

    /**
     * 매주 월요일 자정에 아티스트 주간 랭킹 점수를 갱신한다.
     * 점수 = 지난 7일간 아티스트 게시판에 달린 좋아요 수 + 게시글 수 + 댓글 수 + 팔로우 수
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void updateWeeklyRanking() {
        log.info("[ArtistRankingScheduler] 주간 랭킹 갱신 시작");
        LocalDateTime since = LocalDateTime.now().minusWeeks(1);
        List<Artist> artists = artistRepository.findAll();

        for (Artist artist : artists) {
            long postLikes = postRepository.sumLikeCountByArtistAndSince(artist.getId(), since);
            long postCount = postRepository.countByArtistAndSince(artist.getId(), since);
            long commentCount = commentRepository.countByArtistAndSince(artist.getId(), since);
            long followCount = artistFollowRepository.countByArtistIdAndCreatedAtAfter(artist.getId(), since);
            int score = (int) (postLikes + postCount + commentCount + followCount);
            artist.updateWeeklyScore(score);
        }

        log.info("[ArtistRankingScheduler] 주간 랭킹 갱신 완료 ({}명)", artists.size());
    }
}
