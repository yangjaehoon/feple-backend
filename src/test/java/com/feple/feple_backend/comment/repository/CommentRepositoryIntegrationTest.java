package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommentRepositoryIntegrationTest {

    @Autowired CommentRepository commentRepository;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager em;

    @MockitoBean FileStorageService fileStorageService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .oauthId("comment-repo-test-user").nickname("댓글테스터").build());
        post = postRepository.save(Post.builder()
                .title("테스트게시글").content("내용").user(user)
                .boardType(BoardType.FREE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        em.flush();
    }

    // ── 조회 쿼리 ────────────────────────────────────────────────────

    @Test
    void 댓글_생성순_정렬() {
        commentRepository.save(new Comment("첫번째", post, user, false));
        commentRepository.save(new Comment("두번째", post, user, false));
        commentRepository.save(new Comment("세번째", post, user, false));
        em.flush(); em.clear();

        List<Comment> result = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Comment::getContent)
                .containsExactly("첫번째", "두번째", "세번째");
    }

    @Test
    void 다른_게시글_댓글은_조회_안됨() {
        Post otherPost = postRepository.save(Post.builder()
                .title("다른게시글").content("다른내용").user(user)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        commentRepository.save(new Comment("이 게시글 댓글", post, user, false));
        commentRepository.save(new Comment("다른 게시글 댓글", otherPost, user, false));
        em.flush(); em.clear();

        List<Comment> result = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("이 게시글 댓글");
    }

    @Test
    void 게시글에_댓글_없으면_빈_목록() {
        List<Comment> result = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void 유저별_댓글수_카운트() {
        commentRepository.save(new Comment("댓글1", post, user, false));
        commentRepository.save(new Comment("댓글2", post, user, false));
        em.flush();

        long count = commentRepository.countByUser(user);
        assertThat(count).isEqualTo(2);
    }

    // ── 금칙어 스캔 ──────────────────────────────────────────────────

    @Test
    void 금칙어_포함_댓글_카운트() {
        commentRepository.save(new Comment("이 댓글은 금칙어포함", post, user, false));
        commentRepository.save(new Comment("정상 댓글", post, user, false));
        em.flush();

        long count = commentRepository.countByContentContaining("금칙어");
        assertThat(count).isEqualTo(1);
    }
}
