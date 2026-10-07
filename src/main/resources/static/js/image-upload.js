/**
 * Grand Hotel - Centralized Image Upload & Preview Helper
 * Supports:
 * 1. Single Image Preview (Avatars, Hotel Services, Thumbnails)
 * 2. Multi-image Gallery Preview with remove capability before upload (Room Types)
 */

document.addEventListener('DOMContentLoaded', function () {
    initAllSinglePreviews();
    initAllGalleryPreviews();
});

/**
 * Khởi tạo tự động cho tất cả các input có attribute `data-single-preview`
 * Format: data-single-preview="#previewImgId" data-placeholder="#placeholderId"
 */
function initAllSinglePreviews() {
    const singleInputs = document.querySelectorAll('input[type="file"][data-single-preview]');
    singleInputs.forEach(input => {
        const previewTargetSelector = input.getAttribute('data-single-preview');
        const placeholderSelector = input.getAttribute('data-placeholder');
        initSingleImagePreview(input, previewTargetSelector, placeholderSelector);
    });
}

/**
 * Xử lý live preview cho 1 ảnh
 */
function initSingleImagePreview(inputElement, previewTargetSelector, placeholderSelector) {
    const input = typeof inputElement === 'string' ? document.querySelector(inputElement) : inputElement;
    if (!input) return;

    const previewImg = document.querySelector(previewTargetSelector);
    const placeholder = placeholderSelector ? document.querySelector(placeholderSelector) : null;

    input.addEventListener('change', function (event) {
        const file = event.target.files && event.target.files[0];
        if (file) {
            // Validate client-side (size & extension)
            if (file.size > 5 * 1024 * 1024) {
                alert('Dung lượng ảnh không được vượt quá 5MB!');
                input.value = '';
                return;
            }

            const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
            if (!validTypes.includes(file.type)) {
                alert('Vui lòng chọn định dạng ảnh JPG, JPEG, PNG hoặc WEBP!');
                input.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = function (e) {
                if (previewImg) {
                    previewImg.src = e.target.result;
                    previewImg.classList.remove('d-none');
                    previewImg.style.display = 'block';
                }
                if (placeholder) {
                    placeholder.classList.add('d-none');
                    placeholder.style.display = 'none';
                }
            };
            reader.readAsDataURL(file);
        }
    });
}

/**
 * Khởi tạo tự động cho các input có attribute `data-gallery-preview`
 * Format: data-gallery-preview="#galleryContainerId"
 */
function initAllGalleryPreviews() {
    const galleryInputs = document.querySelectorAll('input[type="file"][data-gallery-preview]');
    galleryInputs.forEach(input => {
        const containerSelector = input.getAttribute('data-gallery-preview');
        initGalleryPreview(input, containerSelector);
    });
}

/**
 * Xử lý live preview cho nhiều ảnh (Gallery) với khả năng loại bỏ từng ảnh trước khi submit
 */
function initGalleryPreview(inputElement, containerSelector) {
    const input = typeof inputElement === 'string' ? document.querySelector(inputElement) : inputElement;
    const container = document.querySelector(containerSelector);
    if (!input || !container) return;

    let dt = new DataTransfer();

    input.addEventListener('change', function (event) {
        const files = Array.from(event.target.files);

        files.forEach(file => {
            if (file.size > 5 * 1024 * 1024) {
                alert(`Ảnh "${file.name}" vượt quá 5MB và sẽ bị bỏ qua.`);
                return;
            }
            const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
            if (!validTypes.includes(file.type)) {
                alert(`Ảnh "${file.name}" không đúng định dạng và sẽ bị bỏ qua.`);
                return;
            }

            dt.items.add(file);

            const reader = new FileReader();
            reader.onload = function (e) {
                const itemDiv = document.createElement('div');
                itemDiv.className = 'gallery-preview-item position-relative rounded-3 overflow-hidden border shadow-sm';
                itemDiv.style.width = '110px';
                itemDiv.style.height = '85px';
                itemDiv.style.background = '#f8fafc';
                itemDiv.dataset.filename = file.name;

                itemDiv.innerHTML = `
                    <img src="${e.target.result}" alt="Preview" class="w-100 h-100" style="object-fit: cover;">
                    <span class="badge bg-dark bg-opacity-75 position-absolute bottom-0 start-0 m-1 text-truncate" style="max-width: 90px; font-size: 9px;">${file.name}</span>
                    <button type="button" class="btn btn-danger btn-sm position-absolute top-0 end-0 m-1 p-0 rounded-circle d-flex align-items-center justify-content-center btn-remove-preview"
                            style="width: 20px; height: 20px; font-size: 11px;" title="Xóa ảnh này">
                        &times;
                    </button>
                `;

                // Xử lý nút xóa ảnh preview
                itemDiv.querySelector('.btn-remove-preview').addEventListener('click', function () {
                    itemDiv.remove();
                    // Loại bỏ file khỏi DataTransfer
                    const newDt = new DataTransfer();
                    for (let i = 0; i < dt.files.length; i++) {
                        if (dt.files[i].name !== file.name || dt.files[i].size !== file.size) {
                            newDt.items.add(dt.files[i]);
                        }
                    }
                    dt = newDt;
                    input.files = dt.files;
                });

                container.appendChild(itemDiv);
            };
            reader.readAsDataURL(file);
        });

        input.files = dt.files;
    });
}

/**
 * Xóa ảnh cũ đã lưu từ database (được đánh dấu để xóa khi submit)
 */
function removeExistingImage(button, imageUrl) {
    const item = button.closest('.existing-image-item');
    if (item) {
        // Tạo hidden input để gửi danh sách ảnh bị xóa lên backend nếu cần
        const form = item.closest('form');
        if (form) {
            const hiddenInput = document.createElement('input');
            hiddenInput.type = 'hidden';
            hiddenInput.name = 'deletedImageUrls';
            hiddenInput.value = imageUrl;
            form.appendChild(hiddenInput);
        }
        item.remove();
    }
}
