package nl.optifit.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.ChunkDto;
import nl.optifit.backendservice.dto.SearchQueryDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class ChunkService {

    @Value("${chat.client.advisors.chunks.similarityThreshold}")
    private double chunksSimilarityThreshold;
    @Value("${chat.client.advisors.chunks.topK}")
    private int chunksTopK;

    private final ObjectMapper objectMapper;
    private final VectorStore chunksVectorStore;

    public ChunkService(ObjectMapper objectMapper, @Qualifier("chunksVectorStore") VectorStore chunksVectorStore) {
        this.objectMapper = objectMapper;
        this.chunksVectorStore = chunksVectorStore;
    }

    public Document storeChunk(ChunkDto chunkDto) {
        Document document = chunkDto.toDocument();
        storeChunks(List.of(document));
        return document;
    }

    public List<Document> storeChunks(MultipartFile file) {
        try (var inputStream = file.getInputStream()) {
            ChunkDto[] chunkDtos = objectMapper.readValue(inputStream, ChunkDto[].class);
            List<Document> documents = Stream.of(chunkDtos).map(ChunkDto::toDocument).toList();
            storeChunks(documents);
            return documents;
        } catch (IOException ioException) {
            log.error("Error while storing chunks: ", ioException);
            throw new RuntimeException(ioException);
        }
    }

    public void storeChunks(List<Document> documents) {
        chunksVectorStore.add(documents);
        log.debug("Stored {} chunks", documents.size());
    }

    public List<Document> search(SearchRequest searchRequest) {
        log.info("Searching for chunks");
        return chunksVectorStore.similaritySearch(searchRequest);
    }

    public List<Document> search(String query) {
        SearchRequest searchRequest = fromQueryString(query);
        return search(searchRequest);
    }

    public List<Document> search(SearchQueryDto searchQueryDto) {
        SearchRequest searchRequest = searchQueryDto.toSearchRequest();
        return search(searchRequest);
    }

    private SearchRequest fromQueryString(String query) {
        return SearchRequest.builder()
                .query(query)
                .topK(chunksTopK)
                .similarityThreshold(chunksSimilarityThreshold)
                .build();
    }
}
