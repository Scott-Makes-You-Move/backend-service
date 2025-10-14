package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatbotService {

    private static final String BASE_SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement.
            Your name is SMYM — please introduce yourself as such if asked.
            Use history only if relevant, but if history doesn’t relate to the question, just ignore it and answer directly.”
            """;

    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;
    @Value("${chat.client.advisors.files.similarity-threshold}")
    private double filesSimilarityThreshold;
    @Value("${chat.client.advisors.files.topK}")
    private int filesTopK;

    private final ChatClient chatClient;
    private final JwtConverter jwtConverter;
    private final FileService fileService;

    public ChatbotService(
            ChatClient chatClient,
            JwtConverter jwtConverter,
            FileService fileService
    ) {
        this.chatClient = chatClient;
        this.jwtConverter = jwtConverter;
        this.fileService = fileService;
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.sessionId());
        long startTime = System.nanoTime();

        try {
            long startTimeChatClient = System.nanoTime();

            String finalBaseSystemPrompt = getFinalBasePrompt();

            String answer = chatClient.prompt()
                    .system(finalBaseSystemPrompt)
                    .user(conversationDto.userMessage())
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

    private String getFinalBasePrompt() {
        if (!filesEnabled) {
            return BASE_SYSTEM_PROMPT;
        }

        var filter = new FilterExpressionBuilder()
                .eq("accountId", jwtConverter.getCurrentUserId())
                .build();

        List<Document> documents = fileService.search(filesTopK, filesSimilarityThreshold, filter);

        if (documents.isEmpty()) {
            return BASE_SYSTEM_PROMPT;
        }

        String joinedNotes = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        String finalBaseSystemPrompt = BASE_SYSTEM_PROMPT + System.lineSeparator() +
                "These are the user's history notes. Use them if relevant. If not, ignore them." +
                System.lineSeparator() + joinedNotes;

        log.debug("Final base system prompt: {}", finalBaseSystemPrompt);
        return finalBaseSystemPrompt;
    }
}
