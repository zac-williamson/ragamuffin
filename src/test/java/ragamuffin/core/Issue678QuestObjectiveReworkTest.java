package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Quest.ObjectiveType;
import ragamuffin.world.LandmarkType;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #678: Rework quest objectives to not require destroying the
 * quest origin building.
 *
 * Previously, many quests asked players to collect items that only drop from
 * breaking the quest-giver's own building (e.g. the Greggs quest wanted sausage
 * rolls, which only drop from Greggs blocks).  These quests have been reworked
 * to use either:
 *   - COLLECT objectives requiring materials from *different* buildings, or
 *   - EXPLORE objectives (visit a landmark) which never require destruction.
 */
class Issue678QuestObjectiveReworkTest {

    private BuildingQuestRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BuildingQuestRegistry();
    }

    // -----------------------------------------------------------------------
    // 1. EXPLORE quest lifecycle
    // -----------------------------------------------------------------------

    /**
     * An EXPLORE quest is not completable before the target location is visited.
     */
    @Test
    void exploreQuest_notCompletableBeforeVisit() {
        Quest quest = new Quest("explore_test", "Test NPC",
                "Go visit the park.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.PARK, Material.CRISPS, 2);
        quest.setActive(true);

        Inventory inventory = new Inventory(9);
        assertFalse(quest.checkCompletion(inventory),
                "EXPLORE quest must not be completable before visiting the target");
    }

    /**
     * An EXPLORE quest becomes completable after markLocationVisited().
     */
    @Test
    void exploreQuest_completableAfterVisit() {
        Quest quest = new Quest("explore_test", "Test NPC",
                "Go visit the park.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.PARK, Material.CRISPS, 2);
        quest.setActive(true);

        quest.markLocationVisited();

        Inventory inventory = new Inventory(9);
        assertTrue(quest.checkCompletion(inventory),
                "EXPLORE quest must be completable after the target is visited");
    }

    /**
     * Completing an EXPLORE quest awards the reward and marks it done.
     */
    @Test
    void exploreQuest_completionAwardsRewardAndMarksDone() {
        Quest quest = new Quest("explore_test", "Test NPC",
                "Go visit the park.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.PARK, Material.CRISPS, 2);
        quest.setActive(true);
        quest.markLocationVisited();

        Inventory inventory = new Inventory(9);
        boolean success = quest.complete(inventory);

        assertTrue(success, "complete() should succeed for a visited EXPLORE quest");
        assertTrue(quest.isCompleted(), "Quest must be marked completed");
        assertFalse(quest.isActive(), "Quest must no longer be active");
        assertEquals(2, inventory.getItemCount(Material.CRISPS),
                "Reward must be added to inventory");
    }

    /**
     * Completing an EXPLORE quest without an inventory (null) still marks it done
     * but awards no item (graceful degradation).
     */
    @Test
    void exploreQuest_completionWithNullInventoryMarksDone() {
        Quest quest = new Quest("explore_test", "Test NPC",
                "Go visit the park.",
                ObjectiveType.EXPLORE, null, 1,
                LandmarkType.PARK, Material.CRISPS, 2);
        quest.setActive(true);
        quest.markLocationVisited();

        boolean success = quest.complete(null);

        assertTrue(success, "complete(null) should succeed for a visited EXPLORE quest");
        assertTrue(quest.isCompleted(), "Quest must be marked completed");
    }

    // -----------------------------------------------------------------------
    // 2. InteractionSystem.onPlayerEntersLandmark()
    // -----------------------------------------------------------------------

    /**
     * When the player enters the target landmark for an active EXPLORE quest,
     * the quest should be marked as visited.
     */
    @Test
    void onPlayerEntersLandmark_marksExploreQuestVisited() {
        InteractionSystem system = new InteractionSystem();

        // Activate the PUB quest (EXPLORE: visit JOB_CENTRE)
        Quest pubQuest = system.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(pubQuest, "PUB quest must exist");
        assertEquals(ObjectiveType.EXPLORE, pubQuest.getType(),
                "PUB quest must be an EXPLORE quest after the rework");
        pubQuest.setActive(true);

        assertFalse(pubQuest.isLocationVisited(), "Location must not be visited yet");

        system.onPlayerEntersLandmark(pubQuest.getTargetLandmark());

        assertTrue(pubQuest.isLocationVisited(),
                "Location must be marked visited after onPlayerEntersLandmark()");
    }

    /**
     * onPlayerEntersLandmark() should not affect quests for a *different* target.
     */
    @Test
    void onPlayerEntersLandmark_doesNotAffectOtherExploreQuests() {
        InteractionSystem system = new InteractionSystem();

        Quest pubQuest = system.getQuestRegistry().getQuest(LandmarkType.PUB);
        Quest launderetteQuest = system.getQuestRegistry().getQuest(LandmarkType.LAUNDERETTE);
        assertNotNull(pubQuest);
        assertNotNull(launderetteQuest);
        pubQuest.setActive(true);
        launderetteQuest.setActive(true);

        // Enter the PUB quest's target — should NOT affect launderette quest
        system.onPlayerEntersLandmark(pubQuest.getTargetLandmark());

        assertFalse(launderetteQuest.isLocationVisited(),
                "Visiting PUB's target must not mark LAUNDERETTE quest as visited");
    }

    /**
     * onPlayerEntersLandmark() should not mark an inactive quest as visited.
     */
    @Test
    void onPlayerEntersLandmark_ignoresInactiveQuests() {
        InteractionSystem system = new InteractionSystem();

        Quest pubQuest = system.getQuestRegistry().getQuest(LandmarkType.PUB);
        assertNotNull(pubQuest);
        // Do NOT activate the quest
        assertFalse(pubQuest.isActive());

        system.onPlayerEntersLandmark(pubQuest.getTargetLandmark());

        assertFalse(pubQuest.isLocationVisited(),
                "Inactive quest must not be marked visited");
    }

    // -----------------------------------------------------------------------
    // 3. Specific formerly-self-destructive quests are now reworked
    // -----------------------------------------------------------------------

    /**
     * The GREGGS quest must no longer require sausage rolls (which only drop
     * from Greggs blocks).
     */
    @Test
    void greggsQuest_doesNotRequireSausageRoll() {
        Quest q = registry.getQuest(LandmarkType.GREGGS);
        assertNotNull(q);
        assertNotEquals(Material.SAUSAGE_ROLL, q.getRequiredMaterial(),
                "GREGGS quest must not require sausage rolls (they only drop from Greggs blocks)");
    }

    /**
     * The PUB quest must be an EXPLORE quest (no longer requires pints from pub blocks).
     */
    @Test
    void pubQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.PUB);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "PUB quest must be EXPLORE type after rework");
        assertNotNull(q.getTargetLandmark(),
                "PUB EXPLORE quest must have a target landmark");
    }

    /**
     * The WETHERSPOONS quest must be an EXPLORE quest (no longer requires pints).
     */
    @Test
    void wetherspoonsQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.WETHERSPOONS);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "WETHERSPOONS quest must be EXPLORE type after rework");
        assertNotNull(q.getTargetLandmark(),
                "WETHERSPOONS EXPLORE quest must have a target landmark");
    }

    /**
     * The CHIPPY quest must be an EXPLORE quest (no longer requires chips from chippy blocks).
     */
    @Test
    void chippyQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.CHIPPY);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "CHIPPY quest must be EXPLORE type after rework");
    }

    /**
     * The NEWSAGENT quest must be an EXPLORE quest (no longer requires newspapers from newsagent blocks).
     */
    @Test
    void newsagentQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.NEWSAGENT);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "NEWSAGENT quest must be EXPLORE type after rework");
    }

    /**
     * The LAUNDERETTE quest must be an EXPLORE quest (no longer requires washing powder from launderette blocks).
     */
    @Test
    void launderetteQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.LAUNDERETTE);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "LAUNDERETTE quest must be EXPLORE type after rework");
    }

    /**
     * The GP_SURGERY quest must be an EXPLORE quest (no longer requires paracetamol from surgery blocks).
     */
    @Test
    void gpSurgeryQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.GP_SURGERY);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "GP_SURGERY quest must be EXPLORE type after rework");
    }

    /**
     * The BARBER quest must be an EXPLORE quest (no longer requires clippers from barber blocks).
     */
    @Test
    void barberQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.BARBER);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "BARBER quest must be EXPLORE type after rework");
    }

    /**
     * The NAIL_SALON quest must be an EXPLORE quest.
     */
    @Test
    void nailSalonQuest_isExploreType() {
        Quest q = registry.getQuest(LandmarkType.NAIL_SALON);
        assertNotNull(q);
        assertEquals(ObjectiveType.EXPLORE, q.getType(),
                "NAIL_SALON quest must be EXPLORE type after rework");
    }

    /**
     * The BOOKIES quest must not require scratch cards (which only drop from bookies blocks).
     */
    @Test
    void bookiesQuest_doesNotRequireScratchCards() {
        Quest q = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(q);
        assertNotEquals(Material.SCRATCH_CARD, q.getRequiredMaterial(),
                "BOOKIES quest must not require scratch cards (they only drop from Bookies blocks)");
    }

    // -----------------------------------------------------------------------
    // 4. EXPLORE quests all have a valid target landmark
    // -----------------------------------------------------------------------

    /**
     * Every EXPLORE quest in the registry must have a non-null target landmark.
     */
    @Test
    void allExploreQuests_haveTargetLandmark() {
        for (LandmarkType type : LandmarkType.values()) {
            Quest q = registry.getQuest(type);
            if (q == null) continue;
            if (q.getType() == ObjectiveType.EXPLORE) {
                assertNotNull(q.getTargetLandmark(),
                        "EXPLORE quest for " + type + " must have a target landmark");
            }
        }
    }

    /**
     * Every EXPLORE quest's target landmark must be different from its origin landmark.
     * (The quest must not send the player to the same building that gave the quest.)
     */
    @Test
    void allExploreQuests_targetIsDifferentFromOrigin() {
        for (LandmarkType origin : LandmarkType.values()) {
            Quest q = registry.getQuest(origin);
            if (q == null) continue;
            if (q.getType() == ObjectiveType.EXPLORE && q.getTargetLandmark() != null) {
                assertNotEquals(origin, q.getTargetLandmark(),
                        "EXPLORE quest for " + origin + " must target a different landmark, not itself");
            }
        }
    }

    // -----------------------------------------------------------------------
    // 5. Reminder lines for EXPLORE quests are non-empty
    // -----------------------------------------------------------------------

    /**
     * getQuestReminderLine() for an EXPLORE quest must return a non-blank message
     * that references the target location.
     */
    @Test
    void exploreQuestReminderLine_isNonBlankAndReferencesTarget() {
        Quest pubQuest = registry.getQuest(LandmarkType.PUB);
        assertNotNull(pubQuest);
        pubQuest.setActive(true);

        String reminder = BuildingQuestRegistry.getQuestReminderLine(pubQuest, null);

        assertNotNull(reminder, "Reminder must not be null");
        assertFalse(reminder.isBlank(), "Reminder must not be blank");
        // The reminder should mention the target location in some form
        assertNotEquals("You haven't finished the job yet.", reminder,
                "EXPLORE quest reminder must be specific, not the generic fallback");
    }

    // -----------------------------------------------------------------------
    // 6. COLLECT quests that were reworked are completable without destruction
    // -----------------------------------------------------------------------

    /**
     * The KEBAB_SHOP quest now asks for TIN_OF_BEANS (from Tesco/corner shop),
     * and must be completable by providing those items without touching kebab shop blocks.
     */
    @Test
    void kebabShopQuest_completableWithTinOfBeans() {
        Quest q = registry.getQuest(LandmarkType.KEBAB_SHOP);
        assertNotNull(q);
        assertEquals(ObjectiveType.COLLECT, q.getType(),
                "KEBAB_SHOP quest should be COLLECT type");
        assertEquals(Material.TIN_OF_BEANS, q.getRequiredMaterial(),
                "KEBAB_SHOP quest should now require tin of beans");

        q.setActive(true);
        Inventory inv = new Inventory(9);
        inv.addItem(Material.TIN_OF_BEANS, q.getRequiredCount());

        assertTrue(q.checkCompletion(inv),
                "KEBAB_SHOP quest must be completable by providing tin of beans");
    }
}
