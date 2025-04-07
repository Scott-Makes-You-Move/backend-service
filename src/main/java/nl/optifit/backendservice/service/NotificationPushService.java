package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String message) {
        log.info("Pushing notification to global");
        messagingTemplate.convertAndSend(
                "/topic/notifications",
                message
        );
    }
}
