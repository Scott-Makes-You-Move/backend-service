package nl.optifit.backendservice.controller;

import com.google.api.services.drive.model.File;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.service.DriveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/drive")
@Tag(name = "Drive", description = "Operations related to Google Drive")
@RestController
public class DriveController {

    private final DriveService driveService;

    @GetMapping("/documents/list")
    public ResponseEntity<List<File>> listFiles() throws GeneralSecurityException, IOException {
        log.info("Listing files");
        List<File> files = driveService.getDriveFiles();
        return ResponseEntity.ok(files);
    }
}
