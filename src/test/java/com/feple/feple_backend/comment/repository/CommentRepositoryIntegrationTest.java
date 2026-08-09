package com.feple.feple_backend.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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

    // ── 금칙어 스캔 ──────────────────────────────────────────────────

    @Test
    void 금칙어_포함_댓글_카운트() {
        commentRepository.save(new Comment("이 댓글은 금칙어포함", post, user, null, null, false));
        commentRepository.save(new Comment("정상 댓글", post, user, null, null, false));
        em.flush();

        long count = commentRepository.countByContentContaining("금칙어");
        assertThat(count).isEqualTo(1);
    }
}
