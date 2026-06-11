(function () {
    var festivalMap = {}; // id → {title, startDate, endDate}
    var rowIndex    = 0;
    var currentFestivalTitle = '';
    var artistNames      = []; // 선택된 페스티벌의 참여 아티스트 목록
    var stageNames       = []; // 선택된 페스티벌의 스테이지 목록
    var festivalStartDate = '';
    var festivalEndDate   = '';

    window.AdminUtils.initTabs();

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

        fetch(CrawlUrls.scrape, {
            method: 'POST', headers: headers,
            body: JSON.stringify({ url: url, source: source })
        })
        .then(function (r) {
            if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || '스크래핑 실패'); });
            return r.json();
        })
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
            showApplyResult('error', '스크래핑 실패: ' + window.AdminUtils.escapeHtml(err.message));
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

        if (!title)               { showApplyResult('error', '제목을 입력해주세요.'); document.getElementById('scrapeTitle').focus(); return; }
        if (!startDate || !endDate) { showApplyResult('error', '시작일과 종료일을 입력해주세요.'); return; }

        var genres = [];
        document.querySelectorAll('input[name="scrapeGenres"]:checked').forEach(function (cb) { genres.push(cb.value); });

        var btn = document.getElementById('btnRegisterFestival');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>등록 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        fetch(CrawlUrls.scrapeApply, {
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
        })
        .then(function (r) {
            if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || '등록 실패'); });
            return r.json();
        })
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

    /* ── 페스티벌 목록 로드 ── */
    fetch(CrawlUrls.festivals)
        .then(function (r) { return r.json(); })
        .then(function (festivals) {
            var sel = document.getElementById('selFestival');
            festivals.forEach(function (f) {
                festivalMap[f.id] = f;
                var opt = document.createElement('option');
                opt.value = f.id;
                opt.textContent = f.title;
                sel.appendChild(opt);
            });
        });

    /* ── 페스티벌 선택 시 날짜 범위 + 아티스트/스테이지 로드 ── */
    document.getElementById('selFestival').addEventListener('change', function () {
        var fid = this.value;
        var dateInput = document.getElementById('selDate');
        dateInput.value = '';

        if (!fid) {
            dateInput.disabled = true;
            artistNames      = [];
            stageNames       = [];
            festivalStartDate = '';
            festivalEndDate   = '';
            return;
        }

        var f = festivalMap[fid];
        currentFestivalTitle = f.title;
        festivalStartDate = f.startDate || '';
        festivalEndDate   = f.endDate   || '';
        if (f.startDate) { dateInput.min = f.startDate; dateInput.value = f.startDate; }
        if (f.endDate)   { dateInput.max = f.endDate; }
        dateInput.disabled = false;

        Promise.all([
            fetch(CrawlUrls.festivals + '/' + fid + '/artists').then(function (r) { return r.json(); }),
            fetch(CrawlUrls.festivals + '/' + fid + '/stages').then(function (r) { return r.json(); })
        ]).then(function (results) {
            artistNames = results[0];
            stageNames  = results[1];
            refreshExistingSelects();
        });
    });

    /* ── 날짜 일괄 설정: 모든 행에 동일 날짜 적용 ── */
    document.getElementById('selDate').addEventListener('change', function () {
        var date = this.value;
        if (!date) return;
        document.getElementById('ocrParseBody').querySelectorAll('[data-field="date"]').forEach(function (input) {
            input.value = date;
        });
    });

    function refreshExistingSelects() {
        document.getElementById('ocrParseBody').querySelectorAll('tr').forEach(function (tr) {
            var artistSel = tr.querySelector('[data-field="artist"]');
            var stageSel  = tr.querySelector('[data-field="stage"]');
            if (artistSel) {
                var cur = artistSel.value;
                artistSel.innerHTML = makeOptions(artistNames, cur);
            }
            if (stageSel) {
                var cur = stageSel.value;
                stageSel.innerHTML = makeOptions(stageNames, cur);
            }
        });
    }

    /* ── OCR 드래그&드롭 / 파일 선택 ── */
    var dropZone  = document.getElementById('ocrDropZone');
    var fileInput = document.getElementById('ocrFileInput');

    dropZone.addEventListener('click', function () { fileInput.click(); });
    document.getElementById('ocrFileSelectBtn').addEventListener('click', function (e) {
        e.stopPropagation();
        fileInput.click();
    });
    dropZone.addEventListener('dragover', function (e) { e.preventDefault(); dropZone.classList.add('drag-over'); });
    dropZone.addEventListener('dragleave', function () { dropZone.classList.remove('drag-over'); });
    dropZone.addEventListener('drop', function (e) {
        e.preventDefault(); dropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) handleOcrFile(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', function () {
        if (fileInput.files.length > 0) handleOcrFile(fileInput.files[0]);
    });

    function handleOcrFile(file) {
        if (!file.type.startsWith('image/')) { showApplyResult('error', '이미지 파일만 업로드 가능합니다.'); return; }
        if (file.size > 10 * 1024 * 1024) { showApplyResult('error', '파일 크기는 10MB 이하여야 합니다.'); return; }

        var reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById('ocrPreviewImg').src = e.target.result;
        };
        reader.readAsDataURL(file);

        document.getElementById('ocrEmptyState').style.display = 'none';
        document.getElementById('ocrPreviewWrap').classList.add('visible');
        document.getElementById('ocrParseBody').innerHTML = '';
        hideToast();
        document.getElementById('btnApplyOcr').disabled = true;

        var progressWrap = document.getElementById('ocrProgress');
        var progressFill = document.getElementById('ocrProgressFill');
        var progressLabel = document.getElementById('ocrProgressLabel');
        progressFill.style.width = '0%';
        progressWrap.classList.add('visible');
        progressLabel.textContent = 'AI 이미지 분석 중...';

        var progressTimer = null;
        var progressPct = 10;
        progressTimer = setInterval(function () {
            progressPct = Math.min(progressPct + 5, 85);
            progressFill.style.width = progressPct + '%';
        }, 800);

        var formData = new FormData();
        formData.append('image', file);
        var headers = window.AdminUtils.getCsrfHeaders();

        fetch(CrawlUrls.ocr, { method: 'POST', headers: headers, body: formData })
            .then(function (r) {
                clearInterval(progressTimer);
                progressFill.style.width = '100%';
                progressLabel.textContent = '완료!';
                if (!r.ok) {
                    return r.json().then(function (e) { throw new Error(e.error || '파싱 실패'); });
                }
                return r.json();
            })
            .then(function (results) {
                setTimeout(function () { progressWrap.classList.remove('visible'); }, 500);
                renderOcrResults(results);
            })
            .catch(function (err) {
                clearInterval(progressTimer);
                progressWrap.classList.remove('visible');
                showApplyResult('error', '오류: ' + err.message);
            });
    }

    function renderOcrResults(results) {
        var body = document.getElementById('ocrParseBody');
        body.innerHTML = '';
        rowIndex = 0;
        var hasLow = false;

        results.forEach(function (row) {
            var conf = row.confidence != null ? row.confidence : 0;
            if (conf < 70) hasLow = true;
            var matchedArtist = findBestMatch(row.artist || '', artistNames);
            var matchedStage  = findBestMatch(row.stage  || '', stageNames);
            var date = row.date || document.getElementById('selDate').value || '';
            appendRow(matchedArtist, matchedStage, date, row.startTime || '', row.endTime || '', conf);
        });

        document.getElementById('ocrWarnBox').classList.toggle('d-none', !hasLow);
        document.getElementById('btnApplyOcr').disabled = results.length === 0;
    }

    // 대소문자 무시 포함 매칭 — 가장 가까운 항목 반환, 없으면 ''
    function findBestMatch(name, options) {
        if (!name || options.length === 0) return '';
        var lower = name.toLowerCase();
        for (var i = 0; i < options.length; i++) {
            if (options[i].toLowerCase() === lower) return options[i];
        }
        for (var i = 0; i < options.length; i++) {
            if (options[i].toLowerCase().includes(lower) || lower.includes(options[i].toLowerCase())) {
                return options[i];
            }
        }
        return '';
    }

    function makeOptions(options, selectedValue) {
        var html = '<option value="">— 선택 —</option>';
        options.forEach(function (opt) {
            html += '<option value="' + window.AdminUtils.escapeHtml(opt) + '"' + (opt === selectedValue ? ' selected' : '') + '>' + window.AdminUtils.escapeHtml(opt) + '</option>';
        });
        return html;
    }

    function confBadge(conf) {
        if (conf == null) return '<span class="conf-mid">—</span>';
        if (conf >= 90) return '<span class="conf-high">🟢 ' + conf + '</span>';
        if (conf >= 70) return '<span class="conf-mid">🟡 ' + conf + '</span>';
        return '<span class="conf-low">🔴 ' + conf + '</span>';
    }

    var selectStyle = 'width:100%; padding:4px 6px; border:1px solid var(--border); border-radius:6px; font-size:12px;';
    var timeStyle   = 'width:100%; padding:4px 6px; border:1px solid var(--border); border-radius:6px; font-size:12px;';

    var dateStyle = 'width:100%; padding:4px 6px; border:1px solid var(--border); border-radius:6px; font-size:12px;';

    function appendRow(artist, stage, date, startTime, endTime, conf) {
        rowIndex++;
        var idx = rowIndex;
        var tr = document.createElement('tr');
        tr.dataset.rowIndex = idx;
        var dateAttrs = '';
        if (festivalStartDate) dateAttrs += ' min="' + festivalStartDate + '"';
        if (festivalEndDate)   dateAttrs += ' max="' + festivalEndDate   + '"';
        tr.innerHTML =
            '<td>' + idx + '</td>' +
            '<td><select data-field="artist" style="' + selectStyle + '">' + makeOptions(artistNames, artist) + '</select></td>' +
            '<td><select data-field="stage"  style="' + selectStyle + '">' + makeOptions(stageNames,  stage)  + '</select></td>' +
            '<td><input type="date" data-field="date" value="' + window.AdminUtils.escapeHtml(date) + '"' + dateAttrs + ' style="' + dateStyle + '"/></td>' +
            '<td><input type="time" data-field="startTime" value="' + window.AdminUtils.escapeHtml(startTime) + '" style="' + timeStyle + '"/></td>' +
            '<td><input type="time" data-field="endTime"   value="' + window.AdminUtils.escapeHtml(endTime)   + '" style="' + timeStyle + '"/></td>' +
            '<td>' + confBadge(conf) + '</td>' +
            '<td><button class="row-del">✕</button></td>';
        document.getElementById('ocrParseBody').appendChild(tr);
    }

    function checkEmpty() {
        var rows = document.getElementById('ocrParseBody').querySelectorAll('tr');
        if (rows.length === 0) {
            document.getElementById('btnApplyOcr').disabled = true;
        }
    }

    document.getElementById('ocrParseBody').addEventListener('click', function (e) {
        if (e.target.classList.contains('row-del')) {
            e.target.closest('tr').remove();
            checkEmpty();
        }
    });

    /* ── 행 추가 ── */
    document.getElementById('btnAddRow').addEventListener('click', function () {
        var defaultDate = document.getElementById('selDate').value || '';
        appendRow('', '', defaultDate, '', '', null);
        document.getElementById('ocrEmptyState').style.display = 'none';
        document.getElementById('ocrPreviewWrap').classList.add('visible');
        document.getElementById('btnApplyOcr').disabled = false;
    });

    /* ── 타임테이블에 적용 ── */
    document.getElementById('btnApplyOcr').addEventListener('click', function () {
        var fid = document.getElementById('selFestival').value;
        if (!fid) { showApplyResult('error', '페스티벌을 선택해주세요.'); return; }

        var entries = [];
        document.getElementById('ocrParseBody').querySelectorAll('tr').forEach(function (tr) {
            entries.push({
                artist:    tr.querySelector('[data-field="artist"]').value.trim(),
                stage:     tr.querySelector('[data-field="stage"]').value.trim(),
                date:      tr.querySelector('[data-field="date"]').value,
                startTime: tr.querySelector('[data-field="startTime"]').value,
                endTime:   tr.querySelector('[data-field="endTime"]').value
            });
        });

        if (entries.length === 0) { showApplyResult('error', '저장할 항목이 없습니다.'); return; }

        var missingDate = entries.some(function (e) { return !e.date; });
        if (missingDate) {
            showApplyResult('error', '날짜가 설정되지 않은 항목이 있습니다.<br>상단 "날짜 일괄 설정"을 사용하거나 각 행의 날짜를 직접 입력해주세요.');
            return;
        }

        var btn = document.getElementById('btnApplyOcr');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>저장 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        fetch(CrawlUrls.ocrApply, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ festivalId: parseInt(fid), entries: entries })
        })
        .then(function (r) {
            if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || '저장 실패'); });
            return r.json();
        })
        .then(function (result) {
            btn.disabled = false;
            btn.innerHTML = '타임테이블에 적용';

            var msg = result.savedCount + '개 저장 완료';
            if (result.failedCount > 0) {
                msg += ', ' + result.failedCount + '개 실패';
                var esc = window.AdminUtils.escapeHtml;
                var reasons = result.failures.map(function (f) {
                    return esc(f.artist) + '/' + esc(f.stage) + ': ' + esc(f.reason);
                }).join('<br>');
                msg += '<br><small>' + reasons + '</small>';
                showApplyResult('partial', msg);
            } else {
                showApplyResult('success', msg);
            }

            var uniqueDates = entries
                .map(function (e) { return e.date; })
                .filter(function (d, i, arr) { return d && arr.indexOf(d) === i; })
                .sort();
            var dateLabel = uniqueDates.length === 1
                ? uniqueDates[0]
                : uniqueDates.length > 1
                    ? uniqueDates[0] + ' 외 ' + (uniqueDates.length - 1) + '일'
                    : '—';
            addHistory(currentFestivalTitle, dateLabel, result.savedCount, result.failedCount);
        })
        .catch(function (err) {
            btn.disabled = false;
            btn.innerHTML = '타임테이블에 적용';
            showApplyResult('error', '오류: ' + err.message);
        });
    });

    var _toastTimer = null;

    function showApplyResult(type, msg) {
        var toast = document.getElementById('applyToast');
        document.getElementById('applyToastBody').innerHTML = msg;
        toast.className = type + ' visible';
        clearTimeout(_toastTimer);
        _toastTimer = setTimeout(hideToast, 5000);
    }

    function addHistory(festivalTitle, date, saved, failed) {
        var body = document.getElementById('ocrHistoryBody');
        var placeholder = body.querySelector('td[colspan]');
        if (placeholder) placeholder.closest('tr').remove();

        var now = new Date().toLocaleString('ko-KR');
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td>' + window.AdminUtils.escapeHtml(festivalTitle) + '</td>' +
            '<td>' + window.AdminUtils.escapeHtml(date) + '</td>' +
            '<td style="color:var(--success); font-weight:700;">' + saved + '</td>' +
            '<td style="color:' + (failed > 0 ? 'var(--danger)' : 'var(--muted)') + '; font-weight:700;">' + failed + '</td>' +
            '<td style="color:var(--muted);">' + now + '</td>';
        body.insertBefore(tr, body.firstChild);
    }

    function hideToast() {
        document.getElementById('applyToast').classList.remove('visible');
    }

    document.getElementById('applyToastClose').addEventListener('click', hideToast);

})();
