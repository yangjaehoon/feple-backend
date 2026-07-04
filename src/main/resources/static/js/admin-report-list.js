(function () {
    function toggleAll(master) {
        document.querySelectorAll('.row-check').forEach(function (cb) {
            cb.checked = master.checked;
        });
        updateToolbar();
    }

    function updateToolbar() {
        var selectedCheckboxes = document.querySelectorAll('.row-check:checked');
        var toolbar = document.getElementById('bulk-toolbar');
        var countEl = document.getElementById('select-count');
        if (!toolbar) return;
        if (selectedCheckboxes.length > 0) {
            toolbar.classList.add('visible');
            countEl.textContent = selectedCheckboxes.length + '건 선택됨';
        } else {
            toolbar.classList.remove('visible');
        }
        var master = document.getElementById('select-all');
        var all = document.querySelectorAll('.row-check');
        if (master && all.length > 0) {
            master.indeterminate = selectedCheckboxes.length > 0 && selectedCheckboxes.length < all.length;
            master.checked = selectedCheckboxes.length === all.length;
        }
    }

    function submitBulk(endpoint, confirmTpl) {
        var selectedCheckboxes = document.querySelectorAll('.row-check:checked');
        if (selectedCheckboxes.length === 0) return;
        var msg = confirmTpl.replace('{n}', selectedCheckboxes.length);
        var ids = Array.from(selectedCheckboxes).map(function (cb) { return cb.value; });
        AdminConfirm.show(msg, function () {
            var form = document.getElementById('bulk-form');
            form.action = endpoint;
            form.querySelectorAll('input[name="ids"]').forEach(function (el) { el.remove(); });
            ids.forEach(function (id) {
                var input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'ids';
                input.value = id;
                form.appendChild(input);
            });
            form.submit();
        });
    }

    /* 일괄 처리 버튼 — 이벤트 위임 */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.bulk-action-btn');
        if (!btn) return;
        submitBulk(btn.dataset.endpoint, btn.dataset.confirmTpl);
    });

    /* 전체 선택 체크박스 — 이벤트 위임 */
    document.addEventListener('change', function (e) {
        if (e.target.id === 'select-all') toggleAll(e.target);
        else if (e.target.classList.contains('row-check')) updateToolbar();
    });
})();
