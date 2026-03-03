package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1399 — WindowCleanerSystem end-to-end scenarios.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>Terry's round is active Mon–Fri 08:30–16:00 only</b>: Verify round starts on Monday 09:00
 *       and ends after advancing past 16:00; verify round is NOT active on Saturday or before 08:30.</li>
 *   <li><b>Employment shift: 8 houses earns WINDOW_LAD achievement</b>: Start a shift with notoriety=0,
 *       simulate 8+ houses via repeated cleanHouse calls (handle pass/fail), verify WINDOW_LAD awarded
 *       when passing 8 houses.</li>
 *   <li><b>Ladder burglary: records crime, adds notoriety and wanted star</b>: Place ladder,
 *       call climbLadder with no witness, verify LADDER_BURGLARY in CriminalRecord, Notoriety +=6,
 *       WantedSystem += 1 star, UP_THE_LADDER achievement awarded.</li>
 *   <li><b>Rival round: grassed result makes Terry hostile; TURF_WAR rumour seeded</b>: Set up system
 *       with a seeded RNG that guarantees GRASSED outcome, verify Terry hostile + rumour seeded.</li>
 *   <li><b>Nosy Neighbour: overhearing 5 gossip exchanges earns CURTAIN_TWITCHER</b>: Force gossip
 *       count to 4, then trigger one more overhear via update; verify CURTAIN_TWITCHER awarded.</li>
 * </ol>
 */
class Issue1399WindowCleanerIntegrationTest {

    private WindowCleanerSystem system;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private HMRCSystem hmrcSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback cb;
    private NPC witnessNpc;

    @BeforeEach
    void setUp() {
        system = new WindowCleanerSystem(new Random(42L));
        inventory = new Inventory(36);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7L));
        hmrcSystem = new HMRCSystem(new Random(13L));

        awarded = new ArrayList<>();
        cb = type -> {
            awarded.add(type);
            achievements.unlock(type);
        };

        system.setNotorietySystem(notorietySystem);
        system.setWantedSystem(wantedSystem);
        system.setCriminalRecord(criminalRecord);
        system.setRumourNetwork(rumourNetwork);
        system.setHmrcSystem(hmrcSystem);

        witnessNpc = new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
    }

    // ── Integration Test 1: Round active Mon–Fri 08:30–16:00 only ─────────────

    /**
     * Verify round lifecycle:
     * <ol>
     *   <li>Monday 09:00 — round starts.</li>
     *   <li>Monday 16:01 — round ends.</li>
     *   <li>Saturday 10:00 — round does not start.</li>
     *   <li>Monday 08:00 (before 08:30) — round does not start.</li>
     * </ol>
     */
    @Test
    void roundActive_monToFri_0830to1600_only() {
        // Monday = 1, 09:00 — should start
        system.update(0.1f, 9.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertTrue(system.isRoundActive(), "Round should be active Monday 09:00");
        assertTrue(system.isLadderPlaced(), "Ladder should be placed on round start");

        // Advance to after 16:00 — round should end
        system.update(0.1f, 16.1f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(system.isRoundActive(), "Round should end after 16:00");
        assertFalse(system.isLadderPlaced(), "Ladder should be removed when round ends");

        // Saturday — round should not start
        WindowCleanerSystem saturdaySystem = new WindowCleanerSystem(new Random(42L));
        saturdaySystem.update(0.1f, 10.0f, 6, 0f, 0f, 0f, 0, null, null);
        assertFalse(saturdaySystem.isRoundActive(), "Round should NOT start on Saturday");

        // Monday before 08:30 — round should not start
        WindowCleanerSystem earlySystem = new WindowCleanerSystem(new Random(42L));
        earlySystem.update(0.1f, 8.0f, 1, 0f, 0f, 0f, 0, null, null);
        assertFalse(earlySystem.isRoundActive(), "Round should NOT start before 08:30");
    }

    // ── Integration Test 2: Employment shift — WINDOW_LAD after 8 houses ──────

    /**
     * Employment shift end-to-end:
     * <ol>
     *   <li>Start a shift with notoriety = 0 (below threshold).</li>
     *   <li>Call cleanHouse repeatedly; on each PASSED result increment counter.</li>
     *   <li>Keep going until 8 houses have been PASSED.</li>
     *   <li>Verify WINDOW_LAD achievement is awarded.</li>
     *   <li>Verify COIN balance increased by at least 8 × HOUSE_WAGE.</li>
     * </ol>
     */
    @Test
    void employmentShift_8HousesEarnsWindowLadAchievement() {
        system.setRoundActiveForTesting(true);
        WindowCleanerSystem.EmploymentResult start = system.startShift(0);
        assertEquals(WindowCleanerSystem.EmploymentResult.STARTED, start);
        assertTrue(system.isShiftActive());

        int coinBefore = inventory.getItemCount(Material.COIN);
        int passed = 0;

        // Run up to 100 attempts to reach 8 passes (probabilistic mini-game)
        for (int attempt = 0; attempt < 100 && passed < WindowCleanerSystem.WINDOW_LAD_THRESHOLD; attempt++) {
            WindowCleanerSystem.HouseCleanResult result = system.cleanHouse(inventory, cb);
            if (result == WindowCleanerSystem.HouseCleanResult.PASSED) {
                passed++;
            }
        }

        assertTrue(passed >= WindowCleanerSystem.WINDOW_LAD_THRESHOLD,
                "Should be able to pass " + WindowCleanerSystem.WINDOW_LAD_THRESHOLD + " houses within 100 attempts");
        assertTrue(awarded.contains(AchievementType.WINDOW_LAD),
                "WINDOW_LAD achievement should be awarded");
        int coinGained = inventory.getItemCount(Material.COIN) - coinBefore;
        assertTrue(coinGained >= WindowCleanerSystem.WINDOW_LAD_THRESHOLD * WindowCleanerSystem.HOUSE_WAGE,
                "COIN should have increased by at least 8 × HOUSE_WAGE, was: " + coinGained);
    }

    // ── Integration Test 3: Ladder burglary end-to-end ────────────────────────

    /**
     * Ladder burglary:
     * <ol>
     *   <li>Place ladder via setLadderPlacedForTesting(true).</li>
     *   <li>Call climbLadder(null, cb) — no witness.</li>
     *   <li>Verify result = CLIMBED.</li>
     *   <li>Verify LADDER_BURGLARY in CriminalRecord (count = 1).</li>
     *   <li>Verify Notoriety = LADDER_BURGLARY_NOTORIETY.</li>
     *   <li>Verify WantedSystem = LADDER_BURGLARY_WANTED_STARS stars.</li>
     *   <li>Verify UP_THE_LADDER achievement awarded.</li>
     * </ol>
     */
    @Test
    void ladderBurglary_recordsCrimeAddsNotorietyAndWantedStar() {
        system.setLadderPlacedForTesting(true);

        WindowCleanerSystem.LadderClimbResult result = system.climbLadder(null, cb);

        assertEquals(WindowCleanerSystem.LadderClimbResult.CLIMBED, result,
                "Result should be CLIMBED with no witness");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.LADDER_BURGLARY),
                "LADDER_BURGLARY should be recorded");
        assertEquals(WindowCleanerSystem.LADDER_BURGLARY_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by LADDER_BURGLARY_NOTORIETY");
        assertEquals(WindowCleanerSystem.LADDER_BURGLARY_WANTED_STARS,
                wantedSystem.getWantedStars(),
                "WantedSystem should gain LADDER_BURGLARY_WANTED_STARS");
        assertTrue(awarded.contains(AchievementType.UP_THE_LADDER),
                "UP_THE_LADDER achievement should be awarded");
    }

    // ── Integration Test 4: Rival round grassed — Terry hostile, TURF_WAR seeded

    /**
     * Rival round "grassed" outcome:
     * <ol>
     *   <li>Use a seeded RNG that guarantees GRASSED (roll >= 0.9).</li>
     *   <li>Give player BUCKET_AND_CHAMOIS.</li>
     *   <li>Call doRivalClean with no Terry NPC nearby.</li>
     *   <li>Verify result = GRASSED.</li>
     *   <li>Verify Terry is now hostile.</li>
     *   <li>Verify TURF_WAR rumour seeded in RumourNetwork.</li>
     * </ol>
     */
    @Test
    void rivalRound_grassed_terryHostileAndTurfWarRumour() {
        // Use a Random that always returns 0.95 to guarantee GRASSED (roll >= 0.9)
        WindowCleanerSystem grassSystem = new WindowCleanerSystem(new Random(0L) {
            @Override
            public float nextFloat() { return 0.95f; }
        });
        grassSystem.setNotorietySystem(notorietySystem);
        grassSystem.setWantedSystem(wantedSystem);
        grassSystem.setCriminalRecord(criminalRecord);
        grassSystem.setRumourNetwork(rumourNetwork);
        grassSystem.setHmrcSystem(hmrcSystem);

        inventory.addItem(Material.BUCKET_AND_CHAMOIS, 1);

        WindowCleanerSystem.RivalCleanResult result = grassSystem.doRivalClean(
                inventory,
                null,        // no Terry nearby
                999f, 999f,  // Terry far away
                0f, 0f,
                witnessNpc, cb);

        assertEquals(WindowCleanerSystem.RivalCleanResult.GRASSED, result,
                "Result should be GRASSED with high RNG roll");
        assertTrue(grassSystem.isTerryHostile(),
                "Terry should be hostile after householder grasses");
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.TURF_WAR),
                "TURF_WAR rumour should be seeded");
    }

    // ── Integration Test 5: CURTAIN_TWITCHER after 5 gossip overheards ────────

    /**
     * Nosy Neighbour:
     * <ol>
     *   <li>Force gossip count to CURTAIN_TWITCHER_THRESHOLD - 1 via test setter.</li>
     *   <li>Manually call the gossip overhear path by forcing the update conditions.</li>
     *   <li>Verify gossipOverheardCount = CURTAIN_TWITCHER_THRESHOLD.</li>
     *   <li>Verify CURTAIN_TWITCHER achievement awarded.</li>
     *   <li>Verify NEIGHBOURHOOD_GOSSIP rumour seeded.</li>
     * </ol>
     */
    @Test
    void noisyNeighbour_5Overheards_earnsCurtainTwitcherAchievement() {
        // Force count to threshold - 1
        system.setGossipOverheardForTesting(WindowCleanerSystem.CURTAIN_TWITCHER_THRESHOLD - 1);
        system.setRoundActiveForTesting(true);

        // Trigger the gossip overhear path:
        // The update method seeds gossip when propertyTimer < 10f and player is within
        // GOSSIP_OVERHEAR_RADIUS and notoriety < GOSSIP_MAX_NOTORIETY.
        // We supply playerX=0, playerZ=0 which gives dist=0 (within radius), notoriety=0, nearbyNpc set.
        // Property timer starts at 0f on round start, so first update satisfies propertyTimer < 10.
        system.update(0.1f, 10.0f, 1, 0f, 0f, 0f, 0, witnessNpc, cb);

        assertTrue(system.getGossipOverheardCount() >= WindowCleanerSystem.CURTAIN_TWITCHER_THRESHOLD,
                "Gossip overheard count should reach threshold after overhear");
        assertTrue(awarded.contains(AchievementType.CURTAIN_TWITCHER),
                "CURTAIN_TWITCHER achievement should be awarded");
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.NEIGHBOURHOOD_GOSSIP),
                "NEIGHBOURHOOD_GOSSIP rumour should be seeded");
    }
}
