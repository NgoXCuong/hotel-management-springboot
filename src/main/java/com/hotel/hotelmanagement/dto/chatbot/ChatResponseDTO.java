package com.hotel.hotelmanagement.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private boolean success;
    private String reply;
    private List<RoomSuggestionDTO> suggestedRooms;
    private String timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSuggestionDTO {
        private Long id;
        private String name;
        private String priceFormatted;
        private Integer maxGuests;
        private String detailUrl;
        private String imageUrl;
    }
}
