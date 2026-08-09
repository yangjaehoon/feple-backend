package com.feple.feple_backend.artistfestival.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtistFestivalResponseDto {
    private Long artistFestivalId;

    private Long artistId;
    private String artistName;
    private String artistNameEn;
    private String artistGenre;
    private String profileImageUrl;

    private Integer lineupOrder;
    private String stageName;
    /** 확정 출연일(ArtistFestival.performanceDate) — 없으면 performanceDates의 첫 날짜로 대체된 값. 화면에 대표로 표시할 단일 날짜. */
    private String performanceDate;
    /** 타임테이블에 등록된 모든 출연일(여러 날 공연/페스티벌 데이 전체) — performanceDate의 대체 산출 근거이자, 여러 날짜를 모두 보여줘야 할 때 사용. */
    private List<String> performanceDates;

}
