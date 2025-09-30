package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.ai.chat.client.ChatClient;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarity-threshold}")
    private double chunksSimilarityThreshold;

    private final ChatClient chatClient;
    private final JwtConverter jwtConverter;
    private final VectorStore filesVectorStore;
    private final VectorStore chunksVectorStore;
    private final ChunkService chunkService;
    private final FileService fileService;

    public ChatbotService(
            ChatClient chatClient,
            JwtConverter jwtConverter,
            @Qualifier("filesVectorStore") VectorStore filesVectorStore,
            @Qualifier("chunksVectorStore") VectorStore chunksVectorStore, ChunkService chunkService, FileService fileService
    ) {
        this.chatClient = chatClient;
        this.jwtConverter = jwtConverter;
        this.filesVectorStore = filesVectorStore;
        this.chunksVectorStore = chunksVectorStore;
        this.chunkService = chunkService;
        this.fileService = fileService;
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.getSessionId());
        long startTime = System.nanoTime();

        try {
            long startTimeChatClient = System.nanoTime();

            ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                    .allowEmptyContext(true)
                    .build();

            SearchRequest chunksSearchRequest = SearchRequest.builder()
                    .query(conversationDto.getUserMessage())
                    .topK(3)
                    .similarityThreshold(chunksSimilarityThreshold)
                    .build();
            SearchRequest filesSearchRequest = SearchRequest.builder()
                    .query("e")
                    .topK(1000)
                    .similarityThreshold(filesSimilarityThreshold)
                    .filterExpression(new FilterExpressionBuilder()
                            .eq("accountId", jwtConverter.getCurrentUserAccountId())
                            .build())
                    .build();

            List<Document> chunksDocs = chunkService.search(chunksSearchRequest);
            List<Document> filesDocs = fileService.search(filesSearchRequest);

            log.debug("Chunk docs found {}", chunksDocs.size());
            log.debug("File docs found {}", filesDocs.size());

            String combinedContext = Stream.concat(chunksDocs.stream(), filesDocs.stream())
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

            Advisor combinedAdvisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(retriever -> Collections.singletonList(Document.builder().text(combinedContext).build()))
                    .queryAugmenter(queryAugmenter)
                    .build();

            String answer = chatClient.prompt()
                    .system(BASE_SYSTEM_PROMPT)
                    .advisors(combinedAdvisor)
                    .user(conversationDto.getUserMessage())
                    .call()
                    .content();

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

    private RetrievalAugmentationAdvisor chunksRAG() {
        return RetrievalAugmentationAdvisor.builder().documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(chunksVectorStore)
                        .topK(3)
                        .similarityThreshold(chunksSimilarityThreshold)
                        .build())
                .build();
    }

    private RetrievalAugmentationAdvisor filesRAG() {
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("accountId", jwtConverter.getCurrentUserAccountId())
                .build();

        DocumentRetriever fileRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(filesVectorStore)
                .topK(1000)
                .filterExpression(filterExpression)
                .build();

        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(fileRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }
}
