package com.feple.feple_backend.artistfestival.repository;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistFestivalRepository extends JpaRepository<ArtistFestival, Long> {

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.festival WHERE af.id = :id")
    Optional<ArtistFestival> findByIdWithFestival(@Param("id") Long id);

    @Query("SELECT af FROM ArtistFestival af WHERE af.festival.id = :festivalId")
    List<ArtistFestival> findByFestivalId(@Param("festivalId") Long festivalId);

    // artist JOIN FETCH — getLineup()에서 af.getArtist() 접근 시 N+1 방지
    // 소프트 삭제된 아티스트/페스티벌은 라인업·일정 화면에서 제외 (연결 자체는 남아있어 복구 시 다시 노출됨)
    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.artist a WHERE af.festival.id = :festivalId AND a.deletedAt IS NULL ORDER BY af.lineupOrder ASC")
    List<ArtistFestival> findByFestivalIdOrderByLineupOrderAsc(@Param("festivalId") Long festivalId);

    // 정렬 기준을 festival.startDate가 아닌 실제 노출 날짜(performanceDate 우선)로 맞춘다.
    // ArtistScheduleService가 응답에 performanceDate가 있으면 그 값을 startDate/endDate로 쓰기 때문에,
    // festival.startDate로만 정렬하면 페스티벌 기간이 겹칠 때 화면에 표시되는 날짜 순서와 어긋난다.
    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.festival f WHERE af.artist.id = :artistId AND f.deletedAt IS NULL ORDER BY COALESCE(af.performanceDate, f.startDate) ASC")
    List<ArtistFestival> findByArtistIdOrderByFestivalStartDateAsc(@Param("artistId") Long artistId);

    // festival JOIN FETCH — getArtistSchedule()에서 af.getFestival() 접근 시 N+1 방지
    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.festival f WHERE af.artist.id = :artistId AND f.deletedAt IS NULL ORDER BY af.festival.startDate DESC")
    List<ArtistFestival> findByArtistIdOrderByFestivalStartDateDesc(@Param("artistId") Long artistId);

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.artist a WHERE af.festival.id IN :festivalIds AND a.deletedAt IS NULL ORDER BY af.lineupOrder ASC")
    List<ArtistFestival> findByFestivalIdInWithArtist(@Param("festivalIds") List<Long> festivalIds);

    // findByFestivalIdInWithArtist 결과를 festivalId로 묶는 로직이 ArtistScheduleService(공동출연자 조회)와
    // ArtistFestivalService(타임테이블 완료 여부 계산) 양쪽에서 동일하게 필요해 여기로 추출.
    default Map<Long, List<ArtistFestival>> findByFestivalIdInGroupedByFestivalId(List<Long> festivalIds) {
        return findByFestivalIdInWithArtist(festivalIds).stream()
                .collect(Collectors.groupingBy(ArtistFestival::getFestivalId));
    }

    @Query("SELECT af FROM ArtistFestival af JOIN FETCH af.artist WHERE af.festival.id = :festivalId AND af.artist.name = :artistName")
    Optional<ArtistFestival> findByFestivalIdAndArtistName(@Param("festivalId") Long festivalId, @Param("artistName") String artistName);

    @Query("SELECT CASE WHEN COUNT(af) > 0 THEN TRUE ELSE FALSE END FROM ArtistFestival af WHERE af.festival.id = :festivalId AND af.artist.id = :artistId")
    boolean existsByFestivalIdAndArtistId(@Param("festivalId") Long festivalId, @Param("artistId") Long artistId);

    @Query("SELECT CASE WHEN COUNT(af) > 0 THEN TRUE ELSE FALSE END FROM ArtistFestival af WHERE af.id = :id AND af.artist.id = :artistId")
    boolean existsByIdAndArtistId(@Param("id") Long id, @Param("artistId") Long artistId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistFestival af WHERE af.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ArtistFestival af WHERE af.artist.id = :artistId")
    void deleteByArtistId(@Param("artistId") Long artistId);
}
