package dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class Train {
    String name;
    int capacity;
    Node current;
    @Builder.Default
    int availableAtInSeconds = 0;
}
