package com.hotel.hotelmanagement.service;

import com.hotel.hotelmanagement.dto.chatbot.ChatRequestDTO;
import com.hotel.hotelmanagement.dto.chatbot.ChatResponseDTO;

import java.security.Principal;

public interface ChatbotService {
    ChatResponseDTO chat(ChatRequestDTO request, Principal principal);
}
