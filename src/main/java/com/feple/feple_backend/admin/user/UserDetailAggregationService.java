package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.PointService;
import com.feple.feple_backend.user.service.UserAdminService;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PointService pointService;
    private final AdminLogService adminLogService;

    public UserListCountsDto getListCounts(List<Long> userIds) {
        return new UserListCountsDto(
                myPageService.getReportCounts(userIds),
                postAdminService.getPostCountsByUserIds(userIds),
                commentService.getCommentCountsByUserIds(userIds)
        );
    }

    public UserDetailDto getDetail(Long userId) {
        UserResponseDto user = userAdminService.getAdminUser(userId);
        return new UserDetailDto(
                user,
                myPageService.getUserStatsForAdmin(userId),
                buildModerationSummary(userId, user),
                postAdminService.getRecentPostsByUser(userId, RECENT_LIMIT),
                commentService.getRecentCommentsByUser(userId, RECENT_LIMIT),
                myPageService.getLikedFestivals(userId),
                myPageService.getFollowedArtists(userId),
                userBlockService.getBlockedUsers(userId),
                certificationAdminService.getByUserId(userId),
                pointService.getRecentPointLogs(userId, RECENT_LIMIT)
        );
    }

    private UserModerationSummaryDto buildModerationSummary(Long userId, UserResponseDto user) {
        long joinedDaysAgo = user.getCreatedAt() != null
                ? ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now())
                : -1L; // 가입일 불명 — 신규 계정으로 오인되지 않도록 음수 sentinel
        return new UserModerationSummaryDto(
                postAdminService.countBlindedPostsByUser(userId),
                adminLogService.countUserBans(userId),
                joinedDaysAgo);
    }
}
