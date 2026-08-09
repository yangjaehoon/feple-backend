package com.feple.feple_backend.artist.event;

/** 아티스트 생성/수정/삭제/복구 등 이름 목록에 영향을 주는 변경이 커밋된 후 발행된다. */
public record ArtistDirectoryChangedEvent() {}
