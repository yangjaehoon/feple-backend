package com.feple.feple_backend.user.service;

import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.NicknameContentValidator;
import com.feple.feple_backend.user.NicknameValidator;
import com.feple.feple_backend.user.dto.NicknameAvailabilityResponse;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserCascadeDeleteService cascadeDeleteService;
    private final BadWordValidator badWordValidator;
    private final NicknameContentValidator nicknameContentValidator;

    @Override
    @Transactional(readOnly = true)
    public NicknameAvailabilityResponse checkNicknameAvailable(String nickname, Long excludeUserId) {
        try {
            NicknameValidator.validate(nickname);
        } catch (IllegalArgumentException e) {
            return NicknameAvailabilityResponse.invalidFormat(e.getMessage());
        }
        for (NicknameContentValidator.Step step : nicknameContentValidator.steps()) {
            try {
                step.validate().accept(nickname);
            } catch (IllegalArgumentException e) {
                return NicknameAvailabilityResponse.rejected(step.failureCode(), e.getMessage());
            }
        }
        boolean taken = excludeUserId != null
                ? userRepository.existsByNicknameAndIdNot(nickname.trim(), excludeUserId)
                : userRepository.existsByNickname(nickname.trim());
        if (taken) {
            return NicknameAvailabilityResponse.duplicate();
        }
        return NicknameAvailabilityResponse.ok();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        return toUserDto(user);
    }

    @Override
    public void updateNickname(@NonNull Long id, String nickname) {
        NicknameValidator.validate(nickname);
        badWordValidator.validateField("nickname", nickname);
        nicknameContentValidator.validateArtistAndRestriction(nickname);
        if (userRepository.existsByNicknameAndIdNot(nickname.trim(), id)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        if (!user.canChangeNickname()) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), user.nextNicknameChangeAt()) + 1;
            throw new IllegalArgumentException(
                    "닉네임은 " + User.NICKNAME_COOLDOWN_DAYS + "일에 한 번만 변경할 수 있습니다. " + daysLeft + "일 후에 변경 가능합니다.");
        }
        user.changeNickname(nickname.trim());
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }
    }

    @Override
    public void updateBio(@NonNull Long id, String bio) {
        if (bio != null) badWordValidator.validateField("bio", bio);
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        user.updateBio(bio);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateProfileImage(@NonNull Long id, MultipartFile file) {
        try {
            // S3 업로드는 커넥션 점유 없이 수행; 완료 후 별도 트랜잭션으로 DB 반영
            User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
            String url = fileStorageService.storeUserProfile(file, user.getNickname());
            user.changeProfileImage(url);
            userRepository.save(user);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }

    @Override
    public void deleteUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        cascadeDeleteService.delete(user);
    }

    @Override
    public UserResponseDto toUserDto(User user) {
        return UserResponseMapper.toUserDto(user, fileStorageService);
    }

    @Override
    public Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
