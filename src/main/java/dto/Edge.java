package dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;

@Value
@Builder
@Jacksonized
public class Edge {
    String name;
    Node source;
    Node destination;
    int journeyTimeInSeconds;

    public Edge(String name, Node source, Node destination, int journeyTimeInMinutes) {
        this.name = name;
        this.source = source;
        this.destination = destination;
        this.journeyTimeInSeconds = journeyTimeInMinutes * 60;
    }
}
