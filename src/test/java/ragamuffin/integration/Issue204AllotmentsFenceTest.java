package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockDropTable;
import ragamuffin.building.Material;
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
 * Integration tests for Issue #204:
 * StructureTracker detecting world-generated allotments fence as player structure.
 *
 * Fix: generateAllotments() uses WOOD_FENCE and buildShed() uses WOOD_WALL —
 * both have isPlayerPlaceable()=false, so StructureTracker ignores them.
 */
class Issue204AllotmentsFenceTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private TooltipSystem tooltipSystem;

    // Allotments position is seed-derived; look it up from the landmark at runtime.
    private int ALLOTMENTS_X;
    private int ALLOTMENTS_Z;

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
    }

    /**
     * Test 1: Council builders do NOT demolish allotments fence.
     * Generate the world. Do NOT place any player blocks. Force structure scan.
     * Verify structureTracker.getStructures() is empty, no COUNCIL_BUILDER NPCs
     * have been spawned, and allotments fence blocks are non-AIR.
     */
    @Test
    void councilBuildersDoNotDemolishAllotmentsFence() {
        // Force scan without placing any player blocks
        npcManager.forceStructureScan(world, tooltipSystem);

        // No structures should be detected from world-generated allotments
        assertEquals(0, npcManager.getStructureTracker().getStructures().size(),
                "World-generated allotments fence should NOT be detected as a player structure");

        // No council builders should have been spawned
        long builderCount = npcManager.getNPCs().stream()
                .filter(npc -> npc.getType() == NPCType.COUNCIL_BUILDER)
                .count();
        assertEquals(0, builderCount,
                "No COUNCIL_BUILDER NPCs should spawn for world-generated allotments fence");

        // Allotments fence blocks must still be present (non-AIR)
        BlockType fenceBlock = world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z);
        assertNotEquals(BlockType.AIR, fenceBlock,
                "Allotments fence block at (" + ALLOTMENTS_X + ",1," + ALLOTMENTS_Z + ") should be non-AIR");
        assertEquals(BlockType.WOOD_FENCE, fenceBlock,
                "Allotments perimeter fence should use WOOD_FENCE block type");
    }

    /**
     * Test 2: Player-placed WOOD IS still detected.
     * Place 15 WOOD blocks in a connected cluster. Force scan.
     * Verify exactly one structure is detected and a COUNCIL_BUILDER has spawned.
     */
    @Test
    void playerPlacedWoodIsStillDetected() {
        // Place 15 WOOD blocks (3x5x1) as player-placed, far from allotments
        for (int x = 10; x < 13; x++) {
            for (int y = 1; y < 6; y++) {
                world.setPlayerBlock(x, y, 10, BlockType.WOOD);
            }
        }
        // 3*5 = 15 blocks — above the SMALL_STRUCTURE_THRESHOLD of 10

        npcManager.forceStructureScan(world, tooltipSystem);

        // Exactly one structure should be detected
        assertEquals(1, npcManager.getStructureTracker().getStructures().size(),
                "Exactly one player-placed WOOD structure should be detected");

        // At least one COUNCIL_BUILDER should have been spawned
        long builderCount = npcManager.getNPCs().stream()
                .filter(npc -> npc.getType() == NPCType.COUNCIL_BUILDER)
                .count();
        assertTrue(builderCount >= 1,
                "At least 1 COUNCIL_BUILDER NPC should spawn for a large player-placed WOOD structure");
    }

    /**
     * Test 3: Regression — sheds and fences not detected at game start.
     * Generate world. Force structure scan immediately.
     * Verify structureTracker.getStructures() is empty.
     */
    @Test
    void shedsAndFencesNotDetectedAtGameStart() {
        npcManager.forceStructureScan(world, tooltipSystem);

        assertEquals(0, npcManager.getStructureTracker().getStructures().size(),
                "No structures should be detected from world-generated allotments (sheds and fences)");

        // Confirm the fence uses WOOD_FENCE (not WOOD)
        assertEquals(BlockType.WOOD_FENCE, world.getBlock(ALLOTMENTS_X, 1, ALLOTMENTS_Z),
                "Allotments north fence corner should be WOOD_FENCE");

        // Confirm shed uses WOOD_WALL — shed1 is at (x+2, z+2), door at (x+2,1,z+2),
        // so adjacent wall at (x+3, 1, z+2) = (63, 1, -98)
        assertEquals(BlockType.WOOD_WALL, world.getBlock(ALLOTMENTS_X + 3, 1, ALLOTMENTS_Z + 2),
                "Allotments shed wall should be WOOD_WALL");
    }

    /**
     * Test 4: WOOD_FENCE drops WOOD material when broken.
     */
    @Test
    void woodFenceDropsWoodMaterial() {
        BlockDropTable dropTable = new BlockDropTable();
        Material drop = dropTable.getDrop(BlockType.WOOD_FENCE, null);
        assertEquals(Material.WOOD, drop,
                "WOOD_FENCE should drop WOOD material when broken");
    }

    /**
     * Test 5: WOOD_WALL drops WOOD material when broken.
     */
    @Test
    void woodWallDropsWoodMaterial() {
        BlockDropTable dropTable = new BlockDropTable();
        Material drop = dropTable.getDrop(BlockType.WOOD_WALL, null);
        assertEquals(Material.WOOD, drop,
                "WOOD_WALL should drop WOOD material when broken");
    }

    /**
     * Test 6: WOOD_FENCE and WOOD_WALL are not player-placeable.
     */
    @Test
    void woodFenceAndWoodWallAreNotPlayerPlaceable() {
        assertFalse(BlockType.WOOD_FENCE.isPlayerPlaceable(),
                "WOOD_FENCE should not be player-placeable");
        assertFalse(BlockType.WOOD_WALL.isPlayerPlaceable(),
                "WOOD_WALL should not be player-placeable");
    }
}
