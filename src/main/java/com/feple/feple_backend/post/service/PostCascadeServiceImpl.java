package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.notification.repository.NotificationRepository;
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
public class PostCascadeServiceImpl implements PostCascadeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final PostReportRepository postReportRepository;
    private final NotificationRepository notificationRepository;
    private final CommentService commentService;

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
        commentService.deleteByPostIds(postIds);
        postLikeRepository.deleteByPostIds(postIds);
        postScrapRepository.deleteByPostIds(postIds);
        postReportRepository.deleteByPostIds(postIds);
        notificationRepository.deleteByPostIdIn(postIds);
        postRepository.deleteAllByIdInBatch(postIds);
    }
}
