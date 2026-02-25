package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #575:
 * transitionToPaused() and transitionToPlaying() do not reset scrollAmountY,
 * causing stale scroll to cycle the hotbar on the first PLAYING frame after resume.
 *
 * <p>The fix adds {@code inputHandler.resetScroll()} to both
 * {@code transitionToPaused()} and {@code transitionToPlaying()}, and also to
 * {@code restartGame()}, mirroring the pattern established by fixes #567, #569,
 * #571, and #573 for other stale-input flags.
 */
class Issue575TransitionStaleScrollTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetScroll() clears scrollAmountY accumulated while the pause menu
     * was open, so the hotbar does not cycle on the first PLAYING frame after resume.
     * Mirrors what transitionToPlaying() now does.
     */
    @Test
    void transitionToPlaying_resetsScrollAmountY() {
        inputHandler.scrolled(0f, 3f);

        assertEquals(3f, inputHandler.getScrollAmountY(), 0.001f,
            "Precondition: scrolled() must accumulate scrollAmountY");

        inputHandler.resetScroll();

        assertEquals(0f, inputHandler.getScrollAmountY(), 0.001f,
            "After transitionToPlaying(), getScrollAmountY() must be 0 — " +
            "stale scroll from pause menu must not cycle hotbar on resume");
    }

    /**
     * Test 2: resetScroll() clears scrollAmountY accumulated on the same frame as
     * the ESC-to-pause transition, so the hotbar does not cycle on the first PLAYING
     * frame after resume. Mirrors what transitionToPaused() now does.
     */
    @Test
    void transitionToPaused_resetsScrollAmountY() {
        inputHandler.scrolled(0f, -2f);

        assertEquals(-2f, inputHandler.getScrollAmountY(), 0.001f,
            "Precondition: scrolled() must accumulate negative scrollAmountY");

        inputHandler.resetScroll();

        assertEquals(0f, inputHandler.getScrollAmountY(), 0.001f,
            "After transitionToPaused(), getScrollAmountY() must be 0 — " +
            "stale scroll on same frame as ESC must not cycle hotbar on resume");
    }

    /**
     * Test 3: Multiple scroll events are all flushed by a single resetScroll() call.
     */
    @Test
    void resetScroll_flushesAccumulatedScroll() {
        inputHandler.scrolled(0f, 1f);
        inputHandler.scrolled(0f, 1f);
        inputHandler.scrolled(0f, 1f);

        assertEquals(3f, inputHandler.getScrollAmountY(), 0.001f,
            "Precondition: three scroll events must accumulate to 3");

        inputHandler.resetScroll();

        assertEquals(0f, inputHandler.getScrollAmountY(), 0.001f,
            "resetScroll() must flush all accumulated scroll in one call");
    }

    /**
     * Test 4: resetScroll() is idempotent — calling it when scrollAmountY is
     * already 0 does not cause errors and leaves it at 0.
     */
    @Test
    void resetScroll_isIdempotent() {
        assertEquals(0f, inputHandler.getScrollAmountY(), 0.001f,
            "Precondition: fresh InputHandler must have scrollAmountY == 0");

        inputHandler.resetScroll();
        inputHandler.resetScroll();

        assertEquals(0f, inputHandler.getScrollAmountY(), 0.001f,
            "Repeated resetScroll() calls must leave scrollAmountY at 0");
    }

    /**
     * Test 5: resetScroll() does not affect other input flags — it is independent
     * of punch, interact, and UI-toggle state.
     */
    @Test
    void resetScroll_doesNotAffectOtherFlags() {
        inputHandler.scrolled(0f, 2f);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);

        assertTrue(inputHandler.isInventoryPressed(),
            "Precondition: I-key must set inventoryPressed");
        assertTrue(inputHandler.isHelpPressed(),
            "Precondition: H-key must set helpPressed");

        inputHandler.resetScroll();

        assertEquals(0f, inputHandler.getScrollAmountY(), 0.001f,
            "scrollAmountY must be 0 after resetScroll()");
        assertTrue(inputHandler.isInventoryPressed(),
            "inventoryPressed must be unaffected by resetScroll()");
        assertTrue(inputHandler.isHelpPressed(),
            "helpPressed must be unaffected by resetScroll()");
    }
}
