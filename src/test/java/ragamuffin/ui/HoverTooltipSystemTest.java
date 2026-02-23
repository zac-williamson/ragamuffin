package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HoverTooltipSystem.
 *
 * Tests the zone key comparison fix (issue #56): hover timer must accumulate
 * across frames when the same tooltip text is registered, not reset every frame.
 *
 * The update() method depends on Gdx.input/Gdx.graphics, so we use a testable
 * subclass that overrides mouse-position resolution.
 */
class HoverTooltipSystemTest {

    /**
     * Testable subclass that injects a fixed mouse position so tests can
     * exercise update() without a real LibGDX backend.
     */
    static class TestableHoverTooltipSystem extends HoverTooltipSystem {

        float mouseX;
        float mouseY;

        TestableHoverTooltipSystem(float mouseX, float mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        @Override
        protected float getMouseX() {
            return mouseX;
        }

        @Override
        protected float getMouseY() {
            return mouseY;
        }
    }

    private TestableHoverTooltipSystem system;

    @BeforeEach
    void setUp() {
        // Mouse at (100, 100) — inside the zone we'll register at (50,50,200,200)
        system = new TestableHoverTooltipSystem(100f, 100f);
    }

    // ---- TooltipZone tests (pure logic, no LibGDX) ----

    @Test
    void testTooltipZone_containsPoint() {
        HoverTooltipSystem.TooltipZone zone =
                new HoverTooltipSystem.TooltipZone(10f, 10f, 100f, 50f, "HP");

        assertTrue(zone.contains(10f, 10f),  "Bottom-left corner should be inside");
        assertTrue(zone.contains(110f, 60f), "Top-right corner should be inside");
        assertTrue(zone.contains(60f, 35f),  "Centre should be inside");
        assertFalse(zone.contains(9f, 35f),  "Just left of zone should be outside");
        assertFalse(zone.contains(60f, 61f), "Just above zone should be outside");
    }

    @Test
    void testAddZone_nullTextIgnored() {
        system.addZone(0, 0, 200, 200, null);
        assertEquals(0, system.getZoneCount(), "null-text zone must not be registered");
    }

    @Test
    void testAddZone_emptyTextIgnored() {
        system.addZone(0, 0, 200, 200, "");
        assertEquals(0, system.getZoneCount(), "empty-text zone must not be registered");
    }

    @Test
    void testAddZone_validText() {
        system.addZone(0, 0, 200, 200, "Health: 100");
        assertEquals(1, system.getZoneCount());
    }

    @Test
    void testClear_removesAllZones() {
        system.addZone(0, 0, 100, 100, "A");
        system.addZone(100, 0, 100, 100, "B");
        assertEquals(2, system.getZoneCount());
        system.clear();
        assertEquals(0, system.getZoneCount());
    }

    // ---- Hover timer accumulation tests (core bug fix) ----

    /**
     * Core regression test for issue #56.
     *
     * When the same tooltip text is re-registered every frame (the normal
     * clear+addZone cycle), hoverTime must accumulate across frames — not reset.
     * After enough frames totalling >= 0.3s the tooltip must become active.
     */
    @Test
    void testHoverTimerAccumulates_acrossFrames() {
        // Simulate 20 frames of 0.02s each (= 0.4s total, well above HOVER_DELAY=0.3s)
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            // Same text every frame — new TooltipZone object, but same text content
            system.addZone(50, 50, 200, 200, "HP");
            system.update(0.02f);
        }

        assertEquals("HP", system.getActiveTooltip(),
                "Tooltip must be active after 0.4s of hovering over the same zone");
    }

    /**
     * Verify the tooltip does NOT appear before HOVER_DELAY has elapsed.
     */
    @Test
    void testTooltipNotActiveBeforeDelay() {
        // 10 frames * 0.02s = 0.2s, which is less than HOVER_DELAY (0.3s)
        for (int frame = 0; frame < 10; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "HP");
            system.update(0.02f);
        }

        assertNull(system.getActiveTooltip(),
                "Tooltip must NOT be active after only 0.2s (below HOVER_DELAY)");
    }

    /**
     * Moving to a different zone (different text) must reset the hover timer.
     */
    @Test
    void testHoverTimerResetsOnZoneChange() {
        // Hover over "HP" for 0.25s (close to threshold but not over it)
        for (int frame = 0; frame < 12; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "HP");
            system.update(0.02f); // 12 * 0.02 = 0.24s
        }
        assertNull(system.getActiveTooltip(), "Should not be active yet");

        // Move mouse to a different zone with different text
        system.mouseX = 350f;
        system.mouseY = 350f;
        for (int frame = 0; frame < 12; frame++) {
            system.clear();
            system.addZone(300, 300, 200, 200, "Hunger");
            system.update(0.02f); // 12 * 0.02 = 0.24s — timer reset, still below delay
        }

        assertNull(system.getActiveTooltip(),
                "Timer should have reset on zone change — tooltip must not appear yet");
    }

    /**
     * After moving away from a zone (no zone under cursor), tooltip clears.
     */
    @Test
    void testTooltipClearsWhenMouseLeavesZone() {
        // Hover until tooltip is active
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "HP");
            system.update(0.02f);
        }
        assertEquals("HP", system.getActiveTooltip(), "Tooltip should be active");

        // Move mouse away — no zone registered at new position
        system.mouseX = 500f;
        system.mouseY = 500f;
        system.clear();
        system.update(0.02f);

        assertNull(system.getActiveTooltip(),
                "Tooltip must clear when mouse leaves the zone");
    }

    /**
     * getZoneCount returns 0 before any zones are added.
     */
    @Test
    void testInitialState() {
        assertNull(system.getActiveTooltip(), "No active tooltip at start");
        assertEquals(0, system.getZoneCount(), "No zones at start");
    }
}
