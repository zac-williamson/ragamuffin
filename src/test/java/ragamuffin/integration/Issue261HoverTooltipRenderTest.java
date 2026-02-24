package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.HoverTooltipSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #261: HoverTooltipSystem render() must be called
 * each frame after zones are registered, so hover tooltips actually appear.
 *
 * The bug: renderUI() populated tooltip zones every frame but never called
 * hoverTooltipSystem.render(), so tooltips were silently discarded.
 *
 * Fix: hoverTooltipSystem.update(delta) and hoverTooltipSystem.render(...) are
 * now called at the end of renderUI(), after all UI components have registered
 * their zones for the frame.
 *
 * These tests verify the correct frame lifecycle: clear → addZone → update →
 * (render activates) using the TestableHoverTooltipSystem to avoid needing a
 * real LibGDX backend.
 */
class Issue261HoverTooltipRenderTest {

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
     * Test 1 — Core issue regression: zones registered and update() called each
     * frame must eventually activate a tooltip (render() would then display it).
     *
     * Before the fix, render() was never called so the tooltip state was
     * irrelevant. This test confirms the update-side of the pipeline works: after
     * enough frames the tooltip becomes active and would be drawn by render().
     */
    @Test
    void test1_ZonesRegisteredEachFrameActivateTooltipAfterDelay() {
        // Simulate the renderUI() frame loop: clear → addZone → update
        // 20 frames × 0.02 s = 0.4 s > HOVER_DELAY (0.3 s)
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Health");
            system.update(0.02f);
        }

        assertEquals("Health", system.getActiveTooltip(),
                "Tooltip must be active after 0.4 s of hovering — render() would display it");
    }

    /**
     * Test 2 — Tooltip inactive until delay elapses: before the render() fix,
     * no tooltips appeared at all; now we also verify premature activation is
     * prevented (tooltip only shows after the hover delay).
     */
    @Test
    void test2_TooltipNotActiveBeforeHoverDelay() {
        // 10 frames × 0.02 s = 0.2 s < HOVER_DELAY (0.3 s)
        for (int frame = 0; frame < 10; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Hunger");
            system.update(0.02f);
        }

        assertNull(system.getActiveTooltip(),
                "Tooltip must NOT be active before hover delay has elapsed");
    }

    /**
     * Test 3 — If no zones are registered (clear() without addZone()), update()
     * must produce no active tooltip — render() should be a no-op that frame.
     */
    @Test
    void test3_NoZonesRegistered_NoActiveTooltip() {
        // Simulate frames where no UI element adds a zone
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            // No addZone() calls — simulates UI elements not contributing zones
            system.update(0.02f);
        }

        assertNull(system.getActiveTooltip(),
                "No active tooltip when no zones are registered");
        assertEquals(0, system.getZoneCount(),
                "Zone count must be zero when no zones were added");
    }

    /**
     * Test 4 — Multiple zones (simulating hotbar + health bar + crosshair):
     * only the zone under the cursor becomes active.
     */
    @Test
    void test4_MultipleZones_OnlyHoveredZoneActivates() {
        // Mouse at (100,100) — inside zone A, outside zone B
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Health");     // contains (100,100)
            system.addZone(300, 300, 200, 200, "Hotbar 1"); // does NOT contain (100,100)
            system.update(0.02f);
        }

        assertEquals("Health", system.getActiveTooltip(),
                "Only the zone under the cursor should become the active tooltip");
    }

    /**
     * Test 5 — Tooltip clears when zone is no longer registered (e.g. inventory
     * closed so its zones are not added): after one frame with no matching zone,
     * render() must produce nothing.
     */
    @Test
    void test5_TooltipClearsWhenZoneNoLongerRegistered() {
        // Activate tooltip
        for (int frame = 0; frame < 20; frame++) {
            system.clear();
            system.addZone(50, 50, 200, 200, "Item: Brick");
            system.update(0.02f);
        }
        assertEquals("Item: Brick", system.getActiveTooltip(), "Tooltip should be active");

        // Inventory closed — zone not re-registered, mouse still at same position
        system.clear();
        system.update(0.02f);

        assertNull(system.getActiveTooltip(),
                "Tooltip must clear when the zone is no longer registered for the frame");
    }
}
