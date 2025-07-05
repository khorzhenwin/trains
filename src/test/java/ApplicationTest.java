import dto.Edge;
import dto.Node;
import dto.Train;
import dto.TravelPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationTest {

    private Node A, B, C;
    private static Map<Node, List<Edge>> trainTracks;

    @BeforeEach
    public void setupGraph() {
        A = Node.builder().name("A").build();
        B = Node.builder().name("B").build();
        C = Node.builder().name("C").build();

        Edge AB = new Edge("E1", A, B, 5);  // 5 min
        Edge BA = new Edge("E1", B, A, 5);
        Edge BC = new Edge("E2", B, C, 7);  // 7 min
        Edge CB = new Edge("E2", C, B, 7);

        trainTracks = new HashMap<>();
        trainTracks.put(A, List.of(AB));
        trainTracks.put(B, List.of(BA, BC));
        trainTracks.put(C, List.of(CB));

        // Injection into Application class
        Application.trainTracks = trainTracks;
    }

    @Test
    public void testDijkstraFindsPathTwoStationsAway() {
        // Act: find shortest path from A to C
        List<Node> path = Application.findShortestPath(A, C);

        // Assert that a path was found
        assertNotNull(path, "Path should not be null");

        // Assert path has 3 nodes (A → B → C)
        assertEquals(3, path.size(), "Path should contain 3 nodes");

        // Assert correct order of traversal
        assertEquals("A", path.get(0).getName());
        assertEquals("B", path.get(1).getName());
        assertEquals("C", path.get(2).getName());
    }

    @Test
    public void testMoveTrainTravelsTwoStationsAndPicksUpPackage() throws Exception {
        // Arrange: create a train starting at node A
        Train train = Train.builder()
                .name("Q1")
                .capacity(10)
                .current(Application.a)
                .availableAtInSeconds(0)
                .build();

        // Clear travel history before testing
        Application.history = new ArrayList<>();

        // Act: move train to C with pickup at A, drop off at C
        Application.moveTrain(train, C, List.of("K1"), List.of("K1"));

        List<TravelPlan> logs = Application.history;

        // Assert that the train moved in two steps (A → B, B → C)
        assertEquals(2, logs.size(), "Expected 2 hops (A → B → C)");

        TravelPlan first = logs.get(0);
        TravelPlan second = logs.get(1);

        // First hop: A → B, with pickup of K1
        assertEquals("A", first.getFrom());
        assertEquals("B", first.getTo());
        assert (first.getPickUps().contains("K1"));

        // Second hop: B → C, with drop-off of K1
        assertEquals("B", second.getFrom());
        assertEquals("C", second.getTo());
        System.out.println(second.getDropOffs());
        assert (second.getDropOffs().contains("K1"));

        // Ensure the train name is consistent
        assertEquals("Q1", first.getTrain());
    }

    @Test
    public void testSelfLoopWithPickupsAndDropoffs() throws Exception {
        Train train = Train.builder()
                .name("Q1")
                .capacity(10)
                .current(Application.a)
                .availableAtInSeconds(0)
                .build();

        Application.history = new ArrayList<>();

        Application.moveTrain(train, Application.a, List.of("K1"), List.of("K1"));

        assertEquals(1, Application.history.size());
        TravelPlan plan = Application.history.get(0);
        assertEquals("A", plan.getFrom());
        assertEquals("A", plan.getTo());
        assertTrue(plan.getPickUps().contains("K1"));
        assertTrue(plan.getDropOffs().contains("K1"));
    }


    @Test
    public void testNoPathBetweenNodes() throws Exception {
        Node Z = Node.builder().name("Z").build(); // not connected

        Train train = Train.builder()
                .name("Q1")
                .capacity(10)
                .current(Z)
                .availableAtInSeconds(0)
                .build();

        Application.history = new ArrayList<>();

        Application.moveTrain(train, Application.c, List.of("K1"), List.of("K1"));

        // No movement should happen
        assertTrue(Application.history.isEmpty());
    }

    @Test
    public void testDropOffWithoutPickup() throws Exception {
        Train train = Train.builder()
                .name("Q1")
                .capacity(10)
                .current(Application.a)
                .availableAtInSeconds(0)
                .build();

        Application.history = new ArrayList<>();

        Application.moveTrain(train, Application.c, Collections.emptyList(), List.of("K1"));

        TravelPlan lastMove = Application.history.get(Application.history.size() - 1);
        assertTrue(lastMove.getDropOffs().contains("K1"));  // It will log it, even if logically odd
    }

    @Test
    public void testTrainExceedsCapacityShouldFail() {
        // Create a train with capacity 3 (too small for K1)
        Train train = Train.builder()
                .name("Q2")
                .capacity(3)  // K1 is 5
                .current(Application.a)
                .availableAtInSeconds(0)
                .build();

        Application.history = new ArrayList<>();

        Exception exception = assertThrows(Exception.class, () -> {
            Application.moveTrain(train, Application.c, List.of("K1"), List.of("K1"));
        });

        assertTrue(exception.getMessage().contains("exceeded its capacity"));
    }
}
