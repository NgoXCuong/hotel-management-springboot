package com.hotel.hotelmanagement.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    private String message;
    @Builder.Default
    private List<ChatMessageDTO> history = new ArrayList<>();
}
