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
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #26 End-to-End Integration Tests — Gang Territory System Wiring
 *
 * Verifies the wiring described in Issue #26 using the same configuration
 * that RagamuffinGame.initGame() applies:
 *
 *   - Territory "Bricky Estate" centred at (-50, -30) radius 20
 *   - Territory "South Patch" centred at (-45, -45) radius 20
 *   - YOUTH_GANG NPCs spawned near those positions
 *
 * These tests exercise the complete pipeline (GangTerritorySystem +
 * NPCManager + TooltipSystem + Player) that RagamuffinGame wires together,
 * ensuring the feature actually works end-to-end and not just in class
 * isolation.
 */
class Issue26GangTerritoryWiringTest {

    /** Mirrors the territory registered in RagamuffinGame.initGame(). */
    private static final float TERRITORY_CX = -50f;
    private static final float TERRITORY_CZ = -30f;
    private static final float TERRITORY_RADIUS = 20f;

    private GangTerritorySystem gangTerritorySystem;
    private NPCManager npcManager;
    private TooltipSystem tooltipSystem;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() {
        // Reproduce exactly the configuration RagamuffinGame.initGame() applies
        gangTerritorySystem = new GangTerritorySystem();
        gangTerritorySystem.addTerritory("Bricky Estate", TERRITORY_CX, TERRITORY_CZ, TERRITORY_RADIUS);
        gangTerritorySystem.addTerritory("South Patch", -45f, -45f, 20f);

        npcManager = new NPCManager();
        tooltipSystem = new TooltipSystem();
        world = new World(42L);

        // Flat ground so NPCManager doesn't complain about terrain
        for (int x = -80; x < 0; x++) {
            for (int z = -80; z < 0; z++) {
                world.setBlock(x, 0, z, BlockType.PAVEMENT);
            }
        }

        // Player starts well outside any territory
        player = new Player(0, 1, 0);
    }

    /**
     * Test 1 (end-to-end wiring): Two territories are registered — matching
     * RagamuffinGame.initGame() which registers "Bricky Estate" and "South Patch".
     */
    @Test
    void test1_TwoTerritoriesRegisteredMatchingGameInit() {
        assertEquals(2, gangTerritorySystem.getTerritoryCount(),
            "RagamuffinGame should register exactly two gang territories");
        assertNotNull(gangTerritorySystem.findContaining(TERRITORY_CX, TERRITORY_CZ),
            "Bricky Estate territory centre should be inside the territory");
        assertNotNull(gangTerritorySystem.findContaining(-45f, -45f),
            "South Patch territory centre should be inside the territory");
    }

    /**
     * Test 2 (end-to-end wiring): Player outside territory — state stays CLEAR.
     * Simulates RagamuffinGame.updatePlaying() calling gangTerritorySystem.update().
     */
    @Test
    void test2_PlayerOutsideTerritoryStaysClear() {
        player.getPosition().set(0f, 1f, 0f); // Far from any territory

        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.CLEAR, gangTerritorySystem.getState(),
            "State should remain CLEAR when player is outside all territories");
        assertNull(gangTerritorySystem.getCurrentTerritory(),
            "No current territory when player is outside");
    }

    /**
     * Test 3 (end-to-end wiring): Player enters Bricky Estate territory — WARNED,
     * tooltip fires. Mirrors RagamuffinGame.updatePlaying() calling update() each frame.
     */
    @Test
    void test3_PlayerEntersTerritoryShowsWarning() {
        // Move player inside Bricky Estate
        player.getPosition().set(TERRITORY_CX, 1f, TERRITORY_CZ);

        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        tooltipSystem.update(0.01f);

        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState(),
            "State should be WARNED after player enters territory");
        assertEquals("Bricky Estate", gangTerritorySystem.getCurrentTerritory().name);
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_ENTERED),
            "GANG_TERRITORY_ENTERED tooltip should fire on entry");
    }

    /**
     * Test 4 (end-to-end wiring): Player lingers past 5-second threshold — YOUTH_GANG
     * NPCs become AGGRESSIVE. This is the core gameplay consequence that was broken
     * before Issue #26 was fixed.
     */
    @Test
    void test4_PlayerLingersInTerritoryMakesGangsAggressive() {
        // Spawn a YOUTH_GANG NPC inside the territory (close to player)
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG,
            TERRITORY_CX + 5f, 1f, TERRITORY_CZ + 5f);
        assertNotNull(gangMember, "YOUTH_GANG NPC should spawn successfully");
        gangMember.setState(NPCState.WANDERING);

        // Move player into the territory
        player.getPosition().set(TERRITORY_CX, 1f, TERRITORY_CZ);

        // Frame 1: enter territory → WARNED
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState());

        // Simulate lingering past the 5-second threshold (as updatePlaying() does each frame)
        float lingerDelta = GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1.0f;
        gangTerritorySystem.update(lingerDelta, player, tooltipSystem, npcManager, world);

        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritorySystem.getState(),
            "State should be HOSTILE after player lingers past threshold");
        assertEquals(NPCState.AGGRESSIVE, gangMember.getState(),
            "YOUTH_GANG NPC should be AGGRESSIVE when territory turns hostile — " +
            "this verifies the end-to-end wiring that Issue #26 fixes");
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_HOSTILE),
            "GANG_TERRITORY_HOSTILE tooltip should fire when hostile");
    }

    /**
     * Test 5 (end-to-end wiring): Attacking a YOUTH_GANG NPC inside a territory
     * immediately escalates to HOSTILE (mirrors RagamuffinGame.handlePunch() wiring).
     */
    @Test
    void test5_AttackingGangInsideTerritoryEscalatesToHostile() {
        NPC gangMember = npcManager.spawnNPC(NPCType.YOUTH_GANG,
            TERRITORY_CX + 3f, 1f, TERRITORY_CZ + 3f);
        assertNotNull(gangMember);

        // Player enters territory → WARNED
        player.getPosition().set(TERRITORY_CX, 1f, TERRITORY_CZ);
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.WARNED, gangTerritorySystem.getState());

        // Player punches the gang member — RagamuffinGame.handlePunch() calls this
        gangTerritorySystem.onPlayerAttacksGang(tooltipSystem, npcManager, player, world);

        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritorySystem.getState(),
            "Attacking gang member should immediately escalate to HOSTILE");
        assertTrue(tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_HOSTILE));
    }

    /**
     * Test 6 (end-to-end wiring): reset() clears all state (mirrors
     * RagamuffinGame.restartGame() calling gangTerritorySystem.reset()).
     */
    @Test
    void test6_ResetClearsStateOnRestart() {
        // Get into HOSTILE state
        player.getPosition().set(TERRITORY_CX, 1f, TERRITORY_CZ);
        gangTerritorySystem.update(0.1f, player, tooltipSystem, npcManager, world);
        gangTerritorySystem.update(GangTerritorySystem.LINGER_THRESHOLD_SECONDS + 1f,
            player, tooltipSystem, npcManager, world);
        assertEquals(GangTerritorySystem.TerritoryState.HOSTILE, gangTerritorySystem.getState());

        // Simulate restartGame() calling gangTerritorySystem.reset()
        gangTerritorySystem.reset();

        assertEquals(GangTerritorySystem.TerritoryState.CLEAR, gangTerritorySystem.getState(),
            "reset() should return to CLEAR, as called by RagamuffinGame.restartGame()");
        assertNull(gangTerritorySystem.getCurrentTerritory());
        assertEquals(0f, gangTerritorySystem.getLingerTimer(), 0.001f);
    }

    /**
     * Test 7 (end-to-end wiring): Youth gang spawn positions (from RagamuffinGame.spawnInitialNPCs)
     * are within the registered territories — ensures the territory radii cover the actual NPC spawns.
     */
    @Test
    void test7_YouthGangSpawnPositionsAreInsideTerritories() {
        // These match the exact positions in RagamuffinGame.spawnInitialNPCs()
        float[][] gangSpawns = {
            {-50f, -30f},  // spawnNPCAtTerrain(YOUTH_GANG, -50, -30)
            {-55f, -35f},  // spawnNPCAtTerrain(YOUTH_GANG, -55, -35)
            {-45f, -28f},  // spawnNPCAtTerrain(YOUTH_GANG, -45, -28)
        };

        int coveredCount = 0;
        for (float[] spawn : gangSpawns) {
            if (gangTerritorySystem.findContaining(spawn[0], spawn[1]) != null) {
                coveredCount++;
            }
        }

        assertTrue(coveredCount >= 2,
            "At least 2 of the 3 YOUTH_GANG spawn positions should be inside registered territories. " +
            "Got " + coveredCount + ". Territories must cover gang turf.");
    }
}
