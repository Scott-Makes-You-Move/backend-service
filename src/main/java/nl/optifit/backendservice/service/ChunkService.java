package nl.optifit.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.dto.zapier.ChatbotResponseDto;
import nl.optifit.backendservice.dto.zapier.UserMessageDto;
import nl.optifit.backendservice.model.Chunk;
import nl.optifit.backendservice.repository.cosmos.ChunkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChunkService {
    private final ObjectMapper objectMapper;
    private final ChunkRepository chunkRepository;
    private final ZapierService zapierService;

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

    public ResponseEntity<ChatbotResponseDto> sendChatbotMessage(UserMessageDto userMessageDto) {
        return zapierService.sendChatbotMessage(userMessageDto);
    }
}
