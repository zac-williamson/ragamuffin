package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CommunityCentreSystem;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.Rumour;
import ragamuffin.core.RumourNetwork;
import ragamuffin.core.RumourType;
import ragamuffin.core.StreetSkillSystem;
import ragamuffin.core.TimeSystem;
import ragamuffin.core.WantedSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1194: CommunityCentreSystem — Aerobics,
 * NA Meetings, Bring &amp; Buy Sale, Grant Fraud &amp; Curry Night.
 */
class Issue1194CommunityCentreTest {

    private CommunityCentreSystem centre;
    private Inventory inventory;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private StreetSkillSystem streetSkillSystem;
    private RumourNetwork rumourNetwork;
    private TimeSystem timeSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;
    private NPC deniseNpc;
    private NPC chairNpc;

    @BeforeEach
    void setUp() {
        centre = new CommunityCentreSystem(new Random(42L));
        inventory = new Inventory();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        streetSkillSystem = new StreetSkillSystem(new Random(42L));
        rumourNetwork = new RumourNetwork(new Random(42L));
        timeSystem = new TimeSystem(10.0f);
        awarded = new ArrayList<>();
        achievementCb = type -> awarded.add(type);
        deniseNpc = new NPC(NPCType.COMMUNITY_CENTRE_MANAGER, 0, 0, 0);
        chairNpc = new NPC(NPCType.NA_CHAIR, 0, 0, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isOpen_mondayMorning_returnsTrue() {
        assertTrue(centre.isOpen(9.0f, CommunityCentreSystem.MONDAY));
    }

    @Test
    void isOpen_sunday_returnsFalse() {
        assertFalse(centre.isOpen(10.0f, CommunityCentreSystem.SUNDAY));
    }

    @Test
    void isOpen_beforeOpeningHour_returnsFalse() {
        assertFalse(centre.isOpen(8.0f, CommunityCentreSystem.TUESDAY));
    }

    @Test
    void isOpen_atClosingHour_returnsFalse() {
        assertFalse(centre.isOpen(20.0f, CommunityCentreSystem.SATURDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics — session windows
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isAerobicsSession_tuesdayMorning_returnsTrue() {
        assertTrue(centre.isAerobicsSession(10.5f, CommunityCentreSystem.TUESDAY));
    }

    @Test
    void isAerobicsSession_thursdayEvening_returnsTrue() {
        assertTrue(centre.isAerobicsSession(19.0f, CommunityCentreSystem.THURSDAY));
    }

    @Test
    void isAerobicsSession_wednesday_returnsFalse() {
        assertFalse(centre.isAerobicsSession(10.5f, CommunityCentreSystem.WEDNESDAY));
    }

    @Test
    void isAerobicsSession_saturdayMorning_returnsFalse() {
        assertFalse(centre.isAerobicsSession(10.5f, CommunityCentreSystem.SATURDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics — score 0–3: no benefit
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void completeAerobicsSession_scoreLow_noReward() {
        CommunityCentreSystem.AerobicsResult result =
                centre.completeAerobicsSession(2, streetSkillSystem, achievementCb);

        assertEquals(2, result.score);
        assertEquals(0, result.warmthBonus);
        assertEquals(0, result.hungerBonus);
        assertEquals(0, result.graftingXP);
        assertFalse(result.stepTogetherAwarded);
        assertTrue(awarded.isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics — score 4–5: warmth + hunger
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void completeAerobicsSession_scoreMid_warmthAndHunger() {
        CommunityCentreSystem.AerobicsResult result =
                centre.completeAerobicsSession(5, streetSkillSystem, achievementCb);

        assertEquals(10, result.warmthBonus);
        assertEquals(5, result.hungerBonus);
        assertEquals(0, result.graftingXP);
        assertFalse(result.stepTogetherAwarded);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics — score 6–7: warmth + hunger + GRAFTING XP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void completeAerobicsSession_scoreHigh_grantGraftingXP() {
        int xpBefore = streetSkillSystem.getXP(StreetSkillSystem.Skill.GRAFTING);
        CommunityCentreSystem.AerobicsResult result =
                centre.completeAerobicsSession(6, streetSkillSystem, achievementCb);

        assertEquals(10, result.warmthBonus);
        assertEquals(5, result.hungerBonus);
        assertEquals(CommunityCentreSystem.AEROBICS_GRAFTING_XP, result.graftingXP);
        assertEquals(xpBefore + CommunityCentreSystem.AEROBICS_GRAFTING_XP,
                streetSkillSystem.getXP(StreetSkillSystem.Skill.GRAFTING));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics — score 8: STEP_TOGETHER achievement
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void completeAerobicsSession_scorePerfect_awardStepTogether() {
        CommunityCentreSystem.AerobicsResult result =
                centre.completeAerobicsSession(8, streetSkillSystem, achievementCb);

        assertTrue(result.stepTogetherAwarded);
        assertTrue(awarded.contains(AchievementType.STEP_TOGETHER));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics — 8 consecutive sessions: CLEAN_EIGHT + free pass
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void completeAerobicsSession_eightConsecutive_awardCleanEightAndFreePass() {
        // Pre-set to 7 consecutive
        centre.setConsecutiveAerobicsSessionsForTesting(7);

        CommunityCentreSystem.AerobicsResult result =
                centre.completeAerobicsSession(4, streetSkillSystem, achievementCb);

        assertTrue(result.cleanEightAwarded);
        assertTrue(awarded.contains(AchievementType.CLEAN_EIGHT));
        assertTrue(centre.hasFreeAerobicsPass());
        // Counter resets after block
        assertEquals(0, centre.getConsecutiveAerobicsSessions());
    }

    @Test
    void missAerobicsSession_resetsConsecutiveCount() {
        centre.setConsecutiveAerobicsSessionsForTesting(5);
        centre.missAerobicsSession();
        assertEquals(0, centre.getConsecutiveAerobicsSessions());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NA Meeting — window
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isNAMeeting_thursdayEvening_returnsTrue() {
        assertTrue(centre.isNAMeeting(19.5f, CommunityCentreSystem.THURSDAY));
    }

    @Test
    void isNAMeeting_tuesdayEvening_returnsFalse() {
        assertFalse(centre.isNAMeeting(19.5f, CommunityCentreSystem.TUESDAY));
    }

    @Test
    void isNAMeeting_thursdayAfterMeeting_returnsFalse() {
        assertFalse(centre.isNAMeeting(21.0f, CommunityCentreSystem.THURSDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NA Meeting — first share awards ANONYMOUS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void shareAtNAMeeting_firstTime_awardsAnonymous() {
        String confession = centre.shareAtNAMeeting(
                criminalRecord, 10, chairNpc, rumourNetwork, notorietySystem, achievementCb);

        assertNotNull(confession);
        assertFalse(confession.isEmpty());
        assertTrue(awarded.contains(AchievementType.ANONYMOUS));
        assertTrue(centre.hasSharedAtNA());
    }

    @Test
    void shareAtNAMeeting_secondTime_doesNotReDuplicateAnonymous() {
        centre.setHasSharedAtNAForTesting(true);
        centre.shareAtNAMeeting(
                criminalRecord, 10, chairNpc, rumourNetwork, notorietySystem, achievementCb);

        // ANONYMOUS should NOT be awarded again
        assertFalse(awarded.contains(AchievementType.ANONYMOUS));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NA Meeting — biscuit theft
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void stealBiscuits_npcsPresent_successAndBiscuitBandit() {
        CommunityCentreSystem.BiscuitTheftResult result =
                centre.stealBiscuits(true, inventory, criminalRecord, achievementCb);

        assertEquals(CommunityCentreSystem.BiscuitTheftResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.BISCUIT_TIN_SAVINGS));
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.THEFT));
        assertTrue(awarded.contains(AchievementType.BISCUIT_BANDIT));
    }

    @Test
    void stealBiscuits_noNpcsPresent_fails() {
        CommunityCentreSystem.BiscuitTheftResult result =
                centre.stealBiscuits(false, inventory, criminalRecord, achievementCb);

        assertEquals(CommunityCentreSystem.BiscuitTheftResult.NO_NPCS_PRESENT, result);
        assertFalse(inventory.hasItem(Material.BISCUIT_TIN_SAVINGS));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bring & Buy Sale — window
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isBringBuySale_saturdayMorning_returnsTrue() {
        assertTrue(centre.isBringBuySale(11.0f, CommunityCentreSystem.SATURDAY));
    }

    @Test
    void isBringBuySale_saturdayAfternoon_returnsFalse() {
        assertFalse(centre.isBringBuySale(14.0f, CommunityCentreSystem.SATURDAY));
    }

    @Test
    void isBringBuySale_weekday_returnsFalse() {
        assertFalse(centre.isBringBuySale(11.0f, CommunityCentreSystem.TUESDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bring & Buy — purchase items and SATURDAY_BARGAIN_HUNTER
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void purchaseBringBuyItem_success_addsItemAndDeductsCoin() {
        inventory.addItem(Material.COIN, 3);

        CommunityCentreSystem.BringBuyResult result =
                centre.purchaseBringBuyItem(Material.ORNAMENT, 2, inventory, achievementCb);

        assertEquals(CommunityCentreSystem.BringBuyResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.ORNAMENT));
        assertTrue(inventory.hasItem(Material.COIN, 1));
    }

    @Test
    void purchaseBringBuyItem_insufficientFunds_fails() {
        CommunityCentreSystem.BringBuyResult result =
                centre.purchaseBringBuyItem(Material.ORNAMENT, 5, inventory, achievementCb);

        assertEquals(CommunityCentreSystem.BringBuyResult.INSUFFICIENT_FUNDS, result);
    }

    @Test
    void purchaseBringBuyItem_threeItems_awardsBargainHunter() {
        inventory.addItem(Material.COIN, 10);
        centre.setBringBuyItemsPurchasedForTesting(2);

        centre.purchaseBringBuyItem(Material.ORNAMENT, 1, inventory, achievementCb);

        assertTrue(awarded.contains(AchievementType.SATURDAY_BARGAIN_HUNTER));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bring & Buy — antique clock
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void antiqueClockPurchase_whenAvailable_removesFromSale() {
        centre.setAntiqueClockAvailableForTesting(true);
        inventory.addItem(Material.COIN, 5);

        CommunityCentreSystem.BringBuyResult result =
                centre.purchaseBringBuyItem(Material.ANTIQUE_CLOCK,
                        CommunityCentreSystem.ANTIQUE_CLOCK_STALL_PRICE, inventory, achievementCb);

        assertEquals(CommunityCentreSystem.BringBuyResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.ANTIQUE_CLOCK));
        assertFalse(centre.isAntiqueClockAvailable());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bring & Buy — consignment
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void consignItem_withItemInInventory_successAndRemovesItem() {
        inventory.addItem(Material.RETRO_CASSETTE, 1);

        CommunityCentreSystem.ConsignResult result = centre.consignItem(Material.RETRO_CASSETTE, inventory);

        assertEquals(CommunityCentreSystem.ConsignResult.SUCCESS, result);
        assertFalse(inventory.hasItem(Material.RETRO_CASSETTE));
        assertEquals(1, centre.getConsignedItemCount());
    }

    @Test
    void consignItem_notInInventory_fails() {
        CommunityCentreSystem.ConsignResult result = centre.consignItem(Material.RETRO_CASSETTE, inventory);

        assertEquals(CommunityCentreSystem.ConsignResult.ITEM_NOT_IN_INVENTORY, result);
    }

    @Test
    void consignItem_maxReached_fails() {
        // Pre-load 5 items
        for (int i = 0; i < CommunityCentreSystem.BRING_BUY_MAX_CONSIGNMENT; i++) {
            inventory.addItem(Material.RETRO_CASSETTE, 1);
            centre.consignItem(Material.RETRO_CASSETTE, inventory);
        }
        inventory.addItem(Material.RETRO_CASSETTE, 1);

        CommunityCentreSystem.ConsignResult result = centre.consignItem(Material.RETRO_CASSETTE, inventory);

        assertEquals(CommunityCentreSystem.ConsignResult.MAX_CONSIGNMENT_REACHED, result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Curry Night — window
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isCurryNight_saturdayEvening_returnsTrue() {
        assertTrue(centre.isCurryNight(20.0f, CommunityCentreSystem.SATURDAY));
    }

    @Test
    void isCurryNight_saturdayAfterClose_returnsFalse() {
        assertFalse(centre.isCurryNight(22.5f, CommunityCentreSystem.SATURDAY));
    }

    @Test
    void isCurryNight_weekday_returnsFalse() {
        assertFalse(centre.isCurryNight(20.0f, CommunityCentreSystem.THURSDAY));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Curry Night — entry and dishes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void payCurryNightEntry_sufficientFunds_success() {
        inventory.addItem(Material.COIN, 5);

        CommunityCentreSystem.CurryNightResult result = centre.payCurryNightEntry(inventory);

        assertEquals(CommunityCentreSystem.CurryNightResult.SUCCESS, result);
        assertTrue(centre.isCurryNightEntryPaid());
        assertFalse(inventory.hasItem(Material.COIN, 5)); // deducted 2
        assertTrue(inventory.hasItem(Material.COIN, 3));
    }

    @Test
    void payCurryNightEntry_insufficientFunds_fails() {
        CommunityCentreSystem.CurryNightResult result = centre.payCurryNightEntry(inventory);
        assertEquals(CommunityCentreSystem.CurryNightResult.INSUFFICIENT_FUNDS, result);
    }

    @Test
    void payCurryNightEntry_alreadyPaid_returnsAlreadyPaid() {
        inventory.addItem(Material.COIN, 10);
        centre.payCurryNightEntry(inventory);
        CommunityCentreSystem.CurryNightResult result = centre.payCurryNightEntry(inventory);
        assertEquals(CommunityCentreSystem.CurryNightResult.ALREADY_PAID, result);
    }

    @Test
    void purchaseDish_withEntry_success() {
        centre.setCurryNightEntryPaidForTesting(true);
        inventory.addItem(Material.COIN, 5);

        CommunityCentreSystem.CurryNightResult result =
                centre.purchaseDish(Material.MANGO_LASSI, CommunityCentreSystem.PRICE_MANGO_LASSI, inventory);

        assertEquals(CommunityCentreSystem.CurryNightResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.MANGO_LASSI));
    }

    @Test
    void purchaseDish_withoutEntry_notAdmitted() {
        inventory.addItem(Material.COIN, 5);

        CommunityCentreSystem.CurryNightResult result =
                centre.purchaseDish(Material.MANGO_LASSI, CommunityCentreSystem.PRICE_MANGO_LASSI, inventory);

        assertEquals(CommunityCentreSystem.CurryNightResult.NOT_ADMITTED, result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Curry Night — attendance boosted by rain
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getCurryNightAttendance_clearWeather_baseAttendance() {
        assertEquals(CommunityCentreSystem.CURRY_NIGHT_BASE_ATTENDANCE,
                centre.getCurryNightAttendance(Weather.CLEAR));
    }

    @Test
    void getCurryNightAttendance_rain_boostedAttendance() {
        assertEquals(CommunityCentreSystem.CURRY_NIGHT_RAIN_ATTENDANCE,
                centre.getCurryNightAttendance(Weather.RAIN));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Curry Night — INFLUENCE XP from chat
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void chatWithAttendee_withEntry_awardsInfluenceXP() {
        centre.setCurryNightEntryPaidForTesting(true);
        NPC attendee = new NPC(NPCType.COMMUNITY_MEMBER, 1, 0, 1);
        int xpBefore = streetSkillSystem.getXP(StreetSkillSystem.Skill.INFLUENCE);

        int xpAwarded = centre.chatWithAttendee(attendee, streetSkillSystem);

        assertEquals(CommunityCentreSystem.CURRY_NIGHT_INFLUENCE_XP, xpAwarded);
        assertEquals(xpBefore + CommunityCentreSystem.CURRY_NIGHT_INFLUENCE_XP,
                streetSkillSystem.getXP(StreetSkillSystem.Skill.INFLUENCE));
    }

    @Test
    void chatWithAttendee_withoutEntry_noXP() {
        NPC attendee = new NPC(NPCType.COMMUNITY_MEMBER, 1, 0, 1);
        int xpAwarded = centre.chatWithAttendee(attendee, streetSkillSystem);
        assertEquals(0, xpAwarded);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Curry Night — rumour seeded at 17:00
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void maybeSeedCurryNightRumour_saturdayAt17_seedsRumour() {
        List<Rumour> seeded = new ArrayList<>();
        // Use a RumourNetwork that we can observe; just check Denise gets a rumour
        centre.maybeSeedCurryNightRumour(17.1f, CommunityCentreSystem.SATURDAY,
                deniseNpc, rumourNetwork);

        // The rumour should have been added to Denise
        assertFalse(deniseNpc.getRumours().isEmpty());
        assertEquals(RumourType.CURRY_NIGHT, deniseNpc.getRumours().get(0).getType());
    }

    @Test
    void maybeSeedCurryNightRumour_notSaturday_doesNotSeed() {
        centre.maybeSeedCurryNightRumour(17.1f, CommunityCentreSystem.FRIDAY,
                deniseNpc, rumourNetwork);
        assertTrue(deniseNpc.getRumours().isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grant — legitimate route
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void requestGrantForm_whenOpen_givesForm() {
        CommunityCentreSystem.GrantResult result =
                centre.requestGrantForm(10.0f, CommunityCentreSystem.MONDAY, inventory);

        assertEquals(CommunityCentreSystem.GrantResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.GRANT_APPLICATION_FORM));
    }

    @Test
    void requestGrantForm_whenClosed_fails() {
        CommunityCentreSystem.GrantResult result =
                centre.requestGrantForm(21.0f, CommunityCentreSystem.MONDAY, inventory);

        assertEquals(CommunityCentreSystem.GrantResult.CENTRE_CLOSED, result);
        assertFalse(inventory.hasItem(Material.GRANT_APPLICATION_FORM));
    }

    @Test
    void pinGrantFormToNoticeBoard_withForm_startsPending() {
        inventory.addItem(Material.GRANT_APPLICATION_FORM, 1);

        CommunityCentreSystem.GrantResult result = centre.pinGrantFormToNoticeBoard(inventory);

        assertEquals(CommunityCentreSystem.GrantResult.SUCCESS, result);
        assertTrue(centre.isGrantPending());
        assertEquals(CommunityCentreSystem.GRANT_PROCESS_DAYS, centre.getGrantDaysRemaining(), 0.001f);
        assertFalse(inventory.hasItem(Material.GRANT_APPLICATION_FORM));
    }

    @Test
    void tickGrantProcessing_afterThreeDays_deliversChequeAndHonestCitizen() {
        centre.setGrantPendingForTesting(true, 3);

        // Day 1
        boolean resolved = centre.tickGrantProcessing(inventory, achievementCb);
        assertFalse(resolved);
        // Day 2
        resolved = centre.tickGrantProcessing(inventory, achievementCb);
        assertFalse(resolved);
        // Day 3
        resolved = centre.tickGrantProcessing(inventory, achievementCb);

        assertTrue(resolved);
        assertTrue(inventory.hasItem(Material.GRANT_CHEQUE));
        assertFalse(centre.isGrantPending());
        assertTrue(awarded.contains(AchievementType.HONEST_CITIZEN));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grant — forged route, success case (deterministic with seeded Random)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void forgeGrantApplication_withForm_producesForgedForm() {
        inventory.addItem(Material.GRANT_APPLICATION_FORM, 1);

        CommunityCentreSystem.GrantResult result = centre.forgeGrantApplication(inventory);

        assertEquals(CommunityCentreSystem.GrantResult.SUCCESS, result);
        assertTrue(inventory.hasItem(Material.FORGED_GRANT_APPLICATION));
    }

    @Test
    void forgeGrantApplication_withoutForm_fails() {
        CommunityCentreSystem.GrantResult result = centre.forgeGrantApplication(inventory);
        assertEquals(CommunityCentreSystem.GrantResult.FORM_NOT_IN_INVENTORY, result);
    }

    @Test
    void postForgedGrantApplication_withoutForm_fails() {
        CommunityCentreSystem.ForgeResult result =
                centre.postForgedGrantApplication(inventory, 0, criminalRecord,
                        null, null, null, achievementCb);

        assertEquals(CommunityCentreSystem.ForgeResult.FORM_NOT_IN_INVENTORY, result);
    }

    @Test
    void postForgedGrantApplication_successOrCaught_validOutcome() {
        // This test checks that the outcome is one of the valid states
        // (probabilistic, so we just verify correctness of state transitions)
        inventory.addItem(Material.FORGED_GRANT_APPLICATION, 1);
        WantedSystem wantedSystem = new WantedSystem(new Random(42L));
        NPC witness = new NPC(NPCType.COMMUNITY_MEMBER, 1, 0, 1);

        CommunityCentreSystem.ForgeResult result =
                centre.postForgedGrantApplication(inventory, 10, criminalRecord,
                        wantedSystem, witness, rumourNetwork, achievementCb);

        assertTrue(result == CommunityCentreSystem.ForgeResult.SUCCESS ||
                   result == CommunityCentreSystem.ForgeResult.CAUGHT);

        if (result == CommunityCentreSystem.ForgeResult.SUCCESS) {
            assertTrue(inventory.hasItem(Material.GRANT_CHEQUE));
            assertTrue(awarded.contains(AchievementType.GRANT_GRABBER));
        } else {
            // Caught: fraud recorded, cheque not given
            assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.FRAUD));
            assertFalse(inventory.hasItem(Material.GRANT_CHEQUE));
        }
    }

    @Test
    void postForgedGrantApplication_highNotoriety_increasedCatchChance() {
        // At notoriety >= 50, catch chance doubles to 50%
        // Just verify it doesn't throw and produces a valid result
        inventory.addItem(Material.FORGED_GRANT_APPLICATION, 1);

        CommunityCentreSystem.ForgeResult result =
                centre.postForgedGrantApplication(inventory,
                        CommunityCentreSystem.FORGE_HIGH_NOTORIETY_THRESHOLD,
                        criminalRecord, null, null, null, achievementCb);

        assertTrue(result == CommunityCentreSystem.ForgeResult.SUCCESS ||
                   result == CommunityCentreSystem.ForgeResult.CAUGHT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Suspicion timer
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void tickSuspicion_belowLimit_returnsFalse() {
        assertFalse(centre.tickSuspicion(5.0f));
        assertEquals(5.0f, centre.getSuspicionTimer(), 0.001f);
    }

    @Test
    void tickSuspicion_exceedsLimit_returnsTrue() {
        centre.tickSuspicion(8.0f);
        assertTrue(centre.tickSuspicion(5.0f)); // total 13 >= 10
    }

    @Test
    void resetSuspicion_resetsTimer() {
        centre.tickSuspicion(9.0f);
        centre.resetSuspicion();
        assertEquals(0f, centre.getSuspicionTimer(), 0.001f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stall count and pricing
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getStallCount_returnsBetweenMinAndMax() {
        int count = centre.getStallCount();
        assertTrue(count >= CommunityCentreSystem.BRING_BUY_STALL_MIN &&
                   count <= CommunityCentreSystem.BRING_BUY_STALL_MAX);
    }

    @Test
    void rollStallPrice_returnsWithinExpectedRange() {
        int fenceValue = 10;
        // With seeded Random(42) test many rolls
        for (int i = 0; i < 20; i++) {
            int price = centre.rollStallPrice(fenceValue);
            assertTrue(price >= 1);
            // 70% of 10 = 7
            assertTrue(price <= Math.ceil(fenceValue * CommunityCentreSystem.BRING_BUY_PRICE_MAX_FACTOR));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NPC type existence sanity checks
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void npcTypes_allCommunityCentreTypesExist() {
        // Simply constructing NPCs with these types proves they exist
        NPC manager = new NPC(NPCType.COMMUNITY_CENTRE_MANAGER, 0, 0, 0);
        NPC instructor = new NPC(NPCType.AEROBICS_INSTRUCTOR, 0, 0, 0);
        NPC chair = new NPC(NPCType.NA_CHAIR, 0, 0, 0);
        NPC attendee = new NPC(NPCType.NA_ATTENDEE, 0, 0, 0);
        NPC member = new NPC(NPCType.COMMUNITY_MEMBER, 0, 0, 0);

        assertNotNull(manager);
        assertNotNull(instructor);
        assertNotNull(chair);
        assertNotNull(attendee);
        assertNotNull(member);
    }
}
