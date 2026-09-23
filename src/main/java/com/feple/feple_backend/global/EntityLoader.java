package com.feple.feple_backend.global;

import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class EntityLoader {
    private EntityLoader() {}

    public static <T, ID> T getOrThrow(Function<ID, Optional<T>> finder, ID id, String entityName) {
        return finder.apply(id)
                .orElseThrow(() -> ResourceNotFoundException.of(entityName, id));
    }

    /**
     * 엔티티가 실제로 필요하지 않고 존재 여부만 확인하면 되는 경우 — getOrThrow와 달리 행을
     * 통째로 로드하지 않는다. 사용하지 않을 반환값을 버리는 대신 검증 의도를 드러낸다.
     */
    public static <ID> void requireExists(Predicate<ID> existsCheck, ID id, String entityName) {
        if (!existsCheck.test(id)) {
            throw ResourceNotFoundException.of(entityName, id);
        }
    }

    /**
     * @param entityDescriptionWithParticle 조사가 포함된 엔티티 표현 (예: "부스가", "항목이")
     */
    public static void requireBelongsToFestival(Long expectedFestivalId, Long actualFestivalId,
                                                  String entityDescriptionWithParticle) {
        if (!expectedFestivalId.equals(actualFestivalId)) {
            throw new InvalidRequestException("해당 페스티벌의 " + entityDescriptionWithParticle + " 아닙니다.");
        }
    }
}
