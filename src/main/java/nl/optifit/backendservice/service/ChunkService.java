package nl.optifit.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.dto.SearchQueryDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ChunkService {
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    public ChunkService(ObjectMapper objectMapper, @Qualifier("chunksVectorStore") VectorStore vectorStore) {
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    public Document storeChunk(ChunkDto chunkDto) {
        log.info("Storing chunk '{}'", chunkDto.getId());
        Document document = chunkDto.toDocument();
        vectorStore.add(List.of(document));
        log.info("Chunk '{}' stored", chunkDto.getId());
        return document;
    }

    public List<Document> storeChunks(MultipartFile file) {
        try {
            List<Document> documents = Arrays.asList(objectMapper.readValue(file.getInputStream(), ChunkDto[].class)).stream()
                    .map(ChunkDto::toDocument)
                    .toList();

            vectorStore.add(documents);
            log.info("Chunks stored");
            return documents;
        } catch (Exception exception) {
            log.error("Error while storing chunks", exception);
            throw new RuntimeException(exception);
        }
    }

    public List<Document> search(SearchRequest searchRequest) {
        log.info("Searching for chunks");
        return vectorStore.similaritySearch(searchRequest);
    }

    public List<Document> search(SearchQueryDto searchQueryDto) {
        log.info("Searching for chunks");

        SearchRequest searchRequest = SearchRequest.builder()
                .query(searchQueryDto.getQuery())
                .topK(searchQueryDto.getTopK())
                .similarityThreshold(searchQueryDto.getSimilarityThreshold())
                .filterExpression(searchQueryDto.getFilterExpression())
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }
}
