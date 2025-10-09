package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
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
import java.util.List;
import java.util.stream.Stream;

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
    @Value("${chat.client.advisors.files.topK}")
    private int filesTopK;
    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarity-threshold}")
    private double chunksSimilarityThreshold;
    @Value("${chat.client.advisors.chunks.topK}")
    private int chunksTopK;

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

        try {
            long startTimeChatClient = System.nanoTime();

            ChatClientResponse response = chatClient.prompt()
                    .system(BASE_SYSTEM_PROMPT)
                    .advisors(combinedRags())
                    .user(conversationDto.getUserMessage())
                    .call()
                    .chatClientResponse();

            log.info("Chat context: {}", response.context());

            String answer = response.chatResponse().getResult().getOutput().getText();

            log.debug("Chat client response time: {}ms", (System.nanoTime() - startTimeChatClient) / 1_000_000);

            return ChatbotResponseDto.builder()
                    .sessionId(conversationDto.getSessionId())
                    .aiResponse(answer)
                    .build();

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Error processing chat request in {}ms: {}", durationMs, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing chat request", e);
        }
    }

    private RetrievalAugmentationAdvisor combinedRags() {
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        VectorStoreDocumentRetriever chunksRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(chunksVectorStore)
                .topK(chunksTopK)
                .similarityThreshold(chunksSimilarityThreshold)
                .build();

        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("accountId", jwtConverter.getCurrentUserAccountId())
                .build();

        DocumentRetriever filesRetriever = (searchRequest) -> filesVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("e")
                        .filterExpression(filterExpression)
                        .topK(filesTopK)
                        .similarityThreshold(0.0)
                        .build()
        );

        DocumentRetriever combinedRetriever = query -> {
            List<Document> retrievedChunks = chunksRetriever.retrieve(query);
            List<Document> retrievedFiles = filesRetriever.retrieve(query);
            return Stream.concat(retrievedChunks.stream(), retrievedFiles.stream()).toList();
        };

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(combinedRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    private RetrievalAugmentationAdvisor chunksRag() {
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(chunksVectorStore)
                .topK(chunksTopK)
                .similarityThreshold(chunksSimilarityThreshold)
                .build();

        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    private RetrievalAugmentationAdvisor filesRag() {
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("accountId", jwtConverter.getCurrentUserAccountId())
                .build();

        DocumentRetriever fileRetriever = (searchRequest) -> filesVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("e")
                        .filterExpression(filterExpression)
                        .topK(filesTopK)
                        .similarityThreshold(0.0)
                        .build()
        );

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(fileRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    private List<Advisor> getAdvisors() {
        List<Advisor> advisors = new ArrayList<>();
        if (chunksEnabled) {
            advisors.add(chunksRag());
        }
        if (filesEnabled) {
            advisors.add(filesRag());
        }

        for (int i = 0; i < advisors.size(); i++) {
            log.debug("Advisor {}: '{}'", i, advisors.get(i).getClass().getSimpleName());
        }

        return advisors;
    }
}
