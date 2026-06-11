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
})();
