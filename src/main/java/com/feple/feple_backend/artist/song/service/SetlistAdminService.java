package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SetlistAdminService {
    List<ArtistFestivalSong> getSetlist(Long artistFestivalId);
    void saveSetlist(Long artistFestivalId, Set<Long> songIds);
    void updateSetlist(Long festivalId, Long artistFestivalId, Set<Long> songIds);
    Map<Long, Integer> getSetlistCounts(List<Long> artistFestivalIds);
}
