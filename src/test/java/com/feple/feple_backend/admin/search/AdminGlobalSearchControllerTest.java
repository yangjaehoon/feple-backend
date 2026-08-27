package com.feple.feple_backend.admin.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.feple.feple_backend.admin.account.AdminPermission;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminGlobalSearchControllerTest {

    @Mock AdminGlobalSearchService searchService;

    @InjectMocks AdminGlobalSearchController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static UsernamePasswordAuthenticationToken adminWith(AdminPermission... permissions) {
        List<SimpleGrantedAuthority> authorities = List.of(permissions).stream()
                .map(p -> new SimpleGrantedAuthority(p.readAuthority()))
                .toList();
        return new UsernamePasswordAuthenticationToken("admin", null, authorities);
    }

    @Test
    void 키워드가_없으면_검색을_수행하지_않는다() throws Exception {
        mockMvc.perform(get("/admin/search").principal(adminWith(AdminPermission.USERS)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/search/results"))
                .andExpect(model().attributeDoesNotExist("results"));

        verify(searchService, never()).search(any(), any());
    }

    @Test
    void 키워드가_있으면_보유_권한만_전달해_검색한다() throws Exception {
        given(searchService.search(eq("아이유"), any())).willReturn(null);

        mockMvc.perform(get("/admin/search")
                        .param("keyword", "아이유")
                        .principal(adminWith(AdminPermission.USERS, AdminPermission.ARTISTS)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/search/results"));

        verify(searchService).search("아이유", Set.of(AdminPermission.USERS, AdminPermission.ARTISTS));
    }

    @Test
    void 권한이_없는_관리자는_빈_권한_집합으로_검색한다() throws Exception {
        given(searchService.search(eq("검색어"), any())).willReturn(null);

        mockMvc.perform(get("/admin/search")
                        .param("keyword", "검색어")
                        .principal(adminWith()))
                .andExpect(status().isOk());

        verify(searchService).search("검색어", Set.of());
    }
}
