(function () {
    function setCertReason(chip, text) {
        document.getElementById('certRejectMsg').value = text;
        document.querySelectorAll('.cert-reject-form .reason-chip').forEach(function (c) {
            c.classList.remove('active');
        });
        chip.classList.add('active');
    }

    function clearCertReason(chip) {
        document.getElementById('certRejectMsg').value = '';
        document.getElementById('certRejectMsg').focus();
        document.querySelectorAll('.cert-reject-form .reason-chip').forEach(function (c) {
            c.classList.remove('active');
        });
        chip.classList.add('active');
    }

    window.setCertReason = setCertReason;
    window.clearCertReason = clearCertReason;
})();
