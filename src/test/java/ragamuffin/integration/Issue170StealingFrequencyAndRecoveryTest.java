package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #170 Integration Tests — Reduce NPC stealing frequency and allow item recovery.
 *
 * Verifies that:
 * 1. After stealing, a YOUTH_GANG NPC cannot steal again immediately (60-second cooldown).
 * 2. Items stolen by a YOUTH_GANG NPC are returned to the player when the NPC is defeated.
 * 3. A YOUTH_GANG NPC that has not yet stolen still steals normally (regression guard).
 */
class Issue170StealingFrequencyAndRecoveryTest {

    private NPCManager npcManager;
    private Player player;
    private World world;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();
        inventory = new Inventory(36);
        world = new World(42L);

        // Flat ground so NPCs can move
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: After stealing, a YOUTH_GANG NPC cannot steal again immediately.
     *
     * Spawn a YOUTH_GANG NPC adjacent to the player. Give the player 5 WOOD.
     * Simulate until the first theft occurs. Record the inventory count.
     * Continue simulating for 10 more seconds (shorter than the 60-second cooldown).
     * Verify no second theft occurred.
     */
    @Test
    void test1_StealCooldownPreventsImmediateReStealing() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1f, 0.5f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.WANDERING);

        player.getPosition().set(0f, 1f, 0f);
        inventory.addItem(Material.WOOD, 5);

        assertEquals(5, inventory.getItemCount(Material.WOOD), "Player should start with 5 wood");

        // Simulate until first theft occurs (up to 10 seconds)
        float delta = 1f / 60f;
        boolean firstTheftOccurred = false;
        int woodAfterFirstTheft = 5;
        for (int i = 0; i < 600; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            int currentWood = inventory.getItemCount(Material.WOOD);
            if (currentWood < 5) {
                firstTheftOccurred = true;
                woodAfterFirstTheft = currentWood;
                break;
            }
        }

        assertTrue(firstTheftOccurred, "First theft should have occurred within 10 seconds");

        // Now simulate for another 10 seconds (well within the 60-second cooldown window)
        int woodAtCooldownStart = woodAfterFirstTheft;
        for (int i = 0; i < 600; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
        }

        assertEquals(woodAtCooldownStart, inventory.getItemCount(Material.WOOD),
            "No second theft should occur within the steal cooldown period. " +
            "Wood count changed from " + woodAtCooldownStart + " to " + inventory.getItemCount(Material.WOOD));
    }

    /**
     * Test 2: Stolen items are returned when the thief NPC is defeated.
     *
     * Spawn a YOUTH_GANG NPC adjacent to the player. Give the player 1 DIAMOND.
     * Wait for the NPC to steal the diamond. Then punch the NPC until it is defeated.
     * Verify the diamond is returned to the player's inventory.
     */
    @Test
    void test2_StolenItemsReturnedOnNPCDefeat() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1f, 0.5f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.WANDERING);

        player.getPosition().set(0f, 1f, 0f);
        inventory.addItem(Material.DIAMOND, 1);

        assertEquals(1, inventory.getItemCount(Material.DIAMOND), "Player should start with 1 diamond");

        // Simulate until theft occurs (up to 10 seconds)
        float delta = 1f / 60f;
        boolean theftOccurred = false;
        for (int i = 0; i < 600; i++) {
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.DIAMOND) == 0) {
                theftOccurred = true;
                break;
            }
        }

        assertTrue(theftOccurred, "NPC should have stolen the diamond");
        assertEquals(0, inventory.getItemCount(Material.DIAMOND),
            "Diamond should be gone from inventory after theft");

        // Defeat the NPC — YOUTH_GANG has 30 HP, 10 per punch = 3 punches
        Vector3 punchDir = new Vector3(1, 0, 0);
        npcManager.punchNPC(gangMember, punchDir, inventory, tooltipSystem);
        npcManager.punchNPC(gangMember, punchDir, inventory, tooltipSystem);
        npcManager.punchNPC(gangMember, punchDir, inventory, tooltipSystem);

        assertFalse(gangMember.isAlive(), "Gang member should be dead after 3 punches");

        // The stolen diamond should be returned
        assertTrue(inventory.getItemCount(Material.DIAMOND) >= 1,
            "Diamond should be returned to the player after defeating the thief. " +
            "Diamond count: " + inventory.getItemCount(Material.DIAMOND));
    }

    /**
     * Test 3: A gang member that has not stolen still steals normally (regression guard).
     *
     * Spawn a fresh YOUTH_GANG NPC adjacent to the player. Give the player some WOOD.
     * Verify theft occurs within a reasonable time. This ensures the cooldown only
     * applies after the first theft, not to fresh NPCs.
     */
    @Test
    void test3_FreshGangMemberStillSteals() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, 0.5f, 1f, 0.5f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn");
        gangMember.setState(NPCState.WANDERING);

        player.getPosition().set(0f, 1f, 0f);
        inventory.addItem(Material.WOOD, 3);

        assertEquals(3, inventory.getItemCount(Material.WOOD), "Player should start with 3 wood");

        float delta = 1f / 60f;
        for (int i = 0; i < 600; i++) { // up to 10 seconds
            npcManager.update(delta, world, player, inventory, tooltipSystem);
            if (inventory.getItemCount(Material.WOOD) < 3) {
                break;
            }
        }

        assertTrue(inventory.getItemCount(Material.WOOD) < 3,
            "Fresh YOUTH_GANG NPC should still steal from the player. " +
            "Wood count: " + inventory.getItemCount(Material.WOOD));
    }
}
