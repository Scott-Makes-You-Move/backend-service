package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/embeddings")
@Tag(name = "Search", description = "Operations related to embeddings")
@RestController
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping
    public ResponseEntity<ChunkDto> storeChunk(@RequestBody ChunkDto chunkDto) {
        log.info("Storing chunk '{}'", chunkDto.getId());
        ChunkDto savedChunk = embeddingService.storeChunk(chunkDto);
        return ResponseEntity.ok(savedChunk);
    }
}
