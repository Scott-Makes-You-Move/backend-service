package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.CreateAccountDTO;
import nl.optifit.backendservice.model.Account;
import nl.optifit.backendservice.service.AccountService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/account")
@RestController
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public Account createAccount(@RequestBody CreateAccountDTO createAccountDTO) {
        log.info("POST Account REST API called");
        return accountService.createAccountForId(createAccountDTO.getAccountId());
    }

    @DeleteMapping("/{accountId}")
    public void deleteAccount(@PathVariable String accountId) {
        log.info("DELETE Account REST API called");
        accountService.deleteAccount(accountId);
    }
}
