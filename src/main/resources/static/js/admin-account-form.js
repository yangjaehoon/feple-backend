(function () {
    function onRoleChange(role) {
        var section = document.getElementById('perm-section');
        if (!section) return;
        section.style.display = role === 'SUPER_ADMIN' ? 'none' : '';
    }

    function previewPhoto(input) {
        if (!input.files || !input.files[0]) return;
        document.getElementById('delete-profile-image-flag').value = 'false';
        var reader = new FileReader();
        reader.onload = function (e) {
            var preview = document.getElementById('photo-preview');
            preview.innerHTML = '<img src="' + e.target.result + '" style="width:100%; height:100%; object-fit:cover;" alt="미리보기"/>';
        };
        reader.readAsDataURL(input.files[0]);
    }

    function deletePhoto() {
        document.getElementById('delete-profile-image-flag').value = 'true';
        document.getElementById('profile-image-input').value = '';
        var preview = document.getElementById('photo-preview');
        preview.innerHTML = '<img src="/img/feple_logo.png" style="width:60%; height:60%; object-fit:contain; opacity:.6;" alt="feple"/>';
        var btn = document.getElementById('delete-photo-btn');
        if (btn) btn.style.display = 'none';
    }

    /* 파일 입력 change */
    document.getElementById('profile-image-input').addEventListener('change', function () {
        previewPhoto(this);
    });

    /* 사진 업로드 버튼 */
    document.getElementById('upload-photo-btn').addEventListener('click', function () {
        document.getElementById('profile-image-input').click();
    });

    /* 사진 삭제 버튼 — admin-confirm.js가 먼저 confirm을 처리 */
    document.addEventListener('click', function (e) {
        if (e.defaultPrevented) return;
        var btn = e.target.closest('#delete-photo-btn');
        if (!btn) return;
        deletePhoto();
    });

    /* 역할 선택 change */
    document.getElementById('role-group').addEventListener('change', function (e) {
        if (e.target.name === 'role') onRoleChange(e.target.value);
    });

    /* 전체 선택 / 전체 해제 버튼 */
    document.getElementById('perm-select-all-btn').addEventListener('click', function () {
        document.querySelectorAll('#perm-section input[type=checkbox]').forEach(function (c) { c.checked = true; });
    });
    document.getElementById('perm-deselect-all-btn').addEventListener('click', function () {
        document.querySelectorAll('#perm-section input[type=checkbox]').forEach(function (c) { c.checked = false; });
    });

    /* 초기화 */
    var checked = document.querySelector('input[name="role"]:checked');
    if (checked) onRoleChange(checked.value);
})();
