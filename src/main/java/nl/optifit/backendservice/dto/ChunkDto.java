package nl.optifit.backendservice.dto;

import org.springframework.ai.document.Document;

import java.util.Map;

public record ChunkDto(String id, String topic, String subtopic, String content) {

    public Document toDocument() {
        return Document.builder()
                .id(this.id)
                .metadata(Map.of(
                        "topic", this.topic,
                        "subtopic", this.subtopic
                ))
                .text(this.content)
                .build();
    }
}
