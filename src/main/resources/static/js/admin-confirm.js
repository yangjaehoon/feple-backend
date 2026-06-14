document.addEventListener('click', function (e) {
    var btn = e.target.closest('button[data-confirm]');
    if (!btn) return;
    if (!confirm(btn.dataset.confirm)) e.preventDefault();
});

document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!form.dataset.confirm) return;
    if (!confirm(form.dataset.confirm)) e.preventDefault();
});

document.addEventListener('change', function (e) {
    var sel = e.target;
    if (sel.tagName !== 'SELECT' || !sel.hasAttribute('data-autosubmit')) return;
    var form = sel.closest('form');
    if (form) form.submit();
});
