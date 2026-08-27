package com.feple.feple_backend.post.entity;

/** PostDraft에 채워지는 내용 묶음 — 생성·수정이 동일한 값들을 받으므로 파라미터 객체로 묶는다. */
public record PostDraftContent(String title, String content, BoardType boardType, boolean anonymous,
                                Long artistId, Long festivalId, String imageKeysCsv) {}
