async function scanWord(btn) {
    const word = btn.dataset.word;
    const resultEl = btn.closest('td').querySelector('.scan-result');
    resultEl.style.display = 'block';
    resultEl.textContent = '스캔 중...';
    try {
        const res = await fetch(BadWordsUrls.scan + '?word=' + encodeURIComponent(word));
        const data = await res.json();
        if (res.ok) {
            const total = data.postCount + data.commentCount;
            if (total === 0) {
                resultEl.style.color = 'var(--muted)';
                resultEl.textContent = '기존 콘텐츠에서 발견되지 않음';
            } else {
                resultEl.style.color = '#b91c1c';
                resultEl.textContent = '게시글 ' + data.postCount + '건, 댓글 ' + data.commentCount + '건에서 발견';
            }
        } else {
            resultEl.textContent = data.error || '스캔 오류';
        }
    } catch (e) {
        resultEl.textContent = '스캔 중 오류가 발생했습니다.';
    }
}
