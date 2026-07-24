package com.feple.feple_backend.admin.scraper;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 실제 소켓 연결에 사용되는 IP를 검증한 뒤 그대로 연결에 사용한다 — 검증과 연결
 * 사이에 별도의 DNS 재조회가 없어 TOCTOU(DNS 리바인딩)로 우회할 수 없다.
 * HttpClient는 리다이렉트를 따라갈 때도 매번 이 리졸버를 거치므로 리다이렉트를
 * 이용한 우회도 함께 막힌다.
 */
final class SsrfSafeDnsResolver implements DnsResolver {

    private final DnsResolver delegate = SystemDefaultDnsResolver.INSTANCE;

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = delegate.resolve(host);
        for (InetAddress addr : addresses) {
            if (SsrfUrlValidator.isUnsafeAddress(addr)) {
                throw new UnknownHostException("내부 네트워크 주소로 연결할 수 없습니다: " + host);
            }
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return delegate.resolveCanonicalHostname(host);
    }
}
