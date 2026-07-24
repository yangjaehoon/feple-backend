(function () {
    // ════════════════════════════════════════════════════════
    //  라인업 OCR 탭
    // ════════════════════════════════════════════════════════

    var lineupSelect = window.AdminCrawl.lineupSelect;
    var lineupPendingFile = null; // 파싱 대기 중인 파일

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
        window.AdminCrawl.showUploadedPreview('lineupDropZone', 'lineupFileInput', file);

        var reader = new FileReader();
        reader.onload = function (e) { document.getElementById('lineupPreviewImg').src = e.target.result; };
        reader.readAsDataURL(file);
        window.AdminCrawl.hideToast();
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
                return window.AdminUtils.parseJsonOrThrow(r, '파싱 실패');
            })
            .then(function (data) {
                setTimeout(function () { document.getElementById('lineupProgress').classList.remove('visible'); }, 500);
                renderLineupResults(data.entries, data.truncated);
                window.AdminCrawl.loadQuota();
            })
            .catch(function (err) {
                clearInterval(timer);
                document.getElementById('lineupProgress').classList.remove('visible');
                window.AdminCrawl.showApplyResult('error', '오류: ' + window.AdminUtils.escapeHtml(err.message));
            });
    }

    function setLineupFestivalError(msg) {
        document.getElementById('lineupFestivalError').textContent = msg;
        lineupSelect.setError(msg.length > 0);
    }

    function setLineupStartError(msg) {
        document.getElementById('lineupStartError').textContent = msg;
    }

    lineupSelect.addEventListener('change', function () {
        if (this.value) setLineupFestivalError('');
    });

    document.getElementById('btnStartLineup').addEventListener('click', function () {
        var fid = lineupSelect.value;
        if (!fid) {
            setLineupFestivalError('페스티벌을 선택해주세요.');
            lineupSelect.focus();
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

    function renderLineupResults(results, truncated) {
        var body = document.getElementById('lineupParseBody');
        body.innerHTML = '';
        var hasUnmatched = false;

        document.getElementById('lineupTruncatedBox').classList.toggle('d-none', !truncated);

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
        var fid = lineupSelect.value;
        if (!fid) { window.AdminCrawl.showApplyResult('error', '페스티벌을 선택해주세요.'); return; }

        var artistIds = [];
        document.querySelectorAll('.lineup-chk:checked:not([disabled])').forEach(function (chk) {
            var id = parseInt(chk.getAttribute('data-artist-id'));
            if (id) artistIds.push(id);
        });
        if (artistIds.length === 0) { window.AdminCrawl.showApplyResult('error', '등록할 아티스트를 선택해주세요.'); return; }

        /* 미매칭 이름 수집 (체크박스 disabled = 미매칭 행) */
        var unmatchedNames = [];
        document.querySelectorAll('#lineupParseBody tr').forEach(function (tr) {
            var chk = tr.querySelector('.lineup-chk');
            if (chk && chk.disabled && !chk.getAttribute('data-artist-id')) {
                var nameTd = tr.querySelectorAll('td')[1];
                if (nameTd) unmatchedNames.push(nameTd.textContent.trim());
            }
        });

        var btn = document.getElementById('btnApplyLineup');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>등록 중...';

        var headers = Object.assign({ 'Content-Type': 'application/json' }, window.AdminUtils.getCsrfHeaders());

        window.AdminUtils.requestJson(CrawlUrls.lineupOcrApply, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ festivalId: parseInt(fid), artistIds: artistIds, unmatchedNames: unmatchedNames })
        }, '등록 실패')
        .then(function (result) {
            btn.disabled = false;
            btn.innerHTML = '참여 아티스트로 등록';
            var msg = result.added + '명 등록 완료';
            if (result.duplicates > 0) msg += ' (' + result.duplicates + '명 이미 등록됨)';
            msg += '. <a href="' + CrawlUrls.festivalDetailBase + '/' + fid + '#artists" style="color:var(--primary);text-decoration:underline;font-weight:700;">페스티벌 확인 →</a>';
            if (unmatchedNames.length > 0) {
                msg += ' · 미매칭 ' + unmatchedNames.length + '명 제안 저장됨.';
                loadSuggestions();
            }
            window.AdminCrawl.showApplyResult('success', msg);
        })
        .catch(function (err) {
            btn.disabled = false;
            btn.innerHTML = '참여 아티스트로 등록';
            window.AdminCrawl.showApplyResult('error', '오류: ' + window.AdminUtils.escapeHtml(err.message));
        });
    });

    /* ── 미매칭 아티스트 등록 제안 ── */

    function loadSuggestions() {
        fetch(CrawlUrls.lineupSuggestions)
            .then(function (r) { return r.json(); })
            .then(function (list) { renderSuggestions(list); })
            .catch(function () { /* 조용히 실패 */ });
    }

    function renderSuggestions(list) {
        var badge  = document.getElementById('suggestionBadge');
        var empty  = document.getElementById('suggestionEmptyState');
        var table  = document.getElementById('suggestionTable');
        var body   = document.getElementById('suggestionBody');

        body.innerHTML = '';

        if (!list || list.length === 0) {
            badge.classList.add('d-none');
            table.classList.add('d-none');
            empty.classList.remove('d-none');
            return;
        }

        badge.textContent = list.length;
        badge.classList.remove('d-none');
        empty.classList.add('d-none');
        table.classList.remove('d-none');

        list.forEach(function (item) {
            var tr = document.createElement('tr');
            tr.setAttribute('data-suggestion-id', item.id);
            tr.innerHTML =
                '<td><strong>' + window.AdminUtils.escapeHtml(item.name) + '</strong></td>' +
                '<td><span class="badge-count">' + item.mentionCount + '회</span></td>' +
                '<td>' +
                  '<a href="' + CrawlUrls.artistCreateBase + '?name=' + encodeURIComponent(item.name) + '" ' +
                     'class="btn btn-sm btn-primary" target="_blank">아티스트 등록</a> ' +
                  '<button class="btn btn-sm btn-danger btn-delete-suggestion" data-id="' + item.id + '">삭제</button>' +
                '</td>';
            body.appendChild(tr);
        });
    }

    document.getElementById('suggestionBody').addEventListener('click', function (e) {
        var btn = e.target.closest('.btn-delete-suggestion');
        if (!btn) return;
        var id = btn.getAttribute('data-id');
        var headers = window.AdminUtils.getCsrfHeaders();
        btn.disabled = true;
        fetch(CrawlUrls.lineupSuggestions + '/' + id, { method: 'DELETE', headers: headers })
            .then(function (r) {
                if (!r.ok) throw new Error();
                var tr = document.querySelector('#suggestionBody tr[data-suggestion-id="' + id + '"]');
                if (tr) tr.remove();
                var remaining = document.querySelectorAll('#suggestionBody tr').length;
                if (remaining === 0) {
                    document.getElementById('suggestionBadge').classList.add('d-none');
                    document.getElementById('suggestionTable').classList.add('d-none');
                    document.getElementById('suggestionEmptyState').classList.remove('d-none');
                } else {
                    document.getElementById('suggestionBadge').textContent = remaining;
                }
            })
            .catch(function () { btn.disabled = false; });
    });

    document.getElementById('btnRefreshSuggestions').addEventListener('click', loadSuggestions);

    /* 라인업 탭 활성화 시 제안 목록 로드 */
    document.querySelectorAll('.header-tab-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            if (this.getAttribute('data-tab') === 'lineup') loadSuggestions();
        });
    });

    /* 라인업 탭이 기본 활성화 상태이면 즉시 로드 */
    (function () {
        var active = document.querySelector('.header-tab-btn.active');
        if (active && active.getAttribute('data-tab') === 'lineup') loadSuggestions();
    })();
})();
