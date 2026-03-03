package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CharityFunRunSystem} — Issue #1487.
 *
 * Tests: isEventDay, register, solicitPledge, collectPledges,
 * checkCourseCompletion, checkRainCancellation, pickpocketMarshal,
 * detectCourseCut, finishRun.
 */
class CharityFunRunSystemTest {

    private CharityFunRunSystem system;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private List<AchievementType> awarded;
    private CharityFunRunSystem.AchievementCallback achievementCb;

    @BeforeEach
    void setUp() {
        system = new CharityFunRunSystem(new Random(42L));
        inventory = new Inventory();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);
    }

    // ── isEventDay ─────────────────────────────────────────────────────────

    @Test
    void isEventDay_firstEventDay_registrationHour_returnsTrue() {
        assertTrue(system.isEventDay(21, 8.5f));
    }

    @Test
    void isEventDay_firstEventDay_raceHour_returnsTrue() {
        assertTrue(system.isEventDay(21, 9.0f));
    }

    @Test
    void isEventDay_firstEventDay_afterClose_returnsFalse() {
        assertFalse(system.isEventDay(21, 11.0f));
    }

    @Test
    void isEventDay_secondCycle_day49_returnsTrue() {
        // 21 + 28 = 49
        assertTrue(system.isEventDay(49, 9.0f));
    }

    @Test
    void isEventDay_dayBeforeEvent_returnsFalse() {
        assertFalse(system.isEventDay(20, 9.0f));
    }

    @Test
    void isEventDay_dayAfterEvent_returnsFalse() {
        assertFalse(system.isEventDay(22, 9.0f));
    }

    @Test
    void isEventDay_beforeRegistration_returnsFalse() {
        assertFalse(system.isEventDay(21, 7.0f));
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    void register_sufficientCoins_success() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 5);

        CharityFunRunSystem.RegistrationResult result = system.register(inventory);

        assertEquals(CharityFunRunSystem.RegistrationResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.RACE_NUMBER_BIB));
        assertTrue(inventory.hasItem(Material.SPONSOR_SHEET));
        assertEquals(3, inventory.getItemCount(Material.COIN)); // 5 - 2
    }

    @Test
    void register_exactCoins_success() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);

        CharityFunRunSystem.RegistrationResult result = system.register(inventory);

        assertEquals(CharityFunRunSystem.RegistrationResult.SUCCESS, result);
        assertEquals(0, inventory.getItemCount(Material.COIN));
    }

    @Test
    void register_insufficientCoins_returnsInsufficientFunds() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 1);

        CharityFunRunSystem.RegistrationResult result = system.register(inventory);

        assertEquals(CharityFunRunSystem.RegistrationResult.INSUFFICIENT_FUNDS, result);
        assertFalse(inventory.hasItem(Material.RACE_NUMBER_BIB));
    }

    @Test
    void register_notOpen_returnsNotOpen() {
        inventory.addItem(Material.COIN, 5);

        // Registration not opened (no update called)
        CharityFunRunSystem.RegistrationResult result = system.register(inventory);

        assertEquals(CharityFunRunSystem.RegistrationResult.NOT_OPEN, result);
    }

    @Test
    void register_twice_returnsAlreadyRegistered() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);

        CharityFunRunSystem.RegistrationResult result = system.register(inventory);

        assertEquals(CharityFunRunSystem.RegistrationResult.ALREADY_REGISTERED, result);
    }

    // ── solicitPledge ──────────────────────────────────────────────────────

    @Test
    void solicitPledge_chugger_alwaysRefuses() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        NPC chugger = new NPC(NPCType.CHUGGER, 0, 0, 0);
        CharityFunRunSystem.PledgeResult result = system.solicitPledge(chugger, inventory);

        assertEquals(CharityFunRunSystem.PledgeResult.REFUSED, result);
    }

    @Test
    void solicitPledge_notRegistered_returnsNotRegistered() {
        openRegistration(system);
        inventory.addItem(Material.SPONSOR_SHEET, 1);
        NPC pensioner = new NPC(NPCType.PENSIONER, 0, 0, 0);

        CharityFunRunSystem.PledgeResult result = system.solicitPledge(pensioner, inventory);

        assertEquals(CharityFunRunSystem.PledgeResult.NOT_REGISTERED, result);
    }

    @Test
    void solicitPledge_noSheet_returnsNoSheet() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);
        inventory.removeItem(Material.SPONSOR_SHEET, 1);

        NPC pensioner = new NPC(NPCType.PENSIONER, 0, 0, 0);
        CharityFunRunSystem.PledgeResult result = system.solicitPledge(pensioner, inventory);

        assertEquals(CharityFunRunSystem.PledgeResult.NO_SHEET, result);
    }

    @Test
    void solicitPledge_pensioner_validResult() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        NPC pensioner = new NPC(NPCType.PENSIONER, 0, 0, 0);
        CharityFunRunSystem.PledgeResult result = system.solicitPledge(pensioner, inventory);

        // Must return a valid pledge outcome (not a structural error)
        assertTrue(result == CharityFunRunSystem.PledgeResult.SUCCESS
                || result == CharityFunRunSystem.PledgeResult.DECLINED);
    }

    @Test
    void solicitPledge_pensioner_seed42_success() {
        // With seed 42, nextFloat() = ~0.72 which is < PENSIONER_PLEDGE_CHANCE (0.80)
        openRegistration(system); // system already seeded with 42
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        NPC pensioner = new NPC(NPCType.PENSIONER, 0, 0, 0);
        CharityFunRunSystem.PledgeResult result = system.solicitPledge(pensioner, inventory);

        assertEquals(CharityFunRunSystem.PledgeResult.SUCCESS, result);
        assertEquals(CharityFunRunSystem.PENSIONER_PLEDGE_AMOUNT, system.getPledgeTotal());
    }

    // ── checkCourseCompletion ─────────────────────────────────────────────

    @Test
    void checkCourseCompletion_all8_returnsComplete() {
        assertEquals(CharityFunRunSystem.CourseStatus.COMPLETE,
                system.checkCourseCompletion(8));
    }

    @Test
    void checkCourseCompletion_moreThan8_returnsComplete() {
        assertEquals(CharityFunRunSystem.CourseStatus.COMPLETE,
                system.checkCourseCompletion(9));
    }

    @Test
    void checkCourseCompletion_5_returnsIncomplete() {
        assertEquals(CharityFunRunSystem.CourseStatus.INCOMPLETE,
                system.checkCourseCompletion(5));
    }

    @Test
    void checkCourseCompletion_0_returnsIncomplete() {
        assertEquals(CharityFunRunSystem.CourseStatus.INCOMPLETE,
                system.checkCourseCompletion(0));
    }

    // ── checkRainCancellation ─────────────────────────────────────────────

    @Test
    void checkRainCancellation_rain_returnsCancelled() {
        CharityFunRunSystem.EventStatus status = system.checkRainCancellation(Weather.RAIN);
        assertEquals(CharityFunRunSystem.EventStatus.CANCELLED, status);
        assertTrue(system.isCancelled());
    }

    @Test
    void checkRainCancellation_thunderstorm_returnsCancelled() {
        CharityFunRunSystem.EventStatus status = system.checkRainCancellation(Weather.THUNDERSTORM);
        assertEquals(CharityFunRunSystem.EventStatus.CANCELLED, status);
    }

    @Test
    void checkRainCancellation_drizzle_returnsRunning() {
        CharityFunRunSystem.EventStatus status = system.checkRainCancellation(Weather.DRIZZLE);
        assertEquals(CharityFunRunSystem.EventStatus.RUNNING, status);
        assertFalse(system.isCancelled());
    }

    @Test
    void checkRainCancellation_clear_returnsRunning() {
        CharityFunRunSystem.EventStatus status = system.checkRainCancellation(Weather.CLEAR);
        assertEquals(CharityFunRunSystem.EventStatus.RUNNING, status);
    }

    // ── collectPledges ────────────────────────────────────────────────────

    @Test
    void collectPledges_hasFinished_paysOut() {
        // Seed 42 gives PENSIONER pledge success on first solicit
        openRegistration(system);
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);

        // Solicit a PENSIONER (seed 42, first nextFloat ~0.72 < 0.80 = success)
        NPC pensioner = new NPC(NPCType.PENSIONER, 0, 0, 0);
        system.solicitPledge(pensioner, inventory);

        int coinBefore = inventory.getItemCount(Material.COIN);
        int pledgeTotal = system.getPledgeTotal();
        assertTrue(pledgeTotal > 0, "Expected pledges after PENSIONER solicitation with seed 42");

        CharityFunRunSystem.CollectPledgesResult result = system.collectPledges(
                inventory, true, notorietySystem, criminalRecord, null, achievementCb);

        assertEquals(CharityFunRunSystem.CollectPledgesResult.SUCCESS, result);
        assertEquals(coinBefore + pledgeTotal, inventory.getItemCount(Material.COIN));
        assertFalse(inventory.hasItem(Material.SPONSOR_SHEET));
    }

    @Test
    void collectPledges_notFinished_highNotoriety_witnessed_fraudCaught() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);
        notorietySystem.addNotoriety(12, null);

        NPC witness = new NPC(NPCType.JOGGER, 0, 0, 0);
        int coinBefore = inventory.getItemCount(Material.COIN);
        CharityFunRunSystem.CollectPledgesResult result = system.collectPledges(
                inventory, false, notorietySystem, criminalRecord, witness, achievementCb);

        assertEquals(CharityFunRunSystem.CollectPledgesResult.FRAUD_CAUGHT, result);
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.CHARITY_FRAUD) > 0);
        assertEquals(coinBefore, inventory.getItemCount(Material.COIN));
        assertFalse(inventory.hasItem(Material.SPONSOR_SHEET));
    }

    @Test
    void collectPledges_notFinished_lowNotoriety_trusted_paysOut() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);
        // Notoriety stays 0 — Janet trusts them

        CharityFunRunSystem.CollectPledgesResult result = system.collectPledges(
                inventory, false, notorietySystem, criminalRecord, null, achievementCb);

        assertEquals(CharityFunRunSystem.CollectPledgesResult.SUCCESS, result);
    }

    @Test
    void collectPledges_noSheet_returnsNoSheet() {
        // No sponsor sheet in inventory
        CharityFunRunSystem.CollectPledgesResult result = system.collectPledges(
                inventory, true, notorietySystem, criminalRecord, null, achievementCb);

        assertEquals(CharityFunRunSystem.CollectPledgesResult.NO_SHEET, result);
    }

    // ── pickpocketMarshal ─────────────────────────────────────────────────

    @Test
    void pickpocketMarshal_succeeds_transfersCoin() {
        openRegistration(system);
        // Register to add coins to the pot
        inventory.addItem(Material.COIN, 10);
        system.register(inventory);

        int coinBefore = inventory.getItemCount(Material.COIN);
        CharityFunRunSystem.PickpocketResult result = system.pickpocketMarshal(
                inventory, notorietySystem, criminalRecord, null, false);

        assertEquals(CharityFunRunSystem.PickpocketResult.SUCCESS, result);
        assertTrue(inventory.getItemCount(Material.COIN) > coinBefore);
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.THEFT_FROM_PERSON) > 0);
        assertEquals(CharityFunRunSystem.PICKPOCKET_NOTORIETY, notorietySystem.getNotoriety());
    }

    @Test
    void pickpocketMarshal_emptyPot_returnsEmpty() {
        // No registrations, pot is 0
        CharityFunRunSystem.PickpocketResult result = system.pickpocketMarshal(
                inventory, notorietySystem, criminalRecord, null, false);

        assertEquals(CharityFunRunSystem.PickpocketResult.EMPTY, result);
    }

    // ── detectCourseCut ───────────────────────────────────────────────────

    @Test
    void detectCourseCut_witnessed_seedsRumourAndAddsNotoriety() {
        RumourNetwork rumourNetwork = new RumourNetwork(new Random(42L));
        NPC jogger = new NPC(NPCType.JOGGER, 0, 0, 0);

        CharityFunRunSystem.CourseCutResult result = system.detectCourseCut(
                3, jogger, rumourNetwork, notorietySystem, null, achievementCb);

        assertEquals(CharityFunRunSystem.CourseCutResult.WITNESSED, result);
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.COURSE_CUTTING));
        assertEquals(CharityFunRunSystem.COURSE_CUT_NOTORIETY, notorietySystem.getNotoriety());
        assertFalse(awarded.contains(AchievementType.SHAMELESS_SHORTCUT));
    }

    @Test
    void detectCourseCut_unwitnessed_awardsShortcutAndNoRumour() {
        RumourNetwork rumourNetwork = new RumourNetwork(new Random(42L));

        CharityFunRunSystem.CourseCutResult result = system.detectCourseCut(
                2, null, rumourNetwork, notorietySystem, null, achievementCb);

        assertEquals(CharityFunRunSystem.CourseCutResult.UNWITNESSED, result);
        assertTrue(awarded.contains(AchievementType.SHAMELESS_SHORTCUT));
        assertEquals(0, notorietySystem.getNotoriety());
        assertFalse(rumourNetwork.getAllRumourTypes().contains(RumourType.COURSE_CUTTING));
    }

    @Test
    void detectCourseCut_noCut_returnsNoCut() {
        CharityFunRunSystem.CourseCutResult result = system.detectCourseCut(
                0, null, null, notorietySystem, null, achievementCb);

        assertEquals(CharityFunRunSystem.CourseCutResult.NO_CUT, result);
    }

    // ── finishRun ─────────────────────────────────────────────────────────

    @Test
    void finishRun_underTimeLimit_awardsMedalAndElite() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        CharityFunRunSystem.FinishResult result = system.finishRun(
                inventory, 1200f, false, achievementCb);

        assertEquals(CharityFunRunSystem.FinishResult.WON, result);
        assertTrue(inventory.hasItem(Material.WINNERS_MEDAL));
        assertTrue(awarded.contains(AchievementType.COMMUNITY_RUNNER_ELITE));
    }

    @Test
    void finishRun_overTimeLimit_noMedal() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        CharityFunRunSystem.FinishResult result = system.finishRun(
                inventory, 2000f, false, achievementCb);

        assertEquals(CharityFunRunSystem.FinishResult.FINISHED, result);
        assertFalse(inventory.hasItem(Material.WINNERS_MEDAL));
        assertFalse(awarded.contains(AchievementType.COMMUNITY_RUNNER_ELITE));
    }

    @Test
    void finishRun_withDog_awardsWalkiesWinner() {
        openRegistration(system);
        inventory.addItem(Material.COIN, 2);
        system.register(inventory);

        system.finishRun(inventory, 1200f, true, achievementCb);

        assertTrue(awarded.contains(AchievementType.WALKIES_WINNER));
    }

    @Test
    void finishRun_notRegistered_returnsNotRegistered() {
        CharityFunRunSystem.FinishResult result = system.finishRun(
                inventory, 1200f, false, achievementCb);

        assertEquals(CharityFunRunSystem.FinishResult.NOT_REGISTERED, result);
    }

    // ── water cup ─────────────────────────────────────────────────────────

    @Test
    void takeWaterCup_firstTime_addsToInventory() {
        boolean result = system.takeWaterCup(inventory);
        assertTrue(result);
        assertTrue(inventory.hasItem(Material.WATER_CUP));
    }

    @Test
    void takeWaterCup_secondTime_returnsFalse() {
        system.takeWaterCup(inventory);
        boolean result = system.takeWaterCup(inventory);
        assertFalse(result);
        assertEquals(1, inventory.getItemCount(Material.WATER_CUP)); // still just 1
    }

    // ── helper ────────────────────────────────────────────────────────────

    /**
     * Force registration to open by running update at event day/hour.
     * TimeSystem starts at day 1; advance to day 21 (20 full days of 240 real-secs each).
     */
    private static void openRegistration(CharityFunRunSystem sys) {
        // TimeSystem default timeSpeed = 0.1 hours/real-sec. 1 day = 24/0.1 = 240 real-secs.
        TimeSystem ts = new TimeSystem(0.0f);
        for (int i = 1; i < 21; i++) {
            ts.update(240f);
        }
        ts.setTime(8.5f);
        List<NPC> npcs = new ArrayList<>();
        sys.update(0f, ts, npcs, null, null, null);
    }
}
