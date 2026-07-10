package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostRepositoryIntegrationTest {

    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager em;

    @MockitoBean FileStorageService fileStorageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .oauthId("post-repo-test-user").nickname("테스터").build());
    }

    // ── 핫 게시글 ────────────────────────────────────────────────────

    @Test
    void 핫게시글_좋아요_내림차순_반환() {
        postRepository.save(freePost(5));
        postRepository.save(freePost(20));
        postRepository.save(freePost(10));
        em.flush(); em.clear();

        List<Post> result = postRepository.findHotPosts(
                LocalDateTime.now().minusHours(1), PageRequest.of(0, 10));

        assertThat(result).extracting(Post::getLikeCount)
                .isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void 핫게시글_기준일_이전_게시글_제외() {
        Post recent = postRepository.save(freePost(5));
        Post old = postRepository.save(Post.builder()
                .title("오래된").content("X").user(user).boardType(BoardType.FREE)
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build());
        em.flush(); em.clear();

        List<Post> result = postRepository.findHotPosts(
                LocalDateTime.now().minusHours(1), PageRequest.of(0, 10));

        List<Long> ids = result.stream().map(Post::getId).toList();
        assertThat(ids).contains(recent.getId());
        assertThat(ids).doesNotContain(old.getId());
    }

    // ── 커서 기반 페이지네이션 ────────────────────────────────────────

    @Test
    void 커서_기반_페이지네이션_ID_범위_필터링() {
        Post p1 = postRepository.save(freePost(0));
        Post p2 = postRepository.save(freePost(0));
        Post p3 = postRepository.save(freePost(0));
        em.flush(); em.clear();

        // p3 이전(id < p3.id)의 포스트만 내림차순
        List<Post> result = postRepository.findByBoardTypeAndIdLessThanOrderByIdDesc(
                BoardType.FREE, p3.getId(), PageRequest.of(0, 10));

        assertThat(result).extracting(Post::getId)
                .containsExactly(p2.getId(), p1.getId())
                .doesNotContain(p3.getId());
    }

    // ── likeCount 엔티티 메서드 ─────────────────────────────────────

    @Test
    void 좋아요_증가_엔티티_메서드() {
        Post post = postRepository.save(freePost(3));
        em.flush();

        post.incrementLikeCount();
        em.flush(); em.clear();

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(4);
    }

    @Test
    void 좋아요_감소_엔티티_메서드() {
        Post post = postRepository.save(freePost(3));
        em.flush();

        post.decrementLikeCount();
        em.flush(); em.clear();

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(2);
    }

    @Test
    void 좋아요_0일때_감소_무시() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        post.decrementLikeCount(); // Math.max(0, ...) 보호
        em.flush(); em.clear();

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(0);
    }

    // ── scrapCount 엔티티 메서드 ─────────────────────────────────────

    @Test
    void 스크랩_증가_감소_엔티티_메서드() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        post.incrementScrapCount();
        em.flush(); em.clear();
        Post after = postRepository.findById(post.getId()).orElseThrow();
        assertThat(after.getScrapCount()).isEqualTo(1);

        after.decrementScrapCount();
        em.flush(); em.clear();
        Post afterDec = postRepository.findById(post.getId()).orElseThrow();
        assertThat(afterDec.getScrapCount()).isEqualTo(0);
    }

    // ── 소프트 삭제 (native query) ────────────────────────────────────

    @Test
    void 소프트삭제_후_일반_조회_불가() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        postRepository.softDeleteByIds(List.of(post.getId())); // clearAutomatically=true

        Optional<Post> found = postRepository.findById(post.getId());
        assertThat(found).isEmpty(); // @SQLRestriction("deleted_at IS NULL")으로 제외됨
    }

    @Test
    void 소프트삭제된_게시글_관리자_native쿼리_조회() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        postRepository.softDeleteByIds(List.of(post.getId()));

        List<Post> softDeleted = postRepository.findSoftDeleted(10);
        assertThat(softDeleted).extracting(Post::getId).contains(post.getId());
    }

    @Test
    void 소프트삭제_복구_후_정상_조회() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        postRepository.softDeleteByIds(List.of(post.getId()));
        assertThat(postRepository.findById(post.getId())).isEmpty();

        postRepository.restore(post.getId()); // clearAutomatically=true

        Optional<Post> restored = postRepository.findById(post.getId());
        assertThat(restored).isPresent();
    }

    // ── 집계 쿼리 ────────────────────────────────────────────────────

    @Test
    void 유저별_게시글수_집계() {
        postRepository.save(freePost(0));
        postRepository.save(freePost(0));
        em.flush();

        List<Object[]> counts = postRepository.countGroupByUserId(List.of(user.getId()));

        assertThat(counts).hasSize(1);
        assertThat(((Number) counts.get(0)[1]).longValue()).isEqualTo(2);
    }

    @Test
    void 금칙어_포함_게시글_카운트() {
        postRepository.save(Post.builder()
                .title("욕설포함제목").content("정상").user(user)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        em.flush();

        long count = postRepository.countByTitleOrContentContaining("욕설");
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private Post freePost(int likeCount) {
        return Post.builder()
                .title("제목").content("내용").user(user)
                .boardType(BoardType.FREE)
                .likeCount(likeCount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
