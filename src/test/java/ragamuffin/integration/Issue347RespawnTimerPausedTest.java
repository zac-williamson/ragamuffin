package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.RespawnSystem;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #347:
 * respawnSystem.update() and renderDeathScreen() were not called in the PAUSED
 * render path, so the respawn countdown froze while the pause menu was open.
 * If the player died and then opened the pause menu before the 3-second countdown
 * completed, the timer would not advance and the player would remain indefinitely
 * stuck in the dead/respawning state as long as the pause menu was open.
 *
 * Fix: call {@code respawnSystem.update(delta, player)} (with post-respawn cleanup)
 * in the PAUSED render block, mirroring the pattern used for damage flash (#321),
 * arm swing (#339), weather (#341), speech log (#343), and footstep timer (#345).
 */
class Issue347RespawnTimerPausedTest {

    private Player player;
    private RespawnSystem respawnSystem;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        respawnSystem = new RespawnSystem();
        tooltipSystem = new TooltipSystem();
    }

    /**
     * Test 1: Respawn countdown advances when update() is called each frame —
     * simulates the paused scenario where each rendered frame advances the timer.
     *
     * After the full RESPAWN_MESSAGE_DURATION elapses via per-frame updates,
     * isRespawning() must be false and the player must be alive.
     */
    @Test
    void updateAdvancesCountdown_respawnCompletesAfterFullDuration() {
        // Kill the player and trigger respawn
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead(), "Player must be dead before respawn triggers");

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning(), "isRespawning() must be true immediately after trigger");

        // Advance the timer in small per-frame ticks (simulating paused frames)
        float delta = 1.0f / 60.0f;
        float totalElapsed = 0f;
        int maxFrames = (int) ((RespawnSystem.RESPAWN_MESSAGE_DURATION + 1.0f) / delta);
        for (int i = 0; i < maxFrames; i++) {
            respawnSystem.update(delta, player);
            totalElapsed += delta;
            if (!respawnSystem.isRespawning()) {
                break;
            }
        }

        assertFalse(respawnSystem.isRespawning(),
                "Respawn countdown must complete after RESPAWN_MESSAGE_DURATION via per-frame update() calls during pause");
        assertFalse(player.isDead(),
                "Player must be alive after respawn completes");
    }

    /**
     * Test 2: Without calling update() (simulating the pre-fix frozen timer),
     * the countdown does NOT advance and isRespawning() stays true indefinitely.
     *
     * This documents the exact bug: if update() is never called during the pause,
     * the player remains stuck in the dead/respawning state.
     */
    @Test
    void withoutUpdate_timerFreezes_playerRemainsDeadIndefinitely() {
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead());

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        // Deliberately do NOT call update() — simulates the frozen timer during pause
        // Timer must still be at full duration, player must still be dead
        float timerAfterNoUpdate = respawnSystem.getRespawnTimer();
        assertEquals(RespawnSystem.RESPAWN_MESSAGE_DURATION, timerAfterNoUpdate, 0.001f,
                "Timer must remain at full duration when update() is never called (pre-fix freeze)");
        assertTrue(respawnSystem.isRespawning(),
                "isRespawning() must still be true when update() is never called (timer frozen — pre-fix bug)");
        assertTrue(player.isDead(),
                "Player must still be dead when update() is never called");
    }

    /**
     * Test 3: A single large delta (simulating a long pause followed by resume)
     * completes the countdown in one call — mirrors realistic post-pause behaviour.
     */
    @Test
    void singleLargeDelta_completesRespawn() {
        player.setHealth(1);
        player.damage(1);
        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);
        assertTrue(respawnSystem.isRespawning());

        // Supply a single delta larger than the full countdown
        float bigDelta = RespawnSystem.RESPAWN_MESSAGE_DURATION + 1.0f;
        respawnSystem.update(bigDelta, player);

        assertFalse(respawnSystem.isRespawning(),
                "A single large delta must complete the respawn countdown");
        assertFalse(player.isDead(),
                "Player must be alive after respawn via single large delta");
    }

    /**
     * Test 4: Respawn timer decreases monotonically with each update() call.
     *
     * Each per-frame update must reduce the remaining timer, confirming that
     * the fix correctly advances the countdown on every rendered frame during pause.
     */
    @Test
    void updateDecreaseRespawnTimerMonotonically() {
        player.setHealth(1);
        player.damage(1);
        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);

        float previous = respawnSystem.getRespawnTimer();
        float delta = 0.1f;

        for (int i = 0; i < 5; i++) {
            respawnSystem.update(delta, player);
            if (!respawnSystem.isRespawning()) break;
            float current = respawnSystem.getRespawnTimer();
            assertTrue(current < previous,
                    "Timer must decrease on each update() call (frame " + i + ")");
            previous = current;
        }
    }

    /**
     * Test 5: After the countdown completes, isRespawning() is false and the player
     * is revived with restored stats — verifying the full post-respawn cleanup chain
     * (the same that the PAUSED block must trigger via the fix).
     */
    @Test
    void afterRespawn_playerIsRevivedWithRestoredStats() {
        player.setHealth(1);
        player.damage(1);
        assertTrue(player.isDead());

        respawnSystem.checkAndTriggerRespawn(player, tooltipSystem);

        // Complete the countdown
        respawnSystem.update(RespawnSystem.RESPAWN_MESSAGE_DURATION + 0.1f, player);

        assertFalse(respawnSystem.isRespawning(), "Must not be respawning after completion");
        assertFalse(player.isDead(), "Player must be alive after respawn");
        assertTrue(player.getHealth() > 0, "Player health must be restored after respawn");
    }
}
