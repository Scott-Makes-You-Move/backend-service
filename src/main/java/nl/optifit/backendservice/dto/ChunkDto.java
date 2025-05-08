package nl.optifit.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.optifit.backendservice.model.Chunk;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkDto {
    private String id;
    private String topic;
    private String subtopic;
    private String content;
    private List<Float> embedding;

    public static Chunk toChunk(ChunkDto chunkDto) {
        Chunk chunk = new Chunk();
        chunk.setId(chunkDto.getId());
        chunk.setTopic(chunkDto.getTopic());
        chunk.setSubtopic(chunkDto.getSubtopic());
        chunk.setContent(chunkDto.getContent());
        chunk.setEmbedding(chunkDto.getEmbedding());
        return chunk;
    }

    public static ChunkDto fromChunk(Chunk chunk) {
        return ChunkDto.builder()
                .id(chunk.getId())
                .topic(chunk.getTopic())
                .subtopic(chunk.getSubtopic())
                .content(chunk.getContent())
                .embedding(chunk.getEmbedding())
                .build();
    }
}
