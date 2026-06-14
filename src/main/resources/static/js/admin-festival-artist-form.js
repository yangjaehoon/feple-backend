(function () {
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
