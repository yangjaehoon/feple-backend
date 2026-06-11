(function () {
    function previewArtistImage(input) {
        if (!input.files || !input.files[0]) return;
        var file = input.files[0];
        var errorEl = document.getElementById('img-size-error');
        if (file.size > 5 * 1024 * 1024) {
            if (errorEl) errorEl.style.display = 'block';
            input.value = '';
            return;
        }
        if (errorEl) errorEl.style.display = 'none';
        var reader = new FileReader();
        reader.onload = function (e) {
            var img = document.getElementById('preview-img');
            var ph  = document.getElementById('preview-ph');
            img.src = e.target.result;
            img.style.display = '';
            ph.style.display = 'none';
        };
        reader.readAsDataURL(file);
    }

    document.getElementById('profile-img-input').addEventListener('change', function () {
        previewArtistImage(this);
    });
})();
