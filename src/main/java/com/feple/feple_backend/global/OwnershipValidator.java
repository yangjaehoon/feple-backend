package com.feple.feple_backend.global;

import org.springframework.security.access.AccessDeniedException;

public final class PermissionValidator {
    private PermissionValidator() {}

    public static void checkOwner(Long ownerId, Long requesterId, String resourceName) {
        checkOwner(ownerId, requesterId, resourceName, "삭제");
    }

    public static void checkOwner(Long ownerId, Long requesterId, String resourceName, String action) {
        if (!ownerId.equals(requesterId)) {
            throw new AccessDeniedException("본인의 " + resourceName + "만 " + action + "할 수 있습니다.");
        }
    }
}
