package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/chatbot")
@Tag(name = "Account", description = "Operations related to chatbot")
@RestController
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/initiate")
    public ResponseEntity<ChatbotResponseDto> initiateChatbotConversation(@RequestBody ConversationDto conversationDto) {
        ChatbotResponseDto chatbotResponseDto = chatbotService.initiateChat(conversationDto);
        return ResponseEntity.ok(chatbotResponseDto);
    }
}
