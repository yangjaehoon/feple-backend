package com.feple.feple_backend.search.repository;

import com.feple.feple_backend.search.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query(value = """
            SELECT keyword, COUNT(*) AS cnt
            FROM search_log
            WHERE created_at >= :since
            GROUP BY keyword
            ORDER BY cnt DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<Object[]> findTopKeywordsSince(@Param("since") LocalDateTime since, @Param("lim") int lim);

    @Modifying
    @Query("DELETE FROM SearchLog sl WHERE sl.createdAt < :cutoff")
    void deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
