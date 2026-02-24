package ragamuffin.render;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.test.HeadlessTestHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NPC visibility and spawning - verifies NPCs are spawned near player
 * and that the rendering system is wired correctly.
 * Note: Actual GL model creation can't be tested in headless mode,
 * so we verify spawn positions and NPC manager wiring.
 *
 * The game uses LibGDX's native 3D rendering (ModelBatch/ModelInstance) with
 * procedurally generated box-primitive body parts. All NPC types including the
 * council worker (COUNCIL_BUILDER) render as articulated 3D humanoids.
 */
class NPCRendererTest {

    @BeforeAll
    static void setup() {
        HeadlessTestHelper.initHeadless();
    }

    @Test
    void initialNPCsSpawnNearPlayerStart() {
        // Verify that spawning NPCs at the designated positions puts them within 10 blocks of origin
        NPCManager manager = new NPCManager();
        manager.spawnNPC(NPCType.PUBLIC, -4, 2, 5);
        manager.spawnNPC(NPCType.DOG, -2, 2, 5);
        manager.spawnNPC(NPCType.YOUTH_GANG, 0, 2, 5);
        manager.spawnNPC(NPCType.COUNCIL_MEMBER, 2, 2, 5);
        manager.spawnNPC(NPCType.POLICE, 4, 2, 5);

        List<NPC> npcs = manager.getNPCs();
        assertEquals(5, npcs.size());

        for (NPC npc : npcs) {
            assertTrue(npc.getPosition().dst(0, 2, 0) <= 10,
                npc.getType() + " should be within 10 blocks of player start");
            assertEquals(2f, npc.getPosition().y, 0.01f,
                npc.getType() + " should spawn at y=2 (above ground)");
        }
    }

    @Test
    void oneOfEveryNPCTypeSpawned() {
        NPCManager manager = new NPCManager();
        manager.spawnNPC(NPCType.PUBLIC, -4, 2, 5);
        manager.spawnNPC(NPCType.DOG, -2, 2, 5);
        manager.spawnNPC(NPCType.YOUTH_GANG, 0, 2, 5);
        manager.spawnNPC(NPCType.COUNCIL_MEMBER, 2, 2, 5);
        manager.spawnNPC(NPCType.POLICE, 4, 2, 5);

        List<NPC> npcs = manager.getNPCs();

        // Verify one of each type
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.PUBLIC));
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.DOG));
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.YOUTH_GANG));
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.COUNCIL_MEMBER));
        assertTrue(npcs.stream().anyMatch(n -> n.getType() == NPCType.POLICE));
    }

    @Test
    void testNPCsFormLineInFrontOfPlayer() {
        // Test NPCs are in a line at z=5, spread along x-axis
        NPCManager manager = new NPCManager();
        manager.spawnNPC(NPCType.PUBLIC, -4, 2, 5);
        manager.spawnNPC(NPCType.DOG, -2, 2, 5);
        manager.spawnNPC(NPCType.YOUTH_GANG, 0, 2, 5);
        manager.spawnNPC(NPCType.COUNCIL_MEMBER, 2, 2, 5);
        manager.spawnNPC(NPCType.POLICE, 4, 2, 5);

        for (NPC npc : manager.getNPCs()) {
            assertEquals(5f, npc.getPosition().z, 0.01f,
                "All test NPCs should be at z=5 (in front of player)");
        }
    }

    /**
     * Verify council worker (COUNCIL_BUILDER) spawns correctly and is integrated
     * into the 3D rendering pipeline. The council worker renders as an orange hi-vis
     * humanoid using LibGDX's native 3D ModelBatch system.
     */
    @Test
    void councilWorkerSpawnsAndIntegratesInto3DWorld() {
        NPCManager manager = new NPCManager();
        NPC councilWorker = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 5, 2, 5);

        assertNotNull(councilWorker, "Council worker should spawn successfully");
        assertEquals(NPCType.COUNCIL_BUILDER, councilWorker.getType());
        assertEquals(5f, councilWorker.getPosition().x, 0.01f);
        assertEquals(2f, councilWorker.getPosition().y, 0.01f);
        assertEquals(5f, councilWorker.getPosition().z, 0.01f);

        // Council worker should start at full health
        assertEquals(40f, NPCType.COUNCIL_BUILDER.getMaxHealth(), 0.01f,
            "COUNCIL_BUILDER should have 40 max health");
        assertEquals(NPCType.COUNCIL_BUILDER.getMaxHealth(), councilWorker.getHealth(), 0.01f,
            "Council worker should spawn at full health");

        // Council worker is in the NPC list managed by NPCManager
        assertTrue(manager.getNPCs().contains(councilWorker),
            "Council worker should be tracked in NPCManager for rendering");
    }

    /**
     * Verify all NPC types have valid health and are renderable (non-null spawn).
     * Each type must spawn successfully so the 3D renderer has an entity to draw.
     */
    @Test
    void allNPCTypesSpawnSuccessfully() {
        NPCManager manager = new NPCManager();
        int x = 0;
        for (NPCType type : NPCType.values()) {
            NPC npc = manager.spawnNPC(type, x, 2, 10);
            assertNotNull(npc, type + " should spawn without error");
            assertTrue(type.getMaxHealth() > 0, type + " should have positive max health");
            assertEquals(type, npc.getType(), "Spawned NPC should have correct type");
            x += 2;
        }
        assertEquals(NPCType.values().length, manager.getNPCs().size(),
            "All NPC types should be spawnable and tracked for 3D rendering");
    }

    /**
     * Verify the council worker NPC (COUNCIL_BUILDER) has the correct combat stats
     * expected of a defensive council worker in the 3D game world.
     */
    @Test
    void councilWorkerHasCorrectStats() {
        NPCManager manager = new NPCManager();
        NPC worker = manager.spawnNPC(NPCType.COUNCIL_BUILDER, 0, 2, 0);

        // COUNCIL_BUILDER: 40 health, 5 damage, 2.0s cooldown, not hostile
        assertEquals(40f, NPCType.COUNCIL_BUILDER.getMaxHealth(), 0.01f);
        assertFalse(NPCType.COUNCIL_BUILDER.isHostile(),
            "Council worker should not be inherently hostile");
        assertEquals(5f, NPCType.COUNCIL_BUILDER.getAttackDamage(), 0.01f,
            "Council worker deals 5 damage when defending");
        assertEquals(2.0f, NPCType.COUNCIL_BUILDER.getAttackCooldown(), 0.01f,
            "Council worker has 2.0s attack cooldown");
    }
}
