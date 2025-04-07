package nl.optifit.backendservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NotificationAckDto {
    private UUID sessionId;
    private String accountId;
}
