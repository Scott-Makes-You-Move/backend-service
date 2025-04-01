package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResultListDataRepresentation<T> {
    private Integer count;
    private Boolean hasMoreItems;
    private Integer totalItems;
    private Integer skipCount;
    private Integer maxItems;
    private List<T> entries;
}
