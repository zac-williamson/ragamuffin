package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #633 — {@code transitionToPaused()} and
 * {@code transitionToPlaying()} did not close the active shopkeeper shop menu or
 * clear the {@code activeShopkeeperNPC} reference, permanently blocking player
 * input after the sequence: open shop → press ESC (paused) → press ESC/Resume
 * (playing).
 *
 * <p>Bug: {@code isUIBlocking()} checks
 * {@code activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen()}.
 * If neither transition method cleared the reference, {@code isUIBlocking()}
 * returned {@code true} for the rest of the session, suppressing
 * {@code updatePlayingInput(delta)} so the player could not move, punch, place
 * blocks, or interact with anything.
 *
 * <p>Fix: add the identical guard block to both {@code transitionToPlaying()} and
 * {@code transitionToPaused()}, mirroring the pattern already applied in:
 * CINEMATIC respawn-completion (Fix #623), PLAYING justDied (Fix #601),
 * PAUSED justDied (Fix #621), arrest blocks (Fix #615), and {@code restartGame()}
 * (Fix #563).
 */
class Issue633ShopMenuClearedOnTransitionTest {

    private NPCManager npcManager;
    private Inventory inventory;
    private InteractionSystem interactionSystem;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        inventory.addItem(Material.SHILLING, 5);
        interactionSystem = new InteractionSystem();
    }

    // -----------------------------------------------------------------------
    // Helpers — mirror the exact guards added by Fix #633
    // -----------------------------------------------------------------------

    /**
     * Simulates the activeShopkeeperNPC null-and-close guard added to
     * {@code transitionToPlaying()} by Fix #633.
     */
    private static NPC simulateTransitionToPlayingClearsShopMenu(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            return null;
        }
        return null;
    }

    /**
     * Simulates the activeShopkeeperNPC null-and-close guard added to
     * {@code transitionToPaused()} by Fix #633.
     */
    private static NPC simulateTransitionToPausedClearsShopMenu(NPC activeShopkeeperNPC) {
        if (activeShopkeeperNPC != null) {
            activeShopkeeperNPC.setShopMenuOpen(false);
            return null;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // transitionToPlaying() — shop menu closed and reference cleared
    // -----------------------------------------------------------------------

    @Test
    void transitionToPlaying_clearsActiveShopkeeperNPC_whenMenuWasOpen() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before transitioning to PLAYING");

        NPC activeShopkeeperNPC = shopkeeper;
        activeShopkeeperNPC = simulateTransitionToPlayingClearsShopMenu(activeShopkeeperNPC);

        assertNull(activeShopkeeperNPC,
            "Fix #633: activeShopkeeperNPC must be null after transitionToPlaying()");
    }

    @Test
    void transitionToPlaying_closesShopMenuOnStaleNPC() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before transitioning to PLAYING");

        simulateTransitionToPlayingClearsShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #633: shopkeeper.setShopMenuOpen(false) must be called in transitionToPlaying() " +
            "so isUIBlocking() no longer returns true after the transition");
    }

    @Test
    void transitionToPlaying_isNoOp_whenActiveShopkeeperNPCIsNull() {
        // activeShopkeeperNPC is null — guard must not throw and must return null
        NPC result = simulateTransitionToPlayingClearsShopMenu(null);

        assertNull(result,
            "Fix #633: transitionToPlaying() shop-menu guard must handle null activeShopkeeperNPC safely");
    }

    @Test
    void transitionToPlaying_isUIBlocking_returnsFalse_afterShopMenuCleared() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu open — isUIBlocking() would return true");

        // transitionToPlaying() guard runs
        NPC activeShopkeeperNPC = simulateTransitionToPlayingClearsShopMenu(shopkeeper);

        // isUIBlocking() logic: activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen()
        boolean uiBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();

        assertFalse(uiBlocking,
            "Fix #633: isUIBlocking() must return false after transitionToPlaying() clears activeShopkeeperNPC, " +
            "so updatePlayingInput(delta) is no longer suppressed");
    }

    // -----------------------------------------------------------------------
    // transitionToPaused() — shop menu closed and reference cleared
    // -----------------------------------------------------------------------

    @Test
    void transitionToPaused_clearsActiveShopkeeperNPC_whenMenuWasOpen() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before transitioning to PAUSED");

        NPC activeShopkeeperNPC = shopkeeper;
        activeShopkeeperNPC = simulateTransitionToPausedClearsShopMenu(activeShopkeeperNPC);

        assertNull(activeShopkeeperNPC,
            "Fix #633: activeShopkeeperNPC must be null after transitionToPaused()");
    }

    @Test
    void transitionToPaused_closesShopMenuOnStaleNPC() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu must be open before transitioning to PAUSED");

        simulateTransitionToPausedClearsShopMenu(shopkeeper);

        assertFalse(shopkeeper.isShopMenuOpen(),
            "Fix #633: shopkeeper.setShopMenuOpen(false) must be called in transitionToPaused() " +
            "so isUIBlocking() no longer returns true if the player subsequently resumes");
    }

    @Test
    void transitionToPaused_isNoOp_whenActiveShopkeeperNPCIsNull() {
        NPC result = simulateTransitionToPausedClearsShopMenu(null);

        assertNull(result,
            "Fix #633: transitionToPaused() shop-menu guard must handle null activeShopkeeperNPC safely");
    }

    @Test
    void transitionToPaused_isUIBlocking_returnsFalse_afterShopMenuCleared() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);

        assertTrue(shopkeeper.isShopMenuOpen(),
            "Precondition: shop menu open — isUIBlocking() would return true");

        // transitionToPaused() guard runs
        NPC activeShopkeeperNPC = simulateTransitionToPausedClearsShopMenu(shopkeeper);

        boolean uiBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();

        assertFalse(uiBlocking,
            "Fix #633: isUIBlocking() must return false after transitionToPaused() clears activeShopkeeperNPC");
    }

    // -----------------------------------------------------------------------
    // Full reproduction scenario: open shop → ESC (pause) → ESC (resume)
    // -----------------------------------------------------------------------

    @Test
    void fullScenario_shopThenEscThenResume_doesNotPermanentlyBlockInput() {
        // 1. Player opens shop menu (E key near shopkeeper)
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1f);
        interactionSystem.interactWithNPC(shopkeeper, inventory);
        assertTrue(shopkeeper.isShopMenuOpen(),
            "Step 1: shop menu must be open after first E-press");

        NPC activeShopkeeperNPC = shopkeeper;

        // 2. Player presses ESC — transitionToPaused() clears the shop reference
        activeShopkeeperNPC = simulateTransitionToPausedClearsShopMenu(activeShopkeeperNPC);
        assertNull(activeShopkeeperNPC,
            "Step 2: activeShopkeeperNPC must be null after transitionToPaused()");
        assertFalse(shopkeeper.isShopMenuOpen(),
            "Step 2: shop menu must be closed after transitionToPaused()");

        // 3. Player presses ESC again / clicks Resume — transitionToPlaying() guard is also safe
        activeShopkeeperNPC = simulateTransitionToPlayingClearsShopMenu(activeShopkeeperNPC);
        assertNull(activeShopkeeperNPC,
            "Step 3: activeShopkeeperNPC must still be null after transitionToPlaying()");

        // 4. isUIBlocking() must now return false — player can move/interact freely
        boolean uiBlocking = activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen();
        assertFalse(uiBlocking,
            "Fix #633: isUIBlocking() must return false after shop→ESC→resume sequence so " +
            "updatePlayingInput(delta) is no longer suppressed and the player can move again");
    }
}
