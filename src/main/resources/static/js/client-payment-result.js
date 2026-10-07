function copyBookingCode() {
    const code = document.getElementById('bookingCodeText').innerText.trim();
    navigator.clipboard.writeText(code).then(() => {
        alert('Đã sao chép mã đặt phòng: ' + code);
    });
}

function copyText(elementId, label) {
    const text = document.getElementById(elementId).innerText.trim();
    navigator.clipboard.writeText(text).then(() => {
        alert('Đã sao chép ' + label + ': ' + text);
    });
}

// Hàm xác nhận thanh toán cọc thủ công tức thì khi khách bấm nút
function confirmPaymentDeposit() {
    const bookingCodeElem = document.getElementById('bookingCodeText');
    if (!bookingCodeElem) return;
    const bookingCode = bookingCodeElem.innerText.trim();
    const btn = document.getElementById('btnManualConfirm');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1.5"></i> Đang xác nhận...';
    }

    fetch('/api/booking/confirm-deposit/' + encodeURIComponent(bookingCode), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(res => res.json())
    .then(data => {
        if (data && data.success) {
            window.location.href = '/booking/result?code=' + encodeURIComponent(bookingCode) + '&success=true&paid=true&method=BANK_TRANSFER';
        } else {
            alert(data.message || 'Có lỗi xảy ra khi xác nhận!');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="fa-solid fa-circle-check me-1.5"></i> Tôi đã chuyển khoản (Xác nhận ngay)';
            }
        }
    })
    .catch(err => {
        console.error(err);
        alert('Lỗi kết nối khi gửi xác nhận!');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-circle-check me-1.5"></i> Tôi đã chuyển khoản (Xác nhận ngay)';
        }
    });
}

// Tự động kiểm tra trạng thái thanh toán từ SePay theo thời gian thực (Polling)
document.addEventListener('DOMContentLoaded', function () {
    const bookingCodeElem = document.getElementById('bookingCodeText');
    if (!bookingCodeElem) return;

    const bookingCode = bookingCodeElem.innerText.trim();
    const isAlreadyPaid = document.querySelector('.alert-success') !== null;

    // Nếu đơn chưa thanh toán, bắt đầu kiểm tra tự động mỗi 2.5 giây
    if (!isAlreadyPaid && bookingCode) {
        console.log('[SePay] Bắt đầu lắng nghe trạng thái thanh toán cho đơn: ' + bookingCode);
        
        let pollCount = 0;
        const maxPolls = 240; // Kiểm tra tối đa trong vòng 10 phút

        const pollInterval = setInterval(() => {
            pollCount++;
            if (pollCount > maxPolls) {
                clearInterval(pollInterval);
                console.log('[SePay] Dừng lắng nghe sau 10 phút.');
                return;
            }

            fetch('/api/booking/check-status/' + encodeURIComponent(bookingCode))
                .then(res => res.json())
                .then(data => {
                    if (data && data.success && data.paid) {
                        clearInterval(pollInterval);
                        console.log('[SePay] Thanh toán thành công! Tự động làm mới trang...');
                        // Chuyển sang màn hình đã thanh toán thành công
                        window.location.href = '/booking/result?code=' + encodeURIComponent(bookingCode) + '&success=true&paid=true&method=BANK_TRANSFER';
                    }
                })
                .catch(err => {
                    console.debug('[SePay] Đang kiểm tra trạng thái...', err);
                });
        }, 2500);
    }
});
