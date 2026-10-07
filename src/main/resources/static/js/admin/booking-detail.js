function increaseQty() {
    const input = document.getElementById('serviceQtyInput');
    let val = parseInt(input.value) || 1;
    if (val < 99) input.value = val + 1;
}

function decreaseQty() {
    const input = document.getElementById('serviceQtyInput');
    let val = parseInt(input.value) || 1;
    if (val > 1) input.value = val - 1;
}
