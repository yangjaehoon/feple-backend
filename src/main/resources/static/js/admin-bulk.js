(function () {
    const selectAll  = document.getElementById('select-all');
    const toolbar    = document.getElementById('bulk-toolbar');
    const countLabel = document.getElementById('select-count');
    const unit       = (toolbar && toolbar.dataset.unit) || '개';

    function updateToolbar() {
        const checked = document.querySelectorAll('.row-check:checked').length;
        const total = document.querySelectorAll('.row-check').length;
        countLabel.textContent = checked + '/' + total + unit + ' 선택 (현재 페이지)';
        if (checked > 0) toolbar.classList.add('visible');
        else toolbar.classList.remove('visible');
        const all = document.querySelectorAll('.row-check');
        if (selectAll && all.length > 0) {
            selectAll.indeterminate = checked > 0 && checked < all.length;
            selectAll.checked = checked === all.length;
        }
    }

    if (selectAll) {
        selectAll.addEventListener('change', () => {
            document.querySelectorAll('.row-check').forEach(cb => cb.checked = selectAll.checked);
            updateToolbar();
        });
    }
    document.querySelectorAll('.row-check').forEach(cb => cb.addEventListener('change', updateToolbar));
})();
