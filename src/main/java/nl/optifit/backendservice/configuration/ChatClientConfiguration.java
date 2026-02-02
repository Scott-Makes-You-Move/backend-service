package nl.optifit.backendservice.configuration;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.advisor.MaskingAdvisor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
public class ChatClientConfiguration {

    @Value("${chat.client.advisors.chat-memory.enabled}")
    private boolean chatMemoryEnabled;
    @Value("${chat.client.advisors.chat-memory.max-messages}")
    private int maxMessages;
    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;
    @Value("${chat.client.advisors.files.similarityThreshold}")
    private double filesSimilarityThreshold;
    @Value("${chat.client.advisors.files.topK}")
    private int filesTopK;
    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarityThreshold}")
    private double chunksSimilarityThreshold;
    @Value("${chat.client.advisors.chunks.topK}")
    private int chunksTopK;
    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;
    @Value("${chat.client.advisors.logging.enabled}")
    private boolean loggingEnabled;

    public static final String ACCOUNT_ID_CONTEXT_KEY = "accountId";

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            @Qualifier("filesVectorStore") VectorStore filesVectorStore,
            @Qualifier("chunksVectorStore") VectorStore chunksVectorStore
    ) {
        List<Advisor> advisors = getAdvisors(filesVectorStore, chunksVectorStore);

        return ChatClient.builder(chatModel)
                .defaultAdvisors(advisors)
                .build();
    }

    private List<Advisor> getAdvisors(VectorStore filesVectorStore, VectorStore chunksVectorStore) {
        List<Advisor> advisors = new ArrayList<>();

        if (chatMemoryEnabled) {
            MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
            MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build();
            advisors.add(messageChatMemoryAdvisor);
        }
        if (filesEnabled || chunksEnabled) {
            RetrievalAugmentationAdvisor multiSourceRagAdvisor = createMultiSourceRagAdvisor(filesVectorStore, chunksVectorStore);
            advisors.add(multiSourceRagAdvisor);
        }
        if (maskingEnabled) {
            advisors.add(MaskingAdvisor.builder().build());
        }
        if (loggingEnabled) {
            advisors.add(SimpleLoggerAdvisor.builder().build());
        }

        return advisors;
    }

    private RetrievalAugmentationAdvisor createMultiSourceRagAdvisor(VectorStore filesVectorStore,
                                                                     VectorStore chunksVectorStore) {

        DocumentRetriever multiSourceRetriever = query -> {

            List<Document> filesDocs = new ArrayList<>();
            List<Document> chunksDocs = new ArrayList<>();

            if (filesEnabled) {
                long startTime = System.nanoTime();
                String accountId = query.context().get(ACCOUNT_ID_CONTEXT_KEY).toString();
                if (StringUtils.isBlank(accountId)) {
                    throw new IllegalArgumentException("Missing required context key: '%s'".formatted(ACCOUNT_ID_CONTEXT_KEY));
                }

                DocumentRetriever filesRetriever = VectorStoreDocumentRetriever.builder()
                        .vectorStore(filesVectorStore)
                        .similarityThreshold(filesSimilarityThreshold)
                        .topK(filesTopK)
                        .filterExpression(new FilterExpressionBuilder()
                                .eq(ACCOUNT_ID_CONTEXT_KEY, accountId)
                                .build())
                        .build();

                filesDocs = filesRetriever.retrieve(query);
                log.debug("Files RAG response time: {}ms", (System.nanoTime() - startTime) / 1_000_000);
            }

            if (chunksEnabled) {
                long startTime = System.nanoTime();
                DocumentRetriever chunksRetriever = VectorStoreDocumentRetriever.builder()
                        .vectorStore(chunksVectorStore)
                        .similarityThreshold(chunksSimilarityThreshold)
                        .topK(chunksTopK)
                        .build();

                chunksDocs = chunksRetriever.retrieve(query);
                log.debug("Chunks RAG response time: {}ms", (System.nanoTime() - startTime) / 1_000_000);
            }

            log.info("Retrieved {} files and {} chunks documents for query: '{}'", filesDocs.size(), chunksDocs.size(), query.text());

            List<Document> combined = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();

            filesDocs.stream().filter(doc -> seenIds.add(doc.getId())).forEach(combined::add);
            chunksDocs.stream().filter(doc -> seenIds.add(doc.getId())).forEach(combined::add);

            return combined;
        };

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(multiSourceRetriever)
                .documentJoiner(new ConcatenationDocumentJoiner())
                .build();
    }
}
