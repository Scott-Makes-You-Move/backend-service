package nl.optifit.backendservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.dto.zapier.ChatbotResponseDto;
import nl.optifit.backendservice.dto.zapier.UserMessageDto;
import nl.optifit.backendservice.service.ChunkService;
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
    public ResponseEntity<ChunkDto> storeChunk(@RequestBody ChunkDto chunkDto) {
        log.info("Storing chunk '{}'", chunkDto.getId());
        ChunkDto savedChunk = chunkService.storeChunk(chunkDto);
        return ResponseEntity.ok(savedChunk);
    }

    @PostMapping("/upload/batch")
    public ResponseEntity<List<ChunkDto>> storeBatchChunks(@RequestParam("file") MultipartFile file) {
        log.info("Storing batch chunks");
        List<ChunkDto> savedChunks = chunkService.storeChunks(file);
        return ResponseEntity.ok(savedChunks);
    }

    @PostMapping("/search")
    public ResponseEntity<ChatbotResponseDto> search(@RequestBody UserMessageDto userMessageDto) {
        log.info("Performing search for user message '{}'", userMessageDto.getUserMessage());
        return chunkService.sendChatbotMessage(userMessageDto);
    }
}
