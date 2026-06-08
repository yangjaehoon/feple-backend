package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


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
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final SongRequestRepository songRequestRepository;
    private final ArtistSuggestionRepository artistSuggestionRepository;
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
        postLikeRepository.deleteByUser(user);
        commentLikeRepository.deleteByUserId(id);
        postScrapRepository.deleteByUser(user);

        notificationRepository.deleteByUserId(id);
        notificationPreferenceRepository.deleteByUserId(id);
        certificationRepository.deleteByUserId(id);
        songRequestRepository.deleteByUserId(id);
        artistSuggestionRepository.deleteByUserId(id);

        artistImageLikeRepository.deleteByUserId(id);
        artistImageRepository.nullifyUploaderByUserId(id);

        // 게시글·댓글은 익명 처리 후 유지 (작성자 닉네임은 "(탈퇴한 사용자)"로 표시됨)
        user.softDelete();

        // S3 삭제는 커밋 후 실행 — 롤백 시 파일이 남아 있어야 하고, 커넥션을 불필요하게 점유하지 않도록 함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.deleteFile(profileImageKey);
            }
        });
    }
}
