(function () {
    // ════════════════════════════════════════════════════════
    //  웹 스크래핑 탭
    // ════════════════════════════════════════════════════════

    /* ── 소스 카드 선택 ── */
    document.querySelectorAll('.source-card').forEach(function (card) {
        card.addEventListener('click', function () {
            document.querySelectorAll('.source-card').forEach(function (c) { c.classList.remove('selected'); });
            card.classList.add('selected');
        });
    });
    document.getElementById('crawlUrl').addEventListener('input', function () {
        document.getElementById('btnStartCrawl').disabled = this.value.trim().length === 0;
    });

    /* ── 스크래핑 시작 ── */
    document.getElementById('btnStartCrawl').addEventListener('click', function () {
        var url    = document.getElementById('crawlUrl').value.trim();
        var source = document.querySelector('input[name="crawlSource"]:checked').value;
        if (!url) return;

        document.getElementById('scrapeEmptyState').style.display   = 'none';
        document.getElementById('scrapeResultForm').classList.add('d-none');
        document.getElementById('scrapeLoading').classList.remove('d-none');
        document.getElementById('scrapeApplyResult').style.display  = 'none';

        var btn = document.getElementById('btnStartCrawl');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>분석 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        window.AdminUtils.requestJson(CrawlUrls.scrape, {
            method: 'POST', headers: headers,
            body: JSON.stringify({ url: url, source: source })
        }, '스크래핑 실패')
        .then(function (data) {
            document.getElementById('scrapeLoading').classList.add('d-none');
            btn.disabled = (document.getElementById('crawlUrl').value.trim().length === 0);
            btn.innerHTML = '스크래핑 시작';
            renderScrapeResult(data);
        })
        .catch(function (err) {
            document.getElementById('scrapeLoading').classList.add('d-none');
            document.getElementById('scrapeEmptyState').style.display = 'block';
            btn.disabled = false;
            btn.innerHTML = '스크래핑 시작';
            window.AdminCrawl.showApplyResult('error', '스크래핑 실패: ' + window.AdminUtils.escapeHtml(err.message));
        });
    });

    function renderScrapeResult(data) {
        var sourceDisplayNames = { interpark: '인터파크', yes24: '예스24', melon: '멜론티켓', direct: '직접 입력' };
        document.getElementById('scrapeSourceLabel').textContent = sourceDisplayNames[data.source] || data.source;

        var warnEl = document.getElementById('scrapeWarnMsg');
        if (data.warning) { warnEl.textContent = data.warning; warnEl.classList.remove('d-none'); }
        else { warnEl.classList.add('d-none'); }

        document.getElementById('scrapeTitle').value      = data.title       || '';
        document.getElementById('scrapeTitleEn').value    = '';
        document.getElementById('scrapeDesc').value       = data.description || '';
        document.getElementById('scrapeLocation').value   = data.location    || '';
        document.getElementById('scrapeStartDate').value  = data.startDate   || '';
        document.getElementById('scrapeEndDate').value    = data.endDate     || '';
        document.getElementById('scrapeRegion').value     = '';
        document.querySelectorAll('input[name="scrapeGenres"]').forEach(function (cb) { cb.checked = false; });

        var img      = document.getElementById('scrapePosterImg');
        var emptyDiv = document.getElementById('scrapePosterEmpty');
        if (data.posterImageUrl) {
            img.src           = data.posterImageUrl;
            img.classList.remove('d-none');
            emptyDiv.style.display = 'none';
        } else {
            img.classList.add('d-none');
            emptyDiv.style.display = 'flex';
        }

        document.getElementById('scrapeResultForm').classList.remove('d-none');
        document.getElementById('scrapeApplyResult').style.display = 'none';
    }

    /* ── 페스티벌 등록 ── */
    document.getElementById('btnRegisterFestival').addEventListener('click', function () {
        var title     = document.getElementById('scrapeTitle').value.trim();
        var startDate = document.getElementById('scrapeStartDate').value;
        var endDate   = document.getElementById('scrapeEndDate').value;

        if (!title)               { window.AdminCrawl.showApplyResult('error', '제목을 입력해주세요.'); document.getElementById('scrapeTitle').focus(); return; }
        if (!startDate || !endDate) { window.AdminCrawl.showApplyResult('error', '시작일과 종료일을 입력해주세요.'); return; }

        var genres = [];
        document.querySelectorAll('input[name="scrapeGenres"]:checked').forEach(function (cb) { genres.push(cb.value); });

        var btn = document.getElementById('btnRegisterFestival');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>등록 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        window.AdminUtils.requestJson(CrawlUrls.scrapeApply, {
            method: 'POST', headers: headers,
            body: JSON.stringify({
                title:       title,
                titleEn:     document.getElementById('scrapeTitleEn').value.trim() || null,
                description: document.getElementById('scrapeDesc').value.trim(),
                location:    document.getElementById('scrapeLocation').value.trim(),
                startDate:   startDate,
                endDate:     endDate,
                region:      document.getElementById('scrapeRegion').value || null,
                genres:      genres
            })
        }, '등록 실패')
        .then(function (result) {
            btn.disabled = false;
            btn.innerHTML = '페스티벌 등록하기';
            var el = document.getElementById('scrapeApplyResult');
            el.className = 'apply-result success';
            el.innerHTML = '페스티벌이 등록되었습니다. <a href="' + CrawlUrls.festivalDetailBase + '/' + result.festivalId + '" style="color:var(--primary); text-decoration:underline; font-weight:700;">상세 보기 →</a>';
            el.style.display = 'block';
        })
        .catch(function (err) {
            btn.disabled = false;
            btn.innerHTML = '페스티벌 등록하기';
            var el = document.getElementById('scrapeApplyResult');
            el.className = 'apply-result error';
            el.textContent = '오류: ' + err.message;
            el.style.display = 'block';
        });
    });

    /* ── 초기화 ── */
    document.getElementById('btnScrapeReset').addEventListener('click', function () {
        document.getElementById('scrapeResultForm').classList.add('d-none');
        document.getElementById('scrapeEmptyState').style.display  = 'block';
    });
})();
