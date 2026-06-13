(function () {
    document.getElementById('profile-img-input').addEventListener('change', function () {
        AdminUtils.readImageAsDataUrl(this, function (dataUrl) {
            var img = document.getElementById('preview-img');
            var ph  = document.getElementById('preview-ph');
            img.src = dataUrl;
            img.classList.remove('d-none');
            ph.style.display = 'none';
        }, 5, 'img-size-error');
    });
})();
