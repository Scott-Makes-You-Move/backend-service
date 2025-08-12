package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.dto.SearchQueryDto;
import nl.optifit.backendservice.dto.SummarizedConversation;
import nl.optifit.backendservice.model.Conversation;
import nl.optifit.backendservice.repository.ConversationRepository;
import nl.optifit.backendservice.util.MaskingUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

import static nl.optifit.backendservice.model.Role.ASSISTANT;
import static nl.optifit.backendservice.model.Role.USER;

@Slf4j
@Service
public class ChatbotService {

    @Value("${search.rag.enabled}")
    private boolean ragSearchEnabled;

    private static final String SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement. 
            Your name is SMYM — please introduce yourself as such if asked.""";

    private static final Executor taskExecutor = Executors.newWorkStealingPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    private final ConversationRepository conversationRepository;
    private final ChunkService chunkService;
    private final ChatClient chatClient;
    private final Cache conversationCache;
    private final Cache ragCache;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    public ChatbotService(
            ConversationRepository conversationRepository,
            ChunkService chunkService,
            ChatClient.Builder chatClientBuilder,
            CacheManager cacheManager) {
        this.conversationRepository = conversationRepository;
        this.chunkService = chunkService;
        this.chatClient = chatClientBuilder.build();
        this.conversationCache = cacheManager.getCache("conversations");
        this.ragCache = cacheManager.getCache("ragResults");
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        long startTime = System.nanoTime();

        try {
            UUID chatSessionId = conversationDto.getSessionId();
            String userMessage = conversationDto.getUserMessage();

            CompletableFuture<String> maskingFuture = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return MaskingUtil.maskEntities(userMessage);
                        } catch (IOException e) {
                            throw new RuntimeException("Masking failed", e);
                        }
                    },
                    taskExecutor
            );

            CompletableFuture<Void> saveFuture = CompletableFuture.runAsync(
                    () -> saveUserMessage(conversationDto, chatSessionId),
                    taskExecutor
            );

            CompletableFuture<List<Conversation>> conversationsFuture = getCachedConversations(chatSessionId);
            CompletableFuture<String> ragFuture = getCachedRagResultsWithTimeout(maskingFuture, 5, TimeUnit.SECONDS);

            return maskingFuture.thenCombine(conversationsFuture, (maskedMsg, conversations) -> {
                SummarizedConversation summary = summarizeConversation(conversations);
                return Pair.of(maskedMsg, summary);
            }).thenCombine(ragFuture, (pair, ragResult) -> {
                String prompt = buildPrompt(
                        pair.getRight().summary(),
                        pair.getLeft(),
                        ragResult != null ? ragResult : "[RAG search timed out or failed]"
                );
                return prompt;
            }).thenApplyAsync(prompt -> {
                String aiResponse = getAiResponse(prompt);
                return saveAndReturnResponse(chatSessionId, aiResponse);
            }, taskExecutor).join();

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Error processing chat request in {}ms: {}", durationMs, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing chat request",
                    e
            );
        }
    }

    private CompletableFuture<String> getCachedRagResultsWithTimeout(
            CompletableFuture<String> maskingFuture,
            long timeout,
            TimeUnit unit) {

        if (!ragSearchEnabled) {
            log.debug("RAG search disabled. Returning null.");
            return CompletableFuture.completedFuture(null);
        }

        return maskingFuture.thenComposeAsync(query -> {
            CompletableFuture<String> ragSearch = CompletableFuture.supplyAsync(() -> {
                try {
                    int queryHash = query.hashCode();
                    String cached = ragCache.get(queryHash, String.class);
                    if (cached != null) {
                        log.debug("Returning cached RAG results for query hash: {}", queryHash);
                        return cached;
                    }

                    long ragStartTime = System.nanoTime();
                    log.debug("Performing fresh RAG search for query: {}", query);
                    List<Document> results = chunkService.search(
                            SearchQueryDto.builder()
                                    .query(query)
                                    .topK(1)
                                    .similarityThreshold(0.1)
                                    .build()
                    );

                    long ragEndTime = System.nanoTime();

                    log.debug("RAG search took {}ms", (ragEndTime - ragStartTime) / 1_000_000);

                    String result = results.stream()
                            .findFirst()
                            .map(Document::getText)
                            .orElse("");

                    if (!result.isEmpty()) {
                        ragCache.put(queryHash, result);
                    }
                    return result;
                } catch (Exception e) {
                    log.warn("RAG search failed for query: {}. Error: {}", query, e.getMessage());
                    return null;
                }
            }, taskExecutor);

            CompletableFuture<String> timeoutFuture = new CompletableFuture<>();
            ScheduledFuture<?> scheduledTimeout = timeoutExecutor.schedule(
                    () -> timeoutFuture.complete(null),
                    timeout,
                    unit
            );

            ragSearch.whenComplete((result, error) -> scheduledTimeout.cancel(false));

            return ragSearch.applyToEither(timeoutFuture, Function.identity())
                    .exceptionally(ex -> {
                        log.warn("RAG search encountered error: {}", ex.getMessage());
                        return null;
                    });
        }, taskExecutor);
    }

    private CompletableFuture<List<Conversation>> getCachedConversations(UUID chatSessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Conversation> cached = conversationCache.get(chatSessionId, List.class);
                if (cached != null) {
                    log.debug("Returning cached conversations for session: {}", chatSessionId);
                    return cached;
                }

                List<Conversation> fresh = conversationRepository.findTop10ByChatSessionIdOrderByCreatedAtDesc(chatSessionId);
                conversationCache.put(chatSessionId, fresh);
                return fresh;
            } catch (Exception e) {
                log.warn("Failed to get conversations for session: {}. Error: {}", chatSessionId, e.getMessage());
                return List.of();
            }
        }, taskExecutor);
    }

    @Cacheable(value = "conversations", key = "#chatSessionId")
    public List<Conversation> getRecentConversations(UUID chatSessionId) {
        return conversationRepository.findTop10ByChatSessionIdOrderByCreatedAtDesc(chatSessionId);
    }

    @Async
    public CompletableFuture<SummarizedConversation> summarizeConversationAsync(List<Conversation> conversations) {
        return CompletableFuture.completedFuture(summarizeConversation(conversations));
    }

    private SummarizedConversation summarizeConversation(List<Conversation> conversations) {
        if (conversations.size() < 3) {
            return new SummarizedConversation("", "");
        }

        StringBuilder chatContext = new StringBuilder(conversations.size() * 50);
        StringBuilder summary = new StringBuilder(conversations.size() * 30);

        int count = 0;
        for (Conversation conv : conversations) {
            if (count < 3) {
                if (count > 0) summary.append(", ");
                summary.append(conv.getMessage());
                count++;
            }
            chatContext.append("\n -").append(conv.getMessage());
        }

        return new SummarizedConversation(
                chatContext.insert(0, "- ").toString(),
                summary.toString()
        );
    }

    private String buildPrompt(String summary, String maskedMessage, String ragSearch) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT.length() +
                (summary != null ? summary.length() : 0) +
                (ragSearch != null ? ragSearch.length() : 0) +
                (maskedMessage != null ? maskedMessage.length() : 0) + 100);

        prompt.append(SYSTEM_PROMPT);

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
        try {
            long chatStartTime = System.nanoTime();

            String chatClientResponse = chatClient.prompt(prompt)
                    .call()
                    .content();

            long chatEndTime = System.nanoTime();

            log.debug("Chat took {}ms", (chatEndTime - chatStartTime) / 1_000_000);

            return MaskingUtil.unmaskEntities(chatClientResponse);
        } catch (Exception e) {
            log.error("Error getting AI response", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error generating AI response",
                    e
            );
        }
    }

    private ChatbotResponseDto saveAndReturnResponse(UUID chatSessionId, String aiResponse) {
        Conversation savedMessage = saveAssistantMessage(chatSessionId, aiResponse);
        return ChatbotResponseDto.builder()
                .sessionId(chatSessionId)
                .aiResponse(savedMessage.getMessage())
                .build();
    }

    private void saveUserMessage(ConversationDto dto, UUID chatSessionId) {
        try {
            Conversation conversation = Conversation.builder()
                    .chatSessionId(chatSessionId)
                    .role(USER)
                    .message(dto.getUserMessage())
                    .createdAt(ZonedDateTime.now())
                    .build();

            conversationRepository.save(conversation);
        } catch (Exception e) {
            log.warn("Failed to save user message for session: {}. Error: {}", chatSessionId, e.getMessage());
        }
    }

    private Conversation saveAssistantMessage(UUID chatSessionId, String message) {
        try {
            Conversation conversation = Conversation.builder()
                    .chatSessionId(chatSessionId)
                    .role(ASSISTANT)
                    .message(message)
                    .createdAt(ZonedDateTime.now())
                    .build();

            return conversationRepository.save(conversation);
        } catch (Exception e) {
            log.warn("Failed to save assistant message for session: {}. Error: {}", chatSessionId, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save assistant response",
                    e
            );
        }
    }
}
