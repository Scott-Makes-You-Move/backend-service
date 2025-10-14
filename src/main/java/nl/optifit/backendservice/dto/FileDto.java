package nl.optifit.backendservice.dto;

import org.springframework.ai.document.Document;

import java.util.Map;

public record FileDto(String id, String accountId, String content) {

    public Document toDocument() {
        return Document.builder()
                .id(this.id)
                .metadata(Map.of(
                        "accountId", this.accountId
                ))
                .text(this.content)
                .build();
    }
}
