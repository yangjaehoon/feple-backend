(function () {
    /* 서버에서 data-msg 속성으로 전달된 에러 메시지를 alert으로 표시 */
    var errEl = document.getElementById('js-page-error');
    if (errEl) alert(errEl.dataset.msg);

    function filterArtists(query) {
        var q = query.toLowerCase();
        document.querySelectorAll('#artistList .artist-item').forEach(function (item) {
            item.style.display = item.dataset.name.includes(q) ? '' : 'none';
        });
    }

    function updateCount() {
        var count = document.querySelectorAll('input[name="artistIds"]:checked').length;
        document.getElementById('selectedCount').textContent = count;
    }

    document.getElementById('artistSearch').addEventListener('input', function () {
        filterArtists(this.value);
    });

    document.getElementById('artistList').addEventListener('change', function (e) {
        if (e.target.name === 'artistIds') updateCount();
    });

    document.addEventListener('DOMContentLoaded', updateCount);
})();
