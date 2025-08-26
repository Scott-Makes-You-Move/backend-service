package nl.optifit.backendservice.advisor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.utility.MaskingUtil;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaskingAdvisor implements CallAdvisor {

    private static final String ENTITY_HANDLING_INSTRUCTIONS = """
            When you encounter messages containing [ENTITY_X] placeholders:
            - These represent sensitive information that has been masked for privacy
            - Respond naturally as if these were normal words
            - Never try to guess or reveal the original content
            - Maintain conversation flow while respecting privacy
            - If asked about [ENTITY_X], explain it's masked private information
            - Keep responses professional and contextually appropriate
            """;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        try {
            ChatClientRequest maskedRequest = createMaskedRequest(request);

            ChatClientResponse response = chain.nextCall(maskedRequest);

            return unmaskResponse(response);
        } catch (Exception e) {
            log.error("Masking advisor processing failed", e);
            return chain.nextCall(request);
        }
    }

    private ChatClientRequest createMaskedRequest(ChatClientRequest request) {
        Prompt originalPrompt = request.prompt();

        String originalText = originalPrompt.getUserMessage().getText();
        String maskedText = MaskingUtil.maskUserMessage(originalText);

        List<Message> messages = new ArrayList<>();

        messages.add(createEnhancedSystemMessage(originalPrompt.getSystemMessage()));

        originalPrompt.getInstructions().stream()
                .filter(m -> !(m instanceof SystemMessage) && !(m instanceof UserMessage))
                .forEach(messages::add);

        messages.add(new UserMessage(maskedText));

        Prompt maskedPrompt = new Prompt(messages, originalPrompt.getOptions());

        return request.mutate()
                .prompt(maskedPrompt)
                .build();
    }

    private SystemMessage createEnhancedSystemMessage(@Nullable SystemMessage original) {
        String enhancedContent = (original != null ? original.getText() + "\n\n" : "")
                + ENTITY_HANDLING_INSTRUCTIONS;
        return new SystemMessage(enhancedContent);
    }

    private ChatClientResponse unmaskResponse(ChatClientResponse response) {
        ChatResponse originalResponse = response.chatResponse();

        List<Generation> processedGenerations = originalResponse.getResults().stream()
                .map(generation -> {
                    String originalText = generation.getOutput().getText();
                    String unmaskedText = MaskingUtil.unmaskAssistantMessage(originalText);
                    return new Generation(new AssistantMessage(unmaskedText));
                })
                .collect(Collectors.toList());

        ChatResponse processedResponse = new ChatResponse(
                processedGenerations,
                originalResponse.getMetadata()
        );

        return response.mutate()
                .chatResponse(processedResponse)
                .build();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
