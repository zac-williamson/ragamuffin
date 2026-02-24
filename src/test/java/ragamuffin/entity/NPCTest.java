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
        assertEquals(3, npc.getCurrentPathIndex()); // Allowed to go past end; followPath checks bounds
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

    @Test
    void testAnimTimeAdvancesWhenMoving() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        // Set a horizontal velocity so the NPC is "moving"
        npc.setVelocity(2.0f, 0f, 0f);

        float initialAnimTime = npc.getAnimTime();
        assertEquals(0f, initialAnimTime, 0.001f);

        npc.updateTimers(0.5f);
        assertTrue(npc.getAnimTime() > 0f, "animTime should advance when NPC is moving");
        assertEquals(0.5f, npc.getAnimTime(), 0.001f);

        npc.updateTimers(0.5f);
        assertEquals(1.0f, npc.getAnimTime(), 0.001f);
    }

    @Test
    void testAnimTimeDoesNotAdvanceWhenStopped() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        // NPC is stationary (velocity = 0)
        npc.setVelocity(0f, 0f, 0f);
        npc.updateTimers(1.0f);

        assertEquals(0f, npc.getAnimTime(), 0.001f,
            "animTime should NOT advance when NPC is stationary");
    }

    @Test
    void testFacingAngleUpdatesFromVelocity() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        // Move in +X direction: facing angle should be 90 degrees
        npc.setVelocity(2.0f, 0f, 0f);
        npc.updateTimers(0.1f);
        assertEquals(90f, npc.getFacingAngle(), 0.1f,
            "Facing angle should be ~90 degrees when moving in +X direction");

        // Move in +Z direction: facing angle should be 0 degrees
        npc.setVelocity(0f, 0f, 2.0f);
        npc.updateTimers(0.1f);
        assertEquals(0f, npc.getFacingAngle(), 0.1f,
            "Facing angle should be ~0 degrees when moving in +Z direction");
    }

    @Test
    void testAnimTimeAccumulatesCorrectlyOverMultipleUpdates() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);
        npc.setVelocity(1.0f, 0f, 1.0f); // Moving diagonally

        // Simulate 60 frames at 60fps
        float delta = 1.0f / 60.0f;
        for (int i = 0; i < 60; i++) {
            npc.updateTimers(delta);
        }

        assertEquals(1.0f, npc.getAnimTime(), 0.01f,
            "animTime should accumulate to ~1 second after 60 frames at 60fps");
    }

    @Test
    void testKnockedOutStateTransition() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        // Initially alive and not knocked out
        assertTrue(npc.isAlive());
        assertNotEquals(NPCState.KNOCKED_OUT, npc.getState());
        assertEquals(0f, npc.getKnockedOutTimer(), 0.001f);

        // Transition to KNOCKED_OUT
        npc.setState(NPCState.KNOCKED_OUT);
        assertEquals(NPCState.KNOCKED_OUT, npc.getState());

        // Timer accumulates while in KNOCKED_OUT state (updateTimers + tickKnockedOutTimer,
        // mirroring what NPCManager.update() does for dead/knocked-out NPCs).
        npc.updateTimers(0.5f);
        npc.tickKnockedOutTimer(0.5f);
        assertEquals(0.5f, npc.getKnockedOutTimer(), 0.001f);

        npc.updateTimers(0.5f);
        npc.tickKnockedOutTimer(0.5f);
        assertEquals(1.0f, npc.getKnockedOutTimer(), 0.001f);
    }

    @Test
    void testKnockedOutTimerDoesNotAccumulateInOtherStates() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        // In IDLE state, timer should not accumulate
        npc.updateTimers(1.0f);
        assertEquals(0f, npc.getKnockedOutTimer(), 0.001f);

        // Switch to WANDERING, still no accumulation
        npc.setState(NPCState.WANDERING);
        npc.updateTimers(1.0f);
        assertEquals(0f, npc.getKnockedOutTimer(), 0.001f);
    }

    /**
     * Fix #423: tickSpeechOnly() must advance the speech timer without touching
     * attackCooldown, blinkTimer, or animTime.
     */
    @Test
    void testTickSpeechOnlyDoesNotAdvanceOtherTimers() {
        NPC npc = new NPC(NPCType.YOUTH_GANG, 0, 0, 0);

        // Give the NPC a speech bubble and a non-zero attack cooldown
        npc.setSpeechText("Oi!", 5.0f);
        npc.resetAttackCooldown(); // sets attackCooldown to type's value (> 0)
        float initialCooldown = npc.getAttackCooldown();
        assertTrue(initialCooldown > 0f, "attackCooldown must be > 0 after resetAttackCooldown()");

        // Set velocity so that a full updateTimers() would advance animTime
        npc.setVelocity(1.0f, 0f, 0f);

        float initialAnimTime = npc.getAnimTime();
        float initialBlinkTimer = npc.getBlinkTimer();

        // Simulate 5 seconds via tickSpeechOnly() (the PAUSED-branch-safe path)
        npc.tickSpeechOnly(5.0f);

        // Speech should now be expired
        assertFalse(npc.isSpeaking(),
                "Fix #423: speech must expire after tickSpeechOnly() advances past its duration");

        // attackCooldown must NOT have changed
        assertEquals(initialCooldown, npc.getAttackCooldown(), 0.001f,
                "Fix #423: attackCooldown must not drain during tickSpeechOnly()");

        // animTime must NOT have changed
        assertEquals(initialAnimTime, npc.getAnimTime(), 0.001f,
                "Fix #423: animTime must not advance during tickSpeechOnly()");

        // blinkTimer must NOT have changed
        assertEquals(initialBlinkTimer, npc.getBlinkTimer(), 0.001f,
                "Fix #423: blinkTimer must not advance during tickSpeechOnly()");
    }

    @Test
    void testNPCDefeatedByDamage() {
        NPC npc = new NPC(NPCType.PUBLIC, 0, 0, 0);

        // PUBLIC has 20 HP — 2 punches of 10 damage each should kill
        assertFalse(npc.takeDamage(10f));
        assertTrue(npc.isAlive());
        boolean killed = npc.takeDamage(10f);

        assertTrue(killed, "NPC should be killed by second hit");
        assertFalse(npc.isAlive());
        assertEquals(0f, npc.getHealth(), 0.001f);
    }
}
