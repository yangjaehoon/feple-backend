//package com.feple.feple_backend.config;
//
//import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//
//import static org.springframework.security.config.Customizer.withDefaults;
//
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (POST 테스트 편하게)
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(PathRequest.toH2Console()).permitAll()
//
//                        .anyRequest().permitAll() // 모든 요청 허용
//                )
//                .httpBasic(httpBasic -> httpBasic.disable())
//                .formLogin(form -> form.disable())
//                .headers(headers -> headers
//                        // 프레임 안에 H2 콘솔이 뜰 수 있도록 Frame-Options 비활성화
//                        .frameOptions(frame -> frame.disable())
//                ).httpBasic(withDefaults());
//
//        return http.build();
//    }
//}

package com.feple.feple_backend.config;

// import org.springframework.boot.autoconfigure.security.servlet.PathRequest; // 이 import도 제거
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 모든 요청 허용
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }
}