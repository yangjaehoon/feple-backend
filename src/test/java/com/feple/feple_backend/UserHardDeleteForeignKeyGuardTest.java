package com.feple.feple_backend;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.s3.S3Template;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * users(id)를 FK로 참조하는 모든 테이블은 회원 완전 삭제(UserAdminServiceImpl.hardDeleteUser →
 * UserCascadeDeleteService.hardDelete) 시 반드시 아래 셋 중 하나로 처리돼야 users 행 물리 삭제가
 * FK 위반 없이 통과한다:
 * <ol>
 *   <li>DB가 ON DELETE CASCADE / SET NULL로 자동 정리 (예: user_block, lineup_change_requests)</li>
 *   <li>UserCascadeDeleteService가 앱 레벨에서 먼저 삭제 (removeAllActivity + hardDelete 잔여 참조 정리)</li>
 *   <li>hardDeleteUser 선조건이 해당 데이터가 있는 계정을 거부 (post/comment 작성, 갤러리 사진 업로드)</li>
 * </ol>
 *
 * <p>Festival·Artist는 소프트 삭제 전용이라 물리 삭제되는 부모 행은 users 하나뿐이다.
 * 새 테이블이 users를 FK로 참조하면서 {@link #HANDLED}에 없으면 이 테스트가 실패한다 —
 * hardDelete에 정리 로직을 넣거나 선조건으로 거부한 뒤 그 테이블을 HANDLED에 등록할 것.
 * (전용 MySQL 컨테이너에 Flyway를 전부 적용한 실제 스키마로 검증한다.)
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserHardDeleteForeignKeyGuardTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void enableFlyway(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @MockitoBean
    S3Template s3Template;

    @Autowired
    DataSource dataSource;

    // RESTRICT FK로 users를 참조하지만 앱 레벨 삭제 또는 hardDeleteUser 선조건으로 처리되는 테이블.
    // ON DELETE CASCADE / SET NULL인 FK는 DB가 알아서 정리하므로 이 목록과 무관하게 통과한다.
    private static final Set<String> HANDLED = Set.of(
            "refresh_tokens",                 // refreshTokenService.revokeAll
            "user_device_tokens",             // userDeviceTokenRepository.deleteByUserId
            "festival_like",                  // festivalLikeService.removeAllByUser
            "festival_attendance",            // festivalAttendanceService.removeAllByUser
            "artist_follow",                  // artistFollowService.removeAllByUser
            "post_like",                      // postCascadeService.removePostActivityByUser
            "post_scrap",                     // postCascadeService.removePostActivityByUser
            "post_report",                    // postReportRepository.deleteByReporterId
            "post",                           // 선조건: 작성 게시글 0
            "post_draft",                     // postDraftRepository.deleteByUserId
            "comment",                        // 선조건: 작성 댓글 0 / mentioned_user는 clearMentionsByUserId
            "comment_like",                   // commentService.removeLikesByUser
            "comment_report",                 // commentReportRepository.deleteByReporterId
            "artist_photos",                  // 선조건: 업로드 갤러리 사진 0
            "artist_photo_likes",             // artistGalleryPhotoService.removeByUser
            "artist_photo_report",            // artistGalleryPhotoReportRepository.deleteByReporterId
            "festival_certification",         // certificationService.removeAllByUser
            "festival_diary",                 // festivalDiaryService.removeAllByUser
            "festival_suggestion",            // festivalSuggestionService.removeAllByUser
            "artist_suggestion",              // artistSuggestionService.removeAllByUser
            "song_request",                   // songRequestService.removeAllByUser
            "notifications",                  // notificationQueryService.deleteAll
            "notification_preferences",       // notificationPreferenceService.removeAllByUser
            "user_access_log",                // userAccessLogRepository.deleteByUserId
            "user_block",                     // userBlockService.removeAllByUser (+ ON DELETE CASCADE)
            "user_point_log",                 // userPointLogRepository.deleteByUserId
            "user_report"                     // userReportRepository.deleteByUserInvolved
    );

    private record ForeignKey(String table, String column, String deleteRule) {}

    @Test
    void users를_참조하는_모든_FK가_hardDelete에서_처리된다() throws Exception {
        List<ForeignKey> foreignKeys = new ArrayList<>();
        String sql = """
                SELECT k.TABLE_NAME, k.COLUMN_NAME, r.DELETE_RULE
                FROM information_schema.KEY_COLUMN_USAGE k
                JOIN information_schema.REFERENTIAL_CONSTRAINTS r
                  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
                 AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
                WHERE k.TABLE_SCHEMA = DATABASE()
                  AND k.REFERENCED_TABLE_NAME = 'users'
                """;
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                foreignKeys.add(new ForeignKey(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }

        assertThat(foreignKeys)
                .as("users를 참조하는 FK가 하나도 조회되지 않음 — 쿼리 또는 스키마 확인")
                .isNotEmpty();

        List<String> unhandled = foreignKeys.stream()
                .filter(fk -> !"CASCADE".equals(fk.deleteRule()) && !"SET NULL".equals(fk.deleteRule()))
                .map(ForeignKey::table)
                .filter(table -> !HANDLED.contains(table))
                .distinct()
                .sorted()
                .toList();

        assertThat(unhandled)
                .as("users를 RESTRICT FK로 참조하지만 UserCascadeDeleteService.hardDelete 또는 "
                        + "hardDeleteUser 선조건이 정리하지 않는 테이블. 정리 로직/선조건을 추가하고 HANDLED에 등록할 것.")
                .isEmpty();

        List<String> stale = HANDLED.stream()
                .filter(table -> foreignKeys.stream().noneMatch(fk -> fk.table().equals(table)))
                .sorted()
                .toList();

        assertThat(stale)
                .as("HANDLED에 있으나 실제 스키마에는 users FK가 없는 항목 — 테이블명 오타 또는 제거된 테이블")
                .isEmpty();
    }
}
