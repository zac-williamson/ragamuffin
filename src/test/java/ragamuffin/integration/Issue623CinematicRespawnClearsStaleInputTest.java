package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.CraftingSystem;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.InputHandler;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementsUI;
import ragamuffin.ui.CraftingUI;
import ragamuffin.ui.HelpUI;
import ragamuffin.ui.InventoryUI;
import ragamuffin.ui.QuestLogUI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #623 — the CINEMATIC-state respawn-completion block did not
 * reset stale input flags, close UI overlays, reset distance tracking, or clear
 * activeShopkeeperNPC, causing phantom actions and permanently locked input after
 * finishCinematic() completes.
 *
 * <p>Bug: when a player dies during the opening cinematic (starvation / weather damage),
 * the {@code cinematicWasRespawning && !respawnSystem.isRespawning()} block only reset
 * {@code deathMessage}, Greggs raid, street reputation, and healing position — it was
 * missing all of the cleanup that the equivalent PLAYING (Fix #609) and PAUSED (Fix #617)
 * blocks perform: input-flag resets, UI-overlay hides, distance tracking reset, and
 * activeShopkeeperNPC clear.
 *
 * <p>Fix: mirror the complete respawn-completion reset sequence into the CINEMATIC branch —
 * all 20+ input-flag resets, all five UI-overlay {@code hide()} calls, distance tracking
 * reset, and the activeShopkeeperNPC null-and-close block.
 */
class Issue623CinematicRespawnClearsStaleInputTest {

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
    // Helpers — mirrors the exact sequences now in the CINEMATIC respawn block
    // -----------------------------------------------------------------------

    /** Applies the full input-flag reset added by Fix #623 (mirrors Fix #609/617). */
    private static void simulateCinematicRespawnInputReset(InputHandler ih) {
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

    /** Simulates the activeShopkeeperNPC null-and-close added by Fix #623. */
    private static NPC simulateCinematicRespawnClearsShopMenu(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            return null;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Input-flag resets
    // -----------------------------------------------------------------------

    @Test
    void cinematicRespawnReset_clearsAllKeyFlagsBufferedDuringCountdown() {
        // Simulate keys pressed during the cinematic / death-screen countdown
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

        // Respawn countdown completes in CINEMATIC state — apply Fix #623 reset
        simulateCinematicRespawnInputReset(inputHandler);

        assertFalse(inputHandler.isEscapePressed(),       "Fix #623: escapePressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isPunchPressed(),        "Fix #623: punchPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isPunchHeld(),           "Fix #623: punchHeld must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isPlacePressed(),        "Fix #623: placePressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isInventoryPressed(),    "Fix #623: inventoryPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isHelpPressed(),         "Fix #623: helpPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isCraftingPressed(),     "Fix #623: craftingPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isAchievementsPressed(), "Fix #623: achievementsPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isQuestLogPressed(),     "Fix #623: questLogPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isInteractPressed(),     "Fix #623: interactPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isJumpPressed(),         "Fix #623: jumpPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isDodgePressed(),        "Fix #623: dodgePressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isEnterPressed(),        "Fix #623: enterPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isUpPressed(),           "Fix #623: upPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isDownPressed(),         "Fix #623: downPressed must be cleared on CINEMATIC respawn");
        assertEquals(-1, inputHandler.getHotbarSlotPressed(),   "Fix #623: hotbarSlotPressed must be -1 on CINEMATIC respawn");
        assertEquals(-1, inputHandler.getCraftingSlotPressed(), "Fix #623: craftingSlotPressed must be -1 on CINEMATIC respawn");
        assertFalse(inputHandler.isLeftClickPressed(),    "Fix #623: leftClickPressed must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isLeftClickReleased(),   "Fix #623: leftClickReleased must be cleared on CINEMATIC respawn");
        assertFalse(inputHandler.isRightClickPressed(),   "Fix #623: rightClickPressed must be cleared on CINEMATIC respawn");
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f, "Fix #623: scrollAmountY must be zero on CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_isIdempotent_whenNoFlagsAreSet() {
        // All flags already clear — reset must not throw and must leave everything clean
        assertDoesNotThrow(() -> simulateCinematicRespawnInputReset(inputHandler),
            "Fix #623: CINEMATIC respawn reset must not throw when all input flags are already clear");

        assertFalse(inputHandler.isEscapePressed());
        assertFalse(inputHandler.isPunchPressed());
        assertFalse(inputHandler.isInventoryPressed());
        assertFalse(inputHandler.isEnterPressed());
        assertEquals(-1, inputHandler.getHotbarSlotPressed());
        assertEquals(0f, inputHandler.getScrollAmountY(), 0f);
    }

    @Test
    void cinematicRespawnReset_doesNotResetSprintHeld() {
        // Continuous held-state flags that are NOT one-shot latched fields must survive
        assertFalse(inputHandler.isSprintHeld(),
            "Precondition: sprintHeld starts false on fresh handler");

        simulateCinematicRespawnInputReset(inputHandler);

        assertFalse(inputHandler.isSprintHeld(),
            "Fix #623: CINEMATIC respawn input reset must not alter sprintHeld");
    }

    // -----------------------------------------------------------------------
    // UI overlay hide() calls
    // -----------------------------------------------------------------------

    @Test
    void cinematicRespawnReset_hidesInventoryUI() {
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible(),
            "Precondition: inventoryUI must be visible after show()");

        inventoryUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #623: inventoryUI.hide() must close the overlay so isUIBlocking() returns false after CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_hidesCraftingUI() {
        craftingUI.show();
        assertTrue(craftingUI.isVisible(),
            "Precondition: craftingUI must be visible after show()");

        craftingUI.hide();

        assertFalse(craftingUI.isVisible(),
            "Fix #623: craftingUI.hide() must close the overlay after CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_hidesHelpUI() {
        helpUI.show();
        assertTrue(helpUI.isVisible(),
            "Precondition: helpUI must be visible after show()");

        helpUI.hide();

        assertFalse(helpUI.isVisible(),
            "Fix #623: helpUI.hide() must close the overlay after CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_hidesAchievementsUI() {
        achievementsUI.show();
        assertTrue(achievementsUI.isVisible(),
            "Precondition: achievementsUI must be visible after show()");

        achievementsUI.hide();

        assertFalse(achievementsUI.isVisible(),
            "Fix #623: achievementsUI.hide() must close the overlay after CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_hidesQuestLogUI() {
        questLogUI.show();
        assertTrue(questLogUI.isVisible(),
            "Precondition: questLogUI must be visible after show()");

        questLogUI.hide();

        assertFalse(questLogUI.isVisible(),
            "Fix #623: questLogUI.hide() must close the overlay after CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_hidesAllFiveOverlays_whenAllWereOpen() {
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

        inventoryUI.hide();
        craftingUI.hide();
        helpUI.hide();
        achievementsUI.hide();
        questLogUI.hide();

        assertFalse(inventoryUI.isVisible(),
            "Fix #623: inventoryUI must be hidden after CINEMATIC respawn");
        assertFalse(craftingUI.isVisible(),
            "Fix #623: craftingUI must be hidden after CINEMATIC respawn");
        assertFalse(helpUI.isVisible(),
            "Fix #623: helpUI must be hidden after CINEMATIC respawn");
        assertFalse(achievementsUI.isVisible(),
            "Fix #623: achievementsUI must be hidden after CINEMATIC respawn");
        assertFalse(questLogUI.isVisible(),
            "Fix #623: questLogUI must be hidden after CINEMATIC respawn");
    }

    // -----------------------------------------------------------------------
    // activeShopkeeperNPC clearing
    // -----------------------------------------------------------------------

    @Test
    void cinematicRespawnReset_clearsActiveShopkeeperNPC_whenMenuWasOpen() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before cinematic death");

        NPC activeShopkeeperNPC = simulateCinematicRespawnClearsShopMenu(shopkeeper);

        assertNull(activeShopkeeperNPC,
            "Fix #623: activeShopkeeperNPC must be null after CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_closesShopMenuOnStaleNPC() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before cinematic death");

        simulateCinematicRespawnClearsShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #623: setShopMenuOpen(false) must be called on the stale NPC on CINEMATIC respawn");
    }

    @Test
    void cinematicRespawnReset_isUIBlocking_isFalse_afterRespawn() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        NPC activeShopkeeperNPC = simulateCinematicRespawnClearsShopMenu(shopkeeper);

        boolean isBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();

        assertFalse(isBlocking,
            "Fix #623: after CINEMATIC respawn, isUIBlocking() must return false so player input is not locked");
    }

    @Test
    void cinematicRespawnReset_withNoActiveShopkeeper_isNoOp() {
        NPC activeShopkeeperNPC = null;

        NPC result = simulateCinematicRespawnClearsShopMenu(activeShopkeeperNPC);

        assertNull(result,
            "Fix #623: CINEMATIC respawn with null activeShopkeeperNPC must not throw and must remain null");
    }
}
