let currentDiscountAmount = 0;

function setNewCustomerMode(isNew) {
    document.getElementById('isNewCustomer').value = isNew;
    const select = document.getElementById('customerSelect');
    const nameInput = document.getElementById('newCustomerName');
    const phoneInput = document.getElementById('newCustomerPhone');

    if (isNew) {
        select.removeAttribute('required');
        nameInput.setAttribute('required', 'required');
        phoneInput.setAttribute('required', 'required');
    } else {
        nameInput.removeAttribute('required');
        phoneInput.removeAttribute('required');
        select.setAttribute('required', 'required');
    }
}

function getNights() {
    const checkInVal = document.getElementById('checkInInput').value;
    const checkOutVal = document.getElementById('checkOutInput').value;
    if (!checkInVal || !checkOutVal) return 1;

    const checkInDate = new Date(checkInVal);
    const checkOutDate = new Date(checkOutVal);
    const diffTime = checkOutDate.getTime() - checkInDate.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
}

function onDateChanged() {
    const nights = getNights();
    document.getElementById('stayNightsText').innerText = nights;
    document.getElementById('summaryNights').innerText = nights + ' đêm';
    calculateFinancials();
    refreshAvailableRooms();
}

function toggleRoomCard(card) {
    const checkbox = card.querySelector('input[type="checkbox"]');
    if (checkbox) {
        checkbox.checked = !checkbox.checked;
        if (checkbox.checked) {
            card.classList.add('selected');
        } else {
            card.classList.remove('selected');
        }
    }
    calculateFinancials();
}

function toggleRoomSelection(card, roomId, price) {
    toggleRoomCard(card);
}

function calculateFinancials() {
    const nights = getNights();
    const checkboxes = document.querySelectorAll('.room-checkbox:checked');
    let roomSubtotal = 0;

    checkboxes.forEach(cb => {
        const price = parseFloat(cb.getAttribute('data-price')) || 0;
        roomSubtotal += (price * nights);
        const parentCard = cb.closest('.room-select-card');
        if (parentCard) parentCard.classList.add('selected');
    });

    document.querySelectorAll('.room-checkbox:not(:checked)').forEach(cb => {
        const parentCard = cb.closest('.room-select-card');
        if (parentCard) parentCard.classList.remove('selected');
    });

    document.getElementById('summaryRoomCount').innerText = checkboxes.length + ' phòng';
    document.getElementById('summaryRoomSubtotal').innerText = formatVND(roomSubtotal);

    // Tổng tiền sau giảm giá
    const totalAmount = Math.max(0, roomSubtotal - currentDiscountAmount);
    document.getElementById('summaryTotalAmount').innerText = formatVND(totalAmount);
    document.getElementById('summaryDiscount').innerText = '-' + formatVND(currentDiscountAmount);
}

function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
}

function applyVoucher() {
    const code = document.getElementById('voucherCodeInput').value.trim();
    const nights = getNights();
    const checkboxes = document.querySelectorAll('.room-checkbox:checked');
    let roomSubtotal = 0;
    checkboxes.forEach(cb => {
        const price = parseFloat(cb.getAttribute('data-price')) || 0;
        roomSubtotal += (price * nights);
    });

    const msgDiv = document.getElementById('voucherMessage');

    if (!code) {
        msgDiv.className = 'small mt-1 text-danger';
        msgDiv.innerText = 'Vui lòng nhập mã voucher!';
        msgDiv.style.display = 'block';
        return;
    }

    if (roomSubtotal <= 0) {
        msgDiv.className = 'small mt-1 text-danger';
        msgDiv.innerText = 'Vui lòng chọn ít nhất 1 phòng trước khi áp dụng voucher!';
        msgDiv.style.display = 'block';
        return;
    }

    fetch(`/admin/bookings/api/check-voucher?code=${encodeURIComponent(code)}&amount=${roomSubtotal}`)
        .then(res => res.json())
        .then(data => {
            msgDiv.style.display = 'block';
            if (data.valid) {
                currentDiscountAmount = data.discountAmount;
                msgDiv.className = 'small mt-1 text-success fw-semibold';
                msgDiv.innerText = data.message;
            } else {
                currentDiscountAmount = 0;
                msgDiv.className = 'small mt-1 text-danger';
                msgDiv.innerText = data.message;
            }
            calculateFinancials();
        })
        .catch(err => {
            msgDiv.className = 'small mt-1 text-danger';
            msgDiv.innerText = 'Lỗi kiểm tra voucher. Vui lòng thử lại!';
            msgDiv.style.display = 'block';
        });
}

function refreshAvailableRooms() {
    const checkInVal = document.getElementById('checkInInput').value;
    const checkOutVal = document.getElementById('checkOutInput').value;
    if (!checkInVal || !checkOutVal) return;

    fetch(`/admin/bookings/api/available-rooms?checkIn=${encodeURIComponent(checkInVal)}&checkOut=${encodeURIComponent(checkOutVal)}`)
        .then(res => res.json())
        .then(rooms => {
            const container = document.getElementById('roomsContainer');
            const alertBox = document.getElementById('noRoomsAlert');
            const badge = document.getElementById('availableCountBadge');

            badge.innerHTML = `Khả dụng: <strong>${rooms.length}</strong> phòng`;

            if (rooms.length === 0) {
                container.innerHTML = '';
                alertBox.style.display = 'block';
                calculateFinancials();
                return;
            }

            alertBox.style.display = 'none';
            let html = '';
            rooms.forEach(r => {
                html += `
                    <div class="col-12 col-sm-6">
                        <div class="room-select-card d-flex align-items-center justify-content-between" 
                             onclick="toggleRoomSelection(this, ${r.id}, ${r.pricePerNight})">
                            <div class="d-flex align-items-center gap-2.5">
                                <input class="form-check-input room-checkbox" type="checkbox" name="roomIds" 
                                       value="${r.id}" 
                                       data-price="${r.pricePerNight}"
                                       data-number="${r.roomNumber}"
                                       onclick="event.stopPropagation(); calculateFinancials();">
                                <div>
                                    <div class="fw-bold text-dark">
                                        <i class="fa-solid fa-bed text-muted me-1"></i>Phòng ${r.roomNumber}
                                    </div>
                                    <div class="text-muted small" style="font-size: 11.5px;">${r.roomTypeName}</div>
                                </div>
                            </div>
                            <div class="text-end">
                                <div class="fw-bold text-dark small">
                                    ${new Intl.NumberFormat('vi-VN').format(r.pricePerNight)} đ
                                </div>
                                <div class="text-muted" style="font-size: 11px;">/ đêm</div>
                            </div>
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
            calculateFinancials();
        })
        .catch(err => console.error(err));
}

// Khởi chạy tính toán ban đầu
document.addEventListener('DOMContentLoaded', () => {
    onDateChanged();
});
