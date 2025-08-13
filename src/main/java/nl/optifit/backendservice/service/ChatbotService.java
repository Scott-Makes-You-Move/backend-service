package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.dto.SearchQueryDto;
import nl.optifit.backendservice.util.MaskingUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
@Service
public class ChatbotService {

    @Value("${search.rag.enabled:true}")
    private boolean ragSearchEnabled;

    private static final String BASE_SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement.
            Your name is SMYM — please introduce yourself as such if asked.
            """;

    private static final Executor taskExecutor = Executors.newWorkStealingPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    private final ChunkService chunkService;
    private final ChatClient chatClient;
    private final Cache ragCache;
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    public ChatbotService(
            ChunkService chunkService,
            CacheManager cacheManager,
            ChatClient chatClient) {
        this.chunkService = chunkService;
        this.chatClient = chatClient;
        this.ragCache = cacheManager.getCache("ragResults");
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        long startTime = System.nanoTime();

        try {
            UUID chatSessionId = conversationDto.getSessionId();
            String userMessage = conversationDto.getUserMessage();

            CompletableFuture<String> maskedFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return MaskingUtil.maskEntities(userMessage);
                } catch (IOException e) {
                    throw new RuntimeException("Masking failed", e);
                }
            }, taskExecutor);

            CompletableFuture<String> ragFuture = getCachedRagResultsWithTimeout(maskedFuture, 5, TimeUnit.SECONDS);

            String userContent = maskedFuture.thenCombine(ragFuture, (masked, rag) -> {
                StringBuilder sb = new StringBuilder();
                if (rag != null && !rag.isBlank()) {
                    sb.append("Relevant internal knowledge:\n").append(rag.trim()).append("\n\n");
                }
                sb.append(masked != null ? masked.trim() : "");
                return sb.toString();
            }).join();

            long chatStartTime = System.nanoTime();
            String aiResponse = chatClient
                    .prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatSessionId.toString()))
                    .system(BASE_SYSTEM_PROMPT)
                    .user(userContent)
                    .call()
                    .content();
            long chatEndTime = System.nanoTime();
            log.debug("Chat model response time: {}ms", (chatEndTime - chatStartTime) / 1_000_000);

            String finalResponse = MaskingUtil.unmaskEntities(aiResponse);

            return ChatbotResponseDto.builder()
                    .sessionId(chatSessionId)
                    .aiResponse(finalResponse)
                    .build();

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Error processing chat request in {}ms: {}", durationMs, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing chat request", e);
        }
    }

    private CompletableFuture<String> getCachedRagResultsWithTimeout(
            CompletableFuture<String> maskingFuture, long timeout, TimeUnit unit) {

        if (!ragSearchEnabled) return CompletableFuture.completedFuture(null);

        return maskingFuture.thenComposeAsync(query -> {
            CompletableFuture<String> ragSearch = CompletableFuture.supplyAsync(() -> {
                try {
                    int queryHash = query.hashCode();
                    String cached = ragCache.get(queryHash, String.class);
                    if (cached != null) return cached;

                    long ragStart = System.nanoTime();
                    List<Document> results = chunkService.search(
                            SearchQueryDto.builder()
                                    .query(query)
                                    .topK(1)
                                    .similarityThreshold(0.1)
                                    .build()
                    );
                    log.debug("RAG search time: {}ms", (System.nanoTime() - ragStart) / 1_000_000);

                    String result = results.stream()
                            .findFirst()
                            .map(Document::getText)
                            .orElse("");

                    if (!result.isEmpty()) ragCache.put(queryHash, result);
                    return result;
                } catch (Exception e) {
                    log.warn("RAG search failed: {}", e.getMessage());
                    return null;
                }
            }, taskExecutor);

            CompletableFuture<String> timeoutFuture = new CompletableFuture<>();
            ScheduledFuture<?> scheduledTimeout =
                    timeoutExecutor.schedule(() -> timeoutFuture.complete(null), timeout, unit);

            ragSearch.whenComplete((r, err) -> scheduledTimeout.cancel(false));
            return ragSearch.applyToEither(timeoutFuture, Function.identity())
                    .exceptionally(ex -> {
                        log.warn("RAG search encountered error: {}", ex.getMessage());
                        return null;
                    });
        }, taskExecutor);
    }
}
