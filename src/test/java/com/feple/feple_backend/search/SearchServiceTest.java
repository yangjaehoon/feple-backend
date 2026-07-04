package com.feple.feple_backend.search;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostSearchService;
import com.feple.feple_backend.search.dto.SearchResultDto;
import com.feple.feple_backend.search.dto.SuggestionDto;
import com.feple.feple_backend.search.entity.SearchLog;
import com.feple.feple_backend.search.repository.SearchLogRepository;
import com.feple.feple_backend.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock ArtistService artistService;
    @Mock FestivalService festivalService;
    @Mock PostSearchService postSearchService;
    @Mock SearchLogRepository searchLogRepository;

    @InjectMocks SearchService searchService;

    @Test
    void search_null_키워드_빈_결과() {
        SearchResultDto result = searchService.search(null);

        assertThat(result.artists()).isEmpty();
        assertThat(result.festivals()).isEmpty();
        assertThat(result.posts()).isEmpty();
        then(searchLogRepository).should(never()).save(any());
    }

    @Test
    void search_공백_키워드_빈_결과() {
        SearchResultDto result = searchService.search("   ");

        assertThat(result.artists()).isEmpty();
        assertThat(result.festivals()).isEmpty();
        assertThat(result.posts()).isEmpty();
        then(searchLogRepository).should(never()).save(any());
    }

    @Test
    void search_성공_로그_저장_후_집계() {
        ArtistResponseDto artist = mock(ArtistResponseDto.class);
        given(artistService.searchArtists("test")).willReturn(List.of(artist));
        given(festivalService.searchFestivals("test")).willReturn(List.of());
        given(postSearchService.searchPosts("test", null)).willReturn(List.of());

        SearchResultDto result = searchService.search("test");

        then(searchLogRepository).should().save(any(SearchLog.class));
        assertThat(result.artists()).hasSize(1);
    }

    @Test
    void search_결과_10개_제한() {
        List<ArtistResponseDto> artists = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            artists.add(mock(ArtistResponseDto.class));
        }
        given(artistService.searchArtists("test")).willReturn(artists);
        given(festivalService.searchFestivals("test")).willReturn(List.of());
        given(postSearchService.searchPosts("test", null)).willReturn(List.of());

        SearchResultDto result = searchService.search("test");

        assertThat(result.artists()).hasSize(10);
    }

    @Test
    void getSuggestions_null_키워드_빈_리스트() {
        List<SuggestionDto> result = searchService.getSuggestions(null);

        assertThat(result).isEmpty();
    }

    @Test
    void getSuggestions_성공_아티스트_페스티벌_매핑() {
        ArtistResponseDto artist = mock(ArtistResponseDto.class);
        given(artist.getName()).willReturn("아티스트1");
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getTitle()).willReturn("페스티벌1");

        given(artistService.searchArtists("kw")).willReturn(List.of(artist));
        given(festivalService.searchFestivals("kw")).willReturn(List.of(festival));

        List<SuggestionDto> result = searchService.getSuggestions("kw");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo("artist");
        assertThat(result.get(1).type()).isEqualTo("festival");
    }

    @Test
    void getSuggestions_결과_5개_제한() {
        List<ArtistResponseDto> artists = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ArtistResponseDto a = mock(ArtistResponseDto.class);
            lenient().when(a.getName()).thenReturn("아티스트" + i);
            artists.add(a);
        }
        List<FestivalResponseDto> festivals = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            FestivalResponseDto f = mock(FestivalResponseDto.class);
            lenient().when(f.getTitle()).thenReturn("페스티벌" + i);
            festivals.add(f);
        }
        given(artistService.searchArtists("kw")).willReturn(artists);
        given(festivalService.searchFestivals("kw")).willReturn(festivals);

        List<SuggestionDto> result = searchService.getSuggestions("kw");

        assertThat(result).hasSize(10);
    }
}
