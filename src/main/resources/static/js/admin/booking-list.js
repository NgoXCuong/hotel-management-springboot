function openCancelModal(btn) {
    const bookingId = btn.getAttribute('data-id');
    const bookingCode = btn.getAttribute('data-code');
    const form = document.getElementById('cancelBookingForm');
    const codeSpan = document.getElementById('cancelBookingCode');

    form.action = '/admin/bookings/cancel/' + bookingId;
    codeSpan.innerText = bookingCode;

    const modal = new bootstrap.Modal(document.getElementById('cancelModal'));
    modal.show();
}
