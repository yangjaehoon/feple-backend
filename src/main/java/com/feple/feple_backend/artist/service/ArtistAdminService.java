package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistAdminListQuery;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ArtistAdminService {
    Long createArtist(ArtistRequestDto dto);
    List<ArtistResponseDto> getAllArtistsSortedByName();
    /** CSV 내보내기 전용 — 다른 4개 exporter와 동일하게 상한을 둔다. */
    List<ArtistResponseDto> getArtistsForExport();
    Page<ArtistResponseDto> getAdminArtistList(ArtistAdminListQuery query);
    ArtistRequestDto getArtistForEdit(Long id);
    void updateArtist(Long id, ArtistRequestDto dto);
    List<ArtistResponseDto> getTopArtists(int limit);
    void updateArtistPhoto(Long id, String imageKey);
    String uploadProfile(MultipartFile file, String artistName) throws IOException;
    void deleteArtist(Long id);
    long getTotalCount();
    void restoreArtist(Long id);
    List<ArtistResponseDto> getDeletedArtists();
}
