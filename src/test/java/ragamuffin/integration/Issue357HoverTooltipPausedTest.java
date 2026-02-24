package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.HoverTooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #357:
 * {@code hoverTooltipSystem.update(delta)} and {@code hoverTooltipSystem.render(...)}
 * were called unconditionally inside {@code renderUI()}, which is invoked from the
 * PAUSED render path.  This meant dwell timers accumulated while the pause menu was
 * open and hover tooltip bubbles could appear on top of the pause menu.
 *
 * <p>Fix: inside {@code renderUI()}, when {@code state == GameState.PAUSED}, call
 * {@code hoverTooltipSystem.clear()} only (discarding all registered zones) and skip
 * {@code update()} / {@code render()} entirely.  Since {@code render()} is skipped,
 * no tooltip bubble appears over the pause menu.  Since {@code update()} is skipped,
 * the dwell timer does not accumulate while paused.
 *
 * <p>These tests exercise the {@link HoverTooltipSystem} lifecycle directly, using
 * a testable subclass that injects a fixed mouse position so no LibGDX backend is
 * required.
 */
class Issue357HoverTooltipPausedTest {

    /**
     * Testable subclass with injected mouse position (no LibGDX required).
     */
    static class TestableHoverTooltipSystem extends HoverTooltipSystem {
        float mouseX;
        float mouseY;

        TestableHoverTooltipSystem(float mouseX, float mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        @Override
        protected float getMouseX() { return mouseX; }

        @Override
        protected float getMouseY() { return mouseY; }
    }

    private TestableHoverTooltipSystem system;

    @BeforeEach
    void setUp() {
        // Mouse positioned inside the zone that will be registered (50,50,200,200)
        system = new TestableHoverTooltipSystem(100f, 100f);
    }

    /**
     * Test 1 — Core regression: calling clear() each frame without update()
     * (the paused-state path) must never activate a tooltip, even after many frames.
     *
     * Before the fix, renderUI() called update() unconditionally so dwell time
     * accumulated while paused.  After the fix, only clear() is called during
     * the PAUSED path, so update() is never called and the dwell timer never
     * reaches the activation threshold.
     */
    @Test
    void pausedPath_clearWithoutUpdate_neverActivatesTooltip() {
        // Simulate 60 frames of the paused render path:
        // renderUI() calls hoverTooltipSystem.clear() but NOT update() or render()
        for (int frame = 0; frame < 60; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Hotbar slot 1");
            // update() deliberately NOT called — matches the PAUSED code path
        }

        assertNull(system.getActiveTooltip(),
                "Tooltip must NOT activate while paused: update() is skipped so dwell timer never advances");
    }

    /**
     * Test 2 — Tooltip activates normally in the PLAYING path (clear + addZone +
     * update each frame).  Verifies the fix does not break the normal lifecycle.
     */
    @Test
    void playingPath_clearAddZoneUpdate_activatesTooltipAfterDelay() {
        // Simulate the PLAYING render path: 20 frames × 0.02 s = 0.4 s > HOVER_DELAY
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Hotbar slot 1");
            system.update(0.02f);
        }

        assertEquals("Hotbar slot 1", system.getActiveTooltip(),
                "Tooltip must activate normally during PLAYING path after hover delay");
    }

    /**
     * Test 3 — During the paused phase, no tooltip fires even if the mouse remains
     * hovering over the same zone for many frames, because update() is never called.
     * After resuming (PLAYING path), the tooltip does eventually activate once the
     * hover delay has elapsed — confirming normal post-pause behaviour is preserved.
     */
    @Test
    void pausedPath_doesNotAccumulateDwell_thenPlayingResumesNormally() {
        // PAUSED: 60 frames of clear-only with mouse over the zone — no update() called
        for (int frame = 0; frame < 60; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Hunger");
            // No update() — paused path: dwell timer must not advance
        }
        assertNull(system.getActiveTooltip(),
                "Tooltip must NOT fire during the paused path (no update() called)");

        // RESUMED: PLAYING path — 20 frames × 0.02 s = 0.4 s > HOVER_DELAY (0.3 s)
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Hunger");
            system.update(0.02f);
        }
        assertEquals("Hunger", system.getActiveTooltip(),
                "Tooltip must activate normally after resume once hover delay elapses");
    }

    /**
     * Test 4 — The pre-fix bug: if update() is called each frame even while paused,
     * the dwell timer accumulates and the tooltip activates.  This test documents
     * the exact failure mode that the fix prevents, by showing that calling update()
     * every frame (the old buggy behaviour) does activate the tooltip.
     */
    @Test
    void preFix_updateCalledWhilePaused_wouldActivateTooltip_documentsOldBug() {
        // Simulate the OLD (buggy) paused path where update() was called unconditionally:
        // 20 frames × 0.02 s = 0.4 s > HOVER_DELAY (0.3 s)
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Health");
            system.update(0.02f); // OLD buggy path — should NOT be called while paused
        }

        // This documents the bug: the tooltip IS active (incorrectly) because
        // update() accumulated the dwell timer during the simulated pause frames.
        assertEquals("Health", system.getActiveTooltip(),
                "Documents pre-fix bug: calling update() while paused accumulates dwell time " +
                "and activates tooltip over the pause menu");
    }

    /**
     * Test 5 — Multiple zones registered each paused frame: since update() is
     * skipped, none of them can activate, regardless of how many frames elapse.
     */
    @Test
    void pausedPath_multipleZones_noneActivateWithoutUpdate() {
        for (int frame = 0; frame < 60; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Health");
            system.addZone(300, 50, 200, 200, "Hotbar slot 2");
            system.addZone(50, 300, 200, 200, "Hunger");
            // No update() — paused path
        }

        assertNull(system.getActiveTooltip(),
                "None of the multiple zones must activate during the paused path " +
                "(update() is skipped so dwell timer never advances)");
    }
}
