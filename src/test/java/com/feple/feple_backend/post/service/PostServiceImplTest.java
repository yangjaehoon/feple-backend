package com.feple.feple_backend.post.service;

import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock CommentService commentService;
    @Mock UserRepository userRepository;
    @Mock ArtistRepository artistRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock FestivalCertificationRepository certificationRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock BadWordFilter badWordFilter;
    @Mock ApplicationEventPublisher eventPublisher;

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

    // ── getPostsByFestivalId ──────────────────────────────────────────

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
        given(certificationRepository.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findGeneralFestivalPosts(eq(festival), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPostsByFestivalId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCertified()).isTrue();
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
        given(certificationRepository.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findGeneralFestivalPosts(eq(festival), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPostsByFestivalId(5L);

        assertThat(result.get(0).isCertified()).isFalse();
    }

    // ── getHotPosts ──────────────────────────────────────────────────

    @Test
    void 핫_게시글_최대_4개_반환() {
        User author = user(1L);
        List<Post> hotPosts = List.of(
                freePost(1L, author), freePost(2L, author),
                freePost(3L, author), freePost(4L, author));
        given(postRepository.findHotPosts(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(hotPosts);

        List<PostResponseDto> result = postService.getHotPosts();

        assertThat(result).hasSize(4);
    }

    @Test
    void 핫_게시글_없으면_빈_리스트() {
        given(postRepository.findHotPosts(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(List.of());

        assertThat(postService.getHotPosts()).isEmpty();
    }

    // ── getPostsByArtistId ────────────────────────────────────────────

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
        given(postRepository.findByArtistOrderByCreatedAtDesc(eq(artist), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPostsByArtistId(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBoardDisplayName()).isEqualTo("아이유 게시판");
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

    // ── deletePost (admin) ────────────────────────────────────────────

    @Test
    void 관리자_게시글_삭제시_삭제이벤트_발행() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deletePost(10L);

        verify(eventPublisher).publishEvent(any(PostDeletedByAdminEvent.class));
        verify(postRepository).deleteById(10L);
    }

    // ── bulkDeletePosts ──────────────────────────────────────────────

    @Test
    void 게시글_일괄_삭제_성공() {
        postService.bulkDeletePosts(List.of(1L, 2L, 3L));

        verify(postRepository).softDeleteByIds(List.of(1L, 2L, 3L));
    }

    @Test
    void 빈_ID_목록으로_일괄_삭제시_아무동작_안함() {
        postService.bulkDeletePosts(List.of());

        verify(postRepository, never()).softDeleteByIds(any());
    }

    // ── restorePost / getDeletedPosts ──────────────────────────────────

    @Test
    void 게시글_복구_성공() {
        postService.restorePost(10L);

        verify(postRepository).restore(10L);
    }

    @Test
    void 삭제된_게시글_목록_조회() {
        User author = user(1L);
        given(postRepository.findSoftDeleted(20)).willReturn(List.of(freePost(1L, author)));

        List<PostResponseDto> result = postService.getDeletedPosts(20);

        assertThat(result).hasSize(1);
    }

    // ── countPostsContaining / getPostCountsByUserIds ───────────────────

    @Test
    void 특정_단어_포함_게시글_수_조회() {
        given(postRepository.countByTitleOrContentContaining("공지")).willReturn(3L);

        assertThat(postService.countPostsContaining("공지")).isEqualTo(3L);
    }

    @Test
    void 사용자별_게시글_수_조회() {
        given(postRepository.countGroupByUserId(List.of(1L, 2L)))
                .willReturn(List.of(new Object[]{1L, 5L}, new Object[]{2L, 3L}));

        Map<Long, Long> result = postService.getPostCountsByUserIds(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 5L).containsEntry(2L, 3L);
    }

    @Test
    void 빈_사용자ID_목록으로_게시글_수_조회시_빈_맵_반환() {
        Map<Long, Long> result = postService.getPostCountsByUserIds(List.of());

        assertThat(result).isEmpty();
        verify(postRepository, never()).countGroupByUserId(any());
    }

    // ── incrementViewCount ───────────────────────────────────────────

    @Test
    void 조회수_증가_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.incrementViewCount(10L);

        assertThat(post.getViewCount()).isEqualTo(1);
    }

    // ── removePostActivityByUser ───────────────────────────────────────

    @Test
    void 사용자_게시글_활동_삭제시_좋아요와_스크랩_모두_정리() {
        postService.removePostActivityByUser(1L);

        verify(postLikeRepository).decrementPostLikeCountByUserId(1L);
        verify(postLikeRepository).deleteByUserId(1L);
        verify(postScrapRepository).decrementPostScrapCountByUserId(1L);
        verify(postScrapRepository).deleteByUserId(1L);
    }

    // ── deletePostsByArtist ──────────────────────────────────────────

    @Test
    void 아티스트_게시글_일괄_삭제시_연관데이터_모두_삭제() {
        User author = user(1L);
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        Post post = Post.builder()
                .id(20L).title("t").content("c").user(author).artist(artist)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        given(postRepository.findByArtist(artist)).willReturn(List.of(post));

        postService.deletePostsByArtist(artist);

        verify(postRepository).nullifyArtistIdForSoftDeleted(3L);
        verify(commentService).deleteByPostIds(List.of(20L));
        verify(postLikeRepository).deleteByPostIds(List.of(20L));
        verify(postScrapRepository).deleteByPostIds(List.of(20L));
        verify(postReportRepository).deleteByPostIds(List.of(20L));
        verify(notificationRepository).deleteByPostIdIn(List.of(20L));
        verify(postRepository).deleteAllByIdInBatch(List.of(20L));
    }

    @Test
    void 아티스트에_연관된_게시글이_없으면_연관데이터_삭제_스킵() {
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        given(postRepository.findByArtist(artist)).willReturn(List.of());

        postService.deletePostsByArtist(artist);

        verify(postRepository).nullifyArtistIdForSoftDeleted(3L);
        verify(commentService, never()).deleteByPostIds(any());
        verify(postRepository, never()).deleteAllByIdInBatch(any());
    }

    // ── getPostsForAdmin ─────────────────────────────────────────────

    @Test
    void 관리자_게시판타입_필터로_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "FREE", null, null, null);
        given(postRepository.findByBoardTypeOrderByCreatedAtDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postService.getPostsForAdmin(params);

        verify(postRepository).findByBoardTypeOrderByCreatedAtDesc(eq(BoardType.FREE), any(Pageable.class));
    }

    @Test
    void 관리자_아티스트ID_지정_필터로_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "ARTIST", null, 3L, null);
        given(postRepository.findByArtistIdOrderByCreatedAtDesc(eq(3L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postService.getPostsForAdmin(params);

        verify(postRepository).findByArtistIdOrderByCreatedAtDesc(eq(3L), any(Pageable.class));
    }

    @Test
    void 관리자_아티스트_필터_ID없이_전체_아티스트_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "ARTIST", null, null, null);
        given(postRepository.findByArtistIsNotNullOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postService.getPostsForAdmin(params);

        verify(postRepository).findByArtistIsNotNullOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void 관리자_필터_없으면_전체_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, null, null, null, null);
        given(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postService.getPostsForAdmin(params);

        verify(postRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void 관리자_키워드_포함시_제목_검색으로_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, null, "공지", null, null);
        given(postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postService.getPostsForAdmin(params);

        verify(postRepository).findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class));
    }

    // ── searchPosts ──────────────────────────────────────────────────

    @Test
    void 게시판타입_지정하여_검색() {
        User author = user(1L);
        given(postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                eq(BoardType.FREE), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postService.searchPosts("제목", "FREE");

        assertThat(result).hasSize(1);
    }

    @Test
    void 게시판타입_없이_전체_검색() {
        User author = user(1L);
        given(postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postService.searchPosts("제목", null);

        assertThat(result).hasSize(1);
    }

    // ── 마이페이지 활동 조회 ─────────────────────────────────────────────

    @Test
    void 내_게시글_수_조회() {
        given(postRepository.countByUserId(1L)).willReturn(7L);

        assertThat(postService.countMyPosts(1L)).isEqualTo(7L);
    }

    @Test
    void 공개_게시글_수_조회() {
        given(postRepository.countPublicByUserId(1L)).willReturn(4L);

        assertThat(postService.countPublicPosts(1L)).isEqualTo(4L);
    }

    @Test
    void 좋아요한_게시글_수_조회() {
        given(postLikeRepository.countByUserId(1L)).willReturn(2L);

        assertThat(postService.countLikedPosts(1L)).isEqualTo(2L);
    }

    @Test
    void 게시판타입_커서_페이징_다음페이지_있음() {
        User author = user(1L);
        List<Post> posts = List.of(freePost(3L, author), freePost(2L, author), freePost(1L, author));
        given(postRepository.findByBoardTypeOrderByIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(posts);

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypePaged(BoardType.FREE, null, 2);

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L);
    }

    @Test
    void 게시판타입_커서_페이징_다음페이지_없음() {
        User author = user(1L);
        List<Post> posts = List.of(freePost(1L, author));
        given(postRepository.findByBoardTypeOrderByIdDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(posts);

        CursorPage<PostResponseDto> result = postService.getPostsByBoardTypePaged(BoardType.FREE, null, 2);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }
}
