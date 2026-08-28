package com.feple.feple_backend.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostTag;
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
class PostTagRepositoryIntegrationTest {

    private static final String TAG = "아이유";

    @Autowired PostTagRepository postTagRepository;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager em;

    @MockitoBean FileStorageService fileStorageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .oauthId("post-tag-repo-test-user").nickname("테스터").build());
    }

    @Test
    void 태그_조회는_삭제블라인드된_게시글의_태그를_제외한다() {
        Post visible = savePostWithTag();
        Post blinded = savePostWithTag();
        Post deleted = savePostWithTag();

        blinded.blind();
        em.flush();
        postRepository.softDeleteByIds(List.of(deleted.getId()));
        em.clear();

        List<PostTag> result = postTagRepository.findByTagOrderByPostIdDesc(TAG, PageRequest.of(0, 10));

        assertThat(result).extracting(t -> t.getPost().getId())
                .containsExactly(visible.getId());
    }

    private Post savePostWithTag() {
        Post post = postRepository.save(Post.builder()
                .title("제목").content("내용").user(user).boardType(BoardType.FREE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        postTagRepository.save(PostTag.builder().post(post).tag(TAG).build());
        return post;
    }
}
