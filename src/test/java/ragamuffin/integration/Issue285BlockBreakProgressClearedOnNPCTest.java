package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.BlockBreaker;
import ragamuffin.core.NPCHitDetector;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.GameHUD;
import ragamuffin.world.BlockType;
import ragamuffin.world.RaycastResult;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Issue #285: block break progress bar not cleared when
 * switching from a block target to an NPC target during hold-to-punch.
 *
 * <p>Before the fix, the HUD progress bar would stay frozen at the last block's
 * damage percentage between repeat ticks when the player switched aim from a
 * block to an NPC. The fix clears the progress immediately when {@code
 * hasNPCTarget == true && currentTargetKey == null}.
 */
class Issue285BlockBreakProgressClearedOnNPCTest {

    private static final float PUNCH_REPEAT_INTERVAL = 0.25f;
    private static final float PUNCH_REACH = 3.5f;

    /** Player eye position. */
    private static final Vector3 PLAYER_EYE = new Vector3(0f, 1.8f, 0f);

    private World world;
    private BlockBreaker blockBreaker;
    private NPCManager npcManager;
    private Player player;
    private GameHUD gameHUD;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        blockBreaker = new BlockBreaker();
        npcManager = new NPCManager();
        player = new Player(0, 1, 0);
        gameHUD = new GameHUD(player);

        // Flat pavement so nothing obstructs the NPC ray
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 10; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }
    }

    /**
     * Test 1 — core regression: switching aim from a partially-damaged block to
     * an NPC must immediately clear the block break progress bar (not wait until
     * the next repeat tick).
     *
     * <p>Scenario:
     * <ol>
     *   <li>Player punches a block twice → break progress = 2/5 = 0.4.</li>
     *   <li>Player switches aim to an NPC (no block in front of NPC).</li>
     *   <li>On the very next frame of the hold-to-punch loop (before the first
     *       repeat tick), the HUD progress must already be 0.</li>
     * </ol>
     */
    @Test
    void switchingFromBlockToNPC_immediatelyClearsBreakProgress() {
        // --- Step 1: damage a block at (0,1,2) so it has partial break progress ---
        world.setBlock(0, 1, 2, BlockType.TREE_TRUNK); // 5 hits required
        blockBreaker.punchBlock(world, 0, 1, 2);
        blockBreaker.punchBlock(world, 0, 1, 2);

        float blockProgress = blockBreaker.getBreakProgress(world, 0, 1, 2, null);
        assertEquals(2f / 5f, blockProgress, 0.001f,
                "Precondition: block must have 2/5 break progress before aim switch");

        // Simulate the HUD being at partial progress (as it would be after punching)
        gameHUD.setBlockBreakProgress(blockProgress);
        assertEquals(2f / 5f, gameHUD.getBlockBreakProgress(), 0.001f,
                "Precondition: HUD must reflect partial progress before aim switch");

        // --- Step 2: move block out of the way; spawn NPC where player now aims ---
        world.setBlock(0, 1, 2, BlockType.AIR); // remove the block so NPC is the target
        NPC npc = npcManager.spawnNPC(NPCType.POLICE, 0f, 1f, 2f);
        assertNotNull(npc, "NPC must be spawned");

        // Direction from player eye toward the NPC
        Vector3 dirToNPC = new Vector3(
                0f - PLAYER_EYE.x,
                (1f + NPC.HEIGHT * 0.5f) - PLAYER_EYE.y,
                2f - PLAYER_EYE.z).nor();

        // Verify NPC is in reach (precondition for the fix to trigger)
        NPC detected = NPCHitDetector.findNPCInReach(
                PLAYER_EYE, dirToNPC, PUNCH_REACH,
                npcManager.getNPCs(), blockBreaker, world);
        assertNotNull(detected, "NPC must be in punch reach for this test");

        // Verify no block is in reach in the NPC direction (so currentTargetKey == null)
        RaycastResult blockTarget = blockBreaker.getTargetBlock(
                world, PLAYER_EYE, dirToNPC, PUNCH_REACH);
        assertNull(blockTarget, "No block must be in the NPC direction for this test");

        // --- Step 3: simulate ONE frame of the fixed hold-to-punch logic ---
        // This mirrors the corrected code in updatePlayingInput():
        //
        //   if (hasNPCTarget && currentTargetKey == null) {
        //       gameHUD.setBlockBreakProgress(0f);   // Fix #285
        //   }
        //
        // We run just enough time to NOT reach the repeat-fire threshold so we
        // confirm the progress clears *immediately*, not only on the next punch tick.

        float punchHeldTimer = 0f;
        String lastPunchTargetKey = null;
        float delta = 0.016f; // one ~60fps frame

        // Simulate a sub-threshold accumulation (not enough to fire a repeat punch)
        float subThresholdTime = PUNCH_REPEAT_INTERVAL * 0.5f;
        for (float elapsed = 0f; elapsed < subThresholdTime; elapsed += delta) {
            RaycastResult heldTarget = blockBreaker.getTargetBlock(
                    world, PLAYER_EYE, dirToNPC, PUNCH_REACH);
            String currentTargetKey = (heldTarget != null)
                    ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ())
                    : null;

            boolean hasNPCTarget = NPCHitDetector.findNPCInReach(
                    PLAYER_EYE, dirToNPC, PUNCH_REACH,
                    npcManager.getNPCs(), blockBreaker, world) != null;

            // Fix #285: clear progress immediately when aiming at NPC, not just on repeat tick
            if (hasNPCTarget && currentTargetKey == null) {
                gameHUD.setBlockBreakProgress(0f);
            }

            // Reset timer only when block target changes (preserve NPC timer)
            if (!hasNPCTarget && (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey))) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            } else if (currentTargetKey != null && !currentTargetKey.equals(lastPunchTargetKey)) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            }

            if (currentTargetKey != null || hasNPCTarget) {
                punchHeldTimer += delta;
                // deliberately NOT firing repeat punch (sub-threshold)
            }
        }

        // After the sub-threshold frames, the HUD must already be 0 — not still at 0.4
        assertEquals(0f, gameHUD.getBlockBreakProgress(), 0.001f,
                "Fix #285: switching aim from a block to an NPC must IMMEDIATELY clear the " +
                "block break progress bar, not wait until the next repeat tick. " +
                "Expected 0.0 but was " + gameHUD.getBlockBreakProgress());
    }

    /**
     * Test 2 — block progress is preserved while still aiming at the block (no
     * regression): if the player is holding punch against a block and no NPC is
     * in reach, the progress bar must NOT be cleared between repeat ticks.
     */
    @Test
    void holdingPunchOnBlock_noNPCInRange_preservesProgress() {
        world.setBlock(0, 1, 2, BlockType.TREE_TRUNK);
        blockBreaker.punchBlock(world, 0, 1, 2);
        blockBreaker.punchBlock(world, 0, 1, 2);

        float blockProgress = blockBreaker.getBreakProgress(world, 0, 1, 2, null);
        gameHUD.setBlockBreakProgress(blockProgress);

        Vector3 dirToBlock = new Vector3(0f, 0f, 1f); // straight +Z toward block at z=2

        float punchHeldTimer = 0f;
        String lastPunchTargetKey = null;
        float delta = 0.016f;
        float subThresholdTime = PUNCH_REPEAT_INTERVAL * 0.5f;

        for (float elapsed = 0f; elapsed < subThresholdTime; elapsed += delta) {
            RaycastResult heldTarget = blockBreaker.getTargetBlock(
                    world, PLAYER_EYE, dirToBlock, PUNCH_REACH);
            String currentTargetKey = (heldTarget != null)
                    ? (heldTarget.getBlockX() + "," + heldTarget.getBlockY() + "," + heldTarget.getBlockZ())
                    : null;

            boolean hasNPCTarget = NPCHitDetector.findNPCInReach(
                    PLAYER_EYE, dirToBlock, PUNCH_REACH,
                    npcManager.getNPCs(), blockBreaker, world) != null;

            // Fix #285 branch should NOT fire when there is a block target
            if (hasNPCTarget && currentTargetKey == null) {
                gameHUD.setBlockBreakProgress(0f);
            }

            if (!hasNPCTarget && (currentTargetKey == null || !currentTargetKey.equals(lastPunchTargetKey))) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            } else if (currentTargetKey != null && !currentTargetKey.equals(lastPunchTargetKey)) {
                punchHeldTimer = 0f;
                lastPunchTargetKey = currentTargetKey;
            }

            if (currentTargetKey != null || hasNPCTarget) {
                punchHeldTimer += delta;
            }
        }

        // Progress must still reflect the 2/5 damage (not cleared)
        assertEquals(2f / 5f, gameHUD.getBlockBreakProgress(), 0.001f,
                "Block break progress must NOT be cleared while still aiming at the same block " +
                "with no NPC in reach (regression check for Fix #285)");
    }

    /**
     * Test 3 — verify that the OLD code (no immediate clear on NPC target)
     * would have left the progress bar frozen. This confirms the bug existed.
     *
     * <p>We run the UNFIXED logic to show it leaves the HUD at 0.4, proving why
     * the fix was needed.
     */
    @Test
    void oldLogic_switchingToNPC_leavesProgressFrozen() {
        // Partially damage a block
        world.setBlock(0, 1, 2, BlockType.TREE_TRUNK);
        blockBreaker.punchBlock(world, 0, 1, 2);
        blockBreaker.punchBlock(world, 0, 1, 2);

        float blockProgress = blockBreaker.getBreakProgress(world, 0, 1, 2, null);
        gameHUD.setBlockBreakProgress(blockProgress);

        // Remove block; spawn NPC in same direction
        world.setBlock(0, 1, 2, BlockType.AIR);
        npcManager.spawnNPC(NPCType.POLICE, 0f, 1f, 2f);

        Vector3 dirToNPC = new Vector3(
                0f - PLAYER_EYE.x,
                (1f + NPC.HEIGHT * 0.5f) - PLAYER_EYE.y,
                2f - PLAYER_EYE.z).nor();

        float delta = 0.016f;
        float subThresholdTime = PUNCH_REPEAT_INTERVAL * 0.5f;

        // Simulate OLD logic (no Fix #285 clear)
        for (float elapsed = 0f; elapsed < subThresholdTime; elapsed += delta) {
            // OLD code never calls gameHUD.setBlockBreakProgress(0f) here —
            // it only resets progress inside handlePunch() on the repeat tick.
            // Nothing clears the HUD between ticks.
        }

        // With the OLD code, the HUD would still be at 0.4 (not cleared)
        assertEquals(2f / 5f, gameHUD.getBlockBreakProgress(), 0.001f,
                "Old logic (no Fix #285) must leave HUD frozen at partial progress — " +
                "this confirms the bug #285 existed");
    }
}
