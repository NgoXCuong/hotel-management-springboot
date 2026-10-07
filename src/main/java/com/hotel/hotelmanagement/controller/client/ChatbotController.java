package com.hotel.hotelmanagement.controller.client;

import com.hotel.hotelmanagement.dto.chatbot.ChatRequestDTO;
import com.hotel.hotelmanagement.dto.chatbot.ChatResponseDTO;
import com.hotel.hotelmanagement.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponseDTO> sendMessage(
            @RequestBody ChatRequestDTO request,
            Principal principal) {
        ChatResponseDTO response = chatbotService.chat(request, principal);
        return ResponseEntity.ok(response);
    }
}
