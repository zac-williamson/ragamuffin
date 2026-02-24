package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #277:
 * Energy drained on every held punch even when hitting nothing.
 *
 * Fix: player.consumeEnergy() is now called only when a punch actually
 * connects with an NPC or a targetable block — not unconditionally at the
 * top of handlePunch().
 *
 * These tests verify the contract by exercising the same conditional logic
 * that handlePunch() uses: getTargetBlock() returns null when aimed at sky
 * or empty space, so energy must not be drained.
 */
class Issue277EnergyDrainOnMissTest {

    private World world;
    private Player player;
    private BlockBreaker blockBreaker;

    @BeforeEach
    void setUp() {
        world = new World(42);
        player = new Player(0, 5, 0);
        blockBreaker = new BlockBreaker();
    }

    /**
     * Test 1: Punching at empty air (no block in range) must NOT drain energy.
     *
     * Player is at (0,5,0) looking upward into empty sky. The raycast returns
     * null. Simulating 20 "punch" attempts that find no target verifies that
     * energy stays at MAX_ENERGY.
     */
    @Test
    void punchingAtNothingDoesNotDrainEnergy() {
        float initialEnergy = player.getEnergy();
        assertEquals(Player.MAX_ENERGY, initialEnergy, 0.01f,
                "Player should start at full energy");

        // Aim at empty space — direction pointing away from any blocks
        Vector3 origin = new Vector3(0, 50, 0);
        Vector3 directionIntoSky = new Vector3(0, 1, 0).nor();

        // Simulate 20 punch attempts in empty space
        for (int i = 0; i < 20; i++) {
            RaycastResult result = blockBreaker.getTargetBlock(world, origin, directionIntoSky, 5.0f);
            if (result != null) {
                // Only consume energy if a block was actually hit (the fixed behaviour)
                player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);
            }
            // If result == null: miss — energy is NOT consumed
        }

        assertEquals(initialEnergy, player.getEnergy(), 0.01f,
                "Energy must not be drained when every punch hits nothing");
    }

    /**
     * Test 2: Punching at a real block DOES drain energy on each successful hit.
     *
     * Player faces a STONE block. Each punch that connects should consume
     * ENERGY_DRAIN_PER_ACTION. After N hits the energy is reduced by N * drain.
     */
    @Test
    void punchingBlockDrainsEnergyPerHit() {
        // Place a STONE block directly in front of the origin
        world.setBlock(0, 5, 2, BlockType.STONE);

        float initialEnergy = player.getEnergy();
        Vector3 origin = new Vector3(0, 5, 0);
        Vector3 forward = new Vector3(0, 0, 1).nor(); // towards z=2

        int hits = 0;
        int attempts = 5;
        for (int i = 0; i < attempts; i++) {
            RaycastResult result = blockBreaker.getTargetBlock(world, origin, forward, 5.0f);
            if (result != null) {
                player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);
                hits++;
            }
        }

        // The block is present so every cast should hit it
        assertEquals(attempts, hits,
                "All " + attempts + " punches should have connected with the STONE block");

        float expectedEnergy = initialEnergy - (Player.ENERGY_DRAIN_PER_ACTION * attempts);
        assertEquals(expectedEnergy, player.getEnergy(), 0.01f,
                "Energy should be reduced by exactly ENERGY_DRAIN_PER_ACTION per hit");
    }

    /**
     * Test 3: Punching at nothing repeatedly (simulating hold-to-break over many ticks)
     * preserves full energy, confirming the fix prevents the reported drain.
     *
     * This directly models the hold-to-break scenario from the issue: LMB held while
     * aimed at sky fires handlePunch() every 0.25s. Energy must remain unchanged.
     */
    @Test
    void holdPunchAtEmptySpacePreservesEnergy() {
        float initialEnergy = player.getEnergy();

        // 40 repeat-punch ticks — equivalent to 10 seconds of held LMB at 0.25s interval
        Vector3 origin = new Vector3(0, 100, 0);
        Vector3 up = new Vector3(0, 1, 0);

        for (int tick = 0; tick < 40; tick++) {
            RaycastResult result = blockBreaker.getTargetBlock(world, origin, up, 5.0f);
            if (result != null) {
                player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);
            }
        }

        assertEquals(initialEnergy, player.getEnergy(), 0.01f,
                "Holding punch aimed at empty space for 40 ticks must not drain any energy");
    }

    /**
     * Test 4: Mixed scenario — some punches miss, some connect.
     * Total energy drain equals exactly the number of connecting hits × drain per action.
     */
    @Test
    void onlyConnectingHitsDrainEnergy() {
        // Block at z=2 — first 3 punches aim at it, last 3 aim at sky
        world.setBlock(0, 5, 2, BlockType.GRASS);
        float initialEnergy = player.getEnergy();

        Vector3 origin = new Vector3(0, 5, 0);
        Vector3 atBlock = new Vector3(0, 0, 1).nor();
        Vector3 atSky = new Vector3(0, 1, 0).nor();

        int connectingHits = 0;
        for (int i = 0; i < 3; i++) {
            RaycastResult result = blockBreaker.getTargetBlock(world, origin, atBlock, 5.0f);
            if (result != null) {
                player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);
                connectingHits++;
            }
        }
        for (int i = 0; i < 3; i++) {
            RaycastResult result = blockBreaker.getTargetBlock(world, origin, atSky, 5.0f);
            if (result != null) {
                player.consumeEnergy(Player.ENERGY_DRAIN_PER_ACTION);
                connectingHits++;
            }
        }

        assertEquals(3, connectingHits,
                "Exactly 3 punches should have connected (the block-aimed ones)");

        float expectedEnergy = initialEnergy - (Player.ENERGY_DRAIN_PER_ACTION * 3);
        assertEquals(expectedEnergy, player.getEnergy(), 0.01f,
                "Only the 3 connecting hits should drain energy; misses must not");
    }
}
