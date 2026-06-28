package com.feple.feple_backend.admin.scraper;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

// http/https만 허용하고 루프백·사설·링크로컬 주소를 차단하는 SSRF 방어 유틸리티
final class SsrfUrlValidator {

    private SsrfUrlValidator() {}

    static void validate(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 URL입니다.");
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("http/https URL만 허용됩니다.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL에 호스트가 없습니다.");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("호스트를 찾을 수 없습니다: " + host);
        }
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
        }
    }
}
