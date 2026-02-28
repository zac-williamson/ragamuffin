package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Quest.ObjectiveType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.world.LandmarkType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #873: Increase variety of quest objectives.
 *
 * Quest objectives now include all three types (COLLECT, DELIVER, EXPLORE) to
 * provide more varied gameplay. DELIVER quests complete when the player takes
 * items to the target landmark rather than back to the quest giver.
 */
class Issue873QuestObjectiveVarietyTest {

    private BuildingQuestRegistry registry;
    private InteractionSystem interactionSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        registry = new BuildingQuestRegistry();
        interactionSystem = new InteractionSystem();
        inventory = new Inventory(36);
    }

    // -----------------------------------------------------------------------
    // 1. All three objective types are present
    // -----------------------------------------------------------------------

    /**
     * The registry must contain at least one COLLECT quest.
     */
    @Test
    void registry_hasCollectQuests() {
        long collectCount = countQuestsByType(registry, ObjectiveType.COLLECT);
        assertTrue(collectCount >= 1, "Registry must have at least 1 COLLECT quest");
    }

    /**
     * The registry must contain at least one EXPLORE quest.
     */
    @Test
    void registry_hasExploreQuests() {
        long exploreCount = countQuestsByType(registry, ObjectiveType.EXPLORE);
        assertTrue(exploreCount >= 1, "Registry must have at least 1 EXPLORE quest");
    }

    /**
     * The registry must contain at least one DELIVER quest.
     * DELIVER quests are the new type added to increase variety.
     */
    @Test
    void registry_hasDeliverQuests() {
        long deliverCount = countQuestsByType(registry, ObjectiveType.DELIVER);
        assertTrue(deliverCount >= 1,
                "Registry must have at least 1 DELIVER quest (new type for variety, Issue #873)");
    }

    /**
     * The registry must have a meaningful spread across types —
     * at least 2 of each type to avoid any single type dominating.
     */
    @Test
    void registry_hasAtLeastTwoOfEachType() {
        long collectCount = countQuestsByType(registry, ObjectiveType.COLLECT);
        long exploreCount = countQuestsByType(registry, ObjectiveType.EXPLORE);
        long deliverCount = countQuestsByType(registry, ObjectiveType.DELIVER);

        assertTrue(collectCount >= 2, "Registry must have at least 2 COLLECT quests, got: " + collectCount);
        assertTrue(exploreCount >= 2, "Registry must have at least 2 EXPLORE quests, got: " + exploreCount);
        assertTrue(deliverCount >= 2, "Registry must have at least 2 DELIVER quests, got: " + deliverCount);
    }

    // -----------------------------------------------------------------------
    // 2. DELIVER quest structure is valid
    // -----------------------------------------------------------------------

    /**
     * Every DELIVER quest must have a non-null target landmark.
     */
    @Test
    void allDeliverQuests_haveTargetLandmark() {
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = registry.getQuest(type);
            if (q == null || q.getType() != ObjectiveType.DELIVER) continue;
            assertNotNull(q.getTargetLandmark(),
                    "DELIVER quest for " + type + " must have a target landmark");
        }
    }

    /**
     * Every DELIVER quest must have a non-null required material.
     */
    @Test
    void allDeliverQuests_haveRequiredMaterial() {
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = registry.getQuest(type);
            if (q == null || q.getType() != ObjectiveType.DELIVER) continue;
            assertNotNull(q.getRequiredMaterial(),
                    "DELIVER quest for " + type + " must specify a required material");
        }
    }

    /**
     * Every DELIVER quest's target landmark must differ from the quest origin.
     */
    @Test
    void allDeliverQuests_targetIsDifferentFromOrigin() {
        for (LandmarkType origin : LandmarkType.values()) {
            Quest q = registry.getQuest(origin);
            if (q == null || q.getType() != ObjectiveType.DELIVER) continue;
            assertNotEquals(origin, q.getTargetLandmark(),
                    "DELIVER quest for " + origin + " must target a different landmark");
        }
    }

    // -----------------------------------------------------------------------
    // 3. DELIVER quest lifecycle — item check before heading out
    // -----------------------------------------------------------------------

    /**
     * A DELIVER quest is not completable without the required items.
     */
    @Test
    void deliverQuest_notCompletableWithoutItems() {
        Quest deliverQuest = findFirstDeliverQuest(registry);
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        deliverQuest.setActive(true);

        Inventory emptyInventory = new Inventory(9);
        assertFalse(deliverQuest.checkCompletion(emptyInventory),
                "DELIVER quest must not be completable without the required items");
    }

    /**
     * A DELIVER quest IS completable once the player has the required items.
     */
    @Test
    void deliverQuest_completableWithItems() {
        Quest deliverQuest = findFirstDeliverQuest(registry);
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        deliverQuest.setActive(true);

        Inventory inv = new Inventory(9);
        inv.addItem(deliverQuest.getRequiredMaterial(), deliverQuest.getRequiredCount());

        assertTrue(deliverQuest.checkCompletion(inv),
                "DELIVER quest must be completable when player has the required items");
    }

    /**
     * Completing a DELIVER quest removes the required items and awards the reward.
     */
    @Test
    void deliverQuest_completionRemovesItemsAndAwardsReward() {
        Quest deliverQuest = findFirstDeliverQuest(registry);
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        deliverQuest.setActive(true);

        Inventory inv = new Inventory(9);
        inv.addItem(deliverQuest.getRequiredMaterial(), deliverQuest.getRequiredCount());

        boolean success = deliverQuest.complete(inv);

        assertTrue(success, "DELIVER quest complete() must return true");
        assertTrue(deliverQuest.isCompleted(), "DELIVER quest must be marked completed");
        assertFalse(deliverQuest.isActive(), "DELIVER quest must no longer be active");
        assertEquals(0, inv.getItemCount(deliverQuest.getRequiredMaterial()),
                "Required items must be removed on DELIVER quest completion");
        if (deliverQuest.getReward() != null) {
            assertEquals(deliverQuest.getRewardCount(),
                    inv.getItemCount(deliverQuest.getReward()),
                    "Reward must be added on DELIVER quest completion");
        }
    }

    // -----------------------------------------------------------------------
    // 4. DELIVER quest completes at TARGET via InteractionSystem
    // -----------------------------------------------------------------------

    /**
     * When the player speaks to an NPC at the DELIVER target while carrying
     * the required items, the quest completes automatically.
     */
    @Test
    void deliverQuest_completesAtTargetLandmark() {
        // Find a DELIVER quest and activate it
        LandmarkType originLandmark = null;
        Quest deliverQuest = null;
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = interactionSystem.getQuestRegistry().getQuest(type);
            if (q != null && q.getType() == ObjectiveType.DELIVER) {
                originLandmark = type;
                deliverQuest = q;
                break;
            }
        }
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        assertNotNull(originLandmark);
        deliverQuest.setActive(true);

        // Give the player the required items
        inventory.addItem(deliverQuest.getRequiredMaterial(), deliverQuest.getRequiredCount());

        // Player speaks to an NPC at the TARGET landmark (not the origin)
        LandmarkType targetLandmark = deliverQuest.getTargetLandmark();
        NPC targetNPC = new NPC(NPCType.SHOPKEEPER, 50, 1, 50);
        targetNPC.setBuildingType(targetLandmark);

        String dialogue = interactionSystem.interactWithNPC(targetNPC, inventory, null, Collections.emptyList());

        assertTrue(deliverQuest.isCompleted(),
                "DELIVER quest must be completed after player speaks to target NPC with items");
        assertNotNull(dialogue, "Target NPC must say something when delivery is made");
    }

    /**
     * The DELIVER quest does NOT complete at the origin building — player must go
     * to the target first.
     */
    @Test
    void deliverQuest_doesNotCompleteAtOriginWithItems() {
        // Find a DELIVER quest
        LandmarkType originLandmark = null;
        Quest deliverQuest = null;
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = interactionSystem.getQuestRegistry().getQuest(type);
            if (q != null && q.getType() == ObjectiveType.DELIVER) {
                originLandmark = type;
                deliverQuest = q;
                break;
            }
        }
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        assertNotNull(originLandmark);

        // Step 1: player speaks to origin NPC — quest is offered (not yet active)
        assertFalse(deliverQuest.isActive());
        NPC originNPC = new NPC(NPCType.SHOPKEEPER, 10, 1, 10);
        originNPC.setBuildingType(originLandmark);
        interactionSystem.interactWithNPC(originNPC, inventory, null, Collections.emptyList());
        assertTrue(deliverQuest.isActive(), "Quest must be activated on first interaction");

        // Step 2: player gets the items and returns to origin — quest should NOT complete here
        inventory.addItem(deliverQuest.getRequiredMaterial(), deliverQuest.getRequiredCount());
        interactionSystem.interactWithNPC(originNPC, inventory, null, Collections.emptyList());

        assertFalse(deliverQuest.isCompleted(),
                "DELIVER quest must NOT complete at origin — player must take items to the target");
    }

    // -----------------------------------------------------------------------
    // 5. DELIVER quest reminder includes target location
    // -----------------------------------------------------------------------

    /**
     * When player has the items, the reminder line must mention the target location.
     */
    @Test
    void deliverQuestReminder_withItems_mentionsTarget() {
        Quest deliverQuest = findFirstDeliverQuest(registry);
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        deliverQuest.setActive(true);

        Inventory inv = new Inventory(9);
        inv.addItem(deliverQuest.getRequiredMaterial(), deliverQuest.getRequiredCount());

        String reminder = BuildingQuestRegistry.getQuestReminderLine(deliverQuest, inv);

        assertNotNull(reminder, "Reminder line must not be null");
        assertFalse(reminder.isBlank(), "Reminder line must not be blank");
        assertNotEquals("You haven't finished the job yet.", reminder,
                "DELIVER quest reminder must be specific, not the generic fallback");
    }

    /**
     * When player lacks the items, the reminder still mentions how many more are needed.
     */
    @Test
    void deliverQuestReminder_withoutItems_mentionsShortfall() {
        Quest deliverQuest = findFirstDeliverQuest(registry);
        assertNotNull(deliverQuest, "Must have at least one DELIVER quest");
        deliverQuest.setActive(true);

        Inventory emptyInv = new Inventory(9);

        String reminder = BuildingQuestRegistry.getQuestReminderLine(deliverQuest, emptyInv);

        assertNotNull(reminder, "Reminder line must not be null");
        assertFalse(reminder.isBlank(), "Reminder line must not be blank");
        assertNotEquals("You haven't finished the job yet.", reminder,
                "DELIVER quest reminder must be specific, not the generic fallback");
    }

    // -----------------------------------------------------------------------
    // 6. No material overused across COLLECT quests
    // -----------------------------------------------------------------------

    /**
     * No single material should appear as the required material in more than 2
     * COLLECT quests — this prevents the repetitive gameplay of always hunting
     * for the same item.
     */
    @Test
    void collectQuests_noMaterialUsedMoreThanTwice() {
        Map<Material, Integer> materialCount = new EnumMap<>(Material.class);
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = registry.getQuest(type);
            if (q == null || q.getType() != ObjectiveType.COLLECT) continue;
            if (q.getRequiredMaterial() == null) continue;
            materialCount.merge(q.getRequiredMaterial(), 1, Integer::sum);
        }
        for (Map.Entry<Material, Integer> entry : materialCount.entrySet()) {
            assertTrue(entry.getValue() <= 2,
                    "Material " + entry.getKey() + " appears in " + entry.getValue()
                    + " COLLECT quests — max 2 allowed for variety (Issue #873)");
        }
    }

    // -----------------------------------------------------------------------
    // 7. Specific new DELIVER quests exist
    // -----------------------------------------------------------------------

    /**
     * The TESCO_EXPRESS quest must now be a DELIVER quest (deliver to food bank).
     */
    @Test
    void tescoQuest_isDeliverType() {
        Quest q = registry.getQuest(LandmarkType.TESCO_EXPRESS);
        assertNotNull(q, "TESCO_EXPRESS quest must exist");
        assertEquals(ObjectiveType.DELIVER, q.getType(),
                "TESCO_EXPRESS quest must be DELIVER type after Issue #873 rework");
        assertNotNull(q.getTargetLandmark(),
                "TESCO_EXPRESS DELIVER quest must have a target landmark");
    }

    /**
     * The OFF_LICENCE quest must now be a DELIVER quest (deliver pints to pub).
     */
    @Test
    void offLicenceQuest_isDeliverType() {
        Quest q = registry.getQuest(LandmarkType.OFF_LICENCE);
        assertNotNull(q, "OFF_LICENCE quest must exist");
        assertEquals(ObjectiveType.DELIVER, q.getType(),
                "OFF_LICENCE quest must be DELIVER type after Issue #873 rework");
        assertNotNull(q.getTargetLandmark(),
                "OFF_LICENCE DELIVER quest must have a target landmark");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private long countQuestsByType(BuildingQuestRegistry reg, ObjectiveType type) {
        long count = 0;
        for (LandmarkType landmark : LandmarkType.values()) {
            Quest q = reg.getQuest(landmark);
            if (q != null && q.getType() == type) count++;
        }
        return count;
    }

    private Quest findFirstDeliverQuest(BuildingQuestRegistry reg) {
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = reg.getQuest(type);
            if (q != null && q.getType() == ObjectiveType.DELIVER) return q;
        }
        return null;
    }
}
