package com.feple.feple_backend.admin.artist;

/**
 * 아티스트 신청/미매칭 제안 목록의 "아티스트 등록" 링크를 타고 생성 폼에 들어온 경우의 출처 참조.
 * 둘 다 선택적(null 가능)이며, 생성 성공 시 해당 출처를 자동으로 해소하는 데 쓰인다.
 */
record SuggestionRefs(Long suggestionId, Long unmatchedSuggestionId) {}
