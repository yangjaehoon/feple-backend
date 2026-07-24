package com.feple.feple_backend.artistfollow.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate 6 파생 쿼리 PathElementException 우회용 @Query 메서드들이
 * 실제 DB 에서 올바르게 동작하는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArtistFollowRepositoryIntegrationTest {

    @Autowired ArtistFollowRepository artistFollowRepository;
    @Autowired UserRepository userRepository;
    @Autowired ArtistRepository artistRepository;
    @PersistenceContext EntityManager em;

    @MockitoBean FileStorageService fileStorageService;

    private User user;
    private Artist artist;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .oauthId("follow-test-user").nickname("팔로워").build());
        artist = artistRepository.save(Artist.builder()
                .name("테스트아티스트").build());
    }

    // ── existsByUserIdAndArtistId (@Query — Hibernate 6 우회) ────────

    @Test
    void 팔로우_존재_여부_참() {
        artistFollowRepository.save(ArtistFollow.of(user, artist));
        em.flush(); em.clear();

        assertThat(artistFollowRepository.existsByUserIdAndArtistId(user.getId(), artist.getId()))
                .isTrue();
    }

    @Test
    void 팔로우_존재_여부_거짓() {
        em.flush(); em.clear();

        assertThat(artistFollowRepository.existsByUserIdAndArtistId(user.getId(), artist.getId()))
                .isFalse();
    }

    @Test
    void 다른_아티스트_팔로우_여부는_거짓() {
        Artist other = artistRepository.save(Artist.builder().name("다른아티스트").build());
        artistFollowRepository.save(ArtistFollow.of(user, artist)); // artist 팔로우
        em.flush(); em.clear();

        // other 아티스트 팔로우 여부 → false
        assertThat(artistFollowRepository.existsByUserIdAndArtistId(user.getId(), other.getId()))
                .isFalse();
    }

    // ── deleteByUserIdAndArtistId (@Modifying) ────────────────────────

    @Test
    void 팔로우_삭제() {
        artistFollowRepository.save(ArtistFollow.of(user, artist));
        em.flush();

        artistFollowRepository.deleteByUserIdAndArtistId(user.getId(), artist.getId());
        em.clear();

        assertThat(artistFollowRepository.existsByUserIdAndArtistId(user.getId(), artist.getId()))
                .isFalse();
    }

    @Test
    void 없는_팔로우_삭제_시_예외_없음() {
        // 존재하지 않는 팔로우 삭제 → 예외 없이 0건 삭제
        artistFollowRepository.deleteByUserIdAndArtistId(user.getId(), 9999L);
    }

    // ── findByUserId (JOIN FETCH) ─────────────────────────────────────

    @Test
    void userId로_팔로우_목록_JOIN_FETCH() {
        Artist artist2 = artistRepository.save(Artist.builder().name("두번째아티스트").build());
        artistFollowRepository.save(ArtistFollow.of(user, artist));
        artistFollowRepository.save(ArtistFollow.of(user, artist2));
        em.flush(); em.clear();

        List<ArtistFollow> follows = artistFollowRepository.findByUserId(user.getId());

        assertThat(follows).hasSize(2);
        // JOIN FETCH로 artist가 이미 로딩됨 — 추가 쿼리 없이 접근 가능
        assertThat(follows).extracting(af -> af.getArtist().getName())
                .containsExactlyInAnyOrder("테스트아티스트", "두번째아티스트");
    }

    @Test
    void 다른_유저의_팔로우는_포함_안됨() {
        User other = userRepository.save(User.builder().oauthId("other").nickname("타인").build());
        artistFollowRepository.save(ArtistFollow.of(user, artist));
        artistFollowRepository.save(ArtistFollow.of(other, artist));
        em.flush(); em.clear();

        List<ArtistFollow> follows = artistFollowRepository.findByUserId(user.getId());

        assertThat(follows).hasSize(1);
        assertThat(follows.get(0).getUserId()).isEqualTo(user.getId());
    }
}
