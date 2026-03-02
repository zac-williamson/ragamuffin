package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1138: Northfield Iceland — Frozen Aisle Economy, the Christmas Club Scam
 * &amp; the Great Turkey Heist.
 *
 * <h3>Three-for-a-Fiver Deal</h3>
 * <ul>
 *   <li>Items: {@link Material#FROZEN_PIZZA}, {@link Material#PRAWN_RING},
 *       {@link Material#CHICKEN_NUGGETS}.</li>
 *   <li>Buying 3 qualifying items at the checkout (press E on Sharon) costs 5 COIN total.</li>
 *   <li>First deal awards {@link AchievementType#THREE_FOR_A_FIVER}.</li>
 * </ul>
 *
 * <h3>Self-Checkout Scam</h3>
 * <ul>
 *   <li>Player brings items to the self-checkout without paying.</li>
 *   <li>Kevin ({@link NPCType#ICELAND_SECURITY}) has a {@link #KEVIN_DETECTION_CHANCE} (40%)
 *       detection chance.</li>
 *   <li>Placing a {@link Material#PRAWN_RING} near Kevin distracts him for
 *       {@link #PRAWN_RING_DISTRACTION_SECONDS} seconds, dropping detection to 0%.</li>
 *   <li>On detection: {@link CrimeType#SHOPLIFTING} recorded, +8 Notoriety, WantedSystem +1.</li>
 *   <li>Successful scam awards {@link AchievementType#RECEIPT_ARTIST}.</li>
 *   <li>Prawn ring bait success awards {@link AchievementType#PRAWN_RING_BAIT}.</li>
 *   <li>Detection awards {@link AchievementType#UNEXPECTED_ITEM}.</li>
 * </ul>
 *
 * <h3>Christmas Club</h3>
 * <ul>
 *   <li>Active in December (in-game days 335–365).</li>
 *   <li>Debbie ({@link NPCType#ICELAND_MANAGER}) holds envelopes for 3–6 customers.</li>
 *   <li>Each {@link Material#CHRISTMAS_ENVELOPE} contains
 *       {@link #CHRISTMAS_CLUB_MIN_COIN}–{@link #CHRISTMAS_CLUB_MAX_COIN} COIN.</li>
 *   <li>Player can return an envelope honestly (awards
 *       {@link AchievementType#CHRISTMAS_CLUB_HONEST}) or steal it.</li>
 *   <li>Stealing: {@link CrimeType#CHRISTMAS_CLUB_THEFT} recorded, +15 Notoriety,
 *       {@link RumourType#LOCAL_SCANDAL} seeded, {@link AchievementType#CHRISTMAS_CLUB_VILLAIN}
 *       awarded.</li>
 * </ul>
 *
 * <h3>Great Turkey Heist</h3>
 * <ul>
 *   <li>Active Dec 1–24 ({@link #TURKEY_HEIST_START_DAY}–{@link #TURKEY_HEIST_END_DAY}).</li>
 *   <li>{@link #TURKEY_COUNT} (6) {@link Material#FROZEN_TURKEY} items locked in the stockroom.</li>
 *   <li>Stockroom opened with {@link Material#ICELAND_STAFF_KEY}.</li>
 *   <li>Stealing all 6 turkeys awards {@link AchievementType#GREAT_TURKEY_HEIST} and
 *       triggers a {@link NewspaperSystem} front-page headline.</li>
 *   <li>Fencing all 6 in one session awards {@link AchievementType#TURKEY_DISTRIBUTOR}.</li>
 * </ul>
 *
 * <h3>NPCs</h3>
 * <ul>
 *   <li>Debbie ({@link NPCType#ICELAND_MANAGER}) — manager, Christmas Club custodian.</li>
 *   <li>Sharon ({@link NPCType#ICELAND_CHECKOUT}) — checkout operator.</li>
 *   <li>Kevin ({@link NPCType#ICELAND_SECURITY}) — self-checkout security, distractable.</li>
 * </ul>
 */
public class IcelandSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** Hour Iceland opens on weekdays and Saturday. */
    public static final float OPEN_HOUR_WEEKDAY = 8.0f;

    /** Hour Iceland closes on weekdays and Saturday. */
    public static final float CLOSE_HOUR_WEEKDAY = 18.0f;

    /** Hour Iceland opens on Sunday. */
    public static final float OPEN_HOUR_SUNDAY = 10.0f;

    /** Hour Iceland closes on Sunday. */
    public static final float CLOSE_HOUR_SUNDAY = 16.0f;

    // ── Three-for-a-fiver deal ─────────────────────────────────────────────────

    /** Cost of the three-for-a-fiver party food deal (3 items for 5 COIN). */
    public static final int DEAL_PRICE = 5;

    /** Number of items in the three-for-a-fiver deal. */
    public static final int DEAL_ITEM_COUNT = 3;

    // ── Self-checkout scam ─────────────────────────────────────────────────────

    /** Base detection chance (0–1) for Kevin catching the self-checkout scam. */
    public static final float KEVIN_DETECTION_CHANCE = 0.40f;

    /** Detection chance (0–1) when Kevin is distracted by a PRAWN_RING bait. */
    public static final float KEVIN_DISTRACTED_DETECTION_CHANCE = 0.0f;

    /** How long (in-game seconds) Kevin remains distracted after a PRAWN_RING is placed. */
    public static final float PRAWN_RING_DISTRACTION_SECONDS = 30.0f;

    /** Notoriety added when Kevin catches the scam. */
    public static final int SHOPLIFTING_NOTORIETY = 8;

    /** Sharon's base chance (0–1) of accepting a FAKE_RECEIPT. */
    public static final float SHARON_FAKE_RECEIPT_ACCEPTANCE = 0.60f;

    /** Sharon's acceptance chance (0–1) when Kevin is distracted. */
    public static final float SHARON_DISTRACTED_ACCEPTANCE = 1.0f;

    // ── Christmas Club ─────────────────────────────────────────────────────────

    /** Minimum COIN per Christmas Club envelope. */
    public static final int CHRISTMAS_CLUB_MIN_COIN = 20;

    /** Maximum COIN per Christmas Club envelope. */
    public static final int CHRISTMAS_CLUB_MAX_COIN = 35;

    /** Minimum customers enrolled in Christmas Club. */
    public static final int CHRISTMAS_CLUB_MIN_CUSTOMERS = 3;

    /** Maximum customers enrolled in Christmas Club. */
    public static final int CHRISTMAS_CLUB_MAX_CUSTOMERS = 6;

    /** Notoriety added when Christmas Club Cash Box is stolen. */
    public static final int CHRISTMAS_CLUB_THEFT_NOTORIETY = 15;

    // ── Great Turkey Heist ─────────────────────────────────────────────────────

    /** Day-of-year when the turkey stockroom is first stocked (Dec 1, approx day 335). */
    public static final int TURKEY_HEIST_START_DAY = 335;

    /** Day-of-year when the turkey stockroom is locked again (Dec 24, approx day 358). */
    public static final int TURKEY_HEIST_END_DAY = 358;

    /** Number of FROZEN_TURKEY items in the stockroom during the heist window. */
    public static final int TURKEY_COUNT = 6;

    /** Notoriety per turkey stolen from the stockroom. */
    public static final int TURKEY_THEFT_NOTORIETY = 5;

    // ── Speech lines ───────────────────────────────────────────────────────────

    public static final String DEBBIE_GREETING          = "Hello love, can I help you?";
    public static final String DEBBIE_DEAL_OFFER        = "Three for a fiver on the party food. Have a look.";
    public static final String DEBBIE_CHRISTMAS_CLUB    = "Christmas Club envelopes are ready. Happy to help if you need one.";
    public static final String DEBBIE_CLOSED            = "We're closed, love. Come back when we're open.";
    public static final String SHARON_DEAL_PROCESSED    = "Three for a fiver — lovely. Have a nice day.";
    public static final String SHARON_NO_COIN           = "That's not enough, love. It's five coin for the deal.";
    public static final String KEVIN_ALERT              = "Excuse me — unexpected item in the bagging area.";
    public static final String KEVIN_PRAWN_RING         = "Ooh, is that a prawn ring? I do love a prawn ring.";
    public static final String KEVIN_CALLING_POLICE     = "Right, that's it. I'm calling the police.";
    public static final String TURKEY_HEIST_HEADLINE    = "TURKEY BANDIT STRIPS ICELAND STOCKROOM — DEBBIE INCONSOLABLE";

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether Kevin is currently distracted by a prawn ring. */
    private boolean kevinDistracted = false;

    /** Remaining distraction time for Kevin in in-game seconds. */
    private float kevinDistractionTimer = 0f;

    /** Whether the three-for-a-fiver achievement has been awarded. */
    private boolean firstDealDone = false;

    /** Turkeys stolen in this session (resets per game day). */
    private int turkeysStolen = 0;

    /** Whether all 6 turkeys have been stolen (GREAT_TURKEY_HEIST achieved). */
    private boolean greatTurkeyHeistComplete = false;

    /** Whether the Christmas Club has been stolen from this session. */
    private boolean christmasClubStolen = false;

    /** Whether UNEXPECTED_ITEM has been awarded this session. */
    private boolean unexpectedItemAwarded = false;

    /** Whether PRAWN_RING_BAIT has been awarded. */
    private boolean prawnRingBaitAwarded = false;

    /** Number of turkeys fenced in this session (for TURKEY_DISTRIBUTOR). */
    private int turkeysFenced = 0;

    /** Whether TURKEY_DISTRIBUTOR has been awarded. */
    private boolean turkeyDistributorAwarded = false;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WitnessSystem witnessSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;
    private NewspaperSystem newspaperSystem;

    // ── Construction ────────────────────────────────────────────────────────────

    public IcelandSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWitnessSystem(WitnessSystem witnessSystem) {
        this.witnessSystem = witnessSystem;
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

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    // ── Opening hours logic ─────────────────────────────────────────────────────

    /**
     * Returns true if Iceland is open at the given hour on the given day-of-week.
     * (0=Sunday, 1=Monday, …, 6=Saturday). Open every day.
     *
     * @param hour       current in-game hour (0–24)
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == 0) {
            return hour >= OPEN_HOUR_SUNDAY && hour < CLOSE_HOUR_SUNDAY;
        }
        return hour >= OPEN_HOUR_WEEKDAY && hour < CLOSE_HOUR_WEEKDAY;
    }

    // ── Update tick ─────────────────────────────────────────────────────────────

    /**
     * Per-frame update — ticks down Kevin's distraction timer.
     *
     * @param delta in-game seconds since last frame
     */
    public void update(float delta) {
        if (kevinDistracted) {
            kevinDistractionTimer -= delta;
            if (kevinDistractionTimer <= 0f) {
                kevinDistracted = false;
                kevinDistractionTimer = 0f;
            }
        }
    }

    // ── Three-for-a-fiver deal ─────────────────────────────────────────────────

    /**
     * Result of attempting the three-for-a-fiver party food deal.
     */
    public enum DealResult {
        /** Deal purchased — 3 items added to inventory, 5 COIN removed. */
        SUCCESS,
        /** Counter is closed right now. */
        CLOSED,
        /** Player does not have enough COIN (needs 5). */
        NO_COIN
    }

    /**
     * Player presses E on Sharon to buy the three-for-a-fiver party food deal.
     * Adds one each of FROZEN_PIZZA, PRAWN_RING, and CHICKEN_NUGGETS for 5 COIN.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public DealResult buyThreeForAFiver(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return DealResult.CLOSED;
        }
        if (inventory.getItemCount(Material.COIN) < DEAL_PRICE) {
            return DealResult.NO_COIN;
        }

        inventory.removeItem(Material.COIN, DEAL_PRICE);
        inventory.addItem(Material.FROZEN_PIZZA, 1);
        inventory.addItem(Material.PRAWN_RING, 1);
        inventory.addItem(Material.CHICKEN_NUGGETS, 1);

        if (!firstDealDone && achievementSystem != null) {
            firstDealDone = true;
            achievementSystem.unlock(AchievementType.THREE_FOR_A_FIVER);
        }

        return DealResult.SUCCESS;
    }

    // ── Prawn ring distraction ─────────────────────────────────────────────────

    /**
     * Player places a PRAWN_RING near Kevin to distract him.
     * Kevin ignores self-checkout events for {@link #PRAWN_RING_DISTRACTION_SECONDS} seconds.
     * Consumes 1 PRAWN_RING from inventory.
     *
     * @param inventory player's inventory
     * @return true if distraction was successfully applied (had PRAWN_RING and Kevin wasn't already distracted)
     */
    public boolean placePrownRingBait(Inventory inventory) {
        if (!inventory.hasItem(Material.PRAWN_RING)) {
            return false;
        }
        inventory.removeItem(Material.PRAWN_RING, 1);
        kevinDistracted = true;
        kevinDistractionTimer = PRAWN_RING_DISTRACTION_SECONDS;
        return true;
    }

    // ── Self-checkout scam ─────────────────────────────────────────────────────

    /**
     * Result of attempting the self-checkout scam.
     */
    public enum ScamResult {
        /** Scam successful — items kept, no penalty. */
        SUCCESS,
        /** Scam successful using PRAWN_RING distraction on Kevin. */
        SUCCESS_BAIT,
        /** Kevin caught the player — crime recorded, notoriety added. */
        CAUGHT_BY_KEVIN,
        /** Counter/checkout is closed. */
        CLOSED
    }

    /**
     * Player attempts to walk through self-checkout without paying.
     * Kevin has a {@link #KEVIN_DETECTION_CHANCE} (40%) chance of catching the player.
     * If Kevin is distracted (via PRAWN_RING), detection drops to 0%.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     * @param npcs       all active NPCs (for rumour seeding)
     */
    public ScamResult attemptSelfCheckoutScam(Inventory inventory, float hour, int dayOfWeek,
                                               List<NPC> npcs) {
        if (!isOpen(hour, dayOfWeek)) {
            return ScamResult.CLOSED;
        }

        boolean distractedThisAttempt = kevinDistracted;
        float detectionChance = distractedThisAttempt
                ? KEVIN_DISTRACTED_DETECTION_CHANCE
                : KEVIN_DETECTION_CHANCE;

        if (random.nextFloat() < detectionChance) {
            // Kevin caught the player
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.SHOPLIFTING);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(SHOPLIFTING_NOTORIETY, null);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, type -> {});
            }
            if (!unexpectedItemAwarded && achievementSystem != null) {
                unexpectedItemAwarded = true;
                achievementSystem.unlock(AchievementType.UNEXPECTED_ITEM);
            }
            return ScamResult.CAUGHT_BY_KEVIN;
        }

        // Scam successful
        if (distractedThisAttempt) {
            if (!prawnRingBaitAwarded && achievementSystem != null) {
                prawnRingBaitAwarded = true;
                achievementSystem.unlock(AchievementType.PRAWN_RING_BAIT);
            }
            return ScamResult.SUCCESS_BAIT;
        }

        // Award RECEIPT_ARTIST on first successful scam without bait
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.RECEIPT_ARTIST);
        }
        return ScamResult.SUCCESS;
    }

    /**
     * Player attempts to use a FAKE_RECEIPT at the checkout to legitimise unpaid items.
     * Sharon accepts it at {@link #SHARON_FAKE_RECEIPT_ACCEPTANCE} (60%) chance normally,
     * or {@link #SHARON_DISTRACTED_ACCEPTANCE} (100%) when Kevin is distracted.
     *
     * @param inventory  player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public enum FakeReceiptResult {
        /** Sharon accepted the receipt — items pass unchallenged. */
        ACCEPTED,
        /** Sharon rejected the receipt — calls Kevin. */
        REJECTED,
        /** Player does not have a FAKE_RECEIPT. */
        NO_RECEIPT,
        /** Closed. */
        CLOSED
    }

    /**
     * Player presents a FAKE_RECEIPT to Sharon.
     */
    public FakeReceiptResult presentFakeReceipt(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) {
            return FakeReceiptResult.CLOSED;
        }
        if (!inventory.hasItem(Material.FAKE_RECEIPT)) {
            return FakeReceiptResult.NO_RECEIPT;
        }

        inventory.removeItem(Material.FAKE_RECEIPT, 1);

        float acceptanceChance = kevinDistracted
                ? SHARON_DISTRACTED_ACCEPTANCE
                : SHARON_FAKE_RECEIPT_ACCEPTANCE;

        if (random.nextFloat() < acceptanceChance) {
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.RECEIPT_ARTIST);
            }
            return FakeReceiptResult.ACCEPTED;
        }
        return FakeReceiptResult.REJECTED;
    }

    // ── Christmas Club ─────────────────────────────────────────────────────────

    /**
     * Result of interacting with the Christmas Club Cash Box.
     */
    public enum ChristmasClubResult {
        /** Player honestly returned an envelope to Debbie — community respect boost. */
        RETURNED_HONESTLY,
        /** Player stole a Christmas Envelope — crime recorded, scandal seeded. */
        ENVELOPE_STOLEN,
        /** Player stole the entire Christmas Club Cash Box. */
        CASH_BOX_STOLEN,
        /** Christmas Club is not active right now (not December). */
        NOT_ACTIVE
    }

    /**
     * Returns true if the Christmas Club scheme is active on the given day-of-year.
     * Active Dec 1–24 (in-game days 335–358).
     *
     * @param dayOfYear current in-game day of year (1–365)
     */
    public boolean isChristmasClubActive(int dayOfYear) {
        return dayOfYear >= TURKEY_HEIST_START_DAY && dayOfYear <= TURKEY_HEIST_END_DAY;
    }

    /**
     * Player interacts with the Christmas Club scheme.
     * Honest return: removes CHRISTMAS_ENVELOPE from inventory, awards CHRISTMAS_CLUB_HONEST.
     * Theft of envelope: records crime, adds notoriety, seeds LOCAL_SCANDAL rumour.
     * Theft of cash box: records crime, adds notoriety, seeds LOCAL_SCANDAL rumour.
     *
     * @param inventory   player's inventory
     * @param stealBox    if true, player steals the entire cash box; if false, handles a single envelope
     * @param returnHonestly  if true and player holds CHRISTMAS_ENVELOPE, returns it honestly
     * @param dayOfYear   current in-game day of year
     * @param npcs        all active NPCs (for rumour seeding)
     */
    public ChristmasClubResult interactChristmasClub(
            Inventory inventory,
            boolean stealBox,
            boolean returnHonestly,
            int dayOfYear,
            List<NPC> npcs) {

        if (!isChristmasClubActive(dayOfYear)) {
            return ChristmasClubResult.NOT_ACTIVE;
        }

        if (returnHonestly && inventory.hasItem(Material.CHRISTMAS_ENVELOPE)) {
            inventory.removeItem(Material.CHRISTMAS_ENVELOPE, 1);
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CHRISTMAS_CLUB_HONEST);
            }
            return ChristmasClubResult.RETURNED_HONESTLY;
        }

        if (stealBox) {
            // Steal the whole cash box
            int totalCoin = 0;
            int customers = CHRISTMAS_CLUB_MIN_CUSTOMERS
                    + random.nextInt(CHRISTMAS_CLUB_MAX_CUSTOMERS - CHRISTMAS_CLUB_MIN_CUSTOMERS + 1);
            for (int i = 0; i < customers; i++) {
                totalCoin += CHRISTMAS_CLUB_MIN_COIN
                        + random.nextInt(CHRISTMAS_CLUB_MAX_COIN - CHRISTMAS_CLUB_MIN_COIN + 1);
            }
            inventory.addItem(Material.COIN, totalCoin);
            inventory.addItem(Material.CHRISTMAS_CLUB_CASH_BOX, 1);

            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.CHRISTMAS_CLUB_THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CHRISTMAS_CLUB_THEFT_NOTORIETY, null);
            }
            seedLocalScandalRumour(npcs, "Someone nicked the Christmas Club money from Iceland. Debbie is in pieces.");
            if (!christmasClubStolen && achievementSystem != null) {
                christmasClubStolen = true;
                achievementSystem.unlock(AchievementType.CHRISTMAS_CLUB_VILLAIN);
            }
            return ChristmasClubResult.CASH_BOX_STOLEN;
        }

        // Steal a single envelope
        int envelopeAmount = CHRISTMAS_CLUB_MIN_COIN
                + random.nextInt(CHRISTMAS_CLUB_MAX_COIN - CHRISTMAS_CLUB_MIN_COIN + 1);
        inventory.addItem(Material.COIN, envelopeAmount);
        inventory.addItem(Material.CHRISTMAS_ENVELOPE, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CHRISTMAS_CLUB_THEFT);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CHRISTMAS_CLUB_THEFT_NOTORIETY / 2, null);
        }
        seedLocalScandalRumour(npcs, "Someone half-inched someone's Christmas envelope at Iceland. Disgraceful.");
        return ChristmasClubResult.ENVELOPE_STOLEN;
    }

    // ── Great Turkey Heist ─────────────────────────────────────────────────────

    /**
     * Result of attempting to steal a turkey from the stockroom.
     */
    public enum TurkeyHeistResult {
        /** One turkey successfully stolen. */
        TURKEY_STOLEN,
        /** All 6 turkeys stolen — achievement and newspaper headline triggered. */
        HEIST_COMPLETE,
        /** Stockroom is not active during this period (outside Dec 1–24). */
        NOT_ACTIVE,
        /** No turkeys remain in the stockroom. */
        NO_TURKEYS,
        /** Player does not have the ICELAND_STAFF_KEY to open the stockroom. */
        NO_KEY
    }

    /**
     * Player steals one FROZEN_TURKEY from the Iceland stockroom.
     * Requires {@link Material#ICELAND_STAFF_KEY} in inventory.
     * Active only Dec 1–24.
     *
     * @param inventory  player's inventory
     * @param dayOfYear  current in-game day of year (1–365)
     * @param npcs       all active NPCs (for rumour seeding / newspaper)
     */
    public TurkeyHeistResult stealTurkey(Inventory inventory, int dayOfYear, List<NPC> npcs) {
        if (!isChristmasClubActive(dayOfYear)) {
            return TurkeyHeistResult.NOT_ACTIVE;
        }
        if (!inventory.hasItem(Material.ICELAND_STAFF_KEY)) {
            return TurkeyHeistResult.NO_KEY;
        }
        if (greatTurkeyHeistComplete || turkeysStolen >= TURKEY_COUNT) {
            return TurkeyHeistResult.NO_TURKEYS;
        }

        inventory.addItem(Material.FROZEN_TURKEY, 1);
        turkeysStolen++;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TURKEY_THEFT_NOTORIETY, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.SHOPLIFTING);
        }

        if (turkeysStolen >= TURKEY_COUNT) {
            greatTurkeyHeistComplete = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.GREAT_TURKEY_HEIST);
            }
            if (newspaperSystem != null) {
                newspaperSystem.recordEvent(
                        new NewspaperSystem.InfamyEvent(
                                "TURKEY_HEIST",
                                "Iceland Northfield",
                                Material.FROZEN_TURKEY,
                                "Debbie",
                                0,
                                null,
                                null,
                                10
                        )
                );
            }
            return TurkeyHeistResult.HEIST_COMPLETE;
        }

        return TurkeyHeistResult.TURKEY_STOLEN;
    }

    /**
     * Called when the player fences a FROZEN_TURKEY via FenceSystem or KebabVanSystem.
     * Tracks progress toward TURKEY_DISTRIBUTOR achievement.
     *
     * @param inventory player's inventory (turkey already removed by fence system)
     */
    public void onTurkeyFenced(Inventory inventory) {
        turkeysFenced++;
        if (!turkeyDistributorAwarded && turkeysFenced >= TURKEY_COUNT && achievementSystem != null) {
            turkeyDistributorAwarded = true;
            achievementSystem.unlock(AchievementType.TURKEY_DISTRIBUTOR);
        }
    }

    // ── Utility helpers ─────────────────────────────────────────────────────────

    private void seedLocalScandalRumour(List<NPC> npcs, String text) {
        if (rumourNetwork == null || npcs == null || npcs.isEmpty()) return;
        NPC target = findPublicNPC(npcs);
        if (target != null) {
            rumourNetwork.addRumour(target, new Rumour(RumourType.LOCAL_SCANDAL, text));
        }
    }

    private NPC findPublicNPC(List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (npc != null && (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.PENSIONER)) {
                return npc;
            }
        }
        return npcs.isEmpty() ? null : npcs.get(0);
    }

    // ── Accessors for testing ─────────────────────────────────────────────────

    /** Returns whether Kevin is currently distracted. */
    public boolean isKevinDistracted() {
        return kevinDistracted;
    }

    /** Returns remaining Kevin distraction time in in-game seconds. */
    public float getKevinDistractionTimer() {
        return kevinDistractionTimer;
    }

    /** Returns the number of turkeys stolen so far. */
    public int getTurkeysStolen() {
        return turkeysStolen;
    }

    /** Returns whether the Great Turkey Heist has been completed. */
    public boolean isGreatTurkeyHeistComplete() {
        return greatTurkeyHeistComplete;
    }

    /** Returns the number of turkeys fenced. */
    public int getTurkeysFenced() {
        return turkeysFenced;
    }

    /** Force-set Kevin distraction for testing. */
    public void setKevinDistractedForTesting(boolean distracted, float timer) {
        this.kevinDistracted = distracted;
        this.kevinDistractionTimer = timer;
    }

    /** Force-set turkeys stolen for testing. */
    public void setTurkeysStolenForTesting(int count) {
        this.turkeysStolen = count;
        if (count >= TURKEY_COUNT) {
            this.greatTurkeyHeistComplete = true;
        }
    }

    /** Force-set turkeys fenced for testing. */
    public void setTurkeysFencedForTesting(int count) {
        this.turkeysFenced = count;
    }
}
