package nl.optifit.backendservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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

    public List<Document> buildContext(String accountId, String query) throws ExecutionException, InterruptedException {
        if (!chunksEnabled && !filesEnabled) {
            return List.of();
        }

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        try (executor) {
            var filesFuture = executor.submit(() -> filesEnabled ? fileService.search(accountId) : List.<Document>of());
            var chunksFuture = executor.submit(() -> chunksEnabled ? chunkService.search(query) : List.<Document>of());

            var files = filesFuture.get();
            var chunks = chunksFuture.get();

            return Stream.concat(files.stream(), chunks.stream()).toList();
        }
    }
}
