package dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class TravelPlan {
    int time;
    String train;
    String from;
    String to;
    List<String> pickUps;
    List<String> dropOffs;

    public String toString() {
        return String.format("W=%ds, T=%s, N1=%s, P1=%s, N2=%s, P2=%s", time, train, from, pickUps, to, dropOffs);
    }
}
