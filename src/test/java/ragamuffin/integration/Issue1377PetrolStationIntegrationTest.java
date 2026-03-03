package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1377 — PetrolStationSystem end-to-end scenarios.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>Fill petrol can deducts 3 COIN and converts item</b>: Give player 5 COIN and
 *       one PETROL_CAN. Call fillPetrolCan(). Verify COIN = 2, PETROL_CAN = 0,
 *       PETROL_CAN_FULL = 1, result = SUCCESS.</li>
 *   <li><b>Night siphon success adds fuel and criminal record</b>: Set time to 02:00
 *       (night window). Give player 1 PETROL_CAN. Start siphon; advance 5.0 s; complete.
 *       Verify PETROL_CAN_FULL in inventory, VEHICLE_TAMPERING in CriminalRecord,
 *       Notoriety +3.</li>
 *   <li><b>Till robbery triggers wanted tier, 3-minute police response, and COMMUNITY_OUTRAGE</b>:
 *       Give player CROWBAR. Call robTill(). Verify COIN gain in range 8–18,
 *       ARMED_ROBBERY in CriminalRecord, WantedSystem ≥ 2 stars, policeResponseTimer set,
 *       COMMUNITY_OUTRAGE rumour seeded.</li>
 *   <li><b>Wayne sleeps 01:00–03:00 — low-noise crime undetected</b>: Set time to 02:00.
 *       Wayne is on duty. Noise = 5 (below threshold). Verify isWayneRespondingToNoise
 *       returns false. Noise = 20 (above threshold). Verify returns true.</li>
 *   <li><b>Drive-off with active CCTV is detected</b>: Fill can then drive-off with
 *       cctvHasLos = true. Verify PETROL_THEFT in CriminalRecord, Notoriety +4,
 *       WantedSystem +1, DRIVE_OFF achievement awarded, CRIME_SPOTTED rumour seeded.</li>
 * </ol>
 *
 * <p>Unit tests:
 * <ul>
 *   <li>Petrol fill deducts COIN correctly.</li>
 *   <li>Siphon interrupt at 4.9 s fails (returns INTERRUPTED).</li>
 *   <li>CCTV LoS detection on drive-off.</li>
 *   <li>Till robbery COIN range 8–18 over 1000 samples.</li>
 *   <li>Cigarette cabinet 3-hit break.</li>
 *   <li>Wayne sleep/wake thresholds.</li>
 *   <li>Raj refusal at WantedSystem (notoriety) tier 3.</li>
 * </ul>
 */
class Issue1377PetrolStationIntegrationTest {

    private PetrolStationSystem petrolStation;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private WantedSystem wantedSystem;
    private List<AchievementType> awarded;
    private NotorietySystem.AchievementCallback achievementCb;
    private List<NPC> npcs;
    private NPC raj;
    private NPC wayne;

    @BeforeEach
    void setUp() {
        petrolStation = new PetrolStationSystem(new Random(42L));
        inventory = new Inventory(36);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7L));
        wantedSystem = new WantedSystem();

        awarded = new ArrayList<>();
        achievementCb = type -> {
            awarded.add(type);
            achievements.unlock(type);
        };

        petrolStation.setNotorietySystem(notorietySystem);
        petrolStation.setAchievementSystem(achievements);
        petrolStation.setCriminalRecord(criminalRecord);
        petrolStation.setRumourNetwork(rumourNetwork);
        petrolStation.setWantedSystem(wantedSystem);

        // Create Raj and Wayne NPCs
        raj = new NPC(NPCType.PETROL_STATION_ATTENDANT, 0f, 1f, 0f);
        wayne = new NPC(NPCType.PETROL_STATION_ASSISTANT, 0f, 1f, 0f);
        petrolStation.setRaj(raj);
        petrolStation.setWayne(wayne);

        npcs = new ArrayList<>();
        NPC publicNpc = new NPC(NPCType.PUBLIC, 5f, 1f, 5f);
        npcs.add(publicNpc);
        npcs.add(raj);
        npcs.add(wayne);
    }

    // ── Integration Test 1: Fill petrol can deducts 3 COIN and converts item ──

    /**
     * Fill petrol can end-to-end:
     * <ol>
     *   <li>Give player 5 COIN and 1 PETROL_CAN.</li>
     *   <li>Call fillPetrolCan() at 10:00 (no frost).</li>
     *   <li>Verify result = SUCCESS.</li>
     *   <li>Verify COIN = 2, PETROL_CAN = 0, PETROL_CAN_FULL = 1.</li>
     * </ol>
     */
    @Test
    void fillPetrolCan_deductsCoinAndConvertsItem() {
        inventory.addItem(Material.COIN, 5);
        inventory.addItem(Material.PETROL_CAN, 1);

        PetrolStationSystem.FillResult result = petrolStation.fillPetrolCan(
                inventory, 10.0f, false, achievementCb);

        assertEquals(PetrolStationSystem.FillResult.SUCCESS, result);
        assertEquals(2, inventory.getItemCount(Material.COIN));
        assertEquals(0, inventory.getItemCount(Material.PETROL_CAN));
        assertEquals(1, inventory.getItemCount(Material.PETROL_CAN_FULL));
    }

    /**
     * Fill fails when player has insufficient COIN.
     */
    @Test
    void fillPetrolCan_insufficientFunds() {
        inventory.addItem(Material.COIN, 2);
        inventory.addItem(Material.PETROL_CAN, 1);

        PetrolStationSystem.FillResult result = petrolStation.fillPetrolCan(
                inventory, 10.0f, false, achievementCb);

        assertEquals(PetrolStationSystem.FillResult.INSUFFICIENT_FUNDS, result);
        assertEquals(2, inventory.getItemCount(Material.COIN));
        assertEquals(1, inventory.getItemCount(Material.PETROL_CAN));
        assertEquals(0, inventory.getItemCount(Material.PETROL_CAN_FULL));
    }

    /**
     * Fill fails when player has no empty PETROL_CAN.
     */
    @Test
    void fillPetrolCan_noEmptyCan() {
        inventory.addItem(Material.COIN, 10);

        PetrolStationSystem.FillResult result = petrolStation.fillPetrolCan(
                inventory, 10.0f, false, achievementCb);

        assertEquals(PetrolStationSystem.FillResult.NO_EMPTY_CAN, result);
    }

    // ── Integration Test 2: Night siphon success adds fuel and criminal record ─

    /**
     * Siphon end-to-end at 02:00 (night window):
     * <ol>
     *   <li>Give player 1 PETROL_CAN.</li>
     *   <li>startSiphon(); advance 5.0 s; completeSiphon().</li>
     *   <li>Verify result = SUCCESS.</li>
     *   <li>Verify PETROL_CAN_FULL = 1, PETROL_CAN = 0.</li>
     *   <li>Verify VEHICLE_TAMPERING recorded in CriminalRecord.</li>
     *   <li>Verify Notoriety = SIPHON_NOTORIETY (3).</li>
     * </ol>
     */
    @Test
    void nightSiphon_successAddsFuelAndCriminalRecord() {
        float nightHour = 2.0f; // 02:00 — in siphon window
        inventory.addItem(Material.PETROL_CAN, 1);

        petrolStation.startSiphon();
        petrolStation.advanceSiphon(PetrolStationSystem.SIPHON_HOLD_SECONDS); // exactly 5s

        PetrolStationSystem.SiphonResult result = petrolStation.completeSiphon(
                nightHour, inventory,
                false, // no CCTV LoS
                0f, 1f, 0f,
                achievementCb);

        assertEquals(PetrolStationSystem.SiphonResult.SUCCESS, result);
        assertEquals(1, inventory.getItemCount(Material.PETROL_CAN_FULL));
        assertEquals(0, inventory.getItemCount(Material.PETROL_CAN));
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.VEHICLE_TAMPERING));
        assertEquals(PetrolStationSystem.SIPHON_NOTORIETY, notorietySystem.getNotoriety());
    }

    /**
     * Siphon interrupt at 4.9 s fails — returns INTERRUPTED (not SUCCESS).
     */
    @Test
    void siphon_interruptAt4_9sFails() {
        float nightHour = 2.0f;
        inventory.addItem(Material.PETROL_CAN, 1);

        petrolStation.startSiphon();
        petrolStation.advanceSiphon(4.9f); // less than 5s

        PetrolStationSystem.SiphonResult result = petrolStation.completeSiphon(
                nightHour, inventory,
                false,
                0f, 1f, 0f,
                achievementCb);

        assertEquals(PetrolStationSystem.SiphonResult.INTERRUPTED, result);
        assertEquals(0, inventory.getItemCount(Material.PETROL_CAN_FULL));
        assertEquals(1, inventory.getItemCount(Material.PETROL_CAN));
    }

    /**
     * Siphon during daytime returns WRONG_TIME.
     */
    @Test
    void siphon_wrongTime_dayReturnsWrongTime() {
        float dayHour = 14.0f; // 14:00 — outside night window
        inventory.addItem(Material.PETROL_CAN, 1);

        petrolStation.startSiphon();
        petrolStation.advanceSiphon(PetrolStationSystem.SIPHON_HOLD_SECONDS);

        PetrolStationSystem.SiphonResult result = petrolStation.completeSiphon(
                dayHour, inventory,
                false,
                0f, 1f, 0f,
                achievementCb);

        assertEquals(PetrolStationSystem.SiphonResult.WRONG_TIME, result);
    }

    /**
     * Siphon with active CCTV LoS adds WantedSystem +1 star.
     */
    @Test
    void siphon_withCctvLos_addsWantedStar() {
        float nightHour = 23.0f;
        inventory.addItem(Material.PETROL_CAN, 1);

        petrolStation.startSiphon();
        petrolStation.advanceSiphon(PetrolStationSystem.SIPHON_HOLD_SECONDS);

        petrolStation.completeSiphon(
                nightHour, inventory,
                true, // CCTV has LoS
                0f, 1f, 0f,
                achievementCb);

        assertEquals(PetrolStationSystem.SIPHON_CCTV_WANTED_STARS, wantedSystem.getWantedStars());
    }

    // ── Integration Test 3: Till robbery ──────────────────────────────────────

    /**
     * Till robbery end-to-end:
     * <ol>
     *   <li>Give player CROWBAR and 0 COIN.</li>
     *   <li>Call robTill() at 12:00 with notoriety=0 (tier 0, no panic).</li>
     *   <li>Verify result = SUCCESS.</li>
     *   <li>Verify COIN gained is 8–18.</li>
     *   <li>Verify ARMED_ROBBERY in CriminalRecord.</li>
     *   <li>Verify WantedSystem ≥ 2 stars.</li>
     *   <li>Verify policeResponseTimer is set (> 0).</li>
     *   <li>Verify COMMUNITY_OUTRAGE rumour seeded.</li>
     * </ol>
     */
    @Test
    void tillRobbery_triggersWantedTierAndPoliceResponse_andCommunityOutrage() {
        inventory.addItem(Material.CROWBAR, 1);

        PetrolStationSystem.TillRobberyResult result = petrolStation.robTill(
                inventory,
                0, // notoriety
                12.0f, // hour — Raj is on duty
                npcs,
                0f, 1f, 0f,
                achievementCb);

        assertEquals(PetrolStationSystem.TillRobberyResult.SUCCESS, result);

        int coinGain = inventory.getItemCount(Material.COIN);
        assertTrue(coinGain >= PetrolStationSystem.TILL_MIN_COIN,
                "COIN gain should be >= " + PetrolStationSystem.TILL_MIN_COIN + ", was " + coinGain);
        assertTrue(coinGain <= PetrolStationSystem.TILL_MAX_COIN,
                "COIN gain should be <= " + PetrolStationSystem.TILL_MAX_COIN + ", was " + coinGain);

        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.ARMED_ROBBERY));
        assertTrue(wantedSystem.getWantedStars() >= PetrolStationSystem.TILL_ROBBERY_WANTED_STARS);
        assertTrue(petrolStation.getPoliceResponseTimer() > 0f);

        // COMMUNITY_OUTRAGE rumour seeded
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.COMMUNITY_OUTRAGE),
                "COMMUNITY_OUTRAGE rumour should be seeded after till robbery");
    }

    /**
     * Till robbery requires CROWBAR — fails without it.
     */
    @Test
    void tillRobbery_noCrowbarReturnsNoCrowbar() {
        // No CROWBAR in inventory
        PetrolStationSystem.TillRobberyResult result = petrolStation.robTill(
                inventory, 0, 12.0f, npcs, 0f, 1f, 0f, achievementCb);

        assertEquals(PetrolStationSystem.TillRobberyResult.NO_CROWBAR, result);
        assertEquals(0, inventory.getItemCount(Material.COIN));
    }

    /**
     * Till robbery COIN range is 8–18 over 1000 samples.
     */
    @Test
    void tillRobbery_coinRange8to18_over1000Samples() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < 1000; i++) {
            PetrolStationSystem ps = new PetrolStationSystem(new Random(i));
            ps.setCriminalRecord(new CriminalRecord());
            ps.setNotorietySystem(new NotorietySystem());
            ps.setWantedSystem(new WantedSystem());

            Inventory inv = new Inventory(36);
            inv.addItem(Material.CROWBAR, 1);

            ps.robTill(inv, 0, 12.0f, new ArrayList<>(), 0f, 1f, 0f, null);

            int coins = inv.getItemCount(Material.COIN);
            if (coins < min) min = coins;
            if (coins > max) max = coins;
        }

        assertEquals(PetrolStationSystem.TILL_MIN_COIN, min,
                "Min COIN yield should be " + PetrolStationSystem.TILL_MIN_COIN);
        assertEquals(PetrolStationSystem.TILL_MAX_COIN, max,
                "Max COIN yield should be " + PetrolStationSystem.TILL_MAX_COIN);
    }

    // ── Integration Test 4: Wayne sleeps 01:00–03:00 ─────────────────────────

    /**
     * Wayne sleep window:
     * <ol>
     *   <li>At 02:00, noise=5 (< 15): isWayneRespondingToNoise = false.</li>
     *   <li>At 02:00, noise=20 (≥ 15): isWayneRespondingToNoise = true.</li>
     *   <li>At 04:00 (outside window): always true.</li>
     * </ol>
     */
    @Test
    void wayne_sleepWindowAndNoiseThresholds() {
        float sleepHour = 2.0f;

        // Low noise — Wayne does not respond
        assertFalse(petrolStation.isWayneRespondingToNoise(sleepHour, 5.0f),
                "Wayne should not respond to noise < 15 at 02:00");

        // High noise — Wayne wakes
        assertTrue(petrolStation.isWayneRespondingToNoise(sleepHour, 20.0f),
                "Wayne should respond to noise >= 15 at 02:00");

        // Outside sleep window — always responds
        assertTrue(petrolStation.isWayneRespondingToNoise(4.0f, 0.1f),
                "Wayne should always respond outside sleep window");
    }

    /**
     * Low-noise crime during Wayne's sleep is undetected (siphon with no CCTV,
     * Wayne asleep, no WantedSystem escalation from CCTV).
     */
    @Test
    void wayne_lowNoiseCrimeDuringSleep_undetectedByWayne() {
        float nightHour = 2.0f;
        inventory.addItem(Material.PETROL_CAN, 1);

        // Wayne is asleep; noise is below threshold
        petrolStation.update(1f / 60f, nightHour, 5.0f, npcs, null, achievementCb);

        petrolStation.startSiphon();
        petrolStation.advanceSiphon(PetrolStationSystem.SIPHON_HOLD_SECONDS);

        PetrolStationSystem.SiphonResult result = petrolStation.completeSiphon(
                nightHour, inventory,
                false, // no CCTV LoS
                0f, 1f, 0f,
                achievementCb);

        assertEquals(PetrolStationSystem.SiphonResult.SUCCESS, result);
        // Wayne was asleep and didn't respond — no wanted escalation from Wayne
        assertEquals(0, wantedSystem.getWantedStars(),
                "No wanted stars should be added when Wayne is asleep and CCTV off");
        // Crime is still recorded
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.VEHICLE_TAMPERING));
    }

    // ── Integration Test 5: Drive-off with active CCTV is detected ───────────

    /**
     * Drive-off with CCTV active and LoS:
     * <ol>
     *   <li>Call onDriveOff(cctvHasLos=true).</li>
     *   <li>Verify PETROL_THEFT in CriminalRecord.</li>
     *   <li>Verify Notoriety += DRIVEOFF_NOTORIETY (4).</li>
     *   <li>Verify WantedSystem += DRIVEOFF_WANTED_STARS (1).</li>
     *   <li>Verify DRIVE_OFF achievement awarded.</li>
     *   <li>Verify CRIME_SPOTTED rumour seeded.</li>
     * </ol>
     */
    @Test
    void driveOff_withActiveCctv_isDetected() {
        petrolStation.onDriveOff(
                true, // CCTV has LoS
                0f, 1f, 0f,
                npcs,
                achievementCb);

        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.PETROL_THEFT),
                "PETROL_THEFT should be recorded");
        assertEquals(PetrolStationSystem.DRIVEOFF_NOTORIETY, notorietySystem.getNotoriety(),
                "Notoriety should increase by DRIVEOFF_NOTORIETY");
        assertEquals(PetrolStationSystem.DRIVEOFF_WANTED_STARS, wantedSystem.getWantedStars(),
                "WantedSystem should gain DRIVEOFF_WANTED_STARS");
        assertTrue(awarded.contains(AchievementType.DRIVE_OFF),
                "DRIVE_OFF achievement should be awarded");
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.CRIME_SPOTTED),
                "CRIME_SPOTTED rumour should be seeded");
    }

    /**
     * Drive-off without CCTV LoS — no criminal record, no notoriety, but achievement still awarded.
     */
    @Test
    void driveOff_noCctvLos_noDetection() {
        petrolStation.onDriveOff(
                false, // CCTV does not have LoS
                0f, 1f, 0f,
                npcs,
                achievementCb);

        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.PETROL_THEFT),
                "PETROL_THEFT should NOT be recorded without CCTV LoS");
        assertEquals(0, notorietySystem.getNotoriety(),
                "Notoriety should NOT increase without detection");
        assertEquals(0, wantedSystem.getWantedStars(),
                "No wanted stars without CCTV detection");
        // Achievement is still awarded (the act happened)
        assertTrue(awarded.contains(AchievementType.DRIVE_OFF),
                "DRIVE_OFF achievement should be awarded regardless of detection");
    }

    /**
     * Drive-off with CCTV broken (isCctvActive=false) — no detection even with LoS.
     */
    @Test
    void driveOff_cctvBroken_noDetection() {
        petrolStation.breakCctv();
        assertFalse(petrolStation.isCctvActive());

        petrolStation.onDriveOff(
                true, // claimed LoS — but CCTV is broken
                0f, 1f, 0f,
                npcs,
                achievementCb);

        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.PETROL_THEFT),
                "Broken CCTV should not detect drive-off");
        assertEquals(0, wantedSystem.getWantedStars());
    }

    // ── Unit tests ────────────────────────────────────────────────────────────

    /**
     * Cigarette cabinet requires 3 hits to smash.
     */
    @Test
    void cigaretteCabinet_3HitsToSmash() {
        // Hit 1
        CabinetSmashResult r1 = petrolStation.hitCigaretteCabinet(inventory);
        assertEquals(CabinetSmashResult.HIT_REGISTERED, r1);
        assertEquals(0, inventory.getItemCount(Material.CIGARETTE_CARTON));

        // Hit 2
        CabinetSmashResult r2 = petrolStation.hitCigaretteCabinet(inventory);
        assertEquals(CabinetSmashResult.HIT_REGISTERED, r2);
        assertEquals(0, inventory.getItemCount(Material.CIGARETTE_CARTON));

        // Hit 3 — smashed
        CabinetSmashResult r3 = petrolStation.hitCigaretteCabinet(inventory);
        assertEquals(CabinetSmashResult.SMASHED, r3);
        assertTrue(petrolStation.isCabinetSmashed());

        int cartons = inventory.getItemCount(Material.CIGARETTE_CARTON);
        assertTrue(cartons >= PetrolStationSystem.CIGARETTE_CARTON_DROP_MIN,
                "Should drop at least " + PetrolStationSystem.CIGARETTE_CARTON_DROP_MIN + " cartons");
        assertTrue(cartons <= PetrolStationSystem.CIGARETTE_CARTON_DROP_MAX,
                "Should drop at most " + PetrolStationSystem.CIGARETTE_CARTON_DROP_MAX + " cartons");

        // Further hits return ALREADY_SMASHED
        CabinetSmashResult r4 = petrolStation.hitCigaretteCabinet(inventory);
        assertEquals(CabinetSmashResult.ALREADY_SMASHED, r4);
    }

    /**
     * Raj refuses service at notoriety tier ≥ 3 (raw notoriety ≥ TIER_3_THRESHOLD = 500).
     */
    @Test
    void raj_refusesServiceAtTier3() {
        // TIER_3_THRESHOLD is 500
        int highNotoriety = NotorietySystem.TIER_3_THRESHOLD;
        inventory.addItem(Material.COIN, 10);

        PetrolStationSystem.PurchaseResult result = petrolStation.buyItem(
                Material.ENERGY_DRINK, inventory, 10.0f, highNotoriety, achievementCb);

        assertEquals(PetrolStationSystem.PurchaseResult.SERVICE_REFUSED, result);
    }

    /**
     * Raj serves normally at tier 2 (notoriety = TIER_2_THRESHOLD = 250).
     */
    @Test
    void raj_servesAtTier2() {
        int tier2Notoriety = NotorietySystem.TIER_2_THRESHOLD;
        inventory.addItem(Material.COIN, 10);

        PetrolStationSystem.PurchaseResult result = petrolStation.buyItem(
                Material.ENERGY_DRINK, inventory, 10.0f, tier2Notoriety, achievementCb);

        assertEquals(PetrolStationSystem.PurchaseResult.SUCCESS, result);
    }

    /**
     * isRajOnDuty and isWayneOnDuty return correct values at boundary hours.
     */
    @Test
    void attendantDutyHours_boundaries() {
        // Raj: 07:00–19:00
        assertFalse(petrolStation.isRajOnDuty(6.9f));
        assertTrue(petrolStation.isRajOnDuty(7.0f));
        assertTrue(petrolStation.isRajOnDuty(18.9f));
        assertFalse(petrolStation.isRajOnDuty(19.0f));

        // Wayne: 19:00–07:00 (night)
        assertTrue(petrolStation.isWayneOnDuty(19.0f));
        assertTrue(petrolStation.isWayneOnDuty(2.0f));
        assertFalse(petrolStation.isWayneOnDuty(10.0f));
    }

    /**
     * isWayneAsleep returns correct values at boundary hours.
     */
    @Test
    void wayne_asleepBoundaryHours() {
        assertFalse(petrolStation.isWayneAsleep(0.9f));
        assertTrue(petrolStation.isWayneAsleep(1.0f));
        assertTrue(petrolStation.isWayneAsleep(2.5f));
        assertFalse(petrolStation.isWayneAsleep(3.0f));
    }

    /**
     * MICROWAVE_MILLIONAIRE achievement awarded when buying pasty after 21:00.
     */
    @Test
    void microwaveMillionaire_awardedAfter21h() {
        inventory.addItem(Material.COIN, 10);

        // Buy before 21:00 — no achievement
        petrolStation.buyItem(Material.MICROWAVE_PASTY, inventory, 20.99f, 0, achievementCb);
        assertFalse(awarded.contains(AchievementType.MICROWAVE_MILLIONAIRE));

        // Buy at 21:00 — achievement awarded
        petrolStation.buyItem(Material.MICROWAVE_PASTY, inventory, 21.0f, 0, achievementCb);
        assertTrue(awarded.contains(AchievementType.MICROWAVE_MILLIONAIRE));
    }

    /**
     * PETROL_HEAD achievement awarded after 5 legitimate fills.
     */
    @Test
    void petrolHead_awardedAfter5Fills() {
        for (int i = 0; i < 5; i++) {
            inventory.addItem(Material.COIN, PetrolStationSystem.PRICE_PETROL_CAN);
            inventory.addItem(Material.PETROL_CAN, 1);
            petrolStation.fillPetrolCan(inventory, 10.0f, false, achievementCb);
            // Remove the full can so we can fill again
            inventory.removeItem(Material.PETROL_CAN_FULL, 1);
        }

        assertTrue(awarded.contains(AchievementType.PETROL_HEAD),
                "PETROL_HEAD achievement should be awarded after 5 fills");
    }

    /**
     * FORECOURT_REGULAR achievement awarded after 10 kiosk purchases.
     */
    @Test
    void forecourtRegular_awardedAfter10Purchases() {
        for (int i = 0; i < 10; i++) {
            inventory.addItem(Material.COIN, PetrolStationSystem.PRICE_ENERGY_DRINK);
            petrolStation.buyItem(Material.ENERGY_DRINK, inventory, 10.0f, 0, achievementCb);
        }

        assertTrue(awarded.contains(AchievementType.FORECOURT_REGULAR),
                "FORECOURT_REGULAR achievement should be awarded after 10 purchases");
    }

    /**
     * Kiosk purchase returns CLOSED outside opening hours (no active attendant).
     * Since Raj works 07–19 and Wayne works 19–07, the kiosk is always open — but
     * we test that service_refused threshold works.
     */
    @Test
    void kiosk_refuseWithInsufficientFunds() {
        // No COIN in inventory
        PetrolStationSystem.PurchaseResult result = petrolStation.buyItem(
                Material.ENERGY_DRINK, inventory, 10.0f, 0, achievementCb);

        assertEquals(PetrolStationSystem.PurchaseResult.INSUFFICIENT_FUNDS, result);
    }

    /**
     * Wayne panic button triggers armed response when tier ≥ 2 and Wayne is awake.
     */
    @Test
    void tillRobbery_panicButton_triggerArmedResponse_whenTierAboveThreshold() {
        inventory.addItem(Material.CROWBAR, 1);

        // Notoriety at tier 2 threshold (TIER_2_THRESHOLD = 250)
        int tier2Notoriety = NotorietySystem.TIER_2_THRESHOLD;

        // Wayne on duty at 22:00
        PetrolStationSystem.TillRobberyResult result = petrolStation.robTill(
                inventory, tier2Notoriety, 22.0f, npcs, 0f, 1f, 0f, achievementCb);

        assertEquals(PetrolStationSystem.TillRobberyResult.PANIC_BUTTON_ARMED_RESPONSE, result);
        assertTrue(petrolStation.isPanicButtonTriggered());
        assertTrue(petrolStation.isArmedResponseInbound());
        assertTrue(petrolStation.getArmedResponseTimer() > 0f);
    }
}
