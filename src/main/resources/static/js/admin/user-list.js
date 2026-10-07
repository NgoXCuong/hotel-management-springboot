let userModalInstance = null;
let resetPasswordModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    userModalInstance = new bootstrap.Modal(document.getElementById('userModal'));
    resetPasswordModalInstance = new bootstrap.Modal(document.getElementById('resetPasswordModal'));
});

function openCreateUserModal() {
    const form = document.getElementById('userForm');
    form.reset();
    form.action = '/admin/users/create';
    document.getElementById('userId').value = '';
    document.getElementById('userModalTitle').innerText = 'Cấp mới Tài khoản Nhân viên';

    const usernameInput = document.getElementById('modalUsernameInput');
    usernameInput.readOnly = false;
    usernameInput.classList.remove('bg-white');
    usernameInput.classList.add('bg-light');

    const passwordInput = document.getElementById('modalInputPassword');
    passwordInput.required = true;
    passwordInput.name = 'rawPassword';
    document.getElementById('modalPasswordRequiredStar').classList.remove('d-none');
    document.getElementById('modalPasswordNote').classList.add('d-none');

    document.querySelectorAll('.user-role-checkbox').forEach(cb => cb.checked = false);
    document.getElementById('modalEnabled').checked = true;
    document.getElementById('modalAccountNonLocked').checked = true;

    // Reset Avatar Preview
    const preview = document.getElementById('modalAvatarPreview');
    const placeholder = document.getElementById('modalAvatarPlaceholder');
    preview.src = '';
    preview.classList.add('d-none');
    placeholder.classList.remove('d-none');

    userModalInstance.show();
}

function openEditUserModal(btn) {
    const id = btn.getAttribute('data-id');
    const username = btn.getAttribute('data-username') || '';
    const fullname = btn.getAttribute('data-fullname') || '';
    const email = btn.getAttribute('data-email') || '';
    const phone = btn.getAttribute('data-phone') || '';
    const enabled = btn.getAttribute('data-enabled') === 'true';
    const locked = btn.getAttribute('data-locked') === 'true';
    const avatarUrl = btn.getAttribute('data-avatar-url') || '';
    const roleIdsStr = btn.getAttribute('data-role-ids') || '';
    const roleIds = roleIdsStr ? roleIdsStr.split(',') : [];

    const form = document.getElementById('userForm');
    form.action = '/admin/users/edit/' + id;
    document.getElementById('userId').value = id;
    document.getElementById('userModalTitle').innerText = 'Cập nhật Tài khoản @' + username;

    const usernameInput = document.getElementById('modalUsernameInput');
    usernameInput.value = username;
    usernameInput.readOnly = true;

    const passwordInput = document.getElementById('modalInputPassword');
    passwordInput.required = false;
    passwordInput.value = '';
    passwordInput.name = 'newRawPassword';
    document.getElementById('modalPasswordRequiredStar').classList.add('d-none');
    document.getElementById('modalPasswordNote').classList.remove('d-none');

    document.getElementById('modalFullName').value = fullname;
    document.getElementById('modalEmail').value = email;
    document.getElementById('modalPhone').value = phone;
    document.getElementById('modalEnabled').checked = enabled;
    document.getElementById('modalAccountNonLocked').checked = !locked;

    // Roles Checkboxes
    document.querySelectorAll('.user-role-checkbox').forEach(cb => {
        cb.checked = roleIds.includes(cb.value);
    });

    // Avatar Preview
    const preview = document.getElementById('modalAvatarPreview');
    const placeholder = document.getElementById('modalAvatarPlaceholder');
    if (avatarUrl && avatarUrl.trim() !== '') {
        preview.src = avatarUrl;
        preview.classList.remove('d-none');
        placeholder.classList.add('d-none');
    } else {
        preview.src = '';
        preview.classList.add('d-none');
        placeholder.classList.remove('d-none');
    }

    userModalInstance.show();
}

function openResetPasswordModal(btn) {
    const id = btn.getAttribute('data-id');
    const username = btn.getAttribute('data-username');

    document.getElementById('modalUsername').innerText = '@' + username;
    document.getElementById('resetPasswordForm').action = '/admin/users/reset-password/' + id;
    document.getElementById('modalNewPassword').value = '';

    resetPasswordModalInstance.show();
}

function togglePasswordVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    const icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
}
