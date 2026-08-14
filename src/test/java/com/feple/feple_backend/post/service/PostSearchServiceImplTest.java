package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PostSearchServiceImplTest {

    @Mock PostRepository postRepository;
    @Spy BlockedContentFilter blockedContentFilter = new BlockedContentFilter(mock(UserBlockService.class));

    @Mock FileStorageService fileStorageService;
    @InjectMocks PostSearchServiceImpl postSearchService;

    @Test
    void 게시판타입_지정하여_검색() {
        User author = user(1L);
        given(postRepository.searchPostsByBoardTypeAndTitleFullText(
                eq(BoardType.FREE), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목검색", "FREE", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void 게시판타입_없이_전체_검색() {
        User author = user(1L);
        given(postRepository.searchPostsByTitleFullText(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목검색", null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void 두글자_이하_키워드는_LIKE_폴백_검색() {
        // "제목"은 2자 — innodb_ft_min_token_size(3) 미만이라 FULLTEXT 대신 LIKE 폴백을 탄다.
        User author = user(1L);
        given(postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목", null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void 두글자_이하_키워드_게시판타입_지정시_LIKE_폴백() {
        User author = user(1L);
        given(postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        eq(BoardType.FREE), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목", "FREE", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void MATE_타입_검색() {
        User author = user(1L);
        given(postRepository.searchPostsByBoardTypeAndTitleFullText(
                        eq(BoardType.MATE), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("동행검색", "MATE", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void 알수없는_boardType이면_전체_검색() {
        User author = user(1L);
        given(postRepository.searchPostsByTitleFullText(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목검색", "UNKNOWN", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void 검색시_노출개수보다_넉넉한_풀을_조회() {
        // 검색은 다음 페이지 개념이 없는 단발성 목록이라, 차단 필터링 이후 결과가 줄어들어도
        // 재요청으로 보충할 수 없다 — 항상 SEARCH_POOL만큼 넉넉히 조회해야 한다
        given(postRepository.searchPostsByTitleFullText(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postSearchService.searchPosts("제목검색", null, null);

        then(postRepository).should().searchPostsByTitleFullText(anyString(),
                argThat((Pageable p) -> p.getPageSize() == PageSize.SEARCH_POOL));
    }

    @Test
    void 검색결과가_노출개수보다_많으면_잘라서_반환() {
        User author = user(1L);
        List<com.feple.feple_backend.post.entity.Post> many = new ArrayList<>();
        for (long i = 1; i <= PageSize.SEARCH_POOL; i++) many.add(freePost(i, author));
        given(postRepository.searchPostsByTitleFullText(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(many));

        List<PostResponseDto> result = postSearchService.searchPosts("제목검색", null, null);

        assertThat(result).hasSize(PageSize.SEARCH);
    }
}
