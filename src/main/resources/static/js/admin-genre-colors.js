document.addEventListener('DOMContentLoaded', function() {
    var genreMap = {
        'Band': 'badge-genre-band',
        'Hip-hop': 'badge-genre-hiphop',
        'Indie': 'badge-genre-indie',
        'Ballad': 'badge-genre-ballad',
        'R&B': 'badge-genre-rnb'
    };
    document.querySelectorAll('.badge-genre').forEach(function(el) {
        var cls = genreMap[el.textContent.trim()];
        if (cls) el.classList.add(cls);
    });
});
