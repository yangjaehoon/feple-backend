package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;

public class V16__add_festival_performance_indexes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {
            addIndexIfNotExists(stmt, "festival", "idx_festival_end_date",
                    "ALTER TABLE `festival` ADD INDEX `idx_festival_end_date` (`end_date`)");
            addIndexIfNotExists(stmt, "festival", "idx_festival_region",
                    "ALTER TABLE `festival` ADD INDEX `idx_festival_region` (`region`)");
            addIndexIfNotExists(stmt, "festival", "idx_festival_start_like",
                    "ALTER TABLE `festival` ADD INDEX `idx_festival_start_like` (`start_date`, `like_count` DESC)");
            addIndexIfNotExists(stmt, "festival_genres", "idx_festival_genres_genre",
                    "ALTER TABLE `festival_genres` ADD INDEX `idx_festival_genres_genre` (`genres`)");
        }
    }

    private void addIndexIfNotExists(Statement stmt, String tableName, String indexName, String ddl) throws Exception {
        String check = "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' " +
                "AND index_name = '" + indexName + "'";
        try (ResultSet rs = stmt.executeQuery(check)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.execute(ddl);
            }
        }
    }
}
