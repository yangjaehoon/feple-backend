package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.repository.PostImageRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostImageRepository postImageRepository;
    @Mock UserRepository userRepository;
    @Mock ArtistRepository artistRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock FestivalCertificationService certificationService;
    @Mock BadWordValidator badWordFilter;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PopularPostCache popularPostCache;
    @Mock S3ObjectVerificationService s3ObjectVerificationService;
    UserBlockService userBlockService = mock(UserBlockService.class);
    @Spy BlockedContentFilter blockedContentFilter = new BlockedContentFilter(userBlockService);

    @InjectMocks PostServiceImpl postService;

    // ── createPost ──────────────────────────────────────────────────

    @Test
    void 게시글_생성_성공() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .boardType(BoardType.FREE).build();
        Post saved = freePost(10L, author);

        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        Long id = postService.createPost(dto, 1L);

        assertThat(id).isEqualTo(10L);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void 금칙어_포함_게시글_생성시_예외() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("욕설포함제목").content("내용")
                .boardType(BoardType.FREE).build();
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        willThrow(new IllegalArgumentException("금칙어가 포함되어 있습니다."))
                .given(badWordFilter).validateField(eq("title"), any());

        assertThatThrownBy(() -> postService.createPost(dto, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_사용자로_게시글_생성시_예외() {
        PostRequestDto dto = PostRequestDto.builder().title("t").content("c")
                .boardType(BoardType.FREE).build();
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(dto, 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void 본인_프리픽스_밖의_이미지URL이면_게시글_생성_예외() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .boardType(BoardType.FREE).imageUrls(List.of("posts/2/other-user.jpg")).build();
        given(userRepository.findById(1L)).willReturn(Optional.of(author));

        assertThatThrownBy(() -> postService.createPost(dto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키입니다.");
        verify(postRepository, never()).save(any());
    }

    @Test
    void 본인_프리픽스_이미지URL이면_S3_존재검증후_게시글_생성() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .boardType(BoardType.FREE).imageUrls(List.of("posts/1/photo.jpg")).build();
        Post saved = freePost(10L, author);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        Long id = postService.createPost(dto, 1L);

        assertThat(id).isEqualTo(10L);
        verify(s3ObjectVerificationService).verifyImageObject("posts/1/photo.jpg");
    }

    // ── getPost ──────────────────────────────────────────────────────

    @Test
    void 게시글_단건_조회_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findWithAssociationsById(10L)).willReturn(Optional.of(post));

        PostResponseDto result = postService.getPost(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getNickname()).isEqualTo("user1");
    }

    @Test
    void 익명_게시글_조회시_nickname이_익명_반환() {
        User author = user(1L);
        Post post = Post.builder()
                .id(10L).title("익명 게시글").content("내용")
                .user(author).boardType(BoardType.FREE)
                .anonymous(true)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        given(postRepository.findWithAssociationsById(10L)).willReturn(Optional.of(post));

        PostResponseDto result = postService.getPost(10L);

        assertThat(result.getNickname()).isEqualTo("익명");
        assertThat(result.getProfileImageUrl()).isNull();
    }

    @Test
    void 존재하지_않는_게시글_조회시_예외() {
        given(postRepository.findWithAssociationsById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── deleteOwnPost ────────────────────────────────────────────────

    @Test
    void 본인_게시글_삭제_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deleteOwnPost(10L, 1L);

        // soft delete: post 행이 남아 FK 무결성 유지 → like 사전 삭제 불필요
        verify(postRepository).deleteById(10L);
    }

    @Test
    void 타인이_게시글_삭제시_접근_거부_예외() {
        User owner = user(1L);
        Post post = freePost(10L, owner);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deleteOwnPost(10L, 2L))
                .isInstanceOf(AccessDeniedException.class);

        verify(postRepository, never()).deleteById(any());
    }

    // ── updateOwnPost ────────────────────────────────────────────────

    @Test
    void 본인_게시글_수정_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        PostRequestDto dto = PostRequestDto.builder().title("수정된 제목").content("수정된 내용")
                .boardType(BoardType.FREE).build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.updateOwnPost(10L, dto, 1L);

        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void 타인이_게시글_수정시_접근_거부_예외() {
        User owner = user(1L);
        Post post = freePost(10L, owner);
        PostRequestDto dto = PostRequestDto.builder().title("t").content("c")
                .boardType(BoardType.FREE).build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updateOwnPost(10L, dto, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 금칙어_포함_게시글_수정시_예외() {
        User author = user(1L);
        Post post = freePost(10L, author);
        PostRequestDto dto = PostRequestDto.builder().title("욕설포함제목").content("내용")
                .boardType(BoardType.FREE).build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        willThrow(new IllegalArgumentException("금칙어가 포함되어 있습니다."))
                .given(badWordFilter).validateField(eq("title"), any());

        assertThatThrownBy(() -> postService.updateOwnPost(10L, dto, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 게시글_수정시_타인_프리픽스_이미지URL이면_예외() {
        User author = user(1L);
        Post post = freePost(10L, author);
        PostRequestDto dto = PostRequestDto.builder().title("t").content("c")
                .boardType(BoardType.FREE).imageUrls(List.of("posts/2/other-user.jpg")).build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updateOwnPost(10L, dto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키입니다.");
    }

    // ── incrementViewCount ───────────────────────────────────────────

    @Test
    void 조회수_증가_성공() {
        given(postRepository.existsById(10L)).willReturn(true);

        postService.incrementViewCount(10L);

        verify(postRepository).incrementViewCount(10L);
    }

    @Test
    void 존재하지_않는_게시글_조회수_증가시_예외() {
        given(postRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> postService.incrementViewCount(999L))
                .isInstanceOf(NoSuchElementException.class);

        verify(postRepository, never()).incrementViewCount(999L);
    }

    // ── getPopularPosts ──────────────────────────────────────────────────

    @Test
    void 핫_게시글_최대_4개_반환() {
        List<PostResponseDto> popularPosts = List.of(
                PostResponseDto.builder().id(1L).userId(1L).build(),
                PostResponseDto.builder().id(2L).userId(1L).build(),
                PostResponseDto.builder().id(3L).userId(1L).build(),
                PostResponseDto.builder().id(4L).userId(1L).build());
        given(popularPostCache.getPopularPosts()).willReturn(popularPosts);

        List<PostResponseDto> result = postService.getPopularPosts(null);

        assertThat(result).hasSize(4);
    }

    @Test
    void 핫_게시글_없으면_빈_리스트() {
        given(popularPostCache.getPopularPosts()).willReturn(List.of());

        assertThat(postService.getPopularPosts(null)).isEmpty();
    }

    @Test
    void 핫_게시글_캐시풀이_4개보다_많아도_4개로_자름() {
        List<PostResponseDto> pool = List.of(
                PostResponseDto.builder().id(1L).userId(1L).build(),
                PostResponseDto.builder().id(2L).userId(1L).build(),
                PostResponseDto.builder().id(3L).userId(1L).build(),
                PostResponseDto.builder().id(4L).userId(1L).build(),
                PostResponseDto.builder().id(5L).userId(1L).build(),
                PostResponseDto.builder().id(6L).userId(1L).build());
        given(popularPostCache.getPopularPosts()).willReturn(pool);

        List<PostResponseDto> result = postService.getPopularPosts(null);

        assertThat(result).hasSize(4);
    }

    @Test
    void 핫_게시글_차단필터링_후에도_4개까지_채움() {
        // 캐시 풀에 차단 작성자의 글이 섞여 있어도, 풀을 넉넉히 가져왔기 때문에
        // 필터링 후에도 최종 4개를 채울 수 있어야 한다 (POPULAR_POSTS_POOL 존재 이유)
        List<PostResponseDto> pool = List.of(
                PostResponseDto.builder().id(1L).userId(99L).build(), // 차단 작성자
                PostResponseDto.builder().id(2L).userId(1L).build(),
                PostResponseDto.builder().id(3L).userId(1L).build(),
                PostResponseDto.builder().id(4L).userId(1L).build(),
                PostResponseDto.builder().id(5L).userId(1L).build());
        given(popularPostCache.getPopularPosts()).willReturn(pool);
        given(userBlockService.getBlockedIds(1L)).willReturn(List.of(99L));

        List<PostResponseDto> result = postService.getPopularPosts(1L);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(PostResponseDto::getId).doesNotContain(1L);
    }

    // ── getPostsByBoardTypeLatest ───────────────────────────────────────

    @Test
    void 게시판타입_커서_페이징_다음페이지_있음() {
        User author = user(1L);
        List<Post> posts = List.of(freePost(3L, author), freePost(2L, author), freePost(1L, author));
        given(postRepository.findByBoardTypeAndPinnedFalseOrderByIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(posts);

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypeLatest(BoardType.FREE, new CursorPageRequest(null, 2, null));

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L);
    }

    @Test
    void 게시판타입_커서_페이징_첫페이지에만_고정글이_상단에_붙음() {
        User author = user(1L);
        Post pinned = freePost(99L, author);
        pinned.togglePinned();
        given(postRepository.findByBoardTypeAndPinnedTrueOrderByCreatedAtDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(List.of(pinned));
        given(postRepository.findByBoardTypeAndPinnedFalseOrderByIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(List.of(freePost(2L, author), freePost(1L, author)));

        CursorPage<PostResponseDto> firstPage = postService.getPostsByBoardTypeLatest(BoardType.FREE, new CursorPageRequest(null, 2, null));
        assertThat(firstPage.content()).extracting(PostResponseDto::getId).containsExactly(99L, 2L);

        given(postRepository.findByBoardTypeAndPinnedFalseAndIdLessThanOrderByIdDesc(eq(BoardType.FREE), eq(2L), any(Pageable.class)))
                .willReturn(List.of(freePost(1L, author)));
        CursorPage<PostResponseDto> secondPage = postService.getPostsByBoardTypeLatest(BoardType.FREE, new CursorPageRequest(2L, 2, null));
        assertThat(secondPage.content()).extracting(PostResponseDto::getId).containsExactly(1L);
    }

    @Test
    void 게시판타입_커서_페이징_다음페이지_없음() {
        User author = user(1L);
        List<Post> posts = List.of(freePost(1L, author));
        given(postRepository.findByBoardTypeAndPinnedFalseOrderByIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(posts);

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypeLatest(BoardType.FREE, new CursorPageRequest(null, 2, null));

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void 커서_페이징_배치가_전부_차단유저글이면_content는_비어도_nextCursor는_유지() {
        User blockedAuthor = user(9L);
        // size=2, fetchSize=3 — 3건 모두 차단된 작성자의 글
        List<Post> posts = List.of(freePost(3L, blockedAuthor), freePost(2L, blockedAuthor), freePost(1L, blockedAuthor));
        given(postRepository.findByBoardTypeAndPinnedFalseOrderByIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(posts);
        given(userBlockService.getBlockedIds(100L)).willReturn(List.of(9L));

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypeLatest(BoardType.FREE, new CursorPageRequest(null, 2, 100L));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isTrue();
        // content가 비어도 nextCursor는 raw 목록(필터링 전) 기준으로 계산되어야 다음 배치를 계속 조회할 수 있다
        assertThat(result.nextCursor()).isEqualTo(2L);
    }

    // ── getPostsByArtistIdPaged ────────────────────────────────────────

    @Test
    void 아티스트_게시글_목록_조회() {
        User author = user(1L);
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        Post post = Post.builder()
                .id(20L).title("아이유 게시글").content("내용")
                .user(author).artist(artist)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));
        given(postRepository.findByArtistOrderByIdDesc(eq(artist), any(Pageable.class)))
                .willReturn(List.of(post));

        CursorPage<PostResponseDto> result = postService.getPostsByArtistIdPaged(3L, new CursorPageRequest(null, 20, null));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getBoardDisplayName()).isEqualTo("아이유 게시판");
    }

    @Test
    void 아티스트_게시글_id_기반_커서로_다음페이지_조회() {
        User author = user(1L);
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        List<Post> posts = List.of(freePost(3L, author), freePost(2L, author), freePost(1L, author));

        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));
        given(postRepository.findByArtistAndIdLessThanOrderByIdDesc(eq(artist), eq(5L), any(Pageable.class)))
                .willReturn(posts);

        CursorPage<PostResponseDto> result = postService.getPostsByArtistIdPaged(3L, new CursorPageRequest(5L, 2, null));

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L);
    }

    // ── getPostsByFestivalIdPaged ──────────────────────────────────────

    @Test
    void 페스티벌_게시글_인증된_사용자_플래그_true() {
        User certifiedUser = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(10L).title("후기").content("좋았음")
                .user(certifiedUser).festival(festival)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationService.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findGeneralFestivalPostsOrderByIdDesc(eq(festival), any(Pageable.class)))
                .willReturn(List.of(post));

        CursorPage<PostResponseDto> result = postService.getPostsByFestivalIdPaged(5L, new CursorPageRequest(null, 20, null));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).isCertified()).isTrue();
    }

    @Test
    void 페스티벌_게시글_미인증_사용자_플래그_false() {
        User uncertified = user(2L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(11L).title("일반 후기").content("내용")
                .user(uncertified).festival(festival)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationService.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findGeneralFestivalPostsOrderByIdDesc(eq(festival), any(Pageable.class)))
                .willReturn(List.of(post));

        CursorPage<PostResponseDto> result = postService.getPostsByFestivalIdPaged(5L, new CursorPageRequest(null, 20, null));

        assertThat(result.content().get(0).isCertified()).isFalse();
    }

    // ── getPostsByBoardTypePopular ───────────────────────────────────────

    @Test
    void 게시판타입_인기순_페이징_다음페이지_있음() {
        User author = user(1L);
        List<Post> posts = List.of(freePost(1L, author), freePost(2L, author));
        given(postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDescIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(new PageImpl<>(posts, PageRequest.of(0, 2), 5));

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypePopular(BoardType.FREE, new CursorPageRequest(null, 2, null));

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(1L);
    }

    @Test
    void 게시판타입_인기순_페이징_다음페이지_없음() {
        User author = user(1L);
        List<Post> posts = List.of(freePost(1L, author));
        given(postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDescIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(new PageImpl<>(posts, PageRequest.of(0, 2), 1));

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypePopular(BoardType.FREE, new CursorPageRequest(null, 2, null));

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    // ── createArtistPost ──────────────────────────────────────────────

    @Test
    void 아티스트_게시글_생성_성공() {
        User author = user(1L);
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용").build();
        Post saved = freePost(20L, author);

        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        Long id = postService.createArtistPost(3L, dto, 1L);

        assertThat(id).isEqualTo(20L);
        verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
    }

    @Test
    void 존재하지_않는_아티스트로_게시글_생성시_예외() {
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용").build();
        given(artistRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createArtistPost(99L, dto, 1L))
                .isInstanceOf(NoSuchElementException.class);
        verify(postRepository, never()).save(any());
    }

    // ── createFestivalPost ───────────────────────────────────────────

    @Test
    void 페스티벌_게시글_생성_성공() {
        User author = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용").build();
        Post saved = freePost(30L, author);

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        Long id = postService.createFestivalPost(5L, dto, 1L);

        assertThat(id).isEqualTo(30L);
        verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
    }

    @Test
    void 존재하지_않는_페스티벌로_게시글_생성시_예외() {
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용").build();
        given(festivalRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createFestivalPost(99L, dto, 1L))
                .isInstanceOf(NoSuchElementException.class);
        verify(postRepository, never()).save(any());
    }

    // ── getPostsByFestivalIdAndBoardTypePaged ─────────────────────────

    @Test
    void 페스티벌_게시판타입별_게시글_목록_조회() {
        User author = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(10L).title("동행 모집").content("내용")
                .user(author).festival(festival).boardType(BoardType.MATE)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationService.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findByFestivalAndBoardTypeOrderByIdDesc(eq(festival), eq(BoardType.MATE), any(Pageable.class)))
                .willReturn(List.of(post));

        CursorPage<PostResponseDto> result = postService.getPostsByFestivalIdAndBoardTypePaged(
                5L, BoardType.MATE, new CursorPageRequest(null, 20, null));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).isCertified()).isTrue();
    }

    @Test
    void 페스티벌_게시판타입별_커서로_다음페이지_조회() {
        User author = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        List<Post> posts = List.of(freePost(3L, author), freePost(2L, author), freePost(1L, author));

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationService.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of());
        given(postRepository.findByFestivalAndBoardTypeAndIdLessThanOrderByIdDesc(
                        eq(festival), eq(BoardType.FREE), eq(5L), any(Pageable.class)))
                .willReturn(posts);

        CursorPage<PostResponseDto> result = postService.getPostsByFestivalIdAndBoardTypePaged(
                5L, BoardType.FREE, new CursorPageRequest(5L, 2, null));

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
    }

    // ── createFestivalTypedPost ───────────────────────────────────────

    @Test
    void 페스티벌_타입지정_게시글_생성_성공() {
        User author = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용").build();
        Post saved = freePost(40L, author);

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        Long id = postService.createFestivalTypedPost(5L, dto, 1L, BoardType.MATE);

        assertThat(id).isEqualTo(40L);
        verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
    }

    // ── getPopularFestivalPosts ────────────────────────────────────────

    @Test
    void 페스티벌_인기_게시글_조회() {
        User author = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(10L).title("후기").content("내용")
                .user(author).festival(festival)
                .likeCount(10).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationService.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findByFestivalOrderByLikeCountDesc(eq(festival), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPopularFestivalPosts(5L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCertified()).isTrue();
    }

    @Test
    void 존재하지_않는_페스티벌_인기_게시글_조회시_예외() {
        given(festivalRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPopularFestivalPosts(99L, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── incrementCommentCount / decrementCommentCount ─────────────────

    @Test
    void 댓글수_증가시_repository에_위임() {
        postService.incrementCommentCount(10L);

        verify(postRepository).incrementCommentCount(10L);
    }

    @Test
    void 댓글수_감소시_repository에_위임() {
        postService.decrementCommentCount(10L);

        verify(postRepository).decrementCommentCount(10L);
    }
}
