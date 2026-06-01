function previewArtistImage(input) {
    if (!input.files || !input.files[0]) return;
    var file = input.files[0];
    if (file.size > 5 * 1024 * 1024) {
        alert('파일 크기가 5MB를 초과합니다.');
        input.value = '';
        return;
    }
    var reader = new FileReader();
    reader.onload = function(e) {
        var img = document.getElementById('preview-img');
        var ph  = document.getElementById('preview-ph');
        img.src = e.target.result;
        img.style.display = '';
        ph.style.display = 'none';
    };
    reader.readAsDataURL(file);
}
