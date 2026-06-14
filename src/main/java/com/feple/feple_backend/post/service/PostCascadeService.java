package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;

public interface PostCascadeService {
    void deletePostsByFestival(Festival festival);
    void deletePostsByArtist(Artist artist);
}
