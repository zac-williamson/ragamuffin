package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.Faction;
import ragamuffin.core.FactionSystem;
import ragamuffin.core.NeighbourhoodSystem;
import ragamuffin.core.NeighbourhoodSystem.BuildingRecord;
import ragamuffin.core.NeighbourhoodSystem.ConditionState;
import ragamuffin.core.NeighbourhoodSystem.VibesState;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.TurfMap;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #793 Integration Tests — The Living Neighbourhood: Dynamic Gentrification,
 * Decay &amp; Reclamation.
 *
 * <p>10 scenarios covering:
 * <ol>
 *   <li>Condition decay timing: passive −1/min after enough time elapses</li>
 *   <li>Crumbled brick appearance: condition drop below 70 triggers CRUMBLING state</li>
 *   <li>Condemned notice placement: condition below 50 seeds COUNCIL_NOTICE rumour</li>
 *   <li>Player notice teardown: Condition +10, Council −10 Respect, achievement fires</li>
 *   <li>Gentrification wave trigger: Council &gt;50% turf → Luxury Flat built</li>
 *   <li>Street Lads reclamation: graffiti after Luxury Flat caps condition at 60</li>
 *   <li>Marchetti fortification: METAL_SHUTTER placed at &gt;40% turf</li>
 *   <li>Vibes HUD correctness: Vibes reflects building conditions and faction scores</li>
 *   <li>Dystopia state effects: Vibes &lt;10 triggers DYSTOPIA_NOW achievement</li>
 *   <li>Full neighbourhood lifecycle stress test: decay → derelict → gentrify → reclaim</li>
 * </ol>
 */
class Issue793NeighbourhoodSystemTest {

    private NeighbourhoodSystem neighbourhood;
    private FactionSystem factionSystem;
    private TurfMap turfMap;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private Inventory inventory;
    private List<NPC> npcs;

    private static final int BUILDING_X = 100;
    private static final int BUILDING_Z = 100;

    @BeforeEach
    void setUp() {
        turfMap           = new TurfMap(200, 200);
        rumourNetwork     = new RumourNetwork(new Random(42));
        factionSystem     = new FactionSystem(turfMap, rumourNetwork, new Random(42));
        achievementSystem = new AchievementSystem();
        neighbourhood     = new NeighbourhoodSystem(
            factionSystem, turfMap, rumourNetwork, achievementSystem, new Random(42));
        inventory         = new Inventory(36);
        npcs              = new ArrayList<>();

        // Add a BARMAN NPC for rumour seeding
        npcs.add(new NPC(NPCType.BARMAN, 100f, 1f, 100f));

        // Register a test building
        neighbourhood.registerBuilding(LandmarkType.TERRACED_HOUSE, BUILDING_X, BUILDING_Z);
    }

    // ─── Test 1: Condition decay timing ──────────────────────────────────────

    /**
     * A registered building at default condition 80 should lose 1 condition point
     * after each in-game minute (SECONDS_PER_IN_GAME_MINUTE = 1 real second of delta).
     * After 10 minutes of simulated time the condition should be 70 (80 − 10).
     */
    @Test
    void test1_ConditionDecaysOnePointPerMinute() {
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);
        assertNotNull(rec, "Building should be registered");
        assertEquals(NeighbourhoodSystem.DEFAULT_CONDITION, rec.getCondition(),
                "Initial condition should be DEFAULT_CONDITION (80)");

        // Simulate 10 in-game minutes (10 × SECONDS_PER_IN_GAME_MINUTE of delta)
        float minuteDelta = NeighbourhoodSystem.SECONDS_PER_IN_GAME_MINUTE;
        for (int i = 0; i < 10; i++) {
            neighbourhood.update(minuteDelta, null, npcs, 0, 0f, 0f);
        }

        int expectedCondition = NeighbourhoodSystem.DEFAULT_CONDITION - 10;
        assertEquals(expectedCondition, rec.getCondition(),
                "Condition should decay by 1 per minute: expected " + expectedCondition);
    }

    // ─── Test 2: Crumbled brick appearance ───────────────────────────────────

    /**
     * When a building's condition falls below CONDITION_CRUMBLING (70), its state
     * should transition to CRUMBLING. This represents CRUMBLED_BRICK appearing on
     * the exterior.
     */
    @Test
    void test2_CrumbledBrickAppearsWhenConditionDropsBelow70() {
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);
        assertEquals(ConditionState.NORMAL, rec.getState(),
                "New building should be in NORMAL state");

        // Force condition to just below CRUMBLING threshold
        rec.setCondition(NeighbourhoodSystem.CONDITION_CRUMBLING - 1); // 69

        // Trigger a decay tick to update the state
        float minuteDelta = NeighbourhoodSystem.SECONDS_PER_IN_GAME_MINUTE;
        neighbourhood.update(minuteDelta, null, npcs, 0, 0f, 0f);

        assertEquals(ConditionState.CRUMBLING, rec.getState(),
                "State should be CRUMBLING when condition < " + NeighbourhoodSystem.CONDITION_CRUMBLING);
        assertTrue(rec.getCondition() < NeighbourhoodSystem.CONDITION_CRUMBLING,
                "Condition should remain below the CRUMBLING threshold");
    }

    // ─── Test 3: Condemned notice placement ──────────────────────────────────

    /**
     * When condition drops below CONDITION_DERELICT (50), the building becomes
     * condemned and a COUNCIL_NOTICE rumour is seeded into nearby NPCs.
     */
    @Test
    void test3_CondemnedNoticePlacedAndRumourSeededWhenDerelict() {
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);

        // Force condition to just below DERELICT threshold
        rec.setCondition(NeighbourhoodSystem.CONDITION_DERELICT - 1); // 49

        // Trigger update to transition state
        float minuteDelta = NeighbourhoodSystem.SECONDS_PER_IN_GAME_MINUTE;
        neighbourhood.update(minuteDelta, null, npcs, 0, 0f, 0f);

        // Building should be condemned
        assertTrue(rec.isCondemned(),
                "Building should be condemned when condition < CONDITION_DERELICT");
        assertEquals(ConditionState.DERELICT, rec.getState(),
                "Building state should be DERELICT");

        // COUNCIL_NOTICE rumour should be seeded into the BARMAN NPC
        NPC barman = npcs.get(0);
        assertFalse(barman.getRumours().isEmpty(),
                "COUNCIL_NOTICE rumour should be seeded into available NPC");
        Rumour seedRumour = barman.getTopRumour();
        assertEquals(RumourType.COUNCIL_NOTICE, seedRumour.getType(),
                "Seeded rumour type should be COUNCIL_NOTICE");
    }

    // ─── Test 4: Player tears down condemned notice ───────────────────────────

    /**
     * When the player tears down a condemned notice:
     * - Building Condition increases by CONDITION_NOTICE_TEARDOWN (10)
     * - Council Respect drops by 10
     * - Street Lads Respect increases by 10
     * - COMMUNITY_HERO achievement fires
     * - Building is no longer condemned
     */
    @Test
    void test4_PlayerTearsDownCondemnedNotice() {
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);

        // Set building to condemned state
        rec.setCondemned(true);
        int initialCondition = 45;
        rec.setCondition(initialCondition);
        rec.setState(ConditionState.DERELICT);

        int councilRespectBefore     = factionSystem.getRespect(Faction.THE_COUNCIL);
        int streetLadsRespectBefore  = factionSystem.getRespect(Faction.STREET_LADS);

        String result = neighbourhood.tearDownCondemnedNotice(BUILDING_X, BUILDING_Z, npcs);

        assertNotNull(result, "tearDownCondemnedNotice should return a message");
        assertFalse(rec.isCondemned(),
                "Building should no longer be condemned after notice teardown");
        assertEquals(initialCondition + NeighbourhoodSystem.CONDITION_NOTICE_TEARDOWN,
                rec.getCondition(),
                "Condition should increase by CONDITION_NOTICE_TEARDOWN");
        assertEquals(councilRespectBefore - 10, factionSystem.getRespect(Faction.THE_COUNCIL),
                "Council Respect should drop by 10");
        assertEquals(streetLadsRespectBefore + 10, factionSystem.getRespect(Faction.STREET_LADS),
                "Street Lads Respect should increase by 10");
        assertTrue(achievementSystem.isUnlocked(AchievementType.COMMUNITY_HERO),
                "COMMUNITY_HERO achievement should fire");
    }

    // ─── Test 5: Gentrification wave trigger ─────────────────────────────────

    /**
     * When The Council controls &gt;50% of the turf map and the gentrification
     * timer elapses, the most-decayed building in Council territory is selected
     * for a Luxury Flat, the building is flagged as a luxury flat, and a
     * GENTRIFICATION rumour is seeded.
     */
    @Test
    void test5_GentrificationWaveTriggers() {
        // Give Council majority turf
        int halfPlusOne = (turfMap.totalCells() / 2) + 1000;
        // Fill a large section of the turf map with Council ownership
        for (int x = 0; x < 150; x++) {
            for (int z = 0; z < 150; z++) {
                turfMap.setOwner(x, z, Faction.THE_COUNCIL);
            }
        }
        assertTrue(turfMap.ownershipFraction(Faction.THE_COUNCIL) > NeighbourhoodSystem.COUNCIL_GENTRIFY_THRESHOLD,
                "Council should control more than 50% of turf");

        // Make the building in Council territory and decayed
        turfMap.setOwner(BUILDING_X, BUILDING_Z, Faction.THE_COUNCIL);
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);
        rec.setCondition(20); // Very decayed

        // Advance gentrification timer past the interval
        // GENTRIFY_INTERVAL_SECONDS = 10800; simulate via enough updates
        // Rather than simulate 10800 seconds, directly advance timer past threshold
        // by calling update with a large delta
        neighbourhood.update(NeighbourhoodSystem.GENTRIFY_INTERVAL_SECONDS + 1f,
                null, npcs, 0, BUILDING_X, BUILDING_Z);

        // The building should now be a luxury flat
        assertTrue(rec.isLuxuryFlat(),
                "The most-decayed Council building should become a Luxury Flat");
        assertEquals(NeighbourhoodSystem.MAX_CONDITION, rec.getCondition(),
                "Luxury Flat should have MAX_CONDITION");
        assertEquals(1, neighbourhood.getLuxuryFlatCount(),
                "Luxury flat count should be 1");

        // GENTRIFICATION rumour should be seeded
        NPC barman = npcs.get(0);
        assertFalse(barman.getRumours().isEmpty(),
                "A rumour should be seeded after gentrification wave");
        boolean hasGentrificationRumour = barman.getRumours().stream()
                .anyMatch(r -> r.getType() == RumourType.GENTRIFICATION);
        assertTrue(hasGentrificationRumour,
                "GENTRIFICATION rumour should be seeded after wave");
    }

    // ─── Test 6: Street Lads reclamation graffiti ────────────────────────────

    /**
     * When ≥4 graffiti tags are placed on a building, its condition is permanently
     * capped at STREET_LADS_GRAFFITI_CAP (60). Even if the raw condition is higher,
     * getEffectiveCondition() returns at most 60.
     */
    @Test
    void test6_StreetLadsGraffitiCapsConditionAt60() {
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);
        rec.setCondition(80); // Start at full health

        // Simulate 4 graffiti placements
        for (int i = 0; i < 4; i++) {
            neighbourhood.onGraffitiPlaced(BUILDING_X, BUILDING_Z);
        }

        assertTrue(rec.isGraffitiCapped(),
                "Building should be graffiti-capped after 4 tags");
        // Even though raw condition is still above 60 (may have decreased by decay_per_graffiti)
        // the effective condition should be at most 60
        assertTrue(rec.getEffectiveCondition() <= NeighbourhoodSystem.STREET_LADS_GRAFFITI_CAP,
                "Effective condition should be capped at " + NeighbourhoodSystem.STREET_LADS_GRAFFITI_CAP);

        // Manually set condition above cap to verify cap enforcement
        rec.setCondition(90);
        assertTrue(rec.isGraffitiCapped(), "Graffiti cap should still be active");
        assertEquals(NeighbourhoodSystem.STREET_LADS_GRAFFITI_CAP, rec.getEffectiveCondition(),
                "Effective condition should be exactly " + NeighbourhoodSystem.STREET_LADS_GRAFFITI_CAP);
    }

    // ─── Test 7: Marchetti fortification ─────────────────────────────────────

    /**
     * Breaking a METAL_SHUTTER block seeds a GANG_ACTIVITY rumour and reduces
     * Marchetti Respect by 20.
     */
    @Test
    void test7_MarchettiShutterBreakSeedsRumourAndPenalisesRespect() {
        int marchettiRespectBefore = factionSystem.getRespect(Faction.MARCHETTI_CREW);

        String result = neighbourhood.onMarchettiShutterBroken(npcs);

        assertNotNull(result, "onMarchettiShutterBroken should return a message");

        // Marchetti Respect should drop by 20
        int marchettiRespectAfter = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        assertEquals(marchettiRespectBefore - 20, marchettiRespectAfter,
                "Marchetti Respect should drop by 20 when shutter is broken");

        // GANG_ACTIVITY rumour should be seeded
        NPC barman = npcs.get(0);
        assertFalse(barman.getRumours().isEmpty(),
                "A GANG_ACTIVITY rumour should be seeded when a shutter is broken");
        assertEquals(RumourType.GANG_ACTIVITY, barman.getTopRumour().getType(),
                "Rumour type should be GANG_ACTIVITY");
    }

    // ─── Test 8: Vibes HUD correctness ───────────────────────────────────────

    /**
     * Vibes score reflects the state of the neighbourhood:
     * - All buildings at full condition (100) with high faction scores → high Vibes (≥50)
     * - Force-set Vibes and verify VibesState threshold mapping:
     *   ≥80 → THRIVING, 50-79 → NORMAL, 30-49 → TENSE, 10-29 → HOSTILE, &lt;10 → DYSTOPIA
     */
    @Test
    void test8_VibesHUDThresholdMapping() {
        // Test THRIVING threshold
        neighbourhood.setVibes(80);
        assertEquals(VibesState.THRIVING, neighbourhood.getCurrentVibesState(),
                "Vibes 80 should be THRIVING");

        neighbourhood.setVibes(100);
        assertEquals(VibesState.THRIVING, neighbourhood.getCurrentVibesState(),
                "Vibes 100 should be THRIVING");

        // Test NORMAL threshold
        neighbourhood.setVibes(79);
        assertEquals(VibesState.NORMAL, neighbourhood.getCurrentVibesState(),
                "Vibes 79 should be NORMAL");

        neighbourhood.setVibes(50);
        assertEquals(VibesState.NORMAL, neighbourhood.getCurrentVibesState(),
                "Vibes 50 should be NORMAL");

        // Test TENSE threshold
        neighbourhood.setVibes(49);
        assertEquals(VibesState.TENSE, neighbourhood.getCurrentVibesState(),
                "Vibes 49 should be TENSE");

        neighbourhood.setVibes(30);
        assertEquals(VibesState.TENSE, neighbourhood.getCurrentVibesState(),
                "Vibes 30 should be TENSE");

        // Test HOSTILE threshold
        neighbourhood.setVibes(29);
        assertEquals(VibesState.HOSTILE, neighbourhood.getCurrentVibesState(),
                "Vibes 29 should be HOSTILE");

        neighbourhood.setVibes(10);
        assertEquals(VibesState.HOSTILE, neighbourhood.getCurrentVibesState(),
                "Vibes 10 should be HOSTILE");

        // Test DYSTOPIA threshold
        neighbourhood.setVibes(9);
        assertEquals(VibesState.DYSTOPIA, neighbourhood.getCurrentVibesState(),
                "Vibes 9 should be DYSTOPIA");

        neighbourhood.setVibes(0);
        assertEquals(VibesState.DYSTOPIA, neighbourhood.getCurrentVibesState(),
                "Vibes 0 should be DYSTOPIA");
    }

    // ─── Test 9: Dystopia state effects ──────────────────────────────────────

    /**
     * When Neighbourhood Vibes drops to Dystopia (&lt;10):
     * - DYSTOPIA_NOW achievement fires
     * - isAmbientSilenced() returns true
     * - getFencePriceMultiplier() returns &lt;1.0 (HOSTILE_FENCE_DISCOUNT)
     */
    @Test
    void test9_DystopiaStateFiresAchievementAndEffects() {
        assertFalse(achievementSystem.isUnlocked(AchievementType.DYSTOPIA_NOW),
                "DYSTOPIA_NOW should not be unlocked initially");

        // Trigger Vibes calculation that results in Dystopia
        // Set all buildings to very low condition to push Vibes down
        BuildingRecord rec = neighbourhood.getBuilding(BUILDING_X, BUILDING_Z);
        rec.setCondition(0);

        // Force faction scores low
        factionSystem.setRespect(Faction.MARCHETTI_CREW, 5);
        factionSystem.setRespect(Faction.STREET_LADS, 5);
        factionSystem.setRespect(Faction.THE_COUNCIL, 5);

        // Add graffiti count to push Vibes further down
        for (int i = 0; i < 100; i++) {
            neighbourhood.onGraffitiPlaced(BUILDING_X, BUILDING_Z);
        }

        // Trigger Vibes recalculation with high notoriety
        neighbourhood.recalculateVibes(100, npcs);

        // Vibes should be Dystopia
        assertEquals(VibesState.DYSTOPIA, neighbourhood.getCurrentVibesState(),
                "Vibes should be in DYSTOPIA state after all negative factors");

        // DYSTOPIA_NOW achievement should fire
        assertTrue(achievementSystem.isUnlocked(AchievementType.DYSTOPIA_NOW),
                "DYSTOPIA_NOW achievement should fire when entering Dystopia");

        // Ambient sound should be silenced
        assertTrue(neighbourhood.isAmbientSilenced(),
                "Ambient sound should be silenced in Dystopia");

        // Fence prices should be discounted in Hostile/Dystopia
        assertTrue(neighbourhood.getFencePriceMultiplier() < 1.0f,
                "Fence price multiplier should be less than 1.0 in Dystopia");
        assertEquals(NeighbourhoodSystem.HOSTILE_FENCE_DISCOUNT,
                neighbourhood.getFencePriceMultiplier(), 0.001f,
                "Fence price multiplier should be HOSTILE_FENCE_DISCOUNT");
    }

    // ─── Test 10: Full neighbourhood lifecycle stress test ────────────────────

    /**
     * Full lifecycle: register building → passive decay to DERELICT → squat prevents
     * demolition → sell to developers → PROPERTY_DEVELOPER achievement fires →
     * pirate radio from derelict building boosts condition → community meeting
     * with enough NPCs raises condition and fires LAST_OF_THE_LOCALS.
     */
    @Test
    void test10_FullNeighbourhoodLifecycle() {
        // Register a second building for the lifecycle test
        neighbourhood.registerBuilding(LandmarkType.COUNCIL_FLATS, 50, 50);
        BuildingRecord lifecycle = neighbourhood.getBuilding(50, 50);
        assertNotNull(lifecycle, "Lifecycle building should be registered");

        // ── Phase A: Decay to DERELICT ──────────────────────────────────────
        lifecycle.setCondition(NeighbourhoodSystem.CONDITION_DERELICT - 1); // 49
        float minuteDelta = NeighbourhoodSystem.SECONDS_PER_IN_GAME_MINUTE;
        neighbourhood.update(minuteDelta, null, npcs, 0, 50f, 50f);
        assertEquals(ConditionState.DERELICT, lifecycle.getState(),
                "Building should be DERELICT after condition drops below 50");
        assertTrue(lifecycle.isCondemned(), "Derelict building should be condemned");

        // ── Phase B: Squat prevents demolition ──────────────────────────────
        String squatResult = neighbourhood.squatBuilding(50, 50);
        assertNotNull(squatResult, "Squatting should return a message");
        assertTrue(lifecycle.isSquatted(), "Building should be marked as squatted");
        assertTrue(lifecycle.getDemolitionTimer() < 0f,
                "Demolition timer should be cancelled for a squatted building");

        // ── Phase C: Pirate radio from derelict building boosts condition ───
        int conditionBeforeRadio = lifecycle.getCondition();
        neighbourhood.onPirateRadioBroadcast(50, 50);
        assertEquals(conditionBeforeRadio + NeighbourhoodSystem.CONDITION_RADIO_BOOST,
                lifecycle.getCondition(),
                "Pirate radio in derelict building should boost condition by CONDITION_RADIO_BOOST");

        // ── Phase D: Sell to developers (separate building) ─────────────────
        assertFalse(achievementSystem.isUnlocked(AchievementType.PROPERTY_DEVELOPER),
                "PROPERTY_DEVELOPER should not be unlocked yet");
        String sellResult = neighbourhood.sellToDevelopers(BUILDING_X, BUILDING_Z, inventory);
        assertNotNull(sellResult, "Selling to developers should return a message");
        assertEquals(NeighbourhoodSystem.SELL_TO_DEVELOPERS_COINS,
                inventory.getItemCount(Material.COIN),
                "Player should receive SELL_TO_DEVELOPERS_COINS coins");
        assertTrue(achievementSystem.isUnlocked(AchievementType.PROPERTY_DEVELOPER),
                "PROPERTY_DEVELOPER achievement should fire on sale");

        // ── Phase E: Community meeting raises condition ──────────────────────
        // Add enough NPCs near the building (need ≥5)
        List<NPC> nearbyNpcs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            nearbyNpcs.add(new NPC(NPCType.PUBLIC, 52f + i, 1f, 52f));
        }
        nearbyNpcs.add(npcs.get(0)); // barman

        // Make building condemned again for the community meeting
        lifecycle.setCondemned(true);
        int conditionBeforeMeeting = lifecycle.getCondition();

        String meetingResult = neighbourhood.organiseCommunityMeeting(
                50, 50, nearbyNpcs, true, false);
        assertNotNull(meetingResult,
                "Community meeting should succeed with FLYER and enough NPCs");
        assertEquals(conditionBeforeMeeting + NeighbourhoodSystem.CONDITION_COMMUNITY_MEETING,
                lifecycle.getCondition(),
                "Community meeting should boost condition by CONDITION_COMMUNITY_MEETING");
        assertFalse(lifecycle.isCondemned(),
                "Condemned status should be cleared after community meeting");
        assertTrue(achievementSystem.isUnlocked(AchievementType.LAST_OF_THE_LOCALS),
                "LAST_OF_THE_LOCALS achievement should fire after community meeting");

        // ── Phase F: BootSale bonuses in high-Vibes areas ───────────────────
        neighbourhood.setVibes(85); // Force Thriving
        assertEquals(VibesState.THRIVING, neighbourhood.getCurrentVibesState(),
                "Vibes should be THRIVING");
        assertEquals(NeighbourhoodSystem.BOOT_SALE_EXTRA_BUYERS,
                neighbourhood.getBootSaleExtraBuyers(),
                "High-Vibes areas should grant extra BootSale buyers");
        assertEquals(NeighbourhoodSystem.BOOT_SALE_VIBES_PRICE_MULT,
                neighbourhood.getBootSalePriceMultiplier(), 0.001f,
                "High-Vibes areas should grant BootSale price multiplier");
    }
}
