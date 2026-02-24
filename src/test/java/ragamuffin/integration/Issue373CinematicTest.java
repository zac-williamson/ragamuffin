package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.CinematicCamera;
import ragamuffin.ui.MainMenuScreen;
import ragamuffin.ui.OpeningSequence;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #373: Opening cinematic cut-scene with city fly-through.
 *
 * Verifies:
 * 1. Cinematic starts inactive and becomes active on start().
 * 2. Camera position changes over time (the fly-through moves).
 * 3. After DURATION seconds the cinematic completes automatically.
 * 4. skip() immediately marks the cinematic completed and inactive.
 * 5. The final camera position is at/near the spawn location (park centre).
 * 6. When "Skip Intro" is ON in the menu, the cinematic is skipped immediately.
 * 7. When the cinematic completes, the text opening sequence can begin.
 * 8. The cinematic can be skipped while mid-flight.
 */
class Issue373CinematicTest {

    private CinematicCamera cinematicCamera;
    private MainMenuScreen mainMenuScreen;
    private OpeningSequence openingSequence;

    @BeforeEach
    void setUp() {
        cinematicCamera = new CinematicCamera();
        mainMenuScreen = new MainMenuScreen();
        openingSequence = new OpeningSequence();
    }

    /**
     * Test 1: Cinematic is inactive before start() is called.
     */
    @Test
    void cinematicInactiveBeforeStart() {
        assertFalse(cinematicCamera.isActive(), "Cinematic must be inactive before start()");
        assertFalse(cinematicCamera.isCompleted(), "Cinematic must not be completed before start()");
    }

    /**
     * Test 2: Cinematic becomes active after start().
     */
    @Test
    void cinematicActiveAfterStart() {
        cinematicCamera.start();
        assertTrue(cinematicCamera.isActive(), "Cinematic must be active after start()");
        assertFalse(cinematicCamera.isCompleted(), "Cinematic must not be completed immediately after start()");
    }

    /**
     * Test 3: Camera position changes as time advances (the fly-through moves the camera).
     */
    @Test
    void cameraPositionChangesOverTime() {
        cinematicCamera.start();

        // Record initial position
        com.badlogic.gdx.math.Vector3 posAtStart = new com.badlogic.gdx.math.Vector3(cinematicCamera.getPosition());

        // Advance by 3 seconds (well past the first waypoint interval)
        cinematicCamera.update(3.0f);

        com.badlogic.gdx.math.Vector3 posAt3s = new com.badlogic.gdx.math.Vector3(cinematicCamera.getPosition());

        // The camera must have moved
        assertFalse(posAtStart.epsilonEquals(posAt3s, 0.01f),
                "Camera position must change as the cinematic progresses");
    }

    /**
     * Test 4: Cinematic completes automatically after DURATION seconds.
     */
    @Test
    void cinematicCompletesAfterDuration() {
        cinematicCamera.start();

        // Advance past the full duration
        cinematicCamera.update(CinematicCamera.DURATION + 0.1f);

        assertFalse(cinematicCamera.isActive(), "Cinematic must no longer be active after duration");
        assertTrue(cinematicCamera.isCompleted(), "Cinematic must be completed after duration");
    }

    /**
     * Test 5: skip() immediately marks the cinematic as completed.
     */
    @Test
    void skipCompletesImmediately() {
        cinematicCamera.start();

        // Skip partway through
        cinematicCamera.update(2.0f);
        cinematicCamera.skip();

        assertFalse(cinematicCamera.isActive(), "Cinematic must be inactive after skip()");
        assertTrue(cinematicCamera.isCompleted(), "Cinematic must be completed after skip()");
    }

    /**
     * Test 6: The final camera position (at end of cinematic) is near the world origin (spawn).
     * The last waypoint is defined at approximately (0, 4, 0) — the park spawn.
     */
    @Test
    void finalPositionIsNearSpawn() {
        cinematicCamera.start();
        cinematicCamera.update(CinematicCamera.DURATION);

        com.badlogic.gdx.math.Vector3 finalPos = cinematicCamera.getPosition();

        // The final waypoint is at (0, 4, 0) — allow a tolerance of 2 blocks
        assertEquals(0f, finalPos.x, 2.0f, "Final camera X must be near spawn (x=0)");
        assertEquals(0f, finalPos.z, 2.0f, "Final camera Z must be near spawn (z=0)");
    }

    /**
     * Test 7: When "Skip Intro" is ON, simulating what startNewGame() does:
     * the cinematic should be skipped immediately and the opening sequence also skipped.
     */
    @Test
    void skipIntroSkipsBothCinematicAndTextSequence() {
        mainMenuScreen.toggleSkipIntro(); // Enable skip intro
        assertTrue(mainMenuScreen.isSkipIntroEnabled());

        // Simulate what startNewGame() does with skip intro enabled:
        // skip both cinematic and text sequence
        if (mainMenuScreen.isSkipIntroEnabled()) {
            // Skip cinematic
            cinematicCamera.start();
            cinematicCamera.skip();
            // Skip text sequence
            openingSequence.start();
            openingSequence.skip();
        }

        assertFalse(cinematicCamera.isActive(), "Cinematic must not be active with skip intro ON");
        assertTrue(cinematicCamera.isCompleted(), "Cinematic must be completed with skip intro ON");
        assertFalse(openingSequence.isActive(), "Opening sequence must not be active with skip intro ON");
        assertTrue(openingSequence.isCompleted(), "Opening sequence must be completed with skip intro ON");
    }

    /**
     * Test 8: Cinematic and text opening sequence run simultaneously (Fix #428).
     * Both start together; the text is active while the cinematic plays, and continues
     * into PLAYING state after the cinematic ends.
     */
    @Test
    void textSequenceRunsSimultaneouslyWithCinematic() {
        // Simulate startNewGame(): start both together
        cinematicCamera.start();
        openingSequence.start();

        // Both must be active immediately
        assertTrue(cinematicCamera.isActive(), "Cinematic must be active after start");
        assertTrue(openingSequence.isActive(), "Text opening sequence must be active from the start of the cinematic");

        // Advance through the cinematic duration (8 s)
        float delta = 1.0f / 60.0f;
        float elapsed = 0f;
        while (elapsed < CinematicCamera.DURATION + 0.1f) {
            cinematicCamera.update(delta);
            openingSequence.update(delta);
            elapsed += delta;
        }

        // Cinematic must be done; text sequence (12 s total) must still be running
        assertTrue(cinematicCamera.isCompleted(), "Cinematic must complete after its duration");
        assertTrue(openingSequence.isActive(),
                "Text opening sequence must still be active after cinematic ends (it runs for 12 s total)");
        assertFalse(openingSequence.isCompleted(),
                "Text opening sequence must not have completed yet (only ~8 s have passed of 12)");
    }

    /**
     * Test 9: The cinematic timer advances correctly with multiple small updates.
     */
    @Test
    void timerAdvancesWithMultipleUpdates() {
        cinematicCamera.start();
        assertEquals(0f, cinematicCamera.getTimer(), 0.001f, "Timer must start at 0");

        cinematicCamera.update(0.5f);
        assertEquals(0.5f, cinematicCamera.getTimer(), 0.001f, "Timer must advance by delta");

        cinematicCamera.update(1.0f);
        assertEquals(1.5f, cinematicCamera.getTimer(), 0.001f, "Timer must accumulate across updates");
    }

    /**
     * Test 10: After skip(), the position equals the final waypoint position.
     */
    @Test
    void skipPositionsAtFinalWaypoint() {
        cinematicCamera.start();
        cinematicCamera.skip();

        com.badlogic.gdx.math.Vector3 pos = cinematicCamera.getPosition();
        // Final waypoint is at (0, 4, 0) — allow tolerance of 1 block
        assertEquals(0f, pos.x, 1.0f, "Skipped position X must match final waypoint");
        assertEquals(4f, pos.y, 1.0f, "Skipped position Y must match final waypoint");
        assertEquals(0f, pos.z, 1.0f, "Skipped position Z must match final waypoint");
    }
}
