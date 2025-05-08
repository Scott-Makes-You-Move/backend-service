package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequestDto {
    private int topK = 3;
    private List<Float> embedding;
}
