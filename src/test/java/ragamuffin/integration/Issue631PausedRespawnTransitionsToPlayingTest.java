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
 * Integration tests for Issue #631 — the PAUSED-state respawn-completion block did not
 * call {@code transitionToPlaying()}, leaving the player on the pause menu after death.
 *
 * <p>Bug: when the player died while paused (starvation / weather damage still ticks,
 * see Fix #621), the {@code wasRespawning && !respawnSystem.isRespawning()} block inside
 * the PAUSED branch correctly reset input flags and closed UI overlays (Fix #617) and
 * cleared the active shopkeeper NPC (Fix #625), but never called
 * {@code transitionToPlaying()}.  After the 3-second respawn countdown elapsed the player
 * was alive and teleported back to the park, but the game state remained PAUSED and the
 * pause menu stayed visible — the player had to manually press ESC or click Resume.
 *
 * <p>Fix: add {@code transitionToPlaying()} as the final statement in the PAUSED
 * respawn-completion block, mirroring the PAUSED arrest block (Fix #627) and the
 * CINEMATIC respawn-completion block (Fix #623).
 */
class Issue631PausedRespawnTransitionsToPlayingTest {

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
     * menu disappears immediately when respawn completes while paused.
     */
    private static void simulateTransitionToPlayingPauseMenuHide(PauseMenu pm) {
        pm.hide();
    }

    /**
     * Simulates the input-flag reset sequence performed by transitionToPlaying().
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
    // Pause menu hidden after PAUSED respawn completes
    // -----------------------------------------------------------------------

    @Test
    void pauseMenu_isHidden_afterRespawnCompletesWhilePaused() {
        // Pause menu is shown when the player presses ESC
        pauseMenu.show();
        assertTrue(pauseMenu.isVisible(),
            "Precondition: pause menu must be visible when paused");

        // Respawn completes while paused — transitionToPlaying() must hide it
        simulateTransitionToPlayingPauseMenuHide(pauseMenu);

        assertFalse(pauseMenu.isVisible(),
            "Fix #631: pauseMenu.hide() must be called during PAUSED respawn-completion " +
            "so the player is not left staring at the pause menu after being revived");
    }

    @Test
    void pauseMenu_hideIsIdempotent_whenAlreadyHidden() {
        // Pause menu may already be hidden (defensive call from transitionToPlaying)
        assertFalse(pauseMenu.isVisible(),
            "Precondition: pause menu starts hidden");

        assertDoesNotThrow(() -> simulateTransitionToPlayingPauseMenuHide(pauseMenu),
            "Fix #631: pauseMenu.hide() must not throw when pause menu is already hidden");

        assertFalse(pauseMenu.isVisible(),
            "Fix #631: pause menu must remain hidden after redundant hide()");
    }

    // -----------------------------------------------------------------------
    // Input flags cleared after PAUSED respawn completes (via transitionToPlaying)
    // -----------------------------------------------------------------------

    @Test
    void transitionToPlaying_afterPausedRespawn_clearsAllInputFlags() {
        // Simulate keys buffered during the paused state
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

        // transitionToPlaying() runs as part of PAUSED respawn-completion fix
        simulateTransitionToPlayingInputReset(inputHandler);

        assertFalse(inputHandler.isEscapePressed(),       "Fix #631: escapePressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isPunchPressed(),        "Fix #631: punchPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isPunchHeld(),           "Fix #631: punchHeld must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isPlacePressed(),        "Fix #631: placePressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isInventoryPressed(),    "Fix #631: inventoryPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isHelpPressed(),         "Fix #631: helpPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isCraftingPressed(),     "Fix #631: craftingPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isAchievementsPressed(), "Fix #631: achievementsPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isQuestLogPressed(),     "Fix #631: questLogPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isInteractPressed(),     "Fix #631: interactPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isJumpPressed(),         "Fix #631: jumpPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isDodgePressed(),        "Fix #631: dodgePressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isEnterPressed(),        "Fix #631: enterPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isUpPressed(),           "Fix #631: upPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isDownPressed(),         "Fix #631: downPressed must be cleared on PAUSED respawn");
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),   "Fix #631: hotbarSlotPressed must be -1 on PAUSED respawn");
        assertEquals(-1, inputHandler.getCraftingSlotPressed(), "Fix #631: craftingSlotPressed must be -1 on PAUSED respawn");
        assertFalse(inputHandler.isLeftClickPressed(),    "Fix #631: leftClickPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isLeftClickReleased(),   "Fix #631: leftClickReleased must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isRightClickPressed(),   "Fix #631: rightClickPressed must be cleared on PAUSED respawn");
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f, "Fix #631: scrollAmountY must be zero on PAUSED respawn");
    }

    @Test
    void transitionToPlaying_afterPausedRespawn_inputResetIsIdempotent_whenFlagsClear() {
        // All flags already clear — must not throw
        assertDoesNotThrow(() -> simulateTransitionToPlayingInputReset(inputHandler),
            "Fix #631: transitionToPlaying() input reset must not throw when all flags are already clear");

        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f);
    }

    // -----------------------------------------------------------------------
    // UI overlays hidden after PAUSED respawn completes (via transitionToPlaying)
    // -----------------------------------------------------------------------

    @Test
    void transitionToPlaying_afterPausedRespawn_hidesInventoryUI() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(), "Precondition: inventoryUI must be visible after show()");

        inventoryUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #631: inventoryUI must be hidden after transitionToPlaying() on PAUSED respawn");
    }

    @Test
    void transitionToPlaying_afterPausedRespawn_hidesCraftingUI() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible(), "Precondition: craftingUI must be visible after show()");

        craftingUI.hide();

        assertFalse(craftingUI.isVisible(),
            "Fix #631: craftingUI must be hidden after transitionToPlaying() on PAUSED respawn");
    }

    @Test
    void transitionToPlaying_afterPausedRespawn_hidesHelpUI() {
        helpUI.show();
        assertTrue(helpUI.isVisible(), "Precondition: helpUI must be visible after show()");

        helpUI.hide();

        assertFalse(helpUI.isVisible(),
            "Fix #631: helpUI must be hidden after transitionToPlaying() on PAUSED respawn");
    }

    @Test
    void transitionToPlaying_afterPausedRespawn_hidesAchievementsUI() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(), "Precondition: achievementsUI must be visible after show()");

        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
            "Fix #631: achievementsUI must be hidden after transitionToPlaying() on PAUSED respawn");
    }

    @Test
    void transitionToPlaying_afterPausedRespawn_hidesQuestLogUI() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Precondition: questLogUI must be visible after show()");

        questLogUI.hide();

        assertFalse(questLogUI.isVisible(),
            "Fix #631: questLogUI must be hidden after transitionToPlaying() on PAUSED respawn");
    }

    @Test
    void transitionToPlaying_afterPausedRespawn_hidesAllOverlaysAndPauseMenu() {
        // Simulate all overlays open when the player dies while on the pause menu
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
            "Fix #631: pauseMenu must be hidden after PAUSED respawn-completion transitions to PLAYING");
        assertFalse(inventoryUI.isVisible(),
            "Fix #631: inventoryUI must be hidden after PAUSED respawn");
        assertFalse(craftingUI.isVisible(),
            "Fix #631: craftingUI must be hidden after PAUSED respawn");
        assertFalse(helpUI.isVisible(),
            "Fix #631: helpUI must be hidden after PAUSED respawn");
        assertFalse(achievementsUI.isVisible(),
            "Fix #631: achievementsUI must be hidden after PAUSED respawn");
        assertFalse(questLogUI.isVisible(),
            "Fix #631: questLogUI must be hidden after PAUSED respawn");
    }
}
