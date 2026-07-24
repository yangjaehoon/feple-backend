(function () {
    var pendingAction = null;
    var previousFocus = null;

    function setButtonLoading(btn) {
        if (!btn) return;
        btn.disabled = true;
        btn.classList.add('btn-submitting');
        btn.dataset.origText = btn.textContent;
        btn.textContent = '처리 중...';
    }

    function showModal(message, action) {
        var modal = document.getElementById('adminConfirmModal');
        if (!modal) {
            if (window.confirm(message)) action();
            return;
        }
        previousFocus = document.activeElement;
        document.getElementById('adminConfirmMsg').textContent = message;
        pendingAction = action;
        modal.style.display = 'flex';
        var cancelBtn = document.getElementById('adminConfirmCancel');
        if (cancelBtn) cancelBtn.focus();
    }

    function hideModal() {
        var modal = document.getElementById('adminConfirmModal');
        if (modal) modal.style.display = 'none';
        pendingAction = null;
        if (previousFocus) { previousFocus.focus(); previousFocus = null; }
    }

    document.addEventListener('DOMContentLoaded', function () {
        var cancelBtn = document.getElementById('adminConfirmCancel');
        var okBtn = document.getElementById('adminConfirmOk');
        var modal = document.getElementById('adminConfirmModal');

        if (!modal) return;

        if (cancelBtn) cancelBtn.addEventListener('click', hideModal);
        if (okBtn) okBtn.addEventListener('click', function () {
            var action = pendingAction;
            hideModal();
            if (action) action();
        });
        modal.addEventListener('click', function (e) {
            if (e.target === modal) hideModal();
        });
    });

    // 키보드: Escape 닫기 + Tab 포커스 트랩
    document.addEventListener('keydown', function (e) {
        var modal = document.getElementById('adminConfirmModal');
        if (!modal || modal.style.display === 'none') return;
        if (e.key === 'Escape') { hideModal(); return; }
        if (e.key === 'Tab') {
            var cancelBtn = document.getElementById('adminConfirmCancel');
            var okBtn = document.getElementById('adminConfirmOk');
            if (!cancelBtn || !okBtn) return;
            if (e.shiftKey) {
                if (document.activeElement === cancelBtn) { e.preventDefault(); okBtn.focus(); }
            } else {
                if (document.activeElement === okBtn) { e.preventDefault(); cancelBtn.focus(); }
            }
        }
    });

    // 버튼[data-confirm] 클릭 가로채기
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('button[data-confirm]');
        if (!btn) return;
        e.preventDefault();
        var msg = btn.dataset.confirm;
        var form = btn.form || btn.closest('form');
        showModal(msg, function () {
            setButtonLoading(btn);
            if (form) form.submit();
        });
    });

    // 폼[data-confirm] submit 가로채기
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!form.hasAttribute('data-confirm')) return;
        e.preventDefault();
        var msg = form.dataset.confirm;
        showModal(msg, function () {
            var submitBtn = form.querySelector('button[type=submit], input[type=submit]');
            setButtonLoading(submitBtn);
            form.removeAttribute('data-confirm');
            form.submit();
        });
    });

    // data-confirm 없는 폼 제출 시 로딩 상태
    // bubble phase + setTimeout: 페이지별 검증 핸들러가 e.preventDefault()를 호출했으면 버튼 상태를 변경하지 않음
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (form.hasAttribute('data-confirm')) return;
        var capturedEvent = e;
        setTimeout(function () {
            if (capturedEvent.defaultPrevented) return;
            var btn = form.querySelector('button[type=submit], input[type=submit]');
            setButtonLoading(btn);
        }, 0);
    });

    // select[data-autosubmit] 자동 제출
    document.addEventListener('change', function (e) {
        var sel = e.target;
        if (sel.tagName !== 'SELECT' || !sel.hasAttribute('data-autosubmit')) return;
        var form = sel.closest('form');
        if (form) form.submit();
    });

    window.AdminConfirm = { show: showModal };
})();
