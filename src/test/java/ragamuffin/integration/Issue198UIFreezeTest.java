package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.GangTerritorySystem;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.core.StreetReputation;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.SpeechLogUI;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #198:
 * NPCs and game systems freeze while inventory/crafting/help UI is open.
 *
 * Root cause: updatePlaying() was gated entirely behind !isUIBlocking().
 * When any UI overlay (inventory, crafting, help) was open, the whole game
 * simulation froze: NPCs, gang territory, arrest checks, and reputation decay
 * all stopped ticking.
 *
 * Fix: Split updatePlaying() into:
 *   - updatePlayingSimulation(): world systems that always run
 *   - updatePlayingInput():      player input gated behind !isUIBlocking()
 *
 * These tests verify the simulation systems directly (no LibGDX runtime needed)
 * by exercising the same subsystems that updatePlayingSimulation() calls.
 */
class Issue198UIFreezeTest {

    private World world;
    private Player player;
    private NPCManager npcManager;
    private Inventory inventory;
    private TooltipSystem tooltipSystem;

    @BeforeEach
    void setUp() {
        world = new World(42);
        player = new Player(0, 1, 0);
        npcManager = new NPCManager();
        inventory = new Inventory(36);
        tooltipSystem = new TooltipSystem();

        // Flat ground
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
            }
        }
    }

    /**
     * Test 1: NPCManager.update() advances NPC AI state regardless of UI state.
     *
     * Police transition from PATROLLING to AGGRESSIVE when the player is NOTORIOUS
     * and within 2 blocks. Place a PATROLLING police officer adjacent (1 block away)
     * with the player NOTORIOUS so the state change fires on the first update.
     *
     * If the UI were blocking and npcManager.update() were skipped (old bug),
     * the police NPC would remain PATROLLING forever and never arrest the player.
     */
    @Test
    void npcManager_updatesNPCState_evenWhenUIWouldBlock() {
        // Give player notorious reputation so police skip the warning and go straight
        // to AGGRESSIVE (see NPCManager.updatePolicePatrolling: within 2 blocks + notorious)
        player.getStreetReputation().addPoints(StreetReputation.NOTORIOUS_THRESHOLD);

        // Spawn police within 2 blocks of the player (player is at 0,1,0) and set
        // state to PATROLLING, mirroring how updatePoliceSpawning() initialises officers.
        NPC police = npcManager.spawnNPC(NPCType.POLICE, 1, 1, 0);
        assertNotNull(police);
        police.setState(NPCState.PATROLLING);

        // Run a few frames of simulation — mirrors updatePlayingSimulation() unconditional call.
        for (int i = 0; i < 10; i++) {
            npcManager.update(0.016f, world, player, inventory, tooltipSystem);
        }

        // Police must have transitioned to AGGRESSIVE (not stuck in PATROLLING).
        // If updatePlaying() was fully gated by isUIBlocking(), police would never update.
        assertEquals(NPCState.AGGRESSIVE, police.getState(),
                "Police NPC must transition to AGGRESSIVE when player is NOTORIOUS and " +
                "within 2 blocks; npcManager.update() must run even when UI is open");
    }

    /**
     * Test 2: Reputation decay continues to tick regardless of UI state.
     *
     * Before the fix, opening the inventory paused the entire updatePlaying()
     * call, which included player.getStreetReputation().update(delta). This
     * meant a notorious player could open their inventory indefinitely without
     * their reputation ever decaying.
     *
     * updatePlayingSimulation() calls reputation.update() unconditionally.
     * Here we verify that reputation does decay over time when update() is called
     * (mirrors what the simulation loop now does even with UI open).
     */
    @Test
    void reputationDecay_continuesTicking_evenWhenUIWouldBlock() {
        StreetReputation rep = player.getStreetReputation();
        rep.addPoints(StreetReputation.NOTORIOUS_THRESHOLD); // 30 pts
        assertTrue(rep.isNotorious(), "Player should start NOTORIOUS");

        // Simulate one full decay interval — mirrors updatePlayingSimulation() calling
        // player.getStreetReputation().update(delta) every frame.
        float decayInterval = StreetReputation.DECAY_INTERVAL_SECONDS;
        for (float elapsed = 0; elapsed < decayInterval + 0.1f; elapsed += 0.1f) {
            rep.update(0.1f);
        }

        // At least one point must have decayed
        assertTrue(rep.getPoints() < StreetReputation.NOTORIOUS_THRESHOLD,
                "Reputation must decay over time; if updatePlaying() was fully skipped " +
                "when UI was open, reputation would never decrease");
    }

    /**
     * Test 3: SpeechLogUI.update() captures NPC speech even when UI would block.
     *
     * Before the fix, if the inventory was open, speechLogUI.update() was never
     * called. Any speech an NPC produced during that time was silently lost.
     * The fix moves speechLogUI.update() into updatePlayingSimulation() which
     * runs every frame.
     *
     * Verify that calling speechLogUI.update() after an NPC starts speaking
     * captures the speech entry (as the fixed simulation loop now does always).
     */
    @Test
    void speechLog_capturesNPCSpeech_evenWhenUIWouldBlock() {
        SpeechLogUI speechLogUI = new SpeechLogUI();
        NPC npc = new NPC(NPCType.PUBLIC, 2, 1, 0);
        npc.setSpeechText("Oi oi!", 3.0f);

        // updatePlayingSimulation() calls speechLogUI.update() unconditionally.
        // Simulate that call — even if inventory UI was open.
        speechLogUI.update(java.util.List.of(npc), 0.016f);

        assertEquals(1, speechLogUI.getEntryCount(),
                "SpeechLogUI must capture NPC speech via update(); if the entire " +
                "updatePlaying() was blocked by UI, this would remain empty");
    }

    /**
     * Test 4: GangTerritorySystem fires warnings regardless of UI state.
     *
     * Before the fix, entering gang territory while the help screen was open
     * would suppress the territory warning entirely. The fix moves
     * gangTerritorySystem.update() into updatePlayingSimulation().
     *
     * Verify the territory warning tooltip triggers when the player enters
     * territory and update() is called (as the simulation always does now).
     */
    @Test
    void gangTerritory_firesWarning_evenWhenUIWouldBlock() {
        GangTerritorySystem gangTerritorySystem = new GangTerritorySystem();
        // Register territory directly around the player's position
        gangTerritorySystem.addTerritory("Test Turf", 0f, 0f, 10f);

        NPC gangNPC = npcManager.spawnNPC(NPCType.YOUTH_GANG, 2, 1, 2);
        assertNotNull(gangNPC);

        // Player is inside the territory (position 0,1,0 vs centre 0,0, radius 10)
        // Simulate several frames of gangTerritorySystem.update() as
        // updatePlayingSimulation() now does unconditionally.
        boolean warningTriggered = false;
        for (int i = 0; i < 10; i++) {
            gangTerritorySystem.update(0.016f, player, tooltipSystem, npcManager, world);
            if (tooltipSystem.hasShown(TooltipTrigger.GANG_TERRITORY_ENTERED)) {
                warningTriggered = true;
                break;
            }
        }

        assertTrue(warningTriggered,
                "Gang territory warning must fire when player enters territory; " +
                "if gangTerritorySystem.update() was skipped due to UI blocking, " +
                "no warning would ever appear");
    }
}
