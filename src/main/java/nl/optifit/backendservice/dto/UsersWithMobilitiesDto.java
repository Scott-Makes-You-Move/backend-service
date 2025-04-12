package nl.optifit.backendservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersWithMobilitiesDto {
    private String name;
    private String email;
    private Integer score;
    private String exerciseType;
}
