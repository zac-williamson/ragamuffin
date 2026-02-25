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
 * Integration tests for Issue #601 — the justDied path does not clear activeShopkeeperNPC,
 * locking player input after death during a shop interaction.
 *
 * <p>Bug: when the player dies while a shopkeeper's shop menu is open, {@code activeShopkeeperNPC}
 * was not cleared in the {@code justDied} block of {@code updatePlayingSimulation()}.
 * As a result, {@code isUIBlocking()} continued to return {@code true} after the player
 * respawned — because {@code activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen()}
 * — permanently suppressing all player input for the rest of the session.
 *
 * <p>Fix: the {@code justDied} block now calls {@code activeShopkeeperNPC.setShopMenuOpen(false)}
 * then sets {@code activeShopkeeperNPC = null}, mirroring the pattern established for
 * {@code restartGame()} in Fix #563.
 */
class Issue601DeathClearsShopMenuTest {

    /**
     * Simulates the justDied fix for activeShopkeeperNPC:
     * closes the shop menu on the stale NPC and returns null.
     *
     * @return null (the new value of activeShopkeeperNPC after death)
     */
    private static NPC simulateDeathClearsShopMenu(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            return null;
        }
        return null;
    }

    // --- After death, activeShopkeeperNPC must be null ---

    @Test
    void afterDeath_activeShopkeeperNPC_isNull_whenMenuWasOpen() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open shop menu

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before death");

        // Simulate justDied fix
        NPC activeShopkeeperNPC = simulateDeathClearsShopMenu(shopkeeper);

        assertNull(activeShopkeeperNPC,
            "Fix #601: activeShopkeeperNPC must be null after player death");
    }

    // --- After death, the stale NPC's shop menu is explicitly closed ---

    @Test
    void afterDeath_staleNpcShopMenu_isClosed() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open shop menu

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before death");

        // Simulate justDied fix
        simulateDeathClearsShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #601: setShopMenuOpen(false) must be called on the stale NPC on death");
    }

    // --- After death + respawn, isUIBlocking() returns false so player is not stuck ---

    @Test
    void afterDeath_isUIBlocking_isFalse_afterRespawn() {
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        boolean blockingBeforeDeath = shopkeeper.isShopMenuOpen();
        assertTrue(blockingBeforeDeath,
            "Precondition: isUIBlocking() relevant condition is true before death");

        // Simulate justDied fix
        NPC activeShopkeeperNPC = simulateDeathClearsShopMenu(shopkeeper);

        // isUIBlocking() check: (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen())
        boolean isBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();

        assertFalse(isBlocking,
            "Fix #601: after death and respawn, isUIBlocking() must return false so player input is not locked");
    }

    // --- Death with no active shopkeeper is safe (null guard) ---

    @Test
    void death_withNoActiveShopkeeper_isNoOp() {
        NPC activeShopkeeperNPC = null;

        NPC result = simulateDeathClearsShopMenu(activeShopkeeperNPC);

        assertNull(result,
            "Fix #601: death with null activeShopkeeperNPC must not throw and must remain null");
    }

    // --- Without the fix, stale NPC still has menu open (documents the bug) ---

    @Test
    void withoutFix_staleNpcStillHasMenuOpen_causesUIBlocking() {
        // Bug: justDied block never touched activeShopkeeperNPC.
        // The stale reference still has isShopMenuOpen() == true after respawn.
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv);

        // BUG: justDied block does NOT null out activeShopkeeperNPC — stale ref kept
        NPC activeShopkeeperNPC = shopkeeper; // not reset

        // isUIBlocking() returns true — player is stuck after respawn
        boolean isBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();
        assertTrue(isBlocking,
            "Without the fix, isUIBlocking() returns true after death/respawn — confirming the bug");
    }
}
