package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatbotService {

    private static final String BASE_SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement.
            Your name is SMYM — please introduce yourself as such if asked.
            """;

    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;
    @Value("${chat.client.advisors.files.similarity-threshold}")
    private double filesSimilarityThreshold;
    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarity-threshold}")
    private double chunksSimilarityThreshold;

    private final ChatClient chatClient;
    private final JwtConverter jwtConverter;
    private final VectorStore filesVectorStore;
    private final VectorStore chunksVectorStore;

    public ChatbotService(
            ChatClient chatClient,
            JwtConverter jwtConverter,
            @Qualifier("filesVectorStore") VectorStore filesVectorStore,
            @Qualifier("chunksVectorStore") VectorStore chunksVectorStore
    ) {
        this.chatClient = chatClient;
        this.jwtConverter = jwtConverter;
        this.filesVectorStore = filesVectorStore;
        this.chunksVectorStore = chunksVectorStore;
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.getSessionId());
        long startTime = System.nanoTime();

        RetrievalAugmentationAdvisor chunksRetrievalAugmentationAdvisor = getChunksRetrievalAugmentationAdvisor();
        RetrievalAugmentationAdvisor filesRetrievalAugmentationAdvisor = getFilesRetrievalAugmentationAdvisor();

        try {
            long startTimeChatClient = System.nanoTime();

            String aiResponse = chatClient
                    .prompt()
                    .advisors(chunksRetrievalAugmentationAdvisor,  filesRetrievalAugmentationAdvisor)
                    .system(BASE_SYSTEM_PROMPT)
                    .user(conversationDto.getUserMessage())
                    .call()
                    .content();

            log.debug("Chat client response time: {}ms", (System.nanoTime() - startTimeChatClient) / 1_000_000);

            return ChatbotResponseDto.builder()
                    .sessionId(conversationDto.getSessionId())
                    .aiResponse(aiResponse)
                    .build();

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Error processing chat request in {}ms: {}", durationMs, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing chat request", e);
        }
    }

    private RetrievalAugmentationAdvisor getChunksRetrievalAugmentationAdvisor() {
        VectorStoreDocumentRetriever chunksDocumentRetriever = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(chunksSimilarityThreshold)
                .vectorStore(chunksVectorStore)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(new LoggingDocumentRetriever(chunksDocumentRetriever, "CHUNKS"))
                .queryAugmenter(new NoOpQueryAugmenter())
                .build();
    }

    private RetrievalAugmentationAdvisor getFilesRetrievalAugmentationAdvisor() {
        String currentUserAccountId = jwtConverter.getCurrentUserAccountId();
        log.debug("Current user account id: {}", currentUserAccountId);

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        Filter.Expression filterExpression = filterExpressionBuilder.eq("accountId", currentUserAccountId).build();

        VectorStoreDocumentRetriever filesDocumentRetriever = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(filesSimilarityThreshold)
                .vectorStore(filesVectorStore)
                .filterExpression(filterExpression)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(new LoggingDocumentRetriever(filesDocumentRetriever, "FILES"))
                .queryAugmenter(new NoOpQueryAugmenter())
                .build();
    }

    /**
     * No-op query augmenter that returns the original query unchanged
     */
    private static class NoOpQueryAugmenter implements QueryAugmenter {
        @Override
        public Query augment(Query query, List<Document> documents) {
            return query;
        }
    }

    /**
     * Wrapper class to log retrieved documents
     */
    private static class LoggingDocumentRetriever implements DocumentRetriever {
        private final DocumentRetriever delegate;
        private final String sourceName;

        public LoggingDocumentRetriever(DocumentRetriever delegate, String sourceName) {
            this.delegate = delegate;
            this.sourceName = sourceName;
        }

        @NotNull
        @Override
        public List<Document> retrieve(Query query) {
            long startTime = System.nanoTime();
            List<Document> documents = delegate.retrieve(query);
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            log.info("=== {} RAG RETRIEVAL ===", sourceName);
            log.info("Query: '{}'", query.text());
            log.info("Retrieved {} documents in {}ms", documents.size(), durationMs);

            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                log.info("Document {} [Score: {}]:", i + 1, doc.getMetadata().get("distance"));
                log.info("  Content: {}", truncateContent(doc.getText()));
                log.info("  Metadata: {}", doc.getMetadata());
            }

            if (documents.isEmpty()) {
                log.info("No documents retrieved from {}", sourceName);
            }

            log.info("=== END {} RAG RETRIEVAL ===", sourceName);

            return documents;
        }

        private String truncateContent(String content) {
            if (content == null) return "null";
            if (content.length() <= 200) return content;
            return content.substring(0, 200) + "... [truncated]";
        }
    }
}
