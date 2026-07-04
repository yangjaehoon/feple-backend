package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserAdminService;
import com.feple.feple_backend.userblock.service.UserBlockService;
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
    private final UserBlockService userBlockService;
    private final FestivalCertificationAdminService certificationAdminService;

    public UserListCountsDto getListCounts(List<Long> userIds) {
        return new UserListCountsDto(
                myPageService.getReportCounts(userIds),
                postAdminService.getPostCountsByUserIds(userIds),
                commentService.getCommentCountsByUserIds(userIds)
        );
    }

    public UserDetailDto getDetail(Long userId) {
        return new UserDetailDto(
                userAdminService.getAdminUser(userId),
                myPageService.getUserStats(userId),
                postAdminService.getRecentPostsByUser(userId, RECENT_LIMIT),
                commentService.getRecentCommentsByUser(userId, RECENT_LIMIT),
                myPageService.getLikedFestivals(userId),
                myPageService.getFollowedArtists(userId),
                userBlockService.getBlockedUsers(userId),
                certificationAdminService.getByUserId(userId)
        );
    }
}
