/**
 * ====================================================================
 * HOTEL ICON PICKER - BỘ CHỌN BIỂU TƯỢNG ĐẶC THÙ CHO KHÁCH SẠN
 * ====================================================================
 * - Danh mục ~60 icon chọn lọc chuyên dùng cho Khách sạn, Resort, Homestay
 * - Lưới chọn trực quan với nhãn tiếng Việt
 * - Cho phép dán/nhập mã FontAwesome tùy chỉnh bất kỳ ngoài danh mục có sẵn
 * - Tự động active viền vàng Gold và cập nhật Live Preview
 * - Tích hợp tìm kiếm nhanh theo tên tiếng Việt hoặc mã icon
 */

(function (window, document) {
    'use strict';

    // 1. Danh sách ~60 Icon đặc thù khách sạn phân loại khoa học
    const HOTEL_ICONS = [
        // --- NHÓM 1: PHÒNG NGỦ & TRANG THIẾT BỊ (ROOM & AMENITIES) ---
        { icon: 'fa-solid fa-wifi', name: 'Wifi tốc độ cao', category: 'room', keywords: 'wifi internet mạng khong day' },
        { icon: 'fa-solid fa-snowflake', name: 'Điều hòa không khí', category: 'room', keywords: 'dieu hoa may lanh nhiet do mat' },
        { icon: 'fa-solid fa-tv', name: 'Smart TV màn hình phẳng', category: 'room', keywords: 'tivi truyen hinh cap netflix truyen hinh' },
        { icon: 'fa-solid fa-bath', name: 'Bồn tắm nằm', category: 'room', keywords: 'bon tam nam massage phong tam' },
        { icon: 'fa-solid fa-shower', name: 'Vòi sen đứng / Nước nóng', category: 'room', keywords: 'voi sen tam nong lanh phong tam' },
        { icon: 'fa-solid fa-vault', name: 'Két sắt an toàn', category: 'room', keywords: 'ket sat an toan bao mat tien bac' },
        { icon: 'fa-solid fa-wine-bottle', name: 'Minibar / Tủ lạnh mini', category: 'room', keywords: 'minibar tu lanh nuoc uong do uong' },
        { icon: 'fa-solid fa-mug-hot', name: 'Ấm đun nước / Cà phê', category: 'room', keywords: 'am dun binh dun tra ca phe free' },
        { icon: 'fa-solid fa-wind', name: 'Máy sấy tóc', category: 'room', keywords: 'may say toc lam dep phong tam' },
        { icon: 'fa-solid fa-shirt', name: 'Bàn là / Cầu ủi quần áo', category: 'room', keywords: 'ban la ban ui quan ao moc treo' },
        { icon: 'fa-solid fa-bed', name: 'Giường King / Queen', category: 'room', keywords: 'giuong ngu king queen dem nem chan ga' },
        { icon: 'fa-solid fa-couch', name: 'Ghế Sofa tiếp khách', category: 'room', keywords: 'ghe sofa ban tiep khach salon thu gian' },
        { icon: 'fa-solid fa-laptop', name: 'Bàn làm việc cao cấp', category: 'room', keywords: 'ban lam viec laptop van phong' },
        { icon: 'fa-solid fa-door-open', name: 'Ban công riêng ngắm cảnh', category: 'room', keywords: 'ban cong cua so view thoang' },
        { icon: 'fa-solid fa-water', name: 'Hướng nhìn ra biển', category: 'room', keywords: 'view bien sea view dai duong song' },
        { icon: 'fa-solid fa-mountain-sun', name: 'Hướng nhìn ra núi', category: 'room', keywords: 'view nui mountain phong canh thien nhien' },
        { icon: 'fa-solid fa-city', name: 'View toàn cảnh thành phố', category: 'room', keywords: 'view thanh pho city view pho thi' },
        { icon: 'fa-solid fa-key', name: 'Khóa cửa thẻ từ', category: 'room', keywords: 'khoa cua the tu smart lock an toan' },
        { icon: 'fa-solid fa-phone', name: 'Điện thoại bàn nội bộ', category: 'room', keywords: 'dien thoai ban le tan noi bo' },
        { icon: 'fa-solid fa-fan', name: 'Quạt trần làm mát', category: 'room', keywords: 'quat tran gio mat' },
        { icon: 'fa-solid fa-volume-xmark', name: 'Cách âm chống ồn', category: 'room', keywords: 'cach am chong on yen tinh rieng tu' },
        { icon: 'fa-solid fa-soap', name: 'Đồ vệ sinh cá nhân', category: 'room', keywords: 'do ve sinh xa bong dau goi sua tam' },
        { icon: 'fa-solid fa-socks', name: 'Dép đi trong nhà', category: 'room', keywords: 'dep di trong phong le di phong' },
        { icon: 'fa-solid fa-vest-patches', name: 'Áo choàng tắm', category: 'room', keywords: 'ao choang tam khan tam cao cap' },

        // --- NHÓM 2: ẨM THỰC & DỊCH VỤ PHỤC VỤ (DINING & ROOM SERVICE) ---
        { icon: 'fa-solid fa-utensils', name: 'Bữa sáng Buffet / Nhà hàng', category: 'dining', keywords: 'bua sang an sang buffet am thuc nha hang' },
        { icon: 'fa-solid fa-martini-glass-citrus', name: 'Quầy Bar / Cocktail', category: 'dining', keywords: 'quay bar cocktail ruou vang lounge' },
        { icon: 'fa-solid fa-bell-concierge', name: 'Dịch vụ phòng 24/7', category: 'dining', keywords: 'phuc vu phong goi do an le tan 247 room service' },
        { icon: 'fa-solid fa-bowl-food', name: 'Thực đơn gọi món (A la carte)', category: 'dining', keywords: 'thuc don goi mon a la carte mon an' },
        { icon: 'fa-solid fa-champagne-glasses', name: 'Tiệc nướng & Tổ chức sự kiện', category: 'dining', keywords: 'tiec nuong bbq su kien sinh nhat party' },

        // --- NHÓM 3: TIỆN ÍCH & THƯ GIÃN (WELLNESS & LEISURE) ---
        { icon: 'fa-solid fa-person-swimming', name: 'Hồ bơi ngoài trời / Vô cực', category: 'wellness', keywords: 'ho boi be boi ngoai troi vo cuc tam bien' },
        { icon: 'fa-solid fa-dumbbell', name: 'Phòng Gym / Thể hình', category: 'wellness', keywords: 'phong tap gym the hinh fitness yoga' },
        { icon: 'fa-solid fa-spa', name: 'Dịch vụ Spa & Massage', category: 'wellness', keywords: 'spa massage thu gian cham soc sac dep' },
        { icon: 'fa-solid fa-hot-tub-person', name: 'Bồn sục Jacuzzi / Xông hơi', category: 'wellness', keywords: 'bon suc jacuzzi xong hoi sauna steam' },
        { icon: 'fa-solid fa-umbrella-beach', name: 'Bãi biển riêng / Ghế tắm nắng', category: 'wellness', keywords: 'bai bien rieng ghe tam nang du che' },
        { icon: 'fa-solid fa-children', name: 'Khu vui chơi trẻ em (Kid Club)', category: 'wellness', keywords: 'khu vui choi tre em kid club em be' },
        { icon: 'fa-solid fa-tree', name: 'Khu vườn / Không gian xanh', category: 'wellness', keywords: 'san vuon cay xanh garden thoang mat' },

        // --- NHÓM 4: VẬN CHUYỂN, AN NINH & HỖ TRỢ (FACILITIES & SERVICES) ---
        { icon: 'fa-solid fa-elevator', name: 'Thang máy di chuyển', category: 'facility', keywords: 'thang may elevator tang lau tien loi' },
        { icon: 'fa-solid fa-square-parking', name: 'Chỗ đỗ xe ô tô miễn phí', category: 'facility', keywords: 'bai do xe cho de xe o to xe may free' },
        { icon: 'fa-solid fa-plane-arrival', name: 'Đưa đón sân bay', category: 'facility', keywords: 'dua don san bay airport shuttle xe dua don' },
        { icon: 'fa-solid fa-car', name: 'Thuê xe du lịch / Taxi', category: 'facility', keywords: 'thue xe tu lai taxi du lich xe may' },
        { icon: 'fa-solid fa-jug-detergent', name: 'Dịch vụ giặt là / Giặt khô', category: 'facility', keywords: 'giat la giat kho giat ui laundry' },
        { icon: 'fa-solid fa-suitcase', name: 'Dịch vụ giữ hành lý', category: 'facility', keywords: 'gui do giu hanh ly luggage storage' },
        { icon: 'fa-solid fa-clock', name: 'Lễ tân phục vụ 24/24', category: 'facility', keywords: 'le tan truc 2424 checkin muon' },
        { icon: 'fa-solid fa-shield-halved', name: 'An ninh / Bảo vệ 24/7', category: 'facility', keywords: 'an ninh bao ve 247 an toan camera' },
        { icon: 'fa-solid fa-fire-extinguisher', name: 'Hệ thống báo cháy tự động', category: 'facility', keywords: 'bao chay chua chay an toan phong chay' },
        { icon: 'fa-solid fa-ban-smoking', name: 'Phòng cấm hút thuốc', category: 'facility', keywords: 'cam hut thuoc no smoking khong khi sach' },
        { icon: 'fa-solid fa-smoking', name: 'Khu vực hút thuốc riêng', category: 'facility', keywords: 'khu hut thuoc rieng smoking area' },
        { icon: 'fa-solid fa-wheelchair', name: 'Lối đi cho người khuyết tật', category: 'facility', keywords: 'nguoi khuyet tat xe lan tien ich' },
        { icon: 'fa-solid fa-paw', name: 'Thân thiện với thú cưng', category: 'facility', keywords: 'thu cung cho meo pet friendly' },
        { icon: 'fa-solid fa-money-bill-wave', name: 'Thu đổi ngoại tệ', category: 'facility', keywords: 'doi ngoai te tien te usd eur quy doi' },
        { icon: 'fa-solid fa-credit-card', name: 'Chấp nhận thanh toán thẻ', category: 'facility', keywords: 'thanh toan the visa mastercard tin dung' },
        { icon: 'fa-solid fa-handshake', name: 'Phòng họp & Hội nghị', category: 'facility', keywords: 'phong hop hoi nghi business center' },
        { icon: 'fa-solid fa-camera', name: 'Tour du lịch & Tham quan', category: 'facility', keywords: 'tour du lich tham quan dat ve' },
        { icon: 'fa-solid fa-map-location-dot', name: 'Vị trí trung tâm du lịch', category: 'facility', keywords: 'vi tri trung tam pho co gan bien' },
        { icon: 'fa-solid fa-gift', name: 'Cửa hàng quà lưu niệm', category: 'facility', keywords: 'qua luu niem shopping qua tang' }
    ];

    // 2. Danh mục tabs
    const CATEGORIES = [
        { id: 'all', name: 'Tất cả icon (~60)' },
        { id: 'room', name: 'Phòng & Tiện nghi' },
        { id: 'dining', name: 'Ẩm thực & Bar' },
        { id: 'wellness', name: 'Thư giãn & Hồ bơi' },
        { id: 'facility', name: 'Dịch vụ & Vận chuyển' }
    ];

    // 3. Inject CSS tự động
    function injectStyles() {
        if (document.getElementById('hotel-icon-picker-styles')) return;

        const style = document.createElement('style');
        style.id = 'hotel-icon-picker-styles';
        style.textContent = `
            .hip-container {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 14px;
                padding: 16px;
                box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
            }
            .hip-header {
                display: flex;
                flex-wrap: wrap;
                gap: 10px;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 14px;
            }
            .hip-search-input {
                font-size: 13px;
                border-radius: 8px;
                padding: 7px 12px;
                border: 1px solid #cbd5e1;
                width: 100%;
                max-width: 260px;
                background: #f8fafc;
                transition: all 0.2s ease;
            }
            .hip-search-input:focus {
                background: #ffffff;
                border-color: #d97706;
                outline: none;
                box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.15);
            }
            .hip-tabs {
                display: flex;
                gap: 6px;
                flex-wrap: wrap;
                margin-bottom: 12px;
                border-bottom: 1px solid #f1f5f9;
                padding-bottom: 8px;
            }
            .hip-tab-btn {
                font-size: 12px;
                font-weight: 600;
                padding: 4px 12px;
                border-radius: 20px;
                border: 1px solid #e2e8f0;
                background: #f8fafc;
                color: #64748b;
                cursor: pointer;
                transition: all 0.2s ease;
            }
            .hip-tab-btn:hover {
                background: #f1f5f9;
                color: #334155;
            }
            .hip-tab-btn.active {
                background: #fffbeb;
                color: #b45309;
                border-color: #fde68a;
                font-weight: 700;
            }
            .hip-grid {
                display: grid;
                grid-template-columns: repeat(auto-fill, minmax(95px, 1fr));
                gap: 8px;
                max-height: 240px;
                overflow-y: auto;
                padding: 4px 2px;
            }
            .hip-grid::-webkit-scrollbar {
                width: 6px;
            }
            .hip-grid::-webkit-scrollbar-track {
                background: #f8fafc;
                border-radius: 4px;
            }
            .hip-grid::-webkit-scrollbar-thumb {
                background: #cbd5e1;
                border-radius: 4px;
            }
            .hip-grid::-webkit-scrollbar-thumb:hover {
                background: #94a3b8;
            }
            .hip-item {
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                padding: 10px 6px;
                border: 1.5px solid #e2e8f0;
                border-radius: 10px;
                background: #ffffff;
                cursor: pointer;
                transition: all 0.18s ease;
                text-align: center;
                user-select: none;
                min-height: 76px;
            }
            .hip-item:hover {
                border-color: #cbd5e1;
                background: #f8fafc;
                transform: translateY(-2px);
                box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);
            }
            .hip-item i {
                font-size: 20px;
                color: #475569;
                margin-bottom: 6px;
                transition: color 0.15s ease;
            }
            .hip-item span {
                font-size: 11px;
                color: #64748b;
                line-height: 1.25;
                font-weight: 500;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }
            /* TRẠNG THÁI ACTIVE VIỀN VÀNG GOLD SANG TRỌNG */
            .hip-item.active {
                border: 2px solid #d97706 !important;
                background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%) !important;
                box-shadow: 0 4px 12px rgba(217, 119, 6, 0.22) !important;
                transform: scale(1.03);
            }
            .hip-item.active i {
                color: #b45309 !important;
            }
            .hip-item.active span {
                color: #92400e !important;
                font-weight: 700 !important;
            }
            .hip-preview-card {
                background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
                border: 2px solid #fde68a;
                border-radius: 12px;
                padding: 12px 16px;
                display: flex;
                align-items: center;
                gap: 14px;
                margin-top: 12px;
            }
            .hip-preview-icon-box {
                width: 48px;
                height: 48px;
                border-radius: 10px;
                background: #ffffff;
                border: 1px solid #fde68a;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 22px;
                color: #b45309;
                box-shadow: 0 2px 6px rgba(217, 119, 6, 0.15);
                flex-shrink: 0;
            }
            .hip-empty-msg {
                grid-column: 1 / -1;
                text-align: center;
                padding: 24px 10px;
                color: #94a3b8;
                font-size: 13px;
            }
            .hip-custom-wrap {
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 10px;
                padding: 10px 14px;
                margin-top: 12px;
            }
            .hip-custom-input {
                font-size: 13px;
                font-family: 'JetBrains Mono', monospace;
            }
            .hip-custom-input:focus {
                border-color: #d97706;
                box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.15);
            }
        `;
        document.head.appendChild(style);
    }

    // Helper bỏ dấu tiếng Việt để tìm kiếm thông minh
    function removeVietnameseTones(str) {
        if (!str) return '';
        str = str.toLowerCase();
        str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, "a");
        str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, "e");
        str = str.replace(/ì|í|ị|ỉ|ĩ/g, "i");
        str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, "o");
        str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, "u");
        str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, "y");
        str = str.replace(/đ/g, "d");
        return str;
    }

    // Làm sạch chuỗi icon (loại bỏ thẻ <i class="..."></i> nếu người dùng paste cả thẻ HTML)
    function sanitizeIconClass(input) {
        if (!input) return '';
        let clean = input.trim();
        // Nếu user paste <i class="fa-solid fa-xxx"></i> hoặc class="fa-solid fa-xxx"
        const tagMatch = clean.match(/class=["']([^"']+)["']/);
        if (tagMatch) {
            clean = tagMatch[1];
        }
        // Nếu chỉ paste tên icon mà thiếu fa-solid / fa-regular (vd: "fa-wifi" hoặc "wifi")
        if (!clean.includes('fa-') && clean.length > 0) {
            clean = 'fa-solid fa-' + clean;
        } else if (clean.startsWith('fa-') && !clean.includes(' ')) {
            clean = 'fa-solid ' + clean;
        }
        return clean.trim();
    }

    // 4. Class chính: HotelIconPicker
    class HotelIconPicker {
        constructor(options) {
            this.targetInput = typeof options.targetInput === 'string' ? document.querySelector(options.targetInput) : options.targetInput;
            this.container = typeof options.container === 'string' ? document.querySelector(options.container) : options.container;
            this.previewBox = options.previewBox ? (typeof options.previewBox === 'string' ? document.querySelector(options.previewBox) : options.previewBox) : null;
            this.currentCategory = 'all';
            this.searchQuery = '';
            this.selectedIcon = this.targetInput ? this.targetInput.value.trim() : (options.defaultIcon || 'fa-solid fa-wifi');

            injectStyles();
            this.render();
            this.bindEvents();
        }

        render() {
            if (!this.container) return;

            let html = `
                <div class="hip-container">
                    <!-- Header: Title & Search -->
                    <div class="hip-header">
                        <div class="fw-bold text-dark" style="font-size: 13px;">
                            <i class="fa-solid fa-icons text-warning me-1.5" style="color: #d97706 !important;"></i>
                            Chọn từ ~60 biểu tượng khách sạn có sẵn:
                        </div>
                        <input type="text" class="hip-search-input" placeholder="Tìm kiếm (vd: wifi, điều hòa, bồn tắm)..." />
                    </div>

                    <!-- Category Tabs -->
                    <div class="hip-tabs">
                        ${CATEGORIES.map(cat => `
                            <button type="button" class="hip-tab-btn ${this.currentCategory === cat.id ? 'active' : ''}" data-cat="${cat.id}">
                                ${cat.name}
                            </button>
                        `).join('')}
                    </div>

                    <!-- Icon Grid -->
                    <div class="hip-grid" id="hipGridArea"></div>

                    <!-- Live Preview Bar -->
                    <div class="hip-preview-card">
                        <div class="hip-preview-icon-box" id="hipLiveIconPreview">
                            <i class="${this.selectedIcon || 'fa-solid fa-circle-question'}"></i>
                        </div>
                        <div class="flex-grow-1 overflow-hidden">
                            <div class="fw-bold text-dark mb-0.5 text-truncate" id="hipLiveIconName" style="font-size: 13.5px;">
                                ${this.getIconName(this.selectedIcon)}
                            </div>
                            <div class="text-muted font-monospace small text-truncate" style="font-size: 12px;" id="hipLiveIconCode">
                                ${this.selectedIcon || 'Chưa chọn biểu tượng'}
                            </div>
                        </div>
                    </div>

                    <!-- Ô DÁN / NHẬP MÃ CUSTOM ICON (NẾU KHÔNG CÓ TRONG DANH SÁCH) -->
                    <div class="hip-custom-wrap">
                        <div class="d-flex justify-content-between align-items-center mb-1.5">
                            <label class="small fw-semibold text-dark mb-0" style="font-size: 12px;">
                                <i class="fa-solid fa-code text-primary me-1"></i>
                                Hoặc dán / nhập mã FontAwesome tùy ý:
                            </label>
                            <a href="https://fontawesome.com/search?o=r&m=free" target="_blank" class="text-primary text-decoration-none small" style="font-size: 11px;">
                                Tra cứu FontAwesome Free <i class="fa-solid fa-arrow-up-right-from-square ms-0.5"></i>
                            </a>
                        </div>
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white text-muted border-end-0">
                                <i class="fa-solid fa-paste"></i>
                            </span>
                            <input type="text" class="form-control hip-custom-input border-start-0" id="hipCustomInput"
                                   placeholder="Dán mã tại đây, ví dụ: fa-solid fa-gamepad, fa-solid fa-umbrella..." 
                                   value="${this.selectedIcon || ''}" />
                            <button class="btn btn-dark hip-apply-custom-btn fw-semibold" type="button" id="hipApplyCustomBtn">
                                <i class="fa-solid fa-check me-1"></i> Áp dụng
                            </button>
                        </div>
                        <div class="text-muted mt-1" style="font-size: 11px;">
                            * Hỗ trợ dán trực tiếp chuỗi class hoặc dán cả thẻ <code>&lt;i class="fa-solid fa-..."&gt;</code>
                        </div>
                    </div>
                </div>
            `;

            this.container.innerHTML = html;
            this.renderGrid();
        }

        getIconName(iconClass) {
            if (!iconClass) return 'Chưa chọn biểu tượng';
            const found = HOTEL_ICONS.find(item => item.icon === iconClass);
            return found ? found.name : 'Biểu tượng tùy chỉnh (' + iconClass + ')';
        }

        renderGrid() {
            const gridArea = this.container.querySelector('#hipGridArea');
            if (!gridArea) return;

            const normalizedQuery = removeVietnameseTones(this.searchQuery.trim());

            const filtered = HOTEL_ICONS.filter(item => {
                // Lọc theo Category
                const matchCategory = (this.currentCategory === 'all' || item.category === this.currentCategory);
                if (!matchCategory) return false;

                // Lọc theo Search Query
                if (!normalizedQuery) return true;
                const matchName = removeVietnameseTones(item.name).includes(normalizedQuery);
                const matchCode = item.icon.toLowerCase().includes(normalizedQuery);
                const matchKeywords = item.keywords ? removeVietnameseTones(item.keywords).includes(normalizedQuery) : false;

                return matchName || matchCode || matchKeywords;
            });

            if (filtered.length === 0) {
                const cleanQuery = sanitizeIconClass(this.searchQuery);
                gridArea.innerHTML = `
                    <div class="hip-empty-msg">
                        <i class="fa-regular fa-face-frown fs-4 mb-2 d-block text-muted"></i>
                        Không có trong ~60 biểu tượng khách sạn có sẵn.
                        <div class="mt-2">
                            <button type="button" class="btn btn-sm btn-outline-primary hip-quick-use-btn rounded-pill px-3 py-1 fw-semibold" data-custom-code="${cleanQuery}">
                                <i class="fa-solid fa-wand-magic-sparkles me-1"></i> Dùng luôn mã "${cleanQuery}"
                            </button>
                        </div>
                        <div class="text-muted mt-2 small" style="font-size: 11.5px;">Hoặc dán trực tiếp mã FontAwesome vào ô bên dưới.</div>
                    </div>
                `;

                // Bind Quick Use Button
                const quickBtn = gridArea.querySelector('.hip-quick-use-btn');
                if (quickBtn) {
                    quickBtn.addEventListener('click', () => {
                        const code = quickBtn.getAttribute('data-custom-code');
                        this.selectIcon(code, 'Biểu tượng tùy chỉnh: ' + code);
                    });
                }
                return;
            }

            gridArea.innerHTML = filtered.map(item => {
                const isActive = (item.icon === this.selectedIcon);
                return `
                    <div class="hip-item ${isActive ? 'active' : ''}" data-icon="${item.icon}" data-name="${item.name}" title="${item.name} (${item.icon})">
                        <i class="${item.icon}"></i>
                        <span>${item.name}</span>
                    </div>
                `;
            }).join('');
        }

        bindEvents() {
            // 1. Search Input
            const searchInput = this.container.querySelector('.hip-search-input');
            if (searchInput) {
                searchInput.addEventListener('input', (e) => {
                    this.searchQuery = e.target.value;
                    this.renderGrid();
                });
            }

            // 2. Category Tabs
            const tabButtons = this.container.querySelectorAll('.hip-tab-btn');
            tabButtons.forEach(btn => {
                btn.addEventListener('click', () => {
                    tabButtons.forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    this.currentCategory = btn.getAttribute('data-cat');
                    this.renderGrid();
                });
            });

            // 3. Click chọn Icon từ Grid
            const gridArea = this.container.querySelector('#hipGridArea');
            if (gridArea) {
                gridArea.addEventListener('click', (e) => {
                    const item = e.target.closest('.hip-item');
                    if (!item) return;

                    const iconClass = item.getAttribute('data-icon');
                    const iconName = item.getAttribute('data-name');
                    this.selectIcon(iconClass, iconName);
                });
            }

            // 4. Ô dán / nhập Custom FontAwesome Icon
            const customInput = this.container.querySelector('#hipCustomInput');
            const applyCustomBtn = this.container.querySelector('#hipApplyCustomBtn');

            if (customInput) {
                // Nhập / Dán và áp dụng tức thì khi gõ/paste
                customInput.addEventListener('input', (e) => {
                    const cleanClass = sanitizeIconClass(e.target.value);
                    if (cleanClass) {
                        this.selectIcon(cleanClass, this.getIconName(cleanClass), true, false);
                    }
                });

                customInput.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        const cleanClass = sanitizeIconClass(customInput.value);
                        if (cleanClass) {
                            this.selectIcon(cleanClass, this.getIconName(cleanClass));
                        }
                    }
                });
            }

            if (applyCustomBtn && customInput) {
                applyCustomBtn.addEventListener('click', () => {
                    const cleanClass = sanitizeIconClass(customInput.value);
                    if (cleanClass) {
                        this.selectIcon(cleanClass, this.getIconName(cleanClass));
                    }
                });
            }

            // 5. Nếu người dùng gõ trực tiếp vào ô input mục tiêu (hidden/text ngoài form)
            if (this.targetInput) {
                this.targetInput.addEventListener('input', (e) => {
                    const typedClass = sanitizeIconClass(e.target.value);
                    this.selectIcon(typedClass, this.getIconName(typedClass), false);
                });
            }
        }

        selectIcon(iconClass, iconName, updateInput = true, updateCustomInput = true) {
            const cleanIcon = sanitizeIconClass(iconClass);
            this.selectedIcon = cleanIcon;

            // Cập nhật ô input đích
            if (updateInput && this.targetInput) {
                this.targetInput.value = cleanIcon;
                this.targetInput.dispatchEvent(new Event('input', { bubbles: true }));
                this.targetInput.dispatchEvent(new Event('change', { bubbles: true }));
            }

            // Cập nhật ô Custom Input (nếu cần)
            if (updateCustomInput) {
                const customInput = this.container.querySelector('#hipCustomInput');
                if (customInput && customInput.value !== cleanIcon) {
                    customInput.value = cleanIcon;
                }
            }

            // Cập nhật viền Active trong Grid
            const allItems = this.container.querySelectorAll('.hip-item');
            allItems.forEach(el => {
                if (el.getAttribute('data-icon') === cleanIcon) {
                    el.classList.add('active');
                } else {
                    el.classList.remove('active');
                }
            });

            // Cập nhật Live Preview trong Picker Box
            const previewIcon = this.container.querySelector('#hipLiveIconPreview i');
            const previewName = this.container.querySelector('#hipLiveIconName');
            const previewCode = this.container.querySelector('#hipLiveIconCode');

            if (previewIcon) {
                previewIcon.className = cleanIcon || 'fa-solid fa-circle-question';
            }
            if (previewName) {
                previewName.textContent = iconName || this.getIconName(cleanIcon);
            }
            if (previewCode) {
                previewCode.textContent = cleanIcon || 'Chưa chọn biểu tượng';
            }

            // Cập nhật Live Preview ngoài form (nếu có)
            if (this.previewBox) {
                const extIcon = this.previewBox.querySelector('i') || this.previewBox;
                if (extIcon) {
                    extIcon.className = cleanIcon || 'fa-solid fa-circle-question';
                }
            }
        }
    }

    // Xuất ra biến toàn cục
    window.HotelIconPicker = {
        init: function (options) {
            return new HotelIconPicker(options);
        },
        icons: HOTEL_ICONS
    };

})(window, document);
