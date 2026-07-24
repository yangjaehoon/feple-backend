(function () {
    // ════════════════════════════════════════════════════════
    //  타임테이블 OCR 탭
    // ════════════════════════════════════════════════════════

    var ocrSelect = window.AdminCrawl.ocrSelect;

    var rowIndex = 0;
    var currentFestivalTitle = '';
    var artistNames      = []; // 선택된 페스티벌의 참여 아티스트 한국명 목록
    var artistNameEnMap  = {}; // 영문명(소문자) → 한국명 역방향 매핑
    var stageNames       = []; // 선택된 페스티벌의 스테이지 목록
    var festivalStartDate = '';
    var festivalEndDate   = '';
    var ocrPendingFile    = null; // 파싱 대기 중인 파일

    /* ── 페스티벌 선택 시 날짜 범위 + 아티스트/스테이지 로드 ── */
    ocrSelect.addEventListener('change', function () {
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

        var f = window.AdminCrawl.festivalMap[fid];
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
        }).catch(function () {});
    });

    /* ── 날짜 미설정 행 보충: 비어있는 행에만 날짜 적용 ── */
    document.getElementById('selDate').addEventListener('change', function () {
        var date = this.value;
        if (!date) return;
        document.getElementById('ocrParseBody').querySelectorAll('[data-field="date"]').forEach(function (input) {
            if (!input.value) input.value = date;
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
        window.AdminCrawl.hideToast();
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
        if (festivalStartDate) {
            formData.append('year', festivalStartDate.substring(0, 4));
        }
        var timer = startOcrProgress();

        fetch(CrawlUrls.ocr, { method: 'POST', headers: window.AdminUtils.getCsrfHeaders(), body: formData })
            .then(function (r) {
                clearInterval(timer);
                document.getElementById('ocrProgressFill').style.width = '100%';
                document.getElementById('ocrProgressLabel').textContent = '완료!';
                return window.AdminUtils.parseJsonOrThrow(r, '파싱 실패');
            })
            .then(function (data) {
                setTimeout(function () { document.getElementById('ocrProgress').classList.remove('visible'); }, 500);
                renderOcrResults(data.entries, data.truncated);
                window.AdminCrawl.loadQuota();
            })
            .catch(function (err) {
                clearInterval(timer);
                document.getElementById('ocrProgress').classList.remove('visible');
                window.AdminCrawl.showApplyResult('error', '오류: ' + window.AdminUtils.escapeHtml(err.message));
            });
    }

    function handleOcrFile(file) {
        if (!validateOcrFile(file)) return;
        ocrPendingFile = file;
        document.getElementById('ocrSelectedFileName').textContent = '';
        setOcrStartError('');
        window.AdminCrawl.showUploadedPreview('ocrDropZone', 'ocrFileInput', file);
        showOcrPreview(file);
    }

    function setOcrFestivalError(msg) {
        document.getElementById('ocrFestivalError').textContent = msg;
        ocrSelect.setError(msg.length > 0);
    }

    function setOcrStartError(msg) {
        document.getElementById('ocrStartError').textContent = msg;
    }

    ocrSelect.addEventListener('change', function () {
        if (this.value) setOcrFestivalError('');
    });

    document.getElementById('btnStartOcr').addEventListener('click', function () {
        var fid = ocrSelect.value;
        if (!fid) {
            setOcrFestivalError('페스티벌을 선택해주세요.');
            ocrSelect.focus();
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

    function renderOcrResults(results, truncated) {
        var body = document.getElementById('ocrParseBody');
        body.innerHTML = '';
        rowIndex = 0;
        var hasLow = false;

        document.getElementById('ocrTruncatedBox').classList.toggle('d-none', !truncated);

        results.forEach(function (row) {
            var conf = row.confidence != null ? row.confidence : 0;
            if (conf < 70) hasLow = true;
            var type = row.type || 'PERFORMANCE';
            var date = row.date || document.getElementById('selDate').value || '';
            if (type === 'OPS') {
                appendRow(row.artist || '', '', date, row.startTime || '', row.endTime || '', conf, 'OPS');
            } else {
                var matchedArtist = findBestMatch(row.artist || '', artistNames);
                var matchedStage  = findBestMatch(row.stage  || '', stageNames);
                appendRow(matchedArtist, matchedStage, date, row.startTime || '', row.endTime || '', conf, 'PERFORMANCE');
            }
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

    var OPS_STAGE = '📢';

    function appendRow(artist, stage, date, startTime, endTime, conf, type) {
        rowIndex++;
        var idx = rowIndex;
        var isOps = (type === 'OPS');
        var tr = document.createElement('tr');
        tr.dataset.rowIndex = idx;
        tr.dataset.type = isOps ? 'OPS' : 'PERFORMANCE';
        if (isOps) tr.style.background = 'rgba(251,191,36,0.08)';
        var dateAttrs = '';
        if (festivalStartDate) dateAttrs += ' min="' + festivalStartDate + '"';
        if (festivalEndDate)   dateAttrs += ' max="' + festivalEndDate   + '"';

        var artistCell, stageCell;
        if (isOps) {
            artistCell = '<span style="font-size:11px;color:var(--warning-text);font-weight:600;margin-right:4px;">📢</span>' +
                         '<input type="text" data-field="artist" value="' + window.AdminUtils.escapeHtml(artist) + '" style="' + fieldStyle + '" placeholder="운영 항목명"/>';
            stageCell  = '<span style="color:var(--muted);font-size:11px;">운영 항목</span>' +
                         '<input type="hidden" data-field="stage" value="' + OPS_STAGE + '"/>';
        } else {
            artistCell = '<select data-field="artist" style="' + selectStyle + '">' + makeOptions(artistNames, artist) + '</select>';
            stageCell  = '<select data-field="stage"  style="' + selectStyle + '">' + makeOptions(stageNames,  stage)  + '</select>';
        }

        tr.innerHTML =
            '<td>' + idx + '</td>' +
            '<td>' + artistCell + '</td>' +
            '<td>' + stageCell  + '</td>' +
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
        appendRow('', '', defaultDate, '', '', null, 'PERFORMANCE');
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
            window.AdminCrawl.showApplyResult('error', '저장할 항목이 없습니다.');
            return false;
        }
        if (entries.some(function (e) { return !e.date; })) {
            window.AdminCrawl.showApplyResult('error', '날짜가 설정되지 않은 항목이 있습니다.<br>상단 "날짜 일괄 설정"을 사용하거나 각 행의 날짜를 직접 입력해주세요.');
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
            window.AdminCrawl.showApplyResult('success', result.savedCount + '개 모두 저장 완료');
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
        window.AdminCrawl.showApplyResult('partial', msg);
    }

    /* ── 타임테이블에 적용 ── */
    document.getElementById('btnApplyOcr').addEventListener('click', function () {
        var fid = ocrSelect.value;
        if (!fid) { window.AdminCrawl.showApplyResult('error', '페스티벌을 선택해주세요.'); return; }

        var entries = collectOcrEntries();
        if (!validateOcrEntries(entries)) return;

        // 현재 DOM 행 순서를 스냅샷으로 유지 (apply 후 성공/실패 구분에 사용)
        var allRows = Array.from(document.getElementById('ocrParseBody').querySelectorAll('tr'));

        var btn = document.getElementById('btnApplyOcr');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>저장 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        window.AdminUtils.requestJson(CrawlUrls.ocrApply, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ festivalId: parseInt(fid), entries: entries })
        }, '저장 실패')
        .then(function (result) {
            btn.disabled = false;
            btn.innerHTML = '타임테이블에 적용';
            renderOcrApplyResult(result, allRows);
            addHistory(currentFestivalTitle, formatDateRange(entries), result.savedCount, result.failedCount);
        })
        .catch(function (err) {
            btn.disabled = false;
            btn.innerHTML = '타임테이블에 적용';
            window.AdminCrawl.showApplyResult('error', '오류: ' + window.AdminUtils.escapeHtml(err.message));
        });
    });

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
})();
