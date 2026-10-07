let roomTypeModalInstance = null;
            let amenityModalInstance = null;
            let amenityPickerInstance = null;

            document.addEventListener('DOMContentLoaded', () => {
                roomTypeModalInstance = new bootstrap.Modal(document.getElementById('roomTypeModal'));
                amenityModalInstance = new bootstrap.Modal(document.getElementById('amenityModal'));

                // Khởi tạo Hotel Icon Picker
                amenityPickerInstance = HotelIconPicker.init({
                    targetInput: '#amenityIcon',
                    container: '#modalAmenityIconPickerContainer'
                });

                const amenitiesTab = document.getElementById('amenities-tab');
                if (amenitiesTab && amenitiesTab.classList.contains('active')) {
                    switchHeaderButton('amenities');
                }
            });

            function switchHeaderButton(tab) {
                const addRoomTypeBtn = document.getElementById('addRoomTypeBtn');
                const addAmenityBtn = document.getElementById('addAmenityBtn');

                if (tab === 'amenities') {
                    if (addRoomTypeBtn) addRoomTypeBtn.style.display = 'none';
                    if (addAmenityBtn) addAmenityBtn.style.display = 'inline-flex';
                } else {
                    if (addRoomTypeBtn) addRoomTypeBtn.style.display = 'inline-flex';
                    if (addAmenityBtn) addAmenityBtn.style.display = 'none';
                }
            }

            // ============ ROOM TYPE MODAL ============
            function openCreateRoomTypeModal() {
                const form = document.getElementById('roomTypeForm');
                form.reset();
                form.action = '/admin/room-types/create';
                document.getElementById('roomTypeId').value = '';
                document.getElementById('roomTypeModalTitle').innerText = 'Thêm mới Loại phòng';
                document.getElementById('rtActive').checked = true;

                // Reset Checkboxes
                document.querySelectorAll('.rt-amenity-checkbox').forEach(cb => cb.checked = false);

                // Reset Gallery
                document.getElementById('modalNewGalleryContainer').innerHTML = '';
                document.getElementById('modalExistingGalleryContainer').innerHTML = '';
                document.getElementById('modalExistingGalleryWrapper').classList.add('d-none');

                roomTypeModalInstance.show();
            }

            function openEditRoomTypeModal(btn, e) {
                const id = btn.getAttribute('data-id');
                const name = btn.getAttribute('data-name') || '';
                const price = btn.getAttribute('data-price') || '';
                const guests = btn.getAttribute('data-guests') || '';
                const description = btn.getAttribute('data-description') || '';
                const active = btn.getAttribute('data-active') === 'true';
                const amenityIdsStr = btn.getAttribute('data-amenity-ids') || '';
                const amenityIds = amenityIdsStr ? amenityIdsStr.split(',') : [];

                const form = document.getElementById('roomTypeForm');
                form.action = '/admin/room-types/edit/' + id;
                document.getElementById('roomTypeId').value = id;
                document.getElementById('rtName').value = name;
                document.getElementById('rtPrice').value = price;
                document.getElementById('rtGuests').value = guests;
                document.getElementById('rtDescription').value = description;
                document.getElementById('rtActive').checked = active;
                document.getElementById('roomTypeModalTitle').innerText = 'Cập nhật Loại phòng #' + id;

                // Checkboxes
                document.querySelectorAll('.rt-amenity-checkbox').forEach(cb => {
                    cb.checked = amenityIds.includes(cb.value);
                });

                // Reset New Gallery Preview
                document.getElementById('modalNewGalleryContainer').innerHTML = '';

                // Populate Existing Images
                const existingContainer = document.getElementById('modalExistingGalleryContainer');
                const existingWrapper = document.getElementById('modalExistingGalleryWrapper');
                existingContainer.innerHTML = '';

                const imageSpans = btn.querySelectorAll('.room-type-images-json span');
                if (imageSpans && imageSpans.length > 0) {
                    existingWrapper.classList.remove('d-none');
                    imageSpans.forEach(span => {
                        const url = span.getAttribute('data-url');
                        const isPrimary = span.getAttribute('data-primary') === 'true';
                        if (url) {
                            const itemDiv = document.createElement('div');
                            itemDiv.className = 'existing-image-item';
                            itemDiv.innerHTML = `
                                <img src="${url}" alt="Room" class="w-100 h-100" style="object-fit: cover;">
                                ${isPrimary ? '<span class="badge bg-warning position-absolute top-0 start-0 m-1" style="font-size: 8px;">Chính</span>' : ''}
                                <button type="button" class="btn btn-danger btn-sm position-absolute top-0 end-0 m-1 p-0 rounded-circle d-flex align-items-center justify-content-center"
                                        style="width: 18px; height: 18px; font-size: 10px;" title="Xóa ảnh"
                                        onclick="removeExistingImage(this, '${url}')">&times;</button>
                            `;
                            existingContainer.appendChild(itemDiv);
                        }
                    });
                } else {
                    existingWrapper.classList.add('d-none');
                }

                roomTypeModalInstance.show();
            }

            // ============ AMENITY MODAL ============
            function openCreateAmenityModal() {
                const form = document.getElementById('amenityForm');
                form.reset();
                form.action = '/admin/amenities/create';
                document.getElementById('amenityId').value = '';
                document.getElementById('amenityModalTitle').innerText = 'Thêm mới Tiện nghi';
                document.getElementById('amenityActive').checked = true;

                if (amenityPickerInstance) {
                    amenityPickerInstance.selectIcon('fa-solid fa-wifi', 'Wifi tốc độ cao');
                }

                amenityModalInstance.show();
            }

            function openEditAmenityModal(btn) {
                const id = btn.getAttribute('data-id');
                const name = btn.getAttribute('data-name') || '';
                const icon = btn.getAttribute('data-icon') || 'fa-solid fa-wifi';
                const description = btn.getAttribute('data-description') || '';
                const active = btn.getAttribute('data-active') === 'true';

                const form = document.getElementById('amenityForm');
                form.action = '/admin/amenities/edit/' + id;
                document.getElementById('amenityId').value = id;
                document.getElementById('amenityName').value = name;
                document.getElementById('amenityIcon').value = icon;
                document.getElementById('amenityDescription').value = description;
                document.getElementById('amenityActive').checked = active;
                document.getElementById('amenityModalTitle').innerText = 'Cập nhật Tiện nghi #' + id;

                if (amenityPickerInstance) {
                    amenityPickerInstance.selectIcon(icon, amenityPickerInstance.getIconName(icon));
                }

                amenityModalInstance.show();
            }