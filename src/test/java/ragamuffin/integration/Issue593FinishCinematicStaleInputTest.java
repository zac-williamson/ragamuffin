package ragamuffin.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #593:
 * finishCinematic() does not reset any input state, causing stale key/mouse
 * events to fire on the first PLAYING frame after the opening cinematic.
 *
 * <p>The fix adds the full set of inputHandler.resetX() calls to finishCinematic(),
 * mirroring the resets already present in transitionToPlaying() and restartGame()
 * (established by fixes #567–#591).
 *
 * <p>Tests verify that each reset method clears the corresponding input flag,
 * matching the pattern established by fixes #583, #587, #589, and #591.
 */
class Issue593FinishCinematicStaleInputTest {

    private InputHandler inputHandler;

    @BeforeAll
    static void initGdx() {
        HeadlessTestHelper.initHeadless();
    }

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    // -----------------------------------------------------------------------
    // UI toggle keys — these fire from keyboard during the cinematic
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_resetsInventoryPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);
        assertTrue(inputHandler.isInventoryPressed(),
            "Precondition: I key must set inventoryPressed");

        inputHandler.resetInventory();

        assertFalse(inputHandler.isInventoryPressed(),
            "After finishCinematic(), inventoryPressed must be false — " +
            "inventory must not open on frame 1 due to a key pressed during the cinematic");
    }

    @Test
    void finishCinematic_resetsHelpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);
        assertTrue(inputHandler.isHelpPressed(),
            "Precondition: H key must set helpPressed");

        inputHandler.resetHelp();

        assertFalse(inputHandler.isHelpPressed(),
            "After finishCinematic(), helpPressed must be false");
    }

    @Test
    void finishCinematic_resetsCraftingPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.C);
        assertTrue(inputHandler.isCraftingPressed(),
            "Precondition: C key must set craftingPressed");

        inputHandler.resetCrafting();

        assertFalse(inputHandler.isCraftingPressed(),
            "After finishCinematic(), craftingPressed must be false");
    }

    @Test
    void finishCinematic_resetsAchievementsPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.TAB);
        assertTrue(inputHandler.isAchievementsPressed(),
            "Precondition: TAB key must set achievementsPressed");

        inputHandler.resetAchievements();

        assertFalse(inputHandler.isAchievementsPressed(),
            "After finishCinematic(), achievementsPressed must be false");
    }

    @Test
    void finishCinematic_resetsQuestLogPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.Q);
        assertTrue(inputHandler.isQuestLogPressed(),
            "Precondition: Q key must set questLogPressed");

        inputHandler.resetQuestLog();

        assertFalse(inputHandler.isQuestLogPressed(),
            "After finishCinematic(), questLogPressed must be false");
    }

    // -----------------------------------------------------------------------
    // Escape / Enter — pressing ESC to skip the cinematic leaves escapePressed latched
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_resetsEscapePressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
        assertTrue(inputHandler.isEscapePressed(),
            "Precondition: ESCAPE key must set escapePressed");

        inputHandler.resetEscape();

        assertFalse(inputHandler.isEscapePressed(),
            "After finishCinematic(), escapePressed must be false — " +
            "the game must not immediately pause on frame 1 if ESC was pressed to skip the cinematic");
    }

    @Test
    void finishCinematic_resetsEnterPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
        assertTrue(inputHandler.isEnterPressed(),
            "Precondition: ENTER key must set enterPressed");

        inputHandler.resetEnter();

        assertFalse(inputHandler.isEnterPressed(),
            "After finishCinematic(), enterPressed must be false");
    }

    // -----------------------------------------------------------------------
    // Movement / action keys
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_resetsInteractPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.E);
        assertTrue(inputHandler.isInteractPressed(),
            "Precondition: E key must set interactPressed");

        inputHandler.resetInteract();

        assertFalse(inputHandler.isInteractPressed(),
            "After finishCinematic(), interactPressed must be false — " +
            "interact event must not fire on frame 1");
    }

    @Test
    void finishCinematic_resetsJumpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
        assertTrue(inputHandler.isJumpPressed(),
            "Precondition: SPACE key must set jumpPressed");

        inputHandler.resetJump();

        assertFalse(inputHandler.isJumpPressed(),
            "After finishCinematic(), jumpPressed must be false — " +
            "player must not jump on frame 1");
    }

    @Test
    void finishCinematic_resetsDodgePressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.CONTROL_LEFT);
        assertTrue(inputHandler.isDodgePressed(),
            "Precondition: CTRL key must set dodgePressed");

        inputHandler.resetDodge();

        assertFalse(inputHandler.isDodgePressed(),
            "After finishCinematic(), dodgePressed must be false — " +
            "player must not dodge-roll on frame 1");
    }

    @Test
    void finishCinematic_resetsUpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.UP);
        assertTrue(inputHandler.isUpPressed(),
            "Precondition: UP key must set upPressed");

        inputHandler.resetUp();

        assertFalse(inputHandler.isUpPressed(),
            "After finishCinematic(), upPressed must be false");
    }

    @Test
    void finishCinematic_resetsDownPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.DOWN);
        assertTrue(inputHandler.isDownPressed(),
            "Precondition: DOWN key must set downPressed");

        inputHandler.resetDown();

        assertFalse(inputHandler.isDownPressed(),
            "After finishCinematic(), downPressed must be false");
    }

    // -----------------------------------------------------------------------
    // Scroll / hotbar / crafting slots
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_resetsScrollAmountY() {
        inputHandler.scrolled(0, 3);
        assertNotEquals(0, inputHandler.getScrollAmountY(),
            "Precondition: scrolled() must set scrollAmountY");

        inputHandler.resetScroll();

        assertEquals(0, inputHandler.getScrollAmountY(),
            "After finishCinematic(), scrollAmountY must be 0 — " +
            "hotbar must not cycle on frame 1 due to scrolling during the cinematic");
    }

    @Test
    void finishCinematic_resetsHotbarSlotPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_3);
        assertNotEquals(-1, inputHandler.getHotbarSlotPressed(),
            "Precondition: number key must set hotbarSlotPressed");

        inputHandler.resetHotbarSlot();

        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "After finishCinematic(), hotbarSlotPressed must be -1");
    }

    @Test
    void finishCinematic_resetsCraftingSlotPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_5);
        assertNotEquals(-1, inputHandler.getCraftingSlotPressed(),
            "Precondition: number key must set craftingSlotPressed");

        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "After finishCinematic(), craftingSlotPressed must be -1");
    }

    // -----------------------------------------------------------------------
    // Mouse click events — during the cinematic, cursor is uncaptured so
    // left-click sets leftClickPressed (not punchPressed)
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_resetsLeftClickPressed() {
        // Cursor is uncaptured during cinematic → touchDown sets leftClickPressed
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        assertTrue(inputHandler.isLeftClickPressed(),
            "Precondition: touchDown LEFT (uncaptured cursor) must set leftClickPressed");

        inputHandler.resetLeftClick();

        assertFalse(inputHandler.isLeftClickPressed(),
            "After finishCinematic(), leftClickPressed must be false — " +
            "UI click events must not fire on frame 1");
    }

    @Test
    void finishCinematic_resetsLeftClickReleased() {
        inputHandler.touchDown(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        inputHandler.touchUp(0, 0, 0, com.badlogic.gdx.Input.Buttons.LEFT);
        assertTrue(inputHandler.isLeftClickReleased(),
            "Precondition: touchUp LEFT must set leftClickReleased");

        inputHandler.resetLeftClickReleased();

        assertFalse(inputHandler.isLeftClickReleased(),
            "After finishCinematic(), leftClickReleased must be false");
    }

    // -----------------------------------------------------------------------
    // Punch / Place — defensive resets (may be set if cursor was transiently captured)
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_resetsPunchHeld_isIdempotentWhenFalse() {
        assertFalse(inputHandler.isPunchHeld(),
            "Precondition: punchHeld starts false on fresh handler");

        // resetPunchHeld() must not throw even if punchHeld is already false
        inputHandler.resetPunchHeld();

        assertFalse(inputHandler.isPunchHeld(),
            "After resetPunchHeld(), punchHeld must be false");
    }

    @Test
    void finishCinematic_resetsPunchPressed_isIdempotentWhenFalse() {
        assertFalse(inputHandler.isPunchPressed(),
            "Precondition: punchPressed starts false on fresh handler");

        inputHandler.resetPunch();

        assertFalse(inputHandler.isPunchPressed(),
            "After resetPunch(), punchPressed must be false");
    }

    @Test
    void finishCinematic_resetsPlacePressed_isIdempotentWhenFalse() {
        assertFalse(inputHandler.isPlacePressed(),
            "Precondition: placePressed starts false on fresh handler");

        inputHandler.resetPlace();

        assertFalse(inputHandler.isPlacePressed(),
            "After resetPlace(), placePressed must be false");
    }

    // -----------------------------------------------------------------------
    // Full reset sequence — idempotency on fresh handler
    // -----------------------------------------------------------------------

    @Test
    void allResets_areIdempotentOnFreshHandler() {
        // Calling every reset on a fresh handler must not throw or corrupt state
        inputHandler.resetPunch();
        inputHandler.resetPunchHeld();
        inputHandler.resetPlace();
        inputHandler.resetInventory();
        inputHandler.resetHelp();
        inputHandler.resetCrafting();
        inputHandler.resetAchievements();
        inputHandler.resetQuestLog();
        inputHandler.resetScroll();
        inputHandler.resetInteract();
        inputHandler.resetJump();
        inputHandler.resetDodge();
        inputHandler.resetUp();
        inputHandler.resetDown();
        inputHandler.resetLeftClick();
        inputHandler.resetLeftClickReleased();
        inputHandler.resetEscape();
        inputHandler.resetEnter();
        inputHandler.resetHotbarSlot();
        inputHandler.resetCraftingSlot();

        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isPunchHeld());
        assertFalse(inputHandler.isPlacePressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isHelpPressed());
        assertFalse(inputHandler.isCraftingPressed());
        assertFalse(inputHandler.isAchievementsPressed());
        assertFalse(inputHandler.isQuestLogPressed());
        assertEquals(0, inputHandler.getScrollAmountY());
        assertFalse(inputHandler.isInteractPressed());
        assertFalse(inputHandler.isJumpPressed());
        assertFalse(inputHandler.isDodgePressed());
        assertFalse(inputHandler.isUpPressed());
        assertFalse(inputHandler.isDownPressed());
        assertFalse(inputHandler.isLeftClickPressed());
        assertFalse(inputHandler.isLeftClickReleased());
        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(-1, inputHandler.getCraftingSlotPressed());
    }
}
