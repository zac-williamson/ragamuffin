package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.PauseMenu;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue #413:
 * Pause menu text alignment mismatch — visual text position did not match
 * the clickable hit-box area.
 *
 * The bug: handleClick() computed optionBottomScreen = optionTopScreen + optionHeight,
 * placing the lower bound of the hit box BELOW the text baseline.  Because LibGDX
 * renders text ABOVE its baseline, the hit box was shifted down relative to the
 * visible text — clicks on the visible label missed the zone, while clicks below
 * the visible text (where nothing was drawn) registered as hits.
 *
 * The fix: align the hit box with the rendered text by treating the baseline as
 * the BOTTOM of the zone:
 *   optionTopScreen    = baselineScreen - optionHeight   (top    of rendered glyph)
 *   optionBottomScreen = baselineScreen                  (bottom = baseline)
 */
class Issue413PauseMenuClickAlignmentTest {

    private static final int SCREEN_WIDTH  = 1280;
    private static final int SCREEN_HEIGHT = 720;

    private PauseMenu pauseMenu;

    @BeforeEach
    void setUp() {
        pauseMenu = new PauseMenu();
        pauseMenu.show();
    }

    // -----------------------------------------------------------------------
    // Helper: compute the screen-Y of the text baseline for option i.
    //   render() draws option i at OpenGL Y = screenHeight * 0.5f - i * 50f
    //   screen coords (0=top): screenHeight - openGlY
    // -----------------------------------------------------------------------
    private float baselineScreenY(int i) {
        float optionStartY = SCREEN_HEIGHT * 0.5f;
        float optionSpacing = 50f;
        float openGlY = optionStartY - i * optionSpacing;
        return SCREEN_HEIGHT - openGlY;
    }

    // -----------------------------------------------------------------------
    // Clicking exactly at the text baseline — top of the rendered glyph in
    // screen coords is just above the baseline.  A click AT the baseline
    // should be inside the hit-box (it is the bottom boundary, inclusive).
    // -----------------------------------------------------------------------
    @Test
    void clickAtTextBaselineHitsOption() {
        int centerX = SCREEN_WIDTH / 2;

        for (int i = 0; i < 3; i++) {
            int screenY = (int) baselineScreenY(i); // exactly at baseline
            int hit = pauseMenu.handleClick(centerX, screenY, SCREEN_WIDTH, SCREEN_HEIGHT);
            assertEquals(i, hit,
                "Click at text baseline for option " + i + " must register as a hit");
        }
    }

    // -----------------------------------------------------------------------
    // Clicking at the centre of the rendered text (halfway between top-of-glyph
    // and baseline) must also register as a hit.
    // -----------------------------------------------------------------------
    @Test
    void clickAtTextCentreHitsOption() {
        float optionHeight = 30f;
        int centerX = SCREEN_WIDTH / 2;

        for (int i = 0; i < 3; i++) {
            float baseline  = baselineScreenY(i);
            int screenY = (int) (baseline - optionHeight / 2f); // midpoint of glyph
            int hit = pauseMenu.handleClick(centerX, screenY, SCREEN_WIDTH, SCREEN_HEIGHT);
            assertEquals(i, hit,
                "Click at text centre for option " + i + " must register as a hit");
        }
    }

    // -----------------------------------------------------------------------
    // Clicking well BELOW the text (in screen coords, larger Y = lower on screen)
    // must NOT register as a hit.  Previously this DID register due to the bug.
    // -----------------------------------------------------------------------
    @Test
    void clickBelowTextDoesNotHitOption() {
        float optionHeight = 30f;
        int centerX = SCREEN_WIDTH / 2;

        for (int i = 0; i < 3; i++) {
            float baseline = baselineScreenY(i);
            // 20 pixels below the baseline — clearly below the rendered text
            int screenY = (int) (baseline + 20f);
            int hit = pauseMenu.handleClick(centerX, screenY, SCREEN_WIDTH, SCREEN_HEIGHT);
            assertNotEquals(i, hit,
                "Click below rendered text for option " + i + " must NOT register as a hit");
        }
    }

    // -----------------------------------------------------------------------
    // Clicking well ABOVE the text (smaller screen Y = higher on screen) must
    // NOT register as a hit.
    // -----------------------------------------------------------------------
    @Test
    void clickAboveTextDoesNotHitOption() {
        float optionHeight = 30f;
        int centerX = SCREEN_WIDTH / 2;

        for (int i = 0; i < 3; i++) {
            float baseline = baselineScreenY(i);
            // 20 pixels above the top of the glyph
            int screenY = (int) (baseline - optionHeight - 20f);
            int hit = pauseMenu.handleClick(centerX, screenY, SCREEN_WIDTH, SCREEN_HEIGHT);
            assertNotEquals(i, hit,
                "Click above rendered text for option " + i + " must NOT register as a hit");
        }
    }

    // -----------------------------------------------------------------------
    // Clicking far outside the horizontal bounds must return -1.
    // -----------------------------------------------------------------------
    @Test
    void clickOutsideHorizontalBoundsReturnsMiss() {
        float optionHeight = 30f;
        // Click at horizontal edge — far left
        for (int i = 0; i < 3; i++) {
            float baseline = baselineScreenY(i);
            int screenY = (int) (baseline - optionHeight / 2f);
            int hit = pauseMenu.handleClick(10, screenY, SCREEN_WIDTH, SCREEN_HEIGHT);
            assertEquals(-1, hit,
                "Click far left of option " + i + " must return -1");
        }
    }

    // -----------------------------------------------------------------------
    // When not visible, handleClick must always return -1.
    // -----------------------------------------------------------------------
    @Test
    void hiddenMenuClickAlwaysReturnsMiss() {
        pauseMenu.hide();
        int hit = pauseMenu.handleClick(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2,
                SCREEN_WIDTH, SCREEN_HEIGHT);
        assertEquals(-1, hit, "Hidden pause menu must not respond to clicks");
    }
}
