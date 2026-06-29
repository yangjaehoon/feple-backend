package com.feple.feple_backend.global;

import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserNicknameResolver {

    public static final String UNKNOWN = "알 수 없음";

    private final UserRepository userRepository;

    public String resolve(Long userId) {
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
