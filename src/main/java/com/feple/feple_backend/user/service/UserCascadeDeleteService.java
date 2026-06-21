package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.photo.service.ArtistProfileImageLikeService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.service.PostCascadeService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class UserCascadeDeleteService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    private final FestivalLikeService festivalLikeService;
    private final FestivalAttendanceService festivalAttendanceService;
    private final ArtistFollowService artistFollowService;
    private final PostCascadeService postCascadeService;
    private final CommentService commentService;
    private final ArtistGalleryPhotoService artistGalleryPhotoService;
    private final ArtistProfileImageLikeService artistProfileImageLikeService;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final SongRequestRepository songRequestRepository;
    private final ArtistSuggestionRepository artistSuggestionRepository;

    private final FileStorageService fileStorageService;

    public void delete(User user) {
        Long id = user.getId();
        String profileImageKey = user.getProfileImageUrl();

        // 인증 세션 무효화
        refreshTokenRepository.deleteByUserId(id);
        userDeviceTokenRepository.deleteByUserId(id);

        // 소셜 활동 데이터 삭제 — 각 도메인 서비스에 위임하여 카운터 정합성 보장
        festivalLikeService.removeAllByUser(id);
        festivalAttendanceService.removeAllByUser(id);
        artistFollowService.removeAllByUser(id);
        postCascadeService.removePostActivityByUser(id);
        commentService.removeLikesByUser(id);
        artistProfileImageLikeService.removeByUser(id);
        artistGalleryPhotoService.removeByUser(id);

        notificationRepository.deleteByUserId(id);
        notificationPreferenceRepository.deleteByUserId(id);
        certificationRepository.deleteByUserId(id);
        songRequestRepository.deleteByUserId(id);
        artistSuggestionRepository.deleteByUserId(id);

        // 게시글·댓글은 익명 처리 후 유지 (작성자 닉네임은 "(탈퇴한 사용자)"로 표시됨)
        user.softDelete();

        fileStorageService.deleteFileAfterCommit(profileImageKey);
    }
}
