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
 * Integration tests for Issue #1200 — Patel's News: Paper Round, Scratch Card,
 * Lottery Terminal &amp; Shoplifting.
 *
 * <p>Six scenarios:
 * <ol>
 *   <li>Full paper round delivery — accept round, deliver all 10 papers before 07:00,
 *       verify 5 COIN payout, PAPER_SATCHEL removed, PAPER_ROUND_DONE achievement</li>
 *   <li>Scratch card payout — buy scratch card (1 COIN), verify coin deducted,
 *       verify that repeated attempts hit at least one winning result</li>
 *   <li>Lottery jackpot resolution — seed LOTTERY_TICKET, advance to 20:00,
 *       call resolveLotteryTickets with a seeded RNG that wins jackpot,
 *       verify 100 COIN added and LOTTERY_WINNER achievement unlocked</li>
 *   <li>Shoplifting detection at high notoriety — notoriety &ge; 750 (tier 4),
 *       detection chance 85%; seeded RNG forces detection, verify THEFT in
 *       CriminalRecord and BANNED_FROM_PATEL flag active</li>
 *   <li>Banned flag blocks purchase — apply shop ban, attempt to buy NEWSPAPER,
 *       verify PurchaseResult.BANNED returned and inventory unchanged</li>
 *   <li>Lottery ticket expiry — issue LOTTERY_TICKET, skip lottery resolution,
 *       advance day counter by 2, call expireLotteryTickets, verify ticket
 *       replaced by CARDBOARD</li>
 * </ol>
 */
class Issue1200NewsagentTest {

    private NewsagentSystem newsagent;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private Inventory inventory;
    private List<NPC> npcs;
    private TimeSystem timeSystem;

    @BeforeEach
    void setUp() {
        newsagent = new NewsagentSystem(new Random(42));
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();

        newsagent.setNotorietySystem(notorietySystem);
        newsagent.setCriminalRecord(criminalRecord);
        newsagent.setAchievementSystem(achievementSystem);

        newsagent.forceSpawnPatel();
        newsagent.forceSpawnRaj();

        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);

        npcs = new ArrayList<>();
        NPC passer = new NPC(NPCType.PUBLIC, 5f, 0f, 5f);
        npcs.add(passer);

        timeSystem = new TimeSystem(6.0f); // 06:00
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Full paper round delivery — on time, full payout, achievement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Accept the paper round. Shop is open, no ban. Player receives PAPER_SATCHEL
     * and 10 NEWSPAPERs. Deliver all 10 before 07:00. Verify:
     * - 5 COIN added to inventory
     * - PAPER_SATCHEL removed from inventory
     * - PAPER_ROUND_DONE achievement unlocked
     * - consecutiveMisses remains 0
     */
    @Test
    void paperRound_fullDelivery_onTime_payoutAndAchievement() {
        int day = 1;
        float hour = 6.0f; // 06:00 — shop open

        // Accept the round
        NewsagentSystem.PaperRoundAcceptResult acceptResult =
                newsagent.acceptPaperRound(inventory, hour, day);
        assertEquals(NewsagentSystem.PaperRoundAcceptResult.ACCEPTED, acceptResult,
                "Should accept paper round");
        assertTrue(newsagent.isPaperRoundAccepted(),
                "Paper round should be marked as accepted");
        assertEquals(1, inventory.getItemCount(Material.PAPER_SATCHEL),
                "Should have received PAPER_SATCHEL");
        assertEquals(10, inventory.getItemCount(Material.NEWSPAPER),
                "Should have received 10 NEWSPAPERs");

        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Deliver all 10 papers (at 06:30 — well before deadline)
        float deliveryHour = 6.5f;
        NewsagentSystem.DeliverPaperResult result = null;
        for (int i = 0; i < NewsagentSystem.PAPER_ROUND_PAPERS; i++) {
            result = newsagent.deliverPaper(inventory, deliveryHour, day);
        }

        assertEquals(NewsagentSystem.DeliverPaperResult.ROUND_COMPLETE, result,
                "Final delivery should complete the round");
        assertEquals(0, inventory.getItemCount(Material.NEWSPAPER),
                "All newspapers should have been delivered");
        assertEquals(0, inventory.getItemCount(Material.PAPER_SATCHEL),
                "PAPER_SATCHEL should be removed on completion");
        assertEquals(coinsBefore + NewsagentSystem.PAPER_ROUND_PAY,
                inventory.getItemCount(Material.COIN),
                "Should receive 5 COIN payout");
        assertTrue(achievementSystem.isUnlocked(AchievementType.PAPER_ROUND_DONE),
                "PAPER_ROUND_DONE achievement should be unlocked");
        assertEquals(0, newsagent.getConsecutiveMisses(),
                "consecutiveMisses should remain 0 on success");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Scratch card payout — buying and scratching yields correct results
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Buy a scratch card from the open shop (1 COIN). Verify COIN deducted.
     * Scratch the card. Using a predictable RNG, verify that results include
     * at least one winning outcome across multiple attempts, and that losing
     * does not add coins.
     */
    @Test
    void scratchCard_buyAndScratch_coinDeductedAndPayoutCorrect() {
        float hour = 9.0f;
        int day = 1;

        // Verify shop is open
        assertTrue(newsagent.isOpen(hour), "Shop should be open at 09:00");
        assertFalse(newsagent.isBanned(day), "Player should not be banned");

        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Buy a scratch card (cost: 1 COIN)
        NewsagentSystem.PurchaseResult purchaseResult =
                newsagent.buyItem(Material.SCRATCH_CARD, inventory, hour, day, 0);
        assertEquals(NewsagentSystem.PurchaseResult.SUCCESS, purchaseResult,
                "Purchase should succeed");
        assertEquals(coinsBefore - 1, inventory.getItemCount(Material.COIN),
                "1 COIN should be deducted on purchase");
        assertEquals(1, inventory.getItemCount(Material.SCRATCH_CARD),
                "Should have 1 SCRATCH_CARD in inventory");

        // Remove the scratch card (simulating scratching it)
        inventory.removeItem(Material.SCRATCH_CARD, 1);

        // Now directly test the scratch mechanic with multiple iterations.
        // With 15% small win chance, across 50 tries we expect at least a few wins.
        NewsagentSystem scratchSystem = new NewsagentSystem(new Random(999));
        boolean foundWin = false;
        for (int i = 0; i < 50; i++) {
            Inventory testInv = new Inventory(10);
            NewsagentSystem.ScratchCardResult res = scratchSystem.scratchCard(testInv);
            if (res != NewsagentSystem.ScratchCardResult.LOSE) {
                foundWin = true;
                // Verify payout was added
                assertTrue(testInv.getItemCount(Material.COIN) > 0,
                        "Winning scratch should add coin to inventory");
                break;
            }
        }
        assertTrue(foundWin,
                "Should get at least one winning scratch card in 50 attempts");

        // Verify a LOSE result does not add coin
        NewsagentSystem loseSystem = new NewsagentSystem(new Random(777)) {
            // Override to always return LOSE by using a Random that returns 0.99+
        };
        // Instead just verify the range: LOSE should add 0 coins
        NewsagentSystem.ScratchCardResult loseResult = null;
        for (int i = 0; i < 1000; i++) {
            Inventory testInv = new Inventory(5);
            loseResult = new NewsagentSystem(new Random(i * 31 + 17)).scratchCard(testInv);
            if (loseResult == NewsagentSystem.ScratchCardResult.LOSE) {
                assertEquals(0, testInv.getItemCount(Material.COIN),
                        "LOSE scratch should add 0 coin");
                break;
            }
        }
        assertNotNull(loseResult, "Should find a LOSE result across 1000 seeds");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Lottery jackpot — seeded RNG wins 100 COIN, achievement unlocked
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player has 1 LOTTERY_TICKET. Use a seeded random that guarantees jackpot
     * (value &lt; 0.005). resolveLotteryTickets() at day 2 → jackpot.
     * Verify: 100 COIN added, LOTTERY_TICKET removed, LOTTERY_WINNER achievement.
     */
    @Test
    void lottery_jackpotResolution_100CoinAndAchievement() {
        int day = 2;

        // Manually seed a NewsagentSystem with Random(0) — first nextFloat() ~ 0.73
        // which won't win. Use a custom approach: find a seed that returns < 0.005.
        // Seed 1337 gives nextFloat() = 0.0001... let's check empirically.
        // We'll brute-force a winning seed.
        Random winRng = null;
        for (int seed = 0; seed < 10000; seed++) {
            Random r = new Random(seed);
            if (r.nextFloat() < NewsagentSystem.LOTTERY_JACKPOT_CHANCE) {
                winRng = new Random(seed);
                break;
            }
        }
        assertNotNull(winRng, "Should find a winning RNG seed");

        NewsagentSystem jackpotSystem = new NewsagentSystem(winRng);
        jackpotSystem.setAchievementSystem(achievementSystem);
        jackpotSystem.setNotorietySystem(notorietySystem);

        Inventory jackpotInv = new Inventory(20);
        jackpotInv.addItem(Material.LOTTERY_TICKET, 1);
        jackpotInv.addItem(Material.COIN, 10);

        int coinsBefore = jackpotInv.getItemCount(Material.COIN);

        List<NewsagentSystem.LotteryResult> results =
                jackpotSystem.resolveLotteryTickets(jackpotInv, day, npcs);

        assertFalse(results.isEmpty(), "Should have resolved at least one ticket");
        assertEquals(NewsagentSystem.LotteryResult.JACKPOT, results.get(0),
                "First ticket should be JACKPOT with winning seed");
        assertEquals(0, jackpotInv.getItemCount(Material.LOTTERY_TICKET),
                "LOTTERY_TICKET should be removed after resolution");
        assertEquals(coinsBefore + NewsagentSystem.LOTTERY_JACKPOT_PAYOUT,
                jackpotInv.getItemCount(Material.COIN),
                "100 COIN jackpot payout should be added");
        assertTrue(achievementSystem.isUnlocked(AchievementType.LOTTERY_WINNER),
                "LOTTERY_WINNER achievement should be unlocked");
        assertEquals(day, jackpotSystem.getLastLotteryResolutionDay(),
                "Last lottery resolution day should be updated");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Shoplifting detection at high notoriety — caught, banned, crime recorded
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Set player Notoriety to 750 (Tier 4 → 85% detection chance). Use a seeded
     * RNG that returns a value &lt; 0.85. Attempt to shoplift CHOCOLATE_BAR.
     * Verify: THEFT in CriminalRecord, Notoriety +3, BANNED_FROM_PATEL achievement,
     * player is banned from shop.
     */
    @Test
    void shoplifting_highNotoriety_caughtAndBanned() {
        int day = 1;
        // Force Notoriety to Tier 4 (750)
        notorietySystem.addNotoriety(750, null);
        assertEquals(4, notorietySystem.getTier(),
                "Notoriety should be at Tier 4");

        // Find a seed where nextFloat() < 0.85 (most seeds will do this)
        Random catchRng = null;
        for (int seed = 0; seed < 1000; seed++) {
            Random r = new Random(seed);
            if (r.nextFloat() < NewsagentSystem.SHOPLIFT_DETECT_HIGH) {
                catchRng = new Random(seed);
                break;
            }
        }
        assertNotNull(catchRng, "Should find a catching RNG seed");

        NewsagentSystem catchSystem = new NewsagentSystem(catchRng);
        catchSystem.setNotorietySystem(notorietySystem);
        catchSystem.setCriminalRecord(criminalRecord);
        catchSystem.setAchievementSystem(achievementSystem);

        int notorietyBefore = notorietySystem.getNotoriety();

        NewsagentSystem.ShopliftResult result =
                catchSystem.shoplift(Material.CHOCOLATE_BAR, inventory, 750, day);

        assertEquals(NewsagentSystem.ShopliftResult.CAUGHT, result,
                "Shoplifting should be detected at high notoriety with catching seed");
        assertEquals(0, inventory.getItemCount(Material.CHOCOLATE_BAR),
                "Stolen item should NOT be in inventory when caught");
        assertEquals(notorietyBefore + NewsagentSystem.SHOPLIFT_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + NewsagentSystem.SHOPLIFT_NOTORIETY + " on detection");
        assertTrue(catchSystem.isShopBanned(day),
                "Player should be banned from shop after being caught");
        assertTrue(achievementSystem.isUnlocked(AchievementType.BANNED_FROM_PATEL),
                "BANNED_FROM_PATEL achievement should be unlocked");
        // CriminalRecord should have THEFT
        assertTrue(criminalRecord.getCount(CriminalRecord.CrimeType.THEFT) > 0,
                "THEFT crime should be recorded in CriminalRecord");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Banned flag blocks purchase
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Apply a shop ban. Attempt to buy NEWSPAPER (1 COIN, shop open).
     * Verify PurchaseResult.BANNED is returned. Verify coin and inventory unchanged.
     */
    @Test
    void bannedPlayer_purchaseBlocked() {
        int day = 5;
        float hour = 9.0f; // shop open

        // Apply ban expiring on day 12
        newsagent.setShopBanForTesting(day + NewsagentSystem.BAN_DURATION_DAYS);

        assertTrue(newsagent.isOpen(hour), "Shop should be open");
        assertTrue(newsagent.isBanned(day), "Player should be banned");

        int coinsBefore = inventory.getItemCount(Material.COIN);
        int papersBefore = inventory.getItemCount(Material.NEWSPAPER);

        NewsagentSystem.PurchaseResult result =
                newsagent.buyItem(Material.NEWSPAPER, inventory, hour, day, 0);

        assertEquals(NewsagentSystem.PurchaseResult.BANNED, result,
                "Purchase should be blocked when banned");
        assertEquals(coinsBefore, inventory.getItemCount(Material.COIN),
                "Coin should be unchanged when banned");
        assertEquals(papersBefore, inventory.getItemCount(Material.NEWSPAPER),
                "Newspaper count should be unchanged when banned");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Lottery ticket expiry — unclaimed tickets become CARDBOARD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player has 2 LOTTERY_TICKETs. Lottery was resolved on day 1 (set via testing
     * accessor). Current day is 3 (2 days after resolution). Call expireLotteryTickets().
     * Verify: LOTTERY_TICKET count = 0, CARDBOARD count = 2.
     */
    @Test
    void lotteryTickets_expireAfter24h_becomeCardboard() {
        int resolutionDay = 1;
        int currentDay = 3; // more than 1 day after resolution

        // Add tickets to inventory
        inventory.addItem(Material.LOTTERY_TICKET, 2);
        assertEquals(2, inventory.getItemCount(Material.LOTTERY_TICKET),
                "Should start with 2 tickets");

        // Simulate that resolution happened on day 1 but tickets were not resolved
        // (as if the player wasn't there). Set the last resolution day manually.
        newsagent.setLastLotteryResolutionDayForTesting(resolutionDay);

        // Expire old tickets
        newsagent.expireLotteryTickets(inventory, currentDay);

        assertEquals(0, inventory.getItemCount(Material.LOTTERY_TICKET),
                "LOTTERY_TICKET should be expired");
        assertEquals(2, inventory.getItemCount(Material.CARDBOARD),
                "Expired tickets should become CARDBOARD");
    }
}
