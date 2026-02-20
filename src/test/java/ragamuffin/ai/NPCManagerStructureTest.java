package ragamuffin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
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
        // Build a large structure
        for (int x = 10; x < 15; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 10; z < 15; z++) {
                    world.setBlock(x, y, z, BlockType.WOOD);
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
}
