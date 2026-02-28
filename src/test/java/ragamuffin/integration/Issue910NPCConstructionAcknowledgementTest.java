package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #910: NPCs should acknowledge player constructions.
 *
 * Verifies that:
 * 1. A PUBLIC NPC near a player-built structure transitions to a reaction state
 *    (STARING, PHOTOGRAPHING, or COMPLAINING).
 * 2. Once at the structure, the NPC speaks dialogue acknowledging it.
 * 3. After the reaction timer expires, the NPC returns to WANDERING.
 */
class Issue910NPCConstructionAcknowledgementTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(12345);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Flat ground
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * A PUBLIC NPC near a player-built structure of 10+ blocks should enter
     * a construction-reaction state (STARING, PHOTOGRAPHING, or COMPLAINING).
     */
    @Test
    void testNPCEntersReactionStateNearPlayerStructure() {
        // Place 12 player-placed WOOD blocks near the NPC spawn point
        for (int x = 5; x < 9; x++) {
            for (int y = 1; y <= 3; y++) {
                world.setPlayerBlock(x, y, 5, BlockType.WOOD);
            }
        }

        // Spawn a PUBLIC NPC adjacent to the structure
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, 3, 1, 5);
        assertNotNull(npc);

        // Force the structure check so the NPC detects the construction immediately
        npcManager.forceCheckForPlayerStructures(npc, world);

        NPCState state = npc.getState();
        assertTrue(
            state == NPCState.STARING || state == NPCState.PHOTOGRAPHING || state == NPCState.COMPLAINING,
            "NPC should be STARING, PHOTOGRAPHING, or COMPLAINING near a player construction, but was: " + state
        );
    }

    /**
     * When the NPC is already at the structure (close enough), calling
     * forceUpdateReactingToStructure should make the NPC speak a dialogue line.
     */
    @Test
    void testNPCSpeaksWhenAtStructure() {
        // Place 12 player-placed WOOD blocks at (5,1-3,5)
        for (int x = 5; x < 9; x++) {
            for (int y = 1; y <= 3; y++) {
                world.setPlayerBlock(x, y, 5, BlockType.WOOD);
            }
        }

        // Spawn NPC right next to the structure (within 3 blocks)
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 7);
        assertNotNull(npc);

        // Force detection so the NPC enters a reaction state
        npcManager.forceCheckForPlayerStructures(npc, world);

        NPCState state = npc.getState();
        assertTrue(
            state == NPCState.STARING || state == NPCState.PHOTOGRAPHING || state == NPCState.COMPLAINING,
            "NPC must be in a reaction state before testing speech"
        );

        // Simulate multiple update ticks so the NPC has time to speak
        // (NPC needs to arrive at structure and trigger speech)
        for (int i = 0; i < 120; i++) {
            npcManager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        // The NPC should have spoken something (speech text is non-null while still speaking,
        // or may have already cleared after 4 seconds; use forceUpdateReactingToStructure
        // with NPC already at structure to reliably trigger speech)
        // Re-spawn NPC directly at structure center for a clean test
        NPC npc2 = npcManager.spawnNPC(NPCType.PUBLIC, 6, 1, 5);
        assertNotNull(npc2);
        npcManager.forceCheckForPlayerStructures(npc2, world);
        assertTrue(
            npc2.getState() == NPCState.STARING
            || npc2.getState() == NPCState.PHOTOGRAPHING
            || npc2.getState() == NPCState.COMPLAINING,
            "Second NPC should also enter reaction state"
        );

        // Force the reaction update with the NPC already at the structure — should trigger speech
        npcManager.forceUpdateReactingToStructure(npc2, 0.016f, world);

        // NPC should now be speaking
        assertTrue(
            npc2.isSpeaking(),
            "NPC should be speaking dialogue acknowledging the player's construction"
        );
        assertNotNull(npc2.getSpeechText(),
            "NPC speech text should not be null when acknowledging a construction");
    }

    /**
     * After STRUCTURE_REACTION_DURATION seconds, the NPC should return to WANDERING.
     */
    @Test
    void testNPCReturnsToWanderingAfterReactionTimeout() {
        // Place 12 player-placed blocks
        for (int x = 5; x < 9; x++) {
            for (int y = 1; y <= 3; y++) {
                world.setPlayerBlock(x, y, 5, BlockType.WOOD);
            }
        }

        // Spawn NPC right at the structure so it doesn't need to travel
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, 6, 1, 5);
        assertNotNull(npc);
        npcManager.forceCheckForPlayerStructures(npc, world);

        assertTrue(
            npc.getState() == NPCState.STARING
            || npc.getState() == NPCState.PHOTOGRAPHING
            || npc.getState() == NPCState.COMPLAINING,
            "NPC must enter a reaction state first"
        );

        // Simulate 15 seconds — more than STRUCTURE_REACTION_DURATION (10s)
        for (int i = 0; i < 900; i++) {
            npcManager.update(1.0f / 60.0f, world, player, inventory, tooltipSystem);
        }

        assertEquals(NPCState.WANDERING, npc.getState(),
            "NPC should return to WANDERING after the reaction timer expires");
    }
}
