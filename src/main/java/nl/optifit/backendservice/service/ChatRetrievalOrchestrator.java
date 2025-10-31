package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class ChatRetrievalOrchestrator {

    @Value("${chat.client.advisors.chunks.enabled}")
    private boolean chunksEnabled;
    @Value("${chat.client.advisors.files.enabled}")
    private boolean filesEnabled;

    private final FileService fileService;
    private final ChunkService chunkService;

    public List<Document> buildContext(String accountId, String query) {
        if (!chunksEnabled && !filesEnabled) {
            return List.of();
        }

        List<Document> files = new ArrayList<>();
        List<Document> chunks = new ArrayList<>();

        if (filesEnabled) {
            files = fileService.search(accountId);
        }

        if (chunksEnabled) {
            chunks = chunkService.search(query);
        }

        return Stream.concat(files.stream(), chunks.stream()).toList();
    }
}
