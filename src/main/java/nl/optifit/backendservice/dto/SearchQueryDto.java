package nl.optifit.backendservice.dto;

import org.springframework.ai.vectorstore.SearchRequest;

public record SearchQueryDto(String query, int topK, double similarityThreshold, String filterExpression) {

    public SearchRequest toSearchRequest() {
        return SearchRequest.builder()
                .query(this.query())
                .topK(this.topK())
                .similarityThreshold(this.similarityThreshold())
                .filterExpression(this.filterExpression())
                .build();
    }
}
