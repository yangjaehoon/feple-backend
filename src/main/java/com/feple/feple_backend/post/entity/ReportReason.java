package com.feple.feple_backend.post.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    SPAM("스팸/광고"),
    ABUSE("욕설/혐오"),
    OBSCENE("음란물"),
    MISINFORMATION("허위정보"),
    OTHER("기타");

    private final String displayName;
}
