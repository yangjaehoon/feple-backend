package com.feple.feple_backend.admin;

import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public UserListCountsModel getListCounts(List<Long> userIds) {
        return new UserListCountsModel(
                myPageService.getReportCounts(userIds),
                postAdminService.getPostCountsByUserIds(userIds),
                commentService.getCommentCountsByUserIds(userIds)
        );
    }

    public UserDetailModel getDetail(Long userId) {
        return new UserDetailModel(
                userAdminService.getAdminUser(userId),
                myPageService.getUserStats(userId),
                postAdminService.getRecentPostsByUser(userId, RECENT_LIMIT),
                commentService.getRecentCommentsByUser(userId, RECENT_LIMIT),
                myPageService.getLikedFestivals(userId),
                myPageService.getFollowedArtists(userId)
        );
    }
}
