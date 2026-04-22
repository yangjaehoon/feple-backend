package com.feple.feple_backend.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PlaywrightBrowser {

    private Playwright playwright;
    private Browser browser;

    @PostConstruct
    public void init() {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu"
                ));
        try {
            // 먼저 이미 설치된 브라우저로 기동 시도
            playwright = Playwright.create();
            browser = playwright.chromium().launch(options);
            log.info("[Playwright] Chromium 브라우저 초기화 완료");
        } catch (Exception e) {
            // 설치되지 않은 경우에만 다운로드 (최초 1회)
            log.info("[Playwright] Chromium 미설치 — 설치 시작 (최초 1회)");
            try {
                com.microsoft.playwright.CLI.main(new String[]{"install", "chromium"});
                playwright = Playwright.create();
                browser = playwright.chromium().launch(options);
                log.info("[Playwright] Chromium 설치 및 초기화 완료");
            } catch (Exception ex) {
                log.error("[Playwright] 브라우저 초기화 실패: {}", ex.getMessage());
            }
        }
    }

    @PreDestroy
    public void close() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    /** 새 페이지를 열어 반환. 사용 후 반드시 page.close() 호출 */
    public Page newPage() {
        return browser.newPage(new Browser.NewPageOptions()
                .setLocale("ko-KR")
                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                              "AppleWebKit/537.36 (KHTML, like Gecko) " +
                              "Chrome/124.0.0.0 Safari/537.36"));
    }

    public boolean isReady() {
        return browser != null && browser.isConnected();
    }
}
