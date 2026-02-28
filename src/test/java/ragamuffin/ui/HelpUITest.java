package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelpUITest {

    private HelpUI helpUI;

    @BeforeEach
    void setUp() {
        helpUI = new HelpUI();
    }

    @Test
    void testInitiallyNotVisible() {
        assertFalse(helpUI.isVisible());
    }

    @Test
    void testToggleVisibility() {
        helpUI.toggle();
        assertTrue(helpUI.isVisible());

        helpUI.toggle();
        assertFalse(helpUI.isVisible());
    }

    @Test
    void testShow() {
        helpUI.show();
        assertTrue(helpUI.isVisible());
    }

    @Test
    void testHide() {
        helpUI.show();
        helpUI.hide();
        assertFalse(helpUI.isVisible());
    }

    @Test
    void testGetHelpText_ContainsAllControls() {
        String helpText = helpUI.getHelpText();

        // Check for all required controls
        assertTrue(helpText.contains("WASD"), "Should contain WASD");
        assertTrue(helpText.contains("Mouse"), "Should contain Mouse");
        assertTrue(helpText.contains("Left click") || helpText.contains("Punch"), "Should contain punch/left click");
        assertTrue(helpText.contains("Right click") || helpText.contains("Place"), "Should contain place/right click");
        assertTrue(helpText.contains("I") && helpText.contains("Inventory"), "Should contain I for inventory");
        assertTrue(helpText.contains("H") && helpText.contains("Help"), "Should contain H for help");
        assertTrue(helpText.contains("C") && helpText.contains("Craft"), "Should contain C for crafting");
        assertTrue(helpText.contains("E") && helpText.contains("Interact"), "Should contain E for interact");
        assertTrue(helpText.contains("ESC"), "Should contain ESC");
        assertTrue(helpText.contains("1-9") || helpText.contains("1") && helpText.contains("9"), "Should contain 1-9 for hotbar");
        // Additional shortcuts
        assertTrue(helpText.contains("Space") || helpText.contains("Jump"), "Should contain Space/Jump");
        assertTrue(helpText.contains("Shift") || helpText.contains("Sprint"), "Should contain Shift/Sprint");
        assertTrue(helpText.contains("F") && (helpText.contains("Pickpocket") || helpText.contains("pickpocket")), "Should contain F for pickpocket");
        assertTrue(helpText.contains("K") && (helpText.contains("Skill") || helpText.contains("skill")), "Should contain K for skills");
        assertTrue(helpText.contains("T") && (helpText.contains("graffiti") || helpText.contains("Graffiti") || helpText.contains("tag") || helpText.contains("Tag")), "Should contain T for graffiti");
        assertTrue(helpText.contains("B") && (helpText.contains("radio") || helpText.contains("Radio") || helpText.contains("broadcast") || helpText.contains("Broadcast")), "Should contain B for pirate radio");
        assertTrue(helpText.contains("Q") && (helpText.contains("Quest") || helpText.contains("quest")), "Should contain Q for quest log");
        assertTrue(helpText.contains("R") && (helpText.contains("criminal") || helpText.contains("Criminal") || helpText.contains("record") || helpText.contains("Record")), "Should contain R for criminal record");
        assertTrue(helpText.contains("Tab") || helpText.contains("TAB"), "Should contain Tab for achievements");
    }
}
