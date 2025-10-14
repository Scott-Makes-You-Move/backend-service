package nl.optifit.backendservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.advisor.MaskingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class AiConfiguration {

    private final VectorStore chunksVectorStore;

    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;
    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarity-threshold}")
    private double chunksSimilarityThreshold;
    @Value("${chat.client.advisors.chunks.topK}")
    private int chunksTopK;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

        List<Advisor> advisors = new ArrayList<>();
        advisors.add(messageChatMemoryAdvisor);

        if (maskingEnabled) {
            MaskingAdvisor maskingAdvisor = new MaskingAdvisor();
            advisors.add(maskingAdvisor);
        }
        if (chunksEnabled) {
            advisors.add(getChunksAdvisor());
        }

        return chatClientBuilder.defaultAdvisors(advisors).build();
    }

    private RetrievalAugmentationAdvisor getChunksAdvisor() {
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
