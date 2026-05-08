package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCascadeDeleteService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final ArtistProfileImageLikeRepository artistImageLikeRepository;
    private final ArtistProfileImageRepository artistImageRepository;
    private final FileStorageService fileStorageService;

    public void delete(User user) {
        Long id = user.getId();
        String profileImageKey = user.getProfileImageUrl();

        commentRepository.deleteAll(commentRepository.findByUser(user));

        List<Post> userPosts = postRepository.findByUser(user);
        for (Post post : userPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }
        postLikeRepository.deleteByUser(user);
        postRepository.deleteAll(userPosts);

        festivalLikeRepository.deleteAll(festivalLikeRepository.findByUserId(id));
        artistFollowRepository.deleteAll(artistFollowRepository.findByUserId(id));

        notificationRepository.deleteByUserId(id);
        userDeviceTokenRepository.deleteAll(userDeviceTokenRepository.findByUserId(id));
        certificationRepository.deleteByUserId(id);

        artistImageLikeRepository.deleteByUserId(id);
        artistImageRepository.nullifyUploaderByUserId(id);

        userRepository.delete(user);

        fileStorageService.deleteFile(profileImageKey);
    }
}
