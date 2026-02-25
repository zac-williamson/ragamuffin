package ragamuffin.building;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.*;

/**
 * Tracks player-built structures for council detection.
 * A structure is a connected group of player-placeable blocks (WOOD, BRICK, STONE,
 * GLASS, CARDBOARD, CONCRETE, ROOF_TILE, CORRUGATED_METAL, DOOR_WOOD, and others).
 */
public class StructureTracker {

    /**
     * Represents a detected structure.
     */
    public static class Structure {
        private final Set<Vector3> blocks;
        private final Vector3 center;
        private int complexity;
        private float noticeTime; // Time when planning notice appeared
        private boolean hasNotice;

        public Structure(Set<Vector3> blocks) {
            this.blocks = new HashSet<>(blocks);
            this.center = calculateCenter(blocks);
            this.complexity = blocks.size();
            this.noticeTime = 0;
            this.hasNotice = false;
        }

        private Vector3 calculateCenter(Set<Vector3> blocks) {
            float sumX = 0, sumY = 0, sumZ = 0;
            for (Vector3 block : blocks) {
                sumX += block.x;
                sumY += block.y;
                sumZ += block.z;
            }
            int count = blocks.size();
            return new Vector3(sumX / count, sumY / count, sumZ / count);
        }

        public Set<Vector3> getBlocks() {
            return blocks;
        }

        public Vector3 getCenter() {
            return center;
        }

        public int getComplexity() {
            return complexity;
        }

        public void setComplexity(int complexity) {
            this.complexity = complexity;
        }

        public float getNoticeTime() {
            return noticeTime;
        }

        public void setNoticeTime(float time) {
            this.noticeTime = time;
        }

        public boolean hasNotice() {
            return hasNotice;
        }

        public void setHasNotice(boolean hasNotice) {
            this.hasNotice = hasNotice;
        }

        public void removeBlock(Vector3 block) {
            blocks.remove(block);
            complexity = blocks.size();
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    private final List<Structure> structures;
    private static final int SMALL_STRUCTURE_THRESHOLD = 10; // Blocks
    private static final int LARGE_STRUCTURE_THRESHOLD = 50; // Blocks for more builders

    public StructureTracker() {
        this.structures = new ArrayList<>();
    }

    /**
     * Scan the world for player-built structures.
     * This is called periodically to detect new structures.
     */
    public void scanForStructures(World world) {
        structures.clear();

        Set<String> visited = new HashSet<>();

        // Scan a reasonable area around world center
        for (int x = -100; x <= 100; x++) {
            for (int z = -100; z <= 100; z++) {
                for (int y = 1; y < 20; y++) {
                    String key = x + "," + y + "," + z;
                    if (visited.contains(key)) {
                        continue;
                    }

                    BlockType block = world.getBlock(x, y, z);
                    if (block.isPlayerPlaceable() && world.isPlayerPlaced(x, y, z)) {
                        // Found a player-placed block - trace the structure
                        Set<Vector3> structureBlocks = traceStructure(world, x, y, z, visited);
                        if (structureBlocks.size() >= SMALL_STRUCTURE_THRESHOLD) {
                            structures.add(new Structure(structureBlocks));
                        }
                    }
                }
            }
        }
    }

    /**
     * Trace a connected structure from a starting block using flood fill.
     */
    private Set<Vector3> traceStructure(World world, int startX, int startY, int startZ, Set<String> visited) {
        Set<Vector3> structure = new HashSet<>();
        Queue<Vector3> queue = new LinkedList<>();
        queue.add(new Vector3(startX, startY, startZ));
        visited.add(startX + "," + startY + "," + startZ);

        while (!queue.isEmpty() && structure.size() < 500) { // Limit to prevent infinite loops
            Vector3 current = queue.poll();
            structure.add(current);

            // Check 6 adjacent blocks
            int[][] offsets = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
            for (int[] offset : offsets) {
                int x = (int)current.x + offset[0];
                int y = (int)current.y + offset[1];
                int z = (int)current.z + offset[2];
                String key = x + "," + y + "," + z;

                if (!visited.contains(key)) {
                    visited.add(key);
                    BlockType block = world.getBlock(x, y, z);
                    if (block.isPlayerPlaceable() && world.isPlayerPlaced(x, y, z)) {
                        queue.add(new Vector3(x, y, z));
                    }
                }
            }
        }

        return structure;
    }

    /**
     * Get all detected structures.
     */
    public List<Structure> getStructures() {
        return structures;
    }

    /**
     * Get structures that are large enough to trigger council builders.
     */
    public List<Structure> getLargeStructures() {
        List<Structure> large = new ArrayList<>();
        for (Structure s : structures) {
            if (s.getComplexity() >= LARGE_STRUCTURE_THRESHOLD) {
                large.add(s);
            }
        }
        return large;
    }

    /**
     * Calculate how many builders should spawn for a structure.
     */
    public int calculateBuilderCount(Structure structure) {
        int complexity = structure.getComplexity();
        if (complexity < SMALL_STRUCTURE_THRESHOLD) {
            return 0;
        } else if (complexity < LARGE_STRUCTURE_THRESHOLD) {
            return 1;
        } else {
            // More builders for larger structures
            return 1 + (complexity - LARGE_STRUCTURE_THRESHOLD) / 50;
        }
    }

    /**
     * Remove a block from structures after demolition.
     */
    public void removeBlock(int x, int y, int z) {
        Vector3 blockPos = new Vector3(x, y, z);
        for (Structure structure : structures) {
            structure.removeBlock(blockPos);
        }
        // Remove empty structures
        structures.removeIf(Structure::isEmpty);
    }
}
