function openReviewDetailModal(btn) {
    const id = btn.getAttribute('data-id');
    const customer = btn.getAttribute('data-customer');
    const phone = btn.getAttribute('data-phone');
    const booking = btn.getAttribute('data-booking');
    const rating = parseInt(btn.getAttribute('data-rating') || '5');
    const comment = btn.getAttribute('data-comment');
    const isVisible = btn.getAttribute('data-visible') === 'true';
    const date = btn.getAttribute('data-date');

    document.getElementById('modalCustomerName').innerText = customer;
    document.getElementById('modalPhone').innerText = phone || 'Chưa có';
    document.getElementById('modalBookingCode').innerText = booking;
    document.getElementById('modalDate').innerText = date;
    document.getElementById('modalRatingText').innerText = rating + ' / 5 sao';
    document.getElementById('modalCommentText').innerText = comment || 'Khách hàng không để lại nhận xét văn bản.';

    // Render stars
    let starsHtml = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= rating) {
            starsHtml += '<i class="fa-solid fa-star text-warning me-1"></i>';
        } else {
            starsHtml += '<i class="fa-solid fa-star text-muted me-1" style="opacity: 0.3;"></i>';
        }
    }
    document.getElementById('modalStarContainer').innerHTML = starsHtml;

    // Form toggle
    const form = document.getElementById('modalToggleForm');
    form.action = '/admin/reviews/toggle-visibility/' + id;

    const toggleBtn = document.getElementById('modalToggleBtn');
    if (isVisible) {
        toggleBtn.innerHTML = '<i class="fa-solid fa-eye-slash me-2 text-warning"></i> Ẩn khỏi trang chủ';
    } else {
        toggleBtn.innerHTML = '<i class="fa-solid fa-eye me-2 text-success"></i> Cho phép hiển thị công khai';
    }

    const modal = new bootstrap.Modal(document.getElementById('reviewDetailModal'));
    modal.show();
}
