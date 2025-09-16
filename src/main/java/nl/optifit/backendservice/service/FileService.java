package nl.optifit.backendservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.optifit.backendservice.dto.FileDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FileService {
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    public FileService(ObjectMapper objectMapper, @Qualifier("filesVectorStore") VectorStore vectorStore) {
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    public Document storeFile(FileDto fileDto) {
        log.info("Storing file '{}'", fileDto.getId());
        Document document = fileDto.toDocument();
        vectorStore.add(List.of(document));
        log.info("File '{}' stored", fileDto.getId());
        return document;
    }
}
