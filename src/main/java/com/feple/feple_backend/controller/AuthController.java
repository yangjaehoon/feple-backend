package com.feple.feple_backend.controller;
import com.feple.feple_backend.domain.user.User;
import com.feple.feple_backend.dto.user.KakaoUserResponse;
import com.feple.feple_backend.dto.user.UserResponseDto;
import com.feple.feple_backend.service.KakaoAuthService;
import com.feple.feple_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final KakaoAuthService kakaoAuthService;
    private final UserService userService;

    @PostMapping("/kakao")
    public ResponseEntity<UserResponseDto> kakaoLogin(@RequestHeader("Authorization") String bearerToken) {
        String accessToken = bearerToken.replace("Bearer ", "");

        // Kakao API로 사용자 정보 조회
        KakaoUserResponse kakaoUser = kakaoAuthService.getKakaoUserInfo(accessToken);

        // DB에 유저가 없으면 생성하고, 있으면 가져오기
        User user = userService.registerOrLogin(kakaoUser);

        UserResponseDto dto = UserResponseDto.from(user);

        // 로그인 성공 응답 (토큰 발급이 있다면 여기서 같이 처리)
        return ResponseEntity.ok(dto);

    }
}
