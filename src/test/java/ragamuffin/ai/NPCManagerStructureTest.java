package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test NPCManager's structure tracking and council builder spawning.
 */
class NPCManagerStructureTest {

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

        // Create ground
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    @Test
    void testStructureScanning() {
        // Build a large player-placed structure (setPlayerBlock marks blocks as
        // player-placed so StructureTracker.scanForStructures() can detect them)
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 10; z < 15; z++) {
                    world.setPlayerBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        // Update for more than 30 seconds to trigger scan
        for (int i = 0; i < 1850; i++) {
            npcManager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        // Check if planning notice was applied
        boolean noticeFound = false;
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 10; z < 15; z++) {
                    if (world.hasPlanningNotice(x, y, z)) {
                        noticeFound = true;
                        System.out.println("Found notice at " + x + "," + y + "," + z);
                        break;
                    }
                }
                if (noticeFound) break;
            }
            if (noticeFound) break;
        }

        int structureCount = npcManager.getStructureTracker().getLargeStructures().size();
        System.out.println("Structures found: " + structureCount);

        assertTrue(noticeFound,
                "Planning notice should be applied after structure scan");
    }

    /**
     * Regression test for Issue #145: notifiedStructures used Set<Vector3> which relies on
     * reference equality, causing the same structure to be re-notified every 30-second scan
     * cycle and spawning COUNCIL_BUILDER NPCs indefinitely.
     *
     * After the fix (using Set<String> with "x,y,z" keys), each structure should only receive
     * one planning notice, and council builder count should not grow unboundedly across scans.
     */
    @Test
    void testIssue145_structureNotifiedOnlyOnceAcrossMultipleScans() {
        // Build a large structure that triggers the council builder system
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 10; z < 15; z++) {
                    world.setBlock(x, y, z, BlockType.WOOD);
                }
            }
        }

        // Use forceStructureScan to simulate 5 scan cycles without running 5550 update frames.
        // Each call is equivalent to one 30-second scan cycle.
        for (int cycle = 0; cycle < 5; cycle++) {
            npcManager.forceStructureScan(world, tooltipSystem);
        }

        // Count planning notices applied to the structure — should be at most 3
        // (applyPlanningNotice caps at 3 blocks per call, and it must only be called once)
        int noticeCount = 0;
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 10; z < 15; z++) {
                    if (world.hasPlanningNotice(x, y, z)) {
                        noticeCount++;
                    }
                }
            }
        }

        // With the bug, each of the 5 scan cycles would re-apply up to 3 notices.
        // With the fix, planning notices are only added once, so total must be <= 3.
        assertTrue(noticeCount <= 3,
                "Planning notice should only be applied once — found " + noticeCount +
                " notice blocks across 5 scans (suggests repeated notifications, bug #145 not fixed)");

        // Council builders should not grow unboundedly. calculateBuilderCount returns at most 3
        // for a large structure, so 5 scan cycles with the bug would produce 15+ builders.
        long builderCount = npcManager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.COUNCIL_BUILDER && n.isAlive())
                .count();
        assertTrue(builderCount <= 5,
                "Council builder count should not grow unboundedly — found " + builderCount +
                " builders after 5 scan cycles (suggests infinite spawning, bug #145 not fixed)");
    }

    /**
     * Regression test for Issue #168: applyPoliceTapeToStructure previously only taped
     * WOOD blocks. BRICK (and other player-placed materials) must also receive police tape.
     */
    @Test
    void testIssue168_applyPoliceTapeToBrickStructure() {
        // Build a 5-block BRICK structure
        int cx = 20, cy = 1, cz = 20;
        for (int i = 0; i < 5; i++) {
            world.setBlock(cx + i, cy, cz, BlockType.BRICK);
        }

        // Trigger applyPoliceTapeToStructure via the test helper, centered on the structure
        npcManager.forceApplyPoliceTape(world, new Vector3(cx + 2, cy, cz));

        // At least one BRICK block must now have police tape
        boolean tapeFound = false;
        for (int i = 0; i < 5; i++) {
            if (world.hasPoliceTape(cx + i, cy, cz)) {
                tapeFound = true;
                break;
            }
        }
        assertTrue(tapeFound,
                "Police tape should be applied to at least one BRICK block (issue #168 not fixed)");
    }
}
