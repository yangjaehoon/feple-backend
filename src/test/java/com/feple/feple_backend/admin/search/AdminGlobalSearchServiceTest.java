package com.feple.feple_backend.admin.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class AdminGlobalSearchServiceTest {

    @Mock UserAdminService userAdminService;
    @Mock PostAdminService postAdminService;
    @Mock ArtistAdminService artistAdminService;
    @Mock FestivalAdminService festivalAdminService;

    @InjectMocks AdminGlobalSearchService searchService;

    @Test
    void 권한이_있는_도메인만_조회한다() {
        given(userAdminService.getUsersPage(anyInt(), anyInt(), any()))
                .willReturn(new PageImpl<>(List.of(UserResponseDto.builder().id(1L).nickname("닉네임").build())));

        AdminSearchResults results = searchService.search("키워드", Set.of(AdminPermission.USERS));

        assertThat(results.users().permitted()).isTrue();
        assertThat(results.users().items()).hasSize(1);
        assertThat(results.posts().permitted()).isFalse();
        assertThat(results.artists().permitted()).isFalse();
        assertThat(results.festivals().permitted()).isFalse();
        verify(postAdminService, never()).getPostsForAdmin(any());
        verify(artistAdminService, never()).getAdminArtistList(any(), any(), any(), anyInt());
        verify(festivalAdminService, never()).getFestivalsAdminPage(any(), anyInt(), anyInt());
    }

    @Test
    void 권한이_없으면_해당_도메인은_빈_섹션을_반환한다() {
        AdminSearchResults results = searchService.search("키워드", Set.of());

        assertThat(results.users().permitted()).isFalse();
        assertThat(results.users().isEmpty()).isTrue();
        assertThat(results.isEmpty()).isTrue();
        verify(userAdminService, never()).getUsersPage(anyInt(), anyInt(), any());
    }

    @Test
    void 게시글_검색_결과를_담는다() {
        given(postAdminService.getPostsForAdmin(any()))
                .willReturn(new PageImpl<>(List.of(PostResponseDto.builder().id(1L).title("제목").build())));

        AdminSearchResults results = searchService.search("키워드", Set.of(AdminPermission.POSTS));

        assertThat(results.posts().items()).extracting(PostResponseDto::getTitle).containsExactly("제목");
    }

    @Test
    void 아티스트_결과가_미리보기_개수보다_많으면_잘라낸다() {
        List<ArtistResponseDto> tenArtists = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> ArtistResponseDto.builder().id((long) i).name("아티스트" + i).build())
                .toList();
        given(artistAdminService.getAdminArtistList(eq(""), eq("키워드"), eq(null), eq(0)))
                .willReturn(new PageImpl<>(tenArtists));

        AdminSearchResults results = searchService.search("키워드", Set.of(AdminPermission.ARTISTS));

        assertThat(results.artists().items()).hasSize(5);
        assertThat(results.artists().total()).isEqualTo(10);
    }

    @Test
    void 페스티벌_검색_결과를_담는다() {
        given(festivalAdminService.getFestivalsAdminPage(eq("키워드"), eq(0), anyInt()))
                .willReturn(new PageImpl<>(List.of(FestivalResponseDto.builder().id(1L).title("페스티벌").build())));

        AdminSearchResults results = searchService.search("키워드", Set.of(AdminPermission.FESTIVALS));

        assertThat(results.festivals().items()).extracting(FestivalResponseDto::getTitle).containsExactly("페스티벌");
    }
}
