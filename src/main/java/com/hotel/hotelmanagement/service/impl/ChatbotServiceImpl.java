package com.hotel.hotelmanagement.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.hotel.hotelmanagement.dto.chatbot.ChatMessageDTO;
import com.hotel.hotelmanagement.dto.chatbot.ChatRequestDTO;
import com.hotel.hotelmanagement.dto.chatbot.ChatResponseDTO;
import com.hotel.hotelmanagement.entity.Booking;
import com.hotel.hotelmanagement.entity.HotelService;
import com.hotel.hotelmanagement.entity.Promotion;
import com.hotel.hotelmanagement.entity.RoomType;
import com.hotel.hotelmanagement.repository.BookingRepository;
import com.hotel.hotelmanagement.repository.HotelServiceRepository;
import com.hotel.hotelmanagement.repository.PromotionRepository;
import com.hotel.hotelmanagement.repository.RoomTypeRepository;
import com.hotel.hotelmanagement.repository.UserRepository;
import com.hotel.hotelmanagement.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.api.model:openai/gpt-oss-120b}")
    private String model;

    @Value("${groq.api.timeout-seconds:30}")
    private int timeoutSeconds;

    private final RoomTypeRepository roomTypeRepository;
    private final PromotionRepository promotionRepository;
    private final HotelServiceRepository hotelServiceRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("(?i)\\b(BK-[A-Z0-9_-]+)\\b");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public ChatResponseDTO chat(ChatRequestDTO request, Principal principal) {
        String userMessage = request != null && request.getMessage() != null ? request.getMessage().trim() : "";
        if (userMessage.isEmpty()) {
            return ChatResponseDTO.builder()
                    .success(false)
                    .reply("Xin chào! Em có thể giúp gì cho kỳ nghỉ của quý khách tại Grand Hotel?")
                    .suggestedRooms(Collections.emptyList())
                    .timestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                    .build();
        }

        try {
            // 1. Chuẩn bị ngữ cảnh hệ thống (Dữ liệu thực tế từ Database)
            String systemPrompt = buildSystemPrompt(userMessage, principal);

            // 2. Chuẩn bị danh sách messages gửi tới Groq
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // Thêm lịch sử hội thoại gần nhất (tối đa 6 tin nhắn trước để giữ context)
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                List<ChatMessageDTO> recentHistory = request.getHistory();
                int startIdx = Math.max(0, recentHistory.size() - 6);
                for (int i = startIdx; i < recentHistory.size(); i++) {
                    ChatMessageDTO msg = recentHistory.get(i);
                    if (msg.getContent() != null && !msg.getContent().isBlank()) {
                        String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant";
                        messages.add(Map.of("role", role, "content", msg.getContent().trim()));
                    }
                }
            }

            // Thêm tin nhắn hiện tại của người dùng
            messages.add(Map.of("role", "user", "content", userMessage));

            // 3. Gọi Groq Cloud API
            String replyContent = callGroqApi(messages);

            // 4. Trích xuất gợi ý phòng phù hợp từ database để hiển thị card trực quan
            List<ChatResponseDTO.RoomSuggestionDTO> suggestedRooms = extractSuggestedRooms(userMessage, replyContent);

            return ChatResponseDTO.builder()
                    .success(true)
                    .reply(replyContent)
                    .suggestedRooms(suggestedRooms)
                    .timestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi xử lý chatbot với Groq API: {}", e.getMessage(), e);

            // Phản hồi thân thiện fallback nếu có sự cố mạng/API
            String fallbackReply = "Dạ, hiện tại hệ thống trợ lý ảo đang bảo trì kết nối một chút. " +
                    "Quý khách vui lòng liên hệ hotline **1900 8888** hoặc gửi yêu cầu tới quầy lễ tân để được hỗ trợ tức thì nhé!";

            return ChatResponseDTO.builder()
                    .success(false)
                    .reply(fallbackReply)
                    .suggestedRooms(Collections.emptyList())
                    .timestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                    .build();
        }
    }

    /**
     * Xây dựng System Prompt chứa toàn bộ dữ liệu thực tế từ CSDL
     */
    private String buildSystemPrompt(String userMessage, Principal principal) {
        StringBuilder sb = new StringBuilder();

        sb.append("Bạn là 'Grand Bot' - Trợ lý ảo lễ tân 5 sao của khách sạn Grand Hotel Resort & Luxury Spa Nha Trang.\n");
        sb.append("Thông tin chung khách sạn:\n");
        sb.append("- Địa chỉ: 123 Trần Phú, Lộc Thọ, TP. Nha Trang, Khánh Hòa.\n");
        sb.append("- Hotline 24/7: 1900 8888 hoặc (0258) 3888 999.\n");
        sb.append("- Giờ nhận phòng (Check-in): 14:00 | Giờ trả phòng (Check-out): 12:00.\n");
        sb.append("- Chính sách hủy phòng: Miễn phí hủy trước 48h trước giờ nhận phòng.\n");
        sb.append("- Phương thức thanh toán: Chuyển khoản ngân hàng tự động qua VietQR / SePay, thẻ tín dụng, tiền mặt tại quầy.\n\n");

        if (principal != null) {
            String username = principal.getName();
            userRepository.findByUsername(username).ifPresent(user -> {
                sb.append("Khách hàng đang trò chuyện đã đăng nhập với tên: ").append(user.getFullName()).append(" (Email: ").append(user.getEmail()).append(").\n\n");
            });
        }

        // Lấy danh sách hạng phòng từ DB
        List<RoomType> roomTypes = roomTypeRepository.findAll().stream()
                .filter(rt -> Boolean.TRUE.equals(rt.getActive()))
                .collect(Collectors.toList());

        sb.append("DANH SÁCH CÁC HẠNG PHÒNG HIỆN CÓ CỦA KHÁCH SẠN:\n");
        for (RoomType rt : roomTypes) {
            String amenitiesStr = rt.getAmenities() != null
                    ? rt.getAmenities().stream().map(a -> a.getName()).collect(Collectors.joining(", "))
                    : "Đầy đủ tiện nghi";
            sb.append(String.format("- [Mã ID: %d] %s: Giá %s VNĐ/đêm | Sức chứa tối đa: %d người lớn | Tiện ích: %s | Đường dẫn chi tiết: /rooms/%d\n",
                    rt.getId(),
                    rt.getName(),
                    CURRENCY_FORMAT.format(rt.getPricePerNight()),
                    rt.getMaxGuests() != null ? rt.getMaxGuests() : 2,
                    amenitiesStr,
                    rt.getId()));
        }
        sb.append("\n");

        // Lấy danh sách khuyến mãi đang chạy
        List<Promotion> promotions = promotionRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .collect(Collectors.toList());
        if (!promotions.isEmpty()) {
            sb.append("CÁC CHƯƠNG TRÌNH KHUYẾN MÃI / MÃ GIẢM GIÁ ĐANG ÁP DỤNG:\n");
            for (Promotion promo : promotions) {
                sb.append(String.format("- Mã voucher '%s': %s (Mô tả: %s)\n",
                        promo.getCode(),
                        promo.getName(),
                        promo.getDescription() != null ? promo.getDescription() : ""));
            }
            sb.append("\n");
        }

        // Lấy dịch vụ bổ sung
        List<HotelService> services = hotelServiceRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .collect(Collectors.toList());
        if (!services.isEmpty()) {
            sb.append("CÁC DỊCH VỤ TIỆN ÍCH ĐI KÈM:\n");
            for (HotelService svc : services) {
                sb.append(String.format("- %s: %s VNĐ (%s)\n",
                        svc.getName(),
                        svc.getPrice() != null ? CURRENCY_FORMAT.format(svc.getPrice()) : "Liên hệ",
                        svc.getDescription() != null ? svc.getDescription() : ""));
            }
            sb.append("\n");
        }

        // Kiểm tra xem khách có hỏi về mã đơn đặt phòng cụ thể không
        Matcher matcher = BOOKING_CODE_PATTERN.matcher(userMessage);
        if (matcher.find()) {
            String bookingCode = matcher.group(1).toUpperCase();
            Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode);
            sb.append("KẾT QUẢ TRA CỨU MÃ ĐƠN '").append(bookingCode).append("' TỪ DATABASE:\n");
            if (bookingOpt.isPresent()) {
                Booking b = bookingOpt.get();
                String statusVn = mapBookingStatus(b.getStatus());
                sb.append(String.format("- Đơn đặt: %s | Trạng thái: %s | Khách: %s | Nhận phòng: %s | Trả phòng: %s | Tổng tiền: %s VNĐ\n",
                        b.getBookingCode(),
                        statusVn,
                        b.getCustomer() != null ? b.getCustomer().getFullName() : "N/A",
                        b.getCheckIn() != null ? b.getCheckIn().format(DATE_TIME_FORMATTER) : "N/A",
                        b.getCheckOut() != null ? b.getCheckOut().format(DATE_TIME_FORMATTER) : "N/A",
                        b.getTotalAmount() != null ? CURRENCY_FORMAT.format(b.getTotalAmount()) : "0"));
            } else {
                sb.append("- Không tìm thấy thông tin đơn đặt nào khớp với mã '").append(bookingCode).append("'. Vui lòng nhắc khách kiểm tra lại mã đơn.\n");
            }
            sb.append("\n");
        }

        sb.append("HƯỚNG DẪN TRẢ LỜI CHO BOT:\n");
        sb.append("1. Luôn giữ phong thái lịch thiệp, niềm nở, mến khách (xưng hô 'Em' và 'Quý khách' hoặc 'Anh/Chị').\n");
        sb.append("2. Trả lời bằng tiếng Việt gãy gọn, xuống dòng rõ ràng, dùng bullet points (-) và emoji phù hợp.\n");
        sb.append("3. Khi giới thiệu phòng, hãy đính kèm đường dẫn xem phòng dạng markdown [Tên phòng](/rooms/id) để khách hàng có thể bấm vào xem ngay.\n");
        sb.append("4. Chỉ trả lời dựa trên dữ liệu thực tế đã cung cấp ở trên. Tuyệt đối không bịa đặt hạng phòng hoặc giá phòng sai sự thật.\n");
        sb.append("5. ĐỊNH DẠNG BẮT BUỘC: KHÔNG DÙNG bảng Markdown (bảng |---|) và KHÔNG dùng thẻ <br>. Khung chat rất hẹp nên bảng biểu sẽ bị vỡ. Luôn trình bày từng phòng thành một khối riêng có gạch đầu dòng rõ ràng, in đậm tên phòng và giá tiền để khách dễ đọc trên điện thoại.\n");

        return sb.toString();
    }

    /**
     * Gửi request HTTP tới Groq API
     */
    private String callGroqApi(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.6);
        requestBody.put("max_tokens", 800);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) HotelManagement/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Groq API trả về mã lỗi: {} - Nội dung: {}", response.statusCode(), response.body());
            // Thử fallback sang mô hình gpt-oss-20b nếu mô hình chính bị lỗi
            if (!"openai/gpt-oss-20b".equals(model)) {
                log.info("Thử gọi lại với mô hình dự phòng openai/gpt-oss-20b...");
                requestBody.put("model", "openai/gpt-oss-20b");
                String fallbackPayload = objectMapper.writeValueAsString(requestBody);
                HttpRequest fallbackReq = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0")
                        .POST(HttpRequest.BodyPublishers.ofString(fallbackPayload))
                        .build();
                HttpResponse<String> fbResponse = client.send(fallbackReq, HttpResponse.BodyHandlers.ofString());
                if (fbResponse.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(fbResponse.body());
                    return root.path("choices").get(0).path("message").path("content").asText();
                }
            }
            throw new RuntimeException("Lỗi kết nối Groq API: HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    /**
     * Trích xuất các phòng được gợi ý để trả về cho UI hiển thị thẻ card
     */
    private List<ChatResponseDTO.RoomSuggestionDTO> extractSuggestedRooms(String userMsg, String reply) {
        List<RoomType> allRooms = roomTypeRepository.findAll();
        List<ChatResponseDTO.RoomSuggestionDTO> suggestions = new ArrayList<>();

        for (RoomType rt : allRooms) {
            if (!Boolean.TRUE.equals(rt.getActive())) continue;

            // Kiểm tra tên phòng hoặc link /rooms/id có xuất hiện trong câu trả lời hoặc câu hỏi
            boolean mentionedInReply = reply != null && (
                    reply.contains("/rooms/" + rt.getId()) ||
                    reply.toLowerCase().contains(rt.getName().toLowerCase())
            );

            if (mentionedInReply) {
                String imgUrl = null;
                if (rt.getImages() != null && !rt.getImages().isEmpty()) {
                    imgUrl = rt.getImages().get(0).getImageUrl();
                }

                suggestions.add(ChatResponseDTO.RoomSuggestionDTO.builder()
                        .id(rt.getId())
                        .name(rt.getName())
                        .priceFormatted(CURRENCY_FORMAT.format(rt.getPricePerNight()) + " VNĐ/đêm")
                        .maxGuests(rt.getMaxGuests())
                        .detailUrl("/rooms/" + rt.getId())
                        .imageUrl(imgUrl)
                        .build());

                if (suggestions.size() >= 3) break; // Tối đa 3 thẻ card để tránh tràn UI
            }
        }

        return suggestions;
    }

    private String mapBookingStatus(Booking.BookingStatus status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case PENDING -> "Chờ xác nhận / Chờ thanh toán cọc";
            case CONFIRMED -> "Đã xác nhận đặt phòng thành công";
            case CHECKED_IN -> "Đang nhận phòng lưu trú (Checked-in)";
            case CHECKED_OUT -> "Đã hoàn tất trả phòng (Checked-out)";
            case CANCELLED -> "Đã hủy đơn";
            case NO_SHOW -> "Khách không đến nhận phòng (No Show)";
        };
    }
}
