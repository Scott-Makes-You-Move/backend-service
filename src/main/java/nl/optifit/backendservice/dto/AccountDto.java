package nl.optifit.backendservice.dto;

import lombok.*;
import nl.optifit.backendservice.model.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDto {
    private String accountId;

    public static AccountDto fromAccount(Account account) {
        return AccountDto.builder().accountId(account.getId()).build();
    }
}
