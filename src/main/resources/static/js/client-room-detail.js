function changeGalleryImg(src, thumb) {
    const main = document.getElementById('mainGalleryImg');
    main.style.opacity = '0.4';
    setTimeout(() => {
        main.src = src;
        main.style.opacity = '1';
    }, 150);
    document.querySelectorAll('.gallery-thumb').forEach(el => el.classList.remove('active'));
    thumb.classList.add('active');
}

function calculateEstimatedTotal() {
    const inVal = document.getElementById('inputCheckIn').value;
    const outVal = document.getElementById('inputCheckOut').value;
    const price = parseFloat(document.getElementById('pricePerNightVal').value || '0');

    if (inVal && outVal) {
        const dIn = new Date(inVal);
        const dOut = new Date(outVal);
        const diffTime = dOut - dIn;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        const nights = diffDays > 0 ? diffDays : 1;

        document.getElementById('displayNights').innerText = nights + ' đêm';
        const total = nights * price;
        document.getElementById('displayEstimatedTotal').innerText = total.toLocaleString('vi-VN') + ' đ';
    }
}

document.addEventListener("DOMContentLoaded", calculateEstimatedTotal);
