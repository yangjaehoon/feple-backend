package com.feple.feple_backend.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 크롤링 실행 시에만 브라우저를 열고, 완료 후 즉시 닫습니다.
 * 앱 실행 중에는 메모리를 점유하지 않습니다.
 */
@Slf4j
@Component
public class PlaywrightBrowser {

    private static final BrowserType.LaunchOptions LAUNCH_OPTIONS =
            new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-gpu",
                            "--single-process",
                            "--disable-extensions",
                            "--disable-plugins",
                            "--disable-background-networking",
                            "--disable-sync",
                            "--disable-translate",
                            "--no-first-run",
                            "--js-flags=--max-old-space-size=128"  // V8 힙 128MB로 제한
                    ));

    /**
     * 브라우저 세션을 열어 반환합니다. try-with-resources 또는 session.close()로 반드시 닫아야 합니다.
     */
    public BrowserSession openSession() {
        try {
            // 미설치 시 최초 1회만 다운로드
            ensureInstalled();
            Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch(LAUNCH_OPTIONS);
            log.info("[Playwright] 브라우저 세션 시작");
            return new BrowserSession(playwright, browser);
        } catch (Exception e) {
            throw new RuntimeException("[Playwright] 브라우저 세션 열기 실패: " + e.getMessage(), e);
        }
    }

    private void ensureInstalled() {
        // 이미 설치된 경우 빠르게 통과
        try (Playwright pw = Playwright.create()) {
            pw.chromium().executablePath();
            return; // 성공하면 이미 설치됨
        } catch (Exception ignored) {}

        // 미설치 → 다운로드
        log.info("[Playwright] Chromium 설치 시작 (최초 1회)");
        try {
            com.microsoft.playwright.CLI.main(new String[]{"install", "chromium"});
        } catch (Exception e) {
            log.warn("[Playwright] Chromium 설치 중 경고: {}", e.getMessage());
        }
        log.info("[Playwright] Chromium 설치 완료");
    }

    public static class BrowserSession implements AutoCloseable {
        private final Playwright playwright;
        private final Browser browser;

        BrowserSession(Playwright playwright, Browser browser) {
            this.playwright = playwright;
            this.browser = browser;
        }

        public Page newPage() {
            return browser.newPage(new Browser.NewPageOptions()
                    .setLocale("ko-KR")
                    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                  "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                  "Chrome/124.0.0.0 Safari/537.36"));
        }

        @Override
        public void close() {
            try { browser.close(); } catch (Exception ignored) {}
            try { playwright.close(); } catch (Exception ignored) {}
            log.info("[Playwright] 브라우저 세션 종료");
        }
    }
}
