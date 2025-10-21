package nl.optifit.backendservice.dto;

import nl.optifit.backendservice.model.Account;

public record AccountDto(String accountId, String timezone) {

    public static AccountDto fromAccount(Account account) {
        return new AccountDto(account.getId(), account.getTimezone());
    }
}
