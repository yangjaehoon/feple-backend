package com.feple.feple_backend.admin.song;

public final class SongApproveMessage {

    private SongApproveMessage() {}

    public static String build(boolean songSaved, String youtubeUrl) {
        if (songSaved) return "노래 요청이 승인되었습니다. 곡이 등록되었습니다.";
        if (youtubeUrl != null && !youtubeUrl.isBlank())
            return "노래 요청이 승인되었습니다. (YouTube 영상 정보를 가져오지 못해 곡은 등록되지 않았습니다.)";
        return "노래 요청이 승인되었습니다.";
    }
}
