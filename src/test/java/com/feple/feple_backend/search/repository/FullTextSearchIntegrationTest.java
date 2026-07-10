package com.feple.feple_backend.search.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import io.awspring.cloud.s3.S3Template;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// PostRepository.searchTitleIds / ArtistRepository.searchArtistsByNameFullText /
// FestivalRepository.findByTitleKeyword는 MySQL 전용 MATCH...AGAINST(FULLTEXT ngram,
// V33 마이그레이션) native query라 H2로는 실행조차 안 된다. 지금까지 이 3개 쿼리는
// 리포지토리를 Mockito로 mock하거나 아예 테스트 대상에서 빠져 있어 실제 실행 여부가
// 한 번도 검증된 적이 없었음. InnoDB FULLTEXT 인덱스는 커밋된 행만 검색에 반영되므로
// (같은 트랜잭션 내 미커밋 INSERT는 안 보일 수 있음) 여기서는 @Transactional 롤백 대신
// 실제 커밋 후 @AfterEach에서 직접 정리한다.
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FullTextSearchIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideSchemaManagement(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired PostRepository postRepository;
    @Autowired ArtistRepository artistRepository;
    @Autowired FestivalRepository festivalRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager em;
    @Autowired PlatformTransactionManager txManager;

    @MockBean FileStorageService fileStorageService;
    @MockBean S3Template s3Template;

    private User user;
    private Post post;
    private Artist artist;
    private Festival festival;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        user = userRepository.save(User.builder()
                .oauthId("fulltext-test-user-" + unique).nickname("풀텍스트테스터" + unique).build());

        post = postRepository.save(Post.builder()
                .title("여름밤페스티벌 라인업 공개" + unique).content("내용").user(user)
                .boardType(BoardType.FREE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        // "IU"처럼 3자 미만 영문 단어는 innodb_ft_min_token_size(기본값 3)에 걸려
        // FULLTEXT 인덱스 자체에서 제외되므로(별도 확인된 제품 이슈), 여기서는 쿼리
        // 메커니즘 자체 검증을 위해 3자 이상인 이름을 사용한다.
        artist = artistRepository.save(Artist.builder()
                .name("아이유" + unique).nameEn("Aiyu " + unique)
                .build());

        festival = festivalRepository.save(Festival.builder()
                .title("여름밤페스티벌" + unique)
                .build());
    }

    @AfterEach
    void tearDown() {
        // Post는 @SQLDelete로 deleteById()가 soft-delete(UPDATE deleted_at)만 수행해
        // users FK가 그대로 남는다 — native 쿼리로 물리 삭제한다(트랜잭션 필요).
        if (post != null) {
            new TransactionTemplate(txManager).executeWithoutResult(status ->
                    em.createNativeQuery("DELETE FROM post WHERE id = ?1").setParameter(1, post.getId()).executeUpdate());
        }
        if (artist != null) artistRepository.deleteById(artist.getId());
        if (festival != null) festivalRepository.deleteById(festival.getId());
        if (user != null) userRepository.deleteById(user.getId());
    }

    @Test
    void 게시글_제목_풀텍스트_검색() {
        var result = postRepository.searchTitleIds("여름밤페스티벌", PageRequest.of(0, 10));

        assertThat(result.getContent()).contains(post.getId());
    }

    @Test
    void 게시판타입_지정_게시글_제목_풀텍스트_검색() {
        var result = postRepository.searchTitleIdsByBoardType("FREE", "여름밤페스티벌", PageRequest.of(0, 10));

        assertThat(result.getContent()).contains(post.getId());
    }

    @Test
    void 아티스트_이름_풀텍스트_검색() {
        List<Artist> result = artistRepository.searchArtistsByNameFullText("아이유");

        assertThat(result).extracting(Artist::getId).contains(artist.getId());
    }

    @Test
    void 아티스트_영문명_풀텍스트_검색() {
        List<Artist> result = artistRepository.searchArtistsByNameFullText("Aiyu");

        assertThat(result).extracting(Artist::getId).contains(artist.getId());
    }

    @Test
    void 페스티벌_제목_풀텍스트_검색() {
        List<Festival> result = festivalRepository.findByTitleKeyword("여름밤페스티벌");

        assertThat(result).extracting(Festival::getId).contains(festival.getId());
    }
}
