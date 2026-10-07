let roomModalInstance = null;

            document.addEventListener('DOMContentLoaded', () => {
                roomModalInstance = new bootstrap.Modal(document.getElementById('roomModal'));
            });

            function openCreateRoomModal() {
                const form = document.getElementById('roomForm');
                form.reset();
                form.action = '/admin/rooms/create';
                document.getElementById('roomId').value = '';
                document.getElementById('roomModalTitle').innerText = 'Thêm mới Phòng';
                document.getElementById('roomStatusSelect').value = 'AVAILABLE';
                document.getElementById('roomActiveInput').checked = true;

                roomModalInstance.show();
            }

            function openEditRoomModal(btn) {
                const id = btn.getAttribute('data-id');
                const roomNumber = btn.getAttribute('data-room-number') || '';
                const floor = btn.getAttribute('data-floor') || '';
                const roomTypeId = btn.getAttribute('data-room-type-id') || '';
                const status = btn.getAttribute('data-status') || 'AVAILABLE';
                const description = btn.getAttribute('data-description') || '';
                const active = btn.getAttribute('data-active') === 'true';

                const form = document.getElementById('roomForm');
                form.action = '/admin/rooms/edit/' + id;
                document.getElementById('roomId').value = id;
                document.getElementById('roomNumberInput').value = roomNumber;
                document.getElementById('roomFloorInput').value = floor;
                document.getElementById('roomTypeSelect').value = roomTypeId;
                document.getElementById('roomStatusSelect').value = status;
                document.getElementById('roomDescriptionInput').value = description;
                document.getElementById('roomActiveInput').checked = active;
                document.getElementById('roomModalTitle').innerText = 'Cập nhật Phòng ' + roomNumber;

                roomModalInstance.show();
            }

            function openQuickBookModal(btn) {
                const roomId = btn.getAttribute('data-id');
                const roomNumber = btn.getAttribute('data-number');
                const roomType = btn.getAttribute('data-type');

                document.getElementById('modalRoomId').value = roomId;
                document.getElementById('modalRoomNumber').innerText = 'P.' + roomNumber;
                document.getElementById('modalRoomType').innerText = roomType;

                const now = new Date();
                const tomorrow = new Date();
                tomorrow.setDate(tomorrow.getDate() + 1);
                tomorrow.setHours(12, 0, 0, 0);

                const pad = (n) => (n < 10 ? '0' + n : n);
                const formatDate = (d) => d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());

                document.getElementById('modalCheckIn').value = formatDate(now);
                document.getElementById('modalCheckOut').value = formatDate(tomorrow);

                const modal = new bootstrap.Modal(document.getElementById('quickBookModal'));
                modal.show();
            }