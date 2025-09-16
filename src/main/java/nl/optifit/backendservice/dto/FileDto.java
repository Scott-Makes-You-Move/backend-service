package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.document.Document;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {
    private String id;
    private String accountId;
    private String content;

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
