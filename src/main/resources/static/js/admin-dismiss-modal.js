(function () {
    function openDismissModal(btn) {
        var id     = btn.dataset.id;
        var artist = btn.dataset.artist;
        document.getElementById('dismiss-artist-name').textContent = '아티스트: ' + artist;
        document.getElementById('dismiss-form').action = window.ArtistSuggestionUrls.dismissBase + '/' + id + '/dismiss';
        document.getElementById('dismiss-note').value = '';
        document.getElementById('dismiss-process-note').value = '';
        document.querySelectorAll('.reason-chip').forEach(function (c) { c.classList.remove('active'); });
        document.getElementById('dismiss-modal').style.display = 'flex';
    }

    function closeDismissModal() {
        document.getElementById('dismiss-modal').style.display = 'none';
    }

    /* 처리하기 버튼 — 이벤트 위임 */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.open-dismiss-btn');
        if (btn) openDismissModal(btn);
    });

    /* 취소 버튼 */
    document.getElementById('dismiss-close-btn').addEventListener('click', closeDismissModal);

    document.getElementById('dismiss-modal').addEventListener('click', function (e) {
        if (e.target === this) closeDismissModal();
    });

    document.querySelectorAll('.reason-chip').forEach(function (chip) {
        chip.addEventListener('click', function () {
            document.querySelectorAll('.reason-chip').forEach(function (c) { c.classList.remove('active'); });
            this.classList.add('active');
            document.getElementById('dismiss-note').value = this.dataset.reason;
        });
    });

    document.getElementById('dismiss-form').addEventListener('submit', function () {
        document.getElementById('dismiss-process-note').value =
            document.getElementById('dismiss-note').value.trim();
    });
})();
