document.addEventListener("DOMContentLoaded", function() {
    const iconInput = document.getElementById('icon');
    if (!iconInput.value) {
        iconInput.value = 'fa-solid fa-wifi';
    }

    HotelIconPicker.init({
        targetInput: '#icon',
        container: '#hotelIconPickerContainer'
    });
});
