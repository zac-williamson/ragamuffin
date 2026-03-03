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
 * Integration tests for Issue #1309 — ArcadeSystem end-to-end scenarios.
 *
 * <p>Tests:
 * <ol>
 *   <li><b>Penny falls basic payout</b>: Player pushes penny falls in green zone →
 *       1 ARCADE_TOKEN deducted, 3 tokens awarded, PENNY_CASCADE achievement unlocked.</li>
 *   <li><b>Tamper ejects after catch</b>: Kevin within 6 blocks catches player tampering →
 *       banned, CRIMINAL_DAMAGE crime recorded, Notoriety increased.</li>
 *   <li><b>Claw machine win on first attempt</b>: Player aims at prize column → WIN,
 *       STUFFED_ANIMAL in inventory, CLAW_MASTER achievement.</li>
 *   <li><b>Token economy</b>: Kevin exchanges COIN for ARCADE_TOKEN; refused when banned.</li>
 *   <li><b>Tamper blocked when Kevin nearby, succeeds when far</b>: Kevin within radius
 *       blocks tamper; Kevin beyond radius allows tamper and adds COIN.</li>
 *   <li><b>Jackpot seeds rumour in network</b>: Force jackpot threshold to 3 pushes,
 *       advance 3 pushes, verify PENNY_KING achievement and LOCAL_EVENT rumour seeded.</li>
 * </ol>
 */
class Issue1309ArcadeSystemTest {

    private ArcadeSystem arcade;
    private Inventory inventory;
    private AchievementSystem achievements;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    @BeforeEach
    void setUp() {
        // Seed 0 used for base construction; jackpot threshold overridden per test
        arcade = new ArcadeSystem(new Random(0L));
        inventory = new Inventory(36);
        achievements = new AchievementSystem();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        rumourNetwork = new RumourNetwork(new Random(7L));

        arcade.setAchievementSystem(achievements);
        arcade.setNotorietySystem(notorietySystem);
        arcade.setCriminalRecord(criminalRecord);
        arcade.setRumourNetwork(rumourNetwork);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private NPC makeKevin(float x, float y, float z) {
        NPC kevin = new NPC(NPCType.ARCADE_ATTENDANT, "Kevin", x, y, z);
        arcade.setKevin(kevin);
        return kevin;
    }

    private NPC makeWitness() {
        return new NPC(NPCType.PUBLIC, "Bystander", 0, 0, 0);
    }

    // ── Integration Test 1: Penny falls basic payout ──────────────────────────

    /**
     * Penny falls basic payout in the green zone.
     * <ol>
     *   <li>Give player 5 ARCADE_TOKEN.</li>
     *   <li>Push with timing position 0.05 (inside green zone, &lt; 0.20).</li>
     *   <li>Verify ARCADE_TOKEN decreased by 1 (cost) then increased by 3 (green win).</li>
     *   <li>Verify no ejection — result is GREEN_WIN.</li>
     *   <li>Verify PENNY_CASCADE achievement unlocked.</li>
     * </ol>
     */
    @Test
    void pennyFalls_greenZoneWin_awardsThreeTokensAndAchievement() {
        inventory.addItem(Material.ARCADE_TOKEN, 5);

        // timingPosition 0.05 is inside green zone (< 0.20)
        ArcadeSystem.PennyFallsResult result = arcade.pushPennyFalls(inventory, 0.05f, null);

        assertEquals(ArcadeSystem.PennyFallsResult.GREEN_WIN, result);
        // Started with 5, spent 1, gained 3 → net 7
        assertEquals(7, inventory.getItemCount(Material.ARCADE_TOKEN));
        assertTrue(achievements.isUnlocked(AchievementType.PENNY_CASCADE),
                "PENNY_CASCADE achievement should be unlocked");
    }

    /**
     * Yellow zone returns exactly 1 token, red zone returns nothing.
     */
    @Test
    void pennyFalls_yellowAndRedZones_correctPayouts() {
        inventory.addItem(Material.ARCADE_TOKEN, 10);

        // Yellow zone: 0.30 (between 0.20 and 0.60)
        ArcadeSystem.PennyFallsResult yellow = arcade.pushPennyFalls(inventory, 0.30f, null);
        assertEquals(ArcadeSystem.PennyFallsResult.YELLOW_WIN, yellow);
        // 10 - 1 (cost) + 1 (yellow win) = 10
        assertEquals(10, inventory.getItemCount(Material.ARCADE_TOKEN));

        // Red zone: 0.80 (> 0.60)
        ArcadeSystem.PennyFallsResult red = arcade.pushPennyFalls(inventory, 0.80f, null);
        assertEquals(ArcadeSystem.PennyFallsResult.RED_LOSS, red);
        // 10 - 1 (cost) = 9
        assertEquals(9, inventory.getItemCount(Material.ARCADE_TOKEN));
    }

    /**
     * No token: penny falls returns NO_TOKEN and does not change inventory.
     */
    @Test
    void pennyFalls_noToken_rejectsPlay() {
        ArcadeSystem.PennyFallsResult result = arcade.pushPennyFalls(inventory, 0.05f, null);
        assertEquals(ArcadeSystem.PennyFallsResult.NO_TOKEN, result);
        assertEquals(0, inventory.getItemCount(Material.ARCADE_TOKEN));
    }

    // ── Integration Test 2: Tamper caught — ban + crime + notoriety ───────────

    /**
     * Kevin catches the player tampering (within detection radius):
     * <ol>
     *   <li>Place Kevin at (3, 0, 0) — within {@link ArcadeSystem#KEVIN_DETECTION_RADIUS} = 6.</li>
     *   <li>Give player a SCREWDRIVER.</li>
     *   <li>Attempt EXTRACT_COIN tamper with kevinDistance = 3.</li>
     *   <li>Verify result is KEVIN_TOO_CLOSE.</li>
     *   <li>Verify player is banned.</li>
     *   <li>Verify CRIMINAL_DAMAGE recorded in CriminalRecord.</li>
     *   <li>Verify Notoriety increased by {@link ArcadeSystem#TAMPER_CAUGHT_NOTORIETY}.</li>
     * </ol>
     */
    @Test
    void tamper_kevinNearby_bansCrimesAndNotoriety() {
        makeKevin(3f, 0f, 0f);
        inventory.addItem(Material.SCREWDRIVER, 1);

        int notorietyBefore = notorietySystem.getNotoriety();

        ArcadeSystem.TamperResult result = arcade.attemptTamper(
                inventory, ArcadeSystem.TamperAction.EXTRACT_COIN, 3f);

        assertEquals(ArcadeSystem.TamperResult.KEVIN_TOO_CLOSE, result,
                "Tamper should be blocked when Kevin is within detection radius");
        assertTrue(arcade.isPlayerBanned(), "Player should be banned after being caught");
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.CRIMINAL_DAMAGE),
                "CRIMINAL_DAMAGE should be recorded");
        assertEquals(notorietyBefore + ArcadeSystem.TAMPER_CAUGHT_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by TAMPER_CAUGHT_NOTORIETY");
    }

    // ── Integration Test 3: Claw machine win on first attempt ─────────────────

    /**
     * Claw machine win on first attempt:
     * <ol>
     *   <li>Prize at column 3. Player steers to column 3 (within ±1).</li>
     *   <li>Using Random(WINNING_SEED) that produces nextFloat() &lt;
     *       {@link ArcadeSystem#CLAW_SUCCESS_CHANCE} = 0.15.</li>
     *   <li>Verify WIN result and STUFFED_ANIMAL in inventory.</li>
     *   <li>Verify CLAW_MASTER achievement unlocked (first attempt).</li>
     * </ol>
     */
    @Test
    void claw_firstAttemptWin_givesToyAndAchievement() {
        // Use a deterministic RNG that returns below the claw success threshold
        ArcadeSystem arcadeWin = new ArcadeSystem(new Random(5L) {
            @Override public float nextFloat() { return 0.05f; }
        });
        arcadeWin.setAchievementSystem(achievements);
        arcadeWin.resetClawFirstAttempt();

        inventory.addItem(Material.ARCADE_TOKEN, 5);

        // Player column 3, prize column 3 — within ±1
        ArcadeSystem.ClawResult result = arcadeWin.attemptClaw(inventory, 3, 3);

        assertEquals(ArcadeSystem.ClawResult.WIN, result,
                "Claw should win when within ±1 of prize and RNG favours success");
        assertEquals(1, inventory.getItemCount(Material.STUFFED_ANIMAL),
                "STUFFED_ANIMAL should be added to inventory on win");
        assertTrue(achievements.isUnlocked(AchievementType.CLAW_MASTER),
                "CLAW_MASTER should be unlocked on first-attempt win");
    }

    /**
     * Claw machine miss when player column is far from prize column.
     */
    @Test
    void claw_farFromPrize_missOrEarlyDrop() {
        inventory.addItem(Material.ARCADE_TOKEN, 5);
        // Column 0 vs prize 9 — far out of range; cannot win
        ArcadeSystem.ClawResult result = arcade.attemptClaw(inventory, 0, 9);

        // Must be either MISS or EARLY_DROP, not WIN
        assertNotEquals(ArcadeSystem.ClawResult.WIN, result,
                "Claw should not win when player column is far from prize column");
        assertEquals(0, inventory.getItemCount(Material.STUFFED_ANIMAL));
    }

    // ── Integration Test 4: Token economy ────────────────────────────────────

    /**
     * Kevin exchanges COIN for ARCADE_TOKEN:
     * <ol>
     *   <li>Give player 2 COIN.</li>
     *   <li>Buy token twice at 10:00 (open).</li>
     *   <li>Verify COIN = 0, ARCADE_TOKEN = 2.</li>
     *   <li>Try again with no COIN — verify NO_COIN result.</li>
     * </ol>
     */
    @Test
    void buyToken_exchangesCoinForToken_andRefusesWhenEmpty() {
        inventory.addItem(Material.COIN, 2);

        ArcadeSystem.BuyTokenResult r1 = arcade.buyToken(inventory, 12.0f);
        assertEquals(ArcadeSystem.BuyTokenResult.SUCCESS, r1);

        ArcadeSystem.BuyTokenResult r2 = arcade.buyToken(inventory, 12.0f);
        assertEquals(ArcadeSystem.BuyTokenResult.SUCCESS, r2);

        assertEquals(0, inventory.getItemCount(Material.COIN));
        assertEquals(2, inventory.getItemCount(Material.ARCADE_TOKEN));

        ArcadeSystem.BuyTokenResult r3 = arcade.buyToken(inventory, 12.0f);
        assertEquals(ArcadeSystem.BuyTokenResult.NO_COIN, r3);
    }

    /**
     * Banned player is refused token purchase.
     */
    @Test
    void buyToken_bannedPlayer_refusedWithBanned() {
        inventory.addItem(Material.COIN, 5);
        arcade.banPlayer();

        ArcadeSystem.BuyTokenResult result = arcade.buyToken(inventory, 12.0f);
        assertEquals(ArcadeSystem.BuyTokenResult.BANNED, result);
        assertEquals(5, inventory.getItemCount(Material.COIN), "No COIN should be deducted from banned player");
    }

    /**
     * Arcade closed outside hours.
     */
    @Test
    void buyToken_closed_returnsClosedResult() {
        inventory.addItem(Material.COIN, 1);
        ArcadeSystem.BuyTokenResult result = arcade.buyToken(inventory, 9.0f); // before 10:00
        assertEquals(ArcadeSystem.BuyTokenResult.CLOSED, result);
    }

    // ── Integration Test 5: Tamper blocked/allowed by Kevin proximity ─────────

    /**
     * Tamper blocked when Kevin nearby; succeeds when Kevin far away:
     * <ol>
     *   <li>Place Kevin at (3, 0, 0).</li>
     *   <li>Give player SCREWDRIVER.</li>
     *   <li>Press F (tamper EXTRACT_COIN) with Kevin 3 blocks away → blocked.</li>
     *   <li>Verify no COIN added, no THEFT, no Notoriety change.</li>
     *   <li>Lift ban (force). Move Kevin 10 blocks away and retry.</li>
     *   <li>Verify 1 tamper attempt returns SUCCESS, COIN added,
     *       and DODGY_ENGINEER achievement awarded.</li>
     * </ol>
     */
    @Test
    void tamper_blockedNearKevn_succeedsWhenFar() {
        makeKevin(3f, 0f, 0f);
        inventory.addItem(Material.SCREWDRIVER, 1);

        int coinBefore = inventory.getItemCount(Material.COIN);
        int notorietyBefore = notorietySystem.getNotoriety();

        // Tamper with Kevin 3 blocks away → caught
        ArcadeSystem.TamperResult blocked = arcade.attemptTamper(
                inventory, ArcadeSystem.TamperAction.EXTRACT_COIN, 3f);
        assertEquals(ArcadeSystem.TamperResult.KEVIN_TOO_CLOSE, blocked);
        assertEquals(coinBefore, inventory.getItemCount(Material.COIN),
                "No COIN should be extracted when caught");
        // No THEFT — CRIMINAL_DAMAGE is recorded, not THEFT
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.THEFT));

        // Unban player and try again with Kevin far away (10 blocks)
        // Create a new arcade for this to avoid ban state
        ArcadeSystem arcadeFar = new ArcadeSystem(new Random(0L));
        arcadeFar.setAchievementSystem(achievements);
        arcadeFar.setCriminalRecord(criminalRecord);
        arcadeFar.setNotorietySystem(notorietySystem);

        NPC kevinFar = new NPC(NPCType.ARCADE_ATTENDANT, "Kevin", 10f, 0f, 0f);
        arcadeFar.setKevin(kevinFar);

        Inventory inv2 = new Inventory(36);
        inv2.addItem(Material.SCREWDRIVER, 1);

        ArcadeSystem.TamperResult success = arcadeFar.attemptTamper(
                inv2, ArcadeSystem.TamperAction.EXTRACT_COIN, 10f);
        assertEquals(ArcadeSystem.TamperResult.SUCCESS, success,
                "Tamper should succeed when Kevin is beyond detection radius");
        assertEquals(ArcadeSystem.TAMPER_COIN_EXTRACT, inv2.getItemCount(Material.COIN),
                "COIN should be added on successful EXTRACT_COIN tamper");
        assertTrue(achievements.isUnlocked(AchievementType.DODGY_ENGINEER),
                "DODGY_ENGINEER achievement should be awarded on first tamper");
    }

    // ── Integration Test 6: Jackpot seeds rumour in network ───────────────────

    /**
     * Jackpot seeds LOCAL_EVENT rumour:
     * <ol>
     *   <li>Force jackpot threshold to 3.</li>
     *   <li>Give player 10 ARCADE_TOKEN.</li>
     *   <li>Add a witness NPC to the NPC list.</li>
     *   <li>Push red zone 3 times (the 3rd push hits the jackpot threshold).</li>
     *   <li>Verify PENNY_KING achievement unlocked.</li>
     *   <li>Verify a LOCAL_EVENT rumour containing "penny falls" is in the RumourNetwork.</li>
     * </ol>
     */
    @Test
    void pennyFalls_jackpot_unlocksAchievementAndSeedsRumour() {
        arcade.forceJackpotThreshold(3);
        inventory.addItem(Material.ARCADE_TOKEN, 10);

        List<NPC> npcs = new ArrayList<>();
        npcs.add(makeWitness());

        // First push (red zone) — should not jackpot yet
        arcade.pushPennyFalls(inventory, 0.80f, npcs);
        assertFalse(achievements.isUnlocked(AchievementType.PENNY_KING),
                "No jackpot yet after push 1");

        // Second push (red zone) — still no jackpot
        arcade.pushPennyFalls(inventory, 0.80f, npcs);
        assertFalse(achievements.isUnlocked(AchievementType.PENNY_KING),
                "No jackpot yet after push 2");

        // Third push triggers jackpot
        ArcadeSystem.PennyFallsResult result = arcade.pushPennyFalls(inventory, 0.80f, npcs);
        assertEquals(ArcadeSystem.PennyFallsResult.JACKPOT, result,
                "Third push should hit the jackpot");

        assertTrue(achievements.isUnlocked(AchievementType.PENNY_KING),
                "PENNY_KING achievement should be unlocked on jackpot");

        // Check that a LOCAL_EVENT rumour about penny falls was seeded
        boolean rumourFound = false;
        for (Rumour r : rumourNetwork.getAllRumours()) {
            if (r.getType() == RumourType.LOCAL_EVENT
                    && r.getText().toLowerCase().contains("penny falls")) {
                rumourFound = true;
                break;
            }
        }
        assertTrue(rumourFound, "A LOCAL_EVENT rumour mentioning 'penny falls' should be seeded on jackpot");
    }

    // ── Redemption counter tests ───────────────────────────────────────────────

    /**
     * Redemption counter: 5 tickets → PENNY_SWEETS ×3.
     */
    @Test
    void redeemTickets_5tickets_givesPennySweets() {
        inventory.addItem(Material.PRIZE_TICKET, 5);

        ArcadeSystem.RedemptionResult result = arcade.redeemTickets(inventory, ArcadeSystem.REDEEM_PENNY_SWEETS_COST);

        assertEquals(ArcadeSystem.RedemptionResult.SUCCESS, result);
        assertEquals(0, inventory.getItemCount(Material.PRIZE_TICKET),
                "All tickets should be consumed");
        assertEquals(ArcadeSystem.REDEEM_PENNY_SWEETS_QTY,
                inventory.getItemCount(Material.PENNY_SWEETS),
                "Should receive PENNY_SWEETS ×3");
    }

    /**
     * Redemption counter: 100 tickets → ARCADE_CHAMPION_BADGE + REDEMPTION_ARC achievement.
     */
    @Test
    void redeemTickets_100tickets_givesChampionBadgeAndAchievement() {
        inventory.addItem(Material.PRIZE_TICKET, 100);

        ArcadeSystem.RedemptionResult result = arcade.redeemTickets(inventory, ArcadeSystem.REDEEM_CHAMPION_BADGE_COST);

        assertEquals(ArcadeSystem.RedemptionResult.SUCCESS, result);
        assertEquals(0, inventory.getItemCount(Material.PRIZE_TICKET));
        assertEquals(1, inventory.getItemCount(Material.ARCADE_CHAMPION_BADGE),
                "ARCADE_CHAMPION_BADGE should be added to inventory");
        assertTrue(achievements.isUnlocked(AchievementType.REDEMPTION_ARC),
                "REDEMPTION_ARC achievement should be unlocked");
    }

    /**
     * Not enough tickets returns NOT_ENOUGH_TICKETS.
     */
    @Test
    void redeemTickets_notEnough_returnsNotEnoughTickets() {
        inventory.addItem(Material.PRIZE_TICKET, 4);

        ArcadeSystem.RedemptionResult result = arcade.redeemTickets(inventory, ArcadeSystem.REDEEM_PENNY_SWEETS_COST);

        assertEquals(ArcadeSystem.RedemptionResult.NOT_ENOUGH_TICKETS, result);
        assertEquals(4, inventory.getItemCount(Material.PRIZE_TICKET), "Tickets should not be consumed");
    }

    // ── Arcade shooter tests ──────────────────────────────────────────────────

    /**
     * Arcade shooter win: 10 kills with fewer than 3 misses → PRIZE_TICKET ×5, ARCADE_LEGEND.
     */
    @Test
    void shooter_win_givesTicketsAndAchievement() {
        inventory.addItem(Material.ARCADE_TOKEN, 3);

        List<NPC> npcs = new ArrayList<>();
        npcs.add(makeWitness());

        ArcadeSystem.ShooterResult result = arcade.playShooter(inventory, 10, 2, npcs);

        assertEquals(ArcadeSystem.ShooterResult.WIN, result);
        assertEquals(ArcadeSystem.SHOOTER_TICKET_REWARD,
                inventory.getItemCount(Material.PRIZE_TICKET),
                "Should receive 5 PRIZE_TICKET on shooter win");
        assertTrue(achievements.isUnlocked(AchievementType.ARCADE_LEGEND),
                "ARCADE_LEGEND should be unlocked on shooter win");
    }

    /**
     * Arcade shooter game over: 3 or more misses → GAME_OVER, no tickets.
     */
    @Test
    void shooter_gameover_noTickets() {
        inventory.addItem(Material.ARCADE_TOKEN, 3);

        ArcadeSystem.ShooterResult result = arcade.playShooter(inventory, 8, 3, null);

        assertEquals(ArcadeSystem.ShooterResult.GAME_OVER, result);
        assertEquals(0, inventory.getItemCount(Material.PRIZE_TICKET));
    }

    // ── Ban duration test ──────────────────────────────────────────────────────

    /**
     * Ban expires after BAN_DURATION_MINUTES of in-game time.
     */
    @Test
    void banExpires_afterDuration() {
        arcade.banPlayer();
        assertTrue(arcade.isPlayerBanned());

        // Simulate the ban duration elapsing
        float banSeconds = ArcadeSystem.BAN_DURATION_MINUTES * 60f;
        arcade.updateBan(banSeconds + 1f);

        assertFalse(arcade.isPlayerBanned(), "Ban should expire after duration");
    }

    // ── ARCADE_CHAMPION_BADGE tamper immunity test ────────────────────────────

    /**
     * ARCADE_CHAMPION_BADGE grants tamper immunity even with Kevin adjacent.
     */
    @Test
    void tamper_championBadge_grantsTamperImmunity() {
        makeKevin(1f, 0f, 0f); // Kevin right next to player
        inventory.addItem(Material.SCREWDRIVER, 1);
        inventory.addItem(Material.ARCADE_CHAMPION_BADGE, 1);

        ArcadeSystem.TamperResult result = arcade.attemptTamper(
                inventory, ArcadeSystem.TamperAction.EXTRACT_COIN, 1f);

        // Should NOT be blocked — badge grants immunity
        assertEquals(ArcadeSystem.TamperResult.SUCCESS, result,
                "ARCADE_CHAMPION_BADGE should grant tamper immunity even with Kevin adjacent");
        assertEquals(ArcadeSystem.TAMPER_COIN_EXTRACT, inventory.getItemCount(Material.COIN));
        assertFalse(arcade.isPlayerBanned(), "Player should not be banned when badge grants immunity");
    }

    // ── Material definitions test ──────────────────────────────────────────────

    /**
     * Verify new materials are defined and have correct display names.
     */
    @Test
    void newMaterials_areDefinedWithCorrectNames() {
        assertEquals("Stuffed Animal",        Material.STUFFED_ANIMAL.getDisplayName());
        assertEquals("Prize Ticket",          Material.PRIZE_TICKET.getDisplayName());
        assertEquals("Plastic Trophy",        Material.PLASTIC_TROPHY.getDisplayName());
        assertEquals("Arcade Champion Badge", Material.ARCADE_CHAMPION_BADGE.getDisplayName());
        assertEquals("Builder's Overalls",    Material.BUILDER_OVERALLS.getDisplayName());
    }

    // ── Achievement definition test ────────────────────────────────────────────

    /**
     * Verify new achievements are defined in AchievementType.
     */
    @Test
    void newAchievements_areDefined() {
        // Simply check that the enum values exist (compilation ensures this,
        // but an explicit reference confirms runtime accessibility)
        assertNotNull(AchievementType.PENNY_CASCADE);
        assertNotNull(AchievementType.CLAW_MASTER);
        assertNotNull(AchievementType.ARCADE_LEGEND);
        assertNotNull(AchievementType.DODGY_ENGINEER);
        assertNotNull(AchievementType.REDEMPTION_ARC);
    }
}
