import dto.Edge;
import dto.Node;
import dto.Train;
import dto.TravelPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Application {
    static Map<Node, List<Edge>> trainTracks = new HashMap<>();
    static List<TravelPlan> history = new ArrayList<>();

    static Node a = Node.builder().name("A").build();
    static Node b = Node.builder().name("B").build();
    static Node c = Node.builder().name("C").build();

    static Edge E1 = Edge.builder()
            .name("E1")
            .source(a)
            .destination(b)
            .journeyTimeInSeconds(30)
            .build();
    static Edge E2 = Edge.builder()
            .name("E2")
            .source(b)
            .destination(c)
            .journeyTimeInSeconds(10)
            .build();

    static dto.Package K1 = dto.Package.builder()
            .name("K1")
            .weight(5)
            .source(a)
            .destination(c)
            .build();

    static Train Q1 = Train.builder()
            .name("Q1")
            .capacity(6)
            .current(b)
            .build();

    public static void main(String[] args) {
        // 1. Build the train tracks
        computeTrainTracks(E1);
        computeTrainTracks(E2);

        try {
            // 1. Move train to pickup
            moveTrain(Q1, a, List.of(), List.of());

            // 2. Pick up package and move back to B
            moveTrain(Q1, b, List.of(K1.getName()), List.of());

            // 3. Deliver package to C
            moveTrain(Q1, c, List.of(), List.of(K1.getName()));
        } catch (Exception e) {
            System.err.println("An error occurred during train movement: " + e.getMessage());
        }

        // Output
        history.forEach(System.out::println);
    }

    /**
     * Computes the train tracks for a given edge and updates the trainTracks map.
     * This method builds an undirected graph representation of the train tracks,
     * where each edge is represented as a connection between two nodes (stations).
     *
     * @param edge
     */
    public static void computeTrainTracks(Edge edge) {
        trainTracks.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        trainTracks.computeIfAbsent(edge.getDestination(), k -> new ArrayList<>()).add(
                new Edge(edge.getName(),
                        edge.getDestination(),
                        edge.getSource(),
                        edge.getJourneyTimeInSeconds() / 60)); // Convert seconds to minutes for the reverse edge);
    }

    /**
     * Moves a train to a specified destination node, updating its current position and available time through log collection.
     *
     * @param train
     * @param destination
     * @param pickUps
     * @param dropOffs
     */
    public static void moveTrain(Train train, Node destination, List<String> pickUps, List<String> dropOffs) throws Exception {
        // Base case: If the train is already at the destination (self-loop), log the move without time cost which default should be 0
        if (train.getCurrent().equals(destination)) {
            history.add(TravelPlan.builder()
                    .time(train.getAvailableAtInSeconds())
                    .train(train.getName())
                    .from(train.getCurrent().getName())
                    .to(destination.getName())
                    .pickUps(pickUps)
                    .dropOffs(dropOffs)
                    .build());
            return;
        }

        // Get the path using a pathfinding method (Dijkstra chosen)
        List<Node> path = findShortestPath(train.getCurrent(), destination);

        // Verifies if the path is disconnected or is also a self-loop (self-loop should not happen here and be caught above)
        if (path == null || path.size() < 2) {
            System.err.println("No valid path found from " + train.getCurrent().getName() + " to " + destination.getName());
            return;
        }

        // Capacity check: sum weights of packages to pick up
        int totalWeight = 0;
        for (String packageName : pickUps) {
            dto.Package pkg = getPackageByName(packageName);
            if (pkg != null)
                totalWeight += pkg.getWeight();
        }

        // Throw if train cannot carry all packages
        if (totalWeight > train.getCapacity())
            throw new Exception("Train " + train.getName() + " exceeded its capacity! Max: " + train.getCapacity() + ", Needed: " + totalWeight);

        // Traverse through the path hop by hop
        for (int i = 1; i < path.size(); i++) {
            Node from = path.get(i - 1);
            Node to = path.get(i);

            // Find the direct edge from 'from' to 'to'
            Edge edge = trainTracks.get(from).stream()
                    .filter(e -> e.getDestination().equals(to))
                    .findFirst()
                    // Should not happen as we already add bi-directional edges in computeTrainTracks, but nice to have error handling
                    .orElseThrow(() -> new Exception("No direct edge found between " + from.getName() + " and " + to.getName()));

            // Only pick up at the start, and drop off at the final hop
            List<String> thisPickUps = (i == 1) ? pickUps : Collections.emptyList();
            List<String> thisDropOffs = (i == path.size() - 1) ? dropOffs : Collections.emptyList();

            // Log the travel
            history.add(TravelPlan.builder()
                    .time(train.getAvailableAtInSeconds())
                    .train(train.getName())
                    .from(from.getName())
                    .to(to.getName())
                    .pickUps(thisPickUps)
                    .dropOffs(thisDropOffs)
                    .build());

            // Advance train's internal state of the total waiting time
            train.setAvailableAtInSeconds(train.getAvailableAtInSeconds() + edge.getJourneyTimeInSeconds());
            train.setCurrent(to);
        }
    }

    public static dto.Package getPackageByName(String name) {
        if (K1.getName().equals(name))
            return K1;

        return null;
    }


    // Generated by ChatGPT
    public static List<Node> findShortestPath(Node start, Node target) {
        Map<Node, Integer> distance = new HashMap<>();
        Map<Node, Node> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(distance::get));

        // Initialize
        for (Node node : trainTracks.keySet()) {
            distance.put(node, Integer.MAX_VALUE);
            previous.put(node, null);
        }
        distance.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.equals(target))
                break;

            for (Edge edge : trainTracks.getOrDefault(current, List.of())) {
                Node neighbor = edge.getDestination();
                int alt = distance.get(current) + edge.getJourneyTimeInSeconds();

                if (alt < distance.get(neighbor)) {
                    distance.put(neighbor, alt);
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        // Reconstruct path
        List<Node> path = new ArrayList<>();
        for (Node at = target; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        return path.get(0).equals(start) ? path : null; // return null if no path found
    }
}

