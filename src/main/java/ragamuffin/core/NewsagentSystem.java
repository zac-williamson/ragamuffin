package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1200: Patel's News — Paper Round, Lottery Terminal &amp; the Dodgy Magazine
 * Under the Counter.
 *
 * <h3>Stock</h3>
 * <ul>
 *   <li>{@link Material#NEWSPAPER} — 1 COIN</li>
 *   <li>{@link Material#SCRATCH_CARD} — 1 COIN; instant payout 15% (2 COIN), 5% (5 COIN), 1% (10 COIN)</li>
 *   <li>{@link Material#LOTTERY_TICKET} — 1 COIN; resolved at 20:00 daily</li>
 *   <li>{@link Material#CHEWING_GUM} — 1 COIN</li>
 *   <li>{@link Material#PHONE_CREDIT_VOUCHER} — 3 COIN</li>
 *   <li>{@link Material#CHOCOLATE_BAR} — 1 COIN</li>
 *   <li>{@link Material#CRISPS} — 1 COIN</li>
 *   <li>{@link Material#ENERGY_DRINK} — 2 COIN</li>
 *   <li>{@link Material#TOBACCO_POUCH} — 3 COIN</li>
 *   <li>{@link Material#DIY_MONTHLY} — 2 COIN</li>
 *   <li>{@link Material#DODGY_MAGAZINE} — 3 COIN; only sold when Notoriety &lt; 30</li>
 * </ul>
 *
 * <h3>NPCs</h3>
 * <ul>
 *   <li>Patel ({@code SHOPKEEPER}) — 06:00–20:00</li>
 *   <li>Raj (nephew) ({@code SHOPKEEPER}) — 20:00–23:00</li>
 *   <li>Paper Round Kid ({@code SCHOOL_KID}) — at 05:30</li>
 *   <li>Morning queue of PENSIONER/PUBLIC NPCs 06:00–08:00</li>
 * </ul>
 *
 * <h3>Paper Round</h3>
 * <ul>
 *   <li>Accept from {@code NEWSAGENT_NOTICE_BOARD_PROP} the evening before.</li>
 *   <li>Receive {@link Material#PAPER_SATCHEL} + 10 {@link Material#NEWSPAPER}s.</li>
 *   <li>Deliver to 10 {@code LETTERBOX_PROP} targets on terraced houses by 07:00.</li>
 *   <li>Success: 5 COIN + Notoriety −1 + {@link AchievementType#PAPER_ROUND_DONE}.</li>
 *   <li>Miss deadline: −2 COIN. Three misses = 7-day ban.</li>
 *   <li>Bundle theft: {@link CrimeType#CRIMINAL_DAMAGE} + Notoriety +4 + BANNED_FROM_PATEL.</li>
 * </ul>
 *
 * <h3>Lottery Resolution</h3>
 * <ul>
 *   <li>At 20:00 daily, all {@link Material#LOTTERY_TICKET} items are resolved.</li>
 *   <li>Jackpot (0.5%): 100 COIN + NewspaperSystem headline.</li>
 *   <li>Match-3 (8%): 5 COIN.</li>
 *   <li>Unclaimed after 24 hours becomes {@link Material#CARDBOARD}.</li>
 * </ul>
 *
 * <h3>Notice Board</h3>
 * <ul>
 *   <li>Rotates daily — Paper Round job card, Lost Cat fetch quest (5 COIN),
 *       random {@link BuildingQuestRegistry} odd-job pointer.</li>
 * </ul>
 *
 * <h3>Shoplifting Detection</h3>
 * <ul>
 *   <li>Detection rate scales with Notoriety tier: 25% / 50% / 85%.</li>
 *   <li>Detected: {@link CrimeType#THEFT} in CriminalRecord, Notoriety +3,
 *       BANNED_FROM_PATEL 7 days.</li>
 * </ul>
 *
 * <h3>Burglary</h3>
 * <ul>
 *   <li>{@link Material#NEWSAGENT_KEY} opens the back-office lockbox at night.</li>
 *   <li>Rewards: 8–14 COIN. Crime: {@link CrimeType#THEFT} + Notoriety +4.</li>
 * </ul>
 */
public class NewsagentSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour Patel opens the shop (06:00). */
    public static final float OPEN_HOUR = 6.0f;

    /** Hour Patel hands over to Raj (20:00). */
    public static final float RAJ_START_HOUR = 20.0f;

    /** Hour the shop closes (23:00). */
    public static final float CLOSE_HOUR = 23.0f;

    /** Hour the Paper Round Kid arrives (05:30). */
    public static final float PAPER_ROUND_KID_HOUR = 5.5f;

    /** Hour the paper round must be completed by (07:00). */
    public static final float PAPER_ROUND_DEADLINE = 7.0f;

    /** Hour lottery tickets are resolved daily (20:00). */
    public static final float LOTTERY_RESOLUTION_HOUR = 20.0f;

    // ── Item prices ────────────────────────────────────────────────────────────

    public static final int NEWSPAPER_PRICE       = 1;
    public static final int SCRATCH_CARD_PRICE    = 1;
    public static final int LOTTERY_TICKET_PRICE  = 1;
    public static final int CHEWING_GUM_PRICE     = 1;
    public static final int PHONE_CREDIT_PRICE    = 3;
    public static final int CHOCOLATE_BAR_PRICE   = 1;
    public static final int CRISPS_PRICE          = 1;
    public static final int ENERGY_DRINK_PRICE    = 2;
    public static final int TOBACCO_POUCH_PRICE   = 3;
    public static final int DIY_MONTHLY_PRICE     = 2;
    public static final int DODGY_MAGAZINE_PRICE  = 3;

    // ── Scratch card odds ──────────────────────────────────────────────────────

    /** Chance (0–1) of 10 COIN prize (top tier). */
    public static final float SCRATCH_JACKPOT_CHANCE = 0.01f;

    /** Chance (0–1) of 5 COIN prize (mid tier). */
    public static final float SCRATCH_MID_CHANCE     = 0.05f;

    /** Chance (0–1) of 2 COIN prize (small tier). */
    public static final float SCRATCH_SMALL_CHANCE   = 0.15f;

    /** Top scratch card payout (COIN). */
    public static final int SCRATCH_JACKPOT_PAYOUT  = 10;

    /** Mid scratch card payout (COIN). */
    public static final int SCRATCH_MID_PAYOUT      = 5;

    /** Small scratch card payout (COIN). */
    public static final int SCRATCH_SMALL_PAYOUT    = 2;

    // ── Lottery odds & payouts ─────────────────────────────────────────────────

    /** Chance (0–1) of jackpot. */
    public static final float LOTTERY_JACKPOT_CHANCE   = 0.005f;

    /** Jackpot payout (COIN). */
    public static final int   LOTTERY_JACKPOT_PAYOUT   = 100;

    /** Chance (0–1) of match-3 payout. */
    public static final float LOTTERY_MATCH3_CHANCE    = 0.08f;

    /** Match-3 payout (COIN). */
    public static final int   LOTTERY_MATCH3_PAYOUT    = 5;

    // ── Paper round ────────────────────────────────────────────────────────────

    /** Number of newspapers to deliver per paper round. */
    public static final int   PAPER_ROUND_PAPERS       = 10;

    /** Payout for completing a paper round on time (COIN). */
    public static final int   PAPER_ROUND_PAY          = 5;

    /** Penalty for missing the paper round deadline (COIN). */
    public static final int   PAPER_ROUND_MISS_PENALTY = 2;

    /** Number of consecutive misses before a 7-day ban. */
    public static final int   PAPER_ROUND_MISS_BAN     = 3;

    /** Duration of paper-round miss ban (in-game days). */
    public static final int   PAPER_ROUND_BAN_DAYS     = 7;

    /** Notoriety reduction on successful paper round. */
    public static final int   PAPER_ROUND_NOTORIETY_REDUCTION = 1;

    /** Notoriety added for stealing a newspaper bundle. */
    public static final int   BUNDLE_THEFT_NOTORIETY   = 4;

    /** GRAFTING XP awarded per completed paper round. */
    public static final int   PAPER_ROUND_GRAFTING_XP  = 20;

    /** Neighbourhood Vibes boost per completed paper round per day. */
    public static final float PAPER_ROUND_VIBES_BONUS  = 0.5f;

    // ── Shoplifting detection rates ────────────────────────────────────────────

    /** Detection rate at Notoriety Tier 0–1 (25%). */
    public static final float SHOPLIFT_DETECT_LOW      = 0.25f;

    /** Detection rate at Notoriety Tier 2–3 (50%). */
    public static final float SHOPLIFT_DETECT_MID      = 0.50f;

    /** Detection rate at Notoriety Tier 4–5 (85%). */
    public static final float SHOPLIFT_DETECT_HIGH     = 0.85f;

    /** Notoriety added on shoplifting detection. */
    public static final int   SHOPLIFT_NOTORIETY       = 3;

    /** Duration of BANNED_FROM_PATEL flag (days). */
    public static final int   BAN_DURATION_DAYS        = 7;

    // ── Burglary ───────────────────────────────────────────────────────────────

    /** Minimum cash box contents (COIN). */
    public static final int   CASH_BOX_MIN             = 8;

    /** Maximum cash box contents (COIN). */
    public static final int   CASH_BOX_MAX             = 14;

    /** Notoriety added for cash box raid. */
    public static final int   BURGLARY_NOTORIETY       = 4;

    // ── Speech lines ───────────────────────────────────────────────────────────

    public static final String PATEL_GREETING             = "Morning. What can I get you?";
    public static final String RAJ_GREETING               = "Alright. Shop's still open, yeah.";
    public static final String CLOSED_MESSAGE             = "We're closed. Come back when we're open.";
    public static final String NO_COIN                    = "You haven't got enough, love.";
    public static final String DODGY_MAG_REFUSED          = "I can't serve you that. Come back when you're better known.";
    public static final String BANNED_MESSAGE             = "You're barred. Don't come in here.";
    public static final String PAPER_ROUND_ACCEPT         = "Right then — here's your satchel and papers. Back by seven, yeah?";
    public static final String PAPER_ROUND_SUCCESS        = "Good lad. On time as well. Here's your fiver.";
    public static final String PAPER_ROUND_MISSED         = "You're late. Again. This isn't good enough.";
    public static final String PAPER_ROUND_BANNED         = "You've let me down too many times. I've given the round to someone else.";
    public static final String LOTTERY_JACKPOT_MSG        = "By 'eck — you've won the jackpot! Patel looks stunned.";
    public static final String LOTTERY_MATCH3_MSG         = "Three numbers! Five coin. Not bad.";
    public static final String LOTTERY_LOSE_MSG           = "Not this time. Better luck next draw.";
    public static final String SHOPLIFT_CAUGHT            = "Oi! Put that back! Get out and don't come back!";
    public static final String BURGLARY_SUCCESS           = "Cash box emptied. Raj will blame the till float.";

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Patel NPC (null if not spawned). */
    private NPC patel = null;

    /** Raj NPC (null if not spawned). */
    private NPC raj = null;

    /** Paper Round Kid NPC (null if not spawned). */
    private NPC paperRoundKid = null;

    /** Whether the player has accepted today's paper round. */
    private boolean paperRoundAccepted = false;

    /** Number of newspapers delivered in the current paper round. */
    private int papersDelivered = 0;

    /** Day the paper round was accepted. */
    private int paperRoundAcceptDay = -1;

    /** Consecutive paper round misses. */
    private int consecutiveMisses = 0;

    /** Day number when the ban expires (-1 = not banned). */
    private int banExpiryDay = -1;

    /** Whether the player is banned from Patel's News. */
    private boolean bannedFromPatel = false;

    /** Day number when BANNED_FROM_PATEL expires (-1 = not banned). */
    private int shopBanExpiryDay = -1;

    /** Whether today's lottery has been resolved. */
    private int lastLotteryResolutionDay = -1;

    /** Whether the back-office cash box has been raided this in-game day. */
    private int lastBurglaryDay = -1;

    /** Whether the first paper round has been done (for achievement). */
    private boolean firstPaperRoundDone = false;

    /** Total purchase count (for RAJS_FAVOURITE loyalty). */
    private int totalPurchaseCount = 0;

    /** Whether RAJS_FAVOURITE has been awarded. */
    private boolean rajsFavouriteAwarded = false;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private NewspaperSystem newspaperSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private StreetSkillSystem streetSkillSystem;
    private WeatherSystem weatherSystem;

    // ── Construction ───────────────────────────────────────────────────────────

    public NewsagentSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    public void setWeatherSystem(WeatherSystem weatherSystem) {
        this.weatherSystem = weatherSystem;
    }

    // ── NPC management ─────────────────────────────────────────────────────────

    /** Force-spawn Patel for testing. */
    public void forceSpawnPatel() {
        patel = new NPC(NPCType.SHOPKEEPER, 0f, 0f, 0f);
        patel.setName("Patel");
    }

    /** Force-spawn Raj for testing. */
    public void forceSpawnRaj() {
        raj = new NPC(NPCType.SHOPKEEPER, 0f, 0f, 0f);
        raj.setName("Raj");
    }

    /** Force-spawn Paper Round Kid for testing. */
    public void forceSpawnPaperRoundKid() {
        paperRoundKid = new NPC(NPCType.SCHOOL_KID, 0f, 0f, 0f);
        paperRoundKid.setName("Paper Round Kid");
    }

    /** Returns Patel NPC (may be null). */
    public NPC getPatel() { return patel; }

    /** Returns Raj NPC (may be null). */
    public NPC getRaj() { return raj; }

    /** Returns Paper Round Kid NPC (may be null). */
    public NPC getPaperRoundKid() { return paperRoundKid; }

    // ── Opening hours ──────────────────────────────────────────────────────────

    /**
     * Returns true if Patel's News is open at the given hour (06:00–23:00 daily).
     */
    public boolean isOpen(float hour) {
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if Patel is on shift (06:00–20:00).
     */
    public boolean isPatelOnShift(float hour) {
        return hour >= OPEN_HOUR && hour < RAJ_START_HOUR;
    }

    /**
     * Returns true if Raj is on shift (20:00–23:00).
     */
    public boolean isRajOnShift(float hour) {
        return hour >= RAJ_START_HOUR && hour < CLOSE_HOUR;
    }

    // ── Shop ban check ─────────────────────────────────────────────────────────

    /**
     * Returns true if the player is currently banned from the shop.
     *
     * @param currentDay current in-game day number
     */
    public boolean isBanned(int currentDay) {
        if (shopBanExpiryDay >= 0 && currentDay < shopBanExpiryDay) {
            return true;
        }
        if (shopBanExpiryDay >= 0 && currentDay >= shopBanExpiryDay) {
            shopBanExpiryDay = -1;
            bannedFromPatel = false;
        }
        return false;
    }

    // ── Purchase items ─────────────────────────────────────────────────────────

    /**
     * Result of a purchase attempt.
     */
    public enum PurchaseResult {
        SUCCESS,
        CLOSED,
        NO_COIN,
        BANNED,
        REFUSED
    }

    /**
     * Player buys an item from the counter.
     *
     * @param item       the item to purchase
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param currentDay current in-game day number
     * @param notoriety  current player notoriety (0–1000)
     */
    public PurchaseResult buyItem(Material item, Inventory inventory,
                                  float hour, int currentDay, int notoriety) {
        if (!isOpen(hour)) {
            return PurchaseResult.CLOSED;
        }
        if (isBanned(currentDay)) {
            return PurchaseResult.BANNED;
        }

        // Dodgy magazine: only sold at Notoriety < 30
        if (item == Material.DODGY_MAGAZINE) {
            if (notoriety >= 30) {
                return PurchaseResult.REFUSED;
            }
        }

        int price = getPrice(item);
        if (inventory.getItemCount(Material.COIN) < price) {
            return PurchaseResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, price);
        inventory.addItem(item, 1);

        totalPurchaseCount++;
        checkRajsFavourite();

        // Achievement: DODGY_MAG_BUYER
        if (item == Material.DODGY_MAGAZINE && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DODGY_MAG_BUYER);
        }

        return PurchaseResult.SUCCESS;
    }

    /**
     * Returns the shop price for an item (0 if not sold here).
     */
    public int getPrice(Material item) {
        switch (item) {
            case NEWSPAPER:        return NEWSPAPER_PRICE;
            case SCRATCH_CARD:     return SCRATCH_CARD_PRICE;
            case LOTTERY_TICKET:   return LOTTERY_TICKET_PRICE;
            case CHEWING_GUM:      return CHEWING_GUM_PRICE;
            case PHONE_CREDIT_VOUCHER: return PHONE_CREDIT_PRICE;
            case CHOCOLATE_BAR:    return CHOCOLATE_BAR_PRICE;
            case CRISPS:           return CRISPS_PRICE;
            case ENERGY_DRINK:     return ENERGY_DRINK_PRICE;
            case TOBACCO_POUCH:    return TOBACCO_POUCH_PRICE;
            case DIY_MONTHLY:      return DIY_MONTHLY_PRICE;
            case DODGY_MAGAZINE:   return DODGY_MAGAZINE_PRICE;
            default:               return 0;
        }
    }

    // ── Scratch card ───────────────────────────────────────────────────────────

    /**
     * Result of scratching a card.
     */
    public enum ScratchCardResult {
        JACKPOT,   // 10 COIN
        MID_WIN,   // 5 COIN
        SMALL_WIN, // 2 COIN
        LOSE
    }

    /**
     * Scratch the card (after purchasing). Resolves instantly.
     *
     * @param inventory player's inventory (receives payout)
     */
    public ScratchCardResult scratchCard(Inventory inventory) {
        float roll = random.nextFloat();
        if (roll < SCRATCH_JACKPOT_CHANCE) {
            inventory.addItem(Material.COIN, SCRATCH_JACKPOT_PAYOUT);
            return ScratchCardResult.JACKPOT;
        } else if (roll < SCRATCH_JACKPOT_CHANCE + SCRATCH_MID_CHANCE) {
            inventory.addItem(Material.COIN, SCRATCH_MID_PAYOUT);
            return ScratchCardResult.MID_WIN;
        } else if (roll < SCRATCH_JACKPOT_CHANCE + SCRATCH_MID_CHANCE + SCRATCH_SMALL_CHANCE) {
            inventory.addItem(Material.COIN, SCRATCH_SMALL_PAYOUT);
            return ScratchCardResult.SMALL_WIN;
        }
        return ScratchCardResult.LOSE;
    }

    // ── Lottery ────────────────────────────────────────────────────────────────

    /**
     * Result of a lottery ticket resolution.
     */
    public enum LotteryResult {
        JACKPOT,   // 100 COIN
        MATCH3,    // 5 COIN
        LOSE,
        EXPIRED    // ticket > 24h old, becomes CARDBOARD
    }

    /**
     * Resolve all LOTTERY_TICKET items in the player's inventory (called at 20:00 daily).
     * Jackpot triggers a NewspaperSystem headline. Unclaimed tickets &gt;24 hours become CARDBOARD.
     *
     * @param inventory  player's inventory
     * @param currentDay current in-game day number (ticket purchased the previous day or more)
     * @param npcs       all active NPCs (for rumour seeding)
     */
    public List<LotteryResult> resolveLotteryTickets(Inventory inventory,
                                                      int currentDay,
                                                      List<NPC> npcs) {
        List<LotteryResult> results = new ArrayList<>();
        int ticketCount = inventory.getItemCount(Material.LOTTERY_TICKET);
        if (ticketCount <= 0) {
            return results;
        }

        for (int i = 0; i < ticketCount; i++) {
            float roll = random.nextFloat();
            LotteryResult result;
            if (roll < LOTTERY_JACKPOT_CHANCE) {
                result = LotteryResult.JACKPOT;
                inventory.addItem(Material.COIN, LOTTERY_JACKPOT_PAYOUT);
                // Trigger newspaper headline
                if (newspaperSystem != null) {
                    newspaperSystem.publishHeadline(
                            "LOCAL WINS LOTTERY AT PATEL'S NEWS — TAKES HOME " + LOTTERY_JACKPOT_PAYOUT + " COIN");
                }
                // Seed rumour
                if (rumourNetwork != null && npcs != null && !npcs.isEmpty()) {
                    NPC target = findAnyNPC(npcs);
                    if (target != null) {
                        rumourNetwork.addRumour(target,
                                new Rumour(RumourType.BIG_WIN_AT_BOOKIES,
                                        "Someone just won the lottery at Patel's. A hundred coin!"));
                    }
                }
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.LOTTERY_WINNER);
                }
            } else if (roll < LOTTERY_JACKPOT_CHANCE + LOTTERY_MATCH3_CHANCE) {
                result = LotteryResult.MATCH3;
                inventory.addItem(Material.COIN, LOTTERY_MATCH3_PAYOUT);
            } else {
                result = LotteryResult.LOSE;
            }
            results.add(result);
        }

        // Remove all lottery tickets
        inventory.removeItem(Material.LOTTERY_TICKET, ticketCount);
        lastLotteryResolutionDay = currentDay;

        return results;
    }

    /**
     * Called on day rollover to expire lottery tickets older than 24 hours.
     * Replaces LOTTERY_TICKET items with CARDBOARD.
     *
     * @param inventory  player's inventory
     * @param currentDay current in-game day number
     */
    public void expireLotteryTickets(Inventory inventory, int currentDay) {
        // Tickets not resolved by the previous day's 20:00 become CARDBOARD
        if (lastLotteryResolutionDay >= 0 && currentDay > lastLotteryResolutionDay + 1) {
            int count = inventory.getItemCount(Material.LOTTERY_TICKET);
            if (count > 0) {
                inventory.removeItem(Material.LOTTERY_TICKET, count);
                inventory.addItem(Material.CARDBOARD, count);
            }
        }
    }

    // ── Paper round ────────────────────────────────────────────────────────────

    /**
     * Result of accepting the paper round from the notice board.
     */
    public enum PaperRoundAcceptResult {
        ACCEPTED,
        BANNED,
        ALREADY_ACCEPTED,
        OUTSIDE_WINDOW  // Can only accept the evening before
    }

    /**
     * Player accepts the paper round job from the NEWSAGENT_NOTICE_BOARD_PROP.
     *
     * @param inventory  player's inventory (receives PAPER_SATCHEL + 10 NEWSPAPERS)
     * @param hour       current in-game hour
     * @param currentDay current in-game day number
     */
    public PaperRoundAcceptResult acceptPaperRound(Inventory inventory,
                                                    float hour,
                                                    int currentDay) {
        // Check for paper-round ban
        if (banExpiryDay >= 0 && currentDay < banExpiryDay) {
            return PaperRoundAcceptResult.BANNED;
        }
        if (banExpiryDay >= 0 && currentDay >= banExpiryDay) {
            banExpiryDay = -1;
            consecutiveMisses = 0;
        }

        if (paperRoundAccepted && paperRoundAcceptDay == currentDay) {
            return PaperRoundAcceptResult.ALREADY_ACCEPTED;
        }

        // Accept the round (can be accepted any time the shop is open the evening before,
        // or in the morning). We do not restrict the window beyond that.
        paperRoundAccepted = true;
        papersDelivered = 0;
        paperRoundAcceptDay = currentDay;

        inventory.addItem(Material.PAPER_SATCHEL, 1);
        inventory.addItem(Material.NEWSPAPER, PAPER_ROUND_PAPERS);

        return PaperRoundAcceptResult.ACCEPTED;
    }

    /**
     * Result of delivering one newspaper to a LETTERBOX_PROP target.
     */
    public enum DeliverPaperResult {
        DELIVERED,
        ROUND_COMPLETE,    // All 10 delivered on time — pay awarded
        ROUND_MISSED,      // Delivered but after deadline
        NO_ROUND_ACTIVE,
        NO_NEWSPAPER
    }

    /**
     * Deliver one newspaper to a LETTERBOX_PROP target.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param currentDay current in-game day number
     */
    public DeliverPaperResult deliverPaper(Inventory inventory, float hour, int currentDay) {
        if (!paperRoundAccepted || paperRoundAcceptDay != currentDay) {
            return DeliverPaperResult.NO_ROUND_ACTIVE;
        }
        if (!inventory.hasItem(Material.NEWSPAPER)) {
            return DeliverPaperResult.NO_NEWSPAPER;
        }

        inventory.removeItem(Material.NEWSPAPER, 1);
        papersDelivered++;

        if (papersDelivered >= PAPER_ROUND_PAPERS) {
            // Round complete
            paperRoundAccepted = false;

            boolean onTime = hour < PAPER_ROUND_DEADLINE;
            if (onTime) {
                // Success
                inventory.addItem(Material.COIN, PAPER_ROUND_PAY);
                inventory.removeItem(Material.PAPER_SATCHEL, 1);
                consecutiveMisses = 0;

                // Notoriety reduction
                if (notorietySystem != null) {
                    notorietySystem.reduceNotoriety(PAPER_ROUND_NOTORIETY_REDUCTION, null);
                }

                // GRAFTING XP
                if (streetSkillSystem != null) {
                    streetSkillSystem.awardXP(StreetSkillSystem.Skill.GRAFTING, PAPER_ROUND_GRAFTING_XP);
                }

                // Neighbourhood vibes boost
                if (neighbourhoodSystem != null) {
                    int currentVibes = neighbourhoodSystem.getVibes();
                    neighbourhoodSystem.setVibes(
                            Math.min(100, currentVibes + (int) (PAPER_ROUND_VIBES_BONUS * 2)));
                }

                // Achievements
                if (!firstPaperRoundDone && achievementSystem != null) {
                    firstPaperRoundDone = true;
                    achievementSystem.unlock(AchievementType.PAPER_ROUND_DONE);
                    achievementSystem.unlock(AchievementType.EARLY_BIRD_PAPERBOY);
                }

                // Seed rumour
                if (rumourNetwork != null && patel != null) {
                    rumourNetwork.addRumour(patel,
                            new Rumour(RumourType.NEIGHBOURHOOD,
                                    "The paper round kid's been reliable lately."));
                }

                return DeliverPaperResult.ROUND_COMPLETE;
            } else {
                // Missed deadline
                consecutiveMisses++;
                inventory.removeItem(Material.PAPER_SATCHEL, 1);

                // Penalty: attempt to deduct coins if available
                int coins = inventory.getItemCount(Material.COIN);
                int deduct = Math.min(PAPER_ROUND_MISS_PENALTY, coins);
                if (deduct > 0) {
                    inventory.removeItem(Material.COIN, deduct);
                }

                // Check for ban
                if (consecutiveMisses >= PAPER_ROUND_MISS_BAN) {
                    banExpiryDay = currentDay + PAPER_ROUND_BAN_DAYS;
                }

                return DeliverPaperResult.ROUND_MISSED;
            }
        }

        return DeliverPaperResult.DELIVERED;
    }

    // ── Bundle theft ───────────────────────────────────────────────────────────

    /**
     * Result of stealing a newspaper bundle from outside the shop.
     */
    public enum BundleTheftResult {
        STOLEN,
        STOLEN_BANNED   // Theft also results in a shop ban
    }

    /**
     * Player steals a newspaper bundle from outside Patel's.
     *
     * @param inventory  player's inventory
     * @param currentDay current in-game day number
     */
    public BundleTheftResult stealBundle(Inventory inventory, int currentDay) {
        inventory.addItem(Material.NEWSPAPER, PAPER_ROUND_PAPERS);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(BUNDLE_THEFT_NOTORIETY, null);
        }

        // Permanent shop ban
        applyShopBan(currentDay);

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.BANNED_FROM_PATEL);
        }

        return BundleTheftResult.STOLEN_BANNED;
    }

    // ── Shoplifting ────────────────────────────────────────────────────────────

    /**
     * Result of shoplifting from Patel's.
     */
    public enum ShopliftResult {
        STOLEN,
        CAUGHT  // Crime recorded, ban issued
    }

    /**
     * Player attempts to shoplift an item.
     *
     * @param item        the item to steal
     * @param inventory   player's inventory
     * @param notoriety   current player notoriety (0–1000)
     * @param currentDay  current in-game day number
     */
    public ShopliftResult shoplift(Material item, Inventory inventory,
                                    int notoriety, int currentDay) {
        float detectionChance = getDetectionChance(notoriety);
        boolean caught = random.nextFloat() < detectionChance;

        if (caught) {
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(SHOPLIFT_NOTORIETY, null);
            }
            applyShopBan(currentDay);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.BANNED_FROM_PATEL);
            }
            return ShopliftResult.CAUGHT;
        }

        // Undetected
        inventory.addItem(item, 1);
        totalPurchaseCount++;

        if (item == Material.DODGY_MAGAZINE && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.DODGY_MAG_BUYER);
        }

        return ShopliftResult.STOLEN;
    }

    private float getDetectionChance(int notoriety) {
        int tier = getTier(notoriety);
        if (tier >= 4) return SHOPLIFT_DETECT_HIGH;
        if (tier >= 2) return SHOPLIFT_DETECT_MID;
        return SHOPLIFT_DETECT_LOW;
    }

    private int getTier(int notoriety) {
        if (notoriety >= NotorietySystem.TIER_5_THRESHOLD) return 5;
        if (notoriety >= NotorietySystem.TIER_4_THRESHOLD) return 4;
        if (notoriety >= NotorietySystem.TIER_3_THRESHOLD) return 3;
        if (notoriety >= NotorietySystem.TIER_2_THRESHOLD) return 2;
        if (notoriety >= NotorietySystem.TIER_1_THRESHOLD) return 1;
        return 0;
    }

    // ── Burglary ───────────────────────────────────────────────────────────────

    /**
     * Result of raiding the back-office cash box.
     */
    public enum BurglaryResult {
        SUCCESS,
        ALREADY_RAIDED, // Already done today
        NO_KEY          // Missing NEWSAGENT_KEY
    }

    /**
     * Player uses NEWSAGENT_KEY to raid the back-office cash box.
     *
     * @param inventory  player's inventory
     * @param currentDay current in-game day number
     */
    public BurglaryResult raidCashBox(Inventory inventory, int currentDay) {
        if (!inventory.hasItem(Material.NEWSAGENT_KEY)) {
            return BurglaryResult.NO_KEY;
        }
        if (lastBurglaryDay == currentDay) {
            return BurglaryResult.ALREADY_RAIDED;
        }

        lastBurglaryDay = currentDay;
        int loot = CASH_BOX_MIN + random.nextInt(CASH_BOX_MAX - CASH_BOX_MIN + 1);
        inventory.addItem(Material.COIN, loot);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.THEFT);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(BURGLARY_NOTORIETY, null);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.NEWSAGENT_BURGLAR);
        }

        return BurglaryResult.SUCCESS;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Per-frame update. Resolves lottery at 20:00, seeds daily Patel rumour,
     * manages NPC schedules.
     *
     * @param delta      seconds since last frame
     * @param timeSystem the time system
     * @param inventory  player's inventory
     * @param npcs       all active NPCs
     * @param currentDay current in-game day number
     */
    public void update(float delta, TimeSystem timeSystem, Inventory inventory,
                       List<NPC> npcs, int currentDay) {
        float hour = timeSystem.getTime();

        // Lottery resolution at 20:00 daily
        if (lastLotteryResolutionDay != currentDay) {
            float prevHour = hour - delta * (1f / 60f); // approximate previous hour
            if (prevHour < LOTTERY_RESOLUTION_HOUR && hour >= LOTTERY_RESOLUTION_HOUR) {
                resolveLotteryTickets(inventory, currentDay, npcs);
            }
        }

        // Expire old tickets on day rollover
        expireLotteryTickets(inventory, currentDay);

        // Seed neighbourhood rumour from Patel once per day
        if (rumourNetwork != null && patel != null && isOpen(hour)) {
            // Only seed once — use a simple boolean flag (already handled by existing
            // logic via the RumourNetwork's own dedup). We seed with a low probability
            // per frame to simulate "daily" seeding without a stored daily flag.
            if (random.nextFloat() < 0.0001f) {
                rumourNetwork.addRumour(patel,
                        new Rumour(RumourType.NEIGHBOURHOOD,
                                "Patel's saying the area's been a bit rough lately."));
            }
        }
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    private void applyShopBan(int currentDay) {
        bannedFromPatel = true;
        shopBanExpiryDay = currentDay + BAN_DURATION_DAYS;
    }

    private void checkRajsFavourite() {
        if (!rajsFavouriteAwarded && totalPurchaseCount >= 10 && achievementSystem != null) {
            rajsFavouriteAwarded = true;
            achievementSystem.unlock(AchievementType.RAJS_FAVOURITE);
        }
    }

    private NPC findAnyNPC(List<NPC> npcs) {
        if (npcs == null || npcs.isEmpty()) return null;
        for (NPC npc : npcs) {
            if (npc != null && npc.isAlive()) return npc;
        }
        return null;
    }

    // ── Accessors for testing ──────────────────────────────────────────────────

    /** Returns whether the player has accepted today's paper round. */
    public boolean isPaperRoundAccepted() { return paperRoundAccepted; }

    /** Returns the number of papers delivered in the current round. */
    public int getPapersDelivered() { return papersDelivered; }

    /** Returns the number of consecutive paper round misses. */
    public int getConsecutiveMisses() { return consecutiveMisses; }

    /** Returns whether the player is paper-round banned. */
    public boolean isPaperRoundBanned(int currentDay) {
        return banExpiryDay >= 0 && currentDay < banExpiryDay;
    }

    /** Returns whether the player is shop-banned. */
    public boolean isShopBanned(int currentDay) {
        return isBanned(currentDay);
    }

    /** Returns the shop ban expiry day. */
    public int getShopBanExpiryDay() { return shopBanExpiryDay; }

    /** Returns the day the lottery was last resolved. */
    public int getLastLotteryResolutionDay() { return lastLotteryResolutionDay; }

    /** Returns the total purchase count. */
    public int getTotalPurchaseCount() { return totalPurchaseCount; }

    /** Force-set the paper round accepted state for testing. */
    public void setPaperRoundAcceptedForTesting(boolean accepted, int day, int delivered) {
        this.paperRoundAccepted = accepted;
        this.paperRoundAcceptDay = day;
        this.papersDelivered = delivered;
    }

    /** Force-set the shop ban for testing. */
    public void setShopBanForTesting(int expiryDay) {
        this.bannedFromPatel = expiryDay >= 0;
        this.shopBanExpiryDay = expiryDay;
    }

    /** Force-set the lottery resolution day for testing. */
    public void setLastLotteryResolutionDayForTesting(int day) {
        this.lastLotteryResolutionDay = day;
    }
}
