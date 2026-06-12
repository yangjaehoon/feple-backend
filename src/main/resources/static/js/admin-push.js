(function () {
    function showPushTargetError(msg) {
        var el = document.getElementById('push-target-error');
        el.textContent = msg;
        el.classList.remove('d-none');
    }

    function fillTargetAndConfirm(type) {
        var title = document.getElementById('target-card-title').value.trim();
        var body  = document.getElementById('target-card-body').value.trim();
        if (!title || !body) {
            showPushTargetError('제목과 내용을 먼저 입력해주세요.');
            return false;
        }
        document.getElementById('push-target-error').classList.add('d-none');
        document.getElementById('target-title-' + type).value = title;
        document.getElementById('target-body-'  + type).value = body;
        return confirm('해당 그룹에게 발송하시겠습니까?');
    }

    function showSearchLoading(el) {
        el.style.display = 'block';
        el.style.color   = 'var(--muted)';
        el.textContent   = '검색 중...';
    }

    function renderSearchSuccess(el, data) {
        el.style.color = 'var(--primary)';
        el.textContent = '';
        var nicknameEl = document.createElement('strong');
        nicknameEl.textContent = data.nickname;
        var btn = document.createElement('button');
        btn.type          = 'button';
        btn.className     = 'btn btn-secondary';
        btn.style.cssText = 'font-size:12px; padding:2px 10px;';
        btn.textContent   = '이 ID로 설정';
        btn.addEventListener('click', function () { fillUserId(data.id); });
        el.appendChild(nicknameEl);
        el.appendChild(document.createTextNode(' (ID: ' + data.id + ')  '));
        el.appendChild(btn);
    }

    function renderSearchError(el, msg) {
        el.style.color = 'var(--danger, #dc3545)';
        el.textContent = msg;
    }

    async function searchNickname() {
        var nickname = document.getElementById('nickname-search').value.trim();
        var resultEl = document.getElementById('search-result');
        if (!nickname) { resultEl.style.display = 'none'; return; }

        showSearchLoading(resultEl);
        try {
            var res  = await fetch(PushUrls.searchUser + '?nickname=' + encodeURIComponent(nickname));
            var data = await res.json();
            if (res.ok) renderSearchSuccess(resultEl, data);
            else        renderSearchError(resultEl, data.error || '사용자를 찾을 수 없습니다.');
        } catch (e) {
            renderSearchError(resultEl, '검색 중 오류가 발생했습니다.');
        }
    }

    function fillUserId(id) {
        document.getElementById('target-user-id').value = id;
    }

    function showPushTestError(msg) {
        var el = document.getElementById('push-test-error');
        el.textContent = msg;
        el.classList.remove('d-none');
    }

    function validateTest(testForm) {
        var title = testForm.querySelector('[name="title"]').value.trim();
        var body  = testForm.querySelector('[name="body"]').value.trim();
        var uid   = testForm.querySelector('[name="targetUserId"]').value.trim();
        if (!title || !body) { showPushTestError('제목과 내용을 입력해주세요.'); return false; }
        if (!uid)            { showPushTestError('테스트 대상 사용자 ID를 입력해주세요.'); return false; }
        document.getElementById('push-test-error').classList.add('d-none');
        return confirm('테스트 발송하시겠습니까?');
    }

    function openBroadcastModal(btn) {
        document.getElementById('modal-title').textContent = btn.dataset.title;
        document.getElementById('modal-date').textContent  = btn.dataset.createdAt;
        document.getElementById('modal-body').textContent  = btn.dataset.body;
        document.getElementById('broadcast-modal').style.display = 'flex';
    }

    function closeBroadcastModal() {
        document.getElementById('broadcast-modal').style.display = 'none';
    }

    /* 전체 발송 폼 — data-confirm 처리는 admin-confirm.js에서 수행 */

    document.querySelectorAll('.target-send-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (!fillTargetAndConfirm(btn.dataset.target)) e.preventDefault();
        });
    });

    document.getElementById('nickname-search-btn').addEventListener('click', searchNickname);

    document.getElementById('nickname-search').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); searchNickname(); }
    });

    document.getElementById('test-send-btn').addEventListener('click', function (e) {
        if (!validateTest(document.getElementById('test-form'))) e.preventDefault();
    });

    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.broadcast-modal-btn');
        if (btn) openBroadcastModal(btn);
    });

    document.getElementById('modal-close-btn').addEventListener('click', closeBroadcastModal);
    document.getElementById('modal-footer-close-btn').addEventListener('click', closeBroadcastModal);

    document.getElementById('broadcast-modal').addEventListener('click', function (e) {
        if (e.target === this) closeBroadcastModal();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeBroadcastModal();
    });
})();
