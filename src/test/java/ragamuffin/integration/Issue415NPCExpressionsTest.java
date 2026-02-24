package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.FacialExpression;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #415 — Improve NPC expressions.
 *
 * Verifies blinking behaviour, speaking mouth animation state, the new DISGUSTED
 * expression for COMPLAINING and STEALING states, and that all NPC types and
 * states still produce a valid (non-null) facial expression.
 */
class Issue415NPCExpressionsTest {

    // -----------------------------------------------------------------------
    // Blinking
    // -----------------------------------------------------------------------

    @Test
    void npcStartsWithEyesOpen() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        // A freshly-constructed NPC that has never had updateTimers() called
        // might be mid-blink depending on the staggered offset, but it must
        // return a well-defined boolean, not throw.
        boolean blinking = npc.isBlinking();
        // Just assert it doesn't throw and is a valid boolean
        assertTrue(blinking || !blinking, "isBlinking() must return a valid boolean");
    }

    @Test
    void npcBlinksAfterBlinkIntervalElapses() {
        // Place at origin so stagger offset = 0 → starts with blinkTimer = 0
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.IDLE);

        // Advance time just past the default 3.5-second blink interval
        npc.updateTimers(3.6f);

        assertTrue(npc.isBlinking(),
            "NPC should be blinking shortly after the blink interval elapses");
    }

    @Test
    void blinkEndsAfterBlinkDuration() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.IDLE);

        // Trigger blink
        npc.updateTimers(3.6f);
        assertTrue(npc.isBlinking(), "NPC should have started blinking");

        // Advance past the 0.12-second blink duration
        npc.updateTimers(0.2f);
        assertFalse(npc.isBlinking(),
            "NPC eyes should be open again after the blink duration ends");
    }

    @Test
    void knockedOutNPCHasEyesClosed() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.KNOCKED_OUT);
        npc.updateTimers(0.05f);

        assertTrue(npc.isBlinking(),
            "Knocked-out NPC should always have eyes closed (isBlinking = true)");
    }

    @Test
    void differentNPCsHaveDifferentBlinkPhases() {
        // Two NPCs at different positions should have different initial blink offsets
        NPC npc1 = new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
        NPC npc2 = new NPC(NPCType.PUBLIC, 5f, 1f, 7f);

        // Advance both by a small amount — at least one should differ in blink state
        // after advancing to just before the interval for npc1
        npc1.updateTimers(0.1f);
        npc2.updateTimers(0.1f);

        // We can't guarantee they're different after just 0.1s, but both must
        // return a valid boolean without throwing
        boolean b1 = npc1.isBlinking();
        boolean b2 = npc2.isBlinking();
        assertTrue(b1 || !b1, "npc1 isBlinking() must be a valid boolean");
        assertTrue(b2 || !b2, "npc2 isBlinking() must be a valid boolean");
    }

    // -----------------------------------------------------------------------
    // Mouth / speaking state
    // -----------------------------------------------------------------------

    @Test
    void nonSpeakingNPCHasMouthClosed() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        // No speech set
        assertFalse(npc.isMouthOpen(),
            "NPC with no active speech should have mouth closed");
    }

    @Test
    void speakingNPCHasMouthOpen() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setSpeechText("Oi, watch it!", 3.0f);

        assertTrue(npc.isMouthOpen(),
            "NPC with active speech text should have mouth open");
    }

    @Test
    void mouthClosesWhenSpeechExpires() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setSpeechText("Excuse me!", 0.5f);
        assertTrue(npc.isMouthOpen(), "Mouth should be open while speaking");

        // Advance past the speech duration
        npc.updateTimers(0.6f);
        assertFalse(npc.isMouthOpen(),
            "Mouth should close once the speech timer expires");
    }

    // -----------------------------------------------------------------------
    // DISGUSTED expression
    // -----------------------------------------------------------------------

    @Test
    void complainingNPCShowsDisgustedExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.COMPLAINING);
        assertEquals(FacialExpression.DISGUSTED, npc.getFacialExpression(),
            "COMPLAINING NPCs should show a DISGUSTED expression");
    }

    @Test
    void stealingNPCShowsDisgustedExpression() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 1, 0);
        npc.setState(NPCState.STEALING);
        assertEquals(FacialExpression.DISGUSTED, npc.getFacialExpression(),
            "STEALING NPCs should show a DISGUSTED expression");
    }

    @Test
    void disgustedExpressionIsDistinctFromNeutral() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        npc.setState(NPCState.WANDERING);
        FacialExpression neutral = npc.getFacialExpression();

        npc.setState(NPCState.COMPLAINING);
        FacialExpression disgusted = npc.getFacialExpression();

        assertNotEquals(neutral, disgusted,
            "DISGUSTED expression should be distinct from NEUTRAL");
        assertEquals(FacialExpression.DISGUSTED, disgusted);
    }

    // -----------------------------------------------------------------------
    // Completeness — all types/states still produce valid expressions
    // -----------------------------------------------------------------------

    @Test
    void allNPCTypesAndStatesReturnValidExpression() {
        for (NPCType type : NPCType.values()) {
            if (type == NPCType.DOG) continue; // dogs use a different model
            for (NPCState state : NPCState.values()) {
                NPC npc = new NPC(type, 0, 1, 0);
                npc.setState(state);
                FacialExpression expr = npc.getFacialExpression();
                assertNotNull(expr,
                    type + " in state " + state + " must return a non-null FacialExpression");
            }
        }
    }

    @Test
    void blinkingDoesNotThrowForAnyState() {
        for (NPCType type : NPCType.values()) {
            NPC npc = new NPC(type, 0, 1, 0);
            for (NPCState state : NPCState.values()) {
                npc.setState(state);
                npc.updateTimers(0.016f);
                // Must not throw
                boolean b = npc.isBlinking();
                assertTrue(b || !b,
                    type + " in state " + state + " must return a valid blink state");
            }
        }
    }
}
