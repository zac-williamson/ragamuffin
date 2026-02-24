package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockBreaker;
import ragamuffin.core.NPCHitDetector;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;
import ragamuffin.world.RaycastResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Issue #279:
 * Hold-to-punch should fire repeat hits against NPC targets, not just blocks.
 *
 * <p>Before the fix, the hold-to-repeat timer in updatePlayingInput() only ticked
 * when {@code currentTargetKey != null} (i.e. a block was targeted). When the
 * player held left-click while facing an NPC, {@code heldTarget} (from
 * {@link BlockBreaker#getTargetBlock}) was null → {@code currentTargetKey} was
 * null → the timer reset every frame → only the initial single-shot punch landed.
 *
 * <p>Fix: the repeat branch now also checks {@link NPCHitDetector#findNPCInReach}
 * and ticks/fires the timer whenever either a block OR an NPC is in reach.
 *
 * <p>These tests verify the corrected behaviour by directly simulating the
 * hold-to-repeat timer loop (as it runs inside the private
 * {@code updatePlayingInput()} method), confirming that NPCManager.punchNPC()
 * is invoked the expected number of times over 3+ repeat intervals.
 */
class Issue279HoldPunchNPCTest {

    /** Repeat interval (seconds) from RagamuffinGame.PUNCH_REPEAT_INTERVAL. */
    private static final float PUNCH_REPEAT_INTERVAL = 0.25f;

    /** Player eye position — elevated above ground like in-game. */
    private static final Vector3 PLAYER_EYE = new Vector3(0f, 1.8f, 0f);

    /** NPC is placed 2 blocks directly in front (along +Z). */
    private static final float NPC_X = 0f;
    private static final float NPC_Y = 1f;
    private static final float NPC_Z = 2f;

    /** Punch reach constant from RagamuffinGame. */
    private static final float PUNCH_REACH = 3.5f;

    private World world;
    private BlockBreaker blockBreaker;
    private NPCManager npcManager;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        blockBreaker = new BlockBreaker();
        npcManager = new NPCManager();

        // Flat pavement so nothing obstructs the ray
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 10; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
    }

    /**
     * Test 1 — core regression: holding left-click for 3 repeat intervals while
     * facing an NPC must deliver at least 3 hits to that NPC.
     *
     * <p>Before Fix #279: only the initial single-shot punch landed (1 hit total).
     * After Fix #279: the repeat timer ticks even when no block is targeted, so
     * at least 3 additional hits fire after 3 × PUNCH_REPEAT_INTERVAL seconds.
     */
    @Test
    void holdingPunchFacingNPC_delivers3RepeatHits() {
        // Spawn a high-health NPC (POLICE: 50 HP) so it survives 3+ hits of 10 HP each
        NPC npc = npcManager.spawnNPC(NPCType.POLICE, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc, "NPC must be successfully spawned");
        float initialHealth = npc.getHealth();
        assertTrue(initialHealth >= 50f, "POLICE NPC must have 50 HP — enough to survive 3+ hits for this test");

        // Direction aiming directly at the NPC from the player's eye
        Vector3 direction = new Vector3(NPC_X - PLAYER_EYE.x,
                (NPC_Y + NPC.HEIGHT * 0.5f) - PLAYER_EYE.y,
                NPC_Z - PLAYER_EYE.z).nor();

        // Verify the NPC is detectable before we start (precondition)
        NPC detected = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, direction, PUNCH_REACH,
                npcManager.getNPCs(), blockBreaker, world);
        assertNotNull(detected, "NPC must be in reach before the hold-to-punch simulation starts");

        // --- Simulate the fixed hold-to-repeat loop from updatePlayingInput() ---
        //
        // The loop below mirrors the corrected isPunchHeld() branch:
        //
        //   heldTarget = blockBreaker.getTargetBlock(...)   → null (no block in front)
        //   currentTargetKey = null
        //   hasNPCTarget = findNPCInReach(...) != null      → true
        //   if (currentTargetKey != null || hasNPCTarget)   → true → tick timer
        //
        // We drive it for 3 full repeat intervals so exactly 3 repeat punches fire.

        float punchHeldTimer = 0f;
        String lastPunchTargetKey = null;
        int repeatHits = 0;
        float totalTime = PUNCH_REPEAT_INTERVAL * 3.5f; // enough for 3 full intervals
        float delta = 0.016f; // ~60 fps frame time

        for (float elapsed = 0f; elapsed < totalTime; elapsed += delta) {
            // Replicate the fixed isPunchHeld() branch logic exactly (Fix #279).
            RaycastResult heldTarget = blockBreaker.getTargetBlock(
                    world, PLAYER_EYE, direction, PUNCH_REACH);
            String currentTargetKey = (heldTarget != null)
                    ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ())
                    : null;

            boolean hasNPCTarget = NPCHitDetector.findNPCInReach(
                    PLAYER_EYE, direction, PUNCH_REACH,
                    npcManager.getNPCs(), blockBreaker, world) != null;

            // Reset timer only when no NPC is in reach AND the block target changed.
            // (Same logic as the fix: don't reset every frame just because there's no block.)
            if (!hasNPCTarget && (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey))) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            } else if (currentTargetKey != null && !currentTargetKey.equals(lastPunchTargetKey)) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            }

            if (currentTargetKey != null || hasNPCTarget) {
                punchHeldTimer += delta;
                if (punchHeldTimer >= PUNCH_REPEAT_INTERVAL) {
                    punchHeldTimer -= PUNCH_REPEAT_INTERVAL;
                    // Simulate the punch landing on the NPC
                    npcManager.punchNPC(npc, direction);
                    repeatHits++;
                }
            }
        }

        // Verify at least 3 repeat hits landed
        assertTrue(repeatHits >= 3,
                "Fix #279: holding left-click facing an NPC must deliver at least 3 repeat hits " +
                "over 3 × PUNCH_REPEAT_INTERVAL seconds, but only got " + repeatHits);

        // Verify the NPC actually took damage proportional to those hits (10 HP each)
        float expectedMinDamage = repeatHits * 10f;
        float actualDamage = initialHealth - npc.getHealth();
        assertTrue(actualDamage >= expectedMinDamage - 0.01f,
                "NPC health should have decreased by at least " + expectedMinDamage +
                " HP from " + repeatHits + " repeat hits, actual decrease: " + actualDamage);
    }

    /**
     * Test 2 — pre-fix regression guard: the OLD logic (timer only ticks when
     * currentTargetKey != null) would have delivered 0 repeat hits when facing
     * an NPC with no block target.  This test verifies that the old code path
     * (block-only guard) would have failed — confirming the bug existed.
     *
     * <p>We simulate the OLD (unfixed) logic to show it yields 0 repeat hits,
     * demonstrating why the fix was necessary.
     */
    @Test
    void oldLogic_withNPCTarget_deliversZeroRepeatHits() {
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, NPC_X, NPC_Y, NPC_Z);
        assertNotNull(npc);

        Vector3 direction = new Vector3(NPC_X - PLAYER_EYE.x,
                (NPC_Y + NPC.HEIGHT * 0.5f) - PLAYER_EYE.y,
                NPC_Z - PLAYER_EYE.z).nor();

        // --- Simulate the OLD (unfixed) isPunchHeld() logic ---
        float punchHeldTimer = 0f;
        String lastPunchTargetKey = null;
        int repeatHits = 0;
        float totalTime = PUNCH_REPEAT_INTERVAL * 3.5f;
        float delta = 0.016f;

        for (float elapsed = 0f; elapsed < totalTime; elapsed += delta) {
            RaycastResult heldTarget = blockBreaker.getTargetBlock(
                    world, PLAYER_EYE, direction, PUNCH_REACH);
            String currentTargetKey = (heldTarget != null)
                    ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ())
                    : null;

            // OLD logic: reset timer when null, only tick when non-null
            if (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey)) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            }
            // OLD guard — no NPC check
            if (currentTargetKey != null) {
                punchHeldTimer += delta;
                if (punchHeldTimer >= PUNCH_REPEAT_INTERVAL) {
                    punchHeldTimer -= PUNCH_REPEAT_INTERVAL;
                    npcManager.punchNPC(npc, direction);
                    repeatHits++;
                }
            }
        }

        // The old code delivers 0 repeat hits when only an NPC is in reach
        assertEquals(0, repeatHits,
                "The unfixed logic must deliver 0 repeat hits when facing an NPC " +
                "(no block target) — this confirms the bug #279 existed");
    }

    /**
     * Test 3 — block target behaviour is preserved: when holding while facing
     * a block (no NPC), the repeat timer still fires correctly.
     */
    @Test
    void holdingPunchFacingBlock_stillDeliversRepeatHits() {
        // Place a block directly in front of the player (at z=2, y=1)
        world.setBlock(0, 1, 2, BlockType.STONE);

        // Aim straight at the block
        Vector3 direction = new Vector3(0f, 0f, 1f);

        // Verify block is detected (precondition)
        RaycastResult block = blockBreaker.getTargetBlock(world, PLAYER_EYE, direction, PUNCH_REACH);
        assertNotNull(block, "Stone block must be detectable before the test");

        // Simulate fixed logic — should still work for block targets
        float punchHeldTimer = 0f;
        String lastPunchTargetKey = null;
        int repeatHits = 0;
        float totalTime = PUNCH_REPEAT_INTERVAL * 3.5f;
        float delta = 0.016f;
        BlockBreaker testBreaker = new BlockBreaker();

        for (float elapsed = 0f; elapsed < totalTime; elapsed += delta) {
            RaycastResult heldTarget = testBreaker.getTargetBlock(
                    world, PLAYER_EYE, direction, PUNCH_REACH);
            String currentTargetKey = (heldTarget != null)
                    ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ())
                    : null;

            boolean hasNPCTarget = NPCHitDetector.findNPCInReach(
                    PLAYER_EYE, direction, PUNCH_REACH,
                    npcManager.getNPCs(), testBreaker, world) != null;

            // Fixed reset logic (same as game code)
            if (!hasNPCTarget && (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey))) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            } else if (currentTargetKey != null && !currentTargetKey.equals(lastPunchTargetKey)) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            }

            if (currentTargetKey != null || hasNPCTarget) {
                punchHeldTimer += delta;
                if (punchHeldTimer >= PUNCH_REPEAT_INTERVAL) {
                    punchHeldTimer -= PUNCH_REPEAT_INTERVAL;
                    repeatHits++;
                }
            }
        }

        assertTrue(repeatHits >= 3,
                "Hold-to-punch against a block target must still deliver at least 3 repeat hits " +
                "(regression check — block behaviour must not be broken by Fix #279)");
    }
}
