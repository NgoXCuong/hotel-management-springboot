let serviceModalInstance = null;

            document.addEventListener('DOMContentLoaded', () => {
                serviceModalInstance = new bootstrap.Modal(document.getElementById('serviceModal'));
            });

            function openCreateServiceModal() {
                const form = document.getElementById('serviceForm');
                form.reset();
                form.action = '/admin/services/create';
                document.getElementById('serviceId').value = '';
                document.getElementById('serviceModalTitle').innerText = 'Thêm mới Dịch vụ';
                document.getElementById('serviceActive').checked = true;

                // Reset preview
                const preview = document.getElementById('modalServiceImgPreview');
                const placeholder = document.getElementById('modalServiceImgPlaceholder');
                preview.src = '';
                preview.classList.add('d-none');
                placeholder.classList.remove('d-none');

                serviceModalInstance.show();
            }

            function openEditServiceModal(btn) {
                const id = btn.getAttribute('data-id');
                const name = btn.getAttribute('data-name') || '';
                const price = btn.getAttribute('data-price') || '0';
                const description = btn.getAttribute('data-description') || '';
                const active = btn.getAttribute('data-active') === 'true';
                const imageUrl = btn.getAttribute('data-image-url') || '';

                const form = document.getElementById('serviceForm');
                form.action = '/admin/services/edit/' + id;
                document.getElementById('serviceId').value = id;
                document.getElementById('serviceName').value = name;
                document.getElementById('servicePrice').value = price;
                document.getElementById('serviceDescription').value = description;
                document.getElementById('serviceActive').checked = active;
                document.getElementById('serviceModalTitle').innerText = 'Chỉnh sửa Dịch vụ #' + id;

                // Preview ảnh
                const preview = document.getElementById('modalServiceImgPreview');
                const placeholder = document.getElementById('modalServiceImgPlaceholder');
                if (imageUrl && imageUrl.trim() !== '') {
                    preview.src = imageUrl;
                    preview.classList.remove('d-none');
                    placeholder.classList.add('d-none');
                } else {
                    preview.src = '';
                    preview.classList.add('d-none');
                    placeholder.classList.remove('d-none');
                }

                serviceModalInstance.show();
            }