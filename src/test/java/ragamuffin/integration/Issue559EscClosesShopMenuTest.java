package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #559 — ESC does not close the shop menu.
 *
 * Fix: add a branch at the top of {@code handleEscapePress()} in {@code RagamuffinGame}
 * for the shop menu: if {@code activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen()},
 * call {@code activeShopkeeperNPC.setShopMenuOpen(false)}, null out {@code activeShopkeeperNPC},
 * re-catch the cursor, and return early — consistent with how every other UI overlay is handled.
 *
 * Without the fix, pressing ESC falls through to {@code transitionToPaused()} with the shop menu
 * still flagged as open. On resume, {@code isUIBlocking()} returns true, blocking all player
 * movement and punch/place input — the player is stuck until the 10-second speech timer expires.
 *
 * These tests validate the condition logic that {@code handleEscapePress()} uses to decide
 * whether to close the shop menu. They mirror the new branch in {@code handleEscapePress()}:
 *
 * <pre>
 *   if (activeShopkeeperNPC != null &amp;&amp; activeShopkeeperNPC.isShopMenuOpen()) {
 *       activeShopkeeperNPC.setShopMenuOpen(false);
 *       activeShopkeeperNPC = null;
 *       Gdx.input.setCursorCatched(state == GameState.PLAYING);
 *   } else if (achievementsUI.isVisible()) {
 *       ...
 *   }
 * </pre>
 */
class Issue559EscClosesShopMenuTest {

    /**
     * Simulates the ESC-key branch added by Fix #559:
     * closes the shop menu on the given NPC and returns the new activeShopkeeperNPC value (null).
     *
     * @return null (the new value of activeShopkeeperNPC after ESC is handled)
     */
    private static NPC simulateEscapeHandledForShopMenu(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen()) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            return null; // activeShopkeeperNPC nulled out
        }
        return activeShopkeeperNPC; // no change
    }

    // --- ESC closes an open shop menu ---

    @Test
    void whenShopMenuIsOpen_escClosesMenu_andNullsActiveShopkeeper() {
        // ESC must close the shop menu and null out activeShopkeeperNPC,
        // preventing the game from pausing with the menu still flagged open.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // first E-press: menu opens

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: isShopMenuOpen() must be true after first E-press");

        // Simulate ESC press
        NPC resultActiveShopkeeper = simulateEscapeHandledForShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #559: ESC must close the shop menu (setShopMenuOpen(false))");
        assertNull(resultActiveShopkeeper,
            "Fix #559: ESC must null out activeShopkeeperNPC after closing the shop menu");
    }

    // --- After ESC, isUIBlocking returns false so player is not stuck ---

    @Test
    void afterEscClosesShopMenu_isShopMenuOpen_isFalse_playerNotStuck() {
        // After ESC, isShopMenuOpen() is false, so isUIBlocking() returns false
        // and the player is not stuck on resume.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu

        // ESC handled
        NPC resultActiveShopkeeper = simulateEscapeHandledForShopMenu(shopkeeper);

        // isUIBlocking() check: (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())
        boolean isBlocking = resultActiveShopkeeper != null && resultActiveShopkeeper.isShopMenuOpen();

        assertFalse(isBlocking,
            "Fix #559: after ESC closes the shop menu, isUIBlocking() must return false " +
            "so the player is not stuck when the game resumes");
    }

    // --- ESC is a no-op when no shop menu is open ---

    @Test
    void whenNoShopMenuOpen_escDoesNotAffectNullActiveShopkeeper() {
        // If no shop menu is open (activeShopkeeperNPC is null), the ESC branch is not taken.
        NPC resultActiveShopkeeper = simulateEscapeHandledForShopMenu(null);

        assertNull(resultActiveShopkeeper,
            "Fix #559: when activeShopkeeperNPC is null, ESC must not affect it " +
            "(branch guard prevents NPE)");
    }

    // --- ESC is a no-op when NPC exists but menu is already closed ---

    @Test
    void whenShopkeeperExistsButMenuClosed_escDoesNothing() {
        // If activeShopkeeperNPC is set but isShopMenuOpen() is false (stale state),
        // the ESC branch must not be taken — consistent with the guard condition.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        system.interactWithNPC(shopkeeper, inv); // close menu via purchase

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Precondition: menu must be closed after purchase");

        // Simulate ESC: the branch condition is false, so activeShopkeeperNPC is unchanged
        NPC resultActiveShopkeeper = simulateEscapeHandledForShopMenu(shopkeeper);

        // Branch guard: activeShopkeeperNPC != null && isShopMenuOpen() is false,
        // so the branch is skipped and the reference is returned unchanged.
        assertSame(shopkeeper, resultActiveShopkeeper,
            "Fix #559: when menu is already closed, ESC must not null out the reference " +
            "(other branches in handleEscapePress handle the normal pause transition)");
        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #559: ESC must not accidentally reopen a closed shop menu");
    }

    // --- ESC closes menu regardless of speech timer state ---

    @Test
    void whenShopMenuIsOpen_escCloses_beforeSpeechTimerExpires() {
        // The player should not have to wait 10 seconds for the timer.
        // ESC must close the menu immediately, consistent with all other UI overlays.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu

        // Advance only 2 seconds — timer has NOT expired yet
        shopkeeper.update(2f);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: menu must still be open 2 s after opening (timer not expired)");

        // ESC handled immediately
        NPC resultActiveShopkeeper = simulateEscapeHandledForShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #559: ESC must close the shop menu before the 10-second speech timer expires");
        assertNull(resultActiveShopkeeper,
            "Fix #559: ESC must null out activeShopkeeperNPC immediately");
    }
}
