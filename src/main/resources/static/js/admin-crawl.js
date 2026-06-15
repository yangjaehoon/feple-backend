(function () {
    var festivalMap = {}; // id → {title, startDate, endDate}
    var rowIndex    = 0;
    var currentFestivalTitle = '';
    var artistNames      = []; // 선택된 페스티벌의 참여 아티스트 한국명 목록
    var artistNameEnMap  = {}; // 영문명(소문자) → 한국명 역방향 매핑
    var stageNames       = []; // 선택된 페스티벌의 스테이지 목록
    var festivalStartDate = '';
    var festivalEndDate   = '';
    var ocrPendingFile    = null; // 타임테이블 OCR: 파싱 대기 중인 파일
    var lineupPendingFile = null; // 라인업 OCR: 파싱 대기 중인 파일

    window.AdminUtils.initTabs();

    /* ── Gemini 사용량 ── */
    function loadQuota() {
        fetch(CrawlUrls.quota)
            .then(function (r) { return r.json(); })
            .then(function (q) { renderQuota(q); });
    }

    function renderQuota(q) {
        var pct = q.limit > 0 ? Math.min(100, Math.round(q.used / q.limit * 100)) : 0;
        var text = q.used + ' / ' + q.limit + '회 사용 (' + q.remaining + '회 남음)';
        var color = pct >= 90 ? 'var(--danger)' : (pct >= 70 ? 'var(--warning-text)' : 'var(--success)');

        ['quotaFill', 'quotaFillLineup'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) { el.style.width = pct + '%'; el.style.background = color; }
        });
        ['quotaText', 'quotaTextLineup'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) { el.textContent = text; el.style.color = color; }
        });
    }

    loadQuota();

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
            var artistData = results[0]; // [{name, nameEn}, ...]
            artistNames   = artistData.map(function (a) { return a.name; });
            artistNameEnMap = {};
            artistData.forEach(function (a) {
                if (a.nameEn) artistNameEnMap[a.nameEn.toLowerCase()] = a.name;
            });
            stageNames = results[1];
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

    function validateOcrFile(file) {
        if (!file.type.startsWith('image/')) { setOcrStartError('이미지 파일만 업로드 가능합니다.'); return false; }
        if (file.size > 10 * 1024 * 1024)   { setOcrStartError('파일 크기는 10MB 이하여야 합니다.'); return false; }
        return true;
    }

    function showOcrPreview(file) {
        var reader = new FileReader();
        reader.onload = function (e) { document.getElementById('ocrPreviewImg').src = e.target.result; };
        reader.readAsDataURL(file);
        document.getElementById('ocrEmptyState').style.display = 'none';
        document.getElementById('ocrPreviewWrap').classList.add('visible');
        document.getElementById('ocrParseBody').innerHTML = '';
        hideToast();
        document.getElementById('btnApplyOcr').disabled = true;
    }

    function startOcrProgress() {
        var fill  = document.getElementById('ocrProgressFill');
        var label = document.getElementById('ocrProgressLabel');
        fill.style.width = '0%';
        document.getElementById('ocrProgress').classList.add('visible');
        label.textContent = 'AI 이미지 분석 중...';
        var pct = 10;
        return setInterval(function () {
            pct = Math.min(pct + 5, 85);
            fill.style.width = pct + '%';
        }, 800);
    }

    function submitOcrRequest(file) {
        var formData = new FormData();
        formData.append('image', file);
        var timer = startOcrProgress();

        fetch(CrawlUrls.ocr, { method: 'POST', headers: window.AdminUtils.getCsrfHeaders(), body: formData })
            .then(function (r) {
                clearInterval(timer);
                document.getElementById('ocrProgressFill').style.width = '100%';
                document.getElementById('ocrProgressLabel').textContent = '완료!';
                if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || '파싱 실패'); });
                return r.json();
            })
            .then(function (results) {
                setTimeout(function () { document.getElementById('ocrProgress').classList.remove('visible'); }, 500);
                renderOcrResults(results);
                loadQuota();
            })
            .catch(function (err) {
                clearInterval(timer);
                document.getElementById('ocrProgress').classList.remove('visible');
                showApplyResult('error', '오류: ' + err.message);
            });
    }

    function showUploadedPreview(zoneId, fileInputId, file) {
        var zone = document.getElementById(zoneId);
        var reader = new FileReader();
        reader.onload = function (e) {
            zone.innerHTML =
                '<div class="upload-preview-state">' +
                '<img class="upload-preview-thumb" src="' + e.target.result + '" alt="미리보기">' +
                '<div class="upload-preview-info">' +
                '<div class="upload-preview-name">' + window.AdminUtils.escapeHtml(file.name) + '</div>' +
                '<div class="upload-preview-size">' + (file.size / 1024).toFixed(0) + ' KB</div>' +
                '</div>' +
                '<button type="button" class="btn btn-secondary btn-sm upload-preview-reselect">다시 선택</button>' +
                '</div>';
            zone.querySelector('.upload-preview-reselect').addEventListener('click', function (evt) {
                evt.stopPropagation();
                document.getElementById(fileInputId).click();
            });
        };
        reader.readAsDataURL(file);
    }

    function handleOcrFile(file) {
        if (!validateOcrFile(file)) return;
        ocrPendingFile = file;
        document.getElementById('ocrSelectedFileName').textContent = '';
        setOcrStartError('');
        showUploadedPreview('ocrDropZone', 'ocrFileInput', file);
        showOcrPreview(file);
    }

    function setOcrFestivalError(msg) {
        var sel = document.getElementById('selFestival');
        var err = document.getElementById('ocrFestivalError');
        err.textContent = msg;
        sel.classList.toggle('select-error', msg.length > 0);
    }

    function setOcrStartError(msg) {
        document.getElementById('ocrStartError').textContent = msg;
    }

    document.getElementById('selFestival').addEventListener('change', function () {
        if (this.value) setOcrFestivalError('');
    });

    document.getElementById('btnStartOcr').addEventListener('click', function () {
        var fid = document.getElementById('selFestival').value;
        if (!fid) {
            setOcrFestivalError('페스티벌을 선택해주세요.');
            document.getElementById('selFestival').focus();
            return;
        }
        setOcrFestivalError('');
        if (!ocrPendingFile) {
            setOcrStartError('이미지를 먼저 업로드해주세요.');
            return;
        }
        setOcrStartError('');
        submitOcrRequest(ocrPendingFile);
    });

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

    // 대소문자 무시 매칭 — 한국명 완전일치 → 영문명 완전일치 → 부분일치 순으로 탐색
    function findBestMatch(name, options) {
        if (!name || options.length === 0) return '';
        var lower = name.toLowerCase();
        // 1. 한국명 완전일치
        for (var i = 0; i < options.length; i++) {
            if (options[i].toLowerCase() === lower) return options[i];
        }
        // 2. 영문명(nameEn) 완전일치 → 해당 한국명 반환
        if (artistNameEnMap[lower]) return artistNameEnMap[lower];
        // 3. 한국명/영문명 부분일치
        for (var i = 0; i < options.length; i++) {
            if (options[i].toLowerCase().includes(lower) || lower.includes(options[i].toLowerCase())) {
                return options[i];
            }
        }
        // 4. 영문명 키 부분일치
        var enKeys = Object.keys(artistNameEnMap);
        for (var i = 0; i < enKeys.length; i++) {
            if (enKeys[i].includes(lower) || lower.includes(enKeys[i])) {
                return artistNameEnMap[enKeys[i]];
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

    var fieldStyle = 'width:100%; padding:4px 6px; border:1px solid var(--border); border-radius:6px; font-size:12px;';
    var selectStyle = fieldStyle, timeStyle = fieldStyle, dateStyle = fieldStyle;

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
            '<td class="row-error-cell"></td>' +
            '<td><button class="row-del">✕</button></td>';
        document.getElementById('ocrParseBody').appendChild(tr);
    }

    function checkEmpty() {
        if (document.getElementById('ocrParseBody').querySelectorAll('tr').length === 0) {
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

    function collectOcrEntries() {
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
        return entries;
    }

    function validateOcrEntries(entries) {
        if (entries.length === 0) {
            showApplyResult('error', '저장할 항목이 없습니다.');
            return false;
        }
        if (entries.some(function (e) { return !e.date; })) {
            showApplyResult('error', '날짜가 설정되지 않은 항목이 있습니다.<br>상단 "날짜 일괄 설정"을 사용하거나 각 행의 날짜를 직접 입력해주세요.');
            return false;
        }
        return true;
    }

    function formatDateRange(entries) {
        var uniqueDates = entries
            .map(function (e) { return e.date; })
            .filter(function (d, i, arr) { return d && arr.indexOf(d) === i; })
            .sort();
        if (uniqueDates.length === 0) return '—';
        if (uniqueDates.length === 1) return uniqueDates[0];
        return uniqueDates[0] + ' 외 ' + (uniqueDates.length - 1) + '일';
    }

    function renderOcrApplyResult(result, allRows) {
        if (result.failedCount === 0) {
            // 전체 성공 — 테이블 비우기
            document.getElementById('ocrParseBody').innerHTML = '';
            checkEmpty();
            showApplyResult('success', result.savedCount + '개 모두 저장 완료');
            return;
        }

        // 실패 인덱스 맵 구성
        var failedMap = {};
        result.failures.forEach(function (f) {
            failedMap[parseInt(f.index)] = f.reason;
        });

        // 성공 행 제거, 실패 행 강조 + 사유 인라인 표시
        allRows.forEach(function (tr, i) {
            if (failedMap.hasOwnProperty(i)) {
                tr.style.backgroundColor = 'rgba(220,53,69,0.08)';
                var cell = tr.querySelector('.row-error-cell');
                if (cell) {
                    cell.innerHTML =
                        '<span style="color:var(--danger);font-size:11px;font-weight:600;white-space:pre-wrap;">' +
                        window.AdminUtils.escapeHtml(failedMap[i]) + '</span>';
                }
            } else {
                tr.remove();
            }
        });

        checkEmpty();
        var msg = result.savedCount + '개 저장, ' + result.failedCount + '개 실패 — 아래 빨간 행을 수정 후 다시 적용하세요.';
        showApplyResult('partial', msg);
    }

    /* ── 타임테이블에 적용 ── */
    document.getElementById('btnApplyOcr').addEventListener('click', function () {
        var fid = document.getElementById('selFestival').value;
        if (!fid) { showApplyResult('error', '페스티벌을 선택해주세요.'); return; }

        var entries = collectOcrEntries();
        if (!validateOcrEntries(entries)) return;

        // 현재 DOM 행 순서를 스냅샷으로 유지 (apply 후 성공/실패 구분에 사용)
        var allRows = Array.from(document.getElementById('ocrParseBody').querySelectorAll('tr'));

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
            renderOcrApplyResult(result, allRows);
            addHistory(currentFestivalTitle, formatDateRange(entries), result.savedCount, result.failedCount);
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

    // ════════════════════════════════════════════════════════
    //  라인업 OCR
    // ════════════════════════════════════════════════════════

    /* ── 페스티벌 드롭다운 공유 ── */
    fetch(CrawlUrls.festivals)
        .then(function (r) { return r.json(); })
        .then(function (festivals) {
            var sel = document.getElementById('selLineupFestival');
            festivals.forEach(function (f) {
                var opt = document.createElement('option');
                opt.value = f.id;
                opt.textContent = f.title;
                sel.appendChild(opt);
            });
        });

    /* ── 파일 업로드 ── */
    var lineupDropZone  = document.getElementById('lineupDropZone');
    var lineupFileInput = document.getElementById('lineupFileInput');

    lineupDropZone.addEventListener('click', function () { lineupFileInput.click(); });
    document.getElementById('lineupFileSelectBtn').addEventListener('click', function (e) {
        e.stopPropagation();
        lineupFileInput.click();
    });
    lineupDropZone.addEventListener('dragover', function (e) { e.preventDefault(); lineupDropZone.classList.add('drag-over'); });
    lineupDropZone.addEventListener('dragleave', function () { lineupDropZone.classList.remove('drag-over'); });
    lineupDropZone.addEventListener('drop', function (e) {
        e.preventDefault(); lineupDropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) handleLineupFile(e.dataTransfer.files[0]);
    });
    lineupFileInput.addEventListener('change', function () {
        if (lineupFileInput.files.length > 0) handleLineupFile(lineupFileInput.files[0]);
    });

    function handleLineupFile(file) {
        if (!file.type.startsWith('image/')) { setLineupStartError('이미지 파일만 업로드 가능합니다.'); return; }
        if (file.size > 10 * 1024 * 1024)   { setLineupStartError('파일 크기는 10MB 이하여야 합니다.'); return; }
        lineupPendingFile = file;
        document.getElementById('lineupSelectedFileName').textContent = '';
        setLineupStartError('');
        showUploadedPreview('lineupDropZone', 'lineupFileInput', file);

        var reader = new FileReader();
        reader.onload = function (e) { document.getElementById('lineupPreviewImg').src = e.target.result; };
        reader.readAsDataURL(file);
        hideToast();
    }

    function submitLineupRequest(file) {
        document.getElementById('lineupEmptyState').style.display = 'none';
        document.getElementById('lineupResultArea').classList.remove('d-none');
        document.getElementById('lineupPreviewWrap').classList.add('visible');
        document.getElementById('lineupParseBody').innerHTML = '';
        document.getElementById('btnApplyLineup').disabled = true;
        document.getElementById('lineupSelectCount').textContent = '';

        var fill  = document.getElementById('lineupProgressFill');
        var label = document.getElementById('lineupProgressLabel');
        fill.style.width = '0%';
        document.getElementById('lineupProgress').classList.add('visible');
        label.textContent = 'AI 이미지 분석 중...';
        var pct = 10;
        var timer = setInterval(function () { pct = Math.min(pct + 5, 85); fill.style.width = pct + '%'; }, 800);

        var formData = new FormData();
        formData.append('image', file);

        fetch(CrawlUrls.lineupOcr, { method: 'POST', headers: window.AdminUtils.getCsrfHeaders(), body: formData })
            .then(function (r) {
                clearInterval(timer);
                fill.style.width = '100%';
                label.textContent = '완료!';
                if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || '파싱 실패'); });
                return r.json();
            })
            .then(function (results) {
                setTimeout(function () { document.getElementById('lineupProgress').classList.remove('visible'); }, 500);
                renderLineupResults(results);
                loadQuota();
            })
            .catch(function (err) {
                clearInterval(timer);
                document.getElementById('lineupProgress').classList.remove('visible');
                showApplyResult('error', '오류: ' + err.message);
            });
    }

    function setLineupFestivalError(msg) {
        var sel = document.getElementById('selLineupFestival');
        var err = document.getElementById('lineupFestivalError');
        err.textContent = msg;
        sel.classList.toggle('select-error', msg.length > 0);
    }

    function setLineupStartError(msg) {
        document.getElementById('lineupStartError').textContent = msg;
    }

    document.getElementById('selLineupFestival').addEventListener('change', function () {
        if (this.value) setLineupFestivalError('');
    });

    document.getElementById('btnStartLineup').addEventListener('click', function () {
        var fid = document.getElementById('selLineupFestival').value;
        if (!fid) {
            setLineupFestivalError('페스티벌을 선택해주세요.');
            document.getElementById('selLineupFestival').focus();
            return;
        }
        setLineupFestivalError('');
        if (!lineupPendingFile) {
            setLineupStartError('이미지를 먼저 업로드해주세요.');
            return;
        }
        setLineupStartError('');
        submitLineupRequest(lineupPendingFile);
    });

    function renderLineupResults(results) {
        var body = document.getElementById('lineupParseBody');
        body.innerHTML = '';
        var hasUnmatched = false;

        results.forEach(function (row) {
            var matched = row.artistId != null;
            if (!matched) hasUnmatched = true;
            var conf = row.confidence != null ? row.confidence : 0;
            var tr = document.createElement('tr');
            if (!matched) tr.classList.add('row-unmatched');

            var confBadgeHtml = conf >= 90
                ? '<span class="conf-high">🟢 ' + conf + '</span>'
                : (conf >= 70 ? '<span class="conf-mid">🟡 ' + conf + '</span>'
                             : '<span class="conf-low">🔴 ' + conf + '</span>');

            var matchedHtml = matched
                ? '<span style="color:var(--success); font-weight:600;">✅ ' + window.AdminUtils.escapeHtml(row.matchedName) + '</span>'
                : '<span style="color:var(--danger);">❌ 미매칭</span>';

            tr.innerHTML =
                '<td><input type="checkbox" class="lineup-chk" data-artist-id="' + (row.artistId || '') + '"' +
                (matched ? ' checked' : ' disabled') + '></td>' +
                '<td>' + window.AdminUtils.escapeHtml(row.parsedName) + '</td>' +
                '<td>' + matchedHtml + '</td>' +
                '<td>' + confBadgeHtml + '</td>';
            body.appendChild(tr);
        });

        document.getElementById('lineupWarnBox').classList.toggle('d-none', !hasUnmatched);
        updateLineupCount();
    }

    function updateLineupCount() {
        var checked = document.querySelectorAll('.lineup-chk:checked:not([disabled])').length;
        document.getElementById('lineupSelectCount').textContent = checked + '명 선택됨';
        document.getElementById('btnApplyLineup').disabled = checked === 0;
    }

    document.getElementById('lineupParseBody').addEventListener('change', function (e) {
        if (e.target.classList.contains('lineup-chk')) updateLineupCount();
    });

    document.getElementById('lineupCheckAll').addEventListener('change', function () {
        var checked = this.checked;
        document.querySelectorAll('.lineup-chk:not([disabled])').forEach(function (chk) {
            chk.checked = checked;
        });
        updateLineupCount();
    });

    /* ── 등록 실행 ── */
    document.getElementById('btnApplyLineup').addEventListener('click', function () {
        var fid = document.getElementById('selLineupFestival').value;
        if (!fid) { showApplyResult('error', '페스티벌을 선택해주세요.'); return; }

        var artistIds = [];
        document.querySelectorAll('.lineup-chk:checked:not([disabled])').forEach(function (chk) {
            var id = parseInt(chk.getAttribute('data-artist-id'));
            if (id) artistIds.push(id);
        });
        if (artistIds.length === 0) { showApplyResult('error', '등록할 아티스트를 선택해주세요.'); return; }

        var btn = document.getElementById('btnApplyLineup');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>등록 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        fetch(CrawlUrls.lineupOcrApply, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ festivalId: parseInt(fid), artistIds: artistIds })
        })
        .then(function (r) {
            if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || '등록 실패'); });
            return r.json();
        })
        .then(function (result) {
            btn.disabled = false;
            btn.innerHTML = '참여 아티스트로 등록';
            var msg = result.added + '명 등록 완료';
            if (result.duplicates > 0) msg += ' (' + result.duplicates + '명 이미 등록됨)';
            var fTitle = document.getElementById('selLineupFestival').selectedOptions[0].textContent;
            msg += '. <a href="' + CrawlUrls.festivalDetailBase + '/' + fid + '#artists" style="color:var(--primary);text-decoration:underline;font-weight:700;">페스티벌 확인 →</a>';
            showApplyResult('success', msg);
        })
        .catch(function (err) {
            btn.disabled = false;
            btn.innerHTML = '참여 아티스트로 등록';
            showApplyResult('error', '오류: ' + err.message);
        });
    });

})();
