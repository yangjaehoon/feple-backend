function openDismissModal(btn) {
    const id     = btn.dataset.id;
    const artist = btn.dataset.artist;
    document.getElementById('dismiss-artist-name').textContent = '아티스트: ' + artist;
    document.getElementById('dismiss-form').action = '/admin/artists/suggestions/' + id + '/dismiss';
    document.getElementById('dismiss-note').value = '';
    document.getElementById('dismiss-process-note').value = '';
    document.querySelectorAll('.reason-chip').forEach(function (c) { c.classList.remove('active'); });
    const modal = document.getElementById('dismiss-modal');
    modal.style.display = 'flex';
}

function closeDismissModal() {
    document.getElementById('dismiss-modal').style.display = 'none';
}

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
