package com.feple.feple_backend.global;

import com.feple.feple_backend.global.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * "existsBy 사전 체크 → save() 사이의 TOCTOU 레이스는 유니크 제약이 최종 방어선"인 삽입 경로를
 * 여러 신고/차단류 서비스가 공통으로 쓴다 — 유니크 제약 위반을 사전 체크와 동일한 메시지의
 * {@link ConflictException}으로 변환하는 부분만 여기서 공유하고, 로드·빌더 조립 등 도메인별
 * 세부사항은 각 서비스가 콜백으로 제공한다.
 */
public final class DuplicateInsertGuard {

    private DuplicateInsertGuard() {}

    public static void save(Runnable insert, String conflictMessage) {
        try {
            insert.run();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(conflictMessage);
        }
    }
}
