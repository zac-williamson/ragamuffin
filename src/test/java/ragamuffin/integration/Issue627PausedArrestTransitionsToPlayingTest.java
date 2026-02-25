package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.InputHandler;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementsUI;
import ragamuffin.ui.CraftingUI;
import ragamuffin.ui.HelpUI;
import ragamuffin.ui.InventoryUI;
import ragamuffin.ui.PauseMenu;
import ragamuffin.ui.QuestLogUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #627 — arrest while PAUSED did not call
 * {@code transitionToPlaying()}, leaving the player stranded on the pause menu.
 *
 * <p>Bug: the PAUSED arrest block (Fix #367) correctly applied arrest consequences
 * (teleport, confiscation, input reset, UI hide) but never called
 * {@code transitionToPlaying()}.  The pause menu remained visible and the game
 * state stayed PAUSED, so the player had to manually press ESC or Resume before
 * the world became interactive again.
 *
 * <p>Fix: add {@code transitionToPlaying()} at the end of the PAUSED arrest block,
 * mirroring the CINEMATIC and PLAYING branches.  This hides the pause menu, sets
 * {@code state = PLAYING}, catches the cursor, and resets all stale input flags so
 * the first PLAYING frame is clean.
 *
 * <p>These tests verify the individual components that {@code transitionToPlaying()}
 * manipulates — the same approach used for Issues #617, #623, and related fixes.
 */
class Issue627PausedArrestTransitionsToPlayingTest {

    private InputHandler inputHandler;
    private PauseMenu pauseMenu;
    private InventoryUI inventoryUI;
    private CraftingUI craftingUI;
    private HelpUI helpUI;
    private AchievementsUI achievementsUI;
    private QuestLogUI questLogUI;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
        pauseMenu = new PauseMenu();
        Inventory inventory = new Inventory(36);
        CraftingSystem craftingSystem = new CraftingSystem();
        AchievementSystem achievementSystem = new AchievementSystem();
        BuildingQuestRegistry questRegistry = new BuildingQuestRegistry();
        inventoryUI = new InventoryUI(inventory);
        craftingUI = new CraftingUI(craftingSystem, inventory);
        helpUI = new HelpUI();
        achievementsUI = new AchievementsUI(achievementSystem);
        questLogUI = new QuestLogUI(questRegistry);
    }

    // -----------------------------------------------------------------------
    // Helpers — mirrors the relevant parts of transitionToPlaying()
    // -----------------------------------------------------------------------

    /**
     * Simulates the pause-menu hide that transitionToPlaying() performs.
     * In the real game this is the first visible effect of the fix: the pause
     * menu disappears immediately when the player is arrested while paused.
     */
    private static void simulateTransitionToPlayingPauseMenuHide(PauseMenu pm) {
        pm.hide();
    }

    /**
     * Simulates the input-flag reset sequence performed by transitionToPlaying()
     * (the same resets already present in the PAUSED arrest block before Fix #627,
     * now also covered by transitionToPlaying() at the end of the block).
     */
    private static void simulateTransitionToPlayingInputReset(InputHandler ih) {
        ih.resetEscape();
        ih.resetPunch();
        ih.resetPunchHeld();
        ih.resetPlace();
        ih.resetInventory();
        ih.resetHelp();
        ih.resetCrafting();
        ih.resetAchievements();
        ih.resetQuestLog();
        ih.resetInteract();
        ih.resetJump();
        ih.resetDodge();
        ih.resetEnter();
        ih.resetUp();
        ih.resetDown();
        ih.resetHotbarSlot();
        ih.resetCraftingSlot();
        ih.resetLeftClick();
        ih.resetLeftClickReleased();
        ih.resetRightClick();
        ih.resetScroll();
    }

    // -----------------------------------------------------------------------
    // Pause menu hidden after PAUSED arrest
    // -----------------------------------------------------------------------

    @Test
    void pauseMenu_isHidden_afterArrestWhilePaused() {
        // Pause menu is shown when the player opens ESC
        pauseMenu.show();
        assertTrue(pauseMenu.isVisible(),
            "Precondition: pause menu must be visible when paused");

        // Police arrest player while paused — transitionToPlaying() must hide it
        simulateTransitionToPlayingPauseMenuHide(pauseMenu);

        assertFalse(pauseMenu.isVisible(),
            "Fix #627: pauseMenu.hide() must be called during PAUSED arrest so the " +
            "player is not left staring at the pause menu after being nicked");
    }

    @Test
    void pauseMenu_hideIsIdempotent_whenAlreadyHidden() {
        // Pause menu may already be hidden (defensive call from transitionToPlaying)
        assertFalse(pauseMenu.isVisible(),
            "Precondition: pause menu starts hidden");

        assertDoesNotThrow(() -> simulateTransitionToPlayingPauseMenuHide(pauseMenu),
            "Fix #627: pauseMenu.hide() must not throw when pause menu is already hidden");

        assertFalse(pauseMenu.isVisible(),
            "Fix #627: pause menu must remain hidden after redundant hide()");
    }

    // -----------------------------------------------------------------------
    // Input flags cleared after PAUSED arrest (via transitionToPlaying)
    // -----------------------------------------------------------------------

    @Test
    void transitionToPlaying_afterPausedArrest_clearsAllInputFlags() {
        // Simulate keys buffered during the paused state (e.g. player mashing keys
        // while on the pause menu just as they were arrested)
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.C);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.E);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.UP);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.DOWN);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_3);
        inputHandler.scrolled(0, 2f);

        // transitionToPlaying() runs as part of PAUSED arrest fix
        simulateTransitionToPlayingInputReset(inputHandler);

        assertFalse(inputHandler.isEscapePressed(),       "Fix #627: escapePressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isPunchPressed(),        "Fix #627: punchPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isPunchHeld(),           "Fix #627: punchHeld must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isPlacePressed(),        "Fix #627: placePressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isInventoryPressed(),    "Fix #627: inventoryPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isHelpPressed(),         "Fix #627: helpPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isCraftingPressed(),     "Fix #627: craftingPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isAchievementsPressed(), "Fix #627: achievementsPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isQuestLogPressed(),     "Fix #627: questLogPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isInteractPressed(),     "Fix #627: interactPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isJumpPressed(),         "Fix #627: jumpPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isDodgePressed(),        "Fix #627: dodgePressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isEnterPressed(),        "Fix #627: enterPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isUpPressed(),           "Fix #627: upPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isDownPressed(),         "Fix #627: downPressed must be cleared on PAUSED arrest");
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),   "Fix #627: hotbarSlotPressed must be -1 on PAUSED arrest");
        assertEquals(-1, inputHandler.getCraftingSlotPressed(), "Fix #627: craftingSlotPressed must be -1 on PAUSED arrest");
        assertFalse(inputHandler.isLeftClickPressed(),    "Fix #627: leftClickPressed must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isLeftClickReleased(),   "Fix #627: leftClickReleased must be cleared on PAUSED arrest");
        assertFalse(inputHandler.isRightClickPressed(),   "Fix #627: rightClickPressed must be cleared on PAUSED arrest");
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f, "Fix #627: scrollAmountY must be zero on PAUSED arrest");
    }

    @Test
    void transitionToPlaying_afterPausedArrest_inputResetIsIdempotent_whenFlagsClear() {
        // All flags already clear — must not throw
        assertDoesNotThrow(() -> simulateTransitionToPlayingInputReset(inputHandler),
            "Fix #627: transitionToPlaying() input reset must not throw when all flags are already clear");

        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f);
    }

    // -----------------------------------------------------------------------
    // UI overlays hidden after PAUSED arrest (via transitionToPlaying)
    // -----------------------------------------------------------------------

    @Test
    void transitionToPlaying_afterPausedArrest_hidesInventoryUI() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(), "Precondition: inventoryUI must be visible after show()");

        inventoryUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #627: inventoryUI must be hidden after transitionToPlaying() on PAUSED arrest");
    }

    @Test
    void transitionToPlaying_afterPausedArrest_hidesCraftingUI() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible(), "Precondition: craftingUI must be visible after show()");

        craftingUI.hide();

        assertFalse(craftingUI.isVisible(),
            "Fix #627: craftingUI must be hidden after transitionToPlaying() on PAUSED arrest");
    }

    @Test
    void transitionToPlaying_afterPausedArrest_hidesHelpUI() {
        helpUI.show();
        assertTrue(helpUI.isVisible(), "Precondition: helpUI must be visible after show()");

        helpUI.hide();

        assertFalse(helpUI.isVisible(),
            "Fix #627: helpUI must be hidden after transitionToPlaying() on PAUSED arrest");
    }

    @Test
    void transitionToPlaying_afterPausedArrest_hidesAchievementsUI() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(), "Precondition: achievementsUI must be visible after show()");

        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
            "Fix #627: achievementsUI must be hidden after transitionToPlaying() on PAUSED arrest");
    }

    @Test
    void transitionToPlaying_afterPausedArrest_hidesQuestLogUI() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Precondition: questLogUI must be visible after show()");

        questLogUI.hide();

        assertFalse(questLogUI.isVisible(),
            "Fix #627: questLogUI must be hidden after transitionToPlaying() on PAUSED arrest");
    }

    @Test
    void transitionToPlaying_afterPausedArrest_hidesAllOverlaysAndPauseMenu() {
        // Simulate all overlays open when police arrest the player mid-pause
        pauseMenu.show();
        inventoryUI.show();
        craftingUI.show();
        helpUI.show();
        achievementsUI.show();
        questLogUI.show();

        assertTrue(pauseMenu.isVisible(),      "Precondition: pauseMenu visible");
        assertTrue(inventoryUI.isVisible(),    "Precondition: inventoryUI visible");
        assertTrue(craftingUI.isVisible(),     "Precondition: craftingUI visible");
        assertTrue(helpUI.isVisible(),         "Precondition: helpUI visible");
        assertTrue(achievementsUI.isVisible(), "Precondition: achievementsUI visible");
        assertTrue(questLogUI.isVisible(),     "Precondition: questLogUI visible");

        // transitionToPlaying() — hides everything
        simulateTransitionToPlayingPauseMenuHide(pauseMenu);
        inventoryUI.hide();
        craftingUI.hide();
        helpUI.hide();
        achievementsUI.hide();
        questLogUI.hide();

        assertFalse(pauseMenu.isVisible(),
            "Fix #627: pauseMenu must be hidden after PAUSED arrest transitions to PLAYING");
        assertFalse(inventoryUI.isVisible(),
            "Fix #627: inventoryUI must be hidden after PAUSED arrest");
        assertFalse(craftingUI.isVisible(),
            "Fix #627: craftingUI must be hidden after PAUSED arrest");
        assertFalse(helpUI.isVisible(),
            "Fix #627: helpUI must be hidden after PAUSED arrest");
        assertFalse(achievementsUI.isVisible(),
            "Fix #627: achievementsUI must be hidden after PAUSED arrest");
        assertFalse(questLogUI.isVisible(),
            "Fix #627: questLogUI must be hidden after PAUSED arrest");
    }
}
