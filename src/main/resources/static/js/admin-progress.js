/* ── 전역: 페이지 전환 진행 바 (상단 얇은 바) ──
   링크 네비게이션 / 폼 제출 시작 시 표시, 새 페이지 로드(load/pageshow) 시 완료.
   서버 렌더 방식이라 전환 중 아무 피드백이 없던 것을 보완한다. */
(function () {
    var bar = document.createElement('div');
    bar.id = 'admin-progress';
    (document.body || document.documentElement).appendChild(bar);

    var timer = null;
    var width = 0;

    function start() {
        if (timer) return;
        width = 8;
        bar.style.transition = 'none';
        bar.style.width = '0';
        bar.style.opacity = '1';
        void bar.offsetWidth; // reflow
        bar.style.transition = 'width .25s ease, opacity .3s ease';
        bar.style.width = width + '%';
        timer = setInterval(function () {
            width += (92 - width) * 0.12; // 92%에 점근
            bar.style.width = width + '%';
        }, 300);
    }

    function done() {
        if (timer) { clearInterval(timer); timer = null; }
        bar.style.width = '100%';
        setTimeout(function () { bar.style.opacity = '0'; }, 180);
        setTimeout(function () { bar.style.width = '0'; }, 480);
    }

    // bubble phase + setTimeout: 페이지별 핸들러가 preventDefault(검증 실패, ajax 전환 등)했으면
    // 실제 네비게이션이 없으므로 바를 띄우지 않는다.
    function startIfNavigating(ev) {
        setTimeout(function () { if (!ev.defaultPrevented) start(); }, 0);
    }

    document.addEventListener('click', function (e) {
        if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
        var a = e.target.closest && e.target.closest('a[href]');
        if (!a) return;
        var href = a.getAttribute('href');
        if (!href || href.charAt(0) === '#') return;
        if (a.target === '_blank' || a.hasAttribute('download')) return;
        if (a.origin && a.origin !== location.origin) return;
        startIfNavigating(e);
    });

    document.addEventListener('submit', function (e) {
        if (e.target.target === '_blank') return;
        startIfNavigating(e);
    });

    window.addEventListener('pageshow', done);
    window.addEventListener('load', done);

    window.AdminProgress = { start: start, done: done };
})();
