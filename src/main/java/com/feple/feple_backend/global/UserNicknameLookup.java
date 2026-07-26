package com.feple.feple_backend.global;

import com.feple.feple_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserNicknameLookup {

    public static final String UNKNOWN = "알 수 없음";

    private final UserService userService;

    public String lookup(Long userId) {
        return resolveOrUnknown(userService.getNicknamesByIds(List.of(userId)).get(userId));
    }

    public Map<Long, String> buildMap(List<Long> userIds) {
        return userService.getNicknamesByIds(userIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> resolveOrUnknown(e.getValue())));
    }

    public <T> Map<Long, String> buildMap(List<T> items, Function<T, Long> userIdExtractor) {
        List<Long> userIds = items.stream().map(userIdExtractor).distinct().toList();
        return buildMap(userIds);
    }

    private String resolveOrUnknown(String nickname) {
        return (nickname != null && !nickname.isBlank()) ? nickname : UNKNOWN;
    }
}
