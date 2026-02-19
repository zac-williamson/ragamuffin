package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.GangTerritorySystem;
import ragamuffin.ai.NPCManager;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Critic 7 Integration Tests — Gang Territory System
 *
 * Verifies that:
 * - GangTerritorySystem starts CLEAR with no territories
 * - Territories can be registered and contain/exclude positions correctly
 * - Player entering a territory transitions to WARNED and shows tooltip
 * - Player lingering past the threshold transitions to HOSTILE
 * - Nearby YOUTH_GANG NPCs are set AGGRESSIVE when state becomes HOSTILE
 * - Leaving territory resets to CLEAR
 * - Attacking a gang member immediately escalates to HOSTILE
 * - reset() returns to CLEAR
 * - New tooltips GANG_TERRITORY_ENTERED and GANG_TERRITORY_HOSTILE have non-empty messages
 */
class Critic7GangTerritoryTest {

    private GangTerritorySystem gangTerritory;
    private TooltipSystem tooltipSystem;
    private NPCManager npcManager;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        gangTerritory = new GangTerritorySystem();
        tooltipSystem = new TooltipSystem();
        npcManager = new NPCManager();
        player = new Player(0, 1, 0);
        world = new World(42L);
    }

    /**
     * Test 1: Initial state is CLEAR with no territories registered.
     */
    @Test
    void test1_InitialStateIsClear() {
        assertEquals(GangTerritorySystem.TerritoryState.CLEAR, gangTerritory.getState(),
            "Territory state should start CLEAR");
        assertNull(gangTerritory.getCurrentTerritory(), "No current territory at start");
        assertEquals(0, gangTerritory.getTerritoryCount(), "No territories registered at start");
        assertEquals(0f, gangTerritory.getLingerTimer(), 0.001f, "Linger timer should be zero");
    }

    /**
     * Test 2: Territory.contains() correctly identifies inside vs outside positions.
     */
    @Test
    void test2_TerritoryContainsCheck() {
        gangTerritory.addTerritory("Test Patch", 100f, 100f, 10f);
        assertEquals(1, gangTerritory.getTerritoryCount(), "One territory should be registered");

        GangTerritorySystem.Territory t = gangTerritory.findContaining(100f, 100f);
        assertNotNull(t, "Centre of territory should be inside");
        assertEquals("Test Patch", t.name);

        assertNotNull(gangTerritory.findContaining(109f, 100f), "Just inside radius should be inside");
        assertNull(gangTerritory.findContaining(111f, 100f), "Just outside radius should be outside");
    }

    /**
     * Test 3: Player entering a territory transitions state to WARNED.
     */
    @Test
    void test3_EnteringTerritoryWarns() {
        gangTerritory.addTerritory("Bricky Estate", -50f, -50f, 15f);
        // Move player inside territory
        player.getPosition().set(-50f, 1f, -50f);

        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritory.getState(),
            "State should be WARNED after entering territory");
        assertNotNull(gangTerritory.getCurrentTerritory(), "Current territory should be set");
        assertEquals("Bricky Estate", gangTerritory.getCurrentTerritory().name);
    }

    /**
     * Test 4: Entering territory triggers GANG_TERRITORY_ENTERED tooltip.
     */
    @Test
    void test4_EntryTooltipTriggered() {
        gangTerritory.addTerritory("Bricky Estate", -50f, -50f, 15f);
        player.getPosition().set(-50f, 1f, -50f);

        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        tooltipSystem.update(0.01f);

        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_ENTERED),
            "GANG_TERRITORY_ENTERED tooltip should trigger on entry");
    }

    /**
     * Test 5: Lingering past threshold transitions to HOSTILE.
     */
    @Test
    void test5_LingerPastThresholdBecomesHostile() {
        gangTerritory.addTerritory("Bricky Estate", -50f, -50f, 15f);
        player.getPosition().set(-50f, 1f, -50f);

        // Enter territory
        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritory.getState());

        // Linger beyond threshold
        float remaining = GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f;
        gangTerritory.update(remaining, player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritory.getState(),
            "State should be HOSTILE after lingering past threshold");
    }

    /**
     * Test 6: Hostile state triggers GANG_TERRITORY_HOSTILE tooltip.
     */
    @Test
    void test6_HostileTooltipTriggered() {
        gangTerritory.addTerritory("Bricky Estate", -50f, -50f, 15f);
        player.getPosition().set(-50f, 1f, -50f);

        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritory.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f,
            player, tooltipSystem, npcManager, world);

        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_HOSTILE),
            "GANG_TERRITORY_HOSTILE tooltip should trigger on going hostile");
    }

    /**
     * Test 7: Nearby YOUTH_GANG NPCs are set AGGRESSIVE when state becomes HOSTILE.
     */
    @Test
    void test7_NearbyGangsBecomesAggressiveOnHostile() {
        gangTerritory.addTerritory("Bricky Estate", 0f, 0f, 20f);
        player.getPosition().set(0f, 1f, 0f);

        // Spawn a YOUTH_GANG NPC near the player
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG, 5f, 1f, 5f);
        assertNotNull(gangMember, "Gang NPC should be spawned");
        gangMember.setState(NPCState.WANDERING);

        // Enter territory then linger to HOSTILE
        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritory.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f,
            player, tooltipSystem, npcManager, world);

        assertEquals(NPCState.AGGRESSIVE, gangMember.getState(),
            "Nearby YOUTH_GANG NPC should be AGGRESSIVE once territory turns hostile");
    }

    /**
     * Test 8: Leaving territory resets state to CLEAR.
     */
    @Test
    void test8_LeavingTerritoryResetsToClear() {
        gangTerritory.addTerritory("Bricky Estate", -50f, -50f, 15f);
        player.getPosition().set(-50f, 1f, -50f);

        // Enter and get warned
        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritory.getState());

        // Move player far outside territory
        player.getPosition().set(0f, 1f, 0f);
        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.CLEAR, gangTerritory.getState(),
            "State should return to CLEAR when player leaves territory");
        assertNull(gangTerritory.getCurrentTerritory(), "No current territory after leaving");
    }

    /**
     * Test 9: onPlayerAttacksGang immediately escalates to HOSTILE (skip linger timer).
     */
    @Test
    void test9_AttackingGangSkipsToHostile() {
        gangTerritory.addTerritory("Bricky Estate", 0f, 0f, 20f);
        player.getPosition().set(0f, 1f, 0f);

        // Enter territory — now WARNED
        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritory.getState());

        // Player attacks a gang member — should skip directly to HOSTILE
        gangTerritory.onPlayerAttacksGang(tooltipSystem, npcManager, player, world);

        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritory.getState(),
            "Attacking a gang member should immediately set state to HOSTILE");
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_HOSTILE),
            "GANG_TERRITORY_HOSTILE tooltip should trigger on player attack");
    }

    /**
     * Test 10: reset() returns to CLEAR regardless of current state.
     */
    @Test
    void test10_ResetReturnsToClear() {
        gangTerritory.addTerritory("Bricky Estate", 0f, 0f, 20f);
        player.getPosition().set(0f, 1f, 0f);

        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritory.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f,
            player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritory.getState());

        gangTerritory.reset();

        assertEquals(GangTerritorySystem.TerritoryState.CLEAR, gangTerritory.getState(),
            "State should be CLEAR after reset");
        assertNull(gangTerritory.getCurrentTerritory(), "No current territory after reset");
        assertEquals(0f, gangTerritory.getLingerTimer(), 0.001f, "Linger timer should be 0 after reset");
    }

    /**
     * Test 11: Multiple territories can be registered; only the entered one activates.
     */
    @Test
    void test11_MultipleTerritoriesOnlyActiveOneTriggered() {
        gangTerritory.addTerritory("North End", 100f, 100f, 10f);
        gangTerritory.addTerritory("South Patch", -100f, -100f, 10f);
        assertEquals(2, gangTerritory.getTerritoryCount());

        // Player enters North End
        player.getPosition().set(100f, 1f, 100f);
        gangTerritory.update(0.1f, player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritory.getState());
        assertEquals("North End", gangTerritory.getCurrentTerritory().name,
            "Only the territory the player is in should activate");

        // South Patch should not be active
        assertNull(gangTerritory.findContaining(player.getPosition().x, player.getPosition().z - 200f),
            "South Patch should not contain north-end position");
    }

    /**
     * Test 12: Tooltip messages for new gang territory triggers are non-empty.
     */
    @Test
    void test12_TooltipMessagesAreNonEmpty() {
        assertFalse(TooltipTrigger.GANG_TERRITORY_ENTERED.getMessage().isEmpty(),
            "GANG_TERRITORY_ENTERED message should not be empty");
        assertFalse(TooltipTrigger.GANG_TERRITORY_HOSTILE.getMessage().isEmpty(),
            "GANG_TERRITORY_HOSTILE message should not be empty");
    }
}
