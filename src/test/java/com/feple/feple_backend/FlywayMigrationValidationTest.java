package com.feple.feple_backend;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.s3.S3Template;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

// 나머지 테스트는 flyway.enabled=false + ddl-auto=create-drop(H2)라 Flyway 마이그레이션이
// 실제로 적용된 적이 없어, 엔티티만 추가하고 마이그레이션을 빠뜨려도 CI가 잡지 못했다
// (V40__create_timetable_entry_member.sql 누락 사고). 실제 MySQL에 전체 마이그레이션을
// 적용한 뒤 ddl-auto=validate로 모든 엔티티 매핑을 대조해 이 종류의 누락을 잡는다.
// 실 MySQL 스키마가 필요한 다른 검증(FK 무결성 등)도 컨테이너·컨텍스트를 재사용하도록 여기 모은다.
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FlywayMigrationValidationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideSchemaManagement(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Hibernate의 자동 dialect 추론이 MySQL 커넥션에서 information_schema.SEQUENCES를
        // 조회하는 경로로 빠지는 경우가 있어(MySQL은 시퀀스 미지원) 명시적으로 고정한다.
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @MockitoBean
    S3Template s3Template;

    @Autowired
    DataSource dataSource;

    @Test
    void 플라이웨이_마이그레이션_전체가_엔티티_매핑과_일치한다() {
        // 컨텍스트 로딩 자체가 검증: Flyway가 실제 MySQL에 모든 마이그레이션을 에러 없이
        // 적용하고, Hibernate가 ddl-auto=validate로 모든 @Entity를 그 스키마와 대조한다.
    }

    // ── 회원 완전 삭제(hardDelete) FK 가드 ──────────────────────────────────────
    //
    // users(id)를 FK로 참조하는 모든 테이블은 UserAdminServiceImpl.hardDeleteUser →
    // UserCascadeDeleteService.hardDelete 시 아래 셋 중 하나로 처리돼야 users 행 물리 삭제가
    // FK 위반 없이 통과한다:
    //   1) DB가 ON DELETE CASCADE / SET NULL로 자동 정리 (예: user_block, lineup_change_requests)
    //   2) 앱 레벨에서 먼저 삭제 (removeAllActivity + hardDelete 잔여 참조 정리)
    //   3) hardDeleteUser 선조건이 해당 데이터가 있는 계정을 거부 (post/comment 작성, 갤러리 사진 업로드)
    // Festival·Artist는 소프트 삭제 전용이라 물리 삭제되는 부모 행은 users 하나뿐이다.
    // 새 테이블이 users를 FK로 참조하면서 HANDLED에 없으면 이 테스트가 실패한다 — hardDelete에
    // 정리 로직을 넣거나 선조건으로 거부한 뒤 그 테이블을 HANDLED에 등록할 것.

    // RESTRICT FK로 users를 참조하지만 앱 레벨 삭제 또는 hardDeleteUser 선조건으로 처리되는 테이블.
    // ON DELETE CASCADE / SET NULL인 FK는 DB가 알아서 정리하므로 이 목록과 무관하게 통과한다.
    private static final Set<String> USERS_FK_HANDLED_ON_HARD_DELETE = Set.of(
            "refresh_tokens",                 // refreshTokenService.revokeAll
            "user_device_tokens",             // userDeviceTokenRepository.deleteByUserId
            "festival_like",                  // festivalLikeService.removeAllByUser
            "festival_attendance",            // festivalAttendanceService.removeAllByUser
            "artist_follow",                  // artistFollowService.removeAllByUser
            "post_like",                      // postCascadeService.removePostActivityByUser
            "post_scrap",                     // postCascadeService.removePostActivityByUser
            "post_report",                    // postCascadeService.removeAuthoredArtifactsByUser + purgeAuthoredPostsByUser
            "post",                           // postCascadeService.purgeAuthoredPostsByUser (물리 삭제)
            "post_draft",                     // postCascadeService.removeAuthoredArtifactsByUser
            "comment",                        // commentService.purgeAuthoredCommentsByUser / mentioned_user는 clearMentionsByUserId
            "comment_like",                   // commentService.removeLikesByUser
            "comment_report",                 // commentReportService.removeReportsByReporter
            "artist_photos",                  // 선조건: 업로드 갤러리 사진 0
            "artist_photo_likes",             // artistGalleryPhotoService.removeByUser
            "artist_photo_report",            // artistGalleryPhotoService.removeReportsByReporter
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

    // post/comment 물리 삭제(hardDelete)가 정리하는 RESTRICT FK 자식 테이블.
    // 자기참조(comment.parent_id)는 ON DELETE SET NULL이라 아래 필터에서 자동 제외된다.
    private static final Set<String> POST_FK_HANDLED_ON_HARD_DELETE = Set.of(
            "post_image",   // PostDeleter (S3 키 정리 후)
            "post_tag",     // PostDeleter
            "post_like",    // PostDeleter
            "post_scrap",   // PostDeleter
            "post_report",  // PostDeleter
            "comment",      // PostCascadeDeleteServiceImpl → CommentService.deleteByPostIds
            "notifications" // PostCascadeDeleteServiceImpl → NotificationQueryService.deleteByPostIds
    );

    private static final Set<String> COMMENT_FK_HANDLED_ON_HARD_DELETE = Set.of(
            "comment_like",   // CommentDeleter.deleteByAuthorId / deleteByPostIds
            "comment_report"  // CommentDeleter.deleteByAuthorId / deleteByPostIds
    );

    private record ForeignKey(String table, String column, String deleteRule) {}

    private List<ForeignKey> foreignKeysReferencing(String referencedTable) throws Exception {
        List<ForeignKey> foreignKeys = new ArrayList<>();
        String sql =
                """
                SELECT k.TABLE_NAME, k.COLUMN_NAME, r.DELETE_RULE
                FROM information_schema.KEY_COLUMN_USAGE k
                JOIN information_schema.REFERENTIAL_CONSTRAINTS r
                  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
                 AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
                WHERE k.TABLE_SCHEMA = DATABASE()
                  AND k.REFERENCED_TABLE_NAME = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, referencedTable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    foreignKeys.add(new ForeignKey(rs.getString(1), rs.getString(2), rs.getString(3)));
                }
            }
        }
        return foreignKeys;
    }

    private void assertAllRestrictFksHandled(String referencedTable, Set<String> handled) throws Exception {
        List<ForeignKey> foreignKeys = foreignKeysReferencing(referencedTable);

        assertThat(foreignKeys)
                .as(referencedTable + "을(를) 참조하는 FK가 하나도 조회되지 않음 — 쿼리 또는 스키마 확인")
                .isNotEmpty();

        List<String> unhandled =
                foreignKeys.stream()
                        .filter(fk -> !"CASCADE".equals(fk.deleteRule()) && !"SET NULL".equals(fk.deleteRule()))
                        .map(ForeignKey::table)
                        .filter(table -> !handled.contains(table))
                        .distinct()
                        .sorted()
                        .toList();

        assertThat(unhandled)
                .as(
                        referencedTable
                                + "을(를) RESTRICT FK로 참조하지만 hardDelete 물리 삭제 경로가 정리하지 않는 테이블. "
                                + "정리 로직을 추가하고 대응 HANDLED 목록에 등록할 것.")
                .isEmpty();

        List<String> stale =
                handled.stream()
                        .filter(table -> foreignKeys.stream().noneMatch(fk -> fk.table().equals(table)))
                        .sorted()
                        .toList();

        assertThat(stale)
                .as("목록에 있으나 실제 스키마에는 " + referencedTable + " FK가 없는 항목 — 테이블명 오타 또는 제거된 테이블")
                .isEmpty();
    }

    @Test
    void users를_참조하는_모든_FK가_hardDelete에서_처리된다() throws Exception {
        assertAllRestrictFksHandled("users", USERS_FK_HANDLED_ON_HARD_DELETE);
    }

    @Test
    void post를_참조하는_모든_FK가_hardDelete_물리삭제에서_처리된다() throws Exception {
        assertAllRestrictFksHandled("post", POST_FK_HANDLED_ON_HARD_DELETE);
    }

    @Test
    void comment를_참조하는_모든_FK가_hardDelete_물리삭제에서_처리된다() throws Exception {
        assertAllRestrictFksHandled("comment", COMMENT_FK_HANDLED_ON_HARD_DELETE);

        // 자기참조 대댓글 FK는 반드시 ON DELETE SET NULL이어야 한다 — RESTRICT면 작성자 댓글을
        // 물리 삭제할 때 다른 유저의 대댓글 때문에 FK 위반으로 hardDelete 전체가 롤백된다.
        assertThat(foreignKeysReferencing("comment"))
                .filteredOn(fk -> "comment".equals(fk.table()) && "parent_id".equals(fk.column()))
                .as("comment.parent_id 자기참조 FK는 ON DELETE SET NULL이어야 함 (V12)")
                .isNotEmpty()
                .allMatch(fk -> "SET NULL".equals(fk.deleteRule()));
    }
}
