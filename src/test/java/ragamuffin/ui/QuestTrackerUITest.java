package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        ui = new QuestTrackerUI(registry);
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
}
