/**
 * Grand Hotel AI Chatbot Widget
 * Tích hợp trợ lý ảo thông minh Groq Cloud AI
 */
document.addEventListener('DOMContentLoaded', function () {
    const chatToggleBtn = document.getElementById('grandChatToggleBtn');
    const chatWindow = document.getElementById('grandChatWindow');
    const chatCloseBtn = document.getElementById('grandChatCloseBtn');
    const chatClearBtn = document.getElementById('grandChatClearBtn');
    const chatBody = document.getElementById('grandChatBody');
    const chatInput = document.getElementById('grandChatInput');
    const chatSendBtn = document.getElementById('grandChatSendBtn');
    const greetingTip = document.getElementById('grandChatGreetingTip');
    const closeGreetingTipBtn = document.getElementById('grandCloseGreetingTip');

    if (!chatToggleBtn || !chatWindow) return;

    const STORAGE_KEY = 'grand_hotel_chat_history';
    let isRequesting = false;

    // Khởi tạo lịch sử hội thoại từ sessionStorage
    let conversationHistory = loadHistory();

    // 1. Render lịch sử đã lưu hoặc lời chào mở đầu
    if (conversationHistory.length === 0) {
        showWelcomeMessage();
    } else {
        renderStoredMessages();
    }

    // Đóng popup tooltip chào sau 10 giây nếu người dùng không bấm
    if (greetingTip) {
        setTimeout(() => {
            if (greetingTip) greetingTip.style.display = 'none';
        }, 12000);

        if (closeGreetingTipBtn) {
            closeGreetingTipBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                greetingTip.style.display = 'none';
            });
        }

        greetingTip.addEventListener('click', () => {
            toggleChat(true);
            greetingTip.style.display = 'none';
        });
    }

    // 2. Toggle mở / đóng hộp chat
    chatToggleBtn.addEventListener('click', () => {
        const isOpen = chatWindow.classList.contains('is-open');
        toggleChat(!isOpen);
    });

    if (chatCloseBtn) {
        chatCloseBtn.addEventListener('click', () => toggleChat(false));
    }

    function toggleChat(open) {
        if (open) {
            chatWindow.classList.add('is-open');
            chatToggleBtn.classList.add('is-active');
            if (greetingTip) greetingTip.style.display = 'none';
            setTimeout(() => {
                if (chatInput) chatInput.focus();
                scrollToBottom();
            }, 250);
        } else {
            chatWindow.classList.remove('is-open');
            chatToggleBtn.classList.remove('is-active');
        }
    }

    // 3. Xóa lịch sử chat
    if (chatClearBtn) {
        chatClearBtn.addEventListener('click', () => {
            if (confirm('Quý khách muốn làm mới và xóa toàn bộ cuộc trò chuyện?')) {
                conversationHistory = [];
                sessionStorage.removeItem(STORAGE_KEY);
                chatBody.innerHTML = '';
                showWelcomeMessage();
            }
        });
    }

    // 4. Gửi tin nhắn
    function handleSend() {
        if (isRequesting) return;
        const text = chatInput.value.trim();
        if (!text) return;

        // Render tin nhắn của User
        appendUserMessage(text);
        conversationHistory.push({ role: 'user', content: text });
        saveHistory();

        chatInput.value = '';
        chatInput.disabled = true;
        chatSendBtn.disabled = true;
        isRequesting = true;

        // Hiển thị hiệu ứng Bot đang gõ...
        const typingEl = showTypingIndicator();
        scrollToBottom();

        // Gửi POST request tới Backend Spring Boot
        fetch('/api/chatbot/message', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: text,
                history: conversationHistory.slice(-8) // Gửi tối đa 8 tin nhắn gần nhất
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Lỗi máy chủ: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            removeTypingIndicator(typingEl);
            const reply = data.reply || 'Dạ, em chưa rõ thông tin này. Quý khách vui lòng gọi hotline 1900 8888 để được hỗ trợ nhé!';
            appendBotMessage(reply, data.suggestedRooms);
            conversationHistory.push({ role: 'assistant', content: reply });
            saveHistory();
        })
        .catch(error => {
            console.error('Lỗi Chatbot:', error);
            removeTypingIndicator(typingEl);
            appendBotMessage('Dạ, kết nối bị gián đoạn đôi chút. Quý khách vui lòng thử lại hoặc liên hệ hotline **1900 8888** để nhân viên hỗ trợ ngay nhé!', []);
        })
        .finally(() => {
            chatInput.disabled = false;
            chatSendBtn.disabled = false;
            isRequesting = false;
            chatInput.focus();
            scrollToBottom();
        });
    }

    if (chatSendBtn) {
        chatSendBtn.addEventListener('click', handleSend);
    }

    if (chatInput) {
        chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
            }
        });
    }

    // 5. Click vào các gợi ý nhanh (Suggestion Chips)
    document.addEventListener('click', (e) => {
        const chip = e.target.closest('.grand-chat-chip');
        if (chip) {
            const query = chip.getAttribute('data-query') || chip.innerText.trim();
            if (chatInput) {
                chatInput.value = query;
                handleSend();
            }
        }
    });

    // 6. Helpers hiển thị & Format Markdown
    function showWelcomeMessage() {
        const welcomeText = `Xin chào quý khách! Em là **Grand Bot** - Trợ lý ảo của **Grand Hotel Resort & Spa** 🌟\n\nEm có thể hỗ trợ quý khách:\n- 🛏️ Tư vấn & tra cứu giá phòng theo nhu cầu\n- 🎁 Các chương trình khuyến mãi & mã Voucher giảm giá\n- 🔍 Kiểm tra tình trạng đơn đặt phòng qua mã đơn\n- 🍽️ Dịch vụ đưa đón sân bay, buffet, spa cao cấp\n\nQuý khách muốn tìm hiểu thông tin gì ạ?`;
        
        appendBotMessage(welcomeText, null, true);
    }

    function appendUserMessage(text) {
        const now = getCurrentTime();
        const msgDiv = document.createElement('div');
        msgDiv.className = 'grand-chat-msg msg-user';
        msgDiv.innerHTML = `
            <div class="grand-chat-msg-bubble">${escapeHtml(text)}</div>
            <div class="grand-chat-time">${now}</div>
        `;
        chatBody.appendChild(msgDiv);
        scrollToBottom();
    }

    function appendBotMessage(text, suggestedRooms, isWelcome = false) {
        const now = getCurrentTime();
        const msgDiv = document.createElement('div');
        msgDiv.className = 'grand-chat-msg msg-bot';

        let formattedText = formatMarkdown(text);

        let roomsHtml = '';
        if (suggestedRooms && suggestedRooms.length > 0) {
            roomsHtml = '<div class="mt-2 d-flex flex-column gap-2">';
            suggestedRooms.forEach(room => {
                const imgTag = room.imageUrl 
                    ? `<img src="${room.imageUrl}" alt="${escapeHtml(room.name)}" onerror="this.style.display='none'">` 
                    : '';
                roomsHtml += `
                    <div class="grand-chat-room-card">
                        ${imgTag}
                        <div class="grand-chat-room-info">
                            <div class="grand-chat-room-title">${escapeHtml(room.name)}</div>
                            <div class="grand-chat-room-price"><i class="fa-solid fa-tag me-1"></i>${escapeHtml(room.priceFormatted)}</div>
                            <a href="${room.detailUrl}" class="grand-chat-room-btn">
                                <i class="fa-solid fa-arrow-up-right-from-square me-1"></i>Xem & Đặt phòng
                            </a>
                        </div>
                    </div>
                `;
            });
            roomsHtml += '</div>';
        }

        let suggestionsHtml = '';
        if (isWelcome) {
            suggestionsHtml = `
                <div class="grand-chat-suggestions">
                    <span class="grand-chat-chip" data-query="Khách sạn đang có những chương trình khuyến mãi nào?">🏷️ Khuyến mãi hôm nay</span>
                    <span class="grand-chat-chip" data-query="Tư vấn giúp tôi phòng view biển cho 2 người">🌊 Phòng view biển 2 người</span>
                    <span class="grand-chat-chip" data-query="Quy định giờ nhận phòng và trả phòng của khách sạn?">🕒 Giờ nhận / trả phòng</span>
                    <span class="grand-chat-chip" data-query="Kiểm tra trạng thái đơn đặt phòng">🔍 Tra cứu mã đơn</span>
                </div>
            `;
        }

        msgDiv.innerHTML = `
            <div class="msg-bot-avatar"><i class="fa-solid fa-robot"></i></div>
            <div class="d-flex flex-column" style="max-width: 85%;">
                <div class="grand-chat-msg-bubble">
                    ${formattedText}
                    ${roomsHtml}
                    ${suggestionsHtml}
                </div>
                <div class="grand-chat-time">${now}</div>
            </div>
        `;

        chatBody.appendChild(msgDiv);
        scrollToBottom();
    }

    function showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'grand-chat-msg msg-bot grand-typing-container';
        typingDiv.innerHTML = `
            <div class="msg-bot-avatar"><i class="fa-solid fa-robot"></i></div>
            <div class="grand-chat-typing">
                <span></span><span></span><span></span>
            </div>
        `;
        chatBody.appendChild(typingDiv);
        return typingDiv;
    }

    function removeTypingIndicator(el) {
        if (el && el.parentNode) {
            el.parentNode.removeChild(el);
        }
    }

    function formatMarkdown(text) {
        if (!text) return '';

        // Chuẩn hóa các thẻ <br> từ câu trả lời của AI thành ký tự xuống dòng
        let cleanText = text.replace(/<br\s*\/?>/gi, '\n');

        if (typeof marked !== 'undefined' && typeof marked.parse === 'function') {
            try {
                marked.setOptions({
                    breaks: true,
                    gfm: true
                });
                return marked.parse(cleanText);
            } catch (err) {
                console.warn('Lỗi parse marked:', err);
            }
        }

        // Fallback an toàn nếu chưa load kịp marked.js
        let escaped = escapeHtml(cleanText);
        escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        escaped = escaped.replace(/\*(.*?)\*/g, '<em>$1</em>');
        escaped = escaped.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" class="text-decoration-underline fw-semibold" style="color: #A68760;">$1</a>');
        escaped = escaped.replace(/^\s*[-•]\s+(.*)$/gim, '<div class="d-flex align-items-start gap-1 mb-1"><span style="color:#C5A880;">•</span> <span>$1</span></div>');
        escaped = escaped.replace(/\n/g, '<br>');
        return escaped;
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.innerText = str;
        return div.innerHTML;
    }

    function getCurrentTime() {
        const d = new Date();
        const h = String(d.getHours()).padStart(2, '0');
        const m = String(d.getMinutes()).padStart(2, '0');
        return `${h}:${m}`;
    }

    function scrollToBottom() {
        setTimeout(() => {
            chatBody.scrollTop = chatBody.scrollHeight;
        }, 50);
    }

    function saveHistory() {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(conversationHistory.slice(-15)));
        } catch (e) {
            console.warn('Không thể lưu session:', e);
        }
    }

    function loadHistory() {
        try {
            const raw = sessionStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function renderStoredMessages() {
        conversationHistory.forEach(msg => {
            if (msg.role === 'user') {
                appendUserMessage(msg.content);
            } else {
                appendBotMessage(msg.content, null);
            }
        });
    }
});
