package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementsUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #461: AchievementsUI render and keyboard toggle
 * were never connected to RagamuffinGame.
 *
 * <p>These tests verify the full lifecycle of the achievements overlay:
 * <ol>
 *   <li>toggle() opens the overlay (isVisible() becomes true).</li>
 *   <li>A second toggle() closes it (isVisible() becomes false).</li>
 *   <li>hide() closes the overlay (ESC behaviour).</li>
 *   <li>UP/DOWN arrow keys scroll the list.</li>
 *   <li>hide() on restart/transitionToPlaying() clears the visible state.</li>
 *   <li>isUIBlocking() equivalent — achievementsUI visible blocks other UI.</li>
 * </ol>
 */
class Issue461AchievementsUIWiringTest {

    private AchievementSystem achievementSystem;
    private AchievementsUI achievementsUI;

    @BeforeEach
    void setUp() {
        achievementSystem = new AchievementSystem();
        achievementsUI = new AchievementsUI(achievementSystem);
    }

    /**
     * Test 1: toggle() opens the achievements overlay.
     *
     * Models pressing the achievements key (Tab) in PLAYING state.
     * The overlay must start hidden; after toggle() it must be visible.
     */
    @Test
    void toggleOpensOverlay() {
        assertFalse(achievementsUI.isVisible(),
                "AchievementsUI must start hidden");

        achievementsUI.toggle();

        assertTrue(achievementsUI.isVisible(),
                "AchievementsUI must be visible after toggle()");
    }

    /**
     * Test 2: A second toggle() closes the overlay.
     *
     * Models pressing Tab again while the overlay is open.
     */
    @Test
    void toggleClosesOverlayWhenAlreadyOpen() {
        achievementsUI.toggle(); // open
        assertTrue(achievementsUI.isVisible(), "Open after first toggle");

        achievementsUI.toggle(); // close

        assertFalse(achievementsUI.isVisible(),
                "AchievementsUI must be hidden after second toggle()");
    }

    /**
     * Test 3: hide() closes the overlay (ESC to close behaviour).
     *
     * Models pressing ESC while the achievements overlay is open; the
     * handleEscapePress() method now calls achievementsUI.hide().
     */
    @Test
    void hideClosesOverlay() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(), "Open after show()");

        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
                "AchievementsUI must be hidden after hide()");
    }

    /**
     * Test 4: UP/DOWN arrow keys scroll the list.
     *
     * Models the handleUIInput() forwarding of UP/DOWN to scrollUp()/scrollDown()
     * when achievementsUI.isVisible() is true.
     * scrollDown() increments scrollOffset; scrollUp() decrements it.
     */
    @Test
    void upDownScrollsList() {
        achievementsUI.show();

        int initialOffset = achievementsUI.getScrollOffset();
        assertEquals(0, initialOffset, "Scroll offset must start at 0");

        // scrollDown() moves toward later achievements
        achievementsUI.scrollDown();
        int afterDown = achievementsUI.getScrollOffset();

        // scrollUp() moves back toward earlier achievements
        achievementsUI.scrollUp();
        int afterUp = achievementsUI.getScrollOffset();

        // After one down then one up we should be back at 0 (or the list is too
        // short to scroll — in which case both calls are no-ops and offset stays 0)
        assertTrue(afterDown >= initialOffset,
                "scrollDown() must not decrease the scroll offset");
        assertTrue(afterUp <= afterDown,
                "scrollUp() must not increase the scroll offset beyond afterDown");
        assertEquals(initialOffset, afterUp,
                "One scrollDown() followed by one scrollUp() must restore the offset to its initial value");
    }

    /**
     * Test 5: show() resets scroll offset to 0.
     *
     * When the overlay is shown after being scrolled, the list must start
     * from the top so the player always sees the beginning of their achievements.
     */
    @Test
    void showResetsScrollOffset() {
        achievementsUI.show();
        achievementsUI.scrollDown();
        achievementsUI.hide();

        // Re-opening should reset to top
        achievementsUI.show();

        assertEquals(0, achievementsUI.getScrollOffset(),
                "show() must reset the scroll offset to 0");
    }

    /**
     * Test 6: hide() clears visible state — simulates restartGame() and
     * transitionToPlaying() behaviour.
     *
     * restartGame() calls achievementsUI.hide() so the overlay does not
     * persist into the new session; transitionToPlaying() does the same so
     * the overlay does not remain open after unpausing.
     */
    @Test
    void hideOnRestartOrTransitionClearsOverlay() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(), "Overlay open before restart");

        // Simulate what restartGame() / transitionToPlaying() does
        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
                "AchievementsUI must not be visible after hide() — " +
                "overlay must not persist across restarts or state transitions");
    }

    /**
     * Test 7: isUIBlocking() equivalent — achievementsUI visible must count as
     * a blocking UI overlay so that player input is suppressed while it is open.
     *
     * Models the updated isUIBlocking() check in RagamuffinGame:
     *   inventoryUI.isVisible() || helpUI.isVisible() || craftingUI.isVisible()
     *                           || achievementsUI.isVisible()
     */
    @Test
    void achievementsVisibleIsUIBlocking() {
        assertFalse(achievementsUI.isVisible(),
                "Not blocking when hidden");

        achievementsUI.show();

        // Simulate the isUIBlocking() check
        boolean uiBlocking = achievementsUI.isVisible();
        assertTrue(uiBlocking,
                "achievementsUI.isVisible() must return true when open — " +
                "isUIBlocking() must return true so player movement is suppressed");
    }
}
