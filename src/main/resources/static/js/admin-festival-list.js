/* ── 탭 전환 ── */
function switchTab(tabName) {
    document.querySelectorAll('.header-tab-btn').forEach(function(b) { b.classList.remove('active'); });
    document.querySelectorAll('.tab-panel').forEach(function(p) { p.classList.remove('active'); });
    var btn = document.querySelector('.header-tab-btn[data-tab="' + tabName + '"]');
    if (btn) btn.classList.add('active');
    var panel = document.getElementById('tab-' + tabName);
    if (panel) panel.classList.add('active');
    history.replaceState(null, '', window.location.pathname + window.location.search + '#' + tabName);
}

document.querySelectorAll('.header-tab-btn').forEach(function(btn) {
    btn.addEventListener('click', function() { switchTab(btn.dataset.tab); });
});

/* 페이지 로드 시 해시로 탭 복원 */
(function() {
    var hash = window.location.hash.replace('#', '');
    if (hash === 'list' || hash === 'checklist') switchTab(hash);
})();

/* ── 체크박스 AJAX 저장 ── */
var csrfToken  = document.querySelector('meta[name="_csrf"]').content;
var csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

var toastTimer;
function showToast(isError, msg) {
    var t = document.getElementById('saveToast');
    if (isError) {
        t.textContent = msg || '저장 실패';
        t.classList.add('error');
    } else {
        t.textContent = '저장됨';
        t.classList.remove('error');
    }
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function() { t.classList.remove('show'); }, 1500);
}

document.querySelectorAll('.cl-check').forEach(function(cb) {
    cb.addEventListener('change', function() {
        var festivalId = cb.dataset.festivalId;
        var field      = cb.dataset.field;
        var checked    = cb.checked;

        var headers = {};
        headers[csrfHeader] = csrfToken;

        fetch('/admin/festivals/' + festivalId + '/checklist?field=' + field, {
            method: 'POST',
            headers: headers
        })
        .then(function(res) {
            if (!res.ok) throw new Error();
            return res.json();
        })
        .then(function(data) {
            cb.checked = data.checked;
            updateProgress(cb.closest('tr'));
            showToast(false);
        })
        .catch(function() {
            cb.checked = !checked;
            showToast(true, '저장 실패. 다시 시도해주세요.');
        });
    });
});

/* ── 메모 자동 저장 (blur) ── */
var memoTimer = {};
document.querySelectorAll('.memo-textarea').forEach(function(ta) {
    ta.addEventListener('input', function() {
        var festivalId = ta.dataset.festivalId;
        clearTimeout(memoTimer[festivalId]);
        memoTimer[festivalId] = setTimeout(function() { saveMemo(festivalId, ta.value); }, 800);
    });
    ta.addEventListener('blur', function() {
        var festivalId = ta.dataset.festivalId;
        clearTimeout(memoTimer[festivalId]);
        saveMemo(festivalId, ta.value);
    });
});

function saveMemo(festivalId, memo) {
    var headers = {};
    headers[csrfHeader] = csrfToken;
    var body = new URLSearchParams();
    body.append('memo', memo);
    fetch('/admin/festivals/' + festivalId + '/checklist/memo', {
        method: 'POST',
        headers: headers,
        body: body
    }).then(function(res) {
        if (res.ok) showToast(false);
        else showToast(true, '메모 저장에 실패했습니다.');
    }).catch(function() {
        showToast(true, '메모 저장에 실패했습니다.');
    });
}

function updateProgress(row) {
    var checks = row.querySelectorAll('.cl-check');
    var done   = Array.from(checks).filter(function(c) { return c.checked; }).length;
    var badge  = row.querySelector('.status-done, .status-pend');
    if (!badge) return;
    badge.textContent = done + '/5';
    if (done === 5) {
        badge.className = 'status-done';
        row.classList.add('all-done');
    } else {
        badge.className = 'status-pend';
        row.classList.remove('all-done');
    }
}
