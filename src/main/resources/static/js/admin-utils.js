(function () {
    window.AdminUtils = {
        initTabs: function (hashKeys) {
            var validHashes = hashKeys || [];

            function activate(tabName) {
                document.querySelectorAll('.header-tab-btn').forEach(function (b) { b.classList.remove('active'); });
                document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.remove('active'); });
                var btn = document.querySelector('.header-tab-btn[data-tab="' + tabName + '"]');
                if (btn) btn.classList.add('active');
                var panel = document.getElementById('tab-' + tabName);
                if (panel) panel.classList.add('active');
                if (validHashes.length > 0) {
                    history.replaceState(null, '', window.location.pathname + window.location.search + '#' + tabName);
                }
            }

            document.querySelectorAll('.header-tab-btn').forEach(function (btn) {
                btn.addEventListener('click', function () { activate(btn.dataset.tab); });
            });

            if (validHashes.length > 0) {
                var hash = window.location.hash.replace('#', '');
                if (validHashes.indexOf(hash) !== -1) activate(hash);
            }
        },
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
        },
        /**
         * input[type=file] 선택 파일을 Data URL로 읽어 callback(dataUrl)을 호출.
         * maxSizeMb 초과 시 errorElId 요소를 표시하고 input을 초기화한 뒤 callback 미호출.
         */
        readImageAsDataUrl: function (input, callback, maxSizeMb, errorElId) {
            if (!input.files || !input.files[0]) return;
            var file = input.files[0];
            var maxBytes = (maxSizeMb || 5) * 1024 * 1024;
            var errorEl = errorElId ? document.getElementById(errorElId) : null;
            if (file.size > maxBytes) {
                if (errorEl) errorEl.style.display = 'block';
                input.value = '';
                return;
            }
            if (errorEl) errorEl.style.display = 'none';
            var reader = new FileReader();
            reader.onload = function (e) { callback(e.target.result); };
            reader.readAsDataURL(file);
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
