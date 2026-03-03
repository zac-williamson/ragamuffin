package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1320 — Temperature mechanics lack viable warming mechanisms.
 *
 * <p>Verifies that hot food and drink items consumed via {@link InteractionSystem#consumeFood}
 * restore the player's warmth, giving viable routes to recover from cold conditions.
 *
 * <p>Tests cover:
 * <ol>
 *   <li>Mug of Tea restores +25 warmth</li>
 *   <li>Builder's Tea restores +30 warmth</li>
 *   <li>Full English restores warmth AND hunger</li>
 *   <li>Bacon Butty restores warmth AND hunger</li>
 *   <li>Curry Club Special restores warmth AND hunger</li>
 *   <li>Hot chips restore warmth</li>
 *   <li>A pint restores warmth</li>
 *   <li>Cold items (crisps, energy drink) do NOT restore warmth</li>
 *   <li>Consuming from critically cold state raises warmth above danger threshold</li>
 * </ol>
 */
class Issue1320WarmingMechanismsTest {

    private InteractionSystem interactionSystem;
    private Player player;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        interactionSystem = new InteractionSystem();
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
    }

    /**
     * Mug of Tea should restore +25 warmth instantly on consumption.
     */
    @Test
    void mugOfTeaRestoresWarmth() {
        player.setWarmth(50f);
        inventory.addItem(Material.MUG_OF_TEA, 1);

        boolean consumed = interactionSystem.consumeFood(Material.MUG_OF_TEA, player, inventory);

        assertTrue(consumed, "Mug of Tea should be consumable");
        assertEquals(75f, player.getWarmth(), 0.01f,
                "Mug of Tea should restore +25 warmth. Expected 75, got " + player.getWarmth());
        assertEquals(0, inventory.getItemCount(Material.MUG_OF_TEA),
                "Mug of Tea should be removed from inventory after consumption");
    }

    /**
     * Builder's Tea should restore +30 warmth instantly on consumption.
     */
    @Test
    void builderSTeaRestoresWarmth() {
        player.setWarmth(40f);
        inventory.addItem(Material.BUILDER_S_TEA, 1);

        boolean consumed = interactionSystem.consumeFood(Material.BUILDER_S_TEA, player, inventory);

        assertTrue(consumed, "Builder's Tea should be consumable");
        assertEquals(70f, player.getWarmth(), 0.01f,
                "Builder's Tea should restore +30 warmth. Expected 70, got " + player.getWarmth());
    }

    /**
     * Full English should restore +60 hunger and +20 warmth.
     */
    @Test
    void fullEnglishRestoresWarmthAndHunger() {
        player.setWarmth(30f);
        player.setHunger(20f);
        inventory.addItem(Material.FULL_ENGLISH, 1);

        boolean consumed = interactionSystem.consumeFood(Material.FULL_ENGLISH, player, inventory);

        assertTrue(consumed, "Full English should be consumable");
        assertEquals(50f, player.getWarmth(), 0.01f,
                "Full English should restore +20 warmth. Expected 50, got " + player.getWarmth());
        assertEquals(80f, player.getHunger(), 0.01f,
                "Full English should restore +60 hunger. Expected 80, got " + player.getHunger());
    }

    /**
     * Bacon Butty should restore +35 hunger and +10 warmth.
     */
    @Test
    void baconButtyRestoresWarmthAndHunger() {
        player.setWarmth(50f);
        player.setHunger(30f);
        inventory.addItem(Material.BACON_BUTTY, 1);

        boolean consumed = interactionSystem.consumeFood(Material.BACON_BUTTY, player, inventory);

        assertTrue(consumed, "Bacon Butty should be consumable");
        assertEquals(60f, player.getWarmth(), 0.01f,
                "Bacon Butty should restore +10 warmth. Expected 60, got " + player.getWarmth());
        assertEquals(65f, player.getHunger(), 0.01f,
                "Bacon Butty should restore +35 hunger. Expected 65, got " + player.getHunger());
    }

    /**
     * Curry Club Special should restore +80 hunger and +20 warmth.
     */
    @Test
    void curryClubSpecialRestoresWarmthAndHunger() {
        player.setWarmth(20f);
        player.setHunger(10f);
        inventory.addItem(Material.CURRY_CLUB_SPECIAL, 1);

        boolean consumed = interactionSystem.consumeFood(Material.CURRY_CLUB_SPECIAL, player, inventory);

        assertTrue(consumed, "Curry Club Special should be consumable");
        assertEquals(40f, player.getWarmth(), 0.01f,
                "Curry Club Special should restore +20 warmth. Expected 40, got " + player.getWarmth());
        assertEquals(90f, player.getHunger(), 0.01f,
                "Curry Club Special should restore +80 hunger. Expected 90, got " + player.getHunger());
    }

    /**
     * Hot chips should restore warmth.
     */
    @Test
    void chipsRestoreWarmth() {
        player.setWarmth(50f);
        inventory.addItem(Material.CHIPS, 1);

        float warmthBefore = player.getWarmth();
        boolean consumed = interactionSystem.consumeFood(Material.CHIPS, player, inventory);

        assertTrue(consumed, "Chips should be consumable");
        assertTrue(player.getWarmth() > warmthBefore,
                "Hot chips should restore warmth. Before: " + warmthBefore + ", after: " + player.getWarmth());
    }

    /**
     * A pint should restore warmth (liquid warmth, however fleeting).
     */
    @Test
    void pintRestoresWarmth() {
        player.setWarmth(50f);
        inventory.addItem(Material.PINT, 1);

        float warmthBefore = player.getWarmth();
        boolean consumed = interactionSystem.consumeFood(Material.PINT, player, inventory);

        assertTrue(consumed, "Pint should be consumable");
        assertTrue(player.getWarmth() > warmthBefore,
                "A pint should restore warmth. Before: " + warmthBefore + ", after: " + player.getWarmth());
    }

    /**
     * Cold items like crisps should NOT restore warmth.
     */
    @Test
    void crispsDontRestoreWarmth() {
        player.setWarmth(50f);
        inventory.addItem(Material.CRISPS, 1);

        float warmthBefore = player.getWarmth();
        boolean consumed = interactionSystem.consumeFood(Material.CRISPS, player, inventory);

        assertTrue(consumed, "Crisps should be consumable");
        assertEquals(warmthBefore, player.getWarmth(), 0.01f,
                "Crisps should NOT restore warmth. Expected " + warmthBefore + ", got " + player.getWarmth());
    }

    /**
     * Cold items like energy drink should NOT restore warmth.
     */
    @Test
    void energyDrinkDoesntRestoreWarmth() {
        player.setWarmth(50f);
        inventory.addItem(Material.ENERGY_DRINK, 1);

        float warmthBefore = player.getWarmth();
        interactionSystem.consumeFood(Material.ENERGY_DRINK, player, inventory);

        assertEquals(warmthBefore, player.getWarmth(), 0.01f,
                "Energy drink should NOT restore warmth. Expected " + warmthBefore + ", got " + player.getWarmth());
    }

    /**
     * A player critically cold (warmth = 5) can recover above the danger threshold
     * by drinking a Builder's Tea (+30 warmth), pushing them above WARMTH_DANGER_THRESHOLD (20).
     */
    @Test
    void hotDrinkRescuesPlayerFromHypothermia() {
        player.setWarmth(5f);
        assertTrue(player.isWarmthDangerous(),
                "Player should be in danger at warmth=5");

        inventory.addItem(Material.BUILDER_S_TEA, 1);
        interactionSystem.consumeFood(Material.BUILDER_S_TEA, player, inventory);

        assertFalse(player.isWarmthDangerous(),
                "After drinking Builder's Tea, player should no longer be in warmth danger. Warmth=" + player.getWarmth());
        assertEquals(35f, player.getWarmth(), 0.01f,
                "Warmth should be 5 + 30 = 35 after Builder's Tea. Got " + player.getWarmth());
    }

    /**
     * isFood() should recognise all hot food and drink items as consumable.
     */
    @Test
    void hotFoodItemsAreRecognisedAsConsumable() {
        assertTrue(interactionSystem.isFood(Material.MUG_OF_TEA), "MUG_OF_TEA should be food");
        assertTrue(interactionSystem.isFood(Material.BUILDER_S_TEA), "BUILDER_S_TEA should be food");
        assertTrue(interactionSystem.isFood(Material.FULL_ENGLISH), "FULL_ENGLISH should be food");
        assertTrue(interactionSystem.isFood(Material.BACON_BUTTY), "BACON_BUTTY should be food");
        assertTrue(interactionSystem.isFood(Material.BEANS_ON_TOAST), "BEANS_ON_TOAST should be food");
        assertTrue(interactionSystem.isFood(Material.FRIED_BREAD), "FRIED_BREAD should be food");
        assertTrue(interactionSystem.isFood(Material.CURRY_CLUB_SPECIAL), "CURRY_CLUB_SPECIAL should be food");
        assertTrue(interactionSystem.isFood(Material.SOUP_CUP), "SOUP_CUP should be food");
        assertTrue(interactionSystem.isFood(Material.CURRY_AND_RICE), "CURRY_AND_RICE should be food");
        assertTrue(interactionSystem.isFood(Material.SAMOSA), "SAMOSA should be food");
        assertTrue(interactionSystem.isFood(Material.WATER_BOTTLE), "WATER_BOTTLE should be food");
        assertTrue(interactionSystem.isFood(Material.CHOCOLATE_BAR), "CHOCOLATE_BAR should be food");
    }

    /**
     * Warmth is capped at MAX_WARMTH (100) when consuming multiple warming items.
     */
    @Test
    void warmthIsCapedAtMax() {
        player.setWarmth(90f);
        inventory.addItem(Material.BUILDER_S_TEA, 1);

        interactionSystem.consumeFood(Material.BUILDER_S_TEA, player, inventory);

        assertEquals(Player.MAX_WARMTH, player.getWarmth(), 0.01f,
                "Warmth should be capped at MAX_WARMTH=" + Player.MAX_WARMTH + ". Got " + player.getWarmth());
    }
}
