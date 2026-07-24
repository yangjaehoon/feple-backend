(function () {
    function showPushTargetError(msg) {
        var el = document.getElementById('push-target-error');
        el.textContent = msg;
        el.classList.remove('d-none');
    }

    function setSearchState(el, stateClass) {
        el.classList.remove('text-muted', 'text-primary', 'text-danger');
        if (stateClass) el.classList.add(stateClass);
    }

    function showSearchLoading(el) {
        setSearchState(el, 'text-muted');
        el.style.display = 'block';
        el.textContent   = '검색 중...';
    }

    function renderSearchSuccess(el, data) {
        setSearchState(el, 'text-primary');
        el.textContent = '';
        var nicknameEl = document.createElement('strong');
        nicknameEl.textContent = data.nickname;
        var btn = document.createElement('button');
        btn.type        = 'button';
        btn.className   = 'btn btn-secondary btn-sm';
        btn.textContent = '이 ID로 설정';
        btn.addEventListener('click', function () { fillUserId(data.id); });
        el.appendChild(nicknameEl);
        el.appendChild(document.createTextNode(' (ID: ' + data.id + ')  '));
        el.appendChild(btn);
    }

    function renderSearchError(el, msg) {
        setSearchState(el, 'text-danger');
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

    function openBroadcastModal(btn) {
        document.getElementById('modal-title').textContent = btn.dataset.title;
        document.getElementById('modal-date').textContent  = btn.dataset.createdAt;
        document.getElementById('modal-body').textContent  = btn.dataset.body;
        document.getElementById('broadcast-modal').style.display = 'flex';
    }

    function closeBroadcastModal() {
        document.getElementById('broadcast-modal').style.display = 'none';
    }

    /* 전체 발송 폼 — 제목/내용 미리보기 포함 확인 */
    var broadcastForm = document.getElementById('broadcast-form');
    if (broadcastForm) {
        broadcastForm.addEventListener('submit', function (e) {
            e.preventDefault();
            var title = this.querySelector('[name="title"]').value.trim();
            var body  = this.querySelector('[name="body"]').value.trim();
            var msg = '전체 기기에 푸시를 발송합니다.\n\n제목: ' + title + '\n내용: ' + body;
            var form = this;
            AdminConfirm.show(msg, function () {
                form.submit();
            });
        });
    }

    /* 타겟 발송 버튼 */
    document.querySelectorAll('.target-send-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var title = document.getElementById('target-card-title').value.trim();
            var body  = document.getElementById('target-card-body').value.trim();
            if (!title || !body) {
                showPushTargetError('제목과 내용을 먼저 입력해주세요.');
                return;
            }
            var targetForm = btn.closest('form');
            var selectEl = targetForm.querySelector('select');
            if (selectEl && !selectEl.value) {
                showPushTargetError(selectEl.name === 'artistId' ? '아티스트를 선택해주세요.' : '페스티벌을 선택해주세요.');
                return;
            }
            document.getElementById('push-target-error').classList.add('d-none');
            document.getElementById('target-title-' + btn.dataset.target).value = title;
            document.getElementById('target-body-'  + btn.dataset.target).value = body;
            AdminConfirm.show('해당 그룹에게 발송하시겠습니까?', function () {
                targetForm.submit();
            });
        });
    });

    document.getElementById('nickname-search-btn').addEventListener('click', searchNickname);

    document.getElementById('nickname-search').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); searchNickname(); }
    });

    /* 테스트 발송 버튼 */
    document.getElementById('test-send-btn').addEventListener('click', function (e) {
        e.preventDefault();
        var testForm = document.getElementById('test-form');
        var title = testForm.querySelector('[name="title"]').value.trim();
        var body  = testForm.querySelector('[name="body"]').value.trim();
        var uid   = testForm.querySelector('[name="targetUserId"]').value.trim();
        if (!title || !body) { showPushTestError('제목과 내용을 입력해주세요.'); return; }
        if (!uid)            { showPushTestError('테스트 대상 사용자 ID를 입력해주세요.'); return; }
        document.getElementById('push-test-error').classList.add('d-none');
        AdminConfirm.show('테스트 발송하시겠습니까?', function () {
            testForm.submit();
        });
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
