package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #389:
 * player.updateDodge(delta) was inside updatePlayingInput(), which is gated behind
 * the !isUIBlocking() check. This meant that opening inventory (I), help (H), or
 * crafting (C) during or immediately after a dodge roll would freeze both the
 * dodgeTimer and the dodgeCooldownTimer for the entire time the UI was open.
 *
 * This created two exploitable bugs:
 *  1. Dodge invincibility extension: opening inventory mid-dodge froze dodgeTimer,
 *     extending i-frames indefinitely.
 *  2. Cooldown bypass: opening inventory while dodgeCooldownTimer was ticking froze
 *     the cooldown, granting free dodges.
 *
 * Fix: player.updateDodge(delta) was moved into updatePlayingSimulation(), which runs
 * unconditionally every frame regardless of UI state — mirroring the fix already
 * applied for PAUSED in Fix #379.
 *
 * These tests verify the Player-level contract that updateDodge() advances timers
 * correctly when called per-frame (as it now is from updatePlayingSimulation),
 * regardless of whether a UI overlay is open.
 */
class Issue389DodgeTimerUIOverlayTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        player.setEnergy(100f);
    }

    /**
     * Test 1: dodgeTimer advances to zero via per-frame updateDodge() calls.
     *
     * Simulates the fix: updatePlayingSimulation() calls updateDodge(delta) each
     * frame even while a UI overlay is open. After DODGE_DURATION has elapsed,
     * isDodging() must be false.
     */
    @Test
    void dodgeTimer_advancesAndExpires_viaPerFrameUpdateCalls() {
        player.dodge(1, 0);
        assertTrue(player.isDodging(), "Player should be dodging after dodge()");

        float delta = 1.0f / 60.0f;
        int framesNeeded = (int) Math.ceil(Player.DODGE_DURATION / delta) + 1;

        for (int i = 0; i < framesNeeded; i++) {
            player.updateDodge(delta);
        }

        assertFalse(player.isDodging(),
                "Dodge must end after DODGE_DURATION has elapsed via per-frame updateDodge() calls " +
                "(simulating updatePlayingSimulation path that runs even with UI open)");
    }

    /**
     * Test 2: Without calling updateDodge() (simulating the pre-fix frozen timer),
     * dodgeTimer never decrements and isDodging() stays true indefinitely.
     *
     * This documents the exact invincibility-extension exploit:
     * the dodge i-frame window would never close if the player opened inventory.
     */
    @Test
    void withoutUpdateDodge_dodgeTimerFreezes_isDodgingRemainsTrue() {
        player.dodge(1, 0);
        assertTrue(player.isDodging(), "Player should be dodging");

        // Deliberately do NOT call updateDodge() — simulates the pre-fix frozen state
        // when UI was open and updatePlayingInput() was skipped
        assertTrue(player.isDodging(),
                "isDodging must remain true when updateDodge() is never called (pre-fix freeze)");
    }

    /**
     * Test 3: dodgeCooldownTimer advances to zero via per-frame updateDodge() calls
     * even after the active dodge has already ended.
     *
     * Simulates the cooldown-bypass exploit: if updateDodge() was not called while
     * the UI was open, dodgeCooldownTimer would freeze and the player could get free
     * dodges by timing inventory opens.
     */
    @Test
    void dodgeCooldownTimer_advancesAndExpires_viaPerFrameUpdateCalls() {
        player.dodge(1, 0);

        // Advance past dodge duration so we enter cooldown-only phase
        player.updateDodge(Player.DODGE_DURATION + 0.01f);
        assertFalse(player.isDodging(), "Active dodge should have ended");
        assertFalse(player.canDodge(), "Should still be in cooldown");
        assertTrue(player.getDodgeCooldownTimer() > 0f,
                "Cooldown timer should be positive after dodge ends");

        // Now simulate per-frame updates (as updatePlayingSimulation does with UI open)
        float delta = 1.0f / 60.0f;
        int framesNeeded = (int) Math.ceil(Player.DODGE_COOLDOWN / delta) + 1;

        for (int i = 0; i < framesNeeded; i++) {
            player.updateDodge(delta);
        }

        assertEquals(0f, player.getDodgeCooldownTimer(), 0.001f,
                "dodgeCooldownTimer must reach zero after DODGE_COOLDOWN has elapsed via " +
                "per-frame updateDodge() calls (simulating updatePlayingSimulation path)");
        assertTrue(player.canDodge(),
                "canDodge() must return true once cooldown expires");
    }

    /**
     * Test 4: A mid-dodge state (nearly expired — one frame remaining) with
     * per-frame updateDodge() calls must end the dodge exactly when expected.
     *
     * Reproduces the exact exploit scenario: player starts dodge, immediately opens
     * inventory. With the fix, updatePlayingSimulation still calls updateDodge() so
     * the last remaining fraction of dodge time drains normally.
     */
    @Test
    void nearExpiredDodge_continuesAdvancingDuringSimulatedUIOpen() {
        player.dodge(1, 0);

        // Advance to just before expiry (all but 2 frames worth)
        float delta = 1.0f / 60.0f;
        float preOpenTime = Player.DODGE_DURATION - 2 * delta;
        player.updateDodge(preOpenTime);

        assertTrue(player.isDodging(),
                "Player should still be dodging with ~2 frames remaining");

        // Simulate the UI opening: updatePlayingInput is skipped, but
        // updatePlayingSimulation still calls updateDodge each frame
        player.updateDodge(delta); // frame 1
        player.updateDodge(delta); // frame 2
        player.updateDodge(delta); // frame 3 (safety margin)

        assertFalse(player.isDodging(),
                "Dodge must expire even when simulating UI-open frames via updateDodge()");
    }

    /**
     * Test 5: dodgeCooldownTimer decrements monotonically with each updateDodge() call.
     *
     * Verifies that the timer drains smoothly (no resets, no skips) when called
     * per-frame — confirming the fix is effective for the cooldown path too.
     */
    @Test
    void dodgeCooldownTimer_decrementsMonotonically() {
        player.dodge(1, 0);
        // Skip past the active dodge phase
        player.updateDodge(Player.DODGE_DURATION + 0.01f);

        float previous = player.getDodgeCooldownTimer();
        float delta = 0.1f;

        for (int i = 0; i < 5; i++) {
            player.updateDodge(delta);
            float current = player.getDodgeCooldownTimer();
            assertTrue(current < previous,
                    "dodgeCooldownTimer must strictly decrease on each updateDodge() call (frame " + i + ")");
            previous = current;
        }
    }
}
