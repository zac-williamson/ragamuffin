package ragamuffin.entity;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NPC entity.
 */
class NPCTest {

    @Test
    void testNPCCreation() {
        NPC npc = new NPC(NPCType.PUBLIC, 10, 1, 10);

        assertEquals(NPCType.PUBLIC, npc.getType());
        assertEquals(10, npc.getPosition().x, 0.01f);
        assertEquals(1, npc.getPosition().y, 0.01f);
        assertEquals(10, npc.getPosition().z, 0.01f);
        assertEquals(NPCState.IDLE, npc.getState());
    }

    @Test
    void testStateChange() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        assertEquals(NPCState.IDLE, npc.getState());

        npc.setState(NPCState.WANDERING);
        assertEquals(NPCState.WANDERING, npc.getState());

        npc.setState(NPCState.STARING);
        assertEquals(NPCState.STARING, npc.getState());
    }

    @Test
    void testMovement() {
        NPC npc = new NPC(NPCType.PUBLIC, 10, 1, 10);
        Vector3 originalPos = new Vector3(npc.getPosition());

        npc.move(1, 0, 0, 1.0f); // Move east for 1 second

        assertTrue(npc.getPosition().x > originalPos.x);
        assertEquals(originalPos.y, npc.getPosition().y, 0.01f);
        assertEquals(originalPos.z, npc.getPosition().z, 0.01f);
    }

    @Test
    void testKnockback() {
        NPC npc = new NPC(NPCType.COUNCIL_MEMBER, 20, 1, 20);
        Vector3 originalPos = new Vector3(npc.getPosition());

        Vector3 knockbackDir = new Vector3(0, 0, -1); // North
        npc.applyKnockback(knockbackDir, 2.0f);

        // Knockback now sets velocity impulse rather than teleporting.
        // Simulate a few frames so the velocity moves the NPC.
        for (int i = 0; i < 12; i++) {
            npc.update(1.0f / 60.0f);
        }

        assertEquals(originalPos.x, npc.getPosition().x, 0.5f);
        assertTrue(npc.getPosition().z < originalPos.z, "NPC should be knocked north");
    }

    @Test
    void testIsNear() {
        NPC npc = new NPC(NPCType.PUBLIC, 10, 1, 10);

        assertTrue(npc.isNear(new Vector3(10, 1, 10), 1.0f));
        assertTrue(npc.isNear(new Vector3(10.5f, 1, 10), 1.0f));
        assertFalse(npc.isNear(new Vector3(15, 1, 10), 3.0f));
        assertTrue(npc.isNear(new Vector3(12, 1, 10), 3.0f));
    }

    @Test
    void testIsWithinBounds() {
        NPC npc = new NPC(NPCType.DOG, 5, 1, 5);

        assertTrue(npc.isWithinBounds(0, 0, 10, 10));
        assertTrue(npc.isWithinBounds(-10, -10, 10, 10));
        assertFalse(npc.isWithinBounds(10, 10, 20, 20));
    }

    @Test
    void testSpeech() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        assertFalse(npc.isSpeaking());
        assertNull(npc.getSpeechText());

        npc.setSpeechText("Blimey!", 5.0f);
        assertTrue(npc.isSpeaking());
        assertEquals("Blimey!", npc.getSpeechText());

        // Simulate time passing
        npc.update(6.0f);
        assertFalse(npc.isSpeaking());
    }

    @Test
    void testPathFollowing() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        assertNull(npc.getPath());
        assertEquals(0, npc.getCurrentPathIndex());

        Vector3[] waypoints = {
            new Vector3(1, 0, 0),
            new Vector3(2, 0, 0),
            new Vector3(3, 0, 0)
        };
        npc.setPath(Arrays.asList(waypoints));

        assertEquals(3, npc.getPath().size());
        assertEquals(0, npc.getCurrentPathIndex());

        npc.advancePathIndex();
        assertEquals(1, npc.getCurrentPathIndex());

        npc.advancePathIndex();
        assertEquals(2, npc.getCurrentPathIndex());

        npc.advancePathIndex();
        assertEquals(2, npc.getCurrentPathIndex()); // Shouldn't exceed bounds
    }

    @Test
    void testTargetPosition() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        assertNull(npc.getTargetPosition());

        Vector3 target = new Vector3(10, 1, 10);
        npc.setTargetPosition(target);

        assertNotNull(npc.getTargetPosition());
        assertEquals(10, npc.getTargetPosition().x, 0.01f);
        assertEquals(1, npc.getTargetPosition().y, 0.01f);
        assertEquals(10, npc.getTargetPosition().z, 0.01f);
    }
}
