package nl.optifit.backendservice.service;

import lombok.*;
import nl.optifit.backendservice.dto.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.*;

import java.util.*;

@RequiredArgsConstructor
@Service
public class ZapierService {

    private final WebClient webClient;

    public Mono<ResponseEntity<String>> triggerZapierWebhook(UsersWithMobilitiesDto userWithMobilityScoreDto) {
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userWithMobilityScoreDto)
                .retrieve()
                .toEntity(String.class);
    }
}
