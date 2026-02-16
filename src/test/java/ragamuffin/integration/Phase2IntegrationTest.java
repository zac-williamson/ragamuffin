package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.Player;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 Integration Tests: World Generation
 * These tests verify that the procedurally generated British town works end-to-end.
 */
public class Phase2IntegrationTest {

    private World world;
    private Player player;

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
        // Generate a new world centered on (0, 0, 0) - the park will be here
        world = new World(0);
        world.generate();
        player = new Player(0, 5, 0); // Start at world center
    }

    /**
     * Integration Test 1: Park exists at world centre
     *
     * Generate the world. Sample a 20x20 block area at the world centre.
     * Verify at least 60% of ground-level blocks are GRASS. Verify at least
     * 2 tree structures exist (a TREE_TRUNK block with LEAVES blocks above it)
     * within the park area. Verify at least 1 WATER block exists (the pond).
     */
    @Test
    public void testParkExistsAtWorldCentre() {
        // Sample 20x20 area at world center (centered on 0,0)
        int sampleSize = 20;
        int startX = -sampleSize / 2;
        int startZ = -sampleSize / 2;
        int grassCount = 0;
        int totalGroundBlocks = 0;
        int treeCount = 0;
        int waterCount = 0;

        for (int x = startX; x < startX + sampleSize; x++) {
            for (int z = startZ; z < startZ + sampleSize; z++) {
                BlockType groundBlock = world.getBlock(x, 0, z);
                totalGroundBlocks++;

                if (groundBlock == BlockType.GRASS) {
                    grassCount++;
                }

                // Check for water blocks
                for (int y = 0; y <= 3; y++) {
                    if (world.getBlock(x, y, z) == BlockType.WATER) {
                        waterCount++;
                        break;
                    }
                }

                // Check for tree structures: TREE_TRUNK with LEAVES above
                if (world.getBlock(x, 0, z) == BlockType.TREE_TRUNK ||
                    world.getBlock(x, 1, z) == BlockType.TREE_TRUNK) {
                    int trunkY = world.getBlock(x, 0, z) == BlockType.TREE_TRUNK ? 0 : 1;
                    boolean hasLeaves = false;
                    for (int y = trunkY + 1; y <= trunkY + 5; y++) {
                        if (world.getBlock(x, y, z) == BlockType.LEAVES) {
                            hasLeaves = true;
                            break;
                        }
                    }
                    if (hasLeaves) {
                        treeCount++;
                    }
                }
            }
        }

        // Verify at least 60% grass
        double grassPercent = (double) grassCount / totalGroundBlocks;
        assertTrue(grassPercent >= 0.6,
            String.format("Park should have at least 60%% grass, but has %.1f%%", grassPercent * 100));

        // Verify at least 2 trees
        assertTrue(treeCount >= 2,
            String.format("Park should have at least 2 trees, but has %d", treeCount));

        // Verify at least 1 water block (pond)
        assertTrue(waterCount >= 1,
            String.format("Park should have at least 1 water block (pond), but has %d", waterCount));
    }

    /**
     * Integration Test 2: Buildings are solid and enclosed
     *
     * Generate the world. Find the office building location. Verify that walking
     * into the building's exterior wall blocks (BRICK or GLASS) prevents player
     * movement — place the player 1 block outside the wall, simulate pressing W
     * for 60 frames, verify the player has not passed through the wall.
     */
    @Test
    public void testBuildingsAreSolidAndEnclosed() {
        // Find the office building
        Landmark officeBuilding = world.getLandmark(LandmarkType.OFFICE_BUILDING);
        assertNotNull(officeBuilding, "Office building should exist in the world");

        // Find an exterior wall of the building
        Vector3 buildingPos = officeBuilding.getPosition();
        int buildingX = (int) buildingPos.x;
        int buildingZ = (int) buildingPos.z;

        // Find the southern wall (front of building)
        int wallZ = buildingZ;
        int wallX = buildingX;

        // Verify there's a solid block at the wall
        BlockType wallBlock = world.getBlock(wallX, 1, wallZ);
        assertTrue(wallBlock == BlockType.BRICK || wallBlock == BlockType.GLASS,
            "Building wall should be BRICK or GLASS, but is " + wallBlock);

        // Place player 1 block south of the wall, facing north (toward the wall)
        player = new Player(wallX, 1, wallZ + 2);
        float startZ = player.getPosition().z;

        // Simulate pressing W for 60 frames (1 second at 60fps)
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            // Move forward (north, negative Z direction)
            world.moveWithCollision(player, 0, 0, -1, delta);
        }

        // Verify player has not passed through the wall
        // Player should be stopped at least 1 block away from the wall center
        float finalZ = player.getPosition().z;
        assertTrue(finalZ >= wallZ + 1.0f - Player.DEPTH / 2,
            String.format("Player should not pass through wall at z=%d, but moved from %.2f to %.2f",
                wallZ, startZ, finalZ));
    }

    /**
     * Integration Test 3: Key landmarks all exist
     *
     * Generate the world. Verify each of the following landmarks exists by finding
     * at least one block tagged/associated with it: park, Greggs, off-licence,
     * charity shop, jeweller, office building, JobCentre. Each landmark must be
     * at a unique position (no two landmarks overlap).
     */
    @Test
    public void testKeyLandmarksAllExist() {
        LandmarkType[] requiredLandmarks = {
            LandmarkType.PARK,
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.OFFICE_BUILDING,
            LandmarkType.JOB_CENTRE
        };

        Set<Vector3> landmarkPositions = new HashSet<>();

        for (LandmarkType type : requiredLandmarks) {
            Landmark landmark = world.getLandmark(type);
            assertNotNull(landmark, "Landmark " + type + " should exist in the world");

            Vector3 pos = landmark.getPosition();
            assertFalse(landmarkPositions.contains(pos),
                "Landmark " + type + " overlaps with another landmark at " + pos);

            landmarkPositions.add(pos);
        }

        // Verify we have all unique positions
        assertEquals(requiredLandmarks.length, landmarkPositions.size(),
            "All landmarks should be at unique positions");
    }

    /**
     * Integration Test 4: Chunks load and unload
     *
     * Place the player at the world centre. Record which chunks are loaded.
     * Move the player 100 blocks north (simulate movement over multiple frames).
     * Verify that new chunks near the player are now loaded, and chunks far behind
     * the player (beyond render distance) have been unloaded.
     */
    @Test
    public void testChunksLoadAndUnload() {
        // Start at world center
        player = new Player(0, 5, 0);
        world.updateLoadedChunks(player.getPosition());

        // Record initially loaded chunks
        Set<String> initialChunks = new HashSet<>(world.getLoadedChunkKeys());
        assertTrue(initialChunks.size() > 0, "Some chunks should be loaded initially");

        // Check that chunk at (0,0,0) is loaded
        assertTrue(world.isChunkLoaded(0, 0, 0), "Chunk at origin should be loaded");

        // Move player 100 blocks north (negative Z)
        player = new Player(0, 5, -100);
        world.updateLoadedChunks(player.getPosition());

        // Record chunks after movement
        Set<String> finalChunks = new HashSet<>(world.getLoadedChunkKeys());

        // Verify new chunks near player are loaded
        // Player is at Z=-100, so chunk around (0, 0, -6) should be loaded
        int playerChunkZ = -100 / Chunk.SIZE;
        assertTrue(world.isChunkLoaded(0, 0, playerChunkZ) ||
                   world.isChunkLoaded(0, 0, playerChunkZ - 1),
            "Chunk near player's new position should be loaded");

        // Verify old chunks far behind are unloaded (chunk at origin should be unloaded)
        // Unless render distance is very large, chunk (0,0,0) should be unloaded
        if (world.getRenderDistance() < 7) { // 7 chunks * 16 = 112 blocks
            assertFalse(world.isChunkLoaded(0, 0, 0),
                "Chunk at origin should be unloaded when player is 100 blocks away");
        }

        // At minimum, verify that the set of loaded chunks has changed
        assertNotEquals(initialChunks, finalChunks,
            "Loaded chunks should change when player moves far away");
    }

    /**
     * Integration Test 5: Streets connect landmarks
     *
     * Generate the world. Pick two landmark positions (e.g. park and Greggs).
     * Verify there exists a continuous path of PAVEMENT or ROAD blocks at ground
     * level between them (BFS/flood fill on walkable blocks).
     */
    @Test
    public void testStreetsConnectLandmarks() {
        Landmark park = world.getLandmark(LandmarkType.PARK);
        Landmark greggs = world.getLandmark(LandmarkType.GREGGS);

        assertNotNull(park, "Park should exist");
        assertNotNull(greggs, "Greggs should exist");

        Vector3 startPos = park.getPosition();
        Vector3 endPos = greggs.getPosition();

        // BFS to find path of PAVEMENT or ROAD blocks
        boolean pathExists = findPath(
            (int) startPos.x, (int) startPos.z,
            (int) endPos.x, (int) endPos.z
        );

        assertTrue(pathExists,
            String.format("Should have continuous path of PAVEMENT/ROAD from park at (%.0f,%.0f) to Greggs at (%.0f,%.0f)",
                startPos.x, startPos.z, endPos.x, endPos.z));
    }

    /**
     * BFS pathfinding to verify street connectivity.
     * Returns true if a path of PAVEMENT or ROAD blocks exists.
     */
    private boolean findPath(int startX, int startZ, int endX, int endZ) {
        Queue<int[]> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(new int[]{startX, startZ});
        visited.add(startX + "," + startZ);

        // BFS with max search radius to avoid infinite loops
        int maxDistance = 200; // World is ~200x200

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int z = current[1];

            // Check if we reached the end
            if (Math.abs(x - endX) <= 5 && Math.abs(z - endZ) <= 5) {
                return true;
            }

            // Don't search too far
            if (Math.abs(x - startX) > maxDistance || Math.abs(z - startZ) > maxDistance) {
                continue;
            }

            // Check 4 neighbors
            int[][] neighbors = {{x+1, z}, {x-1, z}, {x, z+1}, {x, z-1}};

            for (int[] neighbor : neighbors) {
                int nx = neighbor[0];
                int nz = neighbor[1];
                String key = nx + "," + nz;

                if (visited.contains(key)) {
                    continue;
                }

                BlockType block = world.getBlock(nx, 0, nz);

                // Walkable blocks: PAVEMENT, ROAD, or GRASS (park)
                if (block == BlockType.PAVEMENT ||
                    block == BlockType.ROAD ||
                    block == BlockType.GRASS) {

                    visited.add(key);
                    queue.offer(new int[]{nx, nz});
                }
            }
        }

        return false; // No path found
    }
}
