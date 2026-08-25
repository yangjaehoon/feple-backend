package com.feple.feple_backend.artist.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistRankingSchedulerTest {

    @Mock ArtistRepository artistRepository;
    @Mock PostRepository postRepository;
    @Mock ArtistFollowRepository artistFollowRepository;

    @InjectMocks ArtistRankingScheduler scheduler;

    private Artist artist(Long id) {
        return Artist.builder().id(id).name("아티스트" + id).build();
    }

    @Test
    void 주간랭킹_게시글_팔로우_점수_합산() {
        Artist artist1 = artist(1L);
        Artist artist2 = artist(2L);
        given(artistRepository.findAll()).willReturn(List.of(artist1, artist2));

        // artist1: postCount=2, likeSum=3 → 5 / follow=2 → total 7
        given(postRepository.countAndSumByArtistSince(any()))
                .willReturn(List.<Object[]>of(new Object[]{1L, 2L, 3L}));
        given(artistFollowRepository.countByArtistSince(any()))
                .willReturn(List.<Object[]>of(new Object[]{1L, 2L}));

        scheduler.updateWeeklyRanking();

        assertThat(artist1.getWeeklyScore()).isEqualTo(7);
        assertThat(artist2.getWeeklyScore()).isEqualTo(0);
    }

    @Test
    void 주간랭킹_집계데이터_없으면_전부_0점() {
        Artist artist = artist(1L);
        given(artistRepository.findAll()).willReturn(List.of(artist));
        given(postRepository.countAndSumByArtistSince(any())).willReturn(List.of());
        given(artistFollowRepository.countByArtistSince(any())).willReturn(List.of());

        scheduler.updateWeeklyRanking();

        assertThat(artist.getWeeklyScore()).isEqualTo(0);
    }

    @Test
    void 주간랭킹_아티스트_없으면_예외없이_완료() {
        given(artistRepository.findAll()).willReturn(List.of());
        given(postRepository.countAndSumByArtistSince(any())).willReturn(List.of());
        given(artistFollowRepository.countByArtistSince(any())).willReturn(List.of());

        scheduler.updateWeeklyRanking();
        // 예외 없이 완료
    }
}
