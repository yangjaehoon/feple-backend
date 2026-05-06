package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistCascadeDeleteService {

    private final ArtistRepository artistRepository;
    private final ArtistProfileImageRepository artistImageRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void delete(Artist artist) {
        String profileImageKey = artist.getProfileImageKey();

        // 아티스트 이미지 삭제 (S3 + DB, ArtistProfileImageLike는 cascade)
        List<ArtistProfileImage> images = artistImageRepository.findByArtist(artist);
        images.forEach(img -> fileStorageService.deleteFile(img.getImageUrl()));
        artistImageRepository.deleteAll(images);

        // 아티스트-페스티벌 연결, 팔로우 삭제
        artistFestivalRepository.deleteByArtistId(artist.getId());
        artistFollowRepository.deleteAll(artistFollowRepository.findByArtistId(artist.getId()));

        // 게시글 삭제 (PostLike 먼저, Comment는 Post cascade)
        List<Post> artistPosts = postRepository.findByArtist(artist);
        for (Post post : artistPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }
        postRepository.deleteAll(artistPosts);

        artistRepository.deleteById(artist.getId());
        fileStorageService.deleteFile(profileImageKey);
    }
}
