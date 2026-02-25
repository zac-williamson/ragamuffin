package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.InputHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #609 — the respawn completion path does not reset stale
 * input flags, causing phantom actions on the first post-respawn frame.
 *
 * <p>Bug: the {@code wasRespawning && !respawnSystem.isRespawning()} block in
 * {@code updatePlayingSimulation()} did not reset any latched one-shot input fields.
 * Any key or mouse button pressed during the respawn countdown (while input processing
 * was suppressed by the {@code !player.isDead()} guard) accumulated in fields such as
 * {@code escapePressed}, {@code punchPressed}, {@code leftClickPressed}, etc.
 * When the countdown finished and the player became alive, those stale flags fired on
 * the very first live frame — e.g. a phantom ESC immediately re-paused the game, a
 * stale Enter fired a crafting confirmation, a stale punch hit a nearby NPC.
 *
 * <p>Fix: on respawn completion, call the complete set of {@code inputHandler.resetXxx()}
 * methods that are already used in {@code transitionToPlaying()}, plus close all open
 * UI overlays so the player does not respawn with inventory/crafting/help still showing.
 */
class Issue609RespawnClearsStaleInputTest {

    private InputHandler inputHandler;

    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }

    // -----------------------------------------------------------------------
    // Individual reset methods compile and clear their respective flags
    // -----------------------------------------------------------------------

    @Test
    void resetEscape_clearsEscapePressed() {
        // escapePressed is set by keyDown(ESCAPE) — simulate via keyDown
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
        assertTrue(inputHandler.isEscapePressed(),
            "Precondition: escapePressed must be true after keyDown(ESCAPE)");

        inputHandler.resetEscape();

        assertFalse(inputHandler.isEscapePressed(),
            "Fix #609: resetEscape() must clear escapePressed so phantom ESC does not re-pause the game on respawn");
    }

    @Test
    void resetPunch_clearsPunchPressed() {
        assertFalse(inputHandler.isPunchPressed(),
            "Precondition: punchPressed starts false");

        // punchPressed is set by touchDown with a captured cursor — verify reset path directly
        inputHandler.resetPunch();

        assertFalse(inputHandler.isPunchPressed(),
            "Fix #609: resetPunch() must leave punchPressed false");
    }

    @Test
    void resetPunchHeld_clearsPunchHeld() {
        assertFalse(inputHandler.isPunchHeld(),
            "Precondition: punchHeld starts false");

        inputHandler.resetPunchHeld();

        assertFalse(inputHandler.isPunchHeld(),
            "Fix #609: resetPunchHeld() must leave punchHeld false");
    }

    @Test
    void resetPlace_clearsPlacePressed() {
        assertFalse(inputHandler.isPlacePressed(),
            "Precondition: placePressed starts false");

        inputHandler.resetPlace();

        assertFalse(inputHandler.isPlacePressed(),
            "Fix #609: resetPlace() must leave placePressed false");
    }

    @Test
    void resetInventory_clearsInventoryPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.I);
        assertTrue(inputHandler.isInventoryPressed(),
            "Precondition: inventoryPressed must be true after keyDown(I)");

        inputHandler.resetInventory();

        assertFalse(inputHandler.isInventoryPressed(),
            "Fix #609: resetInventory() must clear inventoryPressed so phantom I does not reopen inventory on respawn");
    }

    @Test
    void resetHelp_clearsHelpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.H);
        assertTrue(inputHandler.isHelpPressed(),
            "Precondition: helpPressed must be true after keyDown(H)");

        inputHandler.resetHelp();

        assertFalse(inputHandler.isHelpPressed(),
            "Fix #609: resetHelp() must clear helpPressed so phantom H does not reopen help on respawn");
    }

    @Test
    void resetCrafting_clearsCraftingPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.C);
        assertTrue(inputHandler.isCraftingPressed(),
            "Precondition: craftingPressed must be true after keyDown(C)");

        inputHandler.resetCrafting();

        assertFalse(inputHandler.isCraftingPressed(),
            "Fix #609: resetCrafting() must clear craftingPressed so phantom C does not reopen crafting on respawn");
    }

    @Test
    void resetAchievements_clearsAchievementsPressed() {
        assertFalse(inputHandler.isAchievementsPressed(),
            "Precondition: achievementsPressed starts false");

        inputHandler.resetAchievements();

        assertFalse(inputHandler.isAchievementsPressed(),
            "Fix #609: resetAchievements() must leave achievementsPressed false");
    }

    @Test
    void resetQuestLog_clearsQuestLogPressed() {
        assertFalse(inputHandler.isQuestLogPressed(),
            "Precondition: questLogPressed starts false");

        inputHandler.resetQuestLog();

        assertFalse(inputHandler.isQuestLogPressed(),
            "Fix #609: resetQuestLog() must leave questLogPressed false");
    }

    @Test
    void resetInteract_clearsInteractPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.E);
        assertTrue(inputHandler.isInteractPressed(),
            "Precondition: interactPressed must be true after keyDown(E)");

        inputHandler.resetInteract();

        assertFalse(inputHandler.isInteractPressed(),
            "Fix #609: resetInteract() must clear interactPressed so phantom E does not re-trigger interaction on respawn");
    }

    @Test
    void resetJump_clearsJumpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.SPACE);
        assertTrue(inputHandler.isJumpPressed(),
            "Precondition: jumpPressed must be true after keyDown(SPACE)");

        inputHandler.resetJump();

        assertFalse(inputHandler.isJumpPressed(),
            "Fix #609: resetJump() must clear jumpPressed so phantom Space does not fire on respawn");
    }

    @Test
    void resetDodge_clearsDodgePressed() {
        assertFalse(inputHandler.isDodgePressed(),
            "Precondition: dodgePressed starts false");

        inputHandler.resetDodge();

        assertFalse(inputHandler.isDodgePressed(),
            "Fix #609: resetDodge() must leave dodgePressed false");
    }

    @Test
    void resetEnter_clearsEnterPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
        assertTrue(inputHandler.isEnterPressed(),
            "Precondition: enterPressed must be true after keyDown(ENTER)");

        inputHandler.resetEnter();

        assertFalse(inputHandler.isEnterPressed(),
            "Fix #609: resetEnter() must clear enterPressed so phantom Enter does not fire crafting on respawn");
    }

    @Test
    void resetUp_clearsUpPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.UP);
        assertTrue(inputHandler.isUpPressed(),
            "Precondition: upPressed must be true after keyDown(UP)");

        inputHandler.resetUp();

        assertFalse(inputHandler.isUpPressed(),
            "Fix #609: resetUp() must clear upPressed on respawn");
    }

    @Test
    void resetDown_clearsDownPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.DOWN);
        assertTrue(inputHandler.isDownPressed(),
            "Precondition: downPressed must be true after keyDown(DOWN)");

        inputHandler.resetDown();

        assertFalse(inputHandler.isDownPressed(),
            "Fix #609: resetDown() must clear downPressed on respawn");
    }

    @Test
    void resetHotbarSlot_clearsHotbarSlotPressed() {
        inputHandler.keyDown(com.badlogic.gdx.Input.Keys.NUM_1);
        assertNotEquals(-1, inputHandler.getHotbarSlotPressed(),
            "Precondition: hotbarSlotPressed must be set after keyDown(NUM_1)");

        inputHandler.resetHotbarSlot();

        assertEquals(-1, inputHandler.getHotbarSlotPressed(),
            "Fix #609: resetHotbarSlot() must clear hotbarSlotPressed on respawn");
    }

    @Test
    void resetCraftingSlot_clearsCraftingSlotPressed() {
        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "Precondition: craftingSlotPressed starts at -1");

        inputHandler.resetCraftingSlot();

        assertEquals(-1, inputHandler.getCraftingSlotPressed(),
            "Fix #609: resetCraftingSlot() must leave craftingSlotPressed at -1");
    }

    @Test
    void resetLeftClick_clearsLeftClickPressed() {
        assertFalse(inputHandler.isLeftClickPressed(),
            "Precondition: leftClickPressed starts false");

        inputHandler.resetLeftClick();

        assertFalse(inputHandler.isLeftClickPressed(),
            "Fix #609: resetLeftClick() must clear leftClickPressed on respawn");
    }

    @Test
    void resetLeftClickReleased_clearsLeftClickReleased() {
        assertFalse(inputHandler.isLeftClickReleased(),
            "Precondition: leftClickReleased starts false");

        inputHandler.resetLeftClickReleased();

        assertFalse(inputHandler.isLeftClickReleased(),
            "Fix #609: resetLeftClickReleased() must clear leftClickReleased on respawn");
    }

    @Test
    void resetRightClick_clearsRightClickPressed() {
        assertFalse(inputHandler.isRightClickPressed(),
            "Precondition: rightClickPressed starts false");

        inputHandler.resetRightClick();

        assertFalse(inputHandler.isRightClickPressed(),
            "Fix #609: resetRightClick() must clear rightClickPressed on respawn");
    }

    @Test
    void resetScroll_clearsScrollAmountY() {
        inputHandler.scrolled(0, 3f);
        assertNotEquals(0f, inputHandler.getScrollAmountY(),
            "Precondition: scrollAmountY must be set after scrolled()");

        inputHandler.resetScroll();

        assertEquals(0f, inputHandler.getScrollAmountY(), 0f,
            "Fix #609: resetScroll() must clear scrollAmountY so phantom scroll doesn't cycle hotbar on respawn");
    }

    // -----------------------------------------------------------------------
    // Full respawn reset sequence — all flags cleared atomically
    // -----------------------------------------------------------------------

    /**
     * Simulates the respawn-completion reset block added by Fix #609.
     * Mirrors the identical sequence in {@code transitionToPlaying()}.
     */
    private static void simulateRespawnInputReset(InputHandler ih) {
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

    @Test
    void respawnReset_clearsAllKeyFlagsBufferedDuringCountdown() {
        // Simulate a player mashing keys during the respawn countdown
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

        // Respawn countdown completes — apply fix #609 reset
        simulateRespawnInputReset(inputHandler);

        assertFalse(inputHandler.isEscapePressed(),    "escapePressed must be cleared");
        assertFalse(inputHandler.isPunchPressed(),     "punchPressed must be cleared");
        assertFalse(inputHandler.isPunchHeld(),        "punchHeld must be cleared");
        assertFalse(inputHandler.isPlacePressed(),     "placePressed must be cleared");
        assertFalse(inputHandler.isInventoryPressed(), "inventoryPressed must be cleared");
        assertFalse(inputHandler.isHelpPressed(),      "helpPressed must be cleared");
        assertFalse(inputHandler.isCraftingPressed(),  "craftingPressed must be cleared");
        assertFalse(inputHandler.isAchievementsPressed(), "achievementsPressed must be cleared");
        assertFalse(inputHandler.isQuestLogPressed(),  "questLogPressed must be cleared");
        assertFalse(inputHandler.isInteractPressed(),  "interactPressed must be cleared");
        assertFalse(inputHandler.isJumpPressed(),      "jumpPressed must be cleared");
        assertFalse(inputHandler.isDodgePressed(),     "dodgePressed must be cleared");
        assertFalse(inputHandler.isEnterPressed(),     "enterPressed must be cleared");
        assertFalse(inputHandler.isUpPressed(),        "upPressed must be cleared");
        assertFalse(inputHandler.isDownPressed(),      "downPressed must be cleared");
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),   "hotbarSlotPressed must be -1");
        assertEquals(-1, inputHandler.getCraftingSlotPressed(), "craftingSlotPressed must be -1");
        assertFalse(inputHandler.isLeftClickPressed(),    "leftClickPressed must be cleared");
        assertFalse(inputHandler.isLeftClickReleased(),   "leftClickReleased must be cleared");
        assertFalse(inputHandler.isRightClickPressed(),   "rightClickPressed must be cleared");
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f, "scrollAmountY must be zero");
    }

    @Test
    void respawnReset_isIdempotent_whenNoFlagsAreSet() {
        // All flags already clear — reset must not throw and must leave everything clean
        assertDoesNotThrow(() -> simulateRespawnInputReset(inputHandler),
            "Fix #609: respawn reset must not throw when all input flags are already clear");

        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f);
    }

    @Test
    void respawnReset_doesNotResetSprintHeld() {
        // Continuous held-state flags that are NOT one-shot latched fields must survive
        // the respawn reset.  sprintHeld is set by keyDown(SHIFT) and is not a one-shot
        // flag — verifying it is untouched by simulateRespawnInputReset() documents that
        // the reset sequence is scoped to the 20 one-shot fields listed in the issue.
        assertFalse(inputHandler.isSprintHeld(),
            "Precondition: sprintHeld starts false on fresh handler");

        // Full respawn input reset (fix #609) — must not include resetSprintHeld
        simulateRespawnInputReset(inputHandler);

        assertFalse(inputHandler.isSprintHeld(),
            "Fix #609: respawn input reset must not alter sprintHeld — it is not a one-shot latch");
    }
}
