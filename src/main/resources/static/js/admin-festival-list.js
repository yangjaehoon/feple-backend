(function () {
window.AdminUtils.initTabs(['list', 'checklist']);

/* ── 체크박스 AJAX 저장 ── */
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

        var headers = window.AdminUtils.getCsrfHeaders();

        fetch(FestivalListUrls.festivalBase + '/' + festivalId + '/checklist?field=' + field, {
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
var memoTimers = {};
document.querySelectorAll('.memo-textarea').forEach(function(ta) {
    ta.addEventListener('input', function() {
        var festivalId = ta.dataset.festivalId;
        clearTimeout(memoTimers[festivalId]);
        memoTimers[festivalId] = setTimeout(function() { saveMemo(festivalId, ta.value); }, 800);
    });
    ta.addEventListener('blur', function() {
        var festivalId = ta.dataset.festivalId;
        clearTimeout(memoTimers[festivalId]);
        saveMemo(festivalId, ta.value);
    });
});

function saveMemo(festivalId, memo) {
    var headers = window.AdminUtils.getCsrfHeaders();
    var body = new URLSearchParams();
    body.append('memo', memo);
    fetch(FestivalListUrls.festivalBase + '/' + festivalId + '/checklist/memo', {
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
})();
