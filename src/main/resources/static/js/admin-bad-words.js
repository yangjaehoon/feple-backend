(function () {
    async function scanWord(btn) {
        var word = btn.dataset.word;
        var resultEl = btn.closest('td').querySelector('.scan-result');
        resultEl.style.display = 'block';
        resultEl.textContent = '스캔 중...';
        try {
            var res = await fetch(BadWordsUrls.scan + '?word=' + encodeURIComponent(word));
            var data = await res.json();
            if (res.ok) {
                var total = data.postCount + data.commentCount;
                if (total === 0) {
                    resultEl.style.color = 'var(--muted)';
                    resultEl.textContent = '기존 콘텐츠에서 발견되지 않음';
                } else {
                    resultEl.style.color = 'var(--danger)';
                    resultEl.textContent = '게시글 ' + data.postCount + '건, 댓글 ' + data.commentCount + '건에서 발견';
                }
            } else {
                resultEl.textContent = data.error || '스캔 오류';
            }
        } catch (e) {
            resultEl.textContent = '스캔 중 오류가 발생했습니다.';
        }
    }

    /* 스캔 버튼 이벤트 위임 */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.scan-word-btn');
        if (btn) scanWord(btn);
    });
})();
