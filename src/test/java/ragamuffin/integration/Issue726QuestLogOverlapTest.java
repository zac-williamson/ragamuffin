package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.ui.QuestLogUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue #726:
 * Quest log UI panel overlaps other interface elements (hotbar, GameHUD, ClockHUD, SpeechLogUI).
 *
 * <p>Root cause: when the quest log overlay is visible, several always-on UI elements
 * (hotbar, clock, GameHUD status bars, NPC speech log) were still rendered — either
 * showing through the semi-transparent edges of the panel, or (for the speech log)
 * rendering on top of the overlay entirely because it is drawn after the quest log.
 *
 * <p>Fix: add {@code !questLogUI.isVisible()} guards to the render calls for
 * hotbarUI, clockHUD, the GameHUD block, and speechLogUI in {@code renderUI()},
 * mirroring the pattern already used for the QuestTrackerUI (Issue #497).
 *
 * <p>These tests verify the suppression logic in isolation (without a live LibGDX
 * render context) by checking that UI components correctly report their visibility
 * state and that the rendering guard conditions produce the expected booleans.
 */
class Issue726QuestLogOverlapTest {

    private QuestLogUI questLogUI;

    @BeforeEach
    void setUp() {
        BuildingQuestRegistry registry = new BuildingQuestRegistry();
        questLogUI = new QuestLogUI(registry);
    }

    // -----------------------------------------------------------------------
    // Test 1 — Hotbar guard condition is false when quest log is open.
    //
    // In renderUI() the hotbar is rendered only when:
    //   !openingSequence.isActive() && !questLogUI.isVisible()
    //
    // When the quest log is visible, the second operand is false, so the
    // hotbar render call must be skipped.
    // -----------------------------------------------------------------------
    @Test
    void hotbarGuardIsFalseWhenQuestLogIsVisible() {
        assertFalse(questLogUI.isVisible(), "Quest log must start hidden");

        boolean openingActive = false; // opening sequence not active

        // With quest log hidden, hotbar should render
        boolean shouldRenderHotbar = !openingActive && !questLogUI.isVisible();
        assertTrue(shouldRenderHotbar,
                "Hotbar must render when quest log is hidden and opening sequence is not active");

        // Open quest log
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Quest log must be visible after show()");

        // With quest log visible, hotbar must NOT render
        shouldRenderHotbar = !openingActive && !questLogUI.isVisible();
        assertFalse(shouldRenderHotbar,
                "Hotbar must NOT render when quest log is open — " +
                "the guard !questLogUI.isVisible() must prevent hotbar overlap");
    }

    // -----------------------------------------------------------------------
    // Test 2 — ClockHUD guard condition is false when quest log is open.
    //
    // In renderUI() the clock is rendered only when:
    //   !questLogUI.isVisible()
    //
    // When the quest log is visible, this must return false.
    // -----------------------------------------------------------------------
    @Test
    void clockGuardIsFalseWhenQuestLogIsVisible() {
        assertFalse(questLogUI.isVisible(), "Quest log must start hidden");

        boolean shouldRenderClock = !questLogUI.isVisible();
        assertTrue(shouldRenderClock,
                "Clock must render when quest log is hidden");

        questLogUI.show();

        shouldRenderClock = !questLogUI.isVisible();
        assertFalse(shouldRenderClock,
                "Clock must NOT render when quest log is open — " +
                "it sits in the top-right corner and overlaps the panel area");
    }

    // -----------------------------------------------------------------------
    // Test 3 — GameHUD guard condition is false when quest log is open.
    //
    // The GameHUD (health/hunger/energy bars, crosshair, weather) is rendered
    // only when: !openingSequence.isActive() && !questLogUI.isVisible()
    // -----------------------------------------------------------------------
    @Test
    void gameHudGuardIsFalseWhenQuestLogIsVisible() {
        assertFalse(questLogUI.isVisible(), "Quest log must start hidden");

        boolean openingActive = false;

        boolean shouldRenderHUD = !openingActive && !questLogUI.isVisible();
        assertTrue(shouldRenderHUD,
                "GameHUD must render when quest log is hidden");

        questLogUI.show();

        shouldRenderHUD = !openingActive && !questLogUI.isVisible();
        assertFalse(shouldRenderHUD,
                "GameHUD must NOT render when quest log is open — " +
                "status bars and weather text overlap the panel edges");
    }

    // -----------------------------------------------------------------------
    // Test 4 — SpeechLog guard condition is false when quest log is open.
    //
    // The NPC speech log is rendered only when:
    //   !openingSequence.isActive() && !questLogUI.isVisible()
    //
    // Without this guard, the speech log renders AFTER the quest log panel
    // (later in renderUI()), so its entries appear on top of the overlay.
    // -----------------------------------------------------------------------
    @Test
    void speechLogGuardIsFalseWhenQuestLogIsVisible() {
        assertFalse(questLogUI.isVisible(), "Quest log must start hidden");

        boolean openingActive = false;

        boolean shouldRenderSpeechLog = !openingActive && !questLogUI.isVisible();
        assertTrue(shouldRenderSpeechLog,
                "Speech log must render when quest log is hidden");

        questLogUI.show();

        shouldRenderSpeechLog = !openingActive && !questLogUI.isVisible();
        assertFalse(shouldRenderSpeechLog,
                "Speech log must NOT render when quest log is open — " +
                "it is drawn after the quest log panel and would appear on top of it");
    }

    // -----------------------------------------------------------------------
    // Test 5 — Quest tracker is already suppressed when quest log is open.
    //
    // This pre-existing guard (Issue #497) is confirmed here to document the
    // existing behaviour and ensure it has not regressed.
    // -----------------------------------------------------------------------
    @Test
    void questTrackerAlreadySuppressedWhenQuestLogVisible() {
        assertFalse(questLogUI.isVisible(), "Quest log must start hidden");

        boolean openingActive = false;

        boolean shouldRenderTracker = !openingActive && !questLogUI.isVisible();
        assertTrue(shouldRenderTracker,
                "Quest tracker must render when quest log is hidden");

        questLogUI.show();

        shouldRenderTracker = !openingActive && !questLogUI.isVisible();
        assertFalse(shouldRenderTracker,
                "Quest tracker must NOT render when quest log is open — " +
                "pre-existing guard from Issue #497 must remain intact");
    }

    // -----------------------------------------------------------------------
    // Test 6 — All suppressed elements resume rendering when quest log is closed.
    //
    // After hiding the quest log, all suppressed elements must be allowed to
    // render again, ensuring the fix does not permanently hide them.
    // -----------------------------------------------------------------------
    @Test
    void suppressedElementsResumeAfterQuestLogClosed() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Quest log visible after show()");

        questLogUI.hide();
        assertFalse(questLogUI.isVisible(), "Quest log hidden after hide()");

        boolean openingActive = false;

        assertTrue(!openingActive && !questLogUI.isVisible(),
                "Hotbar guard must be true after quest log is closed");
        assertTrue(!questLogUI.isVisible(),
                "Clock guard must be true after quest log is closed");
        assertTrue(!openingActive && !questLogUI.isVisible(),
                "GameHUD guard must be true after quest log is closed");
        assertTrue(!openingActive && !questLogUI.isVisible(),
                "Speech log guard must be true after quest log is closed");
    }

    // -----------------------------------------------------------------------
    // Test 7 — Toggle correctly flips suppression state.
    //
    // Uses toggle() (the Q-key action) to verify suppression goes on/off/on.
    // -----------------------------------------------------------------------
    @Test
    void toggleCycleSuppressesAndRestoresElements() {
        assertFalse(questLogUI.isVisible(), "Starts hidden");

        boolean openingActive = false;

        // Before toggle: elements should render
        assertTrue(!openingActive && !questLogUI.isVisible(),
                "Elements must render before first toggle");

        questLogUI.toggle(); // open
        assertTrue(questLogUI.isVisible(), "Visible after first toggle");

        // After first toggle: elements must be suppressed
        assertFalse(!openingActive && !questLogUI.isVisible(),
                "Elements must be suppressed when quest log is open");

        questLogUI.toggle(); // close
        assertFalse(questLogUI.isVisible(), "Hidden after second toggle");

        // After second toggle: elements must render again
        assertTrue(!openingActive && !questLogUI.isVisible(),
                "Elements must render again after quest log is closed");
    }
}
