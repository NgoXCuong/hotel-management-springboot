function handleContactSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('contactName').value.trim();
    const phone = document.getElementById('contactPhone').value.trim();
    const email = document.getElementById('contactEmail').value.trim();
    const topic = document.getElementById('contactTopic').value;
    const message = document.getElementById('contactMessage').value.trim();

    const body = encodeURIComponent(
        "Họ tên: " + name + "\n" +
        "SĐT: " + phone + "\n" +
        (email ? "Email: " + email + "\n" : "") +
        "Chủ đề: " + topic + "\n" +
        "Nội dung:\n" + message
    );

    const mailto = "mailto:reservation@grandhotel.vn?subject=" + encodeURIComponent("Yêu cầu tư vấn từ " + name) + "&body=" + body;
    window.location.href = mailto;
}