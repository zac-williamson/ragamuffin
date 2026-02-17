package ragamuffin.render;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;

import static org.junit.jupiter.api.Assertions.*;

class FirstPersonArmTest {

    private FirstPersonArm arm;

    @BeforeAll
    static void setupAll() {
        HeadlessTestHelper.initHeadless();
    }

    @BeforeEach
    void setUp() {
        arm = new FirstPersonArm();
    }

    @Test
    void armStartsAtRest() {
        assertFalse(arm.isSwinging());
        assertEquals(0f, arm.getSwingProgress(), 0.01f);
    }

    @Test
    void punchTriggersSwing() {
        arm.punch();
        assertTrue(arm.isSwinging());
    }

    @Test
    void swingProgressIncreasesWhenPunching() {
        arm.punch();
        // Advance a little into the swing
        arm.update(0.05f);
        assertTrue(arm.getSwingProgress() > 0f);
        assertTrue(arm.isSwinging());
    }

    @Test
    void swingCompletesAndReturnsToRest() {
        arm.punch();
        // Advance past the full swing duration
        arm.update(0.3f);
        assertFalse(arm.isSwinging());
        assertEquals(0f, arm.getSwingProgress(), 0.01f);
    }

    @Test
    void swingProgressPeaksAtMidpoint() {
        arm.punch();
        // At exactly halfway through the swing, progress should be 1.0
        arm.update(0.125f); // Half of 0.25s duration
        assertEquals(1.0f, arm.getSwingProgress(), 0.05f);
    }

    @Test
    void multiplePunchesResetSwing() {
        arm.punch();
        arm.update(0.1f);
        assertTrue(arm.isSwinging());

        // Punch again mid-swing
        arm.punch();
        assertTrue(arm.isSwinging());
        // Timer should be reset to full duration
        arm.update(0.24f);
        assertTrue(arm.isSwinging()); // Should still be swinging
    }
}
