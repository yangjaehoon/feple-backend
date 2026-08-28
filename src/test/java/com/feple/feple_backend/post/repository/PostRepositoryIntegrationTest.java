package com.feple.feple_backend.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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

        List<Post> result = postRepository.findPopularPosts(
                LocalDateTime.now().minusHours(1), PageRequest.of(0, 10));

        assertThat(result).extracting(Post::getLikeCount)
                .isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void 핫게시글_기준일_이전_게시글_제외() {
        Post recent = postRepository.save(freePost(5));
        Post old = postRepository.save(Post.builder()
                .title("오래된").content("X").user(user).boardType(BoardType.FREE)
                .build());
        em.flush();
        // created_at은 @CreationTimestamp가 채우므로, 과거로 만들려면 저장 후 직접 되돌린다
        em.createNativeQuery("UPDATE post SET created_at = :ts WHERE id = :id")
                .setParameter("ts", LocalDateTime.now().minusDays(3))
                .setParameter("id", old.getId())
                .executeUpdate();
        em.clear();

        List<Post> result = postRepository.findPopularPosts(
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
        List<Post> result = postRepository.findByBoardTypeAndPinnedFalseAndIdLessThanOrderByIdDesc(
                BoardType.FREE, p3.getId(), PageRequest.of(0, 10));

        assertThat(result).extracting(Post::getId)
                .containsExactly(p2.getId(), p1.getId())
                .doesNotContain(p3.getId());
    }

    // ── 소프트 삭제 / 블라인드 가시성 ─────────────────────────────────

    @Test
    void 소프트삭제_후_공개조회_제외_기본findById는_포함() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        postRepository.softDeleteByIds(List.of(post.getId()));
        em.clear();

        // 공개 조회는 명시 필터로 제외
        assertThat(postRepository.findWithAssociationsById(post.getId())).isEmpty();
        // 관리자·본인·신고 경로용 기본 findById는 삭제된 행도 조회
        assertThat(postRepository.findById(post.getId())).isPresent();
    }

    @Test
    void 블라인드_후_공개조회_제외_기본findById는_포함() {
        Post post = postRepository.save(freePost(0));
        post.blind();
        em.flush();
        em.clear();

        assertThat(postRepository.findWithAssociationsById(post.getId())).isEmpty();
        assertThat(postRepository.findByBoardTypeAndPinnedFalseOrderByIdDesc(
                BoardType.FREE, PageRequest.of(0, 10))).isEmpty();
        assertThat(postRepository.findById(post.getId())).isPresent();
    }

    @Test
    void 조회수_증가는_삭제블라인드된_게시글에_0행_반환() {
        Post visible = postRepository.save(freePost(0));
        Post blinded = postRepository.save(freePost(0));
        blinded.blind();
        em.flush();
        Post deleted = postRepository.save(freePost(0));
        em.flush();
        postRepository.softDeleteByIds(List.of(deleted.getId()));
        em.clear();

        assertThat(postRepository.incrementViewCount(visible.getId())).isEqualTo(1);
        assertThat(postRepository.incrementViewCount(blinded.getId())).isZero();
        assertThat(postRepository.incrementViewCount(deleted.getId())).isZero();
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
    void 소프트삭제_복구_후_공개조회_정상() {
        Post post = postRepository.save(freePost(0));
        em.flush();

        postRepository.softDeleteByIds(List.of(post.getId()));
        em.clear();
        assertThat(postRepository.findWithAssociationsById(post.getId())).isEmpty();

        postRepository.restore(post.getId());
        em.clear();
        assertThat(postRepository.findWithAssociationsById(post.getId())).isPresent();
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
