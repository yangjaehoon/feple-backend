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
    return confirm('제목: ' + title + '\n\n해당 그룹에게 발송하시겠습니까?');
}

async function searchNickname() {
    const nickname = document.getElementById('nickname-search').value.trim();
    const resultEl = document.getElementById('search-result');
    if (!nickname) { resultEl.style.display = 'none'; return; }

    resultEl.style.display = 'block';
    resultEl.style.color = 'var(--muted)';
    resultEl.textContent = '검색 중...';

    try {
        const res = await fetch(PushUrls.searchUser + '?nickname=' + encodeURIComponent(nickname));
        const data = await res.json();
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
            resultEl.appendChild(document.createTextNode(' (ID: ' + data.id + ')  '));
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
    if (!confirm('사용자 ID ' + uid + ' 에게 테스트 발송하시겠습니까?')) return false;
    testForm.querySelector('[name="title"]').value = title;
    testForm.querySelector('[name="body"]').value  = body;
    return true;
}

function openBroadcastModal(btn) {
    document.getElementById('modal-title').textContent = btn.dataset.title;
    document.getElementById('modal-date').textContent  = btn.dataset.createdAt;
    document.getElementById('modal-body').textContent  = btn.dataset.body;
    const modal = document.getElementById('broadcast-modal');
    modal.style.display = 'flex';
}

function closeBroadcastModal() {
    document.getElementById('broadcast-modal').style.display = 'none';
}

document.getElementById('broadcast-modal').addEventListener('click', function (e) {
    if (e.target === this) closeBroadcastModal();
});

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeBroadcastModal();
});
