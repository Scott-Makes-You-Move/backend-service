package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
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

    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;
    @Value("${chat.client.advisors.files.similarity-threshold}")
    private double filesSimilarityThreshold;
    @Value("${chat.client.advisors.files.topK}")
    private int filesTopK;

    private final ChatClient chatClient;
    private final JwtConverter jwtConverter;
    private final VectorStore filesVectorStore;

    private final static ContextualQueryAugmenter QUERY_AUGMENTER = ContextualQueryAugmenter.builder()
            .allowEmptyContext(true)
            .build();

    public ChatbotService(ChatClient chatClient, JwtConverter jwtConverter, @Qualifier("filesVectorStore") VectorStore filesVectorStore) {
        this.chatClient = chatClient;
        this.jwtConverter = jwtConverter;
        this.filesVectorStore = filesVectorStore;
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.sessionId());
        long startTime = System.nanoTime();

        try {
            long startTimeChatClient = System.nanoTime();

            var request = chatClient.prompt().system(BASE_SYSTEM_PROMPT);

            if (filesEnabled) {
                request.advisors(List.of(filesRetrievalAugmentationAdvisor()));
            }

            String answer = request.user(conversationDto.userMessage()).call().content();

            log.debug("Chat client response time: {}ms", (System.nanoTime() - startTimeChatClient) / 1_000_000);

            return new ChatbotResponseDto(conversationDto.sessionId(), answer);

        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Error processing chat request in {}ms: {}", durationMs, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing chat request", e);
        }
    }

    private RetrievalAugmentationAdvisor filesRetrievalAugmentationAdvisor() {
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("accountId", jwtConverter.getCurrentUserId())
                .build();

        VectorStoreDocumentRetriever filesRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(filesVectorStore)
                .filterExpression(filterExpression)
                .topK(filesTopK)
                .similarityThreshold(filesSimilarityThreshold)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(filesRetriever)
                .queryAugmenter(QUERY_AUGMENTER)
                .build();
    }
}
