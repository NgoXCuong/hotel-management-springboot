function toggleDiscountTypeUI() {
    const isPercentage = document.getElementById('dt_PERCENTAGE') && document.getElementById('dt_PERCENTAGE').checked;
    const maxDiscountContainer = document.getElementById('maxDiscountContainer');
    const suffix = document.getElementById('discountValueSuffix');
    const icon = document.getElementById('discountValueIcon');
    const discountValueInput = document.getElementById('discountValue');

    if (isPercentage) {
        if (maxDiscountContainer) maxDiscountContainer.style.display = 'block';
        if (suffix) suffix.innerText = '%';
        if (icon) icon.className = 'fa-solid fa-percent';
        if (discountValueInput) {
            discountValueInput.placeholder = 'Nhập % giảm (1 - 100)';
            discountValueInput.max = '100';
        }
    } else {
        if (maxDiscountContainer) maxDiscountContainer.style.display = 'none';
        if (suffix) suffix.innerText = 'VNĐ';
        if (icon) icon.className = 'fa-solid fa-money-bill-wave';
        if (discountValueInput) {
            discountValueInput.placeholder = 'Nhập số tiền giảm (VNĐ)';
            discountValueInput.removeAttribute('max');
        }
    }
}

document.addEventListener('DOMContentLoaded', function() {
    toggleDiscountTypeUI();
});
