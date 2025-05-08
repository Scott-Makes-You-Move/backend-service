package nl.optifit.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.dto.VectorSearchRequestDto;
import nl.optifit.backendservice.model.Chunk;
import nl.optifit.backendservice.repository.cosmos.ChunkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingService {
    private final ObjectMapper objectMapper;
    private final ChunkRepository chunkRepository;

    public ChunkDto storeChunk(ChunkDto chunkDto) {
        Chunk chunk = ChunkDto.toChunk(chunkDto);
        Chunk savedChunk = chunkRepository.save(chunk);

        log.info("Chunk saved successfully: '{}'", savedChunk.getId());
        return chunkDto;
    }

    public List<ChunkDto> storeChunks(MultipartFile file) {
        try {
            List<ChunkDto> chunkDtos = Arrays.asList(objectMapper.readValue(file.getInputStream(), ChunkDto[].class)).stream()
                    .toList();

            List<Chunk> chunks = chunkDtos.stream()
                    .map(ChunkDto::toChunk)
                    .toList();
            chunkRepository.saveAll(chunks);

            return chunkDtos;
        } catch (Exception exception) {
            log.error("Error while storing chunks", exception);
            throw new RuntimeException(exception);
        }
    }

    public List<ChunkDto> searchSimilarChunks(VectorSearchRequestDto vectorSearchRequest) {
        Iterable<Chunk> all = chunkRepository.findAll();

        return StreamSupport.stream(all.spliterator(), false)
                .map(chunk -> {
                    double similarity = cosineSimilarity(vectorSearchRequest.getEmbedding(), chunk.getEmbedding());
                    return new AbstractMap.SimpleEntry<>(chunk, similarity);
                }).sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(vectorSearchRequest.getTopK())
                .map(chunkEntry -> ChunkDto.fromChunk(chunkEntry.getKey()))
                .toList();
    }

    private double cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must be of same length");
        }

        double dot = 0.0, normVec1 = 0.0, normVec2 = 0.0;
        for (int i = 0; i < vec1.size(); i++) {
            double v1 = vec1.get(i);
            double v2 = vec2.get(i);
            dot += v1 * v2;
            normVec1 += v1 * v1;
            normVec2 += v2 * v2;
        }

        return dot / (Math.sqrt(normVec1) * Math.sqrt(normVec2));
    }
}
