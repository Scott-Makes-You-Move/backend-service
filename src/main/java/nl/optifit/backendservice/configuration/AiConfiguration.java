package nl.optifit.backendservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.advisor.MaskingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class AiConfiguration {

    private final VectorStore chunksVectorStore;
    private final VectorStore filesVectorStore;

    public AiConfiguration(@Qualifier("chunksVectorStore") VectorStore chunksVectorStore, @Qualifier("filesVectorStore") VectorStore filesVectorStore) {
        this.chunksVectorStore = chunksVectorStore;
        this.filesVectorStore = filesVectorStore;
    }

    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;

    @Value("${chat.client.advisors.chat-memory.enabled}")
    private boolean chatMemoryEnabled;
    @Value("${chat.client.advisors.chat-memory.max-messages}")
    private int maxMessages;

    @Value("${chat.client.advisors.logging.enabled}")
    private boolean loggingEnabled;

    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarity-threshold}")
    private double chunksSimilarityThreshold;
    @Value("${chat.client.advisors.chunks.topK}")
    private int chunksTopK;

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        List<Advisor> advisors = new ArrayList<>();

        if (chatMemoryEnabled) {
            advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        if (loggingEnabled) {
            advisors.add(new SimpleLoggerAdvisor());
        }
        if (maskingEnabled) {
            advisors.add(new MaskingAdvisor());
        }
        if (chunksEnabled) {
            advisors.add(retrievalAugmentationAdvisor());
        }

        return ChatClient.builder(chatModel)
                .defaultAdvisors(advisors)
                .build();
    }

    @ConditionalOnProperty(name = "chat.client.advisors.chat-memory.enabled", havingValue = "true")
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(maxMessages).build();
    }

    @ConditionalOnProperty(name = "chat.client.advisors.chunks.enabled", havingValue = "true")
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor() {
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();

        VectorStoreDocumentRetriever chunksRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(chunksVectorStore)
                .topK(chunksTopK)
                .similarityThreshold(chunksSimilarityThreshold)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(chunksRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }
}
