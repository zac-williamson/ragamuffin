package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #416 — NPC animations.
 *
 * Verifies the new animation features added in this issue:
 *   1. Dog tail-wag animation: idleTime accumulates for stationary dogs, enabling slow idle tail wag.
 *   2. Humanoid head bob: walk cycle bob only fires while moving.
 *   3. Idle breathing: idleTime accumulates when stationary and resets when moving.
 */
class Issue416NPCAnimationsTest {

    // -----------------------------------------------------------------------
    // idleTime accumulation
    // -----------------------------------------------------------------------

    @Test
    void idleTimeAccumulatesWhenStationary() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        // NPC starts stationary
        assertEquals(0f, npc.getIdleTime(), 0.001f,
            "idleTime should start at zero");

        npc.updateTimers(1.0f);
        assertEquals(1.0f, npc.getIdleTime(), 0.001f,
            "idleTime should accumulate while NPC is stationary");

        npc.updateTimers(0.5f);
        assertEquals(1.5f, npc.getIdleTime(), 0.001f,
            "idleTime should continue accumulating");
    }

    @Test
    void idleTimeResetsWhenMoving() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        // Accumulate idle time first
        npc.updateTimers(2.0f);
        assertEquals(2.0f, npc.getIdleTime(), 0.001f,
            "idleTime should be 2.0 after 2 stationary seconds");

        // Start moving
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers(0.1f);
        assertEquals(0f, npc.getIdleTime(), 0.001f,
            "idleTime should reset to zero once NPC starts moving");
    }

    @Test
    void idleTimeDoesNotAccumulateWhileMoving() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setVelocity(1.5f, 0f, 0f);

        npc.updateTimers(1.0f);
        assertEquals(0f, npc.getIdleTime(), 0.001f,
            "idleTime must not accumulate while NPC is moving");
    }

    @Test
    void idleTimeReaccumulatesAfterStoppingAgain() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Move, then stop
        npc.setVelocity(1.0f, 0f, 0f);
        npc.updateTimers(0.5f);
        assertEquals(0f, npc.getIdleTime(), 0.001f);

        npc.setVelocity(0f, 0f, 0f);
        npc.updateTimers(0.8f);
        assertEquals(0.8f, npc.getIdleTime(), 0.001f,
            "idleTime should re-accumulate after NPC stops again");
    }

    @Test
    void idleTimeAccumulatesForDogWhenStationary() {
        NPC dog = new NPC(NPCType.DOG, 0, 1, 0);
        // Dog is stationary — idleTime should accumulate for slow tail wag
        npc_update(dog, 3.0f);
        assertTrue(dog.getIdleTime() >= 3.0f,
            "Dog idleTime should accumulate when stationary (drives slow idle tail wag)");
    }

    // -----------------------------------------------------------------------
    // animTime and idleTime mutual exclusivity
    // -----------------------------------------------------------------------

    @Test
    void animTimeAndIdleTimeAreMutuallyExclusive() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Phase 1: stationary — idleTime grows, animTime stays 0
        npc.updateTimers(1.0f);
        assertEquals(1.0f, npc.getIdleTime(), 0.001f, "idleTime should grow when stationary");
        assertEquals(0f, npc.getAnimTime(), 0.001f, "animTime must not grow when stationary");

        // Phase 2: moving — animTime grows, idleTime resets
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers(0.5f);
        assertEquals(0.5f, npc.getAnimTime(), 0.001f, "animTime should grow when moving");
        assertEquals(0f, npc.getIdleTime(), 0.001f, "idleTime should be 0 while moving");
    }

    // -----------------------------------------------------------------------
    // Dog tail wag: no exceptions thrown for any dog state
    // -----------------------------------------------------------------------

    @Test
    void dogAnimationTimersWorkInAllStates() {
        for (NPCState state : NPCState.values()) {
            NPC dog = new NPC(NPCType.DOG, 0, 1, 0);
            dog.setState(state);
            // Must not throw for any state
            npc_update(dog, 0.5f);
            boolean idle = dog.getIdleTime() >= 0f;
            boolean anim = dog.getAnimTime() >= 0f;
            assertTrue(idle && anim,
                "Dog in state " + state + " must have non-negative timer values");
        }
    }

    // -----------------------------------------------------------------------
    // Head bob: only fires while walking (animTime advancing)
    // -----------------------------------------------------------------------

    @Test
    void headBobOnlyWhenMoving() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Stationary: animTime = 0, speed = 0 → headBob should be 0
        npc.updateTimers(0.5f);
        float speed = npc.getVelocity().len();
        float animT = npc.getAnimTime();
        float headBob = (speed < 0.01f) ? 0f
            : 0.025f * Math.abs((float) Math.sin(animT * 6.0f));
        assertEquals(0f, headBob, 0.001f,
            "Head bob should be zero when NPC is stationary");

        // Moving: animTime > 0, speed > 0 → headBob may be non-zero
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers((float) (Math.PI / 2.0 / 6.0)); // advance to quarter cycle
        speed = npc.getVelocity().len();
        animT = npc.getAnimTime();
        headBob = (speed < 0.01f) ? 0f
            : 0.025f * Math.abs((float) Math.sin(animT * 6.0f));
        assertTrue(headBob >= 0f,
            "Head bob must be non-negative: " + headBob);
        // At quarter cycle, |sin(pi/2)| = 1.0, so bob = 0.025
        assertEquals(0.025f, headBob, 0.002f,
            "Head bob should be ~0.025 at peak of quarter-cycle walk animation");
    }

    // -----------------------------------------------------------------------
    // All NPC types have idleTime getter
    // -----------------------------------------------------------------------

    @Test
    void allNPCTypesHaveIdleTimeGetter() {
        for (NPCType type : NPCType.values()) {
            NPC npc = new NPC(type, 0, 1, 0);
            float idle = npc.getIdleTime();
            assertTrue(idle >= 0f,
                type + " must have a non-negative idleTime on creation: " + idle);
        }
    }

    // Helper to call updateTimers in small steps (simulates game loop)
    private static void npc_update(NPC npc, float totalTime) {
        float step = 1.0f / 60.0f;
        float elapsed = 0f;
        while (elapsed < totalTime) {
            float dt = Math.min(step, totalTime - elapsed);
            npc.updateTimers(dt);
            elapsed += dt;
        }
    }
}
