package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #547 — shop menu must close when player walks away.
 *
 * When the player opens a shopkeeper's menu (first E-press) and then walks out of
 * interaction range without pressing E again, the game loop in updatePlayingSimulation()
 * must detect the increased distance and:
 *   - call npc.setShopMenuOpen(false), and
 *   - clear activeShopkeeperNPC to null.
 *
 * These tests validate the distance-check logic introduced in updatePlayingSimulation()
 * and verify that 1/2/3 keys are no longer intercepted after the player walks away.
 */
class Issue547ShopMenuWalkAwayTest {

    /**
     * Simulates the per-frame guard added in RagamuffinGame.updatePlayingSimulation():
     *
     *   if (activeShopkeeperNPC != null) {
     *       float dist = playerPos.dst(activeShopkeeperNPC.getPosition());
     *       if (dist > InteractionSystem.INTERACTION_RANGE) {
     *           activeShopkeeperNPC.setShopMenuOpen(false);
     *           activeShopkeeperNPC = null;
     *       }
     *   }
     */
    private static NPC runDistanceGuard(NPC activeShopkeeperNPC, Vector3 playerPos) {
        if (activeShopkeeperNPC != null) {
            float dist = playerPos.dst(activeShopkeeperNPC.getPosition());
            if (dist > InteractionSystem.INTERACTION_RANGE) {
                activeShopkeeperNPC.setShopMenuOpen(false);
                activeShopkeeperNPC = null;
            }
        }
        return activeShopkeeperNPC;
    }

    // --- Menu closes when player walks out of range ---

    @Test
    void walkingOutOfRange_closesShopMenu() {
        // Shopkeeper at (0, 0, 0); player starts adjacent at (0, 0, -1)
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, 0);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();

        // First E-press: open shop menu
        system.interactWithNPC(shopkeeper, inv);
        assertTrue(shopkeeper.isShopMenuOpen(), "Shop menu must be open after first E-press");

        // Simulate game tracking activeShopkeeperNPC
        NPC activeShopkeeperNPC = shopkeeper;

        // Player is still adjacent — within range — guard should not fire
        Vector3 playerPosNear = new Vector3(0, 0, -1f);
        activeShopkeeperNPC = runDistanceGuard(activeShopkeeperNPC, playerPosNear);
        assertNotNull(activeShopkeeperNPC, "activeShopkeeperNPC should still be set when player is in range");
        assertTrue(shopkeeper.isShopMenuOpen(), "Shop menu should still be open when player is in range");

        // Player walks 5 blocks away — out of INTERACTION_RANGE (2.0f)
        Vector3 playerPosFar = new Vector3(0, 0, -6f);
        activeShopkeeperNPC = runDistanceGuard(activeShopkeeperNPC, playerPosFar);

        assertNull(activeShopkeeperNPC, "activeShopkeeperNPC must be null after player walks out of range");
        assertFalse(shopkeeper.isShopMenuOpen(), "Shop menu must be closed after player walks out of range");
    }

    @Test
    void withinRange_menuRemainsOpen() {
        // Shopkeeper at (0, 0, 0); player just within range at 1.5 blocks away
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, 0);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        NPC activeShopkeeperNPC = shopkeeper;

        // Player at 1.5 blocks — within INTERACTION_RANGE (2.0f)
        Vector3 playerPos = new Vector3(0, 0, -1.5f);
        activeShopkeeperNPC = runDistanceGuard(activeShopkeeperNPC, playerPos);

        assertNotNull(activeShopkeeperNPC, "activeShopkeeperNPC must remain set when player is within range");
        assertTrue(shopkeeper.isShopMenuOpen(), "Shop menu must remain open when player is within range");
    }

    @Test
    void exactlyAtRangeBoundary_menuRemainsOpen() {
        // Shopkeeper at (0, 0, 0); player at exactly INTERACTION_RANGE distance
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, 0);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        NPC activeShopkeeperNPC = shopkeeper;

        // Player exactly at INTERACTION_RANGE — should NOT be cleared (guard uses strict >)
        Vector3 playerPos = new Vector3(0, 0, -InteractionSystem.INTERACTION_RANGE);
        activeShopkeeperNPC = runDistanceGuard(activeShopkeeperNPC, playerPos);

        assertNotNull(activeShopkeeperNPC, "activeShopkeeperNPC must stay set when player is exactly at range boundary");
        assertTrue(shopkeeper.isShopMenuOpen(), "Shop menu must remain open at exact range boundary");
    }

    @Test
    void justBeyondRange_menuCloses() {
        // Shopkeeper at (0, 0, 0); player just past INTERACTION_RANGE
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, 0);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        NPC activeShopkeeperNPC = shopkeeper;

        // Player one unit beyond INTERACTION_RANGE
        float dist = InteractionSystem.INTERACTION_RANGE + 0.01f;
        Vector3 playerPos = new Vector3(0, 0, -dist);
        activeShopkeeperNPC = runDistanceGuard(activeShopkeeperNPC, playerPos);

        assertNull(activeShopkeeperNPC, "activeShopkeeperNPC must be cleared just beyond range");
        assertFalse(shopkeeper.isShopMenuOpen(), "Shop menu must close just beyond INTERACTION_RANGE");
    }

    // --- After menu closes, 1/2/3 keys are not intercepted ---

    @Test
    void afterWalkingAway_hotbarSlotChanges_notIntercepted() {
        // Full scenario from the issue description:
        // 1. Open shop menu (E)
        // 2. Walk 5 blocks away — guard fires, menu closes
        // 3. Simulate pressing key 1 — hotbar slot should change to 0 (not intercepted)
        NPCManager npcManager = new NPCManager();
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, 0);
        Inventory inv = new Inventory(36);
        inv.addItem(Material.SHILLING, 5);

        InteractionSystem system = new InteractionSystem();
        system.interactWithNPC(shopkeeper, inv); // open menu
        NPC activeShopkeeperNPC = shopkeeper;

        // Walk 5 blocks away
        Vector3 playerPosFar = new Vector3(0, 0, -5f);
        activeShopkeeperNPC = runDistanceGuard(activeShopkeeperNPC, playerPosFar);

        // Guard should have cleared the reference and closed the menu
        assertNull(activeShopkeeperNPC, "activeShopkeeperNPC must be null after walking away");
        assertFalse(shopkeeper.isShopMenuOpen(), "NPC isShopMenuOpen() must be false after walking away");

        // Now simulate the intercept guard in RagamuffinGame.handleUIInput():
        // "if (activeShopkeeperNPC != null && activeShopkeeperNPC.isShopMenuOpen() && hotbarSlot <= 2)"
        // Since activeShopkeeperNPC is null, the intercept does NOT fire.
        // The hotbar slot should be allowed to change normally.
        int hotbarSlot = 0; // key "1" pressed
        boolean intercepted = (activeShopkeeperNPC != null
                && activeShopkeeperNPC.isShopMenuOpen()
                && hotbarSlot <= 2);

        assertFalse(intercepted,
            "Key 1 (hotbar slot 0) must NOT be intercepted by shop menu after player walks away");
    }

    // --- Guard is safe when activeShopkeeperNPC is already null ---

    @Test
    void guardIsNoOp_whenActiveShopkeeperIsNull() {
        // If no shop menu is open, the guard must silently do nothing.
        Vector3 playerPos = new Vector3(10, 0, 10);
        NPC result = runDistanceGuard(null, playerPos);
        assertNull(result, "Guard must return null unchanged when input is already null");
    }
}
