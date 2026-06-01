function onRoleChange(role) {
    const section = document.getElementById('perm-section');
    section.style.display = role === 'SUPER_ADMIN' ? 'none' : '';
}

function previewPhoto(input) {
    if (!input.files || !input.files[0]) return;
    document.getElementById('delete-profile-image-flag').value = 'false';
    var reader = new FileReader();
    reader.onload = function(e) {
        var preview = document.getElementById('photo-preview');
        preview.innerHTML = '<img src="' + e.target.result + '" style="width:100%; height:100%; object-fit:cover;" alt="미리보기"/>';
    };
    reader.readAsDataURL(input.files[0]);
}

function confirmDeletePhoto() {
    if (!confirm('프로필 사진을 삭제할까요?')) return;
    document.getElementById('delete-profile-image-flag').value = 'true';
    document.getElementById('profile-image-input').value = '';
    var preview = document.getElementById('photo-preview');
    preview.innerHTML = '<img src="/img/feple_logo.png" style="width:60%; height:60%; object-fit:contain; opacity:.6;" alt="feple"/>';
    var btn = document.getElementById('delete-photo-btn');
    if (btn) btn.style.display = 'none';
}

// 초기화
(function() {
    const checked = document.querySelector('input[name="role"]:checked');
    if (checked) onRoleChange(checked.value);
})();
