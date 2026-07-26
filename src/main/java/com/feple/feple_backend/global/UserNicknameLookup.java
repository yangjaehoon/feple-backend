package com.feple.feple_backend.global;

import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UserService가 아닌 UserRepository를 직접 쓴다: UserService(→UserAdminService→
 * UserCascadeDeleteService→SongRequestService→여기)로 경유시키면 순환 의존으로
 * ApplicationContext 로딩이 실패한다(실제로 시도 후 확인됨). 이 클래스는 닉네임만
 * 읽는 조회 전용이라, 이 프로젝트에서 조회 전용 크로스도메인 Repository 접근을
 * 허용하는 기존 관례(certification/PostServiceImpl 등)와 같은 선상에 있다.
 */
@Component
@RequiredArgsConstructor
public class UserNicknameLookup {

    public static final String UNKNOWN = "알 수 없음";

    private final UserRepository userRepository;

    public String lookup(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .filter(n -> n != null && !n.isBlank())
                .orElse(UNKNOWN);
    }

    public Map<Long, String> buildMap(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : UNKNOWN
                ));
    }

    public <T> Map<Long, String> buildMap(List<T> items, Function<T, Long> userIdExtractor) {
        List<Long> userIds = items.stream().map(userIdExtractor).distinct().toList();
        return buildMap(userIds);
    }
}
