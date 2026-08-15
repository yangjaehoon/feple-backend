package com.feple.feple_backend.artist.song.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FestivalSetlistEntryDto {
    private Long artistFestivalId;
    private Long artistId;
    private String artistName;
    private String artistNameEn;
    private String profileImageUrl;
    private List<SongResponseDto> songs;
    /** 관리자가 이 공연의 실제 셋리스트를 등록하지 않아, 아티스트가 평소 부르는 곡으로 대체 표시 중인지 여부 */
    private boolean predicted;
}
