package nl.optifit.backendservice.dto;

import org.springframework.ai.vectorstore.SearchRequest;

public record SearchQueryDto(String query, int topK, double similarityThreshold, String filterExpression) {

    public static SearchRequest toSearchRequest(SearchQueryDto searchQueryDto) {
        return SearchRequest.builder()
                .query(searchQueryDto.query())
                .topK(searchQueryDto.topK())
                .similarityThreshold(searchQueryDto.similarityThreshold())
                .filterExpression(searchQueryDto.filterExpression())
                .build();
    }
}
