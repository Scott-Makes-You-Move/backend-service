package nl.optifit.backendservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nl.optifit.backendservice.model.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NotificationDto {

    private UUID sessionId;
    private String title;
    private String linkToVideo;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static NotificationDto fromNotification(Notification notification) {
        return NotificationDto.builder()
                .sessionId(notification.getSessionId())
                .title(notification.getTitle())
                .linkToVideo(notification.getLinkToVideo())
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }
}
