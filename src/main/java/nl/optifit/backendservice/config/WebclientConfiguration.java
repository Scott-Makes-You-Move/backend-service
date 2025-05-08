package nl.optifit.backendservice.config;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.client.*;

@Configuration
public class WebclientConfiguration {

    @Value("${zapier.webhook-url}")
    private String zapierWebhookUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(zapierWebhookUrl)
                .build();
    }
}
