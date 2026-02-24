package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.FacialExpression;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #384 — NPC animations (attack, wave, dance, point).
 *
 * Verifies that the four new animation states exist in NPCState, that NPCs can be
 * placed in each state, that facial expressions are correctly mapped for each new
 * state, and that animTime accumulates correctly while NPCs are in animated states.
 */
class Issue384NPCAnimationsTest {

    // -----------------------------------------------------------------------
    // State existence & transition
    // -----------------------------------------------------------------------

    @Test
    void attackingStateExists() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 1, 0);
        npc.setState(NPCState.ATTACKING);
        assertEquals(NPCState.ATTACKING, npc.getState(),
            "NPC should be able to enter ATTACKING state");
    }

    @Test
    void wavingStateExists() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.WAVING);
        assertEquals(NPCState.WAVING, npc.getState(),
            "NPC should be able to enter WAVING state");
    }

    @Test
    void dancingStateExists() {
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setState(NPCState.DANCING);
        assertEquals(NPCState.DANCING, npc.getState(),
            "NPC should be able to enter DANCING state");
    }

    @Test
    void pointingStateExists() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.POINTING);
        assertEquals(NPCState.POINTING, npc.getState(),
            "NPC should be able to enter POINTING state");
    }

    // -----------------------------------------------------------------------
    // Facial expression mapping for new animation states
    // -----------------------------------------------------------------------

    @Test
    void attackingStateShowsAngryExpression() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 1, 0);
        npc.setState(NPCState.ATTACKING);
        assertEquals(FacialExpression.ANGRY, npc.getFacialExpression(),
            "ATTACKING NPC should show an ANGRY expression");
    }

    @Test
    void wavingStateShowsHappyExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.WAVING);
        assertEquals(FacialExpression.HAPPY, npc.getFacialExpression(),
            "WAVING NPC should show a HAPPY expression");
    }

    @Test
    void dancingStateShowsHappyExpression() {
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setState(NPCState.DANCING);
        assertEquals(FacialExpression.HAPPY, npc.getFacialExpression(),
            "DANCING NPC should show a HAPPY expression");
    }

    @Test
    void pointingStateShowsSurprisedExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.POINTING);
        assertEquals(FacialExpression.SURPRISED, npc.getFacialExpression(),
            "POINTING NPC should show a SURPRISED expression");
    }

    // -----------------------------------------------------------------------
    // animTime accumulates in animated states (drives the animation cycle)
    // -----------------------------------------------------------------------

    @Test
    void animTimeAccumulatesInAttackingState() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 1, 0);
        npc.setState(NPCState.ATTACKING);
        npc.setVelocity(0f, 0f, 0f); // stationary — but animTime should still respond
        // Give the NPC some velocity so animTime advances (punch animation uses animTime)
        npc.setVelocity(1f, 0f, 0f);

        float before = npc.getAnimTime();
        npc.updateTimers(0.5f);
        float after = npc.getAnimTime();

        assertTrue(after > before,
            "animTime should advance while NPC is in ATTACKING state with velocity");
    }

    @Test
    void animTimeAccumulatesInDancingState() {
        NPC npc = new NPC(NPCType.BUSKER, 0, 1, 0);
        npc.setState(NPCState.DANCING);
        npc.setVelocity(1f, 0f, 0f);

        float before = npc.getAnimTime();
        npc.updateTimers(0.5f);
        float after = npc.getAnimTime();

        assertTrue(after > before,
            "animTime should advance while NPC is in DANCING state with velocity");
    }

    @Test
    void animTimeAccumulatesInWavingState() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.WAVING);
        npc.setVelocity(0f, 0f, 1f);

        float before = npc.getAnimTime();
        npc.updateTimers(0.5f);
        float after = npc.getAnimTime();

        assertTrue(after > before,
            "animTime should advance while NPC is in WAVING state with velocity");
    }

    @Test
    void animTimeAccumulatesInPointingState() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.POINTING);
        npc.setVelocity(0f, 0f, 1f);

        float before = npc.getAnimTime();
        npc.updateTimers(0.5f);
        float after = npc.getAnimTime();

        assertTrue(after > before,
            "animTime should advance while NPC is in POINTING state with velocity");
    }

    // -----------------------------------------------------------------------
    // State transitions from and to animation states
    // -----------------------------------------------------------------------

    @Test
    void npcCanTransitionFromIdleToAttackingAndBack() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 1, 0);

        assertEquals(NPCState.IDLE, npc.getState());

        npc.setState(NPCState.ATTACKING);
        assertEquals(NPCState.ATTACKING, npc.getState());

        npc.setState(NPCState.WANDERING);
        assertEquals(NPCState.WANDERING, npc.getState());
    }

    @Test
    void npcCanTransitionBetweenAnimationStates() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        npc.setState(NPCState.WAVING);
        assertEquals(NPCState.WAVING, npc.getState());

        npc.setState(NPCState.DANCING);
        assertEquals(NPCState.DANCING, npc.getState());

        npc.setState(NPCState.POINTING);
        assertEquals(NPCState.POINTING, npc.getState());

        npc.setState(NPCState.IDLE);
        assertEquals(NPCState.IDLE, npc.getState());
    }

    // -----------------------------------------------------------------------
    // All NPC types can enter every animation state
    // -----------------------------------------------------------------------

    @Test
    void allNPCTypesCanEnterAllAnimationStates() {
        NPCState[] animStates = {
            NPCState.ATTACKING, NPCState.WAVING, NPCState.DANCING, NPCState.POINTING
        };

        for (NPCType type : NPCType.values()) {
            for (NPCState animState : animStates) {
                NPC npc = new NPC(type, 0, 1, 0);
                npc.setState(animState);
                assertEquals(animState, npc.getState(),
                    type + " should be able to enter " + animState + " state");
                // Facial expression must never be null
                assertNotNull(npc.getFacialExpression(),
                    type + " in state " + animState + " must return a non-null FacialExpression");
            }
        }
    }
}
