package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/notification")
@Tag(name = "Account", description = "Operations related to notifications")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping()
    public ResponseEntity<Void> notifyUsers() {
        notificationService.sendNotification();
        return ResponseEntity.noContent().build();
    }
}
