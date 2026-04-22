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
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(List.of(
                                    "--no-sandbox",
                                    "--disable-setuid-sandbox",
                                    "--disable-dev-shm-usage",
                                    "--disable-gpu"
                            ))
            );
            log.info("[Playwright] Chromium 브라우저 초기화 완료");
        } catch (Exception e) {
            log.error("[Playwright] 브라우저 초기화 실패: {}", e.getMessage());
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
