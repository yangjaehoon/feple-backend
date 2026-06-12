(function () {
    window.AdminUtils.initTabs();

    /* ── 날짜 범위 프리셋 ── */
    document.querySelectorAll('.preset-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var days = parseInt(btn.dataset.days);
            var to   = new Date();
            var from = new Date(to);
            from.setDate(from.getDate() - days + 1);
            document.getElementById('to-input').value   = to.toISOString().slice(0, 10);
            document.getElementById('from-input').value = from.toISOString().slice(0, 10);
            document.getElementById('date-range-form').submit();
        });
    });

    /* ── Chart.js ── */
    function initStatsCharts(rangeStats) {
        var labels       = rangeStats.map(function (s) { return s.date; });
        var signupsData  = rangeStats.map(function (s) { return s.signups; });
        var postsData    = rangeStats.map(function (s) { return s.posts; });
        var commentsData = rangeStats.map(function (s) { return s.comments; });

        var chartOpts = {
            responsive: true,
            interaction: { mode: 'index', intersect: false },
            plugins: { legend: { position: 'top' } },
            scales: {
                x: { grid: { display: false } },
                y: { beginAtZero: true, ticks: { precision: 0 } }
            }
        };

        new Chart(document.getElementById('signupsChart'), {
            type: 'line',
            options: chartOpts,
            data: {
                labels: labels,
                datasets: [{
                    label: '신규 가입자',
                    data: signupsData,
                    borderColor: '#52C898',
                    backgroundColor: 'rgba(82,200,152,0.12)',
                    fill: true, tension: 0.3, pointRadius: 3
                }]
            }
        });

        new Chart(document.getElementById('activityChart'), {
            type: 'line',
            options: chartOpts,
            data: {
                labels: labels,
                datasets: [
                    {
                        label: '게시글',
                        data: postsData,
                        borderColor: '#5BC0EB',
                        backgroundColor: 'rgba(91,192,235,0.12)',
                        fill: true, tension: 0.3, pointRadius: 3
                    },
                    {
                        label: '댓글',
                        data: commentsData,
                        borderColor: '#A78BFA',
                        backgroundColor: 'rgba(167,139,250,0.12)',
                        fill: true, tension: 0.3, pointRadius: 3
                    }
                ]
            }
        });
    }

    /* 템플릿의 인라인 스크립트에서 호출할 수 있도록 전역 노출 */
    window.initStatsCharts = initStatsCharts;
})();
