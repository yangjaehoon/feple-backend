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
            var tokenEl  = document.querySelector('meta[name="_csrf"]');
            var headerEl = document.querySelector('meta[name="_csrf_header"]');
            if (!tokenEl || !headerEl) return {};
            var h = {};
            h[headerEl.content] = tokenEl.content;
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
        },
        /**
         * 폼 입력값 변경 시 페이지 이탈 경고 등록.
         * 폼 submit 시 자동 해제되어 서버 제출 시에는 경고가 뜨지 않음.
         */
        initDirtyGuard: function () {
            var dirty = false;
            document.querySelectorAll('form input, form textarea, form select').forEach(function (el) {
                el.addEventListener('input', function () { dirty = true; });
                el.addEventListener('change', function () { dirty = true; });
            });
            document.querySelectorAll('form').forEach(function (form) {
                form.addEventListener('submit', function () { dirty = false; });
            });
            window.addEventListener('beforeunload', function (e) {
                if (!dirty) return;
                e.preventDefault();
                e.returnValue = '';
            });
        },
        /**
         * 전역 토스트 알림 표시. type: 'success' | 'error' | 'partial'
         */
        showToast: function (msg, type) {
            var toast = document.getElementById('applyToast');
            var body = document.getElementById('applyToastBody');
            var closeBtn = document.getElementById('applyToastClose');
            if (!toast || !body) return;
            body.textContent = msg;
            toast.className = (type || 'success') + ' visible';
            var timer = setTimeout(function () {
                toast.classList.remove('visible');
            }, 4000);
            if (closeBtn) {
                closeBtn.onclick = function () {
                    clearTimeout(timer);
                    toast.classList.remove('visible');
                };
            }
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
