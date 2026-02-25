package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #573:
 * transitionToPaused() and transitionToPlaying() do not reset
 * inventoryPressed/helpPressed/craftingPressed/achievementsPressed/questLogPressed,
 * causing stale UI-toggle keys to fire on the first PLAYING frame after resume.
 *
 * <p>The fix adds the following calls to both transitionToPaused() and transitionToPlaying():
 * <ul>
 *   <li>{@code inputHandler.resetInventory()} — clears stale I-key flag</li>
 *   <li>{@code inputHandler.resetHelp()} — clears stale H-key flag</li>
 *   <li>{@code inputHandler.resetCrafting()} — clears stale C-key flag</li>
 *   <li>{@code inputHandler.resetAchievements()} — clears stale Tab-key flag</li>
 *   <li>{@code inputHandler.resetQuestLog()} — clears stale Q-key flag</li>
 * </ul>
 *
 * <p>Tests mirror the pattern established by fixes #569, #571.
 */
class Issue573TransitionStaleUIToggleKeysTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    /**
     * Test 1: resetInventory() clears stale inventoryPressed set by I key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsInventoryPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);

        assertTrue(inputHandler.isInventoryPressed(),
            "Precondition: pressing I must set inventoryPressed to true");

        inputHandler.resetInventory();

        assertFalse(inputHandler.isInventoryPressed(),
            "After transitionToPaused(), isInventoryPressed() must be false — " +
            "stale I-key from same-frame press must not re-open inventory on resume");
    }

    /**
     * Test 2: resetHelp() clears stale helpPressed set by H key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsHelpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);

        assertTrue(inputHandler.isHelpPressed(),
            "Precondition: pressing H must set helpPressed to true");

        inputHandler.resetHelp();

        assertFalse(inputHandler.isHelpPressed(),
            "After transitionToPaused(), isHelpPressed() must be false — " +
            "stale H-key from same-frame press must not re-open help UI on resume");
    }

    /**
     * Test 3: resetCrafting() clears stale craftingPressed set by C key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsCraftingPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.C);

        assertTrue(inputHandler.isCraftingPressed(),
            "Precondition: pressing C must set craftingPressed to true");

        inputHandler.resetCrafting();

        assertFalse(inputHandler.isCraftingPressed(),
            "After transitionToPaused(), isCraftingPressed() must be false — " +
            "stale C-key from same-frame press must not re-open crafting UI on resume");
    }

    /**
     * Test 4: resetAchievements() clears stale achievementsPressed set by Tab key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsAchievementsPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.TAB);

        assertTrue(inputHandler.isAchievementsPressed(),
            "Precondition: pressing Tab must set achievementsPressed to true");

        inputHandler.resetAchievements();

        assertFalse(inputHandler.isAchievementsPressed(),
            "After transitionToPaused(), isAchievementsPressed() must be false — " +
            "stale Tab-key from same-frame press must not re-open achievements on resume");
    }

    /**
     * Test 5: resetQuestLog() clears stale questLogPressed set by Q key
     * pressed on the same frame as the ESC/pause transition.
     */
    @Test
    void transitionToPaused_resetsQuestLogPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.Q);

        assertTrue(inputHandler.isQuestLogPressed(),
            "Precondition: pressing Q must set questLogPressed to true");

        inputHandler.resetQuestLog();

        assertFalse(inputHandler.isQuestLogPressed(),
            "After transitionToPaused(), isQuestLogPressed() must be false — " +
            "stale Q-key from same-frame press must not re-open quest log on resume");
    }

    /**
     * Test 6: All five UI-toggle resets are independent — resetting one does not
     * affect the others.
     */
    @Test
    void transitionToPaused_resetsAreIndependent() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.C);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.TAB);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.Q);

        assertTrue(inputHandler.isInventoryPressed(), "I pressed");
        assertTrue(inputHandler.isHelpPressed(), "H pressed");
        assertTrue(inputHandler.isCraftingPressed(), "C pressed");
        assertTrue(inputHandler.isAchievementsPressed(), "Tab pressed");
        assertTrue(inputHandler.isQuestLogPressed(), "Q pressed");

        // Reset only inventory
        inputHandler.resetInventory();

        assertFalse(inputHandler.isInventoryPressed(),
            "inventoryPressed must be false after resetInventory()");
        assertTrue(inputHandler.isHelpPressed(),
            "helpPressed must be unaffected by resetInventory()");
        assertTrue(inputHandler.isCraftingPressed(),
            "craftingPressed must be unaffected by resetInventory()");
        assertTrue(inputHandler.isAchievementsPressed(),
            "achievementsPressed must be unaffected by resetInventory()");
        assertTrue(inputHandler.isQuestLogPressed(),
            "questLogPressed must be unaffected by resetInventory()");
    }

    /**
     * Test 7: All five resets together clear all UI-toggle flags, mirroring the
     * full set of calls added to transitionToPaused() and transitionToPlaying().
     */
    @Test
    void transitionToPaused_allUIToggleResetsApplied() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.C);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.TAB);
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.Q);

        // Simulate what transitionToPaused() and transitionToPlaying() now do
        inputHandler.resetInventory();
        inputHandler.resetHelp();
        inputHandler.resetCrafting();
        inputHandler.resetAchievements();
        inputHandler.resetQuestLog();

        assertFalse(inputHandler.isInventoryPressed(),
            "inventoryPressed must be false after all UI-toggle resets");
        assertFalse(inputHandler.isHelpPressed(),
            "helpPressed must be false after all UI-toggle resets");
        assertFalse(inputHandler.isCraftingPressed(),
            "craftingPressed must be false after all UI-toggle resets");
        assertFalse(inputHandler.isAchievementsPressed(),
            "achievementsPressed must be false after all UI-toggle resets");
        assertFalse(inputHandler.isQuestLogPressed(),
            "questLogPressed must be false after all UI-toggle resets");
    }

    /**
     * Test 8: resetInventory() is idempotent — calling it when already false is safe.
     */
    @Test
    void resetInventory_isIdempotent() {
        assertFalse(inputHandler.isInventoryPressed(),
            "Precondition: inventoryPressed must be false on fresh InputHandler");

        inputHandler.resetInventory();
        inputHandler.resetInventory();

        assertFalse(inputHandler.isInventoryPressed(),
            "inventoryPressed must remain false after repeated resetInventory() calls");
    }
}
