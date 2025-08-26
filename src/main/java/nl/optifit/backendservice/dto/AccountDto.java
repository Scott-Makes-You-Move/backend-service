package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.optifit.backendservice.model.Account;

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
