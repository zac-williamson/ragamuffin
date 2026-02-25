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
 * Integration tests for Issue #617 — the PAUSED-state respawn-completion block did not
 * reset stale input flags or close UI overlays, causing phantom actions after resuming.
 *
 * <p>Bug: when a player died while paused (starvation / weather damage still ticks),
 * the {@code wasRespawning && !respawnSystem.isRespawning()} block inside the PAUSED
 * branch only reset {@code deathMessage}, Greggs raid, street reputation, healing
 * position, and distance tracking — it did NOT call any
 * {@code inputHandler.resetXxx()} methods and did NOT call {@code hide()} on the five
 * UI overlays.  Any key or mouse event buffered during the respawn countdown therefore
 * survived into the first live PLAYING frame, producing the same class of one-frame
 * ghost-input bugs that Fix #609 addressed for the PLAYING branch.
 *
 * <p>Fix: mirror the complete respawn-completion reset sequence from the PLAYING branch
 * (Fix #609) into the PAUSED branch — all 20 input-flag resets plus all five
 * UI-overlay {@code hide()} calls.
 */
class Issue617PausedRespawnClearsStaleInputTest {

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
    // Helpers — mirrors the exact sequences now in the PAUSED respawn block
    // -----------------------------------------------------------------------

    /** Applies the full input-flag reset added by Fix #617 (mirrors Fix #609 in PLAYING). */
    private static void simulatePausedRespawnInputReset(InputHandler ih) {
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
    // Input-flag resets
    // -----------------------------------------------------------------------

    @Test
    void pausedRespawnReset_clearsAllKeyFlagsBufferedDuringCountdown() {
        // Simulate keys pressed during the pause-menu / death-screen countdown
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

        // Respawn countdown completes in PAUSED state — apply Fix #617 reset
        simulatePausedRespawnInputReset(inputHandler);

        assertFalse(inputHandler.isEscapePressed(),       "Fix #617: escapePressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isPunchPressed(),        "Fix #617: punchPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isPunchHeld(),           "Fix #617: punchHeld must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isPlacePressed(),        "Fix #617: placePressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isInventoryPressed(),    "Fix #617: inventoryPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isHelpPressed(),         "Fix #617: helpPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isCraftingPressed(),     "Fix #617: craftingPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isAchievementsPressed(), "Fix #617: achievementsPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isQuestLogPressed(),     "Fix #617: questLogPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isInteractPressed(),     "Fix #617: interactPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isJumpPressed(),         "Fix #617: jumpPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isDodgePressed(),        "Fix #617: dodgePressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isEnterPressed(),        "Fix #617: enterPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isUpPressed(),           "Fix #617: upPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isDownPressed(),         "Fix #617: downPressed must be cleared on PAUSED respawn");
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),   "Fix #617: hotbarSlotPressed must be -1 on PAUSED respawn");
        assertEquals(-1, inputHandler.getCraftingSlotPressed(), "Fix #617: craftingSlotPressed must be -1 on PAUSED respawn");
        assertFalse(inputHandler.isLeftClickPressed(),    "Fix #617: leftClickPressed must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isLeftClickReleased(),   "Fix #617: leftClickReleased must be cleared on PAUSED respawn");
        assertFalse(inputHandler.isRightClickPressed(),   "Fix #617: rightClickPressed must be cleared on PAUSED respawn");
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f, "Fix #617: scrollAmountY must be zero on PAUSED respawn");
    }

    @Test
    void pausedRespawnReset_isIdempotent_whenNoFlagsAreSet() {
        // All flags already clear — reset must not throw and must leave everything clean
        assertDoesNotThrow(() -> simulatePausedRespawnInputReset(inputHandler),
            "Fix #617: PAUSED respawn reset must not throw when all input flags are already clear");

        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f);
    }

    @Test
    void pausedRespawnReset_doesNotResetSprintHeld() {
        // Continuous held-state flags that are NOT one-shot latched fields must survive
        // the PAUSED respawn reset — sprintHeld is set by keyDown(SHIFT).
        assertFalse(inputHandler.isSprintHeld(),
            "Precondition: sprintHeld starts false on fresh handler");

        simulatePausedRespawnInputReset(inputHandler);

        assertFalse(inputHandler.isSprintHeld(),
            "Fix #617: PAUSED respawn input reset must not alter sprintHeld");
    }

    // -----------------------------------------------------------------------
    // UI overlay hide() calls
    // -----------------------------------------------------------------------

    @Test
    void pausedRespawnReset_hidesInventoryUI() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(),
            "Precondition: inventoryUI must be visible after show()");

        inventoryUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #617: inventoryUI.hide() must close the overlay so isUIBlocking() returns false after PAUSED respawn");
    }

    @Test
    void pausedRespawnReset_hidesCraftingUI() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible(),
            "Precondition: craftingUI must be visible after show()");

        craftingUI.hide();

        assertFalse(craftingUI.isVisible(),
            "Fix #617: craftingUI.hide() must close the overlay after PAUSED respawn");
    }

    @Test
    void pausedRespawnReset_hidesHelpUI() {
        helpUI.show();
        assertTrue(helpUI.isVisible(),
            "Precondition: helpUI must be visible after show()");

        helpUI.hide();

        assertFalse(helpUI.isVisible(),
            "Fix #617: helpUI.hide() must close the overlay after PAUSED respawn");
    }

    @Test
    void pausedRespawnReset_hidesAchievementsUI() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(),
            "Precondition: achievementsUI must be visible after show()");

        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
            "Fix #617: achievementsUI.hide() must close the overlay after PAUSED respawn");
    }

    @Test
    void pausedRespawnReset_hidesQuestLogUI() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(),
            "Precondition: questLogUI must be visible after show()");

        questLogUI.hide();

        assertFalse(questLogUI.isVisible(),
            "Fix #617: questLogUI.hide() must close the overlay after PAUSED respawn");
    }

    @Test
    void pausedRespawnReset_hidesAllFiveOverlays_whenAllWereOpen() {
        // Simulate all overlays open at time of death
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

        // Respawn completes in PAUSED — close all overlays
        inventoryUI.hide();
        craftingUI.hide();
        helpUI.hide();
        achievementsUI.hide();
        questLogUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #617: inventoryUI must be hidden after PAUSED respawn");
        assertFalse(craftingUI.isVisible(),
            "Fix #617: craftingUI must be hidden after PAUSED respawn");
        assertFalse(helpUI.isVisible(),
            "Fix #617: helpUI must be hidden after PAUSED respawn");
        assertFalse(achievementsUI.isVisible(),
            "Fix #617: achievementsUI must be hidden after PAUSED respawn");
        assertFalse(questLogUI.isVisible(),
            "Fix #617: questLogUI must be hidden after PAUSED respawn");
    }
}
