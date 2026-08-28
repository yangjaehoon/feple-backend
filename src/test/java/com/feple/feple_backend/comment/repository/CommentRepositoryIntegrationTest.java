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
    void 금칙어_포함_댓글_카운트_삭제블라인드_제외() {
        commentRepository.save(new Comment("이 댓글은 금칙어포함", post, user, null, null, false));
        Comment blinded = commentRepository.save(new Comment("금칙어 블라인드", post, user, null, null, false));
        Comment deleted = commentRepository.save(new Comment("금칙어 삭제", post, user, null, null, false));
        blinded.blind();
        em.flush();
        commentRepository.delete(deleted);
        em.flush();
        em.clear();

        // 공개 카운트는 삭제·블라인드 제외
        assertThat(commentRepository.countByContentContaining("금칙어")).isEqualTo(1);
    }

    // ── 가시성: 공개 조회는 삭제·블라인드 제외, 관리자/기본 조회는 포함 ──

    @Test
    void 공개_댓글목록은_삭제블라인드_제외_관리자는_블라인드_포함() {
        Comment normal = commentRepository.save(new Comment("정상", post, user, null, null, false));
        Comment blinded = commentRepository.save(new Comment("블라인드됨", post, user, null, null, false));
        Comment deleted = commentRepository.save(new Comment("삭제됨", post, user, null, null, false));
        blinded.blind();
        em.flush();
        commentRepository.delete(deleted);
        em.flush();
        em.clear();

        List<Comment> publicList = commentRepository
                .findByPostIdOrderByCreatedAtAsc(post.getId(), PageRequest.of(0, 50)).getContent();
        assertThat(publicList).extracting(Comment::getId).containsExactly(normal.getId());

        List<Comment> adminList = commentRepository.findAdminByPostIdOrderByCreatedAtAsc(post.getId(), 50);
        assertThat(adminList).extracting(Comment::getId)
                .containsExactlyInAnyOrder(normal.getId(), blinded.getId()); // 삭제만 제외

        // 기본 findById는 블라인드도 조회됨 (관리자·본인·신고 경로용)
        assertThat(commentRepository.findById(blinded.getId())).isPresent();
    }
}
