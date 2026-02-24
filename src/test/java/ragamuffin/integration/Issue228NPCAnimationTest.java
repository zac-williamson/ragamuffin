package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #228 — NPC animation correctness.
 *
 * Verifies that animTime accumulates only while an NPC is moving,
 * that the facing angle tracks horizontal velocity, and that the
 * KNOCKED_OUT state stops walk animation but accumulates the
 * knocked-out timer.
 */
class Issue228NPCAnimationTest {

    /**
     * animTime advances while NPC has horizontal velocity, and stops
     * when the NPC is stationary.
     */
    @Test
    void animTimeOnlyAdvancesWhileMoving() {
        NPC npc = new NPC(NPCType.PUBLIC, 10, 1, 10);

        // Initially stationary
        assertEquals(0f, npc.getAnimTime(), 0.001f);

        // Stationary update — animTime must stay at 0
        npc.updateTimers(0.5f);
        assertEquals(0f, npc.getAnimTime(), 0.001f,
            "animTime must not advance while NPC is stationary");

        // Start moving
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers(0.5f);
        assertEquals(0.5f, npc.getAnimTime(), 0.001f,
            "animTime must advance by delta while NPC is moving");

        // Stop moving
        npc.setVelocity(0f, 0f, 0f);
        float animTimeWhenStopped = npc.getAnimTime();
        npc.updateTimers(0.5f);
        assertEquals(animTimeWhenStopped, npc.getAnimTime(), 0.001f,
            "animTime must not advance after NPC stops");
    }

    /**
     * The walk-cycle swing value is zero when animTime is 0 or speed is zero.
     * It becomes non-zero once the NPC starts moving and animTime advances.
     */
    @Test
    void walkCycleSwingIsZeroWhenStationary() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Stationary NPC: animTime = 0, speed = 0
        float animT = npc.getAnimTime();
        float speed = npc.getVelocity().len();
        float expectedSwing = (speed < 0.01f) ? 0f
            : (float) Math.sin(animT * 6.0f) * 45f;

        assertEquals(0f, expectedSwing, 0.001f,
            "Walk swing should be 0 for a stationary NPC");
    }

    /**
     * Once an NPC starts walking, the walk-cycle swing becomes non-zero
     * (unless sin evaluates exactly to 0, which only happens at t=0).
     */
    @Test
    void walkCycleSwingIsNonZeroAfterMovement() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setVelocity(2.0f, 0f, 0f);

        // Advance a quarter cycle (90 degrees) — sin(pi/2) = 1, max swing
        float quarterCycleDelta = (float) (Math.PI / 2.0 / 6.0);
        npc.updateTimers(quarterCycleDelta);

        float animT = npc.getAnimTime();
        float speed = npc.getVelocity().len();
        float swing = (speed < 0.01f) ? 0f : (float) Math.sin(animT * 6.0f) * 45f;

        assertTrue(Math.abs(swing) > 1f,
            "Walk swing should be significantly non-zero at quarter cycle: " + swing);
    }

    /**
     * Facing angle correctly reflects the direction of horizontal velocity:
     * moving in +Z gives 0°, moving in +X gives 90°.
     */
    @Test
    void facingAngleTracksVelocityDirection() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Moving in +Z: atan2(0, 2) = 0 degrees
        npc.setVelocity(0f, 0f, 2.0f);
        npc.updateTimers(0.1f);
        assertEquals(0f, npc.getFacingAngle(), 0.1f,
            "Facing angle should be 0° when moving in +Z direction");

        // Moving in +X: atan2(2, 0) = 90 degrees
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers(0.1f);
        assertEquals(90f, npc.getFacingAngle(), 0.1f,
            "Facing angle should be 90° when moving in +X direction");

        // Moving in -Z: atan2(0, -2) = 180 degrees
        npc.setVelocity(0f, 0f, -2.0f);
        npc.updateTimers(0.1f);
        assertEquals(180f, Math.abs(npc.getFacingAngle()), 0.1f,
            "Facing angle should be ±180° when moving in -Z direction");
    }

    /**
     * In KNOCKED_OUT state the NPC's walk animation stops (velocity is zero)
     * but the knocked-out timer accumulates.
     */
    @Test
    void knockedOutStateStopsWalkAnimation() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Accumulate some animTime first
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers(0.5f);
        float animTimeBeforeKO = npc.getAnimTime();
        assertTrue(animTimeBeforeKO > 0f);

        // Transition to KNOCKED_OUT and stop moving
        npc.setState(NPCState.KNOCKED_OUT);
        npc.setVelocity(0f, 0f, 0f);

        npc.updateTimers(1.0f);

        // animTime should NOT advance (no horizontal speed)
        assertEquals(animTimeBeforeKO, npc.getAnimTime(), 0.001f,
            "animTime must not advance while NPC is KNOCKED_OUT and stationary");

        // knockedOutTimer should accumulate
        assertEquals(1.0f, npc.getKnockedOutTimer(), 0.001f,
            "knockedOutTimer must accumulate while NPC is in KNOCKED_OUT state");
    }

    /**
     * animTime is a running total that never resets — the walk cycle is driven
     * by sin(animTime * WALK_SPEED) which loops naturally.
     */
    @Test
    void animTimeIsMonotonicallyIncreasing() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setVelocity(2.0f, 0f, 0f);

        float prev = 0f;
        for (int i = 0; i < 120; i++) {
            npc.updateTimers(1.0f / 60.0f);
            float curr = npc.getAnimTime();
            assertTrue(curr >= prev,
                "animTime must be monotonically non-decreasing; was " + prev + " then " + curr);
            prev = curr;
        }
    }
}
