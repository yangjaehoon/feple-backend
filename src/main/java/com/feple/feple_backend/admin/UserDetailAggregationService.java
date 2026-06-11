package com.feple.feple_backend.admin;

import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailAggregationService {

    private static final int RECENT_LIMIT = 10;

    private final UserAdminService userAdminService;
    private final MyPageService myPageService;
    private final CommentService commentService;
    private final PostAdminService postAdminService;

    public void populateListCountsModel(List<Long> userIds, Model model) {
        model.addAttribute("reportCounts",  myPageService.getReportCounts(userIds));
        model.addAttribute("postCounts",    postAdminService.getPostCountsByUserIds(userIds));
        model.addAttribute("commentCounts", commentService.getCommentCountsByUserIds(userIds));
    }

    public void populateModel(Long userId, Model model) {
        var user            = userAdminService.getAdminUser(userId);
        var stats           = myPageService.getUserStats(userId);
        var recentPosts     = postAdminService.getRecentPostsByUser(userId, RECENT_LIMIT);
        var likedFestivals  = myPageService.getLikedFestivals(userId);
        var followedArtists = myPageService.getFollowedArtists(userId);
        var recentComments  = commentService.getRecentCommentsByUser(userId, RECENT_LIMIT);

        model.addAttribute("user",            user);
        model.addAttribute("stats",           stats);
        model.addAttribute("recentPosts",     recentPosts);
        model.addAttribute("recentComments",  recentComments);
        model.addAttribute("likedFestivals",  likedFestivals);
        model.addAttribute("followedArtists", followedArtists);
    }
}
