package nl.optifit.backendservice.configuration;

import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.advisor.MaskingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class ChatClientConfiguration {

    @Value("${chat.client.advisors.masking.enabled}")
    private boolean maskingEnabled;
    @Value("${chat.client.advisors.chat-memory.enabled}")
    private boolean chatMemoryEnabled;
    @Value("${chat.client.advisors.chat-memory.max-messages}")
    private int maxMessages;
    @Value("${chat.client.advisors.logging.enabled}")
    private boolean loggingEnabled;


    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        List<Advisor> advisors = new ArrayList<>();

        if (chatMemoryEnabled) {
            advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }
        if (maskingEnabled) {
            advisors.add(new MaskingAdvisor());
        }
        if (loggingEnabled) {
            advisors.add(new SimpleLoggerAdvisor());
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
}
