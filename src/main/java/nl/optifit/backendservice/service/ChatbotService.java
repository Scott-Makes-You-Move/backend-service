package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.SearchQueryDto;
import nl.optifit.backendservice.dto.SummarizedConversation;
import nl.optifit.backendservice.dto.zapier.ChatbotResponseDto;
import nl.optifit.backendservice.dto.zapier.ConversationDto;
import nl.optifit.backendservice.model.Conversation;
import nl.optifit.backendservice.repository.ConversationRepository;
import nl.optifit.backendservice.util.MaskingUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static nl.optifit.backendservice.model.Role.ASSISTANT;
import static nl.optifit.backendservice.model.Role.USER;

@Slf4j
@Service
public class ChatbotService {
    private static final String SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement. 
            Your name is SMYM — please introduce yourself as such if asked.""";

    private final ConversationRepository conversationRepository;
    private final ChunkService chunkService;
    private final ChatClient chatClient;

    public ChatbotService(ConversationRepository conversationRepository, ChunkService chunkService, ChatClient.Builder chatClientBuilder) {
        this.conversationRepository = conversationRepository;
        this.chunkService = chunkService;
        this.chatClient = chatClientBuilder.build();
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        try {
            UUID chatSessionId = conversationDto.getSessionId();
            saveUserMessage(conversationDto, chatSessionId);

            List<Conversation> recentConversations = getRecentConversations(chatSessionId);
            SummarizedConversation summary = summarizeConversation(recentConversations);

            String maskedMessage = MaskingUtil.maskEntities(conversationDto.getUserMessage());
            String ragSearch = getRelevantKnowledge(maskedMessage);

            String prompt = buildPrompt(summary.summary(), maskedMessage, ragSearch);
            String aiResponse = getAiResponse(prompt);

            return createResponse(chatSessionId, aiResponse);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing chat request", e);
        }
    }

    private List<Conversation> getRecentConversations(UUID chatSessionId) {
        return conversationRepository.findTop10ByChatSessionIdOrderByCreatedAtDesc(chatSessionId);
    }

    private SummarizedConversation summarizeConversation(List<Conversation> conversations) {
        String chatContextRaw = conversations.stream()
                .map(Conversation::getMessage)
                .collect(Collectors.joining("\n -", "- ", ""));

        String summary = conversations.stream()
                .limit(3)
                .map(Conversation::getMessage)
                .collect(Collectors.joining(", "));

        return new SummarizedConversation(chatContextRaw, summary);
    }

    private String getRelevantKnowledge(String query) {
        return chunkService.search(SearchQueryDto.builder()
                        .query(query)
                        .topK(1)
                        .similarityThreshold(0.1)
                        .build())
                .stream()
                .findFirst()
                .map(Document::getText)
                .orElse("");
    }

    private String buildPrompt(String summary, String maskedMessage, String ragSearch) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);

        if (summary != null && !summary.isBlank()) {
            prompt.append("\n\nHere's the previous conversation:\n")
                    .append(summary.trim());
        }

        if (ragSearch != null && !ragSearch.isBlank()) {
            prompt.append("\n\nYou may find the following internal knowledge helpful:\n")
                    .append(ragSearch.trim());
        }

        prompt.append("\n\nUser now asked:\n")
                .append(maskedMessage != null ? maskedMessage.trim() : "")
                .append("\n\nRespond clearly, professionally, and concisely.");

        return prompt.toString();
    }

    private String getAiResponse(String prompt) {
        String maskedResponse = chatClient.prompt(prompt).call().content();
        return MaskingUtil.unmaskEntities(maskedResponse);
    }

    private ChatbotResponseDto createResponse(UUID chatSessionId, String aiResponse) {
        Conversation savedMessage = saveAssistantMessage(chatSessionId, aiResponse);
        return ChatbotResponseDto.builder()
                .sessionId(chatSessionId)
                .aiResponse(savedMessage.getMessage())
                .build();
    }

    private Conversation saveUserMessage(ConversationDto dto, UUID chatSessionId) {
        Conversation conversation = Conversation.builder()
                .chatSessionId(chatSessionId)
                .role(USER)
                .message(dto.getUserMessage())
                .createdAt(ZonedDateTime.now())
                .build();

        return conversationRepository.save(conversation);
    }

    private Conversation saveAssistantMessage(UUID chatSessionId, String message) {
        Conversation conversation = Conversation.builder()
                .chatSessionId(chatSessionId)
                .role(ASSISTANT)
                .message(message)
                .createdAt(ZonedDateTime.now())
                .build();

        return conversationRepository.save(conversation);
    }
}
