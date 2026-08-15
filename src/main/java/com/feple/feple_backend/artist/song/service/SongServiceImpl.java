package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.FestivalSetlistEntryDto;
import com.feple.feple_backend.artist.song.dto.SaveSongDto;
import com.feple.feple_backend.artist.song.dto.SongFestivalDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;
import com.feple.feple_backend.artist.song.entity.Song;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.QueryResultMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService, SongAdminService, SetlistAdminService {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFestivalSongRepository artistFestivalSongRepository;
    private final YoutubeSearchService youtubeSearchService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<SongResponseDto> getSongsByArtistId(Long artistId) {
        List<Song> songs = songRepository.findByArtistIdOrderByIdDesc(artistId);
        if (songs.isEmpty()) return List.of();

        // 한 번의 쿼리로 전체 카운트 조회
        Map<Long, Long> countMap = QueryResultMapper.toLongMap(
                artistFestivalSongRepository.countGroupedBySongForArtist(artistId));

        return songs.stream()
                .map(song -> {
                    int count = countMap.getOrDefault(song.getId(), 0L).intValue();
                    return SongResponseDto.from(song, count, List.of());
                })
                .sorted(Comparator.comparingInt(SongResponseDto::getFestivalCount).reversed()
                        .thenComparing(SongResponseDto::getTitle))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongFestivalDto> getSongFestivals(Long songId) {
        return artistFestivalSongRepository.findBySongIdWithFestival(songId)
                .stream()
                .map(afs -> SongFestivalDto.builder()
                            .festivalId(afs.getFestivalId())
                            .festivalTitle(afs.getFestivalTitle())
                            .startDate(afs.getFestivalStartDate() != null ? afs.getFestivalStartDate().toString() : null)
                            .build())
                .toList();
    }

    @Override
    public List<YoutubeVideoDto> searchYoutube(String artistName, String query) {
        return youtubeSearchService.search(artistName, query);
    }

    @Override
    public Optional<YoutubeVideoDto> fetchVideoByUrl(String videoUrlOrId) {
        return youtubeSearchService.fetchVideoByUrl(videoUrlOrId);
    }

    @Override
    @Transactional
    public SongResponseDto saveSong(Long artistId, SaveSongDto dto) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        if (songRepository.existsByYoutubeVideoIdAndArtistId(dto.getYoutubeVideoId(), artistId)) {
            throw new IllegalArgumentException("이미 등록된 곡입니다.");
        }
        return SongResponseDto.from(songRepository.save(buildSong(artist, dto)));
    }

    @Override
    @Transactional
    public Optional<SongResponseDto> saveSongIfAbsent(Long artistId, SaveSongDto dto) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        if (songRepository.existsByYoutubeVideoIdAndArtistId(dto.getYoutubeVideoId(), artistId)) {
            return Optional.empty();
        }
        return Optional.of(SongResponseDto.from(songRepository.save(buildSong(artist, dto))));
    }

    private Song buildSong(Artist artist, SaveSongDto dto) {
        return Song.builder()
                .title(dto.getTitle())
                .youtubeVideoId(dto.getYoutubeVideoId())
                .thumbnailUrl(dto.getThumbnailUrl())
                .artist(artist)
                .build();
    }

    @Override
    @Transactional
    public void deleteSong(Long artistId, Long songId) {
        Song song = EntityLoader.getOrThrow(songRepository::findById, songId, "곡");
        if (!song.getArtistId().equals(artistId)) {
            throw new IllegalArgumentException("해당 아티스트의 곡이 아닙니다.");
        }
        songRepository.delete(song);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalSetlistEntryDto> getFestivalSetlist(Long festivalId) {
        List<ArtistFestival> artistFestivals =
                artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(festivalId);
        if (artistFestivals.isEmpty()) return List.of();

        Map<Long, List<SongResponseDto>> songsByAfId = groupSongsByArtistFestivalId(
                artistFestivalSongRepository.findByFestivalIdWithDetails(festivalId));

        return artistFestivals.stream()
                .map(af -> buildSetlistEntry(af, songsByAfId.get(af.getId())))
                .toList();
    }

    // 관리자가 이 공연의 실제 셋리스트를 등록하지 않았으면, 아티스트가 평소 부르는
    // 곡(공연 횟수 많은 순)으로 대체해 보여준다 — 화면이 항상 비어 보이지 않도록.
    private FestivalSetlistEntryDto buildSetlistEntry(ArtistFestival af, List<SongResponseDto> actualSongs) {
        boolean hasActualSetlist = actualSongs != null && !actualSongs.isEmpty();
        List<SongResponseDto> songs = hasActualSetlist ? actualSongs : getSongsByArtistId(af.getArtistId());
        return FestivalSetlistEntryDto.builder()
                .artistFestivalId(af.getId())
                .artistId(af.getArtistId())
                .artistName(af.getArtistName())
                .artistNameEn(af.getArtistNameEn())
                .profileImageUrl(fileStorageService.buildUrl(af.getArtistProfileImageKey()))
                .songs(songs)
                .predicted(!hasActualSetlist && !songs.isEmpty())
                .build();
    }

    private Map<Long, List<SongResponseDto>> groupSongsByArtistFestivalId(List<ArtistFestivalSong> afSongs) {
        Map<Long, List<SongResponseDto>> songsByAfId = new HashMap<>();
        for (ArtistFestivalSong afs : afSongs) {
            songsByAfId.computeIfAbsent(afs.getArtistFestivalId(), k -> new ArrayList<>())
                       .add(SongResponseDto.from(afs.getSong()));
        }
        return songsByAfId;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getSetlistCounts(List<Long> artistFestivalIds) {
        if (artistFestivalIds.isEmpty()) return Map.of();
        return QueryResultMapper.toIntMap(
                artistFestivalSongRepository.countGroupedByArtistFestivalIds(artistFestivalIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistFestivalSong> getSetlist(Long artistFestivalId) {
        return artistFestivalSongRepository.findByArtistFestivalId(artistFestivalId);
    }

    @Override
    @Transactional
    public void updateSetlist(Long festivalId, Long artistFestivalId, Set<Long> songIds) {
        ArtistFestival artistFestival = EntityLoader.getOrThrow(
                artistFestivalRepository::findById, artistFestivalId, "아티스트 페스티벌");
        if (!artistFestival.getFestivalId().equals(festivalId)) {
            throw new IllegalArgumentException("해당 아티스트는 이 페스티벌에 참여하지 않습니다.");
        }
        doSaveSetlist(artistFestival, songIds);
    }

    @Override
    @Transactional
    public void saveSetlist(Long artistFestivalId, Set<Long> songIds) {
        ArtistFestival artistFestival = EntityLoader.getOrThrow(
                artistFestivalRepository::findById, artistFestivalId, "아티스트 페스티벌");
        doSaveSetlist(artistFestival, songIds);
    }

    private void doSaveSetlist(ArtistFestival artistFestival, Set<Long> songIds) {
        artistFestivalSongRepository.deleteByArtistFestivalId(artistFestival.getId());
        if (songIds == null || songIds.isEmpty()) return;
        List<Song> songs = songRepository.findAllById(songIds);
        List<ArtistFestivalSong> setlist = songs.stream()
                .map(song -> ArtistFestivalSong.builder()
                        .song(song)
                        .artistFestival(artistFestival)
                        .build())
                .toList();
        artistFestivalSongRepository.saveAll(setlist);
    }
}
