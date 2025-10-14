package nl.optifit.backendservice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponseDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PagedResponseDto<T> fromPage(Page<T> page) {
        return new PagedResponseDto<>(page.getContent(), page.getNumber(), page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages());
    }
}
