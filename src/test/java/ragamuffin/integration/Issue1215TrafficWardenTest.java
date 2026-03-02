package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ai.CarManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.Car;
import ragamuffin.entity.Car.CarColour;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1215 — Northfield Traffic Warden:
 * Clive's PCN Circuit, Wheel Clamps &amp; Council Office Appeal.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>PCN issued and escalates to clamp — Monday 10:00, CLEAR weather, car
 *       parked with no ticket. First circuit issues CIVIL_ENFORCEMENT_NOTICE;
 *       after 1 in-game day the second circuit applies WHEEL_CLAMP_PROP;
 *       CLAMPED achievement unlocked.</li>
 *   <li>Clive's Terminal silently removes clamp — seeded RNG forces 80% success
 *       path; CLIVE_TERMINAL consumed; no CRIMINAL_DAMAGE; CLAMP_EVADER unlocked.</li>
 *   <li>PCN appeal succeeds with full bonuses — machine reported + clean record +
 *       BENEFIT_APPEAL_LETTER; seeded RNG forces success; fine refunded; CIVIL_ENFORCEMENT_NOTICE
 *       count reduced; BUREAUCRAT_BESTED unlocked.</li>
 *   <li>Clive absent in HEAVY_RAIN — RAIN weather; Tuesday 11:00; car parked
 *       without ticket; Clive does not spawn; no CIVIL_ENFORCEMENT_NOTICE recorded.</li>
 *   <li>Car towed to impound after third offence; player retrieves it for 20 COIN.</li>
 * </ol>
 */
class Issue1215TrafficWardenTest {

    private TrafficWardenSystem trafficWarden;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private RumourNetwork rumourNetwork;
    private Inventory inventory;
    private List<NPC> npcList;
    private Car playerCar;

    @BeforeEach
    void setUp() {
        // Seeded RNG for deterministic tests; individual tests may override
        trafficWarden = new TrafficWardenSystem(new Random(42));
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem();
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();
        rumourNetwork = new RumourNetwork(new Random());

        trafficWarden.setNotorietySystem(notorietySystem);
        trafficWarden.setWantedSystem(wantedSystem);
        trafficWarden.setCriminalRecord(criminalRecord);
        trafficWarden.setAchievementSystem(achievementSystem);
        trafficWarden.setRumourNetwork(rumourNetwork);

        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 30);

        npcList = new ArrayList<>();

        // Spawn a parked player car
        CarManager carManager = new CarManager();
        playerCar = carManager.spawnParkedCar(50f, 0f, 50f, false, 0f, 100f, CarColour.BLUE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: PCN issued and escalates to clamp
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Monday 10:00, CLEAR weather. Car parked without a valid ticket.
     * Advance 1 full Clive circuit (8 in-game minutes) → PCN issued.
     * Verify CriminalRecord contains CIVIL_ENFORCEMENT_NOTICE.
     * Verify Notoriety increased by 2.
     * Advance 1 in-game day further → clamp applied.
     * Verify isCarClamped() == true.
     * Verify CLAMPED achievement unlocked.
     */
    @Test
    void pcnIssuedAndEsclatesToClamp() {
        int monday = TrafficWardenSystem.MONDAY;
        float hour = 10.0f; // 10:00

        int notorietyBefore = notorietySystem.getNotoriety();

        // ── First circuit: issue PCN ──────────────────────────────────────────
        // Advance 8 in-game minutes (one full circuit)
        trafficWarden.update(8.0f, hour, monday, Weather.CLEAR, playerCar, npcList);

        // Clive should be on duty
        assertTrue(trafficWarden.isCliveOnDuty(),
                "Clive should be on duty Monday 10:00 in CLEAR weather");

        // NPC list should contain TRAFFIC_WARDEN
        boolean clivePresent = npcList.stream()
                .anyMatch(npc -> npc.getType() == NPCType.TRAFFIC_WARDEN);
        assertTrue(clivePresent, "NPC list should contain TRAFFIC_WARDEN");

        // PCN should be issued
        assertTrue(trafficWarden.isCarHasPCN(),
                "Car should have a PCN after one Clive circuit");

        // CIVIL_ENFORCEMENT_NOTICE in criminal record
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE) > 0,
                "CriminalRecord should contain CIVIL_ENFORCEMENT_NOTICE");

        // Notoriety increased by 2
        assertEquals(notorietyBefore + TrafficWardenSystem.NOTORIETY_FIRST_PCN,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by 2 on first PCN");

        // ── Advance 1 in-game day without paying ─────────────────────────────
        // 1 day = 1440 minutes + another circuit (8 min)
        // We use setMinutesSincePCN to fast-forward the escalation clock
        trafficWarden.setMinutesSincePCNForTesting(1440f + 1f); // ≥ 1 day old

        // Second circuit: should apply clamp
        trafficWarden.update(8.0f, hour, monday, Weather.CLEAR, playerCar, npcList);

        assertTrue(trafficWarden.isCarClamped(),
                "Car should be clamped after second circuit with unpaid PCN");

        // CLAMPED achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.CLAMPED),
                "CLAMPED achievement should be unlocked when car is first clamped");

        // Notoriety also increased by CLAMP amount
        int expectedNotoriety = notorietyBefore
                + TrafficWardenSystem.NOTORIETY_FIRST_PCN
                + TrafficWardenSystem.NOTORIETY_CLAMP;
        assertEquals(expectedNotoriety, notorietySystem.getNotoriety(),
                "Notoriety should increase by " + TrafficWardenSystem.NOTORIETY_CLAMP
                        + " more when car is clamped");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Clive's Terminal silently removes clamp
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player car is already clamped. Player has CLIVE_TERMINAL in inventory.
     * Seed TrafficWardenSystem(new Random(42)) — first float is 0.739 which is
     * &gt; 0.20 (the failure side), so we need a seed that gives &lt; 0.80.
     * We use seed=0 which gives nextFloat() ≈ 0.730, still &gt; 0.80?
     * We'll force success via seed=1 which gives 0.7309... still failing.
     * Simplest: seed=7 gives nextFloat() ≈ 0.129 &lt; 0.80 → success.
     * Verify isCarClamped() == false.
     * Verify CLIVE_TERMINAL removed from inventory.
     * Verify CriminalRecord does NOT contain CRIMINAL_DAMAGE.
     * Verify CLAMP_EVADER unlocked.
     */
    @Test
    void cliveTerminalSilentlyRemovesClamp() {
        // Use a seed that guarantees success (nextFloat < 0.80)
        TrafficWardenSystem system = new TrafficWardenSystem(new Random(7));
        system.setNotorietySystem(notorietySystem);
        system.setCriminalRecord(criminalRecord);
        system.setAchievementSystem(achievementSystem);

        // Force car into clamped state
        system.setCarClampedForTesting(true);
        system.setCarHasPCNForTesting(true);

        // Give player CLIVE_TERMINAL
        inventory.addItem(Material.CLIVE_TERMINAL, 1);
        int terminalCountBefore = inventory.getItemCount(Material.CLIVE_TERMINAL);
        assertEquals(1, terminalCountBefore, "Player should have 1 CLIVE_TERMINAL");

        // Attempt terminal removal
        TrafficWardenSystem.Result result = system.attemptTerminalRemoval(inventory);

        assertEquals(TrafficWardenSystem.Result.SUCCESS, result,
                "Terminal removal should succeed with seeded RNG (seed=7)");

        // Clamp removed
        assertFalse(system.isCarClamped(),
                "Car should no longer be clamped after successful terminal removal");

        // CLIVE_TERMINAL consumed
        assertEquals(0, inventory.getItemCount(Material.CLIVE_TERMINAL),
                "CLIVE_TERMINAL should be consumed (removed from inventory)");

        // No CRIMINAL_DAMAGE crime recorded
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.CRIMINAL_DAMAGE),
                "CriminalRecord should NOT contain CRIMINAL_DAMAGE for terminal removal");

        // CLAMP_EVADER achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.CLAMP_EVADER),
                "CLAMP_EVADER achievement should be unlocked after successful terminal removal");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: PCN appeal succeeds with full bonuses
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wednesday 09:00. Player has PENALTY_CHARGE_NOTICE and BLANK_PAPER.
     * Machine reported broken (+20%), clean record (+15%), BENEFIT_APPEAL_LETTER (+20%).
     * Total: 30+20+15+20 = 85% success. Seed TrafficWardenSystem(new Random(7)).
     * Submit appeal. Advance 2 in-game days.
     * Verify CIVIL_ENFORCEMENT_NOTICE count reduced by 1.
     * Verify player COIN increased by 8 (refund).
     * Verify PENALTY_CHARGE_NOTICE removed from inventory.
     * Verify BUREAUCRAT_BESTED unlocked.
     */
    @Test
    void pcnAppealSucceedsWithFullBonuses() {
        // Use seed=7 — first nextFloat() ≈ 0.129 < 0.85 → success
        TrafficWardenSystem system = new TrafficWardenSystem(new Random(7));
        system.setNotorietySystem(notorietySystem);
        system.setCriminalRecord(criminalRecord);
        system.setAchievementSystem(achievementSystem);

        // Record a CIVIL_ENFORCEMENT_NOTICE so we can verify reduction
        criminalRecord.record(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE);
        int pcnCountBefore = criminalRecord.getCount(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE);
        assertEquals(1, pcnCountBefore, "Should have 1 CIVIL_ENFORCEMENT_NOTICE before appeal");

        // Give player required items
        inventory.addItem(Material.PENALTY_CHARGE_NOTICE, 1);
        inventory.addItem(Material.BLANK_PAPER, 1);
        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Force car to have PCN
        system.setCarHasPCNForTesting(true);

        // Submit appeal with all bonuses
        boolean machineReported = true;
        boolean cleanRecord = true;
        boolean hasCABLetter = true; // holds BENEFIT_APPEAL_LETTER

        TrafficWardenSystem.Result submitResult =
                system.submitAppeal(inventory, machineReported, cleanRecord, hasCABLetter);

        assertEquals(TrafficWardenSystem.Result.SUCCESS, submitResult,
                "Appeal submission should succeed when player has PCN and BLANK_PAPER");

        // Advance 2 in-game days + a little extra to trigger resolution
        // 2 days = 2880 minutes; add 1 minute buffer
        system.update(2881f, 9.0f, TrafficWardenSystem.WEDNESDAY, Weather.CLEAR, playerCar, npcList);

        // Appeal resolved → success
        assertFalse(system.isCarHasPCN(),
                "Car should no longer have a PCN after successful appeal");

        // CIVIL_ENFORCEMENT_NOTICE count reduced by 1
        int pcnCountAfter = criminalRecord.getCount(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE);
        assertEquals(pcnCountBefore - 1, pcnCountAfter,
                "CIVIL_ENFORCEMENT_NOTICE count should be reduced by 1 after successful appeal");

        // COIN increased by PCN_FINE_COIN (refund)
        int coinsAfter = inventory.getItemCount(Material.COIN);
        assertEquals(coinsBefore + TrafficWardenSystem.PCN_FINE_COIN, coinsAfter,
                "Player should receive " + TrafficWardenSystem.PCN_FINE_COIN + " COIN refund on successful appeal");

        // PENALTY_CHARGE_NOTICE removed
        assertEquals(0, inventory.getItemCount(Material.PENALTY_CHARGE_NOTICE),
                "PENALTY_CHARGE_NOTICE should be removed from inventory after appeal resolves");

        // BUREAUCRAT_BESTED achievement
        assertTrue(achievementSystem.isUnlocked(AchievementType.BUREAUCRAT_BESTED),
                "BUREAUCRAT_BESTED achievement should be unlocked after winning appeal");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Clive absent in HEAVY_RAIN — no PCN issued
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tuesday 11:00. Weather set to RAIN (heavy rain).
     * Car parked without ticket. Advance 5 in-game minutes multiple times.
     * Verify Clive does NOT spawn (NPC list does not contain TRAFFIC_WARDEN).
     * Verify CriminalRecord does NOT contain CIVIL_ENFORCEMENT_NOTICE.
     * Verify isCliveOnDuty() == false.
     */
    @Test
    void cliveAbsentInHeavyRain_noPcnIssued() {
        int tuesday = TrafficWardenSystem.TUESDAY;
        float hour = 11.0f;

        // Advance multiple times with RAIN weather (simulating several game updates)
        for (int i = 0; i < 5; i++) {
            trafficWarden.update(5.0f, hour, tuesday, Weather.RAIN, playerCar, npcList);
        }

        // Clive should NOT be on duty
        assertFalse(trafficWarden.isCliveOnDuty(),
                "Clive should NOT be on duty in RAIN weather");

        // NPC list should NOT contain TRAFFIC_WARDEN
        boolean clivePresent = npcList.stream()
                .anyMatch(npc -> npc.getType() == NPCType.TRAFFIC_WARDEN);
        assertFalse(clivePresent,
                "NPC list should NOT contain TRAFFIC_WARDEN on a RAIN day");

        // No PCN issued
        assertFalse(trafficWarden.isCarHasPCN(),
                "Car should NOT have a PCN when Clive is absent");

        // No CIVIL_ENFORCEMENT_NOTICE in criminal record
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE),
                "CriminalRecord should NOT contain CIVIL_ENFORCEMENT_NOTICE when Clive is absent");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Car towed to impound; player retrieves it for 20 COIN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Thursday 09:30. CLEAR weather. Player has already received 2 PCNs (both ignored).
     * Car has existing WHEEL_CLAMP and PCN &gt;= 1 in-game day old.
     * Clive completes circuit → car towed to impound.
     * Verify isCarImpounded() == true.
     * Verify Notoriety increased by 5.
     * Player visits VEHICLE_IMPOUND with 20 COIN.
     * Player presses E on impound hatch.
     * Verify player COIN reduced by 20.
     * Verify isCarImpounded() == false.
     * Verify car is accessible (not impounded).
     */
    @Test
    void carTowedToImpoundThenRetrieved() {
        int thursday = TrafficWardenSystem.THURSDAY;
        float hour = 9.5f; // 09:30

        int notorietyBefore = notorietySystem.getNotoriety();
        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Pre-condition: car already has PCN and is clamped, PCN > 1 day old
        trafficWarden.setCarHasPCNForTesting(true);
        trafficWarden.setCarClampedForTesting(true);
        trafficWarden.setMinutesSincePCNForTesting(1440f + 1f); // > 1 day

        // Run one circuit — should trigger tow
        trafficWarden.update(8.0f, hour, thursday, Weather.CLEAR, playerCar, npcList);

        // Car should be impounded
        assertTrue(trafficWarden.isCarImpounded(),
                "Car should be impounded after third enforcement action");

        // Car should no longer be clamped
        assertFalse(trafficWarden.isCarClamped(),
                "Car should no longer be clamped after being towed");

        // Notoriety increased by IMPOUND amount
        assertEquals(notorietyBefore + TrafficWardenSystem.NOTORIETY_IMPOUND,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + TrafficWardenSystem.NOTORIETY_IMPOUND
                        + " when car is impounded");

        // ── Player retrieves car from impound ─────────────────────────────────
        TrafficWardenSystem.Result retrieveResult =
                trafficWarden.retrieveFromImpound(inventory, playerCar);

        assertEquals(TrafficWardenSystem.Result.SUCCESS, retrieveResult,
                "Car retrieval should succeed with sufficient COIN");

        // COIN reduced by 20
        int coinsAfter = inventory.getItemCount(Material.COIN);
        assertEquals(coinsBefore - TrafficWardenSystem.IMPOUND_RETRIEVAL_COIN, coinsAfter,
                "Player should pay " + TrafficWardenSystem.IMPOUND_RETRIEVAL_COIN + " COIN to retrieve car");

        // Car no longer impounded
        assertFalse(trafficWarden.isCarImpounded(),
                "Car should no longer be impounded after retrieval");

        // Car flag cleared
        assertFalse(playerCar.isImpounded(),
                "Car.isImpounded() should be false after retrieval");
    }
}
