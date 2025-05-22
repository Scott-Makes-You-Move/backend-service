package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.optifit.backendservice.model.Chunk;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkDto {
    private String id;
    private String topic;
    private String subtopic;
    private String content;

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
