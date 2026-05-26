package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.FestivalSetlistEntryDto;
import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
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
import com.feple.feple_backend.global.EntityFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

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
        Map<Long, Long> countMap = artistFestivalSongRepository
                .countGroupedBySongForArtist(artistId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return songs.stream()
                .map(song -> {
                    int count = countMap.getOrDefault(song.getId(), 0L).intValue();
                    return SongResponseDto.from(song, count, List.of());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongFestivalDto> getSongFestivals(Long songId) {
        return artistFestivalSongRepository.findBySongIdWithFestival(songId)
                .stream()
                .map(afs -> {
                    var festival = afs.getArtistFestival().getFestival();
                    return SongFestivalDto.builder()
                            .festivalId(festival.getId())
                            .festivalTitle(festival.getTitle())
                            .startDate(festival.getStartDate() != null ? festival.getStartDate().toString() : null)
                            .build();
                })
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
    public SongResponseDto saveSong(Long artistId, SaveSongRequestDto dto) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
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
    public void deleteSong(Long songId) {
        songRepository.deleteById(songId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalSetlistEntryDto> getFestivalSetlist(Long festivalId) {
        List<ArtistFestivalSong> afSongs = artistFestivalSongRepository.findByFestivalIdWithDetails(festivalId);

        Map<Long, List<ArtistFestivalSong>> byArtist = new LinkedHashMap<>();
        for (ArtistFestivalSong afs : afSongs) {
            Long artistId = afs.getArtistFestival().getArtistId();
            byArtist.computeIfAbsent(artistId, k -> new ArrayList<>()).add(afs);
        }

        return byArtist.values().stream()
                .map(group -> {
                    Artist artist = group.get(0).getArtistFestival().getArtist();
                    List<SongResponseDto> songs = group.stream()
                            .map(afs -> SongResponseDto.from(afs.getSong()))
                            .toList();
                    return FestivalSetlistEntryDto.builder()
                            .artistId(artist.getId())
                            .artistName(artist.getName())
                            .profileImageUrl(fileStorageService.buildUrl(artist.getProfileImageKey()))
                            .songs(songs)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistFestivalSong> getSetlist(Long artistFestivalId) {
        return artistFestivalSongRepository.findByArtistFestivalId(artistFestivalId);
    }

    @Override
    @Transactional
    public void saveSetlist(Long artistFestivalId, Set<Long> songIds) {
        ArtistFestival artistFestival = EntityFinder.getOrThrow(
                artistFestivalRepository::findById, artistFestivalId, "아티스트 페스티벌");

        artistFestivalSongRepository.deleteByArtistFestivalId(artistFestivalId);

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
