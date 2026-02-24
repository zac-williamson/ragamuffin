package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #263: ANTIDEPRESSANTS item consumed silently with no effect or feedback.
 *
 * Verifies that ANTIDEPRESSANTS:
 * 1. Restores energy when consumed (meaningful gameplay effect)
 * 2. Produces a flavour feedback message via getLastConsumeMessage()
 * 3. Is removed from inventory on use
 * 4. Does not affect health or hunger
 */
class Issue263AntidepressantsTest {

    private Player player;
    private Inventory inventory;
    private InteractionSystem interactionSystem;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        inventory = new Inventory(36);
        interactionSystem = new InteractionSystem();
    }

    @Test
    void testAntidepressantsRestoresEnergy() {
        // Drain energy so there is headroom to restore
        player.setEnergy(50);
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);

        interactionSystem.consumeFood(Material.ANTIDEPRESSANTS, player, inventory);

        assertTrue(player.getEnergy() > 50,
            "ANTIDEPRESSANTS should restore some energy when consumed");
    }

    @Test
    void testAntidepressantsShowsFeedbackMessage() {
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);

        boolean consumed = interactionSystem.consumeFood(Material.ANTIDEPRESSANTS, player, inventory);

        assertTrue(consumed, "ANTIDEPRESSANTS should be consumed");
        String message = interactionSystem.getLastConsumeMessage();
        assertNotNull(message,
            "ANTIDEPRESSANTS should produce a feedback message after consumption");
        assertFalse(message.trim().isEmpty(),
            "ANTIDEPRESSANTS feedback message should not be blank");
    }

    @Test
    void testAntidepressantsRemovedFromInventoryOnUse() {
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);
        assertEquals(1, inventory.getItemCount(Material.ANTIDEPRESSANTS));

        interactionSystem.consumeFood(Material.ANTIDEPRESSANTS, player, inventory);

        assertEquals(0, inventory.getItemCount(Material.ANTIDEPRESSANTS),
            "ANTIDEPRESSANTS should be removed from inventory on use");
    }

    @Test
    void testAntidepressantsDoesNotAffectHealthOrHunger() {
        player.setHealth(60);
        player.setHunger(40);
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);

        interactionSystem.consumeFood(Material.ANTIDEPRESSANTS, player, inventory);

        assertEquals(60, player.getHealth(), 0.01,
            "ANTIDEPRESSANTS should not change health");
        assertEquals(40, player.getHunger(), 0.01,
            "ANTIDEPRESSANTS should not change hunger");
    }

    @Test
    void testLastConsumeMessageClearedOnNewConsumption() {
        // Consume antidepressants first to set the message
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);
        interactionSystem.consumeFood(Material.ANTIDEPRESSANTS, player, inventory);
        String antidepressantsMsg = interactionSystem.getLastConsumeMessage();
        assertNotNull(antidepressantsMsg);

        // Consume another food item — each consumable now sets its own distinct message
        inventory.addItem(Material.SAUSAGE_ROLL, 1);
        interactionSystem.consumeFood(Material.SAUSAGE_ROLL, player, inventory);
        String sausageRollMsg = interactionSystem.getLastConsumeMessage();
        assertNotNull(sausageRollMsg,
            "lastConsumeMessage should be set after consuming SAUSAGE_ROLL (Issue #271 fix)");
        assertNotEquals(antidepressantsMsg, sausageRollMsg,
            "Each consumable should have its own distinct feedback message");
    }
}
