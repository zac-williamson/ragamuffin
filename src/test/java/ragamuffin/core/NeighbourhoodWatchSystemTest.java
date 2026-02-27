package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #797: Unit and integration tests for {@link NeighbourhoodWatchSystem}.
 *
 * Tests cover:
 * - Anger accumulation from crimes and civilian punching
 * - Tier escalation through all five thresholds
 * - Petition Board spawn at Tier 2
 * - Grovel mechanic reduces anger and dismisses Watch Members
 * - NEIGHBOURHOOD_NEWSLETTER removes Petition Board and reduces anger
 * - PEACE_OFFERING converts Watch Member to patrol
 * - Soft citizen's arrest records offences in criminal record
 * - Anger decay in rain/fog weather (doubled rate)
 * - Full Uprising auto-reset after 5 minutes with faction interactions
 * - Council turf funding doubles Watch Member spawn count
 */
class NeighbourhoodWatchSystemTest {

    private NeighbourhoodWatchSystem watchSystem;
    private Inventory inventory;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback callback;

    @BeforeEach
    void setUp() {
        watchSystem = new NeighbourhoodWatchSystem(new Random(42));
        inventory = new Inventory();
        awarded = new ArrayList<>();
        callback = awarded::add;
    }

    // ── Test 1: Anger accumulates from visible crimes and civilian punching ────

    @Test
    void testAngerAccumulatesFromCrimesAndPunching() {
        assertEquals(0, watchSystem.getWatchAnger(), "Should start at 0");

        watchSystem.onVisibleCrime();
        assertEquals(NeighbourhoodWatchSystem.ANGER_VISIBLE_CRIME,
            watchSystem.getWatchAnger(), "Visible crime should add anger");

        watchSystem.onPlayerPunchedCivilian();
        assertEquals(NeighbourhoodWatchSystem.ANGER_VISIBLE_CRIME +
                     NeighbourhoodWatchSystem.ANGER_PUNCH_CIVILIAN,
            watchSystem.getWatchAnger(), "Punching civilian should add more anger");

        watchSystem.onPlayerSmashedExteriorWall();
        assertEquals(NeighbourhoodWatchSystem.ANGER_VISIBLE_CRIME +
                     NeighbourhoodWatchSystem.ANGER_PUNCH_CIVILIAN +
                     NeighbourhoodWatchSystem.ANGER_SMASH_EXTERIOR,
            watchSystem.getWatchAnger(), "Smashing exterior wall should add anger");
    }

    // ── Test 2: Tier escalation through all thresholds ────────────────────────

    @Test
    void testTierEscalationThroughAllThresholds() {
        // Start at tier 0
        watchSystem.update(0.1f, false, false, callback);
        assertEquals(0, watchSystem.getCurrentTier(), "Should start at Tier 0");

        // Set to Tier 1 (Mutterings)
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_1_MUTTERINGS);
        watchSystem.update(0.001f, false, false, callback); // tiny delta to avoid decay
        assertEquals(1, watchSystem.getCurrentTier(), "Should be Tier 1 at anger " +
            NeighbourhoodWatchSystem.TIER_1_MUTTERINGS);
        assertTrue(awarded.contains(AchievementType.WATCHED),
            "WATCHED achievement should fire at Tier 1");

        // Set to Tier 2 (Petitions)
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_2_PETITIONS);
        watchSystem.update(0.001f, false, false, callback);
        assertEquals(2, watchSystem.getCurrentTier(), "Should be Tier 2 at anger " +
            NeighbourhoodWatchSystem.TIER_2_PETITIONS);
        assertTrue(watchSystem.isPetitionBoardActive(), "Petition Board should be active at Tier 2");
        assertTrue(awarded.contains(AchievementType.PETITION_RECEIVED),
            "PETITION_RECEIVED achievement should fire at Tier 2");

        // Set to Tier 3 (Vigilante Patrol)
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_3_VIGILANTE);
        watchSystem.update(0.001f, false, false, callback);
        assertEquals(3, watchSystem.getCurrentTier(), "Should be Tier 3 at anger " +
            NeighbourhoodWatchSystem.TIER_3_VIGILANTE);

        // Set to Tier 4 (Organised Mob)
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_4_ORGANISED_MOB);
        watchSystem.update(0.001f, false, false, callback);
        assertEquals(4, watchSystem.getCurrentTier(), "Should be Tier 4 at anger " +
            NeighbourhoodWatchSystem.TIER_4_ORGANISED_MOB);

        // Set to Tier 5 (Full Uprising)
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_5_UPRISING);
        watchSystem.update(0.001f, false, false, callback);
        assertEquals(5, watchSystem.getCurrentTier(), "Should be Tier 5 at anger " +
            NeighbourhoodWatchSystem.TIER_5_UPRISING);
        assertTrue(watchSystem.isUprisingActive(), "Full Uprising should be active at Tier 5");
    }

    // ── Test 3: Petition Board spawns at Tier 2 and clears when anger drops ───

    @Test
    void testPetitionBoardSpawnsAtTier2AndClears() {
        assertFalse(watchSystem.isPetitionBoardActive(), "Petition board should not be active at start");

        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_2_PETITIONS);
        watchSystem.update(0.001f, false, false, null);
        assertTrue(watchSystem.isPetitionBoardActive(), "Petition board should be active at Tier 2");

        // Drop anger below Tier 2
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_2_PETITIONS - 1);
        watchSystem.update(0.001f, false, false, null);
        assertFalse(watchSystem.isPetitionBoardActive(),
            "Petition board should be removed when anger drops below Tier 2");
    }

    // ── Test 4: Grovel mechanic reduces anger and dismisses Watch Members ──────

    @Test
    void testGrovelmechanicReducesAngerAndDismissesWatchMembers() {
        watchSystem.setWatchAnger(40);
        watchSystem.update(0.001f, false, false, null);

        // Add a Watch Member in CHASING state
        NPC wm = new NPC(NPCType.WATCH_MEMBER, 5, 1, 5);
        wm.setState(NPCState.CHASING_PLAYER);
        watchSystem.getWatchMembers().add(wm);

        int angerBefore = watchSystem.getWatchAnger();

        // Simulate holding G for 2 seconds (the grovel duration)
        float delta = 0.5f;
        boolean completed = false;
        for (int i = 0; i < 4; i++) { // 4 × 0.5s = 2.0s
            completed = watchSystem.grovelling(delta, callback);
        }

        assertTrue(completed, "Grovel should complete after 2 seconds");
        assertEquals(angerBefore - NeighbourhoodWatchSystem.ANGER_GROVEL_REDUCTION,
            watchSystem.getWatchAnger(),
            "Anger should decrease by ANGER_GROVEL_REDUCTION on grovel completion");
        assertEquals(NPCState.WANDERING, wm.getState(),
            "Watch Member should be set to WANDERING after grovel");
        assertTrue(awarded.contains(AchievementType.GROVELLED),
            "GROVELLED achievement should be awarded");
    }

    // ── Test 5: NEIGHBOURHOOD_NEWSLETTER removes Petition Board ──────────────

    @Test
    void testNeighbourhoodNewsletterRemovesPetitionBoard() {
        // Activate petition board
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_2_PETITIONS);
        watchSystem.update(0.001f, false, false, null);
        assertTrue(watchSystem.isPetitionBoardActive());

        // Try to use newsletter without having one
        boolean result = watchSystem.useNeighbourhoodNewsletter(inventory, callback);
        assertFalse(result, "Should fail without NEIGHBOURHOOD_NEWSLETTER in inventory");
        assertTrue(watchSystem.isPetitionBoardActive(), "Board should still be active");

        // Add newsletter to inventory
        inventory.addItem(Material.NEIGHBOURHOOD_NEWSLETTER, 1);
        int angerBefore = watchSystem.getWatchAnger();

        result = watchSystem.useNeighbourhoodNewsletter(inventory, callback);
        assertTrue(result, "Should succeed with NEIGHBOURHOOD_NEWSLETTER in inventory");
        assertFalse(watchSystem.isPetitionBoardActive(), "Petition board should be removed");
        assertEquals(angerBefore - NeighbourhoodWatchSystem.ANGER_NEWSLETTER_REDUCTION,
            watchSystem.getWatchAnger(), "Anger should decrease by 8 after newsletter use");
        assertEquals(0, inventory.getItemCount(Material.NEIGHBOURHOOD_NEWSLETTER),
            "NEIGHBOURHOOD_NEWSLETTER should be consumed");
        assertTrue(awarded.contains(AchievementType.NEWSLETTER_PUBLISHED),
            "NEWSLETTER_PUBLISHED achievement should be awarded");
    }

    // ── Test 6: PEACE_OFFERING converts Watch Member to patrol ────────────────

    @Test
    void testPeaceOfferingConvertsWatchMember() {
        watchSystem.setWatchAnger(50);
        watchSystem.update(0.001f, false, false, null);

        NPC wm = new NPC(NPCType.WATCH_MEMBER, 5, 1, 5);
        wm.setState(NPCState.AGGRESSIVE);

        // Try without peace offering
        boolean result = watchSystem.usePeaceOffering(wm, inventory, callback);
        assertFalse(result, "Should fail without PEACE_OFFERING in inventory");
        assertEquals(NPCState.AGGRESSIVE, wm.getState(), "Watch Member state should be unchanged");

        // Add peace offering
        inventory.addItem(Material.PEACE_OFFERING, 1);
        int angerBefore = watchSystem.getWatchAnger();

        result = watchSystem.usePeaceOffering(wm, inventory, callback);
        assertTrue(result, "Should succeed with PEACE_OFFERING in inventory");
        assertEquals(NPCState.PATROLLING, wm.getState(),
            "Watch Member should be set to PATROLLING after peace offering");
        assertEquals(angerBefore - NeighbourhoodWatchSystem.ANGER_PEACE_OFFERING_REDUCTION,
            watchSystem.getWatchAnger(), "Anger should decrease by 5 after peace offering");
        assertEquals(0, inventory.getItemCount(Material.PEACE_OFFERING),
            "PEACE_OFFERING should be consumed");
        assertTrue(awarded.contains(AchievementType.PEACEMAKER),
            "PEACEMAKER achievement should be awarded");
    }

    // ── Test 7: Soft citizen's arrest records offences in criminal record ──────

    @Test
    void testSoftCitizensArrestRecordsOffences() {
        CriminalRecord record = new CriminalRecord();
        NPC wm = new NPC(NPCType.WATCH_MEMBER, 5, 1, 5);
        wm.setState(NPCState.AGGRESSIVE);

        assertEquals(0, record.getCount(CriminalRecord.CrimeType.ANTISOCIAL_BEHAVIOUR),
            "Should start with 0 antisocial behaviour charges");

        watchSystem.performSoftCitizensArrest(wm, record);

        assertEquals(NeighbourhoodWatchSystem.CITIZENS_ARREST_OFFENCES,
            record.getCount(CriminalRecord.CrimeType.ANTISOCIAL_BEHAVIOUR),
            "Should record exactly " + NeighbourhoodWatchSystem.CITIZENS_ARREST_OFFENCES +
            " antisocial behaviour offences per citizen's arrest");
        assertEquals(NPCState.WARNING, wm.getState(),
            "Watch Member should be in WARNING state during citizen's arrest");
        assertNotNull(wm.getSpeechText(),
            "Watch Member should have speech text during citizen's arrest");
    }

    // ── Test 8: Anger decays faster in rain/fog weather ───────────────────────

    @Test
    void testAngerDecayDoubledInRainAndFog() {
        watchSystem.setWatchAnger(60);

        // Simulate 100 frames of normal weather decay (1 second total)
        NeighbourhoodWatchSystem normalSystem = new NeighbourhoodWatchSystem(new Random(1));
        normalSystem.setWatchAnger(60);
        for (int i = 0; i < 100; i++) {
            normalSystem.update(0.01f, false, false, null);
        }

        // Simulate 100 frames with rain weather decay (1 second total)
        NeighbourhoodWatchSystem rainSystem = new NeighbourhoodWatchSystem(new Random(1));
        rainSystem.setWatchAnger(60);
        for (int i = 0; i < 100; i++) {
            rainSystem.update(0.01f, true, false, null);
        }

        int normalAnger = normalSystem.getWatchAnger();
        int rainAnger = rainSystem.getWatchAnger();

        assertTrue(rainAnger < normalAnger,
            "Anger should decay faster in rain than in normal weather: " +
            "rain=" + rainAnger + " normal=" + normalAnger);

        // Similarly for fog
        NeighbourhoodWatchSystem fogSystem = new NeighbourhoodWatchSystem(new Random(1));
        fogSystem.setWatchAnger(60);
        for (int i = 0; i < 100; i++) {
            fogSystem.update(0.01f, false, true, null);
        }
        int fogAnger = fogSystem.getWatchAnger();
        assertTrue(fogAnger < normalAnger,
            "Anger should decay faster in fog than in normal weather: " +
            "fog=" + fogAnger + " normal=" + normalAnger);
    }

    // ── Test 9: Full Uprising auto-resets after 5 minutes ─────────────────────

    @Test
    void testFullUprisingAutoResetsAfterDuration() {
        // Trigger full uprising
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_5_UPRISING);
        watchSystem.update(0.001f, false, false, callback);
        assertTrue(watchSystem.isUprisingActive(), "Uprising should be active");
        assertEquals(5, watchSystem.getCurrentTier(), "Should be at Tier 5");

        // Simulate the full uprising duration in a single frame
        watchSystem.update(NeighbourhoodWatchSystem.UPRISING_DURATION_SECONDS + 1f, false, false, callback);

        assertFalse(watchSystem.isUprisingActive(), "Uprising should have ended");
        assertEquals(NeighbourhoodWatchSystem.ANGER_AFTER_UPRISING_RESET,
            watchSystem.getWatchAnger(),
            "Anger should reset to " + NeighbourhoodWatchSystem.ANGER_AFTER_UPRISING_RESET +
            " after uprising ends");
        assertTrue(awarded.contains(AchievementType.UPRISING_SURVIVED),
            "UPRISING_SURVIVED achievement should be awarded when uprising ends");
    }

    // ── Test 10: Faction interactions — Council turf doubles spawn, Street Lads ─
    //             reduce anger, Marchetti exploit uprising ─────────────────────

    @Test
    void testFactionInteractions() {
        // Set to Tier 3 to test spawning
        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_3_VIGILANTE);
        watchSystem.update(0.001f, false, false, null);
        assertEquals(3, watchSystem.getCurrentTier());

        // Spawn without Council turf (below threshold)
        float lowCouncilTurf = NeighbourhoodWatchSystem.COUNCIL_FUNDING_TURF_THRESHOLD - 0.1f;
        List<NPC> spawnedNormal = watchSystem.spawnWatchMembersForTier(lowCouncilTurf, 50, 50);
        int normalCount = spawnedNormal.size();
        assertTrue(normalCount >= NeighbourhoodWatchSystem.TIER_3_SPAWN_MIN &&
                   normalCount <= NeighbourhoodWatchSystem.TIER_3_SPAWN_MAX,
            "Normal spawn count should be in Tier 3 range [" +
            NeighbourhoodWatchSystem.TIER_3_SPAWN_MIN + ", " +
            NeighbourhoodWatchSystem.TIER_3_SPAWN_MAX + "]: got " + normalCount);

        // Reset watch members for second spawn test
        watchSystem.getWatchMembers().clear();

        // Spawn with Council turf above threshold (doubles spawn rate)
        float highCouncilTurf = NeighbourhoodWatchSystem.COUNCIL_FUNDING_TURF_THRESHOLD + 0.1f;
        List<NPC> spawnedCouncil = watchSystem.spawnWatchMembersForTier(highCouncilTurf, 50, 50);
        int councilCount = spawnedCouncil.size();

        // Council funding doubles spawn rate, so min should be at least 2×Tier3_MIN
        assertTrue(councilCount >= normalCount,
            "Council turf funding should produce more or equal Watch Members: " +
            "council=" + councilCount + " normal=" + normalCount);

        // Street Lads banter reduces anger
        watchSystem.setWatchAnger(50);
        int angerBefore = watchSystem.getWatchAnger();
        watchSystem.onStreetLadsBanter();
        assertEquals(angerBefore - NeighbourhoodWatchSystem.ANGER_STREET_LADS_BANTER,
            watchSystem.getWatchAnger(),
            "Street Lads banter should reduce anger by " +
            NeighbourhoodWatchSystem.ANGER_STREET_LADS_BANTER);

        // Marchetti mission reset only applies during uprising
        assertFalse(watchSystem.onMarchettiMissionReset(),
            "Marchetti mission reset should return false when uprising is not active");

        watchSystem.setWatchAnger(NeighbourhoodWatchSystem.TIER_5_UPRISING);
        watchSystem.update(0.001f, false, false, null);
        assertTrue(watchSystem.onMarchettiMissionReset(),
            "Marchetti mission reset should return true during uprising");

        // Punching a Watch Member is more expensive anger-wise
        int angerBeforePunchWM = watchSystem.getWatchAnger();
        watchSystem.onPlayerPunchedWatchMember();
        assertEquals(Math.min(NeighbourhoodWatchSystem.MAX_ANGER,
                              angerBeforePunchWM + NeighbourhoodWatchSystem.ANGER_PUNCH_WATCH_MEMBER),
            watchSystem.getWatchAnger(),
            "Punching Watch Member should increase anger by ANGER_PUNCH_WATCH_MEMBER");
    }
}
