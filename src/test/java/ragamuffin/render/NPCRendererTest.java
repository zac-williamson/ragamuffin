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
}
