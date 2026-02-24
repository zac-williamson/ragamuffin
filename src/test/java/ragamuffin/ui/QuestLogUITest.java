package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.BuildingQuestRegistry;
import ragamuffin.core.Quest;
import ragamuffin.world.LandmarkType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuestLogUI — visibility toggling, scrolling, and quest list ordering.
 */
class QuestLogUITest {

    private BuildingQuestRegistry registry;
    private QuestLogUI ui;

    @BeforeEach
    void setUp() {
        registry = new BuildingQuestRegistry();
        ui = new QuestLogUI(registry);
    }

    // --- Visibility ---

    @Test
    void initiallyNotVisible() {
        assertFalse(ui.isVisible(), "QuestLogUI must be hidden on creation");
    }

    @Test
    void showMakesVisible() {
        ui.show();
        assertTrue(ui.isVisible(), "show() must make the UI visible");
    }

    @Test
    void hideMakesInvisible() {
        ui.show();
        ui.hide();
        assertFalse(ui.isVisible(), "hide() must make the UI invisible");
    }

    @Test
    void toggleFlipsVisibility() {
        assertFalse(ui.isVisible());
        ui.toggle();
        assertTrue(ui.isVisible(), "toggle() must show a hidden UI");
        ui.toggle();
        assertFalse(ui.isVisible(), "toggle() must hide a visible UI");
    }

    @Test
    void showResetsScrollOffset() {
        // Manually set a non-zero scroll offset by scrolling down
        ui.show();
        for (int i = 0; i < 5; i++) {
            ui.scrollDown();
        }
        assertTrue(ui.getScrollOffset() > 0, "Scroll offset should be non-zero after scrolling");

        // Calling show() again should reset
        ui.show();
        assertEquals(0, ui.getScrollOffset(), "show() must reset scroll offset to 0");
    }

    // --- Quest list contents ---

    @Test
    void questListContainsAllRegisteredQuests() {
        List<Quest> quests = ui.getQuestList();
        assertFalse(quests.isEmpty(), "Quest list must not be empty");

        // Registry has one quest per LandmarkType that has a quest
        long expected = 0;
        for (LandmarkType type : LandmarkType.values()) {
            if (registry.getQuest(type) != null) expected++;
        }
        assertEquals(expected, quests.size(),
                "Quest list must contain exactly as many entries as the registry");
    }

    @Test
    void activeQuestsAppearBeforeInactiveOnes() {
        // Activate one quest
        Quest tesco = registry.getQuest(LandmarkType.TESCO_EXPRESS);
        assertNotNull(tesco, "Tesco quest must exist");
        tesco.setActive(true);

        List<Quest> quests = ui.getQuestList();
        int activeIndex   = quests.indexOf(tesco);
        assertTrue(activeIndex >= 0, "Active quest must appear in the list");

        // All inactive/not-yet-started quests must come after it
        for (int i = 0; i < activeIndex; i++) {
            assertTrue(quests.get(i).isActive() && !quests.get(i).isCompleted(),
                    "All quests before the active quest must also be active");
        }
    }

    @Test
    void completedQuestsAppearLast() {
        // Complete one quest
        Quest greggs = registry.getQuest(LandmarkType.GREGGS);
        assertNotNull(greggs, "Greggs quest must exist");
        greggs.setActive(true);
        greggs.setCompleted(true);

        List<Quest> quests = ui.getQuestList();
        int completedIndex = quests.indexOf(greggs);
        assertTrue(completedIndex >= 0, "Completed quest must appear in the list");

        // All quests after completed one must also be completed
        for (int i = completedIndex + 1; i < quests.size(); i++) {
            assertTrue(quests.get(i).isCompleted(),
                    "All quests after the completed quest must also be completed");
        }

        // At least one non-completed quest must appear before it
        boolean foundNonCompleted = false;
        for (int i = 0; i < completedIndex; i++) {
            if (!quests.get(i).isCompleted()) {
                foundNonCompleted = true;
                break;
            }
        }
        assertTrue(foundNonCompleted, "Non-completed quests must appear before completed quests");
    }

    // --- Scrolling ---

    @Test
    void scrollUpDoesNothingAtTopOfList() {
        ui.show();
        assertEquals(0, ui.getScrollOffset());
        ui.scrollUp();
        assertEquals(0, ui.getScrollOffset(), "scrollUp() at offset 0 must have no effect");
    }

    @Test
    void scrollDownIncreasesOffset() {
        ui.show();
        ui.scrollDown();
        assertTrue(ui.getScrollOffset() > 0, "scrollDown() must increase scroll offset");
    }

    @Test
    void scrollDownDoesNotExceedMaximum() {
        ui.show();
        // Scroll down far beyond list size
        for (int i = 0; i < 1000; i++) {
            ui.scrollDown();
        }
        int total = ui.getQuestList().size();
        int maxOffset = Math.max(0, total - 7); // VISIBLE_ROWS = 7
        assertEquals(maxOffset, ui.getScrollOffset(),
                "scrollDown() must not exceed max offset (total - VISIBLE_ROWS)");
    }

    @Test
    void scrollUpAfterScrollDownRestoresOffset() {
        ui.show();
        ui.scrollDown();
        ui.scrollDown();
        int offsetAfterDown = ui.getScrollOffset();
        assertTrue(offsetAfterDown >= 1);
        ui.scrollUp();
        assertEquals(offsetAfterDown - 1, ui.getScrollOffset(),
                "scrollUp() must decrease offset by 1");
    }

    // --- InputHandler Q key ---

    @Test
    void inputHandlerQKeyToggle() {
        ragamuffin.core.InputHandler handler = new ragamuffin.core.InputHandler();
        assertFalse(handler.isQuestLogPressed(), "questLogPressed must start false");

        // Simulate Q key down
        handler.keyDown(com.badlogic.gdx.Input.Keys.Q);
        assertTrue(handler.isQuestLogPressed(), "questLogPressed must be true after Q key down");

        handler.resetQuestLog();
        assertFalse(handler.isQuestLogPressed(), "questLogPressed must be false after reset");
    }
}
