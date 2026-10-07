function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('show');
}

// Tự động kích hoạt Toast khi có Flash Message từ Controller
document.addEventListener('DOMContentLoaded', function () {
    const successToastEl = document.getElementById('liveSuccessToast');
    if (successToastEl) {
        const toast = new bootstrap.Toast(successToastEl, { delay: 4000 });
        toast.show();
    }

    const errorToastEl = document.getElementById('liveErrorToast');
    if (errorToastEl) {
        const toast = new bootstrap.Toast(errorToastEl, { delay: 5500 });
        toast.show();
    }

    // Khởi tạo tất cả Bootstrap Tooltip trên trang
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});

// Hàm JavaScript toàn cục để trang con gọi khi cần hiển thị Toast động
window.showToast = function(type, message) {
    const container = document.querySelector('.toast-container');
    if (!container) return;

    const isSuccess = (type === 'success');
    const toastId = 'toast_' + Date.now();
    const borderCol = isSuccess ? '#10b981' : '#ef4444';
    const iconBg = isSuccess ? '#ecfdf5' : '#fef2f2';
    const iconClass = isSuccess ? 'fa-solid fa-circle-check text-success' : 'fa-solid fa-triangle-exclamation text-danger';
    const titleText = isSuccess ? 'Thành công' : 'Thông báo lỗi';

    const toastHtml = `
        <div id="${toastId}" class="toast luxury-toast align-items-center border-0 mb-2" role="alert" aria-live="assertive" aria-atomic="true"
             style="border-left: 5px solid ${borderCol} !important;">
            <div class="d-flex p-3">
                <div class="d-flex align-items-center me-3">
                    <div class="rounded-circle d-flex align-items-center justify-content-center" style="width: 38px; height: 38px; background: ${iconBg};">
                        <i class="${iconClass}" style="font-size: 20px;"></i>
                    </div>
                </div>
                <div class="toast-body p-0 flex-grow-1 align-self-center">
                    <div class="fw-bold text-dark mb-0.5" style="font-size: 13.5px;">${titleText}</div>
                    <div class="text-secondary small">${message}</div>
                </div>
                <button type="button" class="btn-close ms-2 my-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', toastHtml);
    const newToastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(newToastEl, { delay: isSuccess ? 4000 : 5500 });
    toast.show();
    newToastEl.addEventListener('hidden.bs.toast', () => newToastEl.remove());
};
// Tự động tiêm _csrf vào các form POST không có th:action (fix 403 khi submit form qua JS)
document.addEventListener('submit', function (e) {
    var form = e.target;
    if (form.method && form.method.toLowerCase() === 'post') {
        if (!form.querySelector('input[name="_csrf"]')) {
            var csrfMeta = document.querySelector('meta[name="_csrf"]');
            if (csrfMeta) {
                var input = document.createElement('input');
                input.type = 'hidden';
                input.name = '_csrf';
                input.value = csrfMeta.getAttribute('content');
                form.appendChild(input);
            }
        }
    }
}, true);
