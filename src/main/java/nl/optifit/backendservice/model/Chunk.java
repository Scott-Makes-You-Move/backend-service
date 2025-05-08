package nl.optifit.backendservice.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "cosmicworks")
public class Chunk {

    private String id;
    @PartitionKey
    private String topic;
    private String subtopic;
    private String content;
    private List<Float> embedding;
}
