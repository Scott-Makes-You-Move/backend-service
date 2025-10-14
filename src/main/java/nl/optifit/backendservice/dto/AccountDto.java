package nl.optifit.backendservice.dto;

import nl.optifit.backendservice.model.Account;

public record AccountDto(String accountId) {

    public static AccountDto fromAccount(Account account) {
        return new AccountDto(account.getId());
    }
}
