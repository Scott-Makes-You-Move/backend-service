package nl.optifit.backendservice.service;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChatbotResponseDto;
import nl.optifit.backendservice.dto.ConversationDto;
import nl.optifit.backendservice.security.JwtConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
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
    private final FileService fileService;

    public ChatbotService(
            ChatClient chatClient,
            JwtConverter jwtConverter,
            @Qualifier("filesVectorStore") VectorStore filesVectorStore,
            @Qualifier("chunksVectorStore") VectorStore chunksVectorStore,
            FileService fileService
    ) {
        this.chatClient = chatClient;
        this.jwtConverter = jwtConverter;
        this.filesVectorStore = filesVectorStore;
        this.chunksVectorStore = chunksVectorStore;
        this.fileService = fileService;
    }

    public ChatbotResponseDto initiateChat(ConversationDto conversationDto) {
        log.debug("Initiating chat with session id '{}'", conversationDto.getSessionId());
        long startTime = System.nanoTime();

        try {
            long startTimeChatClient = System.nanoTime();

            Filter.Expression filterExpression = new FilterExpressionBuilder()
                    .eq("accountId", jwtConverter.getCurrentUserAccountId())
                    .build();

            SearchRequest searchRequest = SearchRequest.builder()
                    .query("e")
                    .filterExpression(filterExpression)
                    .topK(filesTopK)
                    .similarityThreshold(filesSimilarityThreshold)
                    .build();

            List<Document> search = fileService.search(searchRequest);
            String finalBaseSystemPrompt = BASE_SYSTEM_PROMPT;

            if (!search.isEmpty()) {
                finalBaseSystemPrompt = BASE_SYSTEM_PROMPT + "These are the user's history notes. Use them if relevant. If not, ignore them.";
                finalBaseSystemPrompt += "\n\n" + search.stream();
            }

            log.info("Final base system prompt: '{}'", finalBaseSystemPrompt);

            ChatClientResponse response = chatClient.prompt()
                    .system(finalBaseSystemPrompt)
                    .advisors(getAdvisors())
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

    private RetrievalAugmentationAdvisor getAdvisors() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate(
                "The user asked: {query}. No relevant documents were found, but try to answer anyway."
        );

        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();

        DocumentRetriever combinedRetriever = getCombinedRetriever();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(combinedRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    @NotNull
    private DocumentRetriever getCombinedRetriever() {
        if (!chunksEnabled && !filesEnabled) {
            return query -> Collections.emptyList();
        }

        return query -> {
            List<Document> documents = new ArrayList<>();

            if (chunksEnabled) {
                documents.addAll(getChunksRetriever().retrieve(query));
            }
            if (filesEnabled) {
                documents.addAll(getFilesRetriever().retrieve(query));
            }

            return documents;
        };
    }

    @NotNull
    private DocumentRetriever getChunksRetriever() {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(chunksVectorStore)
                .topK(chunksTopK)
                .similarityThreshold(chunksSimilarityThreshold)
                .build();
    }

    @NotNull
    private DocumentRetriever getFilesRetriever() {
        Filter.Expression filterExpression = new FilterExpressionBuilder()
                .eq("accountId", jwtConverter.getCurrentUserAccountId())
                .build();

        return query -> filesVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("e") // this should find all documents
                        .filterExpression(filterExpression)
                        .topK(filesTopK)
                        .similarityThreshold(filesSimilarityThreshold)
                        .build()
        );
    }
}
