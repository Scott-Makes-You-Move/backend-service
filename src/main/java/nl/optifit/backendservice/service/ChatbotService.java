package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatbotService {

    private static final String BASE_SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement.
            Your name is SMYM — introduce yourself as such if asked.
            
            Use retrieved context when it is relevant.
            When information is missing, use your own knowledge to provide the best helpful response.
            Never say "I don't know" if you can reasonably infer or explain the answer.
            If retrieval context conflicts with general knowledge, prefer the retrieval context.
            """;

    private final ChatClient chatClient;
    private final JwtConverter jwtConverter;
    private final ChatRetrievalOrchestrator orchestrator;

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.sessionId());
        long startTime = System.nanoTime();

        List<Document> documents = orchestrator.buildContext(jwtConverter.getCurrentUserId(), conversationDto.userMessage());
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(BASE_SYSTEM_PROMPT),
                new UserMessage(documentsToText(documents)),
                new UserMessage(conversationDto.userMessage())
        ));

        try {
            long startTimeChatClient = System.nanoTime();

            String answer = chatClient.prompt(prompt)
                    .call()
                    .content();


            log.debug("Chat client response time: {}ms", (System.nanoTime() - startTimeChatClient) / 1_000_000);

            return new ChatbotResponseDto(conversationDto.sessionId(), answer);

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Error processing chat request in {}ms: {}", durationMs, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing chat request", e);
        }
    }

    private String documentsToText(List<Document> docs) {
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
