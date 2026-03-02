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
import ragamuffin.world.PropType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1299 — ChuggerSystem end-to-end scenarios.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>Full donation flow</b>: Chugger intercepts player → player donates →
 *       COIN deducted, Notoriety reduced, CHUGGER_GOODWILL achievement unlocked,
 *       chugger returns to non-accosting state.</li>
 *   <li><b>Direct debit drain</b>: Player signs up → midnight ticks drain 1 COIN/day
 *       for 3 days → debit stops; STANDING_ORDER achievement unlocked.</li>
 *   <li><b>Punch flow with full integration</b>: Player punches chugger →
 *       FLEEING state, Notoriety +8, Wanted +1, CLIPBOARD_RAGE achievement,
 *       ASSAULT in criminal record.</li>
 *   <li><b>Fake tabard scam and fraud detection</b>: Player equips CHARITY_TABARD +
 *       CHARITY_CLIPBOARD, collects fake donations, triggers fraud near Tracy on 2nd
 *       suspicious collection → DIRECT_DEBIT_HUSTLE achievement, CHARITY_FRAUD crime,
 *       Notoriety increased, Wanted increased.</li>
 *   <li><b>CHARITY_CLIPBOARD_STAND_PROP exists with correct spec</b>: Verify the prop
 *       is defined with hitsToBreak=4 and drops CHARITY_CLIPBOARD.</li>
 * </ol>
 */
class Issue1299ChuggerBlitzIntegrationTest {

    private ChuggerSystem chuggerSystem;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private TimeSystem time;

    @BeforeEach
    void setUp() {
        // Use a seeded Random — seed 99 gives first nextFloat() ≈ 0.14 (< 0.60 accept)
        chuggerSystem = new ChuggerSystem(new Random(99));
        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);

        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(1));
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7));
        time = new TimeSystem(12.0f); // Tuesday 12:00 — within operating hours

        chuggerSystem.setAchievementSystem(achievements);
        chuggerSystem.setNotorietySystem(notorietySystem);
        chuggerSystem.setWantedSystem(wantedSystem);
        chuggerSystem.setCriminalRecord(criminalRecord);
        chuggerSystem.setRumourNetwork(rumourNetwork);
    }

    private NPC makeChugger() {
        return new NPC(NPCType.CHUGGER, "Chugger", 0, 0, 0);
    }

    private NPC makeTracy() {
        return new NPC(NPCType.CHUGGER_LEADER, "Tracy", 0, 0, 0);
    }

    // ── Integration Test 1: Full donation flow ────────────────────────────────

    /**
     * Full end-to-end donation scenario:
     * <ol>
     *   <li>Chugger patrol is active at 12:00 on a weekday.</li>
     *   <li>Chugger starts intercepting the player (ACCOSTING state).</li>
     *   <li>Player donates 2 COIN.</li>
     *   <li>2 COIN deducted from inventory.</li>
     *   <li>Notoriety reduced by 1.</li>
     *   <li>CHUGGER_GOODWILL achievement unlocked.</li>
     *   <li>Chugger is no longer accosting (state transitions to WANDERING).</li>
     * </ol>
     */
    @Test
    void fullDonationFlow() {
        // Seed some notoriety to make the reduction visible
        notorietySystem.addNotoriety(10, null);
        int notorietyBefore = notorietySystem.getNotoriety();
        int coinBefore = inventory.getItemCount(Material.COIN);

        NPC chugger = makeChugger();

        // Verify patrol active
        assertTrue(chuggerSystem.isPatrolActive(time),
            "Patrol should be active at 12:00 on a weekday");
        assertTrue(chuggerSystem.canApproach(Weather.CLEAR),
            "Chugger should be able to approach in clear weather");

        // Start intercept
        chuggerSystem.startIntercept(chugger);
        assertTrue(chuggerSystem.isPlayerBeingAccosted(), "Player should be accosted");
        assertEquals(NPCState.ACCOSTING, chugger.getState(), "Chugger should be ACCOSTING");

        // Donate
        ChuggerSystem.InteractionResult result = chuggerSystem.donate(inventory, chugger);
        assertEquals(ChuggerSystem.InteractionResult.DONATED, result, "Donation should succeed");

        // COIN deducted
        assertEquals(coinBefore - ChuggerSystem.DONATION_COIN_COST,
            inventory.getItemCount(Material.COIN),
            "Should have deducted " + ChuggerSystem.DONATION_COIN_COST + " COIN");

        // Notoriety reduced
        assertEquals(notorietyBefore - ChuggerSystem.DONATION_NOTORIETY_REDUCTION,
            notorietySystem.getNotoriety(),
            "Notoriety should be reduced by " + ChuggerSystem.DONATION_NOTORIETY_REDUCTION);

        // Achievement unlocked
        assertTrue(achievements.isUnlocked(AchievementType.CHUGGER_GOODWILL),
            "CHUGGER_GOODWILL should be unlocked");

        // Chugger no longer accosting
        assertFalse(chuggerSystem.isPlayerBeingAccosted(),
            "Player should not be accosted after donation");
        assertNotEquals(NPCState.ACCOSTING, chugger.getState(),
            "Chugger should no longer be in ACCOSTING state");
    }

    // ── Integration Test 2: Direct debit 3-day drain ─────────────────────────

    /**
     * Direct debit sign-up and 3-day drain:
     * <ol>
     *   <li>Chugger intercepts player; player signs up for direct debit.</li>
     *   <li>STANDING_ORDER achievement unlocked immediately.</li>
     *   <li>Each midnight tick deducts 1 COIN for 3 consecutive days.</li>
     *   <li>After 3 deductions, direct debit goes inactive; no more deductions.</li>
     * </ol>
     */
    @Test
    void directDebitDrainOverThreeDays() {
        NPC chugger = makeChugger();
        chuggerSystem.startIntercept(chugger);

        int coinBefore = inventory.getItemCount(Material.COIN);
        int startDay = time.getDayCount();

        // Sign up
        ChuggerSystem.InteractionResult result = chuggerSystem.signUpDirectDebit(inventory, chugger, time);
        assertEquals(ChuggerSystem.InteractionResult.SIGNED_UP_DIRECT_DEBIT, result);
        assertTrue(chuggerSystem.isDirectDebitActive(), "Direct debit should be active");
        assertTrue(achievements.isUnlocked(AchievementType.STANDING_ORDER),
            "STANDING_ORDER should unlock on sign-up");

        // Day 1 tick (same day — should NOT deduct)
        int deducted = chuggerSystem.onMidnightTick(inventory, startDay);
        assertEquals(0, deducted, "No deduction on same day as sign-up");

        // Day +1 tick
        deducted = chuggerSystem.onMidnightTick(inventory, startDay + 1);
        assertEquals(1, deducted, "Should deduct 1 COIN on first midnight tick");
        assertTrue(chuggerSystem.isDirectDebitActive(), "Still active after tick 1");

        // Day +2 tick
        deducted = chuggerSystem.onMidnightTick(inventory, startDay + 2);
        assertEquals(1, deducted, "Should deduct 1 COIN on second midnight tick");
        assertTrue(chuggerSystem.isDirectDebitActive(), "Still active after tick 2");

        // Day +3 tick — last deduction
        deducted = chuggerSystem.onMidnightTick(inventory, startDay + 3);
        assertEquals(1, deducted, "Should deduct 1 COIN on third midnight tick");
        assertFalse(chuggerSystem.isDirectDebitActive(), "Direct debit should be inactive after 3 payments");

        // Day +4 — no more deductions
        deducted = chuggerSystem.onMidnightTick(inventory, startDay + 4);
        assertEquals(0, deducted, "No deduction after direct debit ends");

        // Total: 3 COIN deducted
        assertEquals(coinBefore - 3, inventory.getItemCount(Material.COIN),
            "Total of 3 COIN should be deducted");
    }

    // ── Integration Test 3: Full punch flow with criminal consequences ─────────

    /**
     * Player punches chugger — full integration scenario:
     * <ol>
     *   <li>Chugger is accosting player.</li>
     *   <li>Player punches chugger → FLEEING state.</li>
     *   <li>Notoriety increases by 8.</li>
     *   <li>Wanted level increases by 1 star.</li>
     *   <li>CLIPBOARD_RAGE achievement unlocked.</li>
     *   <li>ASSAULT recorded in criminal record.</li>
     *   <li>Player is no longer accosted.</li>
     * </ol>
     */
    @Test
    void fullPunchFlowWithCriminalConsequences() {
        NPC chugger = makeChugger();
        chuggerSystem.startIntercept(chugger);

        int notorietyBefore = notorietySystem.getNotoriety();
        int wantedBefore = wantedSystem.getWantedStars();

        ChuggerSystem.InteractionResult result = chuggerSystem.punchChugger(chugger);
        assertEquals(ChuggerSystem.InteractionResult.PUNCHED, result);

        // NPC state
        assertEquals(NPCState.FLEEING, chugger.getState(), "Chugger should be FLEEING");

        // Notoriety
        assertEquals(notorietyBefore + ChuggerSystem.PUNCH_NOTORIETY_GAIN,
            notorietySystem.getNotoriety(),
            "Notoriety should increase by " + ChuggerSystem.PUNCH_NOTORIETY_GAIN);

        // Wanted stars
        assertEquals(wantedBefore + ChuggerSystem.PUNCH_WANTED_STARS,
            wantedSystem.getWantedStars(),
            "Wanted stars should increase by " + ChuggerSystem.PUNCH_WANTED_STARS);

        // Achievement
        assertTrue(achievements.isUnlocked(AchievementType.CLIPBOARD_RAGE),
            "CLIPBOARD_RAGE should be unlocked");

        // Criminal record
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.ASSAULT),
            "ASSAULT should be in criminal record");

        // No longer accosted
        assertFalse(chuggerSystem.isPlayerBeingAccosted(),
            "Player should not be accosted after punching");
    }

    // ── Integration Test 4: Fake tabard scam and fraud detection ──────────────

    /**
     * Fake tabard scam end-to-end:
     * <ol>
     *   <li>Player equips CHARITY_TABARD and CHARITY_CLIPBOARD.</li>
     *   <li>First collection near Tracy — not yet detected (1 suspicious interaction).</li>
     *   <li>Second collection near Tracy — fraud detected.</li>
     *   <li>Notoriety increases by 6.</li>
     *   <li>Wanted level increases by 1.</li>
     *   <li>CHARITY_FRAUD recorded in criminal record.</li>
     *   <li>DIRECT_DEBIT_HUSTLE achievement unlocked.</li>
     *   <li>Fraud flag set; no further fake collections possible.</li>
     * </ol>
     */
    @Test
    void fakeTabardScamAndFraudDetection() {
        inventory.addItem(Material.CHARITY_TABARD, 1);
        inventory.addItem(Material.CHARITY_CLIPBOARD, 1);
        NPC tracy = makeTracy();

        int notorietyBefore = notorietySystem.getNotoriety();
        int wantedBefore = wantedSystem.getWantedStars();

        assertFalse(chuggerSystem.isFraudDetected());
        assertFalse(achievements.isUnlocked(AchievementType.DIRECT_DEBIT_HUSTLE));
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.CHARITY_FRAUD));

        // First suspicious collection near Tracy — not yet fraud
        ChuggerSystem.FakeCollectionResult first = chuggerSystem.attemptFakeCollection(
            inventory, true, false, tracy);
        assertNotEquals(ChuggerSystem.FakeCollectionResult.FRAUD_DETECTED, first,
            "First suspicious collection should not immediately trigger fraud");
        assertFalse(chuggerSystem.isFraudDetected());

        // Second suspicious collection near Tracy — fraud triggered
        ChuggerSystem.FakeCollectionResult second = chuggerSystem.attemptFakeCollection(
            inventory, true, false, tracy);
        assertEquals(ChuggerSystem.FakeCollectionResult.FRAUD_DETECTED, second,
            "Second suspicious collection near Tracy should trigger fraud");

        // Fraud consequences
        assertTrue(chuggerSystem.isFraudDetected(), "Fraud flag should be set");
        assertEquals(notorietyBefore + ChuggerSystem.FRAUD_NOTORIETY_GAIN,
            notorietySystem.getNotoriety(),
            "Notoriety should increase by " + ChuggerSystem.FRAUD_NOTORIETY_GAIN + " on fraud");
        assertEquals(wantedBefore + ChuggerSystem.FRAUD_WANTED_STARS,
            wantedSystem.getWantedStars(),
            "Wanted stars should increase by " + ChuggerSystem.FRAUD_WANTED_STARS + " on fraud");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.CHARITY_FRAUD),
            "CHARITY_FRAUD should be in criminal record");
        assertTrue(achievements.isUnlocked(AchievementType.DIRECT_DEBIT_HUSTLE),
            "DIRECT_DEBIT_HUSTLE should be unlocked on fraud detection");
    }

    // ── Integration Test 5: CHARITY_CLIPBOARD_STAND_PROP spec ────────────────

    /**
     * Verify CHARITY_CLIPBOARD_STAND_PROP exists in PropType with correct specification:
     * <ul>
     *   <li>hitsToBreak = 4</li>
     *   <li>materialDrop = CHARITY_CLIPBOARD</li>
     *   <li>collisionWidth = 1.50f</li>
     *   <li>collisionHeight = 0.90f</li>
     * </ul>
     */
    @Test
    void charityClipboardStandPropHasCorrectSpec() {
        PropType prop = PropType.CHARITY_CLIPBOARD_STAND_PROP;

        assertNotNull(prop, "CHARITY_CLIPBOARD_STAND_PROP should exist");
        assertEquals(4, prop.getHitsToBreak(),
            "CHARITY_CLIPBOARD_STAND_PROP should require 4 hits to break");
        assertEquals(Material.CHARITY_CLIPBOARD, prop.getMaterialDrop(),
            "CHARITY_CLIPBOARD_STAND_PROP should drop CHARITY_CLIPBOARD");
        assertEquals(1.50f, prop.getCollisionWidth(), 0.001f,
            "CHARITY_CLIPBOARD_STAND_PROP collision width should be 1.50");
        assertEquals(0.90f, prop.getCollisionHeight(), 0.001f,
            "CHARITY_CLIPBOARD_STAND_PROP collision height should be 0.90");
    }
}
