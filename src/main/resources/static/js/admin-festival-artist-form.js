/* 서버에서 data-msg 속성으로 전달된 에러 메시지를 alert으로 표시 */
(function() {
    var errEl = document.getElementById('js-page-error');
    if (errEl) alert(errEl.dataset.msg);
})();

function filterArtists(query) {
    const q = query.toLowerCase();
    document.querySelectorAll('#artistList .artist-item').forEach(item => {
        item.style.display = item.dataset.name.includes(q) ? '' : 'none';
    });
}
function updateCount() {
    const count = document.querySelectorAll('input[name="artistIds"]:checked').length;
    document.getElementById('selectedCount').textContent = count;
}
document.addEventListener('DOMContentLoaded', updateCount);
