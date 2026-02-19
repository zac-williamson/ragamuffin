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

        // Fast path: if start == end (already there), return trivial path
        if (startX == endX && startY == endY && startZ == endZ) {
            List<Vector3> trivial = new ArrayList<>();
            trivial.add(new Vector3(endX + 0.5f, endY, endZ + 0.5f));
            return trivial;
        }

        // Fast path: line-of-sight check for short distances on flat ground
        // If the two points are within 20 blocks and have clear straight-line walkability, skip A*
        int dx = Math.abs(endX - startX);
        int dz = Math.abs(endZ - startZ);
        if (Math.abs(endY - startY) <= 1 && dx + dz <= 20) {
            List<Vector3> losPath = tryLineOfSightPath(world, startX, startY, startZ, endX, endY, endZ);
            if (losPath != null) {
                return losPath;
            }
        }

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
     * Attempt to find a direct line-of-sight path between two nearby points.
     * Uses Bresenham's line algorithm to check each block along the line.
     * Returns the path if clear, or null if any block is not walkable.
     */
    private List<Vector3> tryLineOfSightPath(World world, int x0, int y0, int z0, int x1, int y1, int z1) {
        List<Vector3> path = new ArrayList<>();
        path.add(new Vector3(x0 + 0.5f, y0, z0 + 0.5f));

        // Bresenham's line in XZ, checking walkability at each step
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;
        int cx = x0;
        int cz = z0;
        int cy = y0;

        while (cx != x1 || cz != z1) {
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                cx += sx;
            } else {
                err += dx;
                cz += sz;
            }

            // Allow one step up or down
            if (!isWalkable(world, cx, cy, cz)) {
                if (isWalkable(world, cx, cy + 1, cz)) {
                    cy += 1;
                } else if (isWalkable(world, cx, cy - 1, cz)) {
                    cy -= 1;
                } else {
                    return null; // Obstacle in the way
                }
            }

            path.add(new Vector3(cx + 0.5f, cy, cz + 0.5f));
        }

        return path;
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
