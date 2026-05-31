package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ArtistService {
    Long createArtist(ArtistRequestDto dto);
    List<ArtistResponseDto> getAllArtists();
    List<ArtistResponseDto> getAllArtistsSortedByName();
    List<ArtistResponseDto> searchArtists(String keyword);
    List<ArtistResponseDto> getAdminArtistList(String sort, String keyword, ArtistGenre genre);
    ArtistResponseDto getArtistById(Long id);
    Page<ArtistResponseDto> getArtistsPage(int page, int size);
    ArtistRequestDto getArtistForEdit(Long id);
    void updateArtist(Long id, ArtistRequestDto dto);
    List<ArtistResponseDto> getTopArtists(int limit);
    void updateArtistPhoto(Long id, String imageKey);
    String uploadProfile(MultipartFile file, String artistName) throws IOException;
    void batchUpdateNameEn(List<Long> ids, List<String> nameEns);
    void deleteArtist(Long id);
    List<ArtistResponseDto> getFollowedArtists(Long userId);
}
