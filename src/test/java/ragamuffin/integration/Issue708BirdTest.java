package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #708: Add birds to the game.
 *
 * Verifies that:
 * 1. BIRD is a valid NPCType with appropriate stats (passive, no attack).
 * 2. Birds can be spawned via NPCManager.
 * 3. Birds start in IDLE state (perched).
 * 4. Bird stats are correct (low health, no attack damage, not hostile).
 */
class Issue708BirdTest {

    @Test
    void birdTypeExists() {
        // BIRD must be a member of the NPCType enum
        NPCType bird = NPCType.BIRD;
        assertNotNull(bird, "BIRD NPCType must exist");
    }

    @Test
    void birdIsPassive() {
        // Birds should not be hostile and should do no attack damage
        assertFalse(NPCType.BIRD.isHostile(), "Bird must not be hostile");
        assertEquals(0f, NPCType.BIRD.getAttackDamage(), 0.001f, "Bird must have zero attack damage");
        assertEquals(0f, NPCType.BIRD.getAttackCooldown(), 0.001f, "Bird must have zero attack cooldown");
    }

    @Test
    void birdHasLowHealth() {
        // Birds are fragile
        assertTrue(NPCType.BIRD.getMaxHealth() > 0f, "Bird must have positive health");
        assertTrue(NPCType.BIRD.getMaxHealth() <= 10f, "Bird should have low health (<=10)");
    }

    @Test
    void birdCanBeSpawned() {
        NPCManager manager = new NPCManager();
        NPC bird = manager.spawnNPC(NPCType.BIRD, 5f, 2f, 5f);
        assertNotNull(bird, "Bird NPC should spawn successfully");
        assertEquals(NPCType.BIRD, bird.getType());
    }

    @Test
    void birdStartsIdle() {
        // Birds start perched (IDLE) — they take flight only when the player approaches
        NPCManager manager = new NPCManager();
        NPC bird = manager.spawnNPC(NPCType.BIRD, 5f, 2f, 5f);
        assertNotNull(bird);
        assertEquals(NPCState.IDLE, bird.getState(),
            "Bird should start in IDLE (perched) state");
    }

    @Test
    void birdSpawnPosition() {
        NPCManager manager = new NPCManager();
        NPC bird = manager.spawnNPC(NPCType.BIRD, 3f, 1f, 7f);
        assertNotNull(bird);
        assertEquals(3f, bird.getPosition().x, 0.001f, "Bird x position should match spawn");
        assertEquals(1f, bird.getPosition().y, 0.001f, "Bird y position should match spawn");
        assertEquals(7f, bird.getPosition().z, 0.001f, "Bird z position should match spawn");
    }

    @Test
    void multipleBirdsCanBeSpawned() {
        NPCManager manager = new NPCManager();
        for (int i = 0; i < 7; i++) {
            manager.spawnNPC(NPCType.BIRD, i * 3f, 1f, i * 3f);
        }
        List<NPC> npcs = manager.getNPCs();
        long birdCount = npcs.stream().filter(n -> n.getType() == NPCType.BIRD).count();
        assertEquals(7, birdCount, "All 7 birds should be spawned");
    }

    @Test
    void birdDoesNotExceedNPCCap() {
        NPCManager manager = new NPCManager();
        // Spawn up to cap (100); birds should be included in the cap
        for (int i = 0; i < 110; i++) {
            manager.spawnNPC(NPCType.BIRD, i, 1f, i);
        }
        assertTrue(manager.getNPCs().size() <= 100, "NPC cap must be respected when spawning birds");
    }
}
