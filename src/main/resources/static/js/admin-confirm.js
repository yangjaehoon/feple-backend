(function () {
    var pendingAction = null;

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
            // 모달 없을 때 폴백 (정상 상황에서는 발생하지 않음)
            if (window.confirm(message)) action();
            return;
        }
        document.getElementById('adminConfirmMsg').textContent = message;
        pendingAction = action;
        modal.style.display = 'flex';
    }

    function hideModal() {
        var modal = document.getElementById('adminConfirmModal');
        if (modal) modal.style.display = 'none';
        pendingAction = null;
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
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && modal.style.display !== 'none') hideModal();
        });
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
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (form.hasAttribute('data-confirm')) return;
        var btn = form.querySelector('button[type=submit], input[type=submit]');
        setButtonLoading(btn);
    }, true);

    // select[data-autosubmit] 자동 제출
    document.addEventListener('change', function (e) {
        var sel = e.target;
        if (sel.tagName !== 'SELECT' || !sel.hasAttribute('data-autosubmit')) return;
        var form = sel.closest('form');
        if (form) form.submit();
    });
})();
