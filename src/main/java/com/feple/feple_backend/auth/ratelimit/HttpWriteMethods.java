package com.feple.feple_backend.auth.ratelimit;

import java.util.Locale;
import java.util.Set;

/** 상태를 변경하는 HTTP 메서드 판별. GET/HEAD/OPTIONS 등 안전한 메서드는 제외한다. */
final class HttpWriteMethods {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private HttpWriteMethods() {}

    static boolean isWriteMethod(String method) {
        return method != null && WRITE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }
}
