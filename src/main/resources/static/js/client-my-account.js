function filterBookings(status, btn) {
    document.querySelectorAll('.filter-status-btn').forEach(el => {
        el.classList.remove('btn-dark');
        el.classList.add('btn-light', 'border');
    });
    btn.classList.remove('btn-light', 'border');
    btn.classList.add('btn-dark');

    document.querySelectorAll('.booking-item-card').forEach(card => {
        if (status === 'ALL' || card.getAttribute('data-status') === status) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}
