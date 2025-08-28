package nl.optifit.backendservice.configuration;

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
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AiConfiguration {

    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;
    @Value("${chat.client.advisors.rag.enabled}")
    private boolean ragEnabled;
    @Value("${chat.client.advisors.rag.similarity-threshold}")
    private double similarityThreshold;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(similarityThreshold)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();

        MaskingAdvisor maskingAdvisor = new MaskingAdvisor();

        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

        List<Advisor> advisors = new ArrayList<>();
        advisors.add(messageChatMemoryAdvisor);
        if (ragEnabled) {
            advisors.add(retrievalAugmentationAdvisor);
        }
        if (maskingEnabled) {
            advisors.add(maskingAdvisor);
        }

        return chatClientBuilder.defaultAdvisors(advisors).build();
    }
}
