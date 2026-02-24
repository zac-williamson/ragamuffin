package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.core.InteractionSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.world.LandmarkType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #513: Interaction with quest NPCs fails when
 * player looks up or down.
 *
 * The facing check in findNPCInRange() must use the horizontal projection of
 * lookDirection so that vertical camera pitch does not prevent interaction
 * with an NPC the player is horizontally facing.
 */
class Issue513NpcInteractionPitchTest {

    /**
     * Test 1: Interaction works when player looks straight ahead.
     *
     * Player at (0, 0, 0), NPC at (0, 0, -1.5f) (directly in front on Z axis).
     * lookDirection = (0, 0, -1) — horizontal, no pitch.
     * Verify findNPCInRange returns the NPC.
     */
    @Test
    void test1_InteractionWorksLookingStraightAhead() {
        InteractionSystem interactionSystem = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC npc = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1.5f);
        List<NPC> npcs = npcManager.getNPCs();

        Vector3 playerPos = new Vector3(0, 0, 0);
        Vector3 lookDir = new Vector3(0, 0, -1).nor();

        NPC found = interactionSystem.findNPCInRange(playerPos, lookDir, npcs);
        assertNotNull(found, "Should find NPC when looking directly at it horizontally");
    }

    /**
     * Test 2: Interaction works when player looks upward (positive Y pitch).
     *
     * Player at (0, 0, 0), NPC at (0, 0, -1.5f).
     * lookDirection = (0, 0.5, -1).nor() — looking up at ~27 degrees.
     * Without the fix the dot product drops below 0.5 and interaction fails.
     * With the fix, horizontal projection is (0, 0, -1) and dot = 1.0 → succeeds.
     */
    @Test
    void test2_InteractionWorksWhenLookingUp() {
        InteractionSystem interactionSystem = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC npc = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1.5f);
        List<NPC> npcs = npcManager.getNPCs();

        Vector3 playerPos = new Vector3(0, 0, 0);
        // Pitched upward — simulates player looking at NPC's face/head
        Vector3 lookDir = new Vector3(0, 0.5f, -1f).nor();

        NPC found = interactionSystem.findNPCInRange(playerPos, lookDir, npcs);
        assertNotNull(found, "Should find NPC even when camera is pitched upward");
    }

    /**
     * Test 3: Interaction works when player looks downward (negative Y pitch).
     *
     * Player at (0, 1.62f, 0) (eye height), NPC at (0, 0.9f, -1.5f) (NPC centre).
     * lookDirection = (0, -0.3f, -1f).nor() — looking slightly down.
     * This is the typical in-game scenario: camera at ~y+1.62, NPC centre at ~y+0.9.
     */
    @Test
    void test3_InteractionWorksWhenLookingDown() {
        InteractionSystem interactionSystem = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC npc = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0.9f, -1.5f);
        List<NPC> npcs = npcManager.getNPCs();

        Vector3 playerPos = new Vector3(0, 1.62f, 0);
        // Slightly down — camera at eye height looking at NPC centre
        Vector3 lookDir = new Vector3(0, -0.3f, -1f).nor();

        NPC found = interactionSystem.findNPCInRange(playerPos, lookDir, npcs);
        assertNotNull(found, "Should find NPC when camera is slightly pitched downward (typical case)");
    }

    /**
     * Test 4: Interaction still fails when player is not facing the NPC.
     *
     * Player at (0, 0, 0), NPC at (0, 0, -1.5f).
     * lookDirection = (0, 0, 1) — facing away from the NPC.
     * Verify findNPCInRange returns null.
     */
    @Test
    void test4_InteractionFailsWhenFacingAway() {
        InteractionSystem interactionSystem = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC npc = npcManager.spawnNPC(NPCType.SHOPKEEPER, 0, 0, -1.5f);
        List<NPC> npcs = npcManager.getNPCs();

        Vector3 playerPos = new Vector3(0, 0, 0);
        // Facing the opposite direction
        Vector3 lookDir = new Vector3(0, 0, 1).nor();

        NPC found = interactionSystem.findNPCInRange(playerPos, lookDir, npcs);
        assertNull(found, "Should NOT find NPC when player is facing away from it");
    }

    /**
     * Test 5: Interaction works with a quest NPC (building type) when pitched upward.
     *
     * Simulates the exact scenario from the issue report: quest NPC (e.g. GREGGS
     * shopkeeper) fails when player looks slightly up.
     * Player at (0, 0, 0), NPC at (0, 0.9f, -1.5f).
     * lookDirection pitched upward at ~30 degrees.
     */
    @Test
    void test5_QuestNpcInteractionWorksWhenPitchedUp() {
        InteractionSystem interactionSystem = new InteractionSystem();
        NPCManager npcManager = new NPCManager();
        NPC questNpc = npcManager.spawnBuildingNPC(LandmarkType.GREGGS, 0, 0.9f, -1.5f);
        assertNotNull(questNpc, "Quest NPC should be spawned");
        List<NPC> npcs = npcManager.getNPCs();

        Vector3 playerPos = new Vector3(0, 0, 0);
        // Looking up at the NPC's face (upward pitch)
        Vector3 lookDir = new Vector3(0, 0.6f, -1f).nor();

        NPC found = interactionSystem.findNPCInRange(playerPos, lookDir, npcs);
        assertNotNull(found, "Quest NPC should be findable even when player looks upward");
        assertEquals(LandmarkType.GREGGS, found.getBuildingType(),
                "Found NPC should be the GREGGS quest NPC");
    }
}
