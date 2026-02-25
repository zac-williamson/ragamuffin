package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Bug Fix #44:
 * StructureTracker scans BRICK blocks, causing council builders to demolish
 * world-generated buildings.
 *
 * Fix: World tracks player-placed block positions via setPlayerBlock(). Only
 * blocks recorded as player-placed are considered for structure detection, so
 * world-generated BRICK buildings are never treated as player structures.
 * Issue #156 extended detection to all player-placeable materials (BRICK, STONE,
 * GLASS, CARDBOARD, CONCRETE, etc.) — the player-placed tracking ensures
 * world-gen buildings with those materials are still excluded.
 */
class BugFix044IntegrationTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Create a flat ground
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Integration Test 1: Council builders do NOT demolish world-generated buildings.
     * Generate the world. Do NOT place any blocks. Call forceStructureScan.
     * Verify no structures are detected and no COUNCIL_BUILDER NPCs are spawned.
     *
     * This specifically tests that BRICK blocks from world generation are NOT
     * detected as player-built structures.
     */
    @Test
    void worldGeneratedBrickDoesNotTriggerCouncilBuilders() {
        // Place a cluster of BRICK blocks simulating a world-generated building
        // (15 connected BRICK blocks, above the 10-block threshold)
        for (int x = 10; x < 13; x++) {
            for (int y = 1; y < 4; y++) {
                for (int z = 10; z < 12; z++) {
                    world.setBlock(x, y, z, BlockType.BRICK);
                }
            }
        }
        // That's 3*3*2 = 18 BRICK blocks — above the 10-block threshold

        // Force structure scan
        npcManager.forceStructureScan(world, tooltipSystem);

        // Verify no structures detected
        assertEquals(0, npcManager.getStructureTracker().getStructures().size(),
                "BRICK blocks should NOT be detected as player-built structures");

        // Verify no council builders spawned
        long builderCount = npcManager.getNPCs().stream()
                .filter(npc -> npc.getType() == NPCType.COUNCIL_BUILDER)
                .count();

        assertEquals(0, builderCount,
                "No COUNCIL_BUILDER NPCs should spawn for world-generated BRICK buildings");
    }

    /**
     * Integration Test 2: Council builders DO detect player-built WOOD structures.
     * Place 55 WOOD blocks (as player-placed) in a connected cluster at (10, 1, 10).
     * Force scan. Verify exactly one structure is detected and a COUNCIL_BUILDER has spawned.
     * (Fix #646: COUNCIL_BUILDER spawning now requires LARGE_STRUCTURE_THRESHOLD = 50 blocks.)
     */
    @Test
    void playerBuiltWoodStructureTriggersCouncilBuilders() {
        // Place 55 WOOD blocks in a connected cluster (5x11x1) as player-placed —
        // must be >= LARGE_STRUCTURE_THRESHOLD (50) to trigger COUNCIL_BUILDER spawning.
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
}
