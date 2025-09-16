package nl.optifit.backendservice.configuration;

import nl.optifit.backendservice.advisor.MaskingAdvisor;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AiConfiguration {

    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;
    @Value("${chat.client.advisors.rag.enabled}")
    private boolean ragEnabled;
    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;
    @Value("${chat.client.advisors.rag.similarity-threshold}")
    private double similarityThreshold;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, @Qualifier("chunksVectorStore") VectorStore vectorStore) {
        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

        List<Advisor> advisors = new ArrayList<>();
        advisors.add(messageChatMemoryAdvisor);

        if (ragEnabled) {
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = getRetrievalAugmentationAdvisor(vectorStore);
            advisors.add(retrievalAugmentationAdvisor);
        }
        if (maskingEnabled) {
            MaskingAdvisor maskingAdvisor = new MaskingAdvisor();
            advisors.add(maskingAdvisor);
        }
        if (filesEnabled) {
            // Create a custom FilesAdvisor here
            // advisors.add(new FilesAdvisor());
        }

        return chatClientBuilder.defaultAdvisors(advisors).build();
    }

    @NotNull
    private RetrievalAugmentationAdvisor getRetrievalAugmentationAdvisor(@Qualifier("chunksVectorStore") VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(similarityThreshold)
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
