package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.GangTerritorySystem;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #382:
 * {@code gangTerritorySystem.update()} was not called in the PAUSED render path,
 * so the {@code lingerTimer} accumulator froze while the pause menu was open.
 *
 * A player inside a gang territory who was approaching the
 * {@link GangTerritorySystem#LINGER_THRESHOLD_SECONDS 5-second} hostility
 * escalation threshold could open the pause menu to freeze the timer indefinitely,
 * preventing gang NPCs from ever turning HOSTILE.
 *
 * Fix: call {@code gangTerritorySystem.update(delta, player, tooltipSystem,
 * npcManager, world)} in the PAUSED render block of {@code RagamuffinGame.render()},
 * mirroring the pattern used for healing (#381), dodge (#379), reputation (#359),
 * weather (#341), speech log (#343), respawn (#347), particles (#351),
 * hover tooltip (#357), time (#361), and police spawning (#371).
 */
class Issue382GangTerritoryLingerPausedTest {

    private GangTerritorySystem gangTerritorySystem;
    private Player player;
    private TooltipSystem tooltipSystem;
    private NPCManager npcManager;

    @BeforeEach
    void setUp() {
        gangTerritorySystem = new GangTerritorySystem();
        // Register a territory centred at (0, 0) with radius 50 — player starts inside it
        gangTerritorySystem.addTerritory("Bricky Estate", 0f, 0f, 50f);
        // Player at the centre of the territory
        player = new Player(0, 1, 0);
        tooltipSystem = new TooltipSystem();
        npcManager = new NPCManager();
    }

    /**
     * Test 1: lingerTimer accumulates when update() is called each frame —
     * simulates the paused scenario where each rendered frame advances the timer.
     *
     * After just under LINGER_THRESHOLD_SECONDS elapses via per-frame updates,
     * the timer must be close to (but not yet at) LINGER_THRESHOLD_SECONDS.
     * We stop just before the threshold to avoid triggering makeNearbyGangsAggressive.
     */
    @Test
    void updateAdvancesLingerTimer_accumulatesBeforeThreshold() {
        // Advance to just under the 5-second threshold (4.9 s in small per-frame steps)
        float delta = 1.0f / 60.0f;
        float target = GangTerritorySystem.LINGER_THRESHOLD_SECONDS - 0.1f;
        int framesNeeded = (int) Math.ceil(target / delta);

        for (int i = 0; i < framesNeeded; i++) {
            gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, null);
        }

        float linger = gangTerritorySystem.getLingerTimer();
        assertTrue(linger >= target - delta,
                "lingerTimer must have accumulated close to " + target + "s after per-frame update() calls during pause, got: " + linger);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState(),
                "State must remain WARNED until threshold is crossed");
    }

    /**
     * Test 2: Without calling update() (simulating the pre-fix frozen timer),
     * lingerTimer stays at 0 and the HOSTILE escalation never triggers.
     *
     * This documents the exact exploit: if update() is never called during the pause,
     * the timer freezes and the player can stay in a territory indefinitely without
     * gangs turning hostile.
     */
    @Test
    void withoutUpdate_timerFreezes_hostilityNeverEscalates() {
        // Deliberately do NOT call update() — simulates the frozen timer during pause
        assertEquals(0f, gangTerritorySystem.getLingerTimer(), 0.001f,
                "lingerTimer must remain at 0 when update() is never called (pre-fix freeze)");
        assertEquals(GangTerritorySystem.TerritoryState.CLEAR, gangTerritorySystem.getState(),
                "Territory state must remain CLEAR when update() is never called");
    }

    /**
     * Test 3: A player who has been inside a territory for nearly 5 seconds (4.5 s)
     * before pausing should have their timer continue to advance during the pause.
     *
     * This directly reproduces the exploit scenario — the timer should keep
     * accumulating while paused rather than freezing.
     */
    @Test
    void nearThresholdLingerTimer_continuesAccumulatingDuringPause() {
        // Simulate 4.5 seconds of lingering before the pause (under the 5-second threshold)
        float prePauseDelta = 4.5f;
        gangTerritorySystem.update(prePauseDelta, player, tooltipSystem, npcManager, null);
        float lingerBeforePause = gangTerritorySystem.getLingerTimer();

        assertTrue(lingerBeforePause >= 4.5f - 0.01f,
                "lingerTimer must be ~4.5s after pre-pause lingering");
        assertTrue(lingerBeforePause < GangTerritorySystem.LINGER_THRESHOLD_SECONDS,
                "Must not yet have crossed the 5-second threshold before pausing");
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState(),
                "State must be WARNED after entering the territory");

        // Simulate a few paused frames (0.3 s total) — still under threshold so no NPE
        float delta = 1.0f / 60.0f;
        int pauseFrames = (int) Math.ceil(0.3f / delta) + 1;
        for (int i = 0; i < pauseFrames; i++) {
            gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, null);
        }

        float lingerAfterPause = gangTerritorySystem.getLingerTimer();
        assertTrue(lingerAfterPause > lingerBeforePause,
                "lingerTimer must have advanced during the simulated pause frames (pre-fix: timer would freeze)");
        assertTrue(lingerAfterPause >= 4.5f + 0.3f - delta,
                "lingerTimer must reflect the additional time that elapsed during the pause");
    }

    /**
     * Test 4: lingerTimer increments monotonically with each update() call
     * while the player remains inside the territory.
     *
     * The first update() call transitions from CLEAR to WARNED (entering territory).
     * Subsequent calls with the player still inside must strictly increase lingerTimer.
     */
    @Test
    void updateIncreasesLingerTimeMonotonically() {
        float delta = 0.1f;

        // First update transitions state to WARNED — lingerTimer starts at 0 then accumulates
        gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, null);
        float previous = gangTerritorySystem.getLingerTimer();

        // Subsequent updates with the player inside must strictly increase lingerTimer
        // Run 10 frames at 0.1s each — stays well under the 5-second threshold
        for (int i = 0; i < 10; i++) {
            gangTerritorySystem.update(delta, player, tooltipSystem, npcManager, null);
            float current = gangTerritorySystem.getLingerTimer();
            assertTrue(current > previous,
                    "lingerTimer must increase on each update() call while player stays inside territory (frame " + i + ")");
            previous = current;
        }
    }

    /**
     * Test 5: update() with zero delta does NOT advance the timer — zero-delta
     * frame must be a no-op for the linger accumulator.
     */
    @Test
    void updateWithZeroDelta_lingerTimerUnchanged() {
        // Seed the territory state with one real frame
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, null);
        float timerAfterSeed = gangTerritorySystem.getLingerTimer();

        // Subsequent zero-delta updates must not advance the timer
        for (int i = 0; i < 100; i++) {
            gangTerritorySystem.update(0f, player, tooltipSystem, npcManager, null);
        }

        assertEquals(timerAfterSeed, gangTerritorySystem.getLingerTimer(), 0.001f,
                "Zero-delta updates must not advance the linger timer");
    }
}
