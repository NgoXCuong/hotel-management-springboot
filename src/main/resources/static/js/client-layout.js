document.addEventListener('DOMContentLoaded', function () {
    const successToastEl = document.getElementById('liveSuccessToast');
    if (successToastEl) {
        const toast = new bootstrap.Toast(successToastEl, { delay: 4500 });
        toast.show();
    }

    const errorToastEl = document.getElementById('liveErrorToast');
    if (errorToastEl) {
        const toast = new bootstrap.Toast(errorToastEl, { delay: 6000 });
        toast.show();
    }
});
