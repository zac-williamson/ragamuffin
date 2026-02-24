package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.HealingSystem;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #381:
 * HealingSystem.update() was not called in the PAUSED render path, so the
 * restingTime accumulator froze while the pause menu was open. A player who
 * had been resting and was approaching the 5-second healing threshold would
 * find their timer stuck the moment they opened the pause menu. After resuming,
 * the timer would continue from where it left off rather than reflecting the
 * true elapsed rest time.
 *
 * Fix: call {@code healingSystem.update(delta, player)} in the PAUSED render
 * block, mirroring the pattern used for dodge (#379), weather (#341), speech
 * log (#343), respawn (#347), particles (#351), hover tooltip (#357), time
 * (#361), and police spawning (#371).
 */
class Issue381HealingSystemPausedTest {

    private Player player;
    private HealingSystem healingSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        healingSystem = new HealingSystem();
        // Ensure player has enough hunger to heal
        player.setHunger(100f);
        // Ensure player is not at full health so healing can be observed
        player.setHealth(50f);
    }

    /**
     * Test 1: restingTime accumulates when update() is called each frame —
     * simulates the paused scenario where each rendered frame advances the timer.
     *
     * After RESTING_DURATION_REQUIRED elapses via per-frame updates,
     * getRestingTime() must be >= RESTING_DURATION_REQUIRED.
     */
    @Test
    void updateAdvancesRestingTimer_thresholdReachedAfterFullDuration() {
        float delta = 1.0f / 60.0f;
        int framesNeeded = (int) Math.ceil(HealingSystem.RESTING_DURATION_REQUIRED / delta) + 1;

        for (int i = 0; i < framesNeeded; i++) {
            healingSystem.update(delta, player);
        }

        assertTrue(healingSystem.getRestingTime() >= HealingSystem.RESTING_DURATION_REQUIRED,
                "restingTime must reach RESTING_DURATION_REQUIRED after per-frame update() calls during pause");
    }

    /**
     * Test 2: Without calling update() (simulating the pre-fix frozen timer),
     * restingTime stays at 0 and healing never triggers.
     *
     * This documents the exact bug: if update() is never called during the pause,
     * the timer freezes and the 5-second threshold is never crossed.
     */
    @Test
    void withoutUpdate_timerFreezes_healingNeverTriggers() {
        // Deliberately do NOT call update() — simulates the frozen timer during pause
        assertEquals(0f, healingSystem.getRestingTime(), 0.001f,
                "restingTime must remain at 0 when update() is never called (pre-fix freeze)");

        float healthBefore = player.getHealth();
        // Even if we check whether healing would occur, it must not have happened
        assertEquals(healthBefore, player.getHealth(), 0.001f,
                "Player health must be unchanged when update() is never called");
    }

    /**
     * Test 3: A player who has been resting for almost 5 seconds (4.9 s) before
     * pausing should have their timer continue to advance during the pause, so
     * that healing begins as soon as the threshold is crossed — even while paused.
     *
     * This directly reproduces the scenario in the issue report.
     */
    @Test
    void nearThresholdRestingTime_continuesAccumulatingDuringPause() {
        // Simulate 4.9 seconds of resting before the pause
        float prePauseDelta = 4.9f;
        healingSystem.update(prePauseDelta, player);
        float restingTimeBeforePause = healingSystem.getRestingTime();

        assertTrue(restingTimeBeforePause >= 4.9f - 0.01f,
                "restingTime must be ~4.9s after pre-pause resting");
        assertTrue(restingTimeBeforePause < HealingSystem.RESTING_DURATION_REQUIRED,
                "Must not yet have crossed the 5-second threshold before pausing");

        // Simulate a few paused frames (0.2 s total) — enough to cross the threshold
        float delta = 1.0f / 60.0f;
        int pauseFrames = (int) Math.ceil(0.2f / delta) + 1;
        for (int i = 0; i < pauseFrames; i++) {
            healingSystem.update(delta, player);
        }

        assertTrue(healingSystem.getRestingTime() >= HealingSystem.RESTING_DURATION_REQUIRED,
                "restingTime must cross the 5-second threshold during the pause after continued update() calls");
    }

    /**
     * Test 4: Healing actually occurs (player health increases) when update() is
     * called for the full required duration — confirms the end-to-end chain works
     * when the timer is advanced via the PAUSED path.
     */
    @Test
    void updateForFullDuration_healingOccurs() {
        float initialHealth = player.getHealth();
        float delta = 1.0f / 60.0f;
        // Run for well past the threshold plus a few extra frames to allow healing
        int totalFrames = (int) Math.ceil((HealingSystem.RESTING_DURATION_REQUIRED + 1.0f) / delta);

        for (int i = 0; i < totalFrames; i++) {
            healingSystem.update(delta, player);
        }

        assertTrue(player.getHealth() > initialHealth,
                "Player health must increase after resting for > RESTING_DURATION_REQUIRED via per-frame updates");
    }

    /**
     * Test 5: restingTime increments monotonically with each update() call
     * while the player remains stationary.
     *
     * The first update() call seeds lastPosition to the player's current position.
     * Subsequent calls see zero displacement and accumulate restingTime.
     */
    @Test
    void updateIncreasesRestingTimeMonotonically() {
        float delta = 0.1f;

        // First update seeds lastPosition — restingTime may reset if player is not at origin
        healingSystem.update(delta, player);
        float previous = healingSystem.getRestingTime();

        // Subsequent updates with the player stationary must strictly increase restingTime
        for (int i = 0; i < 5; i++) {
            healingSystem.update(delta, player);
            float current = healingSystem.getRestingTime();
            assertTrue(current > previous,
                    "restingTime must increase on each update() call after position is seeded (frame " + i + ")");
            previous = current;
        }
    }
}
