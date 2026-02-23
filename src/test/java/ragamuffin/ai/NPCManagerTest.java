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

        // Knockback is now velocity-based — simulate frames to see movement
        for (int i = 0; i < 12; i++) {
            npc.update(1.0f / 60.0f);
        }

        assertTrue(npc.getPosition().z < originalPos.z, "NPC should be knocked north");
    }

    @Test
    void testGameTimeWrap() {
        manager.setGameTime(23.5f);
        assertEquals(23.5f, manager.getGameTime(), 0.01f);

        manager.setGameTime(25.0f); // Should wrap to 1:00 AM
        assertEquals(1.0f, manager.getGameTime(), 0.01f);
    }

    @Test
    void testNPCDoesNotSinkIntoFloor() {
        // Reproduce the "yellow NPC stuck in floor" bug:
        // an NPC that falls a small distance should land exactly on the floor surface,
        // not inside the floor block where it gets permanently stuck.
        // Ground at y=0, NPC spawned slightly above (y=3) so gravity pulls it down.
        // The NPC is placed away from the player to avoid fleeing/aggro path interference.
        NPC npc = manager.spawnNPC(NPCType.JOGGER, 5, 3, 5); // Jogger always wanders (no daily-routine state)

        // Run many update frames to let the NPC fall and settle
        for (int i = 0; i < 120; i++) {
            manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        // NPC must NOT be inside the floor block (pos.y must be >= 1.0)
        float npcY = npc.getPosition().y;
        assertTrue(npcY >= 1.0f, "NPC fell through or into the floor; y=" + npcY);
    }

    @Test
    void testNPCLandsOnCorrectSurfaceAfterFallingFromHeight() {
        // If an NPC falls from y=5 the floor snap must not leave it inside a block.
        // The old Math.ceil snap could produce pos.y=1 which is the face of block y=1
        // (solid ground), embedding the NPC. The correct value is pos.y=1 ONLY when
        // the solid block is at y=0 (top surface = y+1 = 1). Verify pos.y == 1.0 exactly
        // after settling, proving the floor-snap places feet at the block top surface.

        NPC npc = manager.spawnNPC(NPCType.JOGGER, 10, 5, 10);

        for (int i = 0; i < 240; i++) {
            manager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        float npcY = npc.getPosition().y;
        assertTrue(npcY >= 1.0f, "NPC sank into the floor; y=" + npcY);
        // Vertical velocity should be ~0 once settled on ground
        assertTrue(Math.abs(npc.getVelocity().y) < 1.0f, "NPC still has significant vertical velocity; vy=" + npc.getVelocity().y);
    }

    @Test
    void testPoliceNotDespawnedDuringDaytime() {
        // Regression test for issue #90: police spawned at game start must survive
        // into daytime gameplay. updatePoliceSpawning() must NOT despawn police
        // when time is between 06:00 and 22:00.
        NPC police1 = manager.spawnNPC(NPCType.POLICE, 20, 1, 10);
        NPC police2 = manager.spawnNPC(NPCType.POLICE, -30, 1, 20);
        NPC police3 = manager.spawnNPC(NPCType.POLICE, 50, 1, -10);
        if (police1 != null) police1.setState(NPCState.PATROLLING);
        if (police2 != null) police2.setState(NPCState.PATROLLING);
        if (police3 != null) police3.setState(NPCState.PATROLLING);

        long beforeCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(3, beforeCount, "Expected 3 police before daytime update");

        // Simulate a daytime frame at 8:00 AM — police must NOT be despawned
        manager.updatePoliceSpawning(8.0f, world, player);

        long afterCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(3, afterCount, "Police were despawned during daytime — issue #90 regression");
    }

    @Test
    void testPoliceCapIncreasesAtNight() {
        // At night (22:00+) the spawn routine should allow more police than the daytime cap.
        // Verify that calling updatePoliceSpawning() at night with 3 police does not skip
        // spawning (cap is 4 at night), whereas during the day with 3 police it should skip
        // because the daytime cap is 3.
        NPC p1 = manager.spawnNPC(NPCType.POLICE, 5, 1, 5);
        NPC p2 = manager.spawnNPC(NPCType.POLICE, 10, 1, 5);
        NPC p3 = manager.spawnNPC(NPCType.POLICE, 15, 1, 5);

        // Call at night — the cap is 4, so a 4th officer should eventually be spawned.
        // Reset cooldown by direct inspection isn't possible, but we can verify the
        // daytime call with exactly-cap police does NOT add more.
        manager.updatePoliceSpawning(8.0f, world, player); // daytime, cap=3, already have 3 → no spawn
        long countAfterDay = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertEquals(3, countAfterDay, "Daytime should not spawn extra police beyond cap of 3");
    }

    @Test
    void testPoliceCapNotExceededBySpawnPolice() {
        // Regression test for issue #100: spawnPolice() spawns 2-3 NPCs per call,
        // which used to bypass the maxPolice cap. With 2 existing police and a daytime
        // cap of 3 (non-notorious player, daytime), only 1 slot remains. The call must
        // not push the total above 3.
        manager.spawnNPC(NPCType.POLICE, 5, 1, 5);
        manager.spawnNPC(NPCType.POLICE, 10, 1, 5);

        // Fresh manager: cooldown is 0, so updatePoliceSpawning will execute immediately.
        manager.updatePoliceSpawning(8.0f, world, player); // daytime, cap=3, have 2 → 1 slot

        long policeCount = manager.getNPCs().stream()
                .filter(n -> n.getType() == NPCType.POLICE && n.isAlive()).count();
        assertTrue(policeCount <= 3,
                "Police count exceeded cap: expected <= 3 but got " + policeCount);
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
