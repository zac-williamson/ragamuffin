package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.*;

/**
 * A* pathfinding on the 3D voxel grid.
 * Finds paths for NPCs to navigate around obstacles in the block world.
 */
public class Pathfinder {

    private static final int MAX_SEARCH_NODES = 2000; // Allow larger search area for buildings
    private static final float DIAGONAL_COST = 1.414f;
    private static final float STRAIGHT_COST = 1.0f;

    /**
     * Find a path from start to end position in the world.
     * Returns a list of Vector3 waypoints, or null if no path found.
     */
    public List<Vector3> findPath(World world, Vector3 start, Vector3 end) {
        // Convert to block coordinates
        int startX = (int) Math.floor(start.x);
        int startY = (int) Math.floor(start.y);
        int startZ = (int) Math.floor(start.z);

        int endX = (int) Math.floor(end.x);
        int endY = (int) Math.floor(end.y);
        int endZ = (int) Math.floor(end.z);

        // Check if start and end are walkable
        if (!isWalkable(world, startX, startY, startZ) || !isWalkable(world, endX, endY, endZ)) {
            return null;
        }

        // A* search
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startX, startY, startZ);
        startNode.g = 0;
        startNode.h = heuristic(startX, startY, startZ, endX, endY, endZ);
        startNode.f = startNode.h;

        openSet.add(startNode);
        allNodes.put(startNode.key(), startNode);

        Set<String> closedSet = new HashSet<>();
        int nodesExplored = 0;

        while (!openSet.isEmpty() && nodesExplored < MAX_SEARCH_NODES) {
            Node current = openSet.poll();
            nodesExplored++;

            // Check if we reached the goal
            if (current.x == endX && current.y == endY && current.z == endZ) {
                return reconstructPath(current);
            }

            closedSet.add(current.key());

            // Explore neighbors
            for (Node neighbor : getNeighbors(world, current)) {
                if (closedSet.contains(neighbor.key())) {
                    continue;
                }

                float tentativeG = current.g + cost(current, neighbor);

                Node existingNeighbor = allNodes.get(neighbor.key());
                if (existingNeighbor == null) {
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(neighbor.x, neighbor.y, neighbor.z, endX, endY, endZ);
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.parent = current;

                    openSet.add(neighbor);
                    allNodes.put(neighbor.key(), neighbor);
                } else if (tentativeG < existingNeighbor.g) {
                    existingNeighbor.g = tentativeG;
                    existingNeighbor.f = existingNeighbor.g + existingNeighbor.h;
                    existingNeighbor.parent = current;

                    // Re-add to priority queue with updated priority
                    openSet.remove(existingNeighbor);
                    openSet.add(existingNeighbor);
                }
            }
        }

        // No path found
        return null;
    }

    /**
     * Check if a block position is walkable (air above solid ground).
     */
    private boolean isWalkable(World world, int x, int y, int z) {
        // Check if there's a solid block below (ground)
        BlockType below = world.getBlock(x, y - 1, z);
        if (!below.isSolid()) {
            return false;
        }

        // Check if the block at position and above are air (space to walk)
        BlockType atPos = world.getBlock(x, y, z);
        BlockType above = world.getBlock(x, y + 1, z);

        return !atPos.isSolid() && !above.isSolid();
    }

    /**
     * Get walkable neighbors of a node.
     */
    private List<Node> getNeighbors(World world, Node node) {
        List<Node> neighbors = new ArrayList<>();

        // 8 horizontal directions (N, S, E, W, NE, NW, SE, SW)
        int[][] directions = {
            {0, 0, 1},   // North
            {0, 0, -1},  // South
            {1, 0, 0},   // East
            {-1, 0, 0},  // West
            {1, 0, 1},   // NE
            {-1, 0, 1},  // NW
            {1, 0, -1},  // SE
            {-1, 0, -1}  // SW
        };

        for (int[] dir : directions) {
            int nx = node.x + dir[0];
            int ny = node.y + dir[1];
            int nz = node.z + dir[2];

            // Check same level
            if (isWalkable(world, nx, ny, nz)) {
                neighbors.add(new Node(nx, ny, nz));
            }
            // Check one level up (climbing)
            else if (isWalkable(world, nx, ny + 1, nz)) {
                neighbors.add(new Node(nx, ny + 1, nz));
            }
            // Check one level down (descending)
            else if (isWalkable(world, nx, ny - 1, nz)) {
                neighbors.add(new Node(nx, ny - 1, nz));
            }
        }

        return neighbors;
    }

    /**
     * Calculate heuristic (Euclidean distance).
     */
    private float heuristic(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int dz = z2 - z1;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate cost between two adjacent nodes.
     */
    private float cost(Node from, Node to) {
        // Diagonal movement costs more
        int dx = Math.abs(to.x - from.x);
        int dz = Math.abs(to.z - from.z);

        if (dx > 0 && dz > 0) {
            return DIAGONAL_COST;
        }
        return STRAIGHT_COST;
    }

    /**
     * Reconstruct path from end node back to start.
     */
    private List<Vector3> reconstructPath(Node end) {
        List<Vector3> path = new ArrayList<>();
        Node current = end;

        while (current != null) {
            // Center of block
            path.add(new Vector3(current.x + 0.5f, current.y, current.z + 0.5f));
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * A* search node.
     */
    private static class Node {
        int x, y, z;
        float g; // Cost from start
        float h; // Heuristic to end
        float f; // Total cost (g + h)
        Node parent;

        Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        String key() {
            return x + "," + y + "," + z;
        }
    }
}
