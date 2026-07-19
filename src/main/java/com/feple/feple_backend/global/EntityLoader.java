package com.feple.feple_backend.global;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

public final class EntityLoader {
    private EntityLoader() {}

    public static <T, ID> T getOrThrow(Function<ID, Optional<T>> finder, ID id, String entityName) {
        return finder.apply(id)
                .orElseThrow(() -> new NoSuchElementException(entityName + "을(를) 찾을 수 없습니다: " + id));
    }

    /**
     * @param entityDescriptionWithParticle 조사가 포함된 엔티티 표현 (예: "부스가", "항목이")
     */
    public static void requireBelongsToFestival(Long expectedFestivalId, Long actualFestivalId,
                                                  String entityDescriptionWithParticle) {
        if (!expectedFestivalId.equals(actualFestivalId)) {
            throw new IllegalArgumentException("해당 페스티벌의 " + entityDescriptionWithParticle + " 아닙니다.");
        }
    }
}
