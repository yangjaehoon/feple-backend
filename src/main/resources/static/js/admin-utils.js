(function () {
    window.AdminUtils = {
        getCsrfHeaders: function () {
            var token  = document.querySelector('meta[name="_csrf"]').content;
            var header = document.querySelector('meta[name="_csrf_header"]').content;
            var h = {};
            h[header] = token;
            return h;
        },
        escapeHtml: function (str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;');
        }
    };

    // Mobile sidebar toggle
    document.addEventListener('DOMContentLoaded', function () {
        var menuBtn = document.querySelector('.topbar-menu-btn');
        var overlay = document.querySelector('.sb-overlay');
        var sidebar = document.querySelector('.sidebar');

        function openSidebar() {
            sidebar.classList.add('sb-open');
            overlay.classList.add('visible');
            document.body.style.overflow = 'hidden';
        }
        function closeSidebar() {
            sidebar.classList.remove('sb-open');
            overlay.classList.remove('visible');
            document.body.style.overflow = '';
        }

        if (menuBtn) menuBtn.addEventListener('click', openSidebar);
        if (overlay) overlay.addEventListener('click', closeSidebar);
    });
})();
