(function () {
    function onRoleChange(role) {
        var section = document.getElementById('perm-section');
        if (!section) return;
        section.style.display = role === 'SUPER_ADMIN' ? 'none' : '';
    }

    function setPreviewImage(src, className, alt) {
        var img = document.createElement('img');
        img.src = src;
        img.alt = alt;
        img.className = className;
        var preview = document.getElementById('photo-preview');
        preview.innerHTML = '';
        preview.appendChild(img);
    }

    function previewPhoto(input) {
        document.getElementById('delete-profile-image-flag').value = 'false';
        AdminUtils.readImageAsDataUrl(input, function (dataUrl) {
            setPreviewImage(dataUrl, 'profile-preview-img', '미리보기');
        });
    }

    function deletePhoto() {
        document.getElementById('delete-profile-image-flag').value = 'true';
        document.getElementById('profile-image-input').value = '';
        setPreviewImage('/img/feple_logo.png', 'profile-preview-placeholder', 'feple');
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

    /* 비밀번호 표시/숨기기 토글 */
    (function () {
        var pwInput = document.getElementById('password-input');
        var toggle = document.getElementById('pw-toggle-btn');
        if (!pwInput || !toggle) return;
        toggle.addEventListener('click', function () {
            var visible = pwInput.type === 'text';
            pwInput.type = visible ? 'password' : 'text';
            toggle.textContent = visible ? '표시' : '숨기기';
        });
    })();

    /* 초기화 */
    var checked = document.querySelector('input[name="role"]:checked');
    if (checked) onRoleChange(checked.value);

    /* 페이지 이탈 경고 */
    AdminUtils.initDirtyGuard();
})();
