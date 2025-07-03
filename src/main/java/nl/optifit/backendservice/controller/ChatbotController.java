package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.zapier.InitiateChatbotConversationDto;
import nl.optifit.backendservice.dto.zapier.ChatbotResponseDto;
import nl.optifit.backendservice.dto.zapier.ZapierWorkflowResponseDto;
import nl.optifit.backendservice.service.ZapierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final ZapierService zapierService;

    @PostMapping("/initiate")
    public ResponseEntity<ZapierWorkflowResponseDto> initiateChatbotConversation(@RequestBody InitiateChatbotConversationDto initiateChatbotConversationDto,
                                                                                 HttpServletRequest request) {
        return zapierService.initiateChatbotConversation(initiateChatbotConversationDto, request);
    }

    @PostMapping("/response")
    public void receiveResponse(@RequestBody ChatbotResponseDto chatbotResponseDto) {
        zapierService.storeResponse(chatbotResponseDto);
    }

    @GetMapping("/response/{sessionId}")
    public ResponseEntity<ChatbotResponseDto> receiveSessionResponse(@PathVariable String sessionId) {
        ChatbotResponseDto response = zapierService.getResponseForSession(sessionId);
        return ResponseEntity.ok(response);
    }
}
