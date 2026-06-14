(function () {
    /* ── 메모 위젯 (localStorage) ── */
    var MEMO_KEY = 'feple_admin_memos';

    function renderMemos() {
        var memos = JSON.parse(localStorage.getItem(MEMO_KEY) || '[]');
        var saved = document.getElementById('memo-saved');
        if (!memos.length) {
            saved.innerHTML = '<div class="memo-empty">저장된 메모가 없습니다.</div>';
            return;
        }
        saved.innerHTML = memos.map(function (m) {
            return '<div class="memo-chip">' +
                '<span class="memo-chip-text">' + window.AdminUtils.escapeHtml(m.text) + '</span>' +
                '<button class="memo-chip-del" data-id="' + m.id + '" title="삭제">×</button>' +
                '</div>';
        }).join('');
    }

    function addMemo() {
        var input = document.getElementById('memo-input');
        var text = input.value.trim();
        if (!text) { input.focus(); return; }
        var memos = JSON.parse(localStorage.getItem(MEMO_KEY) || '[]');
        memos.unshift({ id: Date.now(), text: text });
        localStorage.setItem(MEMO_KEY, JSON.stringify(memos));
        input.value = '';
        renderMemos();
    }

    function deleteMemo(id) {
        var memos = JSON.parse(localStorage.getItem(MEMO_KEY) || '[]').filter(function (m) { return m.id !== id; });
        localStorage.setItem(MEMO_KEY, JSON.stringify(memos));
        renderMemos();
    }

    function clearAllMemos() {
        localStorage.removeItem(MEMO_KEY);
        renderMemos();
    }

    /* Enter 키로 저장 */
    document.getElementById('memo-input').addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) addMemo();
    });

    document.getElementById('memo-add-btn').addEventListener('click', addMemo);

    /* 전체 삭제 — admin-confirm.js가 먼저 confirm을 처리하므로 document 레벨에서 순서 보장 */
    document.addEventListener('click', function (e) {
        if (e.defaultPrevented) return;
        var btn = e.target.closest('#memo-clear-btn');
        if (!btn) return;
        clearAllMemos();
    });

    /* 삭제 버튼 이벤트 위임 */
    document.getElementById('memo-saved').addEventListener('click', function (e) {
        var btn = e.target.closest('.memo-chip-del');
        if (btn) deleteMemo(Number(btn.dataset.id));
    });

    renderMemos();
})();
