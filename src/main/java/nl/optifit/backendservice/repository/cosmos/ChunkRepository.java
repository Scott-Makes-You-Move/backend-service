package nl.optifit.backendservice.repository.cosmos;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import nl.optifit.backendservice.model.Chunk;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends CosmosRepository<Chunk, String> {
    @Query("SELECT * FROM products p WHERE p.topic = @topic")
    List<Chunk> getChunkByTopic(@Param("topic") String topic);
}
