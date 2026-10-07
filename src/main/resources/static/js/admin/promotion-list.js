let promotionModalInstance = null;

document.addEventListener('DOMContentLoaded', function() {
    promotionModalInstance = new bootstrap.Modal(document.getElementById('promotionModal'));
});

function copyVoucher(code) {
    if (!code) return;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(code).then(() => {
            if (typeof window.showToast === 'function') {
                window.showToast('success', 'Đã sao chép mã voucher: ' + code);
            } else {
                alert('Đã sao chép: ' + code);
            }
        }).catch(() => {
            prompt('Sao chép mã voucher:', code);
        });
    } else {
        prompt('Sao chép mã voucher:', code);
    }
}

function toggleModalDiscountTypeUI() {
    const isPercentage = document.getElementById('modalDtPercentage').checked;
    const maxDiscountContainer = document.getElementById('modalMaxDiscountContainer');
    const suffix = document.getElementById('modalDiscountSuffix');
    const input = document.getElementById('promoDiscountValue');

    if (isPercentage) {
        maxDiscountContainer.style.display = 'block';
        suffix.innerText = '%';
        input.placeholder = '15';
        input.max = '100';
    } else {
        maxDiscountContainer.style.display = 'none';
        suffix.innerText = 'VNĐ';
        input.placeholder = '200000';
        input.removeAttribute('max');
    }
}

function openCreatePromotionModal() {
    const form = document.getElementById('promotionForm');
    form.reset();
    form.action = '/admin/promotions/create';
    document.getElementById('promoId').value = '';
    document.getElementById('promotionModalTitle').innerText = 'Tạo mới Mã khuyến mãi & Voucher';
    document.getElementById('modalDtPercentage').checked = true;
    document.getElementById('promoActive').checked = true;
    document.getElementById('promoMinOrderAmount').value = '0';

    // Default Dates: Now -> +1 Month
    const now = new Date();
    const nextMonth = new Date();
    nextMonth.setMonth(nextMonth.getMonth() + 1);

    const pad = (n) => (n < 10 ? '0' + n : n);
    const formatDate = (d) => d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());

    document.getElementById('promoStartDate').value = formatDate(now);
    document.getElementById('promoEndDate').value = formatDate(nextMonth);

    toggleModalDiscountTypeUI();
    promotionModalInstance.show();
}

function openEditPromotionModal(btn) {
    const id = btn.getAttribute('data-id');
    const code = btn.getAttribute('data-code') || '';
    const name = btn.getAttribute('data-name') || '';
    const description = btn.getAttribute('data-description') || '';
    const discountType = btn.getAttribute('data-discount-type') || 'PERCENTAGE';
    const discountValue = btn.getAttribute('data-discount-value') || '';
    const maxDiscount = btn.getAttribute('data-max-discount') || '';
    const minOrder = btn.getAttribute('data-min-order') || '0';
    const startDate = btn.getAttribute('data-start-date') || '';
    const endDate = btn.getAttribute('data-end-date') || '';
    const usageLimit = btn.getAttribute('data-usage-limit') || '';
    const active = btn.getAttribute('data-active') === 'true';

    const form = document.getElementById('promotionForm');
    form.action = '/admin/promotions/edit/' + id;
    document.getElementById('promoId').value = id;
    document.getElementById('promoCode').value = code;
    document.getElementById('promoName').value = name;
    document.getElementById('promoDescription').value = description;

    if (discountType === 'FIXED_AMOUNT') {
        document.getElementById('modalDtFixed').checked = true;
    } else {
        document.getElementById('modalDtPercentage').checked = true;
    }

    document.getElementById('promoDiscountValue').value = discountValue;
    document.getElementById('promoMaxDiscountAmount').value = maxDiscount;
    document.getElementById('promoMinOrderAmount').value = minOrder;
    document.getElementById('promoStartDate').value = startDate;
    document.getElementById('promoEndDate').value = endDate;
    document.getElementById('promoUsageLimit').value = usageLimit;
    document.getElementById('promoActive').checked = active;
    document.getElementById('promotionModalTitle').innerText = 'Cập nhật Voucher [' + code + ']';

    toggleModalDiscountTypeUI();
    promotionModalInstance.show();
}
