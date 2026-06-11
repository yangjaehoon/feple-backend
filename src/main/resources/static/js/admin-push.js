(function () {
    function showPushTargetError(msg) {
        var el = document.getElementById('push-target-error');
        el.textContent = msg;
        el.style.display = 'block';
    }

    function fillTargetAndConfirm(type) {
        var title = document.querySelector('[name="title"]').value.trim();
        var body  = document.querySelector('[name="body"]').value.trim();
        if (!title || !body) {
            showPushTargetError('제목과 내용을 먼저 입력해주세요.');
            return false;
        }
        document.getElementById('push-target-error').style.display = 'none';
        document.getElementById('target-title-' + type).value = title;
        document.getElementById('target-body-'  + type).value = body;
        return confirm('해당 그룹에게 발송하시겠습니까?');
    }

    async function searchNickname() {
        var nickname = document.getElementById('nickname-search').value.trim();
        var resultEl = document.getElementById('search-result');
        if (!nickname) { resultEl.style.display = 'none'; return; }

        resultEl.style.display = 'block';
        resultEl.style.color = 'var(--muted)';
        resultEl.textContent = '검색 중...';

        try {
            var res = await fetch(PushUrls.searchUser + '?nickname=' + encodeURIComponent(nickname));
            var data = await res.json();
            if (res.ok) {
                resultEl.style.color = 'var(--primary)';
                resultEl.textContent = '';
                var strong = document.createElement('strong');
                strong.textContent = data.nickname;
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn btn-secondary';
                btn.style.cssText = 'font-size:12px; padding:2px 10px;';
                btn.textContent = '이 ID로 설정';
                btn.addEventListener('click', function () { fillUserId(data.id); });
                resultEl.appendChild(strong);
                resultEl.appendChild(document.createTextNode(' (ID: ' + data.id + ')  '));
                resultEl.appendChild(btn);
            } else {
                resultEl.style.color = 'var(--danger, #dc3545)';
                resultEl.textContent = data.error || '사용자를 찾을 수 없습니다.';
            }
        } catch (e) {
            resultEl.style.color = 'var(--danger, #dc3545)';
            resultEl.textContent = '검색 중 오류가 발생했습니다.';
        }
    }

    function fillUserId(id) {
        document.getElementById('target-user-id').value = id;
    }

    function showPushTestError(msg) {
        var el = document.getElementById('push-test-error');
        el.textContent = msg;
        el.style.display = 'block';
    }

    function validateTest(testForm) {
        var broadcastForm = document.getElementById('broadcast-form');
        var title = broadcastForm.querySelector('[name="title"]').value.trim();
        var body  = broadcastForm.querySelector('[name="body"]').value.trim();
        var uid   = testForm.querySelector('[name="targetUserId"]').value.trim();
        if (!title || !body) { showPushTestError('제목과 내용을 입력해주세요.'); return false; }
        if (!uid)            { showPushTestError('테스트 대상 사용자 ID를 입력해주세요.'); return false; }
        document.getElementById('push-test-error').style.display = 'none';
        if (!confirm('테스트 발송하시겠습니까?')) return false;
        testForm.querySelector('[name="title"]').value = title;
        testForm.querySelector('[name="body"]').value  = body;
        return true;
    }

    function openBroadcastModal(btn) {
        document.getElementById('modal-title').textContent = btn.dataset.title;
        document.getElementById('modal-date').textContent  = btn.dataset.createdAt;
        document.getElementById('modal-body').textContent  = btn.dataset.body;
        var modal = document.getElementById('broadcast-modal');
        modal.style.display = 'flex';
    }

    function closeBroadcastModal() {
        document.getElementById('broadcast-modal').style.display = 'none';
    }

    /* 전체 발송 폼 — data-confirm 처리는 admin-confirm.js에서 수행 */
    /* (form에 data-confirm 속성 추가됨) */

    /* 타겟 발송 버튼 */
    document.querySelectorAll('.target-send-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            if (!fillTargetAndConfirm(btn.dataset.target)) e.preventDefault();
        });
    });

    /* 닉네임 검색 버튼 */
    document.getElementById('nickname-search-btn').addEventListener('click', searchNickname);

    /* 닉네임 검색 Enter 키 */
    document.getElementById('nickname-search').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); searchNickname(); }
    });

    /* 테스트 발송 버튼 */
    document.getElementById('test-send-btn').addEventListener('click', function (e) {
        if (!validateTest(document.getElementById('test-form'))) e.preventDefault();
    });

    /* 공지 내용 전체보기 모달 열기 — 이벤트 위임 */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.broadcast-modal-btn');
        if (btn) openBroadcastModal(btn);
    });

    /* 모달 닫기 */
    document.getElementById('modal-close-btn').addEventListener('click', closeBroadcastModal);
    document.getElementById('modal-footer-close-btn').addEventListener('click', closeBroadcastModal);

    document.getElementById('broadcast-modal').addEventListener('click', function (e) {
        if (e.target === this) closeBroadcastModal();
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeBroadcastModal();
    });
})();
