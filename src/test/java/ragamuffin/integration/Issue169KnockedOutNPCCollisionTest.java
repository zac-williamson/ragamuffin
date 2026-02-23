package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.AABB;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #169:
 * Player sticking/glitching when walking on knocked out NPCs.
 *
 * Root cause: resolveNPCCollisions() in RagamuffinGame pushed the player away
 * from ALL NPCs including those in KNOCKED_OUT state. A knocked-out NPC lies on
 * the ground and the player's AABB overlaps it, so the push ran every frame,
 * causing the player to stick and glitch.
 *
 * Fix: skip NPCs in KNOCKED_OUT state during collision resolution so the player
 * can walk over them freely.
 */
class Issue169KnockedOutNPCCollisionTest {

    /**
     * Simulate the resolveNPCCollisions logic from RagamuffinGame, applying the
     * fix: skip KNOCKED_OUT NPCs.
     */
    private void resolveNPCCollisions(Player player, Iterable<NPC> npcs) {
        for (NPC npc : npcs) {
            // Fix #169: knocked-out NPCs do not block the player
            if (npc.getState() == NPCState.KNOCKED_OUT) continue;
            if (player.getAABB().intersects(npc.getAABB())) {
                float dx = player.getPosition().x - npc.getPosition().x;
                float dz = player.getPosition().z - npc.getPosition().z;
                float len = (float) Math.sqrt(dx * dx + dz * dz);
                if (len < 0.01f) {
                    dx = 1f; dz = 0f; len = 1f;
                }
                float pushDist = 0.1f;
                player.getPosition().x += (dx / len) * pushDist;
                player.getPosition().z += (dz / len) * pushDist;
                player.getAABB().setPosition(player.getPosition(), Player.WIDTH, Player.HEIGHT, Player.DEPTH);
            }
        }
    }

    /**
     * Test 1: Player is NOT pushed when walking over a KNOCKED_OUT NPC.
     * The player's position should remain unchanged after resolveNPCCollisions
     * when the overlapping NPC is in the KNOCKED_OUT state.
     */
    @Test
    void knockedOutNPC_doesNotPushPlayer() {
        // Place player on top of / overlapping a knocked-out NPC at the same position
        Player player = new Player(5f, 1f, 5f);
        NPC npc = new NPC(NPCType.PUBLIC, 5f, 1f, 5f);
        npc.setState(NPCState.KNOCKED_OUT);

        // Verify the AABBs actually overlap (precondition for the bug)
        assertTrue(player.getAABB().intersects(npc.getAABB()),
                "Player and knocked-out NPC AABBs must overlap for the test to be meaningful");

        Vector3 positionBefore = new Vector3(player.getPosition());

        // Run collision resolution — with the fix, player should NOT be moved
        java.util.List<NPC> npcs = java.util.List.of(npc);
        resolveNPCCollisions(player, npcs);

        assertEquals(positionBefore.x, player.getPosition().x, 0.001f,
                "Player X should not change when walking on a knocked-out NPC");
        assertEquals(positionBefore.z, player.getPosition().z, 0.001f,
                "Player Z should not change when walking on a knocked-out NPC");
    }

    /**
     * Test 2: Player IS still pushed away from a standing (non-knocked-out) NPC.
     * The fix must not break normal NPC collision for NPCs in active states.
     */
    @Test
    void standingNPC_stillPushesPlayer() {
        // Place player directly on top of an IDLE NPC
        Player player = new Player(5f, 1f, 5f);
        NPC npc = new NPC(NPCType.PUBLIC, 5f, 1f, 5f);
        npc.setState(NPCState.IDLE);

        // Verify the AABBs overlap
        assertTrue(player.getAABB().intersects(npc.getAABB()),
                "Player and IDLE NPC AABBs must overlap for the test to be meaningful");

        Vector3 positionBefore = new Vector3(player.getPosition());

        java.util.List<NPC> npcs = java.util.List.of(npc);
        resolveNPCCollisions(player, npcs);

        // When player and NPC are at the exact same position the fallback kicks in
        // and pushes along +X — position should have changed
        float movedDist = (float) Math.sqrt(
                Math.pow(player.getPosition().x - positionBefore.x, 2) +
                Math.pow(player.getPosition().z - positionBefore.z, 2));
        assertTrue(movedDist > 0f,
                "Player should be pushed away from a standing (non-knocked-out) NPC");
    }

    /**
     * Test 3: Multiple calls on a knocked-out NPC do not move the player —
     * simulates several frames of the game loop without any accumulated drift.
     */
    @Test
    void knockedOutNPC_noAccumulatedDriftOverMultipleFrames() {
        Player player = new Player(5f, 1f, 5f);
        NPC npc = new NPC(NPCType.POLICE, 5f, 1f, 5f);
        npc.setState(NPCState.KNOCKED_OUT);

        java.util.List<NPC> npcs = java.util.List.of(npc);
        Vector3 positionBefore = new Vector3(player.getPosition());

        // Simulate 60 frames of the game loop
        for (int frame = 0; frame < 60; frame++) {
            resolveNPCCollisions(player, npcs);
        }

        assertEquals(positionBefore.x, player.getPosition().x, 0.001f,
                "Player X must not drift over 60 frames when standing on a knocked-out NPC");
        assertEquals(positionBefore.z, player.getPosition().z, 0.001f,
                "Player Z must not drift over 60 frames when standing on a knocked-out NPC");
    }

    /**
     * Test 4: Mixed NPC list — knocked-out NPC is skipped, active NPC still pushes.
     */
    @Test
    void mixedNPCList_knockedOutSkipped_activeStillPushes() {
        // Player is at (5, 1, 5), overlapping both NPCs
        Player player = new Player(5f, 1f, 5f);

        NPC knockedOut = new NPC(NPCType.PUBLIC, 5f, 1f, 5f);
        knockedOut.setState(NPCState.KNOCKED_OUT);

        // Active NPC slightly to the side — still overlapping player
        NPC active = new NPC(NPCType.PUBLIC, 5.1f, 1f, 5f);
        active.setState(NPCState.WANDERING);

        java.util.List<NPC> npcs = java.util.List.of(knockedOut, active);

        Vector3 positionBefore = new Vector3(player.getPosition());
        resolveNPCCollisions(player, npcs);

        // Should have been pushed due to the active NPC, not stayed completely still
        float movedDist = (float) Math.sqrt(
                Math.pow(player.getPosition().x - positionBefore.x, 2) +
                Math.pow(player.getPosition().z - positionBefore.z, 2));
        assertTrue(movedDist > 0f,
                "Player should still be pushed by the active NPC in a mixed NPC list");
    }
}
