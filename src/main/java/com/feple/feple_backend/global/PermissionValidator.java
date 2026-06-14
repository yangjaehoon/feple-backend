package com.feple.feple_backend.global;

import org.springframework.security.access.AccessDeniedException;

public final class PermissionValidator {
    private PermissionValidator() {}

    public static void checkOwner(Long ownerId, Long requesterId, String resourceName) {
        if (!ownerId.equals(requesterId)) {
            throw new AccessDeniedException("본인의 " + resourceName + "만 삭제할 수 있습니다.");
        }
    }
}
