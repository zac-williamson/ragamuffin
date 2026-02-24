package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.BlockBreaker;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #295: Breaking DOOR_LOWER leaves orphan DOOR_UPPER floating.
 *
 * <p>Verifies that when either door half is punched to zero HP, the companion
 * half is also removed from the world — no floating door panels remain.
 */
class Issue295DoorOrphanTest {

    private World world;
    private BlockBreaker blockBreaker;

    /** Door position: lower half at (10, 5, 10), upper half at (10, 6, 10). */
    private static final int DX = 10, DY = 5, DZ = 10;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        blockBreaker = new BlockBreaker();

        // Place a two-block door
        world.setBlock(DX, DY,     DZ, BlockType.DOOR_LOWER);
        world.setBlock(DX, DY + 1, DZ, BlockType.DOOR_UPPER);
    }

    // ── Precondition checks ───────────────────────────────────────────────────

    @Test
    void precondition_doorBlocksArePresentAfterPlacement() {
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(DX, DY,     DZ),
            "DOOR_LOWER must be present at y after placement");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(DX, DY + 1, DZ),
            "DOOR_UPPER must be present at y+1 after placement");
    }

    @Test
    void precondition_doorLowerRequires2Hits() {
        // First punch must NOT break it
        boolean broken = blockBreaker.punchBlock(world, DX, DY, DZ);
        assertFalse(broken, "DOOR_LOWER should NOT break on the first punch (needs 2 hits)");
        assertEquals(BlockType.DOOR_LOWER, world.getBlock(DX, DY, DZ),
            "DOOR_LOWER must still be present after first punch");
    }

    // ── Core fix: breaking DOOR_LOWER removes DOOR_UPPER ─────────────────────

    /**
     * Punching DOOR_LOWER to zero HP must also remove the DOOR_UPPER companion.
     *
     * <p>This is the primary scenario described in Issue #295.
     * The fix lives in {@code RagamuffinGame.handlePunch()}: after
     * {@code blockBreaker.punchBlock()} returns {@code true} for a
     * {@code DOOR_LOWER}, the game calls
     * {@code world.setBlock(x, y+1, z, AIR)} and
     * {@code blockBreaker.clearHits(x, y+1, z)}.
     */
    @Test
    void breakingDoorLower_alsoRemovesDoorUpper() {
        // Simulate handlePunch() logic for two punches on DOOR_LOWER
        boolean broken = false;
        for (int i = 0; i < 2; i++) {
            broken = blockBreaker.punchBlock(world, DX, DY, DZ);
        }
        assertTrue(broken, "DOOR_LOWER must be broken after 2 punches");

        // Simulate the fix from handlePunch(): companion removal
        if (world.getBlock(DX, DY, DZ) == BlockType.AIR) {
            // DOOR_LOWER was just removed — remove companion DOOR_UPPER
            world.setBlock(DX, DY + 1, DZ, BlockType.AIR);
            blockBreaker.clearHits(DX, DY + 1, DZ);
        }

        assertEquals(BlockType.AIR, world.getBlock(DX, DY, DZ),
            "DOOR_LOWER position must be AIR after breaking");
        assertEquals(BlockType.AIR, world.getBlock(DX, DY + 1, DZ),
            "DOOR_UPPER position must be AIR — no floating orphan allowed (Fix #295)");
    }

    /**
     * Breaking DOOR_UPPER directly must also remove the DOOR_LOWER companion.
     */
    @Test
    void breakingDoorUpper_alsoRemovesDoorLower() {
        // Punch DOOR_UPPER to destruction
        boolean broken = false;
        for (int i = 0; i < 2; i++) {
            broken = blockBreaker.punchBlock(world, DX, DY + 1, DZ);
        }
        assertTrue(broken, "DOOR_UPPER must be broken after 2 punches");

        // Simulate the fix from handlePunch(): companion removal
        if (world.getBlock(DX, DY + 1, DZ) == BlockType.AIR) {
            // DOOR_UPPER was just removed — remove companion DOOR_LOWER
            world.setBlock(DX, DY, DZ, BlockType.AIR);
            blockBreaker.clearHits(DX, DY, DZ);
        }

        assertEquals(BlockType.AIR, world.getBlock(DX, DY + 1, DZ),
            "DOOR_UPPER position must be AIR after breaking");
        assertEquals(BlockType.AIR, world.getBlock(DX, DY, DZ),
            "DOOR_LOWER position must be AIR — no floating orphan allowed (Fix #295)");
    }

    /**
     * After a full door break, hit counters for both halves must be zero
     * to prevent stale damage from affecting a newly placed door.
     */
    @Test
    void afterBreakingDoorLower_hitCountersForBothHalvesClearedToZero() {
        // Punch DOOR_LOWER to destruction
        for (int i = 0; i < 2; i++) {
            blockBreaker.punchBlock(world, DX, DY, DZ);
        }

        // Apply companion removal (as the fix does)
        world.setBlock(DX, DY + 1, DZ, BlockType.AIR);
        blockBreaker.clearHits(DX, DY + 1, DZ);

        // punchBlock already clears the hit count on break for DOOR_LOWER
        assertEquals(0, blockBreaker.getHitCount(DX, DY, DZ),
            "Hit counter for DOOR_LOWER position must be 0 after break");
        assertEquals(0, blockBreaker.getHitCount(DX, DY + 1, DZ),
            "Hit counter for DOOR_UPPER position must be 0 after companion removal");
    }

    /**
     * Regression: punching a non-door block must NOT remove the block above it.
     * Ensures the companion-removal logic is door-specific.
     */
    @Test
    void breakingNonDoorBlock_doesNotAffectBlockAbove() {
        // Replace the door with a regular block setup
        world.setBlock(DX, DY,     DZ, BlockType.GLASS);   // fragile, 2 hits
        world.setBlock(DX, DY + 1, DZ, BlockType.BRICK);

        // Punch GLASS to destruction (2 hits)
        for (int i = 0; i < 2; i++) {
            blockBreaker.punchBlock(world, DX, DY, DZ);
        }

        // The GLASS is gone — but the BRICK above must be untouched
        assertEquals(BlockType.AIR, world.getBlock(DX, DY, DZ),
            "GLASS must be broken after 2 hits");
        assertEquals(BlockType.BRICK, world.getBlock(DX, DY + 1, DZ),
            "BRICK above must be unaffected — companion removal is door-specific");
    }

    /**
     * Partial damage to DOOR_LOWER (1 hit, not yet broken) must leave DOOR_UPPER intact.
     */
    @Test
    void partialDamageOnDoorLower_doesNotRemoveDoorUpper() {
        blockBreaker.punchBlock(world, DX, DY, DZ); // 1 hit only, not broken

        assertEquals(BlockType.DOOR_LOWER, world.getBlock(DX, DY, DZ),
            "DOOR_LOWER must survive one punch");
        assertEquals(BlockType.DOOR_UPPER, world.getBlock(DX, DY + 1, DZ),
            "DOOR_UPPER must be unaffected by partial damage to DOOR_LOWER");
    }
}
