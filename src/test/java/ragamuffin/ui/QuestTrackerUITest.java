package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.Quest;
import ragamuffin.world.LandmarkType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuestTrackerUI — visibility, active quest filtering.
 */
class QuestTrackerUITest {

    private BuildingQuestRegistry registry;
    private QuestTrackerUI ui;

    @BeforeEach
    void setUp() {
        registry = new BuildingQuestRegistry();
        ui = new QuestTrackerUI(registry, null);
    }

    // --- Visibility ---

    @Test
    void initiallyVisible() {
        assertTrue(ui.isVisible(), "QuestTrackerUI must be visible on creation");
    }

    @Test
    void hideMakesInvisible() {
        ui.hide();
        assertFalse(ui.isVisible(), "hide() must make the tracker invisible");
    }

    @Test
    void showMakesVisible() {
        ui.hide();
        ui.show();
        assertTrue(ui.isVisible(), "show() must make the tracker visible");
    }

    // --- Active quest list ---

    @Test
    void noActiveQuestsWhenNoneAccepted() {
        List<Quest> active = ui.getActiveQuests();
        assertTrue(active.isEmpty(), "getActiveQuests() must return empty list when no quests are active");
    }

    @Test
    void activeQuestAppearsInList() {
        Quest tesco = registry.getQuest(LandmarkType.TESCO_EXPRESS);
        assertNotNull(tesco, "Tesco quest must exist");
        tesco.setActive(true);

        List<Quest> active = ui.getActiveQuests();
        assertTrue(active.contains(tesco), "Active quest must appear in getActiveQuests()");
    }

    @Test
    void completedQuestDoesNotAppearInList() {
        Quest tesco = registry.getQuest(LandmarkType.TESCO_EXPRESS);
        assertNotNull(tesco);
        tesco.setActive(true);
        tesco.setCompleted(true);

        List<Quest> active = ui.getActiveQuests();
        assertFalse(active.contains(tesco), "Completed quest must not appear in getActiveQuests()");
    }

    @Test
    void inactiveQuestDoesNotAppearInList() {
        // Quests start inactive by default
        Quest greggs = registry.getQuest(LandmarkType.GREGGS);
        assertNotNull(greggs);
        assertFalse(greggs.isActive());

        List<Quest> active = ui.getActiveQuests();
        assertFalse(active.contains(greggs), "Inactive quest must not appear in getActiveQuests()");
    }

    @Test
    void multipleActiveQuestsAllAppear() {
        Quest tesco = registry.getQuest(LandmarkType.TESCO_EXPRESS);
        Quest greggs = registry.getQuest(LandmarkType.GREGGS);
        assertNotNull(tesco);
        assertNotNull(greggs);

        tesco.setActive(true);
        greggs.setActive(true);

        List<Quest> active = ui.getActiveQuests();
        assertTrue(active.contains(tesco), "Tesco quest must be in active list");
        assertTrue(active.contains(greggs), "Greggs quest must be in active list");
    }

    @Test
    void maxVisibleConstantIsAtLeastOne() {
        assertTrue(QuestTrackerUI.MAX_VISIBLE >= 1,
                "MAX_VISIBLE must be at least 1 so at least one quest can be shown");
    }

    // --- Objective string for COLLECT quests (Fix #519) ---
    // Using BOOKIES quest: COLLECT 3x ENERGY_DRINK (a stable COLLECT quest with count=3)

    @Test
    void objectiveStringForCollectQuestShowsCollectPrefix() {
        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies, "BOOKIES quest must exist");
        assertEquals(Quest.ObjectiveType.COLLECT, bookies.getType(),
                "BOOKIES quest must be COLLECT type");
        String obj = ui.buildObjectiveString(bookies);
        assertEquals("Collect 3x energy drink", obj,
                "COLLECT quest objective must read 'Collect Nx material'");
    }

    @Test
    void objectiveStringDoesNotExceed35Chars() {
        // buildObjectiveString for COLLECT quests uses the auto-generated form which is always short.
        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);
        String obj = ui.buildObjectiveString(bookies);
        assertTrue(obj.length() <= 35,
                "Objective string must not exceed 35 characters to fit the tracker panel");
    }

    @Test
    void objectiveStringNeverReturnsFullNPCDialogue() {
        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);
        String obj = ui.buildObjectiveString(bookies);
        assertNotEquals(bookies.getDescription(), obj,
                "Tracker must not show the full NPC dialogue as the objective");
    }

    // --- Objective string remaining count (Fix #527) ---

    @Test
    void objectiveStringShowsRemainingCountWhenInventoryHasSome() {
        Inventory inv = new Inventory(36);
        inv.addItem(Material.ENERGY_DRINK, 2);
        QuestTrackerUI uiWithInv = new QuestTrackerUI(registry, inv);

        // BOOKIES quest: COLLECT 3x ENERGY_DRINK
        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);

        String obj = uiWithInv.buildObjectiveString(bookies);
        assertEquals("Collect 1x energy drink", obj,
                "Objective must show remaining count (3-2=1) when inventory is available");
    }

    @Test
    void objectiveStringShowsZeroRemainingWhenInventoryFull() {
        Inventory inv = new Inventory(36);
        inv.addItem(Material.ENERGY_DRINK, 3);
        QuestTrackerUI uiWithInv = new QuestTrackerUI(registry, inv);

        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);

        String obj = uiWithInv.buildObjectiveString(bookies);
        assertEquals("Collect 0x energy drink", obj,
                "Objective must show 0 remaining when inventory already has required count");
    }

    @Test
    void objectiveStringNeverNegativeWhenInventoryExceedsRequired() {
        Inventory inv = new Inventory(36);
        inv.addItem(Material.ENERGY_DRINK, 5);
        QuestTrackerUI uiWithInv = new QuestTrackerUI(registry, inv);

        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);

        String obj = uiWithInv.buildObjectiveString(bookies);
        assertEquals("Collect 0x energy drink", obj,
                "Objective remaining count must not go below 0");
    }

    @Test
    void objectiveStringFallsBackToTotalWhenInventoryNull() {
        // ui was constructed with null inventory in @BeforeEach
        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);

        String obj = ui.buildObjectiveString(bookies);
        assertEquals("Collect 3x energy drink", obj,
                "Without inventory, objective must fall back to showing total required count");
    }

    // --- Objective string for DELIVER quests (Issue #873) ---

    @Test
    void objectiveStringForDeliverQuestShowsDeliverPrefix() {
        // TESCO_EXPRESS is now a DELIVER quest (Issue #873)
        Quest tesco = registry.getQuest(LandmarkType.TESCO_EXPRESS);
        assertNotNull(tesco, "TESCO_EXPRESS quest must exist");
        assertEquals(Quest.ObjectiveType.DELIVER, tesco.getType(),
                "TESCO_EXPRESS must be a DELIVER quest after Issue #873");
        String obj = ui.buildObjectiveString(tesco);
        assertTrue(obj.startsWith("Deliver "),
                "DELIVER quest objective must start with 'Deliver ': got '" + obj + "'");
        assertFalse(obj.equals(tesco.getDescription()),
                "Tracker must not show the full NPC dialogue for a DELIVER quest");
        assertTrue(obj.length() <= 50,
                "DELIVER objective string must be reasonably short: got '" + obj + "'");
    }

    // --- Progress string (Fix #511) ---

    @Test
    void progressStringWithoutInventoryShowsRequiredOnly() {
        // ui has null inventory (set up in @BeforeEach)
        // BOOKIES quest: COLLECT 3x ENERGY_DRINK
        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);
        String progress = ui.buildProgressString(bookies);
        assertEquals("3x energy drink", progress,
                "Without inventory, progress must show 'required x material'");
    }

    @Test
    void progressStringWithInventoryShowsCurrentAndRequired() {
        Inventory inv = new Inventory(36);
        inv.addItem(Material.ENERGY_DRINK, 1);
        QuestTrackerUI uiWithInv = new QuestTrackerUI(registry, inv);

        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);

        String progress = uiWithInv.buildProgressString(bookies);
        assertEquals("1/3x energy drink", progress,
                "With inventory, progress must show 'current/required x material'");
    }

    @Test
    void progressStringShowsZeroWhenInventoryEmpty() {
        Inventory inv = new Inventory(36);
        QuestTrackerUI uiWithInv = new QuestTrackerUI(registry, inv);

        Quest bookies = registry.getQuest(LandmarkType.BOOKIES);
        assertNotNull(bookies);

        String progress = uiWithInv.buildProgressString(bookies);
        assertEquals("0/3x energy drink", progress,
                "With empty inventory, current count must be 0");
    }
}
