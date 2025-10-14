package nl.optifit.backendservice.controller.user;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.SearchQueryDto;
import nl.optifit.backendservice.service.FileService;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/files")
@Tag(name = "Search", description = "Operations related to files")
@RestController
public class FileController {

    private final FileService fileService;

    @PostMapping("/sync")
    public ResponseEntity<Void> syncFiles() {
        log.info("Syncing files");
        fileService.syncFiles();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestBody SearchQueryDto searchQueryDto) throws Exception {
        log.info("Performing search");
        List<Document> search = fileService.search(searchQueryDto);
        return ResponseEntity.ok(search);
    }
}
