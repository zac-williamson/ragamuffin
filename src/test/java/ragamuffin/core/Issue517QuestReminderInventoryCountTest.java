package ragamuffin.core;

import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Quest.ObjectiveType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #517 — NPC quest reminder dialogue should include the player's
 * current item count when a partial collection is in progress.
 */
class Issue517QuestReminderInventoryCountTest {

    /**
     * When the player has partial items and an inventory is provided,
     * the reminder line should contain the current count and the remaining count (not total required).
     */
    @Test
    void getQuestReminderLine_withInventory_includesCurrentAndRemainingCount() {
        Quest quest = new Quest("test_quest", "Test NPC",
                "Bring me 3 tins of beans.",
                ObjectiveType.COLLECT, Material.TIN_OF_BEANS, 3,
                Material.CRISPS, 5);
        quest.setActive(true);

        Inventory inventory = new Inventory(36);
        inventory.addItem(Material.TIN_OF_BEANS, 1);

        String reminder = BuildingQuestRegistry.getQuestReminderLine(quest, inventory);

        assertNotNull(reminder);
        assertTrue(reminder.contains("1"),
                "Reminder should contain the current count (1), got: " + reminder);
        // remaining = 3 - 1 = 2; the message should say "need 2 more", not "still need 3 total"
        assertTrue(reminder.contains("2"),
                "Reminder should contain the remaining count (2), got: " + reminder);
        assertFalse(reminder.contains("total"),
                "Reminder should not say 'total' (misleading phrasing), got: " + reminder);
    }

    /**
     * When no inventory is provided, the reminder falls back to the static
     * form (required count only) — no regression.
     */
    @Test
    void getQuestReminderLine_withoutInventory_usesStaticForm() {
        Quest quest = new Quest("test_quest", "Test NPC",
                "Bring me 3 tins of beans.",
                ObjectiveType.COLLECT, Material.TIN_OF_BEANS, 3,
                Material.CRISPS, 5);
        quest.setActive(true);

        String reminder = BuildingQuestRegistry.getQuestReminderLine(quest, null);

        assertNotNull(reminder);
        assertTrue(reminder.contains("3"),
                "Reminder should contain the required count (3), got: " + reminder);
        // The static form should not show "0/" or similar
        assertFalse(reminder.contains("/"),
                "Static reminder should not contain a slash separator, got: " + reminder);
    }

    /**
     * Zero-argument overload delegates to the inventory-null path — no regression.
     */
    @Test
    void getQuestReminderLine_zeroArgOverload_matchesNullInventory() {
        Quest quest = new Quest("test_quest", "Test NPC",
                "Bring me 3 tins of beans.",
                ObjectiveType.COLLECT, Material.TIN_OF_BEANS, 3,
                Material.CRISPS, 5);
        quest.setActive(true);

        String fromOverload = BuildingQuestRegistry.getQuestReminderLine(quest);
        String fromNull = BuildingQuestRegistry.getQuestReminderLine(quest, null);

        assertEquals(fromNull, fromOverload,
                "Zero-arg overload should produce the same result as passing null inventory");
    }

    /**
     * When the player has zero of the required item, the reminder should still
     * show "0" as the current count.
     */
    @Test
    void getQuestReminderLine_withInventory_zeroItems_showsZeroCount() {
        Quest quest = new Quest("test_quest", "Test NPC",
                "Bring me 2 newspapers.",
                ObjectiveType.COLLECT, Material.NEWSPAPER, 2,
                Material.TIN_OF_BEANS, 1);
        quest.setActive(true);

        Inventory inventory = new Inventory(36);
        // No newspapers added

        String reminder = BuildingQuestRegistry.getQuestReminderLine(quest, inventory);

        assertNotNull(reminder);
        assertTrue(reminder.contains("0"),
                "Reminder should contain '0' as current count, got: " + reminder);
        assertTrue(reminder.contains("2"),
                "Reminder should contain '2' as required count, got: " + reminder);
    }
}
