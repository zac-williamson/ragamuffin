package ragamuffin.ai;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NPCManager.
 */
class NPCManagerTest {

    private NPCManager manager;
    private World world;
    private Player player;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        manager = new NPCManager();
        world = new World(12345);
        player = new Player(0, 1, 0);
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
    void testSpawnNPC() {
        assertEquals(0, manager.getNPCs().size());

        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        assertNotNull(npc);
        assertEquals(1, manager.getNPCs().size());
        assertEquals(NPCType.PUBLIC, npc.getType());
        assertEquals(10, npc.getPosition().x, 0.01f);
    }

    @Test
    void testSpawnMultipleNPCs() {
        manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);
        manager.spawnNPC(NPCType.DOG, 15, 1, 15);
        manager.spawnNPC(NPCType.YOUTH_GANG, 20, 1, 20);

        assertEquals(3, manager.getNPCs().size());
    }

    @Test
    void testRemoveNPC() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);
        assertEquals(1, manager.getNPCs().size());

        manager.removeNPC(npc);
        assertEquals(0, manager.getNPCs().size());
    }

    @Test
    void testDailyRoutineWorkHours() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(8.0f); // 8:00 AM
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.GOING_TO_WORK, npc.getState());

        manager.setGameTime(12.0f); // Noon
        // State should still be work-related unless interrupted
        assertTrue(npc.getState() == NPCState.GOING_TO_WORK ||
                   npc.getState() == NPCState.WANDERING); // May be wandering during work
    }

    @Test
    void testDailyRoutineEvening() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(17.0f); // 5:00 PM
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        assertEquals(NPCState.GOING_HOME, npc.getState());
    }

    @Test
    void testDailyRoutineNight() {
        NPC npc = manager.spawnNPC(NPCType.PUBLIC, 10, 1, 10);

        manager.setGameTime(20.0f); // 8:00 PM
        manager.update(0.1f, world, player, inventory, tooltipSystem);

        assertTrue(npc.getState() == NPCState.AT_PUB || npc.getState() == NPCState.AT_HOME);
    }

    @Test
    void testDogsStartWandering() {
        NPC dog = manager.spawnNPC(NPCType.DOG, 0, 1, 0);

        assertEquals(NPCState.WANDERING, dog.getState());
    }

    @Test
    void testPunchNPC() {
        NPC npc = manager.spawnNPC(NPCType.COUNCIL_MEMBER, 20, 1, 20);
        Vector3 originalPos = new Vector3(npc.getPosition());

        Vector3 punchDir = new Vector3(0, 0, -1); // North
        manager.punchNPC(npc, punchDir);

        assertTrue(npc.getPosition().z < originalPos.z); // Knocked back north
        float knockbackDistance = originalPos.dst(npc.getPosition());
        assertTrue(knockbackDistance >= 1.8f); // At least ~2 blocks
    }

    @Test
    void testGameTimeWrap() {
        manager.setGameTime(23.5f);
        assertEquals(23.5f, manager.getGameTime(), 0.01f);

        manager.setGameTime(25.0f); // Should wrap to 1:00 AM
        assertEquals(1.0f, manager.getGameTime(), 0.01f);
    }

    @Test
    void testYouthGangStealing() {
        NPC youth = manager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1, 0.5f);
        player.getPosition().set(0, 1, 0);
        inventory.addItem(Material.WOOD, 5);

        assertEquals(5, inventory.getItemCount(Material.WOOD));

        // Update until youth is adjacent and steals
        for (int i = 0; i < 100; i++) {
            manager.update(0.1f, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.WOOD) < 5) {
                break;
            }
        }

        // Should have stolen at least 1 wood
        assertTrue(inventory.getItemCount(Material.WOOD) < 5);
    }
}
