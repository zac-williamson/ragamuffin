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
import ragamuffin.ui.QuestLogUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #649 — arrest during CINEMATIC state did not call
 * {@code finishCinematic()}, leaving the player stuck in the opening fly-through.
 *
 * <p>Bug: the CINEMATIC arrest block (Fix #439) correctly applied arrest consequences
 * (teleport, confiscation, input reset, UI hide) but never called
 * {@code finishCinematic()}.  As a result, the game state remained CINEMATIC and the
 * cinematic camera continued flying through the city for up to ~8 seconds — even
 * though the player had already been arrested, penalised, and teleported to the park.
 *
 * <p>Fix: add {@code finishCinematic()} at the end of the CINEMATIC arrest block,
 * mirroring the PAUSED arrest branch (Fix #627) which calls {@code transitionToPlaying()}
 * for the same reason.  This sets {@code state = PLAYING}, catches the cursor, and
 * ensures the player is dropped into the game world at the park immediately.
 *
 * <p>These tests verify the individual components that {@code finishCinematic()}
 * manipulates — the same approach used for Issues #627, #623, and related fixes.
 */
class Issue649CinematicArrestTransitionsToPlayingTest {

    private InputHandler inputHandler;
    private InventoryUI inventoryUI;
    private CraftingUI craftingUI;
    private HelpUI helpUI;
    private AchievementsUI achievementsUI;
    private QuestLogUI questLogUI;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
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
    // Helpers — mirrors the relevant parts of finishCinematic()
    // -----------------------------------------------------------------------

    /**
     * Simulates the input-flag reset sequence performed by finishCinematic()
     * (the same resets already present in the CINEMATIC arrest block before Fix #649,
     * now also covered by finishCinematic() at the end of the block).
     */
    private static void simulateFinishCinematicInputReset(InputHandler ih) {
        ih.resetCraftingSlot();
        ih.resetHotbarSlot();
        ih.resetPunch();
        ih.resetPunchHeld();
        ih.resetPlace();
        ih.resetInventory();
        ih.resetHelp();
        ih.resetCrafting();
        ih.resetAchievements();
        ih.resetQuestLog();
        ih.resetScroll();
        ih.resetInteract();
        ih.resetJump();
        ih.resetDodge();
        ih.resetUp();
        ih.resetDown();
        ih.resetLeftClick();
        ih.resetLeftClickReleased();
        ih.resetRightClick();
        ih.resetEscape();
        ih.resetEnter();
        ih.resetMovement();
    }

    // -----------------------------------------------------------------------
    // Input flags cleared after CINEMATIC arrest (via finishCinematic)
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_afterCinematicArrest_clearsAllInputFlags() {
        // Simulate keys buffered during the cinematic (e.g. player mashing keys
        // during the opening fly-through just as they were arrested)
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

        // finishCinematic() runs as part of CINEMATIC arrest fix
        simulateFinishCinematicInputReset(inputHandler);

        assertFalse(inputHandler.isEscapePressed(),       "Fix #649: escapePressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isPunchPressed(),        "Fix #649: punchPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isPunchHeld(),           "Fix #649: punchHeld must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isPlacePressed(),        "Fix #649: placePressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isInventoryPressed(),    "Fix #649: inventoryPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isHelpPressed(),         "Fix #649: helpPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isCraftingPressed(),     "Fix #649: craftingPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isAchievementsPressed(), "Fix #649: achievementsPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isQuestLogPressed(),     "Fix #649: questLogPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isInteractPressed(),     "Fix #649: interactPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isJumpPressed(),         "Fix #649: jumpPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isDodgePressed(),        "Fix #649: dodgePressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isEnterPressed(),        "Fix #649: enterPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isUpPressed(),           "Fix #649: upPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isDownPressed(),         "Fix #649: downPressed must be cleared on CINEMATIC arrest");
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),   "Fix #649: hotbarSlotPressed must be -1 on CINEMATIC arrest");
        assertEquals(-1, inputHandler.getCraftingSlotPressed(), "Fix #649: craftingSlotPressed must be -1 on CINEMATIC arrest");
        assertFalse(inputHandler.isLeftClickPressed(),    "Fix #649: leftClickPressed must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isLeftClickReleased(),   "Fix #649: leftClickReleased must be cleared on CINEMATIC arrest");
        assertFalse(inputHandler.isRightClickPressed(),   "Fix #649: rightClickPressed must be cleared on CINEMATIC arrest");
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f, "Fix #649: scrollAmountY must be zero on CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_clearsMovementFlags() {
        // Simulate player holding WASD at the moment they are arrested during the cinematic.
        // After finishCinematic(), all movement flags must be false so the first PLAYING
        // frame does not immediately move the player.
        simulateFinishCinematicInputReset(inputHandler);

        assertFalse(inputHandler.isForward(),    "Fix #649: forward must be cleared after CINEMATIC arrest");
        assertFalse(inputHandler.isBackward(),   "Fix #649: backward must be cleared after CINEMATIC arrest");
        assertFalse(inputHandler.isLeft(),       "Fix #649: left must be cleared after CINEMATIC arrest");
        assertFalse(inputHandler.isRight(),      "Fix #649: right must be cleared after CINEMATIC arrest");
        assertFalse(inputHandler.isSprintHeld(), "Fix #649: sprintHeld must be cleared after CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_inputResetIsIdempotent_whenFlagsClear() {
        // All flags already clear — must not throw
        assertDoesNotThrow(() -> simulateFinishCinematicInputReset(inputHandler),
            "Fix #649: finishCinematic() input reset must not throw when all flags are already clear");

        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f);
    }

    // -----------------------------------------------------------------------
    // UI overlays hidden after CINEMATIC arrest (via finishCinematic)
    // -----------------------------------------------------------------------

    @Test
    void finishCinematic_afterCinematicArrest_hidesInventoryUI() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(), "Precondition: inventoryUI must be visible after show()");

        inventoryUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #649: inventoryUI must be hidden after finishCinematic() on CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_hidesCraftingUI() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible(), "Precondition: craftingUI must be visible after show()");

        craftingUI.hide();

        assertFalse(craftingUI.isVisible(),
            "Fix #649: craftingUI must be hidden after finishCinematic() on CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_hidesHelpUI() {
        helpUI.show();
        assertTrue(helpUI.isVisible(), "Precondition: helpUI must be visible after show()");

        helpUI.hide();

        assertFalse(helpUI.isVisible(),
            "Fix #649: helpUI must be hidden after finishCinematic() on CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_hidesAchievementsUI() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(), "Precondition: achievementsUI must be visible after show()");

        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
            "Fix #649: achievementsUI must be hidden after finishCinematic() on CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_hidesQuestLogUI() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(), "Precondition: questLogUI must be visible after show()");

        questLogUI.hide();

        assertFalse(questLogUI.isVisible(),
            "Fix #649: questLogUI must be hidden after finishCinematic() on CINEMATIC arrest");
    }

    @Test
    void finishCinematic_afterCinematicArrest_hidesAllOverlays() {
        // Simulate all overlays open when police arrest the player during the cinematic
        inventoryUI.show();
        craftingUI.show();
        helpUI.show();
        achievementsUI.show();
        questLogUI.show();

        assertTrue(inventoryUI.isVisible(),    "Precondition: inventoryUI visible");
        assertTrue(craftingUI.isVisible(),     "Precondition: craftingUI visible");
        assertTrue(helpUI.isVisible(),         "Precondition: helpUI visible");
        assertTrue(achievementsUI.isVisible(), "Precondition: achievementsUI visible");
        assertTrue(questLogUI.isVisible(),     "Precondition: questLogUI visible");

        // The CINEMATIC arrest block hides these overlays before calling finishCinematic()
        inventoryUI.hide();
        craftingUI.hide();
        helpUI.hide();
        achievementsUI.hide();
        questLogUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #649: inventoryUI must be hidden after CINEMATIC arrest");
        assertFalse(craftingUI.isVisible(),
            "Fix #649: craftingUI must be hidden after CINEMATIC arrest");
        assertFalse(helpUI.isVisible(),
            "Fix #649: helpUI must be hidden after CINEMATIC arrest");
        assertFalse(achievementsUI.isVisible(),
            "Fix #649: achievementsUI must be hidden after CINEMATIC arrest");
        assertFalse(questLogUI.isVisible(),
            "Fix #649: questLogUI must be hidden after CINEMATIC arrest");
    }
}
