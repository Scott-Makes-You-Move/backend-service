package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.zapier.InitiateChatbotConversationDto;
import nl.optifit.backendservice.dto.zapier.ReceiveChatbotResponseDto;
import nl.optifit.backendservice.dto.zapier.ZapierWorkflowResponseDto;
import nl.optifit.backendservice.service.ZapierService;
import org.apache.commons.lang3.StringUtils;
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

    private final ZapierService zapierService;

    @PostMapping("/initiate")
    public ResponseEntity<ZapierWorkflowResponseDto> initiateChatbotConversation(@RequestBody InitiateChatbotConversationDto initiateChatbotConversationDto) {
        return zapierService.initiateChatbotConversation(initiateChatbotConversationDto);
    }

    @PostMapping("/response")
    public ResponseEntity<String> receiveResponse(@RequestBody ReceiveChatbotResponseDto receiveChatbotResponseDto) {
        String aiResponse = receiveChatbotResponseDto.getAiResponse();
        return StringUtils.isNotBlank(aiResponse) ? ResponseEntity.ok(aiResponse) : ResponseEntity.badRequest().build();
    }
}
