import java.util.*;

/**
 * Friendship Graph
 */
public class Graph {
    // all vertices (Email addresses)
    private final Set<String> vertices = new HashSet<>();
    // adjacent lists for sent messages (X -> Ys)
    private final Map<String, Set<String>> sent = new HashMap<>();
    // adjacent lists for received messages (X <- Ys)
    private final Map<String, Set<String>> received = new HashMap<>();
    // connectors
    private final Set<String> connectors = new HashSet<>();
    // teams
    private final List<Set<String>> teams = new ArrayList<>();
    // counter for calculate connectors
    private int counter = 0;

    /**
     * Constructor
     */
    public Graph() {
    }

    /**
     * Add vertex to graph
     *
     * @param vertex the vertex to be added
     */
    public void addVertex(String vertex) {
        vertices.add(vertex);
    }

    /**
     * Add edge to graph
     *
     * @param from the src vertex
     * @param to   the dst vertex
     */
    public void addEdge(String from, String to) {
        // add vertex
        addVertex(from);
        addVertex(to);

        // 'from' sent message to 'to'
        if (!sent.containsKey(from)) {
            sent.put(from, new HashSet<>());
        }
        sent.get(from).add(to);

        // 'to' received message from 'from'
        if (!received.containsKey(to)) {
            received.put(to, new HashSet<>());
        }
        received.get(to).add(from);
    }

    /**
     * Check whether graph has vertex
     *
     * @param vertex the vertex to check
     * @return true/false
     */
    public boolean hasVertex(String vertex) {
        return vertices.contains(vertex);
    }

    /**
     * Get connectors
     *
     * @return connectors
     */
    public Set<String> getConnectors() {
        return connectors;
    }

    /**
     * Calculate connectors and teams
     */
    public void calculate() {
        // visited set
        Set<String> visited = new HashSet<>();
        // dfs number map
        Map<String, Integer> dfs = new HashMap<>();
        // back number map
        Map<String, Integer> back = new HashMap<>();

        // process all vertices
        for (String vertex : vertices) {
            // skip visited vertex
            if (visited.contains(vertex)) {
                continue;
            }

            // create a new empty team
            Set<String> team = new HashSet<>();
            // run DFS from the vertex
            DFS(vertex, visited, dfs, back, team);
            // add team to teams
            teams.add(team);
        }
    }

    /**
     * Get how many individuals the vertex sent message to
     *
     * @param vertex the vertex
     * @return individuals number
     */
    public int sentIndividualCount(String vertex) {
        // no sent
        if (!sent.containsKey(vertex)) {
            return 0;
        }

        // return sent number
        return sent.get(vertex).size();
    }

    /**
     * Get how many individuals the vertex received message from
     *
     * @param vertex the vertex
     * @return individuals number
     */
    public int receivedIndividualCount(String vertex) {
        // no receive
        if (!received.containsKey(vertex)) {
            return 0;
        }

        // return received number
        return received.get(vertex).size();
    }

    /**
     * Calculate the team number of vertex
     *
     * @param vertex the vertex
     * @return the team member number
     */
    public int teamMemberCount(String vertex) {
        // search all team
        for (Set<String> team : teams) {
            // found the vertex
            if (team.contains(vertex)) {
                // return team size
                return team.size();
            }
        }

        // not found
        return 0;
    }

    /**
     * DFS travel graph
     *
     * @param root    the root vertex
     * @param visited visited set
     * @param dfs     dfs number map
     * @param back    back number map
     * @param team    team set
     */
    private void DFS(String root, Set<String> visited, Map<String, Integer> dfs,
                     Map<String, Integer> back, Set<String> team) {
        // create stack for DFS
        Stack<String> stack = new Stack<>();
        // parent map of vertex
        Map<String, String> parents = new HashMap<>();

        // root has no parent
        parents.put(root, null);
        // push stack to root
        stack.push(root);

        // loop until stack is empty
        while (!stack.isEmpty()) {
            // check the stack top
            String u = stack.peek();

            // not visited
            if (!visited.contains(u)) {
                // create init dfs and back number
                int n = ++counter;
                dfs.put(u, n);
                back.put(u, n);

                // add vertex to visited
                visited.add(u);
                // add vertex to team
                team.add(u);
            }

            // get all neighbors of vertex
            Set<String> neighbors = new HashSet<>();
            if (sent.containsKey(u)) {
                neighbors.addAll(sent.get(u));
            }

            if (received.containsKey(u)) {
                neighbors.addAll(received.get(u));
            }

            // processed neighbors number
            boolean processed = false;
            // process all neighbors
            for (String v : neighbors) {
                // already visited, skip
                if (visited.contains(v)) {
                    continue;
                }
                // find an unvisited neighbor
                processed = true;

                // push it to stack
                stack.push(v);
                // record it's parent
                parents.put(v, u);
                // need to break for DFS
                break;
            }

            // continue DFS
            if (processed) {
                continue;
            }

            // no neighbor processed, so the vertex is leaf node or back from neighbors
            // remove it from stack
            stack.pop();

            String parent = parents.get(u);
            for (String v : neighbors) {
                // u is not backward from v
                if (!u.equals(parents.get(v))) {
                    // update back of u
                    if (!v.equals(parent)) {
                        int n = Math.min(back.get(u), dfs.get(v));
                        back.put(u, n);
                    }
                }
            }

            // children number
            int children = 0;
            for (String v : neighbors) {
                // u is backward from v
                if (u.equals(parents.get(v))) {
                    ++children;

                    // update back of u
                    if (dfs.get(u) > back.get(v)) {
                        int n = Math.min(back.get(u), back.get(v));
                        back.put(u, n);
                    }

                    // u is not root
                    if (parent != null && dfs.get(u) <= back.get(v)) {
                        connectors.add(u);
                    }
                }
            }

            // check  whether more than one vertex traveled from root
            if (parent == null && children > 1) {
                // root is connector
                connectors.add(u);
            }
        }
    }
}
