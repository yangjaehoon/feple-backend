package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class UserCascadeDeleteService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final ArtistProfileImageLikeRepository artistImageLikeRepository;
    private final ArtistProfileImageRepository artistImageRepository;
    private final FileStorageService fileStorageService;

    public void delete(User user) {
        Long id = user.getId();
        String profileImageKey = user.getProfileImageUrl();

        // 인증 세션 무효화
        refreshTokenRepository.deleteByUserId(id);
        userDeviceTokenRepository.deleteByUserId(id);

        // 소셜 활동 데이터 삭제
        festivalLikeRepository.deleteByUserId(id);
        artistFollowRepository.deleteByUserId(id);

        notificationRepository.deleteByUserId(id);
        notificationPreferenceRepository.deleteByUserId(id);
        certificationRepository.deleteByUserId(id);

        artistImageLikeRepository.deleteByUserId(id);
        artistImageRepository.nullifyUploaderByUserId(id);

        // S3 프로필 이미지 삭제
        fileStorageService.deleteFile(profileImageKey);

        // 게시글·댓글은 익명 처리 후 유지 (작성자 닉네임은 "(탈퇴한 사용자)"로 표시됨)
        user.softDelete();
    }
}
