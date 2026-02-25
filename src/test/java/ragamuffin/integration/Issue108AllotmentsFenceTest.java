package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.entity.NPCType;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #108:
 * StructureTracker detects world-generated allotments fence as a player structure.
 *
 * Fix: generateAllotments() and buildShed() now use WOOD_FENCE and WOOD_WALL
 * instead of WOOD. StructureTracker only scans for WOOD, so world-generated
 * allotments are no longer treated as player structures.
 */
class Issue108AllotmentsFenceTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private TooltipSystem tooltipSystem;

    // Allotments position is seed-derived; look it up from the landmark at runtime.
    private int ALLOTMENTS_X;
    private int ALLOTMENTS_Z;
    private int ALLOTMENTS_WIDTH;
    private int ALLOTMENTS_DEPTH;

    @BeforeEach
    void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(0);
        world.generate();
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();

        Landmark allotments = world.getLandmark(LandmarkType.ALLOTMENTS);
        assertNotNull(allotments, "ALLOTMENTS landmark must exist after world generation");
        ALLOTMENTS_X = (int) allotments.getPosition().x;
        ALLOTMENTS_Z = (int) allotments.getPosition().z;
        ALLOTMENTS_WIDTH  = allotments.getWidth();
        ALLOTMENTS_DEPTH  = allotments.getDepth();
    }

    /**
     * Test 1: Generate world, force structure scan immediately, verify:
     * - structureTracker.getStructures() is empty
     * - no COUNCIL_BUILDER NPCs spawned
     * - allotments fence blocks are intact (non-AIR)
     */
    @Test
    void worldGeneratedAllotmentsFenceDoesNotTriggerCouncilBuilders() {
        // Force structure scan immediately after world generation
        npcManager.forceStructureScan(world, tooltipSystem);

        // Verify no structures detected from world-generated blocks
        assertEquals(0, npcManager.getStructureTracker().getStructures().size(),
                "World-generated allotments fence should NOT be detected as a player structure");

        // Verify no council builders spawned
        long builderCount = npcManager.getNPCs().stream()
                .filter(npc -> npc.getType() == NPCType.COUNCIL_BUILDER)
                .count();
        assertEquals(0, builderCount,
                "No COUNCIL_BUILDER NPCs should spawn for world-generated allotments fence");

        // Verify allotments fence blocks are still intact (using WOOD_FENCE, not AIR)
        BlockType fenceBlock = world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z);
        assertNotEquals(BlockType.AIR, fenceBlock,
                "Allotments fence blocks should be intact (not AIR) after scan");
        assertEquals(BlockType.WOOD_FENCE, fenceBlock,
                "Allotments perimeter fence should use WOOD_FENCE block type");
    }

    /**
     * Test 2: Place 55 WOOD blocks in a connected cluster, force scan, verify:
     * - exactly one structure detected
     * - a council builder spawned
     * (Fix #646: COUNCIL_BUILDER spawning now requires LARGE_STRUCTURE_THRESHOLD = 50 blocks.)
     */
    @Test
    void playerBuiltWoodStructureStillTriggersCouncilBuilders() {
        // Place 55 WOOD blocks in a connected cluster (5x11x1), far from allotments.
        // Use setPlayerBlock() to mark them as player-placed.
        // Must be >= LARGE_STRUCTURE_THRESHOLD (50) to trigger COUNCIL_BUILDER spawning.
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 12; y++) {
                world.setPlayerBlock(x, y, 10, BlockType.WOOD);
            }
        }
        // That's 5*11 = 55 WOOD blocks — above the LARGE_STRUCTURE_THRESHOLD of 50

        // Force structure scan
        npcManager.forceStructureScan(world, tooltipSystem);

        // Verify exactly one structure detected
        assertEquals(1, npcManager.getStructureTracker().getStructures().size(),
                "Exactly one WOOD structure should be detected");

        // Verify a COUNCIL_BUILDER NPC has been spawned
        long builderCount = npcManager.getNPCs().stream()
                .filter(npc -> npc.getType() == NPCType.COUNCIL_BUILDER)
                .count();
        assertTrue(builderCount >= 1,
                "At least 1 COUNCIL_BUILDER NPC should spawn for a large WOOD structure");
    }

    /**
     * Test 3: Regression — verify neither allotments fence nor sheds are detected
     * as structures at game start. The allotments perimeter (30x20, 2 heights = ~200
     * WOOD_FENCE blocks) and sheds (WOOD_WALL) must not appear in structure list.
     */
    @Test
    void allotmentsFenceAndShedsNotDetectedAsStructures() {
        // Force structure scan
        npcManager.forceStructureScan(world, tooltipSystem);

        // Confirm allotments fence uses WOOD_FENCE (not WOOD)
        // Check multiple fence positions around the perimeter
        // North fence (z = ALLOTMENTS_Z, various x positions)
        assertEquals(BlockType.WOOD_FENCE, world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z),
                "North fence should be WOOD_FENCE");
        assertEquals(BlockType.WOOD_FENCE, world.getBlock(ALLOTMENTS_X + ALLOTMENTS_WIDTH - 1, 1, ALLOTMENTS_Z),
                "North-east corner fence should be WOOD_FENCE");
        // South fence (z = ALLOTMENTS_Z + depth - 1)
        assertEquals(BlockType.WOOD_FENCE, world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z + ALLOTMENTS_DEPTH - 1),
                "South fence should be WOOD_FENCE");

        // Confirm sheds use WOOD_WALL
        // Shed 1 is at (x+2, z+2) = (62, 1, -98), width=2, depth=2.
        // The door is at (x+2, 1, z+2) = (62, 1, -98), so check the adjacent wall at (63, 1, -98).
        assertEquals(BlockType.WOOD_WALL, world.getBlock(ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2),
                "Shed wall should be WOOD_WALL");

        // Structure tracker should have found nothing
        assertEquals(0, npcManager.getStructureTracker().getStructures().size(),
                "No structures should be detected from world-generated allotments");
    }
}
