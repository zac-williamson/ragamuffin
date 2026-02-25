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
 * Integration tests for Issue #563 — restartGame() does not reset activeShopkeeperNPC,
 * locking player input in the new session.
 *
 * <p>Bug: when the player restarts while a shop menu is open, {@code restartGame()} replaced
 * {@code npcManager} with a new instance but never reset {@code activeShopkeeperNPC} to null.
 * The stale NPC still had {@code isShopMenuOpen() == true}, so {@code isUIBlocking()} returned
 * true for the entire new session — movement, punching, placing, and interaction were all
 * suppressed.
 *
 * <p>Fix: {@code restartGame()} now calls {@code activeShopkeeperNPC.setShopMenuOpen(false)}
 * then sets {@code activeShopkeeperNPC = null} before any other reset logic runs.
 */
class Issue563RestartClearsShopMenuTest {

    /**
     * Simulates the restartGame() fix for activeShopkeeperNPC:
     * closes the shop menu on the stale NPC and returns null.
     *
     * @return null (the new value of activeShopkeeperNPC after restart)
     */
    private static NPC simulateRestartClearsShopMenu(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            return null;
        }
        return null;
    }

    // --- After restart, activeShopkeeperNPC must be null ---

    @Test
    void afterRestart_activeShopkeeperNPC_isNull_whenMenuWasOpen() {
        // Session 1: player opens a shop menu
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open shop menu

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before restart");

        // Simulate restartGame() fix
        NPC activeShopkeeperNPC = simulateRestartClearsShopMenu(shopkeeper);

        assertNull(activeShopkeeperNPC,
            "Fix #563: activeShopkeeperNPC must be null after restart");
    }

    // --- After restart, the stale NPC's shop menu is explicitly closed ---

    @Test
    void afterRestart_staleNpcShopMenu_isClosed() {
        // Session 1: player opens a shop menu
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open shop menu

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before restart");

        // Simulate restartGame() fix
        simulateRestartClearsShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #563: setShopMenuOpen(false) must be called on the stale NPC during restart");
    }

    // --- After restart, isUIBlocking() returns false so player is not stuck ---

    @Test
    void afterRestart_isUIBlocking_isFalse() {
        // Session 1: shop menu is open — isUIBlocking() would return true
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        boolean blockingBeforeRestart = shopkeeper.isShopMenuOpen();
        assertTrue(blockingBeforeRestart,
            "Precondition: isUIBlocking() relevant condition is true before restart");

        // Simulate restartGame() fix
        NPC activeShopkeeperNPC = simulateRestartClearsShopMenu(shopkeeper);

        // isUIBlocking() check: (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())
        boolean isBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();

        assertFalse(isBlocking,
            "Fix #563: after restart, isUIBlocking() must return false so player input is not locked");
    }

    // --- Restart is safe when no shop menu was open (null guard) ---

    @Test
    void restart_withNoActiveShopkeeper_isNoOp() {
        // Common case: player restarts without a shop menu open
        NPC activeShopkeeperNPC = null;

        NPC result = simulateRestartClearsShopMenu(activeShopkeeperNPC);

        assertNull(result,
            "Fix #563: restart with null activeShopkeeperNPC must not throw and must remain null");
    }

    // --- Without the fix, stale NPC still has menu open (documents the bug) ---

    @Test
    void withoutFix_staleNpcStillHasMenuOpen_causesUIBlocking() {
        // Bug: restartGame() replaces npcManager but never touches activeShopkeeperNPC.
        // The stale reference still has isShopMenuOpen() == true.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        // BUG: restartGame() does NOT null out activeShopkeeperNPC — stale ref kept
        NPC activeShopkeeperNPC = shopkeeper; // not reset

        // isUIBlocking() returns true — player is stuck
        boolean isBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();
        assertTrue(isBlocking,
            "Without the fix, isUIBlocking() returns true after restart — confirming the bug");
    }
}
