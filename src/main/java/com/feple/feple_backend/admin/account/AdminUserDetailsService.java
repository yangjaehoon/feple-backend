package com.feple.feple_backend.admin.account;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminAccountRepository adminAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminAccount account = adminAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("관리자 계정을 찾을 수 없습니다: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (account.getRole() == AdminRole.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            for (AdminPermission permission : AdminPermission.values()) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name()));
            }
        } else {
            for (AdminPermission permission : account.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name()));
            }
        }

        return new User(
                account.getUsername(),
                account.getPassword(),
                account.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }
}
