package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.model.ExerciseType;
import nl.optifit.backendservice.service.AccountService;
import nl.optifit.backendservice.service.NotificationService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static nl.optifit.backendservice.model.ExerciseType.HIP;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/test")
@RestController
public class TestController {

    private final AccountService accountService;
    private final SessionService sessionService;

    @PostMapping("/notification")
    public void test() {
        log.info("Test endpoint called");
        createSessionsForAllAccounts(HIP);
        log.info("Finished calling test endpoint");
    }

    private void createSessionsForAllAccounts(ExerciseType exerciseType) {
        accountService.findAllAccounts()
                .forEach(account -> sessionService.createSessionForAccount(account, exerciseType));
    }
}
