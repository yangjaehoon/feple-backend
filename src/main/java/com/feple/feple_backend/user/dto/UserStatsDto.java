package com.feple.feple_backend.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserStatsDto {
    private long postCount;
    private long commentCount;
    // 신고당한 횟수는 모더레이션 민감정보 — GET /users/{id}/stats는 본인 확인 없는 공개
    // 엔드포인트라 REST(JSON) 응답에서는 숨긴다. 관리자 상세 페이지(Thymeleaf)는 이 getter를
    // 리플렉션으로 직접 호출해 값을 그대로 쓰므로(Jackson 미개입) 영향받지 않는다.
    @JsonIgnore
    private long reportCount;
    private long likedPostCount;
    private long scrapCount;
    private long certificationCount;
}
