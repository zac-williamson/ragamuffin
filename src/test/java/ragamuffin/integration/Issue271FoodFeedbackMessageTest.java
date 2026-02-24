package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #271: Food consumption must always set a feedback
 * message via lastConsumeMessage so the tooltip system can display it.
 */
class Issue271FoodFeedbackMessageTest {

    private Player player;
    private Inventory inventory;
    private InteractionSystem interactionSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        interactionSystem = new InteractionSystem();
    }

    private void assertFeedbackMessage(Material food) {
        inventory.addItem(food, 1);
        boolean consumed = interactionSystem.consumeFood(food, player, inventory);
        assertTrue(consumed, food + " should be consumed successfully");
        String msg = interactionSystem.getLastConsumeMessage();
        assertNotNull(msg, food + " should produce a non-null feedback message");
        assertFalse(msg.isEmpty(), food + " feedback message should not be empty");
    }

    @Test
    void testSausageRollSetsFeedbackMessage() {
        assertFeedbackMessage(Material.SAUSAGE_ROLL);
    }

    @Test
    void testSteakBakeSetsFeedbackMessage() {
        assertFeedbackMessage(Material.STEAK_BAKE);
    }

    @Test
    void testChipsSetsFeedbackMessage() {
        assertFeedbackMessage(Material.CHIPS);
    }

    @Test
    void testKebabSetsFeedbackMessage() {
        assertFeedbackMessage(Material.KEBAB);
    }

    @Test
    void testCrispsSetsFeedbackMessage() {
        assertFeedbackMessage(Material.CRISPS);
    }

    @Test
    void testTinOfBeansSetsFeedbackMessage() {
        assertFeedbackMessage(Material.TIN_OF_BEANS);
    }

    @Test
    void testEnergyDrinkSetsFeedbackMessage() {
        assertFeedbackMessage(Material.ENERGY_DRINK);
    }

    @Test
    void testPintSetsFeedbackMessage() {
        assertFeedbackMessage(Material.PINT);
    }

    @Test
    void testPeriPeriChickenSetsFeedbackMessage() {
        assertFeedbackMessage(Material.PERI_PERI_CHICKEN);
    }

    @Test
    void testParacetamolSetsFeedbackMessage() {
        assertFeedbackMessage(Material.PARACETAMOL);
    }

    @Test
    void testFireExtinguisherSetsFeedbackMessage() {
        assertFeedbackMessage(Material.FIRE_EXTINGUISHER);
    }

    @Test
    void testWashingPowderSetsFeedbackMessage() {
        assertFeedbackMessage(Material.WASHING_POWDER);
    }

    @Test
    void testSausageRollMessageContent() {
        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        interactionSystem.consumeFood(Material.SAUSAGE_ROLL, player, inventory);
        assertEquals("You eat the sausage roll. Warm, flaky, perfect.",
            interactionSystem.getLastConsumeMessage());
    }

    @Test
    void testParacetamolMessageContent() {
        inventory.addItem(Material.PARACETAMOL, 1);
        interactionSystem.consumeFood(Material.PARACETAMOL, player, inventory);
        assertEquals("You take two paracetamol. That should help.",
            interactionSystem.getLastConsumeMessage());
    }

    @Test
    void testFireExtinguisherMessageContent() {
        inventory.addItem(Material.FIRE_EXTINGUISHER, 1);
        interactionSystem.consumeFood(Material.FIRE_EXTINGUISHER, player, inventory);
        assertEquals("You blast yourself with foam. Oddly soothing.",
            interactionSystem.getLastConsumeMessage());
    }

    @Test
    void testWashingPowderMessageContent() {
        inventory.addItem(Material.WASHING_POWDER, 1);
        interactionSystem.consumeFood(Material.WASHING_POWDER, player, inventory);
        assertEquals("You sniff the washing powder. Invigorating.",
            interactionSystem.getLastConsumeMessage());
    }
}
