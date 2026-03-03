package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1194: Northfield Community Centre — Aerobics, NA Meetings,
 * Bring &amp; Buy Sale, Grant Fraud &amp; Curry Night.
 *
 * <p>The Community Centre is open Mon–Sat 08:30–20:00 under the supervision of
 * Denise ({@link ragamuffin.entity.NPCType#COMMUNITY_CENTRE_MANAGER}).  It hosts
 * five distinct activity strands:
 *
 * <h3>1. Aerobics Sessions</h3>
 * Tue/Thu 10:00–11:00 (morning) and 18:30–19:30 (evening), Room A.
 * Eight timed WASD prompts scored 0–8.
 * <ul>
 *   <li>Score 0–3: no benefit.</li>
 *   <li>Score 4–5: Warmth +10, Hunger +5.</li>
 *   <li>Score 6–7: Warmth +10, Hunger +5 + GRAFTING XP +5.</li>
 *   <li>Score 8: Warmth +10, Hunger +5 + GRAFTING XP +5 + {@link AchievementType#STEP_TOGETHER}.</li>
 * </ul>
 * Attend 8 consecutive sessions without missing: {@link AchievementType#CLEAN_EIGHT} +
 * free pass for the next block.
 *
 * <h3>2. Narcotics Anonymous Meeting</h3>
 * Thu 19:00–20:30, Room B.
 * Player shares an auto-generated confession based on {@link CriminalRecord} entries.
 * <ul>
 *   <li>First share: {@link AchievementType#ANONYMOUS}.</li>
 *   <li>Share at Notoriety ≥ 30: 15% chance {@link RumourType#NA_RECOGNITION} seeded (+3 Notoriety).</li>
 *   <li>Biscuit theft from {@code BISCUIT_TIN_PROP} when NPCs are present:
 *       {@link CriminalRecord.CrimeType#THEFT} + {@link AchievementType#BISCUIT_BANDIT}.</li>
 * </ul>
 *
 * <h3>3. Bring &amp; Buy Sale</h3>
 * Sat 10:00–13:00.
 * 4–6 {@link NPCType#COMMUNITY_MEMBER} NPCs with stalls selling items at 40–70% fence value.
 * Rare {@link Material#ANTIQUE_CLOCK} (15% chance, 1 COIN, worth 12 COIN at pawn shop).
 * Player can list up to 5 items via Denise (10% cut).
 * Buy 3+ items: {@link AchievementType#SATURDAY_BARGAIN_HUNTER}.
 *
 * <h3>4. Curry Night</h3>
 * Sat 19:00–22:00, 2 COIN entry.
 * Dishes at 1–2 COIN. Rain boosts attendance to 10+.
 * Chat with attendees for INFLUENCE XP.
 * {@link RumourType#CURRY_NIGHT} seeded at 17:00 each Saturday.
 *
 * <h3>5. Grant Application</h3>
 * <b>Legitimate</b>: E on Denise → {@link Material#GRANT_APPLICATION_FORM} →
 * pin to {@code NOTICE_BOARD_PROP} → 3-day wait → {@link Material#GRANT_CHEQUE} (30 COIN)
 * at Post Office → {@link AchievementType#HONEST_CITIZEN}.
 * <b>Forged</b>: lockpick {@code FILING_CABINET_PROP} → {@code PHOTOCOPIER_PROP} →
 * {@link Material#FORGED_GRANT_APPLICATION} → post at {@code POST_BOX_PROP} →
 * 25% catch rate (doubled at Notoriety ≥ 50) → {@link AchievementType#GRANT_GRABBER}
 * on success, {@link RumourType#GRANT_FRAUD} + {@link CriminalRecord.CrimeType#FRAUD}
 * + WantedSystem +1 star on catch.
 */
public class CommunityCentreSystem {

    // ── Day-of-week constants (dayCount % 7; 0=Mon … 5=Sat, 6=Sun) ──────────
    public static final int MONDAY    = 0;
    public static final int TUESDAY   = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY  = 3;
    public static final int FRIDAY    = 4;
    public static final int SATURDAY  = 5;
    public static final int SUNDAY    = 6;

    // ── Opening hours (Denise / general) ──────────────────────────────────────
    /** Denise on duty Mon–Sat from this hour. */
    public static final float OPEN_HOUR  = 8.5f;  // 08:30
    /** Denise off duty after this hour. */
    public static final float CLOSE_HOUR = 20.0f; // 20:00

    // ── Aerobics session windows ───────────────────────────────────────────────
    public static final float AEROBICS_MORNING_START = 10.0f;
    public static final float AEROBICS_MORNING_END   = 11.0f;
    public static final float AEROBICS_EVENING_START = 18.5f;  // 18:30
    public static final float AEROBICS_EVENING_END   = 19.5f;  // 19:30

    /** Prompts in an aerobics session. */
    public static final int AEROBICS_PROMPT_COUNT = 8;
    /** Score threshold for warmth/hunger reward. */
    public static final int AEROBICS_SCORE_WARMTH_MIN = 4;
    /** Score threshold for GRAFTING XP reward. */
    public static final int AEROBICS_SCORE_XP_MIN = 6;
    /** Score required for STEP_TOGETHER achievement. */
    public static final int AEROBICS_SCORE_PERFECT = 8;
    /** GRAFTING XP awarded for a score ≥ 6. */
    public static final int AEROBICS_GRAFTING_XP = 5;
    /** Consecutive sessions for CLEAN_EIGHT achievement. */
    public static final int CLEAN_EIGHT_SESSION_COUNT = 8;

    // ── NA meeting window ─────────────────────────────────────────────────────
    public static final float NA_MEETING_START = 19.0f; // 19:00
    public static final float NA_MEETING_END   = 20.5f; // 20:30
    /** Notoriety level at which NA_RECOGNITION rumour may be seeded. */
    public static final int NA_RECOGNITION_NOTORIETY_THRESHOLD = 30;
    /** Probability (0–1) of NA_RECOGNITION rumour being seeded on share. */
    public static final float NA_RECOGNITION_CHANCE = 0.15f;
    /** Notoriety added when NA_RECOGNITION is seeded. */
    public static final int NA_RECOGNITION_NOTORIETY_PENALTY = 3;

    // ── Bring & Buy Sale window ───────────────────────────────────────────────
    public static final float BRING_BUY_START     = 10.0f; // 10:00
    public static final float BRING_BUY_END       = 13.0f; // 13:00
    /** Minimum number of community member stalls. */
    public static final int BRING_BUY_STALL_MIN   = 4;
    /** Maximum number of community member stalls. */
    public static final int BRING_BUY_STALL_MAX   = 6;
    /** Minimum discount factor applied to fence value for stall prices. */
    public static final float BRING_BUY_PRICE_MIN_FACTOR = 0.40f;
    /** Maximum discount factor applied to fence value for stall prices. */
    public static final float BRING_BUY_PRICE_MAX_FACTOR = 0.70f;
    /** Chance (0–1) that the antique clock appears at a stall. */
    public static final float ANTIQUE_CLOCK_CHANCE = 0.15f;
    /** Price of the antique clock at the stall (1 COIN). */
    public static final int ANTIQUE_CLOCK_STALL_PRICE = 1;
    /** Pawn shop value of antique clock. */
    public static final int ANTIQUE_CLOCK_PAWN_VALUE  = 12;
    /** Player may list this many items via Denise's consignment service. */
    public static final int BRING_BUY_MAX_CONSIGNMENT  = 5;
    /** Cut Denise takes from consignment sales (10%). */
    public static final float BRING_BUY_DENISE_CUT     = 0.10f;
    /** Number of purchases for SATURDAY_BARGAIN_HUNTER achievement. */
    public static final int BRING_BUY_BARGAIN_THRESHOLD = 3;

    // ── Curry night window ────────────────────────────────────────────────────
    public static final float CURRY_NIGHT_START        = 19.0f; // 19:00
    public static final float CURRY_NIGHT_END          = 22.0f; // 22:00
    public static final float CURRY_NIGHT_RUMOUR_HOUR  = 17.0f; // 17:00
    /** Entry fee for curry night. */
    public static final int CURRY_NIGHT_ENTRY_COST     = 2;
    /** Base attendance (number of NPCs). */
    public static final int CURRY_NIGHT_BASE_ATTENDANCE = 6;
    /** Rain-boosted attendance. */
    public static final int CURRY_NIGHT_RAIN_ATTENDANCE = 10;
    /** INFLUENCE XP per attendee chatted with. */
    public static final int CURRY_NIGHT_INFLUENCE_XP   = 2;

    // ── Curry night dish prices ────────────────────────────────────────────────
    public static final int PRICE_CHICKEN_TIKKA_MASALA = 2;
    public static final int PRICE_VEGETABLE_BALTI      = 1;
    public static final int PRICE_NAAN_BREAD           = 1;
    public static final int PRICE_MANGO_LASSI          = 1;

    // ── Grant application ─────────────────────────────────────────────────────
    /** Legitimate grant payout in COIN. */
    public static final int GRANT_CHEQUE_VALUE  = 30;
    /** In-game days the legitimate grant takes to process. */
    public static final int GRANT_PROCESS_DAYS  = 3;
    /** Base catch chance (0–1) for posting a forged grant application. */
    public static final float FORGE_CATCH_CHANCE_BASE = 0.25f;
    /** Catch chance multiplier when Notoriety ≥ 50. */
    public static final float FORGE_CATCH_CHANCE_HIGH_NOTORIETY = 0.50f;
    /** Notoriety level above which catch chance is doubled. */
    public static final int FORGE_HIGH_NOTORIETY_THRESHOLD = 50;

    // ── Suspicion timer ───────────────────────────────────────────────────────
    /** Seconds (real-time) before Denise becomes suspicious near props. */
    public static final float SUSPICION_TIME_LIMIT = 10.0f;

    // ─────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────

    private final Random random;

    // Aerobics tracking
    /** Number of consecutive aerobics sessions attended without missing. */
    private int consecutiveAerobicsSessions = 0;
    /** Whether the player has earned a free pass for the next aerobics block. */
    private boolean hasFreeAerobicsPass = false;
    /** Whether a session is currently in progress. */
    private boolean aerobicsSessionActive = false;
    /** Score accumulated in the current session (0–8). */
    private int aerobicsSessionScore = 0;

    // NA tracking
    /** Whether the player has ever shared their story at NA. */
    private boolean hasSharedAtNA = false;

    // Bring & Buy tracking
    /** Number of items purchased by the player at the current/last Bring & Buy. */
    private int bringBuyItemsPurchased = 0;
    /** Number of items the player has consigned via Denise. */
    private int consignedItemCount = 0;
    /** Whether the antique clock is available at this week's sale. */
    private boolean antiqueClockAvailable = false;
    /** Whether the antique clock has been stocked for this sale (rolled once). */
    private boolean antiqueClockRolledThisWeek = false;

    // Curry Night tracking
    /** Whether the CURRY_NIGHT rumour has been seeded for the current Saturday. */
    private boolean curryNightRumourSeededToday = false;
    /** Whether the player has paid entry for the current curry night. */
    private boolean curryNightEntryPaid = false;

    // Grant tracking
    /** Whether the player has a legitimate grant application pending. */
    private boolean grantPending = false;
    /** Remaining in-game days until the legitimate grant resolves. */
    private float grantDaysRemaining = 0f;
    /** Whether HONEST_CITIZEN has already been awarded (once-only). */
    private boolean honestCitizenAwarded = false;
    /** Whether GRANT_GRABBER has already been awarded (once-only). */
    private boolean grantGrabberAwarded = false;

    // Suspicion timer
    /** Accumulated seconds the player has lingered near the photocopier or filing cabinet. */
    private float suspicionTimer = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public CommunityCentreSystem() {
        this.random = new Random();
    }

    public CommunityCentreSystem(Random random) {
        this.random = random;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opening hours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the community centre (and Denise) is available.
     *
     * @param hour      current in-game hour (0.0–24.0)
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if open
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == SUNDAY) return false;
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Aerobics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if an aerobics session is currently running.
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if in an aerobics window
     */
    public boolean isAerobicsSession(float hour, int dayOfWeek) {
        if (dayOfWeek != TUESDAY && dayOfWeek != THURSDAY) return false;
        boolean morning = hour >= AEROBICS_MORNING_START && hour < AEROBICS_MORNING_END;
        boolean evening = hour >= AEROBICS_EVENING_START && hour < AEROBICS_EVENING_END;
        return morning || evening;
    }

    /**
     * Start an aerobics session. Resets session score.
     *
     * @return true if a session was started
     */
    public boolean startAerobicsSession() {
        aerobicsSessionActive = true;
        aerobicsSessionScore = 0;
        return true;
    }

    /**
     * Record one prompt result (hit or miss) in the current aerobics session.
     *
     * @param hit true if the player hit the prompt in time
     */
    public void recordAerobicsPrompt(boolean hit) {
        if (!aerobicsSessionActive) return;
        if (hit) aerobicsSessionScore++;
    }

    /**
     * Complete the aerobics session and apply rewards.
     *
     * <p>Score 0–3: no benefit. Score 4–5: Warmth +10, Hunger +5.
     * Score 6–7: add GRAFTING XP +5. Score 8: STEP_TOGETHER achievement.
     * Consecutive 8 sessions without missing: CLEAN_EIGHT + free pass.
     *
     * @param score            the number of prompts hit (0–8)
     * @param streetSkillSystem for GRAFTING XP (may be null)
     * @param achievementCb    for awarding achievements (may be null)
     * @return the AerobicsResult describing what was awarded
     */
    public AerobicsResult completeAerobicsSession(int score,
                                                   StreetSkillSystem streetSkillSystem,
                                                   NotorietySystem.AchievementCallback achievementCb) {
        aerobicsSessionActive = false;
        aerobicsSessionScore = score;

        AerobicsResult result = new AerobicsResult(score);

        if (score >= AEROBICS_SCORE_WARMTH_MIN) {
            result.warmthBonus = 10;
            result.hungerBonus = 5;
        }

        if (score >= AEROBICS_SCORE_XP_MIN) {
            result.graftingXP = AEROBICS_GRAFTING_XP;
            if (streetSkillSystem != null) {
                streetSkillSystem.awardXP(StreetSkillSystem.Skill.GRAFTING, AEROBICS_GRAFTING_XP);
            }
        }

        if (score == AEROBICS_SCORE_PERFECT) {
            result.stepTogetherAwarded = true;
            if (achievementCb != null) {
                achievementCb.award(AchievementType.STEP_TOGETHER);
            }
        }

        // Consecutive session tracking
        consecutiveAerobicsSessions++;
        if (consecutiveAerobicsSessions >= CLEAN_EIGHT_SESSION_COUNT) {
            result.cleanEightAwarded = true;
            hasFreeAerobicsPass = true;
            consecutiveAerobicsSessions = 0;
            if (achievementCb != null) {
                achievementCb.award(AchievementType.CLEAN_EIGHT);
            }
        }

        return result;
    }

    /**
     * Called when the player misses an aerobics session. Resets the consecutive counter.
     */
    public void missAerobicsSession() {
        consecutiveAerobicsSessions = 0;
    }

    /** Returns true if the player has a free aerobics pass for the next block. */
    public boolean hasFreeAerobicsPass() {
        return hasFreeAerobicsPass;
    }

    /** Consume the free aerobics pass. */
    public void consumeFreeAerobicsPass() {
        hasFreeAerobicsPass = false;
    }

    /** Returns the number of consecutive aerobics sessions attended. */
    public int getConsecutiveAerobicsSessions() {
        return consecutiveAerobicsSessions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Narcotics Anonymous Meeting
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the NA meeting is currently running.
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if the meeting is in session
     */
    public boolean isNAMeeting(float hour, int dayOfWeek) {
        return dayOfWeek == THURSDAY && hour >= NA_MEETING_START && hour < NA_MEETING_END;
    }

    /**
     * Player shares their confession at the NA meeting.
     *
     * <p>Auto-generates a brief confession from the criminal record.
     * First share: {@link AchievementType#ANONYMOUS}.
     * If Notoriety ≥ 30: 15% chance an {@link RumourType#NA_RECOGNITION} rumour is seeded.
     *
     * @param criminalRecord   the player's criminal record
     * @param notoriety        current Notoriety value (0–1000)
     * @param chairNpc         the NA_CHAIR NPC (for rumour seeding; may be null)
     * @param rumourNetwork    the rumour network (may be null)
     * @param notorietySystem  for applying notoriety penalty (may be null)
     * @param achievementCb    for awarding ANONYMOUS (may be null)
     * @return the generated confession text
     */
    public String shareAtNAMeeting(CriminalRecord criminalRecord,
                                    int notoriety,
                                    NPC chairNpc,
                                    RumourNetwork rumourNetwork,
                                    NotorietySystem notorietySystem,
                                    NotorietySystem.AchievementCallback achievementCb) {
        String confession = generateConfession(criminalRecord);

        if (!hasSharedAtNA) {
            hasSharedAtNA = true;
            if (achievementCb != null) {
                achievementCb.award(AchievementType.ANONYMOUS);
            }
        }

        if (notoriety >= NA_RECOGNITION_NOTORIETY_THRESHOLD) {
            if (random.nextFloat() < NA_RECOGNITION_CHANCE) {
                if (rumourNetwork != null && chairNpc != null) {
                    rumourNetwork.addRumour(chairNpc,
                            new Rumour(RumourType.NA_RECOGNITION,
                                    "Someone at the NA meeting — sounds like someone the police might know."));
                }
                if (notorietySystem != null && achievementCb != null) {
                    notorietySystem.addNotoriety(NA_RECOGNITION_NOTORIETY_PENALTY, achievementCb);
                }
            }
        }

        return confession;
    }

    /**
     * Auto-generates a confession based on the criminal record.
     *
     * @param criminalRecord the player's criminal record
     * @return a short narrative confession string
     */
    private String generateConfession(CriminalRecord criminalRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append("My name is... well. I've had a difficult time lately. ");

        int thefts = criminalRecord.getCount(CriminalRecord.CrimeType.THEFT);
        int assaults = criminalRecord.getCount(CriminalRecord.CrimeType.MEMBERS_OF_PUBLIC_PUNCHED);
        int shoplifting = criminalRecord.getCount(CriminalRecord.CrimeType.SHOPLIFTING);

        if (thefts > 0) {
            sb.append("I've taken things that weren't mine — ").append(thefts).append(" time")
              .append(thefts > 1 ? "s" : "").append(". ");
        }
        if (shoplifting > 0) {
            sb.append("I've walked out of shops without paying. ");
        }
        if (assaults > 0) {
            sb.append("I've hurt people. That's not who I want to be. ");
        }
        if (thefts == 0 && assaults == 0 && shoplifting == 0) {
            sb.append("I've been struggling. Just trying to get by. ");
        }
        sb.append("I'm here because I want things to be different.");
        return sb.toString();
    }

    /**
     * Player steals biscuits from the BISCUIT_TIN_PROP while NPCs are in the meeting.
     *
     * @param npcsPresent      true if NA attendees are currently in the meeting room
     * @param inventory        the player's inventory
     * @param criminalRecord   for recording PETTY_THEFT
     * @param achievementCb    for BISCUIT_BANDIT achievement (may be null)
     * @return result of the theft attempt
     */
    public BiscuitTheftResult stealBiscuits(boolean npcsPresent,
                                             Inventory inventory,
                                             CriminalRecord criminalRecord,
                                             NotorietySystem.AchievementCallback achievementCb) {
        if (!npcsPresent) {
            return BiscuitTheftResult.NO_NPCS_PRESENT;
        }

        inventory.addItem(Material.BISCUIT_TIN_SAVINGS, 1);
        criminalRecord.record(CriminalRecord.CrimeType.THEFT);

        if (achievementCb != null) {
            achievementCb.award(AchievementType.BISCUIT_BANDIT);
        }
        return BiscuitTheftResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bring & Buy Sale
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the Bring &amp; Buy Sale is currently open.
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if the sale is running
     */
    public boolean isBringBuySale(float hour, int dayOfWeek) {
        return dayOfWeek == SATURDAY && hour >= BRING_BUY_START && hour < BRING_BUY_END;
    }

    /**
     * Roll (once per week) whether the antique clock is available at the sale.
     */
    public void rollAntiqueClockAvailability() {
        if (!antiqueClockRolledThisWeek) {
            antiqueClockAvailable = random.nextFloat() < ANTIQUE_CLOCK_CHANCE;
            antiqueClockRolledThisWeek = true;
        }
    }

    /**
     * Purchase an item from a Bring &amp; Buy stall.
     *
     * @param item             the item to buy
     * @param price            the stall price in COIN
     * @param inventory        the player's inventory
     * @param achievementCb    for SATURDAY_BARGAIN_HUNTER (may be null)
     * @return result of the purchase
     */
    public BringBuyResult purchaseBringBuyItem(Material item, int price,
                                                Inventory inventory,
                                                NotorietySystem.AchievementCallback achievementCb) {
        if (!inventory.hasItem(Material.COIN, price)) {
            return BringBuyResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, price);
        inventory.addItem(item, 1);

        if (item == Material.ANTIQUE_CLOCK) {
            antiqueClockAvailable = false;
        }

        bringBuyItemsPurchased++;
        if (achievementCb != null && bringBuyItemsPurchased >= BRING_BUY_BARGAIN_THRESHOLD) {
            achievementCb.award(AchievementType.SATURDAY_BARGAIN_HUNTER);
        }

        return BringBuyResult.SUCCESS;
    }

    /**
     * Consign an item with Denise for sale at the Bring &amp; Buy (10% cut).
     *
     * @param item      the item to consign
     * @param inventory the player's inventory
     * @return result of the consignment attempt
     */
    public ConsignResult consignItem(Material item, Inventory inventory) {
        if (consignedItemCount >= BRING_BUY_MAX_CONSIGNMENT) {
            return ConsignResult.MAX_CONSIGNMENT_REACHED;
        }
        if (!inventory.hasItem(item)) {
            return ConsignResult.ITEM_NOT_IN_INVENTORY;
        }
        inventory.removeItem(item, 1);
        consignedItemCount++;
        return ConsignResult.SUCCESS;
    }

    /**
     * Calculate stall price for an item given a fence value.
     *
     * @param fenceValue the reference fence value in COIN
     * @return a random price between 40% and 70% of fence value (minimum 1)
     */
    public int rollStallPrice(int fenceValue) {
        float factor = BRING_BUY_PRICE_MIN_FACTOR
                + random.nextFloat() * (BRING_BUY_PRICE_MAX_FACTOR - BRING_BUY_PRICE_MIN_FACTOR);
        return Math.max(1, Math.round(fenceValue * factor));
    }

    /**
     * Returns the number of stalls at the current Bring &amp; Buy.
     *
     * @return 4–6 stalls
     */
    public int getStallCount() {
        return BRING_BUY_STALL_MIN + random.nextInt(BRING_BUY_STALL_MAX - BRING_BUY_STALL_MIN + 1);
    }

    /** Returns true if the antique clock is available for purchase. */
    public boolean isAntiqueClockAvailable() {
        return antiqueClockAvailable;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Curry Night
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if Curry Night is currently running.
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if curry night is active
     */
    public boolean isCurryNight(float hour, int dayOfWeek) {
        return dayOfWeek == SATURDAY && hour >= CURRY_NIGHT_START && hour < CURRY_NIGHT_END;
    }

    /**
     * Seed the CURRY_NIGHT rumour at 17:00 on Saturday.
     *
     * @param hour          current in-game hour
     * @param dayOfWeek     0=Mon … 5=Sat, 6=Sun
     * @param deniseNpc     the COMMUNITY_CENTRE_MANAGER NPC (may be null)
     * @param rumourNetwork the rumour network (may be null)
     */
    public void maybeSeedCurryNightRumour(float hour, int dayOfWeek,
                                           NPC deniseNpc, RumourNetwork rumourNetwork) {
        if (dayOfWeek != SATURDAY) return;
        if (curryNightRumourSeededToday) return;
        if (hour >= CURRY_NIGHT_RUMOUR_HOUR && hour < CURRY_NIGHT_RUMOUR_HOUR + 0.5f) {
            if (rumourNetwork != null && deniseNpc != null) {
                rumourNetwork.addRumour(deniseNpc,
                        new Rumour(RumourType.CURRY_NIGHT,
                                "Curry night at the Community Centre tonight — doors open at seven, two quid on the door."));
            }
            curryNightRumourSeededToday = true;
        }
    }

    /**
     * Player pays entry for Curry Night.
     *
     * @param inventory the player's inventory
     * @return INSUFFICIENT_FUNDS if can't pay, ALREADY_PAID if entry already purchased, SUCCESS otherwise
     */
    public CurryNightResult payCurryNightEntry(Inventory inventory) {
        if (curryNightEntryPaid) {
            return CurryNightResult.ALREADY_PAID;
        }
        if (!inventory.hasItem(Material.COIN, CURRY_NIGHT_ENTRY_COST)) {
            return CurryNightResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, CURRY_NIGHT_ENTRY_COST);
        curryNightEntryPaid = true;
        return CurryNightResult.SUCCESS;
    }

    /**
     * Purchase a curry night dish.
     *
     * @param dish      the dish material
     * @param price     cost in COIN
     * @param inventory the player's inventory
     * @return SUCCESS, INSUFFICIENT_FUNDS, or NOT_ADMITTED
     */
    public CurryNightResult purchaseDish(Material dish, int price, Inventory inventory) {
        if (!curryNightEntryPaid) {
            return CurryNightResult.NOT_ADMITTED;
        }
        if (!inventory.hasItem(Material.COIN, price)) {
            return CurryNightResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, price);
        inventory.addItem(dish, 1);
        return CurryNightResult.SUCCESS;
    }

    /**
     * Chat with a curry night attendee for INFLUENCE XP.
     *
     * @param attendeeNpc       the NPC being chatted with (may be null)
     * @param streetSkillSystem for INFLUENCE XP (may be null)
     * @return XP awarded (0 if not admitted or null NPC)
     */
    public int chatWithAttendee(NPC attendeeNpc, StreetSkillSystem streetSkillSystem) {
        if (!curryNightEntryPaid || attendeeNpc == null) return 0;
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.INFLUENCE, CURRY_NIGHT_INFLUENCE_XP);
        }
        return CURRY_NIGHT_INFLUENCE_XP;
    }

    /**
     * Returns the number of attendees at curry night, boosted by rain.
     *
     * @param weather current weather
     * @return number of attendee NPCs
     */
    public int getCurryNightAttendance(Weather weather) {
        if (weather == Weather.RAIN || weather == Weather.DRIZZLE) {
            return CURRY_NIGHT_RAIN_ATTENDANCE;
        }
        return CURRY_NIGHT_BASE_ATTENDANCE;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grant Application — Legitimate Route
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player requests a grant application form from Denise.
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @param inventory the player's inventory
     * @return GrantResult.SUCCESS if the form was given, CENTRE_CLOSED or ALREADY_PENDING otherwise
     */
    public GrantResult requestGrantForm(float hour, int dayOfWeek, Inventory inventory) {
        if (!isOpen(hour, dayOfWeek)) {
            return GrantResult.CENTRE_CLOSED;
        }
        if (grantPending) {
            return GrantResult.ALREADY_PENDING;
        }
        inventory.addItem(Material.GRANT_APPLICATION_FORM, 1);
        return GrantResult.SUCCESS;
    }

    /**
     * Pin the grant application form to the notice board.
     * Starts the 3-day countdown to processing.
     *
     * @param inventory the player's inventory
     * @return GrantResult.SUCCESS, or FORM_NOT_IN_INVENTORY if form not held
     */
    public GrantResult pinGrantFormToNoticeBoard(Inventory inventory) {
        if (!inventory.hasItem(Material.GRANT_APPLICATION_FORM)) {
            return GrantResult.FORM_NOT_IN_INVENTORY;
        }
        inventory.removeItem(Material.GRANT_APPLICATION_FORM, 1);
        grantPending = true;
        grantDaysRemaining = GRANT_PROCESS_DAYS;
        return GrantResult.SUCCESS;
    }

    /**
     * Tick the grant processing countdown.
     * Call once per in-game day.
     *
     * @param inventory     the player's inventory (to deliver GRANT_CHEQUE when ready)
     * @param achievementCb for HONEST_CITIZEN (may be null)
     * @return true if the grant was resolved this tick
     */
    public boolean tickGrantProcessing(Inventory inventory,
                                        NotorietySystem.AchievementCallback achievementCb) {
        if (!grantPending) return false;

        grantDaysRemaining--;
        if (grantDaysRemaining <= 0) {
            grantPending = false;
            inventory.addItem(Material.GRANT_CHEQUE, 1);
            if (!honestCitizenAwarded) {
                honestCitizenAwarded = true;
                if (achievementCb != null) {
                    achievementCb.award(AchievementType.HONEST_CITIZEN);
                }
            }
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grant Application — Forged Route
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Use the photocopier to create a forged grant application.
     * Requires the player to already hold a GRANT_APPLICATION_FORM.
     *
     * @param inventory the player's inventory
     * @return GrantResult.SUCCESS or FORM_NOT_IN_INVENTORY
     */
    public GrantResult forgeGrantApplication(Inventory inventory) {
        if (!inventory.hasItem(Material.GRANT_APPLICATION_FORM)) {
            return GrantResult.FORM_NOT_IN_INVENTORY;
        }
        inventory.addItem(Material.FORGED_GRANT_APPLICATION, 1);
        return GrantResult.SUCCESS;
    }

    /**
     * Post a forged grant application.
     *
     * <p>Base catch chance 25%, doubled when Notoriety ≥ 50.
     * Success: {@link AchievementType#GRANT_GRABBER} + GRANT_CHEQUE added.
     * Caught: {@link RumourType#GRANT_FRAUD} rumour, {@link CriminalRecord.CrimeType#FRAUD},
     * WantedSystem +1 star.
     *
     * @param inventory        the player's inventory
     * @param notoriety        current Notoriety value
     * @param criminalRecord   for recording FRAUD on catch
     * @param wantedSystem     for +1 star on catch (may be null)
     * @param npcForRumour     any nearby NPC for rumour seeding (may be null)
     * @param rumourNetwork    for GRANT_FRAUD rumour (may be null)
     * @param achievementCb    for GRANT_GRABBER (may be null)
     * @return ForgeResult describing what happened
     */
    public ForgeResult postForgedGrantApplication(Inventory inventory,
                                                   int notoriety,
                                                   CriminalRecord criminalRecord,
                                                   WantedSystem wantedSystem,
                                                   NPC npcForRumour,
                                                   RumourNetwork rumourNetwork,
                                                   NotorietySystem.AchievementCallback achievementCb) {
        if (!inventory.hasItem(Material.FORGED_GRANT_APPLICATION)) {
            return ForgeResult.FORM_NOT_IN_INVENTORY;
        }
        inventory.removeItem(Material.FORGED_GRANT_APPLICATION, 1);

        float catchChance = FORGE_CATCH_CHANCE_BASE;
        if (notoriety >= FORGE_HIGH_NOTORIETY_THRESHOLD) {
            catchChance = FORGE_CATCH_CHANCE_HIGH_NOTORIETY;
        }

        if (random.nextFloat() < catchChance) {
            // Caught
            criminalRecord.record(CriminalRecord.CrimeType.FRAUD);
            if (rumourNetwork != null && npcForRumour != null) {
                rumourNetwork.addRumour(npcForRumour,
                        new Rumour(RumourType.GRANT_FRAUD,
                                "Someone round here has been forging grant applications. Audacious."));
            }
            if (wantedSystem != null) {
                WantedSystem.AchievementCallback wantedCb =
                        achievementCb != null ? achievementCb::award : null;
                wantedSystem.addWantedStars(1, 0, 0, 0, wantedCb);
            }
            return ForgeResult.CAUGHT;
        } else {
            // Success
            inventory.addItem(Material.GRANT_CHEQUE, 1);
            if (!grantGrabberAwarded) {
                grantGrabberAwarded = true;
                if (achievementCb != null) {
                    achievementCb.award(AchievementType.GRANT_GRABBER);
                }
            }
            return ForgeResult.SUCCESS;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Suspicion timer
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Accumulate suspicion while the player lingers near the photocopier or filing cabinet.
     *
     * @param delta seconds since last frame
     * @return true if the suspicion limit has been reached (Denise should react)
     */
    public boolean tickSuspicion(float delta) {
        suspicionTimer += delta;
        return suspicionTimer >= SUSPICION_TIME_LIMIT;
    }

    /** Reset the suspicion timer (e.g. when the player moves away). */
    public void resetSuspicion() {
        suspicionTimer = 0f;
    }

    /** Returns the current suspicion timer value in seconds. */
    public float getSuspicionTimer() {
        return suspicionTimer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-frame update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update time-dependent state each frame.
     *
     * @param delta      seconds since last frame (real time)
     * @param timeSystem for time speed conversions
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Mon … 5=Sat, 6=Sun
     */
    public void update(float delta, TimeSystem timeSystem, float hour, int dayOfWeek) {
        // Reset daily curry night entry on day change (midnight)
        if (hour < 0.1f) {
            curryNightEntryPaid = false;
            if (dayOfWeek != SATURDAY) {
                curryNightRumourSeededToday = false;
            }
        }

        // Reset weekly Bring & Buy state on Sunday midnight (new week starting Mon)
        if (dayOfWeek == SUNDAY && hour < 0.1f) {
            bringBuyItemsPurchased = 0;
            consignedItemCount = 0;
            antiqueClockAvailable = false;
            antiqueClockRolledThisWeek = false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State queries
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if the player has ever shared at an NA meeting. */
    public boolean hasSharedAtNA() {
        return hasSharedAtNA;
    }

    /** Returns the number of items purchased at the Bring &amp; Buy this week. */
    public int getBringBuyItemsPurchased() {
        return bringBuyItemsPurchased;
    }

    /** Returns the number of items consigned via Denise. */
    public int getConsignedItemCount() {
        return consignedItemCount;
    }

    /** Returns true if a legitimate grant application is currently pending. */
    public boolean isGrantPending() {
        return grantPending;
    }

    /** Returns the remaining in-game days until the grant is processed. */
    public float getGrantDaysRemaining() {
        return grantDaysRemaining;
    }

    /** Returns true if the curry night entry has been paid. */
    public boolean isCurryNightEntryPaid() {
        return curryNightEntryPaid;
    }

    /**
     * Returns true if the player has completed at least one community activity
     * (aerobics session, NA share, Bring &amp; Buy purchase, or curry night attendance).
     * Used by DefibrillatorSystem to gate Denise's cabinet code disclosure.
     */
    public boolean hasCompletedAnyActivity() {
        return consecutiveAerobicsSessions > 0
                || hasSharedAtNA
                || bringBuyItemsPurchased > 0
                || curryNightEntryPaid;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Testing helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Set the consecutive aerobics session count (for testing). */
    public void setConsecutiveAerobicsSessionsForTesting(int count) {
        this.consecutiveAerobicsSessions = count;
    }

    /** Set the hasSharedAtNA flag (for testing). */
    public void setHasSharedAtNAForTesting(boolean val) {
        this.hasSharedAtNA = val;
    }

    /** Set the grant pending state (for testing). */
    public void setGrantPendingForTesting(boolean pending, float daysRemaining) {
        this.grantPending = pending;
        this.grantDaysRemaining = daysRemaining;
    }

    /** Set the Bring &amp; Buy items purchased count (for testing). */
    public void setBringBuyItemsPurchasedForTesting(int count) {
        this.bringBuyItemsPurchased = count;
    }

    /** Set the antique clock availability (for testing). */
    public void setAntiqueClockAvailableForTesting(boolean available) {
        this.antiqueClockAvailable = available;
        this.antiqueClockRolledThisWeek = true;
    }

    /** Set the curry night entry paid state (for testing). */
    public void setCurryNightEntryPaidForTesting(boolean paid) {
        this.curryNightEntryPaid = paid;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result types
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Outcome of completing an aerobics session.
     */
    public static class AerobicsResult {
        public final int score;
        public int warmthBonus    = 0;
        public int hungerBonus    = 0;
        public int graftingXP     = 0;
        public boolean stepTogetherAwarded = false;
        public boolean cleanEightAwarded   = false;

        public AerobicsResult(int score) {
            this.score = score;
        }
    }

    /** Result of a biscuit theft attempt. */
    public enum BiscuitTheftResult {
        SUCCESS,
        NO_NPCS_PRESENT
    }

    /** Result of a Bring &amp; Buy purchase. */
    public enum BringBuyResult {
        SUCCESS,
        INSUFFICIENT_FUNDS
    }

    /** Result of consigning an item with Denise. */
    public enum ConsignResult {
        SUCCESS,
        MAX_CONSIGNMENT_REACHED,
        ITEM_NOT_IN_INVENTORY
    }

    /** Result of a curry night action. */
    public enum CurryNightResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        ALREADY_PAID,
        NOT_ADMITTED
    }

    /** Result of a grant-related action. */
    public enum GrantResult {
        SUCCESS,
        CENTRE_CLOSED,
        ALREADY_PENDING,
        FORM_NOT_IN_INVENTORY
    }

    /** Result of posting a forged grant application. */
    public enum ForgeResult {
        SUCCESS,
        CAUGHT,
        FORM_NOT_IN_INVENTORY
    }
}
