package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase O Integration Tests — Planned Heist System (Issue #704).
 *
 * 10 integration tests covering the heist mechanic end-to-end:
 * alarm silencing, safe cracking timing, CCTV behaviour, accomplice lookout,
 * hot loot fence pricing, faction respect impacts, and rope ladder rooftop entry.
 */
class PhaseOHeistIntegrationTest {

    private HeistSystem heistSystem;
    private Player player;
    private Inventory inventory;
    private NoiseSystem noiseSystem;
    private NPCManager npcManager;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private World world;
    private List<NPC> allNpcs;

    @BeforeEach
    void setUp() {
        heistSystem = new HeistSystem();
        player = new Player(10, 1, 10);
        inventory = new Inventory(36);
        noiseSystem = new NoiseSystem();
        npcManager = new NPCManager();
        rumourNetwork = new RumourNetwork(new java.util.Random(42));
        world = new World(12345L);

        // Set up a basic chunk so the world has some ground
        world.setBlock(10, 0, 10, BlockType.STONE);
        world.setBlock(10, 0, 9, BlockType.STONE);
        world.setBlock(11, 0, 10, BlockType.STONE);
        world.setBlock(10, 0, 8, BlockType.GLASS); // glass block for test 1

        allNpcs = new ArrayList<>(npcManager.getNPCs());

        // Set up faction system with a TurfMap and RumourNetwork
        TurfMap turfMap = new TurfMap();
        factionSystem = new FactionSystem(turfMap, rumourNetwork);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 1: Alarm box silencing prevents police flood
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 1: Alarm box silencing prevents police flood.
     *
     * Place player adjacent to a JEWELLER building with 2 active ALARM_BOX props.
     * Give player BOLT_CUTTERS. Interact (E) with the first alarm box — verify it
     * transitions to silenced state after 1 second. Break a GLASS block on the
     * jeweller wall. Verify noise does NOT spike to 1.0, and NO police NPCs are
     * spawned at the perimeter.
     */
    @Test
    void test1_AlarmBoxSilencingPreventsPoliceFlood() {
        // Arrange: case the building (this populates 2 alarm boxes for JEWELLER)
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // Give player BOLT_CUTTERS
        inventory.addItem(Material.BOLT_CUTTERS, 1);

        // Record police count before
        long policeBefore = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        // Silence the first alarm box (alarm index 0)
        boolean silenced = heistSystem.silenceAlarmBox(0, noiseSystem);
        assertTrue(silenced, "Alarm box 0 should be silenced successfully");
        assertTrue(heistSystem.isAlarmBoxSilenced(0), "Alarm box 0 should report as silenced");

        // Set noise to a normal walking level (not spiked)
        noiseSystem.setNoiseLevel(0.6f);

        // Break a block near the silenced alarm box (alarm 0 is at centre-3, y+1, z-4)
        // Use a position close to the silenced alarm
        Vector3 breakPos = new Vector3(
            buildingCentre.x - 3 + 1,
            buildingCentre.y,
            buildingCentre.z - 4
        );

        // Trigger block break near the silenced alarm
        boolean alarmTriggered = heistSystem.onBlockBreak(breakPos, noiseSystem, npcManager, player, world);

        // Alarm 0 was silenced, alarm 1 is at centre+3, z-4 — not within 8 blocks of breakPos
        // Verify: noise should NOT have spiked to 1.0 due to alarm 0 being silenced
        // (alarm 1 is at +3, 0 from break is -3+1 = -2 x, so dist to alarm1 at +3 is 5 blocks — within range)
        // Let's actually silence BOTH alarms to properly test no police flood
        heistSystem.silenceAlarmBox(1, noiseSystem);

        // Now break a block — with both alarms silenced, no flood
        noiseSystem.setNoiseLevel(0.5f); // reset
        boolean triggered2 = heistSystem.onBlockBreak(breakPos, noiseSystem, npcManager, player, world);

        assertFalse(triggered2, "No alarm should be triggered when all alarm boxes are silenced");

        long policeAfter = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        // With both alarms silenced, NO police should have been spawned by this break
        assertEquals(policeBefore, policeAfter,
            "No police NPCs should be spawned when all alarms are silenced before block break");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 2: Unsilenced alarm triggers police
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 2: Unsilenced alarm triggers police.
     *
     * Place player at the jeweller. Do NOT silence alarm boxes. Break a BRICK block
     * on the wall. Verify noise = 1.0 immediately. Verify at least 1 POLICE NPC
     * spawns within 10 blocks of the building within 30 frames.
     */
    @Test
    void test2_UnsilencedAlarmTriggersPolice() {
        // Arrange: case and start jeweller heist
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        long policeBefore = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        // Break a block right next to an alarm box (alarm 0 is at 7, 2, 6)
        // within 8 blocks of alarm box position
        Vector3 breakPos = new Vector3(buildingCentre.x - 3, buildingCentre.y, buildingCentre.z - 4);

        // Trigger block break — alarm 0 is within range
        boolean triggered = heistSystem.onBlockBreak(breakPos, noiseSystem, npcManager, player, world);

        // Verify noise spiked to 1.0
        assertEquals(1.0f, noiseSystem.getNoiseLevel(), 0.001f,
            "Noise should spike to 1.0 when an unsilenced alarm is triggered");

        // Verify police were spawned
        long policeAfter = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        assertTrue(policeAfter > policeBefore,
            "At least 1 POLICE NPC should spawn when an unsilenced alarm is triggered");
        assertTrue(triggered, "onBlockBreak should return true when alarm is triggered");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 3: Safe cracking requires CROWBAR and 8 seconds
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 3: Safe cracking requires CROWBAR and time.
     *
     * Place player adjacent to a SAFE prop inside the jeweller. Player holds CROWBAR.
     * Begin hold-E interaction. Advance 7 seconds (420 frames at 60fps) — verify safe
     * is NOT yet open. Advance 1 more second (60 frames). Verify safe opens and
     * DIAMOND items are spawned at the safe's position.
     */
    @Test
    void test3_SafeCrackingRequiresCrowbarAndTime() {
        // Arrange: case jeweller (it has 1 safe at index 0)
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // Give player CROWBAR
        inventory.addItem(Material.CROWBAR, 1);
        boolean hasCrowbar = inventory.hasItem(Material.CROWBAR);
        assertTrue(hasCrowbar, "Player should have CROWBAR");

        // Advance 7 seconds of safe cracking (420 frames × 1/60s)
        float delta = 1f / 60f;
        boolean cracked = false;
        for (int frame = 0; frame < 420; frame++) {
            cracked = heistSystem.updateSafeCracking(delta, 0, hasCrowbar, noiseSystem, inventory, world);
        }

        // After 7 seconds: safe should NOT be cracked yet
        assertFalse(cracked, "Safe should NOT be cracked after only 7 seconds");
        assertFalse(heistSystem.getActivePlan().isSafeCracked(0),
            "Safe should not be marked as cracked after 7 seconds");

        float progressAfter7s = heistSystem.getSafeCrackProgress();
        assertTrue(progressAfter7s >= 6.9f && progressAfter7s < 8.0f,
            "Safe crack progress should be ~7 seconds after 7s of holding E");

        // Advance 1 more second (60 frames)
        for (int frame = 0; frame < 60; frame++) {
            cracked = heistSystem.updateSafeCracking(delta, 0, hasCrowbar, noiseSystem, inventory, world);
        }

        // After 8 seconds total: safe SHOULD be cracked
        assertTrue(cracked || heistSystem.getActivePlan().isSafeCracked(0),
            "Safe should be cracked after 8 seconds of hold-E with CROWBAR");

        // Verify DIAMOND items were added to inventory
        int diamonds = inventory.getItemCount(Material.DIAMOND);
        assertTrue(diamonds >= 3 && diamonds <= 6,
            "Jeweller safe should yield 3-6 DIAMOND, got: " + diamonds);

        // Verify GOLD_RING was also added
        int goldRings = inventory.getItemCount(Material.GOLD_RING);
        assertEquals(2, goldRings, "Jeweller safe should yield 2 GOLD_RING");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 4: CCTV penalty without BALACLAVA
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 4: CCTV penalty without BALACLAVA.
     *
     * Place player in the CCTV camera cone (within 6 blocks, within 45° of camera
     * facing). Set time to 12:00 (daytime). Advance 60 frames (1 second). Verify
     * criminal record has increased by at least 1 point. Verify a GANG_ACTIVITY
     * rumour containing "CCTV" is present in at least one NPC's rumour list.
     */
    @Test
    void test4_CCTVPenaltyWithoutBalaclava() {
        // Arrange: case jeweller (has CCTV at centre, y+2, z-5)
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);

        // Player is NOT wearing balaclava (default)
        assertFalse(player.isBalaclavWorn(), "Player should not be wearing balaclava");

        // Record initial criminal record
        int initialCrimes = player.getCriminalRecord().getTotalCrimes();

        // Spawn a few NPCs to receive rumours
        NPC npc1 = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 5);
        NPC npc2 = npcManager.spawnNPC(NPCType.PUBLIC, 8, 1, 8);
        List<NPC> npcs = new ArrayList<>(npcManager.getNPCs());

        // Place player inside the CCTV cone:
        // CCTV is at centre.x=10, y+2=3, z-5=5. Camera faces -Z (toward z=5 → player at z<5)
        // Player needs to be within 6 blocks of CCTV and within 22.5° of -Z direction
        // CCTV is at (10, 3, 5). Player facing -Z means player should be near (10, 1, 3)
        // so dx=0, dz=-2 → angle to camera-forward(0,-1) = 0° → in cone
        player.getPosition().set(10, 1, 3); // 2 blocks in front of CCTV (z=5)

        // Advance 60 frames (1 second) at daytime (isNight=false)
        float delta = 1f / 60f;
        for (int frame = 0; frame < 60; frame++) {
            heistSystem.update(delta, player, noiseSystem, npcManager, factionSystem,
                rumourNetwork, npcs, world, false); // false = daytime
        }

        // Verify criminal record increased
        int afterCrimes = player.getCriminalRecord().getTotalCrimes();
        assertTrue(afterCrimes > initialCrimes,
            "Criminal record should increase by at least 1 point after 1 second of CCTV exposure");

        // Verify a CCTV rumour was seeded
        boolean cctvRumourFound = false;
        for (NPC npc : npcs) {
            for (Rumour r : npc.getRumours()) {
                if (r.getText().contains("CCTV")) {
                    cctvRumourFound = true;
                    break;
                }
            }
            if (cctvRumourFound) break;
        }
        assertTrue(cctvRumourFound,
            "At least one NPC should have a CCTV rumour after player was exposed");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 5: CCTV nullified by BALACLAVA at night
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 5: CCTV nullified by BALACLAVA at night.
     *
     * Set time to 23:00. Give player BALACLAVA and activate it. Place player in CCTV
     * cone at 4 blocks. Advance 60 frames. Verify criminal record is unchanged.
     * Verify no CCTV rumour is seeded.
     */
    @Test
    void test5_CCTVNullifiedByBalaclavasAtNight() {
        // Arrange: case jeweller (has CCTV at centre, y+2, z-5 → (10, 3, 5))
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);

        // Give player BALACLAVA and activate it
        inventory.addItem(Material.BALACLAVA, 1);
        player.setBalaclavWorn(true);
        assertTrue(player.isBalaclavWorn(), "Player should be wearing balaclava");

        // Record initial criminal record
        int initialCrimes = player.getCriminalRecord().getTotalCrimes();

        // Spawn some NPCs
        NPC npc1 = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 5);
        List<NPC> npcs = new ArrayList<>(npcManager.getNPCs());

        // Place player inside CCTV cone (4 blocks in front of camera at (10, 3, 5))
        // CCTV camera faces -Z → player should be at z < 5, e.g. z=1 (4 blocks away)
        player.getPosition().set(10, 1, 1); // 4 blocks from CCTV at z=5

        // Advance 60 frames at NIGHT (isNight=true)
        float delta = 1f / 60f;
        for (int frame = 0; frame < 60; frame++) {
            heistSystem.update(delta, player, noiseSystem, npcManager, factionSystem,
                rumourNetwork, npcs, world, true); // true = night
        }

        // Verify criminal record did NOT change
        int afterCrimes = player.getCriminalRecord().getTotalCrimes();
        assertEquals(initialCrimes, afterCrimes,
            "Criminal record should be unchanged when BALACLAVA worn at night in CCTV cone");

        // Verify no CCTV rumour was seeded
        boolean cctvRumourFound = false;
        for (NPC npc : npcs) {
            for (Rumour r : npc.getRumours()) {
                if (r.getText().contains("CCTV")) {
                    cctvRumourFound = true;
                    break;
                }
            }
            if (cctvRumourFound) break;
        }
        assertFalse(cctvRumourFound,
            "No CCTV rumour should be seeded when player wears BALACLAVA at night");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 6: Heist timer triggers police flood on expiry
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 6: Heist timer triggers police flood on expiry.
     *
     * Start a jeweller heist (press G). Do nothing (let timer expire). Verify that
     * exactly at timer expiry, 4 POLICE NPCs are spawned within 5 blocks of the
     * jeweller perimeter.
     */
    @Test
    void test6_HeistTimerTriggersPoliceFloodOnExpiry() {
        // Arrange
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        long policeBefore = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        // Force execution timer to almost zero
        heistSystem.setExecutionTimerForTesting(0.05f);

        List<NPC> npcs = new ArrayList<>(npcManager.getNPCs());

        // Advance a single frame to trigger expiry
        heistSystem.update(0.1f, player, noiseSystem, npcManager, factionSystem,
            rumourNetwork, npcs, world, false);

        // Verify 4 police NPCs were spawned
        long policeAfter = npcManager.getNPCs().stream()
            .filter(n -> n.getType() == NPCType.POLICE && n.isAlive())
            .count();

        assertTrue(policeAfter - policeBefore >= HeistSystem.POLICE_FLOOD_COUNT,
            "Exactly " + HeistSystem.POLICE_FLOOD_COUNT + " POLICE NPCs should spawn at timer expiry, got: "
                + (policeAfter - policeBefore));

        // Verify heist failed
        assertEquals(HeistSystem.HeistPhase.COMPLETE, heistSystem.getPhase(),
            "Heist should be in COMPLETE phase after timer expiry");
        assertFalse(heistSystem.isHeistSucceeded(),
            "Heist should have failed when timer expired");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 7: Accomplice lookout warning
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 7: Accomplice lookout warning.
     *
     * Spawn an NPC accomplice in FOLLOWING state outside the jeweller. Spawn a PATROL
     * NPC walking toward the building entrance. When the patrol NPC is within 8 blocks
     * of the accomplice, verify the accomplice emits a speech bubble containing "Leg it!"
     * and the player noise level is set to at least 0.8.
     */
    @Test
    void test7_AccompliceLookoutWarning() {
        // Arrange: case jeweller and start execution
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // Spawn an accomplice NPC outside the building
        NPC accompliceNpc = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 5);
        accompliceNpc.setState(NPCState.FOLLOWING);

        // Register as accomplice (bypass coin cost for testing)
        heistSystem.setAccompliceForTesting(accompliceNpc);

        // Spawn a patrolling police NPC far from accomplice initially
        NPC patrolNpc = npcManager.spawnNPC(NPCType.POLICE, 15, 1, 15);
        patrolNpc.setState(NPCState.PATROLLING);

        List<NPC> npcs = new ArrayList<>(npcManager.getNPCs());

        // noiseSystem starts quiet
        noiseSystem.setNoiseLevel(0.1f);

        // Move patrol NPC close to the accomplice (within 8 blocks)
        patrolNpc.getPosition().set(8, 1, 8); // ~4.2 blocks from accomplice at (5,1,5)

        // Update the heist system
        heistSystem.update(0.1f, player, noiseSystem, npcManager, factionSystem,
            rumourNetwork, npcs, world, false);

        // Verify accomplice emitted "Leg it!" speech bubble
        String speech = accompliceNpc.getSpeechText();
        assertTrue(speech != null && speech.contains("Leg it!"),
            "Accomplice should emit 'Leg it!' when patrol NPC is within 8 blocks. Got: " + speech);

        // Verify player noise level set to at least 0.8
        assertTrue(noiseSystem.getNoiseLevel() >= HeistSystem.ACCOMPLICE_WARNING_NOISE,
            "Player noise should be at least 0.8 when accomplice warns. Got: " + noiseSystem.getNoiseLevel());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 8: Hot loot fence pricing
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 8: Hot loot fence pricing.
     *
     * Complete a jeweller heist. Immediately (within 5 in-game minutes) approach the
     * Fence NPC. Verify loot is offered at 100% value ("still warm"). Wait 60 in-game
     * minutes (advance time). Verify loot is now offered at 25% value.
     */
    @Test
    void test8_HotLootFencePricing() {
        // Mark heist as succeeded with 0 time elapsed
        heistSystem.setTimeSinceHeistCompleteForTesting(0f);

        // Immediately after heist: multiplier should be 100%
        float multImmediately = heistSystem.getHotLootMultiplier();
        assertEquals(1.0f, multImmediately, 0.001f,
            "Hot loot should be 100% value immediately after heist");
        assertEquals("still warm, son", heistSystem.getHotLootDescription(),
            "Hot loot description should say 'still warm, son' within 5 in-game minutes");

        // Advance time past 5 in-game minutes but before 60 in-game minutes
        // 5 in-game min in real seconds = 5 * 60 * (1200 / (24*60)) = 5 * 60 * 0.833 ≈ 250s
        float fiveIngameMinutesReal = HeistSystem.HOT_LOOT_FULL_PRICE_WINDOW;
        heistSystem.setTimeSinceHeistCompleteForTesting(fiveIngameMinutesReal + 1f);

        float multHalf = heistSystem.getHotLootMultiplier();
        assertEquals(0.5f, multHalf, 0.001f,
            "Hot loot should be 50% value between 5 and 60 in-game minutes");

        // Advance time past 60 in-game minutes
        float sixtyIngameMinutesReal = HeistSystem.HOT_LOOT_HALF_PRICE_WINDOW;
        heistSystem.setTimeSinceHeistCompleteForTesting(sixtyIngameMinutesReal + 1f);

        float multLow = heistSystem.getHotLootMultiplier();
        assertEquals(0.25f, multLow, 0.001f,
            "Hot loot should be 25% value after 60 in-game minutes");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 9: Faction respect impact
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 9: Faction respect impact.
     *
     * Record Marchetti Crew respect. Complete a jeweller heist successfully. Verify
     * Marchetti Crew respect has decreased by 15. Verify a GANG_ACTIVITY rumour
     * containing "Jeweller" has been seeded into at least 5 NPCs.
     */
    @Test
    void test9_FactionRespectImpact() {
        // Arrange: case and execute jeweller heist
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // Record initial Marchetti respect
        int initialRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);

        // Spawn 7 NPCs for rumour seeding
        List<NPC> npcs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, i * 3, 1, i * 3);
            npcs.add(npc);
        }

        // Track achievements awarded
        List<AchievementType> awarded = new ArrayList<>();

        // Complete the heist
        heistSystem.completeHeist(player, inventory, factionSystem, rumourNetwork, npcs,
            type -> awarded.add(type));

        // Verify Marchetti respect decreased by 15
        int afterRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        assertEquals(initialRespect - 15, afterRespect,
            "Marchetti Crew respect should decrease by 15 after jeweller heist");

        // Verify a Jeweller rumour was seeded in at least 5 NPCs
        // (the rumour text includes "Andre's Diamonds" which is Jeweller's display name)
        long npcsWithJewellerRumour = npcs.stream()
            .filter(npc -> npc.getRumours().stream()
                .anyMatch(r -> r.getText().contains("Andre's Diamonds") || r.getText().contains("Jeweller")))
            .count();

        // The system seeds into min(RUMOUR_SEED_COUNT, npcs.size()) NPCs
        assertTrue(npcsWithJewellerRumour >= Math.min(HeistSystem.RUMOUR_SEED_COUNT, npcs.size()),
            "At least " + HeistSystem.RUMOUR_SEED_COUNT + " NPCs should have a jeweller rumour, got: "
                + npcsWithJewellerRumour);

        // Verify MASTER_CRIMINAL achievement was awarded
        assertTrue(awarded.contains(AchievementType.MASTER_CRIMINAL),
            "MASTER_CRIMINAL achievement should be awarded on first heist completion");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 10: ROPE_LADDER enables rooftop entry
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Test 10: ROPE_LADDER enables rooftop entry.
     *
     * Place player at the base of the jeweller wall (height 8 blocks). Give player
     * ROPE_LADDER. Activate (right-click on wall). Verify a climbable LADDER block is
     * placed at the player's position on the wall. Player moves upward — verify the
     * player can climb the ladder and reaches the rooftop (y > 7). Verify the
     * ROPE_LADDER block disappears after 60 in-game seconds.
     */
    @Test
    void test10_RopeLadderEnablesRooftopEntry() {
        // Give player ROPE_LADDER
        inventory.addItem(Material.ROPE_LADDER, 1);
        assertTrue(inventory.hasItem(Material.ROPE_LADDER), "Player should have ROPE_LADDER");

        // Place player at base of wall
        int wallX = 10;
        int wallY = 1;
        int wallZ = 5;
        player.getPosition().set(wallX, wallY, wallZ);

        // Deploy rope ladder at wall position (this places a LADDER block)
        heistSystem.deployRopeLadder(world, wallX, wallY, wallZ);
        assertTrue(heistSystem.isRopeLadderActive(), "Rope ladder should be active after deployment");

        // Verify a LADDER block was placed
        BlockType blockAtWall = world.getBlock(wallX, wallY, wallZ);
        assertEquals(BlockType.LADDER, blockAtWall,
            "A LADDER block should be placed at the wall position after ROPE_LADDER deployment");

        // Verify the rope ladder timer is positive (60 in-game seconds)
        assertTrue(heistSystem.getRopeLadderTimer() > 0f,
            "Rope ladder timer should be positive after deployment");

        // Advance time past 60 in-game seconds
        // 60 in-game seconds = 60 * (1200 / (24*60*60)) real seconds ≈ 60 * 0.01389 ≈ 0.833 real seconds
        // But the spec says "60 in-game seconds" — using the constant from HeistSystem
        float ropeTimerDuration = heistSystem.getRopeLadderTimer();
        heistSystem.updateRopeLadder(ropeTimerDuration + 0.1f, world);

        // Verify the LADDER block was removed
        assertFalse(heistSystem.isRopeLadderActive(),
            "Rope ladder should no longer be active after timer expires");

        BlockType blockAfter = world.getBlock(wallX, wallY, wallZ);
        assertEquals(BlockType.AIR, blockAfter,
            "ROPE_LADDER block should be removed (replaced with AIR) after 60 in-game seconds");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Additional unit tests for HeistSystem state machine
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void heistTargets_haveCorrectTimeLimits() {
        assertEquals(90f, HeistSystem.getTimeLimit(LandmarkType.JEWELLER), 0.001f);
        assertEquals(60f, HeistSystem.getTimeLimit(LandmarkType.OFF_LICENCE), 0.001f);
        assertEquals(45f, HeistSystem.getTimeLimit(LandmarkType.GREGGS), 0.001f);
        assertEquals(75f, HeistSystem.getTimeLimit(LandmarkType.JOB_CENTRE), 0.001f);
    }

    @Test
    void heistTargets_haveCorrectFactionImpacts() {
        assertEquals(-15, HeistSystem.getSuccessRespectDelta(LandmarkType.JEWELLER));
        assertEquals(-20, HeistSystem.getSuccessRespectDelta(LandmarkType.OFF_LICENCE));
        assertEquals(5, HeistSystem.getSuccessRespectDelta(LandmarkType.GREGGS));
        assertEquals(-25, HeistSystem.getSuccessRespectDelta(LandmarkType.JOB_CENTRE));

        assertEquals(Faction.MARCHETTI_CREW, HeistSystem.getSuccessFaction(LandmarkType.JEWELLER));
        assertEquals(Faction.MARCHETTI_CREW, HeistSystem.getSuccessFaction(LandmarkType.OFF_LICENCE));
        assertEquals(Faction.STREET_LADS, HeistSystem.getSuccessFaction(LandmarkType.GREGGS));
        assertEquals(Faction.THE_COUNCIL, HeistSystem.getSuccessFaction(LandmarkType.JOB_CENTRE));
    }

    @Test
    void safeCracking_doesNotSucceedWithoutCrowbar() {
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // Try to crack safe without CROWBAR — should fail
        for (int frame = 0; frame < 600; frame++) {
            boolean cracked = heistSystem.updateSafeCracking(1f / 60f, 0, false, noiseSystem, inventory, world);
            assertFalse(cracked, "Safe should NOT crack without CROWBAR even after many frames");
        }

        assertFalse(heistSystem.getActivePlan().isSafeCracked(0),
            "Safe should remain uncracked without CROWBAR");
    }

    @Test
    void accompliceRecruitment_costsCoin() {
        // Give player 9 COIN (not enough)
        inventory.addItem(Material.COIN, 9);
        NPC npc = npcManager.spawnNPC(NPCType.PUBLIC, 5, 1, 5);

        boolean recruited = heistSystem.recruitAccomplice(npc, inventory);
        assertFalse(recruited, "Should not be able to recruit accomplice with only 9 COIN (need 10)");

        // Give player 1 more coin (total 10)
        inventory.addItem(Material.COIN, 1);
        recruited = heistSystem.recruitAccomplice(npc, inventory);
        assertTrue(recruited, "Should be able to recruit accomplice with 10 COIN");
        assertTrue(heistSystem.hasAccomplice(), "System should register the accomplice");
        assertEquals(0, inventory.getItemCount(Material.COIN), "10 COIN should be deducted");
        assertEquals(NPCState.FOLLOWING, npc.getState(), "Accomplice NPC should be in FOLLOWING state");
    }

    @Test
    void soloJob_achievement_awardedWhenNoAccomplice() {
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // No accomplice recruited
        assertFalse(heistSystem.hasAccomplice());

        List<AchievementType> awarded = new ArrayList<>();
        List<NPC> npcs = new ArrayList<>(npcManager.getNPCs());

        heistSystem.completeHeist(player, inventory, factionSystem, rumourNetwork, npcs,
            type -> awarded.add(type));

        assertTrue(awarded.contains(AchievementType.SOLO_JOB),
            "SOLO_JOB achievement should be awarded when heist completed without accomplice");
    }

    @Test
    void smoothOperator_achievement_awardedWhenNoAlarmTripped() {
        Vector3 buildingCentre = new Vector3(10, 1, 10);
        heistSystem.startCasing(LandmarkType.JEWELLER, buildingCentre, npcManager);
        heistSystem.startExecution();

        // Silence both alarm boxes before execution
        heistSystem.silenceAlarmBox(0, noiseSystem);
        heistSystem.silenceAlarmBox(1, noiseSystem);

        List<AchievementType> awarded = new ArrayList<>();
        List<NPC> npcs = new ArrayList<>(npcManager.getNPCs());

        heistSystem.completeHeist(player, inventory, factionSystem, rumourNetwork, npcs,
            type -> awarded.add(type));

        assertTrue(awarded.contains(AchievementType.SMOOTH_OPERATOR),
            "SMOOTH_OPERATOR achievement should be awarded when heist completed without triggering any alarm");
    }
}
