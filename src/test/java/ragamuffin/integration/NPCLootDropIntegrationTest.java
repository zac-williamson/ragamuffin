package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NPC loot drops on death.
 * Verifies that defeating NPCs awards appropriate items to the player's inventory.
 */
class NPCLootDropIntegrationTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Create flat ground
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1: Defeating a shopkeeper drops food items.
     * Shopkeepers should drop crisps, energy drinks, and possibly tinned beans.
     */
    @Test
    void test1_ShopkeeperDropsFood() {
        NPC shopkeeper = npcManager.spawnNPC(NPCType.SHOPKEEPER, 5, 1, 5);
        assertNotNull(shopkeeper);

        // Punch until dead (shopkeeper has 20 HP, 10 per punch = 2 punches)
        Vector3 direction = new Vector3(1, 0, 0);
        npcManager.punchNPC(shopkeeper, direction, inventory, tooltipSystem);
        assertTrue(shopkeeper.isAlive(), "Shopkeeper should survive first punch");

        npcManager.punchNPC(shopkeeper, direction, inventory, tooltipSystem);
        assertFalse(shopkeeper.isAlive(), "Shopkeeper should be dead after second punch");

        // Verify loot was awarded — shopkeepers always drop crisps and energy drink
        assertTrue(inventory.getItemCount(Material.CRISPS) >= 1,
                "Should have received crisps from shopkeeper");
        assertTrue(inventory.getItemCount(Material.ENERGY_DRINK) >= 1,
                "Should have received energy drink from shopkeeper");
    }

    /**
     * Test 2: Defeating a youth gang member drops wood and possibly scrap metal.
     */
    @Test
    void test2_YouthGangDropsWoodAndScrap() {
        NPC youth = npcManager.spawnNPC(NPCType.YOUTH_GANG, 5, 1, 5);
        assertNotNull(youth);

        // Youth has 30 HP, 10 per punch = 3 punches
        Vector3 direction = new Vector3(1, 0, 0);
        npcManager.punchNPC(youth, direction, inventory, tooltipSystem);
        npcManager.punchNPC(youth, direction, inventory, tooltipSystem);
        assertTrue(youth.isAlive(), "Youth should survive two punches");

        npcManager.punchNPC(youth, direction, inventory, tooltipSystem);
        assertFalse(youth.isAlive(), "Youth should be dead after three punches");

        // Youth gangs always drop 2-4 wood
        assertTrue(inventory.getItemCount(Material.WOOD) >= 2,
                "Should have received at least 2 wood from youth gang");
    }

    /**
     * Test 3: Defeating a dog drops nothing.
     * Dogs are innocent creatures and carry no loot.
     */
    @Test
    void test3_DogDropsNothing() {
        NPC dog = npcManager.spawnNPC(NPCType.DOG, 5, 1, 5);
        assertNotNull(dog);

        // Dog has 15 HP, 10 per punch = 2 punches
        Vector3 direction = new Vector3(1, 0, 0);
        npcManager.punchNPC(dog, direction, inventory, tooltipSystem);
        npcManager.punchNPC(dog, direction, inventory, tooltipSystem);
        assertFalse(dog.isAlive(), "Dog should be dead after two punches");

        // Verify no items were added (dogs don't carry loot)
        int totalItems = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            totalItems += inventory.getCountInSlot(i);
        }
        assertEquals(0, totalItems, "Dog should not drop any items");
    }

    /**
     * Test 4: Loot drop triggers the FIRST_NPC_LOOT tooltip.
     */
    @Test
    void test4_LootDropTriggersTooltip() {
        NPC publicNPC = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 5);
        assertNotNull(publicNPC);

        assertFalse(tooltipSystem.hasShown(TooltipTrigger.FIRST_NPC_LOOT),
                "Tooltip should not be shown before any kill");

        // Public has 20 HP, 10 per punch = 2 punches
        Vector3 direction = new Vector3(1, 0, 0);
        npcManager.punchNPC(publicNPC, direction, inventory, tooltipSystem);
        npcManager.punchNPC(publicNPC, direction, inventory, tooltipSystem);
        assertFalse(publicNPC.isAlive(), "Public NPC should be dead");

        assertTrue(tooltipSystem.hasShown(TooltipTrigger.FIRST_NPC_LOOT),
                "FIRST_NPC_LOOT tooltip should have been triggered");
    }

    /**
     * Test 5: Punching without inventory parameter does NOT drop loot (backward compat).
     */
    @Test
    void test5_PunchWithoutInventoryNoLoot() {
        NPC publicNPC = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 5);
        assertNotNull(publicNPC);

        // Use the old API without inventory
        Vector3 direction = new Vector3(1, 0, 0);
        npcManager.punchNPC(publicNPC, direction);
        npcManager.punchNPC(publicNPC, direction);
        assertFalse(publicNPC.isAlive(), "Public NPC should be dead");

        // Verify no items were added to the separate inventory
        int totalItems = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            totalItems += inventory.getCountInSlot(i);
        }
        assertEquals(0, totalItems, "No loot should be given when no inventory is passed");
    }

    /**
     * Test 6: Council builder drops building materials.
     */
    @Test
    void test6_CouncilBuilderDropsBuildingMaterials() {
        NPC builder = npcManager.spawnNPC(NPCType.COUNCIL_BUILDER, 5, 1, 5);
        assertNotNull(builder);

        // Builder has 40 HP, 10 per punch = 4 punches
        Vector3 direction = new Vector3(1, 0, 0);
        for (int i = 0; i < 4; i++) {
            npcManager.punchNPC(builder, direction, inventory, tooltipSystem);
        }
        assertFalse(builder.isAlive(), "Builder should be dead after 4 punches");

        // Builders always drop brick and stone
        assertTrue(inventory.getItemCount(Material.BRICK) >= 2,
                "Should have received at least 2 brick from builder");
        assertTrue(inventory.getItemCount(Material.STONE) >= 1,
                "Should have received at least 1 stone from builder");
    }

    /**
     * Test 7: Defeating a pensioner drops tinned beans (shopping bag contents).
     */
    @Test
    void test7_PensionerDropsTinnedBeans() {
        NPC pensioner = npcManager.spawnNPC(NPCType.PENSIONER, 5, 1, 5);
        assertNotNull(pensioner);

        // Pensioner has 10 HP, 10 per punch = 1 punch
        Vector3 direction = new Vector3(1, 0, 0);
        npcManager.punchNPC(pensioner, direction, inventory, tooltipSystem);
        assertFalse(pensioner.isAlive(), "Pensioner should be dead after one punch");

        // Pensioners always drop tinned beans
        assertTrue(inventory.getItemCount(Material.TIN_OF_BEANS) >= 1,
                "Should have received at least 1 tin of beans from pensioner");
    }
}
