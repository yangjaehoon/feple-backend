/* ── 전역: Alert 자동 닫기 ── */
(function() {
    function dismissAlert(el) {
        el.style.opacity = '0';
        el.style.maxHeight = el.offsetHeight + 'px';
        setTimeout(function() { el.style.maxHeight = '0'; el.style.padding = '0'; el.style.marginBottom = '0'; }, 300);
        setTimeout(function() { if (el.parentNode) el.parentNode.removeChild(el); }, 600);
    }
    document.addEventListener('DOMContentLoaded', function() {
        document.querySelectorAll('.alert-success, .alert-danger').forEach(function(el) {
            var btn = document.createElement('button');
            btn.className = 'alert-close';
            btn.innerHTML = '&times;';
            btn.addEventListener('click', function() { dismissAlert(el); });
            el.appendChild(btn);
        });
        document.querySelectorAll('.alert-success').forEach(function(el) {
            setTimeout(function() { dismissAlert(el); }, 4000);
        });
    });
})();
