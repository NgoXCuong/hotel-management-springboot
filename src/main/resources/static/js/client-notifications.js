(function () {
    const bellBtn = document.getElementById('notificationBellBtn');
    if (!bellBtn) return;

    const badge = document.getElementById('notificationBadge');
    const listEl = document.getElementById('notifList');

    function getCsrf() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.getAttribute('content') : null;
    }

    function formatTime(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const now = new Date();
        const diffMs = now - date;
        const diffMin = Math.floor(diffMs / 60000);

        if (diffMin < 1) return 'Vừa xong';
        if (diffMin < 60) return diffMin + ' phút trước';
        const diffHours = Math.floor(diffMin / 60);
        if (diffHours < 24) return diffHours + ' giờ trước';
        const diffDays = Math.floor(diffHours / 24);
        if (diffDays === 1) return 'Hôm qua';
        if (diffDays < 7) return diffDays + ' ngày trước';
        return date.toLocaleDateString('vi-VN');
    }

    function badgeColorMap(icon) {
        if (!icon) return { bg: '#eff6ff', color: '#2563eb' };
        if (icon.includes('check') || icon.includes('confirm') || icon.includes('shield')) return { bg: '#ecfdf5', color: '#059669' };
        if (icon.includes('gift') || icon.includes('percent') || icon.includes('tag')) return { bg: '#fffbeb', color: '#d97706' };
        if (icon.includes('cancel') || icon.includes('xmark') || icon.includes('triangle')) return { bg: '#fef2f2', color: '#dc2626' };
        return { bg: '#eff6ff', color: '#2563eb' };
    }

    function renderNotifications(notifications, unreadCount) {
        if (badge) {
            badge.textContent = unreadCount;
            badge.classList.toggle('d-none', unreadCount === 0);
        }

        if (!listEl) return;
        if (!notifications || notifications.length === 0) {
            listEl.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <i class="fa-regular fa-bell-slash mb-2 d-block" style="font-size: 32px; opacity: 0.35;"></i>
                    <div class="small fw-semibold">Không có thông báo nào</div>
                    <div class="small" style="font-size: 11.5px;">Các cập nhật về đơn đặt phòng sẽ hiển thị tại đây</div>
                </div>`;
            return;
        }

        listEl.innerHTML = notifications.map(function (n) {
            const colors = badgeColorMap(n.icon);
            const iconClass = n.icon || 'fa-solid fa-bell';
            const cls = n.read ? 'notif-item' : 'notif-item unread';
            const linkTarget = n.link ? n.link : '/my-account?tab=notifications';
            return `
                <a href="${linkTarget}" class="${cls}" data-id="${n.id}" data-read="${n.read ? 'true' : 'false'}">
                    <div class="notif-icon-box" style="background: ${colors.bg}; color: ${colors.color};">
                        <i class="${iconClass}"></i>
                    </div>
                    <div class="notif-content">
                        <div class="notif-title">
                            ${n.read ? '' : '<span class="notif-unread-dot"></span>'}
                            <span>${escapeHtml(n.title)}</span>
                        </div>
                        <div class="notif-message">${escapeHtml(n.message || '')}</div>
                        <div class="notif-time"><i class="fa-regular fa-clock me-1"></i>${formatTime(n.createdAt)}</div>
                    </div>
                </a>`;
        }).join('');

        listEl.querySelectorAll('.notif-item:not([data-read="true"])').forEach(function (item) {
            item.addEventListener('click', function () {
                markOneRead(item);
            });
        });
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function loadNotifications() {
        fetch('/api/notifications')
            .then(function (res) { return res.json(); })
            .then(function (data) {
                renderNotifications(data.notifications, data.unreadCount);
            })
            .catch(function () {
                if (listEl) {
                    listEl.innerHTML = '<div class="text-center py-4 text-muted small">Không thể tải thông báo</div>';
                }
            });
    }

    function markOneRead(item) {
        const id = item.getAttribute('data-id');
        fetch('/api/notifications/' + id + '/read', {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': getCsrf()
            }
        }).then(function () {
            item.classList.remove('unread');
            item.setAttribute('data-read', 'true');
            item.querySelector('.notif-unread-dot')?.remove();
            if (badge) {
                const count = parseInt(badge.textContent || '0', 10);
                badge.textContent = Math.max(0, count - 1);
                badge.classList.toggle('d-none', badge.textContent === '0');
            }
        }).catch(function () {});
    }

    window.markAllRead = function () {
        const btn = document.getElementById('markAllReadBtn');
        if (btn) {
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';
            btn.disabled = true;
        }
        fetch('/api/notifications/read-all', {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': getCsrf()
            }
        }).then(function () {
            if (listEl) {
                listEl.querySelectorAll('.notif-item.unread').forEach(function (item) {
                    item.classList.remove('unread');
                    item.setAttribute('data-read', 'true');
                    item.querySelector('.notif-unread-dot')?.remove();
                });
            }
            if (badge) {
                badge.textContent = '0';
                badge.classList.add('d-none');
            }
        }).catch(function () {}).finally(function () {
            if (btn) {
                btn.innerHTML = 'Đánh dấu đã đọc';
                btn.disabled = false;
            }
        });
    };

    bellBtn.addEventListener('click', function () {
        loadNotifications();
    });

    document.addEventListener('click', function (e) {
        const panel = document.getElementById('notifPanel');
        const drop = document.getElementById('notifDropdown');
        if (!drop || !panel) return;
        if (!drop.contains(e.target)) {
            const open = bootstrap.Dropdown.getOrCreateInstance(bellBtn);
            if (open && panel.classList.contains('show')) {
                open.hide();
            }
        }
    });

    loadNotifications();
})();
