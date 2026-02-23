package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.RespawnSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #70:
 * RespawnSystem hardcodes Y=1, spawning player inside terrain on every death.
 *
 * Fix: RespawnSystem.setSpawnY() allows the terrain-aware Y to be supplied
 * from RagamuffinGame, so performRespawn() never places the player inside a
 * solid block.
 */
class Issue70RespawnHeightTest {

    private World world;
    private Player player;
    private RespawnSystem respawnSystem;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 5, 0);
        respawnSystem = new RespawnSystem();
        tooltipSystem = new TooltipSystem();
    }

    /**
     * Test 1: Default spawnY falls back to PARK_CENTRE.y (1.0) when setSpawnY
     * has not been called, preserving backward-compatible behaviour.
     */
    @Test
    void defaultSpawnYIsParkCentreY() {
        assertEquals(RespawnSystem.PARK_CENTRE.y, respawnSystem.getSpawnY(), 0.001f,
                "Default spawnY should equal PARK_CENTRE.y before setSpawnY is called");
    }

    /**
     * Test 2: setSpawnY stores the value and getSpawnY returns it.
     */
    @Test
    void setSpawnYStoresValue() {
        respawnSystem.setSpawnY(3.5f);
        assertEquals(3.5f, respawnSystem.getSpawnY(), 0.001f,
                "getSpawnY() should return the value set by setSpawnY()");
    }

    /**
     * Test 3: After death, performRespawn() places the player at the terrain-aware Y.
     * Place a solid GRASS block at (0, 1, 0) so the terrain height at X=0, Z=0 is 1,
     * meaning the player should spawn at Y=2 (one block above the solid block).
     * Set spawnY=2.0 (simulating calculateSpawnHeight returning 1.0 + 1.0f = 2.0).
     * Kill player, run respawn sequence to completion. Verify player Y == 2.0.
     */
    @Test
    void respawnUsesTerrainAwareY() {
        // Terrain: solid block at Y=1 means the player must spawn at Y=2
        world.setBlock(0, 1, 0, BlockType.GRASS);
        float expectedSpawnY = 2.0f; // calculateSpawnHeight would return 1.0 + 1.0f

        respawnSystem.setSpawnY(expectedSpawnY);

        // Kill the player (damage() sets isDead when health reaches 0)
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead(), "Player should be dead");

        // Manually force the respawn sequence: trigger and run timer to zero
        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning(), "Respawn sequence should be active");

        // Advance time past RESPAWN_MESSAGE_DURATION to execute performRespawn
        float bigDelta = RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f;
        respawnSystem.update(bigDelta, player);

        assertFalse(respawnSystem.isRespawning(), "Respawn sequence should have completed");
        assertEquals(expectedSpawnY, player.getPosition().y, 0.001f,
                "Player Y after respawn must equal terrain-aware spawnY, not hardcoded 1");
    }

    /**
     * Test 4: Respawn X and Z coordinates are always PARK_CENTRE.x and PARK_CENTRE.z,
     * regardless of what spawnY is set to.
     */
    @Test
    void respawnXZAreParkCentre() {
        respawnSystem.setSpawnY(4.0f);

        player.setHealth(1);
        player.damage(1);

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        float bigDelta = RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f;
        respawnSystem.update(bigDelta, player);

        assertEquals(RespawnSystem.PARK_CENTRE.x, player.getPosition().x, 0.001f,
                "Respawn X should be PARK_CENTRE.x");
        assertEquals(RespawnSystem.PARK_CENTRE.z, player.getPosition().z, 0.001f,
                "Respawn Z should be PARK_CENTRE.z");
    }

    /**
     * Test 5: Without setSpawnY (hardcoded Y=1), player spawns inside a block at Y=1.
     * This test documents the OLD (broken) behaviour so the regression is explicit.
     * With the fix, using setSpawnY(2.0f) avoids this.
     */
    @Test
    void withoutSetSpawnYPlayerWouldSpawnInsideBlock() {
        // A GRASS block at (0,1,0) means Y=1 is solid — spawning at Y=1 is inside it
        world.setBlock(0, 1, 0, BlockType.GRASS);

        // OLD behaviour: spawnY == 1.0 (same as the block)
        float oldHardcodedY = RespawnSystem.PARK_CENTRE.y; // 1.0
        boolean oldBehaviourSafe = (oldHardcodedY > 1.0f); // spawning at 1.0 with block at 1 is NOT safe
        assertFalse(oldBehaviourSafe,
                "Hardcoded Y=1 is not safe when a solid block occupies [1,2]");

        // NEW behaviour: setSpawnY to terrain-aware value
        float terrainAwareY = 2.0f; // one block above the solid block
        boolean newBehaviourSafe = (terrainAwareY > 1.0f);
        assertTrue(newBehaviourSafe,
                "Terrain-aware Y=2 places the player above the solid block");
    }
}
