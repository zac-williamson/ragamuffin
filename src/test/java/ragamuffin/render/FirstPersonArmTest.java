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

    /**
     * Fix #339: Arm swing animation must advance when update() is called regardless
     * of game state. The PAUSED render path now calls firstPersonArm.update(delta)
     * so a mid-punch swing completes rather than freezing in the extended position.
     */
    @Test
    void swingAdvancesWhenUpdateCalledWhilePaused() {
        arm.punch();
        arm.update(0.05f); // Advance partway into the swing
        float progressMidSwing = arm.getSwingProgress();
        assertTrue(progressMidSwing > 0f, "Arm should be mid-swing after update");
        assertTrue(arm.isSwinging(), "Arm should still be swinging");

        // Simulate the PAUSED path calling update() — animation must continue to advance
        arm.update(0.22f); // Advance past the remaining duration
        assertFalse(arm.isSwinging(), "Arm should have completed swing after enough update time");
        assertEquals(0f, arm.getSwingProgress(), 0.01f, "Arm should return to rest after swing completes");
    }

    /**
     * Fix #445: The CINEMATIC render path must call firstPersonArm.update(delta)
     * so that the idleTimer advances during the ~8-second fly-through. Without
     * this call the idleTimer freezes at 0, causing the idle bob animation to
     * start from the wrong phase on the very first PLAYING frame.
     *
     * <p>Simulates 300 frames at 60 fps (5 seconds) to verify:
     * <ul>
     *   <li>swingProgress is 0 (arm is not frozen mid-swing)</li>
     *   <li>idleTimer has advanced by approximately 5 seconds</li>
     * </ul>
     */
    @Test
    void idleTimerAdvancesDuringCinematic() {
        // Simulate 300 frames at 60 fps (5 seconds) as the CINEMATIC path would do.
        float delta = 1f / 60f;
        for (int frame = 0; frame < 300; frame++) {
            arm.update(delta);
        }

        float expectedElapsed = 300 * delta; // ~5.0 seconds
        assertEquals(0f, arm.getSwingProgress(), 0.01f,
                "Arm should not be mid-swing after cinematic frames (no punch was triggered)");
        assertEquals(expectedElapsed, arm.getIdleTimer(), 0.1f,
                "idleTimer should have advanced by ~5 seconds during the simulated cinematic");
    }
}
