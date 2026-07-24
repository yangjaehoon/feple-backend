(function () {
    // ════════════════════════════════════════════════════════
    //  크롤링 대시보드 공용 인프라 — 페스티벌 콤보박스, 토스트, quota, 파일 미리보기
    //  admin-crawl-scrape.js / admin-crawl-timetable-ocr.js / admin-crawl-lineup-ocr.js 보다 먼저 로드되어야 함
    // ════════════════════════════════════════════════════════

    var festivalMap = {}; // id → {title, startDate, endDate}

    // ── 검색 가능 페스티벌 콤보박스 ────────────────────────────
    function FestivalCombobox(elId) {
        var root = document.getElementById(elId);
        root.classList.add('fest-combobox');

        var input = document.createElement('input');
        input.type = 'text';
        input.className = 'fest-cb-input';
        input.placeholder = '페스티벌 검색...';
        input.autocomplete = 'off';

        var dropdown = document.createElement('div');
        dropdown.className = 'fest-cb-dropdown';

        root.appendChild(input);
        root.appendChild(dropdown);

        this._root     = root;
        this._input    = input;
        this._dropdown = dropdown;
        this._value    = '';
        this._items    = [];
        this._changeHandlers = [];

        var self = this;
        input.addEventListener('input', function () { self._filter(); self._open(); });
        input.addEventListener('focus', function () { self._filter(); self._open(); });
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') { self._close(); input.blur(); }
        });
        document.addEventListener('mousedown', function (e) {
            if (!root.contains(e.target)) self._close();
        });
    }

    FestivalCombobox.prototype.populate = function (festivals) {
        this._items = festivals;
        this._renderList(festivals);
    };

    FestivalCombobox.prototype._filter = function () {
        var q = this._input.value.trim().toLowerCase();
        var filtered = q
            ? this._items.filter(function (f) { return f.title.toLowerCase().indexOf(q) !== -1; })
            : this._items;
        this._renderList(filtered);
    };

    FestivalCombobox.prototype._renderList = function (items) {
        var self = this;
        this._dropdown.innerHTML = '';
        if (items.length === 0) {
            var empty = document.createElement('div');
            empty.className = 'fest-cb-empty';
            empty.textContent = '검색 결과가 없습니다.';
            this._dropdown.appendChild(empty);
            return;
        }
        items.forEach(function (f) {
            var item = document.createElement('div');
            item.className = 'fest-cb-item' + (String(f.id) === self._value ? ' selected' : '');
            item.textContent = f.title;
            item.addEventListener('mousedown', function (e) {
                e.preventDefault();
                self._select(f);
            });
            self._dropdown.appendChild(item);
        });
    };

    FestivalCombobox.prototype._select = function (f) {
        this._value = String(f.id);
        this._input.value = f.title;
        this._close();
        this._root.classList.remove('select-error');
        var self = this;
        this._changeHandlers.forEach(function (fn) { fn.call({ value: self._value }); });
    };

    FestivalCombobox.prototype._open = function () {
        this._dropdown.classList.add('open');
    };

    FestivalCombobox.prototype._close = function () {
        this._dropdown.classList.remove('open');
    };

    Object.defineProperty(FestivalCombobox.prototype, 'value', {
        get: function () { return this._value; }
    });

    FestivalCombobox.prototype.focus = function () { this._input.focus(); };

    FestivalCombobox.prototype.setError = function (hasError) {
        this._root.classList.toggle('select-error', hasError);
    };

    FestivalCombobox.prototype.addEventListener = function (type, fn) {
        if (type === 'change') this._changeHandlers.push(fn);
    };

    var ocrSelect    = new FestivalCombobox('selFestival');
    var lineupSelect = new FestivalCombobox('selLineupFestival');

    window.AdminUtils.initTabs();

    /* ── Gemini 사용량 ── */
    function loadQuota() {
        fetch(CrawlUrls.quota)
            .then(function (r) { return r.json(); })
            .then(function (q) { renderQuota(q); })
            .catch(function () {});
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

    /* ── 페스티벌 목록 로드 ── */
    fetch(CrawlUrls.festivals)
        .then(function (r) { return r.json(); })
        .then(function (festivals) {
            festivals.forEach(function (f) { festivalMap[f.id] = f; });
            ocrSelect.populate(festivals);
            lineupSelect.populate(festivals);
        })
        .catch(function () {});

    /* ── 공용 토스트 (스크래핑/타임테이블 OCR/라인업 OCR 결과 안내, HTML 콘텐츠 허용) ── */
    var _toastTimer = null;

    function showApplyResult(type, msg) {
        var toast = document.getElementById('applyToast');
        document.getElementById('applyToastBody').innerHTML = msg;
        toast.className = type + ' visible';
        clearTimeout(_toastTimer);
        _toastTimer = setTimeout(hideToast, 5000);
    }

    function hideToast() {
        document.getElementById('applyToast').classList.remove('visible');
    }

    document.getElementById('applyToastClose').addEventListener('click', hideToast);

    /* ── OCR 업로드 파일 미리보기 (타임테이블/라인업 공용) ── */
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

    window.AdminCrawl = {
        FestivalCombobox:   FestivalCombobox,
        ocrSelect:          ocrSelect,
        lineupSelect:       lineupSelect,
        festivalMap:        festivalMap,
        loadQuota:          loadQuota,
        showApplyResult:    showApplyResult,
        hideToast:          hideToast,
        showUploadedPreview: showUploadedPreview
    };
})();
