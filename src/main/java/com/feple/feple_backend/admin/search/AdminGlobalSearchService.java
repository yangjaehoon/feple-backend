package com.feple.feple_backend.admin.search;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

// 관리자 전역 검색 — 각 도메인의 기존 관리자 검색 로직(UserAdminService 등)을 그대로 재사용해
// 결과를 미리보기 개수로만 잘라 모은다. 권한이 없는 도메인은 조회 자체를 건너뛴다(AdminPermissionInterceptor와
// 동일하게 PERM_* 권한 기준).
@Service
@RequiredArgsConstructor
public class AdminGlobalSearchService {

    private static final int PREVIEW_LIMIT = 5;

    private final UserAdminService userAdminService;
    private final PostAdminService postAdminService;
    private final ArtistAdminService artistAdminService;
    private final FestivalAdminService festivalAdminService;

    public AdminSearchResults search(String keyword, Set<AdminPermission> granted) {
        return new AdminSearchResults(
                searchUsers(keyword, granted),
                searchPosts(keyword, granted),
                searchArtists(keyword, granted),
                searchFestivals(keyword, granted));
    }

    private AdminSearchSection<UserResponseDto> searchUsers(String keyword, Set<AdminPermission> granted) {
        if (!granted.contains(AdminPermission.USERS)) return AdminSearchSection.notPermitted();
        Page<UserResponseDto> page = userAdminService.getUsersPage(0, PREVIEW_LIMIT, keyword);
        return new AdminSearchSection<>(page.getContent(), page.getTotalElements(), true);
    }

    private AdminSearchSection<PostResponseDto> searchPosts(String keyword, Set<AdminPermission> granted) {
        if (!granted.contains(AdminPermission.POSTS)) return AdminSearchSection.notPermitted();
        PostAdminFilterDto params = new PostAdminFilterDto(0, PREVIEW_LIMIT, "", keyword, null, null);
        Page<PostResponseDto> page = postAdminService.getPostsForAdmin(params);
        return new AdminSearchSection<>(page.getContent(), page.getTotalElements(), true);
    }

    private AdminSearchSection<ArtistResponseDto> searchArtists(String keyword, Set<AdminPermission> granted) {
        if (!granted.contains(AdminPermission.ARTISTS)) return AdminSearchSection.notPermitted();
        Page<ArtistResponseDto> page = artistAdminService.getAdminArtistList("", keyword, null, 0);
        return new AdminSearchSection<>(preview(page.getContent()), page.getTotalElements(), true);
    }

    private AdminSearchSection<FestivalResponseDto> searchFestivals(String keyword, Set<AdminPermission> granted) {
        if (!granted.contains(AdminPermission.FESTIVALS)) return AdminSearchSection.notPermitted();
        Page<FestivalResponseDto> page = festivalAdminService.getFestivalsAdminPage(keyword, 0, PREVIEW_LIMIT);
        return new AdminSearchSection<>(page.getContent(), page.getTotalElements(), true);
    }

    private static <T> List<T> preview(List<T> items) {
        return items.size() <= PREVIEW_LIMIT ? items : items.subList(0, PREVIEW_LIMIT);
    }
}
