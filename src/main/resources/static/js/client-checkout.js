function highlightPayment(labelEl) {
    document.querySelectorAll('.payment-radio-card').forEach(el => el.classList.remove('selected'));
    labelEl.classList.add('selected');
}

function applyVoucher() {
    const code = document.getElementById('voucherInput').value.trim();
    const url = new URL(window.location.href);
    if (code) {
        url.searchParams.set('promoCode', code);
    } else {
        url.searchParams.delete('promoCode');
    }
    window.location.href = url.toString();
}
