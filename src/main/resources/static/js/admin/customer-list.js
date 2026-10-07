let customerModalInstance = null;

document.addEventListener('DOMContentLoaded', function() {
    customerModalInstance = new bootstrap.Modal(document.getElementById('customerModal'));
});

function openCreateCustomerModal() {
    const form = document.getElementById('customerForm');
    form.reset();
    form.action = '/admin/customers/create';
    document.getElementById('customerId').value = '';
    document.getElementById('customerCode').value = '';
    document.getElementById('custNationality').value = 'Việt Nam';
    document.getElementById('customerModalTitle').innerText = 'Tiếp nhận Khách hàng mới';
    document.getElementById('custActive').checked = true;

    const maleRadio = document.getElementById('modalGender_MALE');
    if (maleRadio) maleRadio.checked = true;

    customerModalInstance.show();
}

function openEditCustomerModal(btn) {
    const id = btn.getAttribute('data-id');
    const code = btn.getAttribute('data-code') || '';
    const name = btn.getAttribute('data-name') || '';
    const phone = btn.getAttribute('data-phone') || '';
    const email = btn.getAttribute('data-email') || '';
    const dob = btn.getAttribute('data-dob') || '';
    const identity = btn.getAttribute('data-identity') || '';
    const gender = btn.getAttribute('data-gender') || 'MALE';
    const nationality = btn.getAttribute('data-nationality') || 'Việt Nam';
    const address = btn.getAttribute('data-address') || '';
    const active = btn.getAttribute('data-active') === 'true';

    const form = document.getElementById('customerForm');
    form.action = '/admin/customers/edit/' + id;
    document.getElementById('customerId').value = id;
    document.getElementById('customerCode').value = code;
    document.getElementById('custFullName').value = name;
    document.getElementById('custPhone').value = phone;
    document.getElementById('custEmail').value = email;
    document.getElementById('custDob').value = dob;
    document.getElementById('custIdentity').value = identity;
    document.getElementById('custNationality').value = nationality;
    document.getElementById('custAddress').value = address;
    document.getElementById('custActive').checked = active;
    document.getElementById('customerModalTitle').innerText = 'Cập nhật Khách hàng [' + (code ? code : '#' + id) + ']';

    const genderRadio = document.getElementById('modalGender_' + gender);
    if (genderRadio) genderRadio.checked = true;

    customerModalInstance.show();
}
