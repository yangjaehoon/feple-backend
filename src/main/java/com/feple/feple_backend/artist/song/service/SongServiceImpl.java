package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.entity.Song;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.global.EntityFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService, SongAdminService {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final YoutubeSearchService youtubeSearchService;

    @Override
    @Transactional(readOnly = true)
    public List<SongResponseDto> getSongsByArtistId(Long artistId) {
        return songRepository.findByArtistIdOrderByIdDesc(artistId)
                .stream()
                .map(SongResponseDto::from)
                .toList();
    }

    @Override
    public List<YoutubeVideoDto> searchYoutube(String query) {
        return youtubeSearchService.search(query);
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
}
