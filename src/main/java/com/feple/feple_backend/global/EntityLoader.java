package com.feple.feple_backend.global;

import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.function.Function;

public final class EntityLoader {
    private EntityLoader() {}

    public static <T, ID> T getOrThrow(Function<ID, Optional<T>> finder, ID id, String entityName) {
        return finder.apply(id)
                .orElseThrow(() -> ResourceNotFoundException.of(entityName, id));
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
