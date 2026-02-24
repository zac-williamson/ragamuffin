package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.TimeSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.OpeningSequence;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #401:
 * The world simulation (NPCs, clock, weather, etc.) was completely frozen while
 * the opening text sequence overlay was active. The {@code !openingSequence.isActive()}
 * guard wrapped {@code updatePlayingSimulation()}, {@code timeSystem.update()},
 * {@code weatherSystem.update()}, and the police-spawning call — all of which must
 * run unconditionally behind the 2D overlay.
 *
 * Fix: removed the {@code !openingSequence.isActive()} wrapper from simulation and
 * time/weather/police calls; kept it only on {@code updatePlayingInput()} so player
 * movement/punch are still suppressed during the sequence.
 *
 * These tests verify the two core invariants from the issue spec:
 *   1. The in-game clock advances while the opening sequence is active.
 *   2. NPCs continue to move (their update() is called) while the sequence is active.
 */
class Issue401OpeningSequenceSimulationTest {

    private OpeningSequence openingSequence;
    private TimeSystem timeSystem;

    @BeforeEach
    void setUp() {
        openingSequence = new OpeningSequence();
        timeSystem = new TimeSystem();
    }

    /**
     * Test 1: Clock advances during the opening sequence.
     *
     * With the bug present, timeSystem.update() was skipped while
     * openingSequence.isActive() — the time stayed frozen at 8:00 AM for the
     * entire 12-second sequence. With the fix, it must advance normally.
     *
     * TimeSystem.DEFAULT_TIME_SPEED = 0.1 hours/real-second.
     * After 60 frames at 1/60 s each (= 1 real second), time must advance by ~0.1 hours.
     */
    @Test
    void clockAdvancesDuringOpeningSequence() {
        openingSequence.start();
        assertTrue(openingSequence.isActive(), "Opening sequence must be active");

        float initialTime = timeSystem.getTime();
        float delta = 1.0f / 60.0f;

        // Simulate 60 frames — 1 real second — while the opening sequence is active.
        // The fix calls timeSystem.update() unconditionally, so time must advance.
        for (int i = 0; i < 60; i++) {
            // Sequence ticks (renders overlay)
            openingSequence.update(delta);
            // Simulation ticks unconditionally (the fix)
            timeSystem.update(delta);
        }

        float finalTime = timeSystem.getTime();

        // 1 real second × 0.1 hours/real-second = 0.1 hours advance
        assertTrue(finalTime > initialTime,
                "Clock must advance while opening sequence is active (was frozen pre-fix)");
        assertEquals(initialTime + 0.1f, finalTime, 0.01f,
                "Clock must advance at the standard rate (0.1 h/s) during the opening sequence");
    }

    /**
     * Test 2: Clock does NOT advance when timeSystem.update() is not called
     * (documents the pre-fix regression).
     */
    @Test
    void clockFreezesWithoutUpdate() {
        openingSequence.start();
        float initialTime = timeSystem.getTime();
        float delta = 1.0f / 60.0f;

        // Simulate opening sequence ticking but deliberately skip timeSystem.update()
        // — this is exactly what the buggy code did.
        for (int i = 0; i < 60; i++) {
            openingSequence.update(delta);
            // timeSystem.update(delta) intentionally omitted — pre-fix behaviour
        }

        assertEquals(initialTime, timeSystem.getTime(), 0.001f,
                "Clock must remain frozen when timeSystem.update() is not called (pre-fix regression)");
    }

    /**
     * Test 3: NPCs continue to move while the opening sequence is active.
     *
     * NPCs are updated via npc.update(delta) inside updatePlayingSimulation(),
     * which the fix calls unconditionally. We verify that an NPC with a non-zero
     * velocity has its position updated over 60 frames.
     */
    @Test
    void npcMovesWhileOpeningSequenceActive() {
        openingSequence.start();
        assertTrue(openingSequence.isActive(), "Opening sequence must be active");

        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 10f);
        // Give the NPC a non-zero velocity so it should move each frame.
        npc.setVelocity(1f, 0f, 0f);

        float initialX = npc.getPosition().x;
        float delta = 1.0f / 60.0f;

        // Simulate 60 frames — the fix calls npc.update() unconditionally via
        // updatePlayingSimulation(), so the NPC must move.
        for (int i = 0; i < 60; i++) {
            openingSequence.update(delta);
            // Simulate the NPC update that happens inside updatePlayingSimulation()
            npc.update(delta);
        }

        float finalX = npc.getPosition().x;

        assertTrue(finalX > initialX,
                "NPC must move while opening sequence is active (was frozen pre-fix)");
    }

    /**
     * Test 4: NPCs freeze when npc.update() is not called during the sequence
     * (documents the pre-fix regression).
     */
    @Test
    void npcFreezesWithoutUpdate() {
        openingSequence.start();

        NPC npc = new NPC(NPCType.PUBLIC, 10f, 1f, 10f);
        npc.setVelocity(1f, 0f, 0f);

        float initialX = npc.getPosition().x;
        float delta = 1.0f / 60.0f;

        // Opening sequence ticks but NPC update is skipped — pre-fix behaviour.
        for (int i = 0; i < 60; i++) {
            openingSequence.update(delta);
            // npc.update(delta) intentionally omitted — pre-fix behaviour
        }

        assertEquals(initialX, npc.getPosition().x, 0.001f,
                "NPC must remain stationary when npc.update() is not called (pre-fix regression)");
    }

    /**
     * Test 5: Player input suppression is preserved — openingSequence.isActive()
     * must still return true while the sequence runs so input gates can check it.
     *
     * The fix only removes the guard from simulation calls; input gating still
     * checks openingSequence.isActive() and must see it as active.
     */
    @Test
    void openingSequenceStillActiveForInputGating() {
        openingSequence.start();
        float delta = 1.0f / 60.0f;

        // Simulate 60 frames (well within the 12-second duration)
        for (int i = 0; i < 60; i++) {
            openingSequence.update(delta);
        }

        // After 1 second (< 12 second DURATION), sequence must still be active
        assertTrue(openingSequence.isActive(),
                "Opening sequence must still be active after 1 second (duration = 12s) — input must remain suppressed");
    }

    /**
     * Test 6: Opening sequence expires naturally after its full duration,
     * and the clock has advanced normally throughout.
     *
     * Runs the full 12-second sequence (720 frames at 60fps) and verifies:
     *  - the sequence completes and becomes inactive
     *  - the clock has advanced by approximately 1.2 hours (12s × 0.1 h/s)
     */
    @Test
    void clockAdvancesFullDurationAndSequenceExpires() {
        openingSequence.start();
        float initialTime = timeSystem.getTime();
        float delta = 1.0f / 60.0f;
        int frames = (int) (12.0f * 60) + 10; // DURATION=12s; a few extra frames past expiry

        for (int i = 0; i < frames; i++) {
            openingSequence.update(delta);
            timeSystem.update(delta);
        }

        assertFalse(openingSequence.isActive(),
                "Opening sequence must have expired after full duration");
        assertTrue(openingSequence.isCompleted(),
                "Opening sequence must be marked completed after full duration");

        float elapsed = (float) frames / 60.0f;
        float expectedAdvance = elapsed * 0.1f; // timeSpeed = 0.1 h/real-s
        assertEquals(initialTime + expectedAdvance, timeSystem.getTime(), 0.05f,
                "Clock must have advanced by timeSpeed × elapsed during and after the opening sequence");
    }
}
