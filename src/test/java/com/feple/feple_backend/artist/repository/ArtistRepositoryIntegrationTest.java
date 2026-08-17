package com.feple.feple_backend.artist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.MusicGenre;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 아티스트 목록의 장르 필터가 모든 정렬 옵션과 함께 동작하는지 검증한다.
 * 네이티브 쿼리였을 때는 Sort 속성명(followerCount/weeklyScore)이 컬럼명으로 그대로
 * 붙어 "Unknown column" 500 에러가 났었다 — mock 기반 단위 테스트로는 잡히지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ArtistRepositoryIntegrationTest {

    @Autowired ArtistRepository artistRepository;
    @PersistenceContext EntityManager em;

    @MockitoBean FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        artistRepository.save(Artist.builder()
                .name("힙합가수A").followerCount(10).weeklyScore(5)
                .genres(List.of(MusicGenre.HIP_HOP)).build());
        artistRepository.save(Artist.builder()
                .name("힙합가수B").followerCount(30).weeklyScore(1)
                .genres(List.of(MusicGenre.HIP_HOP)).build());
        artistRepository.save(Artist.builder()
                .name("아이돌가수").followerCount(99).weeklyScore(99)
                .genres(List.of(MusicGenre.IDOL)).build());
        em.flush();
        em.clear();
    }

    @Test
    void 장르필터는_해당_장르만_반환한다() {
        Page<Artist> page = findHipHop(Sort.by(Direction.ASC, "name"));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Artist::getName)
                .containsExactly("힙합가수A", "힙합가수B");
    }

    @Test
    void 장르필터_팔로워순_정렬() {
        Page<Artist> page = findHipHop(Sort.by(Direction.DESC, "followerCount"));

        assertThat(page.getContent()).extracting(Artist::getName)
                .containsExactly("힙합가수B", "힙합가수A");
    }

    @Test
    void 장르필터_랭킹순_정렬() {
        Page<Artist> page = findHipHop(
                Sort.by(Direction.DESC, "weeklyScore").and(Sort.by(Direction.ASC, "id")));

        assertThat(page.getContent()).extracting(Artist::getName)
                .containsExactly("힙합가수A", "힙합가수B");
    }

    private Page<Artist> findHipHop(Sort sort) {
        return artistRepository.findByGenre(MusicGenre.HIP_HOP, PageRequest.of(0, 20, sort));
    }
}
