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
    }
}
