package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.HoverTooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #395:
 * {@code hoverTooltipSystem.clear()} in the PAUSED branch only emptied the zones list
 * but did NOT reset {@code hoverTime}, {@code lastHoverZoneKey}, or {@code activeTooltip}.
 *
 * <p>Consequence: on the first PLAYING frame after resume, {@code update(delta)} found
 * the same zone, saw that {@code lastHoverZoneKey} still matched (no reset during pause),
 * and added the new delta to a {@code hoverTime} that already exceeded {@code HOVER_DELAY}.
 * The tooltip fired immediately on resume rather than requiring the full 0.3 s dwell again.
 *
 * <p>Fix: {@link HoverTooltipSystem#reset()} clears zones AND resets all dwell state.
 * The PAUSED branch in {@code renderUI()} now calls {@code reset()} instead of
 * {@code clear()}, ensuring the hover dwell timing starts fresh on resume.
 */
class Issue395HoverTooltipResumeTest {

    /** Testable subclass with injected mouse position (no LibGDX required). */
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
     * Core regression test: after calling reset() during the PAUSED path, the first
     * PLAYING frame must NOT immediately show the tooltip — the full 0.3 s dwell is
     * required from scratch.
     *
     * Before the fix, clear() left hoverTime and lastHoverZoneKey intact, so a single
     * update(0.02f) frame would find the existing accumulated dwell time and fire the
     * tooltip instantly on resume.
     */
    @Test
    void afterReset_firstPlayingFrame_doesNotFireTooltipImmediately() {
        // PLAYING: accumulate enough dwell time to activate the tooltip
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Inventory slot");
            system.update(0.02f); // 20 × 0.02 s = 0.4 s > HOVER_DELAY (0.3 s)
        }
        assertEquals("Inventory slot", system.getActiveTooltip(),
                "Tooltip must be active before pause");

        // PAUSED: call reset() (the fixed PAUSED path) — clears ALL dwell state
        system.reset();

        assertNull(system.getActiveTooltip(),
                "Active tooltip must be null immediately after reset()");

        // RESUMED: first PLAYING frame — tooltip must NOT fire on this single frame
        system.addZone(50, 50, 200, 200, "Inventory slot");
        system.update(0.02f); // Only 0.02 s elapsed — well below HOVER_DELAY

        assertNull(system.getActiveTooltip(),
                "Tooltip must NOT fire immediately on resume: full 0.3 s dwell is required from scratch");
    }

    /**
     * After reset(), tooltip activates normally once the full dwell period elapses.
     * Verifies that reset() does not permanently suppress tooltips.
     */
    @Test
    void afterReset_tooltipActivatesNormallyOnceDwellElapses() {
        // PLAYING: build up dwell, then pause (reset)
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Crafting slot");
            system.update(0.02f);
        }
        system.reset();

        // RESUMED: 20 frames × 0.02 s = 0.4 s > HOVER_DELAY (0.3 s)
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Crafting slot");
            system.update(0.02f);
        }

        assertEquals("Crafting slot", system.getActiveTooltip(),
                "Tooltip must activate normally after resume once the full dwell period elapses");
    }

    /**
     * reset() must clear the zones list (same behaviour as clear()).
     */
    @Test
    void reset_clearsZones() {
        system.addZone(50, 50, 200, 200, "Zone 1");
        system.addZone(300, 50, 200, 200, "Zone 2");
        assertEquals(2, system.getZoneCount(), "Zones must be registered before reset");

        system.reset();

        assertEquals(0, system.getZoneCount(), "reset() must clear all registered zones");
    }

    /**
     * Calling reset() on a fresh (never-used) system must be a no-op with no exceptions.
     */
    @Test
    void reset_onFreshSystem_isNoOp() {
        assertDoesNotThrow(() -> system.reset(),
                "reset() on a fresh system must not throw");
        assertNull(system.getActiveTooltip(), "Active tooltip must be null after reset on fresh system");
        assertEquals(0, system.getZoneCount(), "Zone count must be 0 after reset on fresh system");
    }

    /**
     * Simulates the exact scenario from the bug report:
     * 1. Inventory open, cursor hovering over a slot → tooltip is active
     * 2. Player presses ESC → PAUSED path calls reset()
     * 3. Player presses ESC again → PLAYING resumes, mouse hasn't moved
     * 4. First frame after resume: tooltip must NOT appear immediately
     */
    @Test
    void bugScenario_inventoryOpen_pause_resume_tooltipDoesNotFireImmediately() {
        // Step 1: Inventory open, cursor hovering → tooltip becomes active
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Iron sword x2");
            system.update(0.02f);
        }
        assertEquals("Iron sword x2", system.getActiveTooltip(),
                "Prerequisite: tooltip must be active before pause");

        // Step 2: Several paused frames — PAUSED path calls reset() each frame
        for (int pauseFrame = 0; pauseFrame < 30; pauseFrame++) {
            system.reset();
            // During pause, UI zones are NOT re-registered and update() is NOT called
        }

        // Step 3: First PLAYING frame after resume (mouse still over the same zone)
        system.addZone(50, 50, 200, 200, "Iron sword x2");
        system.update(0.02f);

        // Step 4: Tooltip must NOT fire yet — requires fresh 0.3 s dwell
        assertNull(system.getActiveTooltip(),
                "Bug #395: tooltip must NOT fire immediately on resume; full 0.3 s dwell required");
    }
}
