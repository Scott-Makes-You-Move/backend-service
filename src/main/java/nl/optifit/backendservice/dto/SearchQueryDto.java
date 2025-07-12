package nl.optifit.backendservice.dto;

import com.azure.core.annotation.Get;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQueryDto {
    private String query;
    private Integer topK;
    private Double similarityThreshold;
    private String filterExpression;
}
