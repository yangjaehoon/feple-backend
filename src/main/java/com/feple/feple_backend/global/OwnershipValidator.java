package com.feple.feple_backend.global;

import org.springframework.security.access.AccessDeniedException;

public final class OwnershipValidator {
    private OwnershipValidator() {}

    public static void checkOwner(Long ownerId, Long requesterId, String resourceName) {
        checkOwner(ownerId, requesterId, resourceName, "삭제");
    }

    public static void checkOwner(Long ownerId, Long requesterId, String resourceName, String action) {
        if (!isOwner(ownerId, requesterId)) {
            throw new AccessDeniedException("본인의 " + resourceName + "만 " + action + "할 수 있습니다.");
        }
    }

    /**
     * requesterId가 null일 수 있는(비로그인 등) 조회 흐름에서 예외 없이 소유 여부만 확인할 때 사용.
     * ownerId는 항상 이미 로드된 엔티티에서 가져오므로 null이 될 수 없다는 전제는 checkOwner와 동일 —
     * ownerId가 null이면 NPE로 그 전제 위반을 드러낸다.
     */
    public static boolean isOwner(Long ownerId, Long requesterId) {
        return ownerId.equals(requesterId);
    }
}
