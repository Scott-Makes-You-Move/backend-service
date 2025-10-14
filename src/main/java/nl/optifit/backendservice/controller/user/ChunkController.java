package nl.optifit.backendservice.controller.user;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.dto.SearchQueryDto;
import nl.optifit.backendservice.service.ChunkService;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/chunks")
@Tag(name = "Search", description = "Operations related to chunks")
@RestController
public class ChunkController {

    private final ChunkService chunkService;

    @PostMapping("/upload")
    public ResponseEntity<Document> storeChunk(@RequestBody ChunkDto chunkDto) {
        log.info("POST Store Chunk REST API called");
        Document document = chunkService.storeChunk(chunkDto);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/upload/batch")
    public ResponseEntity<List<Document>> storeBatchChunks(@RequestParam("file") MultipartFile file) {
        log.info("POST Store Batch Chunks REST API called");
        List<Document> savedChunks = chunkService.storeChunks(file);
        return ResponseEntity.ok(savedChunks);
    }

    @PostMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestBody SearchQueryDto searchQueryDto) throws Exception {
        log.info("POST Search Chunks REST API called");
        List<Document> search = chunkService.search(searchQueryDto);
        return ResponseEntity.ok(search);
    }
}
