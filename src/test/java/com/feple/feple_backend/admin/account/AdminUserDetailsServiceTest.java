package com.feple.feple_backend.admin.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {

    @Mock AdminAccountRepository repository;

    @InjectMocks AdminUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_계정_없으면_UsernameNotFoundException() {
        given(repository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void loadUserByUsername_SUPER_ADMIN_모든_권한_부여() {
        AdminAccount account = AdminAccount.builder()
                .username("superadmin")
                .password("hashed")
                .role(AdminRole.SUPER_ADMIN)
                .build();
        given(repository.findByUsername("superadmin")).willReturn(Optional.of(account));

        UserDetails details = userDetailsService.loadUserByUsername("superadmin");

        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        for (AdminPermission permission : AdminPermission.values()) {
            assertThat(authorities).contains("PERM_" + permission.name());
        }
    }

    @Test
    void loadUserByUsername_MANAGER_개인_권한만_부여() {
        AdminAccount account = AdminAccount.builder()
                .username("manager")
                .password("hashed")
                .role(AdminRole.MANAGER)
                .permissions(Set.of(AdminPermission.POSTS, AdminPermission.REPORTS))
                .build();
        given(repository.findByUsername("manager")).willReturn(Optional.of(account));

        UserDetails details = userDetailsService.loadUserByUsername("manager");

        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_ADMIN", "PERM_POSTS", "PERM_REPORTS");
        assertThat(authorities).doesNotContain("ROLE_SUPER_ADMIN", "PERM_USERS");
    }

    @Test
    void loadUserByUsername_계정_정보_그대로_반환() {
        AdminAccount account = AdminAccount.builder()
                .username("admin1")
                .password("encodedPw")
                .role(AdminRole.MANAGER)
                .build();
        given(repository.findByUsername("admin1")).willReturn(Optional.of(account));

        UserDetails details = userDetailsService.loadUserByUsername("admin1");

        assertThat(details.getUsername()).isEqualTo("admin1");
        assertThat(details.getPassword()).isEqualTo("encodedPw");
        assertThat(details.isEnabled()).isEqualTo(account.isEnabled());
    }
}
