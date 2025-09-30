package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
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
import java.util.Map;

@Slf4j
@Service
public class ChatbotService {

    private static final String BASE_SYSTEM_PROMPT = """
            You are a specialist in mobility exercises, habit creation and mental health improvement.
            Your name is SMYM — please introduce yourself as such if asked.
            
            If no relevant context is provided, rely on your general expertise to give the best possible answer.
            Do not answer "I don't know"; instead, give general suggestions, practical tips, or clarifying questions.
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
                    .advisors(filesRetrievalAugmentationAdvisor, chunksRetrievalAugmentationAdvisor)
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

        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(chunksDocumentRetriever)
                .queryAugmenter(queryAugmenter)
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

        QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(filesDocumentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }
}
