/* ── 메모 위젯 (localStorage) ── */
const MEMO_KEY = 'feple_admin_memos';

function escHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function renderMemos() {
    const memos = JSON.parse(localStorage.getItem(MEMO_KEY) || '[]');
    const saved = document.getElementById('memo-saved');
    if (!memos.length) { saved.innerHTML = '<div style="color:var(--muted);font-size:12px;text-align:center;padding:8px 0;">저장된 메모가 없습니다.</div>'; return; }
    saved.innerHTML = memos.map(m =>
        `<div class="memo-chip">
            <span class="memo-chip-text">${escHtml(m.text)}</span>
            <button class="memo-chip-del" onclick="deleteMemo(${m.id})" title="삭제">×</button>
        </div>`
    ).join('');
}
function addMemo() {
    const input = document.getElementById('memo-input');
    const text = input.value.trim();
    if (!text) { input.focus(); return; }
    const memos = JSON.parse(localStorage.getItem(MEMO_KEY) || '[]');
    memos.unshift({ id: Date.now(), text });
    localStorage.setItem(MEMO_KEY, JSON.stringify(memos));
    input.value = '';
    renderMemos();
}
function deleteMemo(id) {
    const memos = JSON.parse(localStorage.getItem(MEMO_KEY) || '[]').filter(m => m.id !== id);
    localStorage.setItem(MEMO_KEY, JSON.stringify(memos));
    renderMemos();
}
function clearAllMemos() {
    if (!confirm('메모를 모두 삭제할까요?')) return;
    localStorage.removeItem(MEMO_KEY);
    renderMemos();
}
/* Enter 키로 저장 */
document.getElementById('memo-input').addEventListener('keydown', function(e) {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) addMemo();
});
renderMemos();
