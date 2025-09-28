package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
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
            Your name is SMYM — please introduce yourself as such if asked.
            """;

    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;
    @Value("${chat.client.advisors.files.similarity-threshold}")
    private double filesSimilarityThreshold;

    private final ChatClient chatClient;
    private final JwtConverter jwtConverter;
    private final VectorStore filesVectorStore;

    public ChatbotService(ChatClient chatClient, JwtConverter jwtConverter, @Qualifier("filesVectorStore") VectorStore filesVectorStore) {
        this.chatClient = chatClient;
        this.jwtConverter = jwtConverter;
        this.filesVectorStore = filesVectorStore;
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.getSessionId());
        long startTime = System.nanoTime();

        try {
            long startTimeChatClient = System.nanoTime();
            String aiResponse = chatClient
                    .prompt()
                    .advisors(a -> {
                        if (filesEnabled) {
                            a.advisors(getFilesRetrievalAugmentationAdvisor());
                        }
                        a.param(ChatMemory.CONVERSATION_ID, conversationDto.getSessionId().toString());
                    })
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

    private RetrievalAugmentationAdvisor getFilesRetrievalAugmentationAdvisor() {
        String currentUserAccountId = jwtConverter.getCurrentUserAccountId();
        log.debug("Current user account id: {}", currentUserAccountId);

        FilterExpressionBuilder feb = new FilterExpressionBuilder();
        Filter.Expression filterExpression = feb.eq("accountId", currentUserAccountId).build();

        VectorStoreDocumentRetriever delegate = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(filesSimilarityThreshold)
                .vectorStore(filesVectorStore)
                .filterExpression(filterExpression)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(query -> {
                    List<Document> docs = delegate.retrieve(query);

                    log.debug("RAG retrieved {} docs for query '{}':", docs.size(), query.text());
                    docs.forEach(d -> log.debug("Doc: {}", d.getText()));

                    return docs;
                })
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
