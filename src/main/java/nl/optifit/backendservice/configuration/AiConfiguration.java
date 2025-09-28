package nl.optifit.backendservice.configuration;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.advisor.MaskingAdvisor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
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

@Slf4j
@Configuration
public class AiConfiguration {

    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;
    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.chunks.similarity-threshold}")
    private double chunksSimilarityThreshold;

    private final VectorStore chunksVectorStore;

    public AiConfiguration(@Qualifier("chunksVectorStore") VectorStore chunksVectorStore) {
        this.chunksVectorStore = chunksVectorStore;
    }

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

        if (chunksEnabled) {
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = getRetrievalAugmentationAdvisor();
            advisors.add(retrievalAugmentationAdvisor);
        }
        if (maskingEnabled) {
            MaskingAdvisor maskingAdvisor = new MaskingAdvisor();
            advisors.add(maskingAdvisor);
        }

        return chatClientBuilder.defaultAdvisors(advisors).build();
    }

    @NotNull
    private RetrievalAugmentationAdvisor getRetrievalAugmentationAdvisor() {
        VectorStoreDocumentRetriever delegate = VectorStoreDocumentRetriever.builder()
                .similarityThreshold(chunksSimilarityThreshold)
                .vectorStore(chunksVectorStore)
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
