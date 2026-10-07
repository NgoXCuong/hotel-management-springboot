function togglePassword(inputId, iconId) {
            const input = document.getElementById(inputId);
            const icon = document.getElementById(iconId);
            if (input.type === 'password') {
                input.type = 'text';
                icon.classList.replace('fa-eye', 'fa-eye-slash');
            } else {
                input.type = 'password';
                icon.classList.replace('fa-eye-slash', 'fa-eye');
            }
        }

        // Kiểm tra mật khẩu xác nhận
        document.getElementById('registerForm').addEventListener('submit', function (e) {
            const pw = document.getElementById('password').value;
            const cpw = document.getElementById('confirmPassword').value;
            const errEl = document.getElementById('passwordError');
            const cpwInput = document.getElementById('confirmPassword');

            if (pw !== cpw) {
                e.preventDefault();
                errEl.style.display = 'block';
                cpwInput.classList.add('input-error');
            } else {
                errEl.style.display = 'none';
                cpwInput.classList.remove('input-error');
            }
        });