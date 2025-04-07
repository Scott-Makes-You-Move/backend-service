package nl.optifit.backendservice.controller;

import lombok.RequiredArgsConstructor;
import nl.optifit.backendservice.model.Notification;
import nl.optifit.backendservice.service.NotificationPushService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
@RestController
public class NotificationController {
    private final NotificationPushService notificationPushService;

    @PostMapping
    public void sendNotification() {
        notificationPushService.broadcast("New notification");
    }
}
