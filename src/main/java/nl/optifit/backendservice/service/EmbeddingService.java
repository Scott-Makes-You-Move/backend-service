package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.model.Chunk;
import nl.optifit.backendservice.repository.ChunkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingService {
    private final ChunkRepository chunkRepository;

    public ChunkDto storeChunk(ChunkDto chunkDto) {
        Chunk chunk = ChunkDto.toChunk(chunkDto);
        Chunk savedChunk = chunkRepository.save(chunk);

        log.info("Chunk saved successfully: '{}'", savedChunk.getId());
        return chunkDto;
    }
}
