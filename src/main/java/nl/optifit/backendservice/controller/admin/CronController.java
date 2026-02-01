package nl.optifit.backendservice.controller.admin;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.cron.DriveCronScheduler;
import nl.optifit.backendservice.service.FileService;
import nl.optifit.backendservice.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("@jwtConverter.currentUserHasRole('smym-admin')")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/management")
@Tag(name = "Bootstrap", description = "Operations related to bootstrapping")
@RestController
public class CronController {

    private final FileService fileService;
    private final SessionService sessionService;

    @PostMapping("/execute")
    public ResponseEntity<String> executeCronJob(@RequestParam("cronjob") String cronjob) {
        if (cronjob.equals("sync-files")) {
            fileService.syncFiles();
            return ResponseEntity.ok("Sync files successful");
        } else if (cronjob.equals("remove-stale-sessions")) {
            sessionService.removeStaleSessions();
            return ResponseEntity.ok("Remove stale sessions successful");
        } else {
            throw new IllegalArgumentException("Cronjob not recognized");
        }
    }
}
