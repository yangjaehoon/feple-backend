package com.feple.feple_backend.admin.scraper;

import com.feple.feple_backend.global.exception.InvalidRequestException;

import java.net.Inet4Address;
import java.net.Inet6Address;
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
            throw new InvalidRequestException("유효하지 않은 URL입니다.");
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new InvalidRequestException("http/https URL만 허용됩니다.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidRequestException("URL에 호스트가 없습니다.");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new InvalidRequestException("호스트를 찾을 수 없습니다: " + host);
        }
        if (isUnsafeAddress(addr)) {
            throw new InvalidRequestException("내부 네트워크 주소는 허용되지 않습니다.");
        }
    }

    // 이 시점의 검증은 스킴·호스트 형식에 대한 빠른 실패용이다. 실제 연결 시점의
    // DNS 재조회(TOCTOU/DNS 리바인딩)에 대한 방어는 SsrfSafeDnsResolver가 담당한다.
    static boolean isUnsafeAddress(InetAddress addr) {
        return addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()
                || isIpv6UniqueLocal(addr) || isIpv4ZeroBlock(addr);
    }

    // Inet6Address.isSiteLocalAddress()는 폐기된 IPv6 site-local(fec0::/10)만 인식하고
    // 현재 표준인 ULA(Unique Local Address, fc00::/7)는 걸러내지 못한다 — 공격자가 도메인의
    // AAAA 레코드를 fd00::/8 등 내부망 주소로 설정하면 우회 가능하므로 별도로 차단한다.
    private static boolean isIpv6UniqueLocal(InetAddress addr) {
        if (!(addr instanceof Inet6Address)) return false;
        byte[] bytes = addr.getAddress();
        return (bytes[0] & 0xFE) == 0xFC;
    }

    // isAnyLocalAddress()는 정확히 0.0.0.0만 차단한다 — 0.0.0.0/8 나머지 대역(0.0.0.1 등)도
    // 일부 환경에서 loopback처럼 라우팅될 수 있어 대역 전체를 차단한다.
    private static boolean isIpv4ZeroBlock(InetAddress addr) {
        if (!(addr instanceof Inet4Address)) return false;
        return addr.getAddress()[0] == 0;
    }
}
