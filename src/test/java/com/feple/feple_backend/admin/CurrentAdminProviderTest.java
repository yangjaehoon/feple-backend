package com.feple.feple_backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentAdminProviderTest {

    private final CurrentAdminProvider provider = new CurrentAdminProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username, String... roles) {
        var authorities = List.of(roles).stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(username, null, authorities));
    }

    @Test
    void usernameOrNull_인증되어_있으면_계정명() {
        authenticateAs("admin", "ROLE_ADMIN");
        assertThat(provider.usernameOrNull()).isEqualTo("admin");
    }

    @Test
    void usernameOrNull_미인증이면_null() {
        assertThat(provider.usernameOrNull()).isNull();
    }

    @Test
    void isSuperAdmin_ROLE_SUPER_ADMIN_보유시_true() {
        authenticateAs("admin", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        assertThat(provider.isSuperAdmin()).isTrue();
    }

    @Test
    void isSuperAdmin_일반_관리자면_false() {
        authenticateAs("admin", "ROLE_ADMIN");
        assertThat(provider.isSuperAdmin()).isFalse();
    }

    @Test
    void isSuperAdmin_미인증이면_false() {
        assertThat(provider.isSuperAdmin()).isFalse();
    }
}
