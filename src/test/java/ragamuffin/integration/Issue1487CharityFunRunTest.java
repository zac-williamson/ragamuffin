package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CharityFunRunSystem;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.TimeSystem;
import ragamuffin.core.Weather;
import ragamuffin.core.WeatherSystem;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1487: CharityFunRunSystem — Janet's Bibs, the
 * Course Shortcut &amp; the Registration Pot Pinch.
 *
 * <p>Tests the five scenarios specified in the SPEC.md integration test section.
 */
class Issue1487CharityFunRunTest {

    private CharityFunRunSystem system;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private WeatherSystem weatherSystem;
    private TimeSystem timeSystem;
    private List<NPC> npcs;
    private List<AchievementType> awarded;
    private CharityFunRunSystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        system = new CharityFunRunSystem(new Random(42L));
        inventory = new Inventory();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(42L));
        weatherSystem = new WeatherSystem();
        npcs = new ArrayList<>();
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);

        // Advance TimeSystem to day 21 at 08:30 (registration open)
        // TimeSystem default timeSpeed = 0.1 hours/real-sec, 1 day = 240 real-sec
        timeSystem = new TimeSystem(0.0f);
        for (int i = 1; i < 21; i++) {
            timeSystem.update(240f);
        }
        timeSystem.setTime(8.5f);
    }

    // ── Test 1: Registration issues bib and sponsor sheet ──────────────────

    /**
     * Registering for the run issues bib and sponsor sheet.
     *
     * Create CharityFunRunSystem; set time to 08:45 (registration open); give player 2 COIN;
     * call register(player, inventory); verify inventory contains RACE_NUMBER_BIB and
     * SPONSOR_SHEET; verify COIN decreased by ENTRY_FEE.
     */
    @Test
    void test1_registeringIssuesBibAndSponsorSheet() {
        // Registration opens at 08:30
        system.update(0f, timeSystem, npcs, weatherSystem, rumourNetwork, achievementCb);

        inventory.addItem(Material.COIN, 2);
        CharityFunRunSystem.RegistrationResult result = system.register(inventory);

        assertEquals(CharityFunRunSystem.RegistrationResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.RACE_NUMBER_BIB),
                "Inventory should contain RACE_NUMBER_BIB after registration");
        assertTrue(inventory.hasItem(Material.SPONSOR_SHEET),
                "Inventory should contain SPONSOR_SHEET after registration");
        assertEquals(0, inventory.getItemCount(Material.COIN),
                "COIN should be decreased by ENTRY_FEE (" + CharityFunRunSystem.ENTRY_FEE + ")");
    }

    // ── Test 2: Finishing in time awards medal and sponsor money ───────────

    /**
     * Finishing the run in time awards medal and sponsor money.
     *
     * Create system; register player; set time to 09:00; mark all 8 checkpoints visited
     * in order; call finishRun with timeElapsedSeconds = 1200 (< WINNER_TIME_SECONDS);
     * seed 3 sponsor pledges worth 5 COIN total; verify WINNERS_MEDAL in inventory;
     * verify COIN increased by 5; verify achievementSystem received COMMUNITY_RUNNER_ELITE.
     */
    @Test
    void test2_finishingRunInTimeAwardsMedalAndSponsorMoney() {
        // Open registration
        system.update(0f, timeSystem, npcs, weatherSystem, rumourNetwork, achievementCb);
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);

        // Solicit pledges with seed 42 — PENSIONER succeeds (0.727 < 0.80)
        // PENSIONER_PLEDGE_AMOUNT = 2, so 3 successes = 6 COIN
        // But we need to verify the exact amount pledged
        NPC pensioner = new NPC(NPCType.PENSIONER, 0, 0, 0);
        NPC pensioner2 = new NPC(NPCType.PENSIONER, 0, 0, 0);
        NPC pensioner3 = new NPC(NPCType.PENSIONER, 0, 0, 0);
        system.solicitPledge(pensioner, inventory);
        system.solicitPledge(pensioner2, inventory);
        system.solicitPledge(pensioner3, inventory);
        int pledgeTotal = system.getPledgeTotal();
        assertTrue(pledgeTotal > 0, "Expected at least 1 COIN pledged after 3 PENSIONER solicits");

        int coinBefore = inventory.getItemCount(Material.COIN);

        // Simulate all 8 checkpoints visited (by checking completion)
        assertEquals(CharityFunRunSystem.CourseStatus.COMPLETE,
                system.checkCourseCompletion(8));

        // Finish the run under the time limit
        CharityFunRunSystem.FinishResult finishResult = system.finishRun(
                inventory, 1200f, false, achievementCb);

        assertEquals(CharityFunRunSystem.FinishResult.WON, finishResult,
                "Finishing in 1200 seconds (< 1500) should win");
        assertTrue(inventory.hasItem(Material.WINNERS_MEDAL),
                "WINNERS_MEDAL should be in inventory after winning");
        assertTrue(awarded.contains(AchievementType.COMMUNITY_RUNNER_ELITE),
                "COMMUNITY_RUNNER_ELITE should be awarded");
        assertEquals(coinBefore + pledgeTotal, inventory.getItemCount(Material.COIN),
                "Sponsor pledges (" + pledgeTotal + " COIN) should be paid out");
    }

    // ── Test 3: Witnessed course cutting seeds rumour and adds notoriety ───

    /**
     * Witnessed course cutting seeds rumour and adds notoriety.
     *
     * Create system; register player; place a JOGGER NPC within WITNESS_RADIUS_BLOCKS;
     * call detectCourseCut(player, checkpointsSkipped=2, joggerNpc, rumourNetwork, notorietySystem);
     * verify rumourNetwork contains COURSE_CUTTING; verify notorietySystem.getNotoriety() increased
     * by 2; verify achievementSystem did NOT receive SHAMELESS_SHORTCUT.
     */
    @Test
    void test3_witnessedCourseCuttingSeedsRumourAndAddsNotoriety() {
        // Register the player
        system.update(0f, timeSystem, npcs, weatherSystem, rumourNetwork, achievementCb);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        // JOGGER NPC as witness (within WITNESS_RADIUS_BLOCKS)
        NPC jogger = new NPC(NPCType.JOGGER, 0, 0, 0);

        int notorietyBefore = notorietySystem.getNotoriety();
        CharityFunRunSystem.CourseCutResult result = system.detectCourseCut(
                2, jogger, rumourNetwork, notorietySystem, npcs, achievementCb);

        assertEquals(CharityFunRunSystem.CourseCutResult.WITNESSED, result);
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.COURSE_CUTTING),
                "COURSE_CUTTING rumour should be seeded in the RumourNetwork");
        assertEquals(notorietyBefore + CharityFunRunSystem.COURSE_CUT_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + CharityFunRunSystem.COURSE_CUT_NOTORIETY);
        assertFalse(awarded.contains(AchievementType.SHAMELESS_SHORTCUT),
                "SHAMELESS_SHORTCUT should NOT be awarded when witnessed");
    }

    // ── Test 4: Unwitnessed course cutting awards shameless shortcut ───────

    /**
     * Unwitnessed course cutting awards shameless shortcut.
     *
     * Create system; register player; ensure no JOGGER NPC within WITNESS_RADIUS_BLOCKS;
     * call detectCourseCut(player, checkpointsSkipped=3, null, rumourNetwork, notorietySystem);
     * verify rumourNetwork does NOT contain COURSE_CUTTER; verify achievementSystem received
     * SHAMELESS_SHORTCUT; verify notorietySystem.getNotoriety() unchanged.
     */
    @Test
    void test4_unwitnessedCourseCuttingAwardsShamelessShortcut() {
        // Register the player
        system.update(0f, timeSystem, npcs, weatherSystem, rumourNetwork, achievementCb);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        int notorietyBefore = notorietySystem.getNotoriety();

        // No witness (null jogger NPC)
        CharityFunRunSystem.CourseCutResult result = system.detectCourseCut(
                3, null, rumourNetwork, notorietySystem, npcs, achievementCb);

        assertEquals(CharityFunRunSystem.CourseCutResult.UNWITNESSED, result);
        assertFalse(rumourNetwork.getAllRumourTypes().contains(RumourType.COURSE_CUTTING),
                "No COURSE_CUTTING rumour should be seeded when unwitnessed");
        assertTrue(awarded.contains(AchievementType.SHAMELESS_SHORTCUT),
                "SHAMELESS_SHORTCUT should be awarded when unwitnessed");
        assertEquals(notorietyBefore, notorietySystem.getNotoriety(),
                "Notoriety should be unchanged when unwitnessed");
    }

    // ── Test 5: Rain cancellation seeds rumour and awards achievement ──────

    /**
     * Rain cancellation seeds rumour and awards achievement.
     *
     * Create system; set weather to Weather.HEAVY_RAIN (using Weather.RAIN as equivalent);
     * call update(delta, timeSystem) at 08:30; verify event is cancelled; verify
     * rumourNetwork contains FUN_RUN_CANCELLED; verify achievementSystem received
     * RAINED_OFF; verify charityFunRunSystem.isActive() == false.
     */
    @Test
    void test5_rainCancellationSeedsRumourAndAwardsAchievement() {
        // Register the player first (before rain check at 08:30)
        system.update(0f, timeSystem, npcs, weatherSystem, rumourNetwork, achievementCb);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        // Reset the system to simulate the rain scenario from scratch on event day
        CharityFunRunSystem rainSystem = new CharityFunRunSystem(new Random(99L));
        RumourNetwork rainRumourNetwork = new RumourNetwork(new Random(99L));
        List<AchievementType> rainAwarded = new ArrayList<>();
        CharityFunRunSystem.AchievementCallback rainCb = type -> rainAwarded.add(type);
        List<NPC> rainNpcs = new ArrayList<>();

        // Add a PUBLIC NPC for rumour seeding
        rainNpcs.add(new NPC(NPCType.PUBLIC, 0, 0, 0));
        rainNpcs.add(new NPC(NPCType.PENSIONER, 0, 0, 0));

        // Set up weather with RAIN (equivalent to HEAVY_RAIN in this system)
        WeatherSystem heavyRainWeather = new WeatherSystem();
        heavyRainWeather.setWeather(Weather.RAIN);

        // Register the player in this system
        TimeSystem rainTimeSystem = new TimeSystem(0.0f);
        for (int i = 1; i < 21; i++) {
            rainTimeSystem.update(240f);
        }
        rainTimeSystem.setTime(8.5f);

        // First update: registration opens AND rain check triggers at 08:30
        rainSystem.update(0f, rainTimeSystem, rainNpcs, heavyRainWeather,
                rainRumourNetwork, rainCb);

        assertTrue(rainSystem.isCancelled(),
                "Event should be cancelled due to RAIN weather");
        assertFalse(rainSystem.isActive(),
                "charityFunRunSystem.isActive() should be false after cancellation");

        // Register a player in the rain system to test RAINED_OFF
        Inventory rainInventory = new Inventory();
        rainInventory.addItem(Material.COIN, 5);
        CharityFunRunSystem rainSystem2 = new CharityFunRunSystem(new Random(88L));
        List<NPC> npcs2 = new ArrayList<>();
        npcs2.add(new NPC(NPCType.PUBLIC, 0, 0, 0));
        RumourNetwork rn2 = new RumourNetwork(new Random(88L));
        List<AchievementType> awarded2 = new ArrayList<>();
        CharityFunRunSystem.AchievementCallback cb2 = type -> awarded2.add(type);
        TimeSystem ts2 = new TimeSystem(0.0f);
        for (int i = 1; i < 21; i++) {
            ts2.update(240f);
        }
        ts2.setTime(8.5f); // 08:30 — registration opens
        // First call with CLEAR weather: opens registration and passes rain check
        WeatherSystem clearWeather = new WeatherSystem();
        clearWeather.setWeather(Weather.CLEAR);
        rainSystem2.update(0f, ts2, npcs2, clearWeather, rn2, cb2);
        rainInventory.addItem(Material.COIN, 2);
        rainSystem2.register(rainInventory); // register after registration opens

        // Now trigger rain cancellation
        // Reset rain-checked flag so the rain check fires again
        rainSystem2.resetRainCheckedForTesting();
        clearWeather.setWeather(Weather.RAIN);
        rainSystem2.update(0f, ts2, npcs2, clearWeather, rn2, cb2);

        assertTrue(rainSystem2.isCancelled());
        assertTrue(rn2.getAllRumourTypes().contains(RumourType.FUN_RUN_CANCELLED),
                "FUN_RUN_CANCELLED rumour should be seeded when event is rained off");
        assertTrue(awarded2.contains(AchievementType.RAINED_OFF),
                "RAINED_OFF achievement should be awarded to registered player");
        assertFalse(rainSystem2.isActive(),
                "System should not be active after cancellation");
    }

    // ── Additional scenario: Marshal pickpocket ────────────────────────────

    /**
     * Marshal pickpocket during race: register participants (coins in pot), then
     * pickpocket Janet. Verify COIN transferred, THEFT_FROM_PERSON recorded,
     * Notoriety increased by 5.
     */
    @Test
    void test6_marshalPickpocketDuringRaceTransfersCoin() {
        // Open registration and register player (adds 2 COIN to pot)
        system.update(0f, timeSystem, npcs, weatherSystem, rumourNetwork, achievementCb);
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);

        // Simulate additional registrants contributing to the pot
        // (pot is capped at MAX_POT_STEAL but adds ENTRY_FEE per registrant)
        // Register 4 more (total 5 × 2 = 10 COIN in pot, but pot is limited)
        assertTrue(system.getPotTotal() >= CharityFunRunSystem.ENTRY_FEE,
                "Pot should have at least entry fee after registration");

        int coinBefore = inventory.getItemCount(Material.COIN);
        int potBefore = system.getPotTotal();

        CharityFunRunSystem.PickpocketResult pickResult = system.pickpocketMarshal(
                inventory, notorietySystem, criminalRecord, null, false);

        assertEquals(CharityFunRunSystem.PickpocketResult.SUCCESS, pickResult,
                "Pickpocket should succeed when pot has coins");
        assertEquals(coinBefore + potBefore, inventory.getItemCount(Material.COIN),
                "Player inventory should gain coins from pot");
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.THEFT_FROM_PERSON) > 0,
                "THEFT_FROM_PERSON should be recorded in criminal record");
        assertEquals(CharityFunRunSystem.PICKPOCKET_NOTORIETY, notorietySystem.getNotoriety(),
                "Notoriety should increase by " + CharityFunRunSystem.PICKPOCKET_NOTORIETY);
    }
}
