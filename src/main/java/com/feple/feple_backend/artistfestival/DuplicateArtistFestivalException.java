package com.feple.feple_backend.artistfestival;

public class DuplicateArtistFestivalException extends RuntimeException {
    public DuplicateArtistFestivalException() {
        super("이미 이 페스티벌에 참여 중인 아티스트입니다.");
    }
    public DuplicateArtistFestivalException(String message) {
        super(message);
    }
}