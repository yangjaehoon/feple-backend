package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCascadeDeleteServiceImpl implements PostCascadeDeleteService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final PostReportRepository postReportRepository;
    private final NotificationQueryService notificationQueryService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void deletePostsByArtist(Artist artist) {
        postRepository.nullifyArtistIdForSoftDeleted(artist.getId());
        deletePostLikesAndPosts(postRepository.findByArtist(artist));
    }

    @Override
    @Transactional
    public void deletePostsByFestival(Festival festival) {
        postRepository.nullifyFestivalIdForSoftDeleted(festival.getId());
        deletePostLikesAndPosts(postRepository.findByFestival(festival));
    }

    @Override
    @Transactional
    public void removePostActivityByUser(Long userId) {
        postLikeRepository.decrementPostLikeCountByUserId(userId);
        postLikeRepository.deleteByUserId(userId);
        postScrapRepository.decrementPostScrapCountByUserId(userId);
        postScrapRepository.deleteByUserId(userId);
    }

    private void deletePostLikesAndPosts(List<Post> posts) {
        if (posts.isEmpty()) return;
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        // 자식 → 부모 순서로 삭제 (FK 제약)
        commentService.deleteByPostIds(postIds);
        postLikeRepository.deleteByPostIds(postIds);
        postScrapRepository.deleteByPostIds(postIds);
        postReportRepository.deleteByPostIds(postIds);
        notificationQueryService.removeAllByPostIds(postIds);
        // deleteAllByIdInBatch는 @SQLDelete(soft delete)를 우회하는 하드 삭제라, row와 함께
        // image_url 참조도 영구히 사라진다 — 삭제 전에 S3 이미지를 커밋 후 정리해야 고아 객체가 안 남는다.
        posts.stream()
                .map(Post::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .forEach(fileStorageService::deleteFileAfterCommit);
        postRepository.deleteAllByIdInBatch(postIds);
    }
}
