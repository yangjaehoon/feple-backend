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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.global.QueryResultMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService, SongAdminService {

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
        Song song = Song.builder()
                .title(dto.getTitle())
                .youtubeVideoId(dto.getYoutubeVideoId())
                .thumbnailUrl(dto.getThumbnailUrl())
                .artist(artist)
                .build();
        return SongResponseDto.from(songRepository.save(song));
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

        List<ArtistFestivalSong> afSongs =
                artistFestivalSongRepository.findByFestivalIdWithDetails(festivalId);

        Map<Long, List<SongResponseDto>> songsByAfId = new HashMap<>();
        for (ArtistFestivalSong afs : afSongs) {
            songsByAfId.computeIfAbsent(afs.getArtistFestivalId(), k -> new ArrayList<>())
                       .add(SongResponseDto.from(afs.getSong()));
        }

        return artistFestivals.stream()
                .map(af -> FestivalSetlistEntryDto.builder()
                        .artistFestivalId(af.getId())
                        .artistId(af.getArtistId())
                        .artistName(af.getArtistName())
                        .artistNameEn(af.getArtistNameEn())
                        .profileImageUrl(fileStorageService.buildUrl(af.getArtistProfileImageKey()))
                        .songs(songsByAfId.getOrDefault(af.getId(), List.of()))
                        .build())
                .toList();
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
