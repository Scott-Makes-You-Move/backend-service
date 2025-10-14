package nl.optifit.backendservice.dto;

public record SearchQueryDto(String query, int topK, double similarityThreshold, String filterExpression) {
}
