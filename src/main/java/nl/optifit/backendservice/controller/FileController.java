package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.FileDto;
import nl.optifit.backendservice.service.FileService;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/files")
@Tag(name = "Search", description = "Operations related to files")
@RestController
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<Document> storeChunk(@RequestBody FileDto fileDto) {
        log.info("Storing chunk '{}'", fileDto.getId());
        Document document = fileService.storeFile(fileDto);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncFiles() {
        log.info("Syncing files");
        fileService.syncFiles();
        return ResponseEntity.ok().build();
    }
}
