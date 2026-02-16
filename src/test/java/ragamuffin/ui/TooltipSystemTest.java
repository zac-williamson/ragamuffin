package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TooltipSystemTest {

    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        tooltipSystem = new TooltipSystem();
    }

    @Test
    void testTriggerTooltip_FirstTime() {
        assertTrue(tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH));
        assertEquals("Punch a tree to get wood", tooltipSystem.getCurrentTooltip());
    }

    @Test
    void testTriggerTooltip_SecondTime_DoesNotTrigger() {
        tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        tooltipSystem.clearCurrent();

        assertFalse(tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH));
        assertNull(tooltipSystem.getCurrentTooltip());
    }

    @Test
    void testMultipleDifferentTooltips() {
        assertTrue(tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH));
        tooltipSystem.clearCurrent();

        assertTrue(tooltipSystem.trigger(TooltipTrigger.JEWELLER_DIAMOND));
        assertEquals("Jewellers can be a good source of diamond", tooltipSystem.getCurrentTooltip());
    }

    @Test
    void testClearCurrent() {
        tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        assertNotNull(tooltipSystem.getCurrentTooltip());

        tooltipSystem.clearCurrent();
        assertNull(tooltipSystem.getCurrentTooltip());
    }

    @Test
    void testHasShown() {
        assertFalse(tooltipSystem.hasShown(TooltipTrigger.FIRST_TREE_PUNCH));

        tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.FIRST_TREE_PUNCH));
    }

    @Test
    void testReset() {
        tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        tooltipSystem.trigger(TooltipTrigger.JEWELLER_DIAMOND);

        tooltipSystem.reset();

        assertFalse(tooltipSystem.hasShown(TooltipTrigger.FIRST_TREE_PUNCH));
        assertFalse(tooltipSystem.hasShown(TooltipTrigger.JEWELLER_DIAMOND));
    }

    @Test
    void testAllTooltipTriggersHaveMessages() {
        for (TooltipTrigger trigger : TooltipTrigger.values()) {
            tooltipSystem.reset();
            tooltipSystem.trigger(trigger);
            assertNotNull(tooltipSystem.getCurrentTooltip(),
                         "Trigger " + trigger + " should have a message");
            assertFalse(tooltipSystem.getCurrentTooltip().isEmpty(),
                       "Trigger " + trigger + " message should not be empty");
        }
    }

    @Test
    void testIsActive() {
        assertFalse(tooltipSystem.isActive());

        tooltipSystem.trigger(TooltipTrigger.FIRST_TREE_PUNCH);
        assertTrue(tooltipSystem.isActive());

        tooltipSystem.clearCurrent();
        assertFalse(tooltipSystem.isActive());
    }
}
