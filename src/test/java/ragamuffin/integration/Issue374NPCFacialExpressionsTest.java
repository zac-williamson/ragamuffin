package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.FacialExpression;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #374 — NPC facial expressions.
 *
 * Verifies that each NPCState maps to the correct FacialExpression so the
 * renderer can display the appropriate face geometry.
 */
class Issue374NPCFacialExpressionsTest {

    @Test
    void idleAndWanderingNPCsHaveNeutralExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        npc.setState(NPCState.IDLE);
        assertEquals(FacialExpression.NEUTRAL, npc.getFacialExpression(),
            "IDLE NPCs should show a NEUTRAL expression");

        npc.setState(NPCState.WANDERING);
        assertEquals(FacialExpression.NEUTRAL, npc.getFacialExpression(),
            "WANDERING NPCs should show a NEUTRAL expression");
    }

    @Test
    void aggressiveAndArrestingNPCsShowAngryExpression() {
        NPC police = new NPC(NPCType.POLICE, 0, 1, 0);

        police.setState(NPCState.AGGRESSIVE);
        assertEquals(FacialExpression.ANGRY, police.getFacialExpression(),
            "AGGRESSIVE police should show an ANGRY expression");

        police.setState(NPCState.ARRESTING);
        assertEquals(FacialExpression.ANGRY, police.getFacialExpression(),
            "ARRESTING police should show an ANGRY expression");

        NPC builder = new NPC(NPCType.COUNCIL_BUILDER, 0, 1, 0);
        builder.setState(NPCState.DEMOLISHING);
        assertEquals(FacialExpression.ANGRY, builder.getFacialExpression(),
            "DEMOLISHING council builder should show an ANGRY expression");

        police.setState(NPCState.WARNING);
        assertEquals(FacialExpression.ANGRY, police.getFacialExpression(),
            "WARNING police should show an ANGRY expression");
    }

    @Test
    void fleeingNPCsShowScaredExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);
        npc.setState(NPCState.FLEEING);
        assertEquals(FacialExpression.SCARED, npc.getFacialExpression(),
            "FLEEING civilians should show a SCARED expression");
    }

    @Test
    void atPubAndAtHomeNPCsShowHappyExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        npc.setState(NPCState.AT_PUB);
        assertEquals(FacialExpression.HAPPY, npc.getFacialExpression(),
            "NPCs AT_PUB should show a HAPPY expression");

        npc.setState(NPCState.AT_HOME);
        assertEquals(FacialExpression.HAPPY, npc.getFacialExpression(),
            "NPCs AT_HOME should show a HAPPY expression");
    }

    @Test
    void staringAndPhotographingNPCsShowSurprisedExpression() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        npc.setState(NPCState.STARING);
        assertEquals(FacialExpression.SURPRISED, npc.getFacialExpression(),
            "STARING NPCs should show a SURPRISED expression");

        npc.setState(NPCState.PHOTOGRAPHING);
        assertEquals(FacialExpression.SURPRISED, npc.getFacialExpression(),
            "PHOTOGRAPHING NPCs should show a SURPRISED expression");
    }

    @Test
    void expressionChangesWhenStateChanges() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 1, 0);

        // Start neutral
        npc.setState(NPCState.WANDERING);
        assertEquals(FacialExpression.NEUTRAL, npc.getFacialExpression());

        // Scare them
        npc.setState(NPCState.FLEEING);
        assertEquals(FacialExpression.SCARED, npc.getFacialExpression());

        // Police approach — they return to neutral (complaining)
        npc.setState(NPCState.COMPLAINING);
        assertEquals(FacialExpression.NEUTRAL, npc.getFacialExpression());
    }

    @Test
    void allNPCTypesReturnValidExpression() {
        for (NPCType type : NPCType.values()) {
            if (type == NPCType.DOG) continue; // dogs use a different model
            NPC npc = new NPC(type, 0, 1, 0);
            for (NPCState state : NPCState.values()) {
                npc.setState(state);
                FacialExpression expr = npc.getFacialExpression();
                assertNotNull(expr,
                    type + " in state " + state + " must return a non-null FacialExpression");
            }
        }
    }
}
