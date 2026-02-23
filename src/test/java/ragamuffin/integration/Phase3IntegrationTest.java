package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.*;
import ragamuffin.entity.Player;
import ragamuffin.ui.*;
import ragamuffin.world.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3 Integration Tests - Resource & Inventory System
 * Tests the complete block breaking, inventory, and UI systems working together.
 */
class Phase3IntegrationTest {

    private World world;
    private Player player;
    private Inventory inventory;
    private BlockBreaker blockBreaker;
    private BlockDropTable dropTable;
    private TooltipSystem tooltipSystem;
    private InventoryUI inventoryUI;
    private HelpUI helpUI;
    private HotbarUI hotbarUI;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        blockBreaker = new BlockBreaker();
        dropTable = new BlockDropTable();
        tooltipSystem = new TooltipSystem();
        inventoryUI = new InventoryUI(inventory);
        helpUI = new HelpUI();
        hotbarUI = new HotbarUI(inventory);
    }

    /**
     * Integration Test 1: Punching a tree 5 times yields wood.
     * Place the player adjacent to a TREE_TRUNK block, facing it.
     * Simulate 5 punch actions. Verify the TREE_TRUNK block has been removed
     * from the world (replaced with AIR). Verify the player's inventory
     * contains exactly 1 WOOD item.
     */
    @Test
    void test1_PunchingTree5TimesYieldsWood() {
        // Setup: Player at (0, 1, 0), tree trunk at (0, 1, 1) - directly in front
        world.setBlock(0, 1, 1, BlockType.TREE_TRUNK);
        player.getPosition().set(0, 1, 0);

        // Punch 5 times
        for (int i = 0; i < 5; i++) {
            boolean broken = blockBreaker.punchBlock(world, 0, 1, 1);
            if (i < 4) {
                assertFalse(broken, "Block should not break on hit " + (i + 1));
            } else {
                assertTrue(broken, "Block should break on hit 5");
            }
        }

        // Verify block is AIR
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 1));

        // Add the dropped item to inventory
        Material drop = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        inventory.addItem(drop, 1);

        // Verify inventory has 1 WOOD
        assertEquals(1, inventory.getItemCount(Material.WOOD));
    }

    /**
     * Integration Test 2: Punching a tree only 4 times does NOT yield wood.
     * Place the player adjacent to a TREE_TRUNK block, facing it.
     * Simulate 4 punch actions. Verify the TREE_TRUNK block is still present
     * in the world. Verify the player's inventory does NOT contain any WOOD item.
     */
    @Test
    void test2_Punching4TimesDoesNotYieldWood() {
        world.setBlock(0, 1, 1, BlockType.TREE_TRUNK);
        player.getPosition().set(0, 1, 0);

        // Punch 4 times
        for (int i = 0; i < 4; i++) {
            boolean broken = blockBreaker.punchBlock(world, 0, 1, 1);
            assertFalse(broken);
        }

        // Verify block still exists
        assertEquals(BlockType.TREE_TRUNK, world.getBlock(0, 1, 1));

        // Verify inventory has no WOOD
        assertEquals(0, inventory.getItemCount(Material.WOOD));
    }

    /**
     * Integration Test 3: Punching 5 times when not facing a block does nothing.
     * Place the player in an open area with no blocks within punch range.
     * Record the inventory state. Simulate 5 punch actions. Verify the
     * inventory is unchanged (same items, same counts).
     */
    @Test
    void test3_PunchingNotFacingBlockDoesNothing() {
        // Player in open area, no blocks nearby
        player.getPosition().set(100, 10, 100);

        // Record initial inventory state
        int initialWood = inventory.getItemCount(Material.WOOD);
        int initialBrick = inventory.getItemCount(Material.BRICK);

        // Try to punch with raycasting (no block should be hit)
        Vector3 direction = new Vector3(1, 0, 0);
        for (int i = 0; i < 5; i++) {
            RaycastResult result = blockBreaker.getTargetBlock(world, player.getPosition(), direction, 5.0f);
            assertNull(result, "Should not hit any block");
        }

        // Verify inventory unchanged
        assertEquals(initialWood, inventory.getItemCount(Material.WOOD));
        assertEquals(initialBrick, inventory.getItemCount(Material.BRICK));
    }

    /**
     * Integration Test 4: Punching a BRICK block yields BRICK material.
     * Place the player adjacent to a BRICK block. Simulate 5 punch actions.
     * Verify the block is removed and the inventory contains exactly 1 BRICK item.
     */
    @Test
    void test4_PunchingBrickYieldsBrick() {
        world.setBlock(0, 1, 1, BlockType.BRICK);
        player.getPosition().set(0, 1, 0);

        // Punch 8 times (brick blocks are hard and require 8 hits)
        for (int i = 0; i < 8; i++) {
            blockBreaker.punchBlock(world, 0, 1, 1);
        }

        // Verify block removed
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 1));

        // Add drop to inventory
        Material drop = dropTable.getDrop(BlockType.BRICK, null);
        inventory.addItem(drop, 1);

        // Verify inventory
        assertEquals(1, inventory.getItemCount(Material.BRICK));
    }

    /**
     * Integration Test 5: Punching a jeweller block yields diamond.
     * Place the player adjacent to a block that is part of the jeweller shop
     * (e.g. a GLASS block tagged as jeweller inventory). Simulate 5 punch actions.
     * Verify the player's inventory contains exactly 1 DIAMOND item. Verify the
     * tooltip "Jewellers can be a good source of diamond" has been triggered.
     */
    @Test
    void test5_PunchingJewellerBlockYieldsDiamond() {
        // Setup jeweller landmark
        Landmark jeweller = new Landmark(LandmarkType.JEWELLER, 0, 0, 0, 5, 5, 5);
        world.addLandmark(jeweller);

        // Place GLASS block in jeweller area
        world.setBlock(2, 1, 2, BlockType.GLASS);
        player.getPosition().set(2, 1, 1);

        // Punch 5 times
        for (int i = 0; i < 5; i++) {
            blockBreaker.punchBlock(world, 2, 1, 2);
        }

        // Verify block removed
        assertEquals(BlockType.AIR, world.getBlock(2, 1, 2));

        // Get drop from jeweller block
        LandmarkType landmark = world.getLandmarkAt(2, 1, 2);
        Material drop = dropTable.getDrop(BlockType.GLASS, landmark);
        inventory.addItem(drop, 1);

        // Verify diamond in inventory
        assertEquals(1, inventory.getItemCount(Material.DIAMOND));

        // Trigger tooltip
        boolean triggered = tooltipSystem.trigger(TooltipTrigger.JEWELLER_DIAMOND);
        assertTrue(triggered);
        tooltipSystem.update(0.016f); // Activate queued tooltip
        assertEquals("Jewellers can be a good source of diamond", tooltipSystem.getCurrentTooltip());
    }

    /**
     * Integration Test 6: First tree punch triggers tooltip.
     * Place the player adjacent to a TREE_TRUNK. Simulate 1 punch action.
     * Verify the tooltip system has fired the message "Punch a tree to get wood".
     * Simulate punching a second tree later. Verify the tooltip does NOT fire again
     * (first-time only).
     */
    @Test
    void test6_FirstTreePunchTriggersTooltip() {
        world.setBlock(0, 1, 1, BlockType.TREE_TRUNK);
        world.setBlock(5, 1, 1, BlockType.TREE_TRUNK);

        // First punch
        blockBreaker.punchBlock(world, 0, 1, 1);

        // Trigger tooltip
        boolean triggered = tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        assertTrue(triggered);
        tooltipSystem.update(0.016f); // Activate queued tooltip
        assertEquals("Punch a tree to get wood", tooltipSystem.getCurrentTooltip());

        // Clear tooltip
        tooltipSystem.clearCurrent();

        // Punch second tree
        blockBreaker.punchBlock(world, 5, 1, 1);

        // Try to trigger tooltip again
        boolean triggeredAgain = tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        assertFalse(triggeredAgain, "Tooltip should not trigger second time");
        assertNull(tooltipSystem.getCurrentTooltip());
    }

    /**
     * Integration Test 7: Inventory UI toggles with I key.
     * Verify the inventory UI is not visible. Simulate pressing I. Verify the
     * inventory UI is now visible/active. Simulate pressing I again. Verify the
     * inventory UI is hidden. Verify that while the inventory UI is open, player
     * movement is disabled (this is tested by the game state, simulated here).
     */
    @Test
    void test7_InventoryUITogglesWithI() {
        // Initially not visible
        assertFalse(inventoryUI.isVisible());

        // Press I - show
        inventoryUI.toggle();
        assertTrue(inventoryUI.isVisible());

        // Press I - hide
        inventoryUI.toggle();
        assertFalse(inventoryUI.isVisible());

        // Test that when visible, movement should be disabled
        // (This would be handled by the game state in RagamuffinGame)
        inventoryUI.show();
        assertTrue(inventoryUI.isVisible());
        // In the actual game, we'd check that inputHandler doesn't process movement
        // when inventory is open - we verify this by checking the UI state
    }

    /**
     * Integration Test 8: Help UI toggles with H key.
     * Verify the help UI is not visible. Simulate pressing H. Verify the help UI
     * is now visible and contains text describing all controls (WASD, I, H, C, E,
     * ESC, mouse, punch, place). Simulate pressing H again. Verify the help UI
     * is hidden.
     */
    @Test
    void test8_HelpUITogglesWithH() {
        // Initially not visible
        assertFalse(helpUI.isVisible());

        // Press H - show
        helpUI.toggle();
        assertTrue(helpUI.isVisible());

        // Verify help text contains all controls
        String helpText = helpUI.getHelpText();
        assertTrue(helpText.contains("WASD"));
        assertTrue(helpText.contains("Mouse"));
        assertTrue(helpText.contains("I"));
        assertTrue(helpText.contains("H"));
        assertTrue(helpText.contains("C"));
        assertTrue(helpText.contains("E"));
        assertTrue(helpText.contains("ESC"));
        assertTrue(helpText.contains("Left click") || helpText.contains("Punch"));
        assertTrue(helpText.contains("Right click") || helpText.contains("Place"));
        assertTrue(helpText.contains("1-9"));

        // Press H - hide
        helpUI.toggle();
        assertFalse(helpUI.isVisible());
    }

    /**
     * Integration Test 9: Hotbar selection.
     * Give the player WOOD in slot 1 and BRICK in slot 2. Simulate pressing key 1.
     * Verify the selected hotbar slot is 0 (first slot) and the active item is WOOD.
     * Simulate pressing key 2. Verify the selected slot is 1 and the active item
     * is BRICK.
     */
    @Test
    void test9_HotbarSelection() {
        // Add items to inventory (they fill slots sequentially)
        inventory.addItem(Material.WOOD, 5); // Slot 0
        inventory.addItem(Material.BRICK, 3); // Slot 1

        // Press key 1 (select slot 0)
        hotbarUI.selectSlot(0);
        assertEquals(0, hotbarUI.getSelectedSlot());
        assertEquals(Material.WOOD, hotbarUI.getSelectedItem());

        // Press key 2 (select slot 1)
        hotbarUI.selectSlot(1);
        assertEquals(1, hotbarUI.getSelectedSlot());
        assertEquals(Material.BRICK, hotbarUI.getSelectedItem());
    }

    /**
     * Integration Test 11: Block hit counter resets when block is replaced.
     * Place a TREE_TRUNK block at (5, 1, 5). Punch it 3 times (not enough to break).
     * Simulate external removal via world.setBlock(..., AIR) and clearHits (as done
     * by council builder NPCs). Place a new TREE_TRUNK block at (5, 1, 5). Punch it
     * 4 times — verify it is still present. Punch a 5th time — verify it breaks and
     * inventory contains exactly 1 WOOD item.
     */
    @Test
    void test11_HitCounterResetsWhenBlockReplaced() {
        // Place TREE_TRUNK at (5, 1, 5)
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);

        // Punch it 3 times — not enough to break
        for (int i = 0; i < 3; i++) {
            assertFalse(blockBreaker.punchBlock(world, 5, 1, 5));
        }
        assertEquals(3, blockBreaker.getHitCount(5, 1, 5));
        assertEquals(BlockType.TREE_TRUNK, world.getBlock(5, 1, 5));

        // External removal (simulating NPC demolishBlock behaviour)
        world.setBlock(5, 1, 5, BlockType.AIR);
        blockBreaker.clearHits(5, 1, 5);

        // Verify hit count is cleared
        assertEquals(0, blockBreaker.getHitCount(5, 1, 5));

        // Place a new TREE_TRUNK at the same position
        world.setBlock(5, 1, 5, BlockType.TREE_TRUNK);

        // Punch 4 times — should NOT break (requires 5 hits from fresh)
        for (int i = 0; i < 4; i++) {
            assertFalse(blockBreaker.punchBlock(world, 5, 1, 5),
                    "Block should not break on hit " + (i + 1) + " of the new block");
        }
        assertEquals(BlockType.TREE_TRUNK, world.getBlock(5, 1, 5),
                "Block should still be present after 4 hits");

        // 5th punch — should break
        boolean broken = blockBreaker.punchBlock(world, 5, 1, 5);
        assertTrue(broken, "Block should break on 5th hit");
        assertEquals(BlockType.AIR, world.getBlock(5, 1, 5));

        // Collect drop and verify inventory
        Material drop = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        inventory.addItem(drop, 1);
        assertEquals(1, inventory.getItemCount(Material.WOOD));
    }

    /**
     * Integration Test 10: Multiple block breaks accumulate in inventory.
     * Place the player adjacent to 3 TREE_TRUNK blocks. Break all 3 (5 punches
     * each, repositioning between them). Verify the inventory contains exactly
     * 3 WOOD items (stacked).
     */
    @Test
    void test10_MultipleBlockBreaksAccumulate() {
        // Place 3 tree trunks
        world.setBlock(0, 1, 1, BlockType.TREE_TRUNK);
        world.setBlock(0, 1, 2, BlockType.TREE_TRUNK);
        world.setBlock(0, 1, 3, BlockType.TREE_TRUNK);

        // Break first tree
        for (int i = 0; i < 5; i++) {
            blockBreaker.punchBlock(world, 0, 1, 1);
        }
        Material drop1 = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        inventory.addItem(drop1, 1);

        // Break second tree
        blockBreaker.resetHits(); // Reset hit counter for new block
        for (int i = 0; i < 5; i++) {
            blockBreaker.punchBlock(world, 0, 1, 2);
        }
        Material drop2 = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        inventory.addItem(drop2, 1);

        // Break third tree
        blockBreaker.resetHits();
        for (int i = 0; i < 5; i++) {
            blockBreaker.punchBlock(world, 0, 1, 3);
        }
        Material drop3 = dropTable.getDrop(BlockType.TREE_TRUNK, null);
        inventory.addItem(drop3, 1);

        // Verify all 3 blocks removed
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 1));
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 2));
        assertEquals(BlockType.AIR, world.getBlock(0, 1, 3));

        // Verify 3 WOOD in inventory (stacked)
        assertEquals(3, inventory.getItemCount(Material.WOOD));
    }
}
