package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1167: Northfield Amateur Boxing Club — Tommy's Gym, Friday Night
 * Fights &amp; the White-Collar Circuit.
 *
 * <h3>Overview</h3>
 * Tommy's Gym is a brick unit on the industrial estate ({@link LandmarkType#BOXING_CLUB}).
 * It houses {@code BOXING_BAG_PROP}, {@code BOXING_RING_PROP}, {@code SPEED_BAG_PROP},
 * {@code BOXING_TROPHY_CABINET_PROP}, {@code BOXING_NOTICE_BOARD_PROP}, and
 * (underground nights only) {@code BET_TABLE_PROP}.
 *
 * <h3>Training</h3>
 * Press E on {@code BOXING_BAG_PROP} (max {@value #MAX_DAILY_TRAINING_SESSIONS}/day)
 * during open hours ({@value #OPEN_HOUR}:00–{@value #CLOSE_HOUR}:00 Mon/Wed/Fri) to
 * receive +1 {@link StreetSkillSystem.Skill#BOXING} via
 * {@link StreetSkillSystem#awardSkillPoint}. A {@link Material#GYM_MEMBERSHIP_CARD}
 * is required after the first free session (5 COIN/week from Tommy).
 *
 * <h3>Sparring</h3>
 * Press E on a {@link NPCType#BOXING_AMATEUR} NPC at the ring. Requires
 * {@link Material#BOXING_GLOVES} and BOXING skill ≥ {@value #MIN_BOXING_SKILL_SPAR}.
 * Uses a 3-round {@link BattleBarMiniGame}. Each piece of {@link Material#SPEED_BAG_CHALK}
 * in inventory reduces incoming damage by {@value #CHALK_DAMAGE_REDUCTION} per piece
 * (consumed on use).
 *
 * <h3>Friday Night Fights</h3>
 * Active Fri {@value #FRIDAY_FIGHT_START_HOUR}:00–{@value #FRIDAY_FIGHT_END_HOUR}:00.
 * Player signs up Mon–Thu at {@code BOXING_NOTICE_BOARD_PROP} with BOXING ≥
 * {@value #MIN_BOXING_SKILL_FIGHT}. Win → {@value #FRIDAY_WIN_PRIZE} COIN +
 * {@value #FRIDAY_WIN_SKILL_POINTS} skill points + {@link RumourType#STREET_TALENT}.
 * Loss → {@link Material#PROTEIN_BAR} from Tommy.
 *
 * <h3>Underground White-Collar Circuit</h3>
 * Unlocked after {@value #WINS_TO_UNLOCK_CIRCUIT} Friday Night Fight wins.
 * Alternate Saturdays at {@value #CIRCUIT_START_HOUR}:00 (avoids MatchDay home games).
 * Opponents are {@link NPCType#WHITE_COLLAR_BOXER} NPCs.
 * Win → {@value #CIRCUIT_WIN_PRIZE} COIN + {@link RumourType#UNDERGROUND_FIGHT} seeded.
 * Wayne ({@link NPCType#FIGHT_PROMOTER}) offers bout-fixing bribes:
 * {@value #BOUT_FIX_SUCCESS_CHANCE * 100}% success, {@value #BOUT_FIX_GRASS_CHANCE * 100}%
 * grass → {@link CriminalRecord.CrimeType#BOUT_FIXING} + Notoriety +{@value #NOTORIETY_BOUT_FIXING}.
 *
 * <h3>Loaded Gloves</h3>
 * Craft {@link Material#LOADED_GLOVE} from BOXING_GLOVES + SCRAP_METAL.
 * {@value #LOADED_GLOVE_CATCH_CHANCE * 100}% catch during pat-down →
 * ejection + {@link CriminalRecord.CrimeType#FIGHT_FIXING} + Notoriety
 * +{@value #NOTORIETY_FIGHT_FIXING}.
 *
 * <h3>Tommy's Trophy Quest</h3>
 * Read clues at {@code BOXING_NOTICE_BOARD_PROP} → lockpick Derek's house
 * ({@code PADLOCK_PROP}) → retrieve {@link Material#ABA_TROPHY} → return to
 * {@code BOXING_TROPHY_CABINET_PROP} → permanent membership +
 * BOXING +{@value #TROPHY_QUEST_SKILL_REWARD} +
 * {@link AchievementType#LEGACY_OF_THE_RING}.
 */
public class BoxingClubSystem {

    // ── Opening hours ────────────────────────────────────────────────────────

    /** Gym opening hour (18:00 Mon/Wed/Fri). */
    public static final float OPEN_HOUR  = 18.0f;
    /** Gym closing hour (21:00 Mon/Wed/Fri). */
    public static final float CLOSE_HOUR = 21.0f;

    /** Day-of-week index for Monday (0). */
    public static final int MONDAY    = 0;
    /** Day-of-week index for Wednesday (2). */
    public static final int WEDNESDAY = 2;
    /** Day-of-week index for Friday (4). */
    public static final int FRIDAY    = 4;
    /** Day-of-week index for Saturday (5). */
    public static final int SATURDAY  = 5;

    // ── Training constants ───────────────────────────────────────────────────

    /** Maximum bag training sessions per in-game day. */
    public static final int MAX_DAILY_TRAINING_SESSIONS = 3;

    /** Minimum BOXING skill level required to spar with BOXING_AMATEUR. */
    public static final int MIN_BOXING_SKILL_SPAR = 2;

    /** Incoming damage reduction per SPEED_BAG_CHALK item consumed during sparring. */
    public static final int CHALK_DAMAGE_REDUCTION = 1;

    // ── Membership constants ─────────────────────────────────────────────────

    /** Weekly cost of a GYM_MEMBERSHIP_CARD (COIN). */
    public static final int MEMBERSHIP_COST_PER_WEEK = 5;

    // ── Friday Night Fights constants ────────────────────────────────────────

    /** Hour at which Friday Night Fights begin (20:00). */
    public static final float FRIDAY_FIGHT_START_HOUR = 20.0f;
    /** Hour at which Friday Night Fights end (23:00). */
    public static final float FRIDAY_FIGHT_END_HOUR   = 23.0f;

    /** Minimum BOXING skill level to sign up for Friday Night Fights. */
    public static final int MIN_BOXING_SKILL_FIGHT = 3;

    /** COIN prize for winning a Friday Night Fight. */
    public static final int FRIDAY_WIN_PRIZE = 8;

    /** BOXING skill points awarded for winning a Friday Night Fight. */
    public static final int FRIDAY_WIN_SKILL_POINTS = 5;

    // ── White-Collar Circuit constants ───────────────────────────────────────

    /** Friday Night Fight wins required to unlock the underground circuit. */
    public static final int WINS_TO_UNLOCK_CIRCUIT = 2;

    /** Hour at which the underground circuit starts (22:00). */
    public static final float CIRCUIT_START_HOUR = 22.0f;

    /** COIN prize for winning the underground circuit. */
    public static final int CIRCUIT_WIN_PRIZE = 30;

    // ── Bout-fixing (Wayne bribe) constants ──────────────────────────────────

    /** Probability that a bout-fixing bribe from Wayne succeeds. */
    public static final float BOUT_FIX_SUCCESS_CHANCE = 0.70f;

    /** Probability that accepting a bribe results in Wayne grassing (30%). */
    public static final float BOUT_FIX_GRASS_CHANCE = 0.30f;

    /** Notoriety added on BOUT_FIXING crime. */
    public static final int NOTORIETY_BOUT_FIXING = 5;

    // ── Loaded glove constants ───────────────────────────────────────────────

    /** Probability that a LOADED_GLOVE is caught during pat-down (40%). */
    public static final float LOADED_GLOVE_CATCH_CHANCE = 0.40f;

    /** Notoriety added on FIGHT_FIXING crime (loaded glove caught). */
    public static final int NOTORIETY_FIGHT_FIXING = 3;

    /** Boxing club ban length in days when caught with loaded gloves. */
    public static final int LOADED_GLOVE_BAN_DAYS = 3;

    // ── Trophy quest constants ───────────────────────────────────────────────

    /** BOXING skill points awarded for completing the trophy quest. */
    public static final int TROPHY_QUEST_SKILL_REWARD = 15;

    // ── Result enums ─────────────────────────────────────────────────────────

    /** Outcome of attempting a bag training session. */
    public enum TrainingResult {
        /** Session completed; +1 BOXING skill point awarded. */
        SUCCESS,
        /** Gym is not open at this time/day. */
        CLOSED,
        /** Player has already completed the maximum sessions today. */
        DAILY_LIMIT_REACHED,
        /** Player needs a GYM_MEMBERSHIP_CARD (after first session). */
        NEEDS_MEMBERSHIP
    }

    /** Outcome of signing up for a Friday Night Fight. */
    public enum SignUpResult {
        /** Signed up successfully; FIGHT_ENTRY_FORM added to inventory. */
        SUCCESS,
        /** Sign-up window is not open (Mon–Thu only, before Friday fights). */
        WRONG_TIME,
        /** Player BOXING skill is below the minimum required. */
        SKILL_TOO_LOW,
        /** Player does not have a valid GYM_MEMBERSHIP_CARD. */
        NEEDS_MEMBERSHIP,
        /** Player already signed up for this week's fight night. */
        ALREADY_SIGNED_UP
    }

    /** Outcome of a Friday Night Fight or circuit bout. */
    public enum BoutResult {
        /** Player won the bout. */
        WIN,
        /** Player lost the bout. */
        LOSS,
        /** Player was disqualified (loaded gloves caught). */
        DISQUALIFIED,
        /** Bout cannot proceed (wrong time / not signed up / wrong day). */
        NOT_AVAILABLE
    }

    /** Outcome of Wayne's bout-fixing bribe offer. */
    public enum FixResult {
        /** Player declined or Wayne was not encountered. */
        DECLINED,
        /** Bribe accepted and fix succeeded. */
        FIXED_SUCCESS,
        /** Bribe accepted but Wayne grassed — BOUT_FIXING crime recorded. */
        GRASSED
    }

    /** Outcome of returning the ABA trophy. */
    public enum TrophyQuestResult {
        /** Trophy returned — permanent membership + skill points + achievement awarded. */
        COMPLETE,
        /** Player does not have the ABA_TROPHY in inventory. */
        NO_TROPHY,
        /** Trophy quest has already been completed. */
        ALREADY_COMPLETE
    }

    /** Callback for awarding achievements. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── State ────────────────────────────────────────────────────────────────

    private final Random random;

    /** Training sessions completed on the current in-game day. */
    private int sessionsToday = 0;

    /** In-game day on which sessionsToday was last reset. */
    private int lastTrainingDay = -1;

    /** Whether the player has had their first (free) training session. */
    private boolean hadFirstSession = false;

    /** Whether the player holds a permanent membership (trophy quest reward). */
    private boolean permanentMember = false;

    /** Day on which the player's paid membership expires (-1 = no paid membership). */
    private int membershipExpiry = -1;

    /** Whether the player is signed up for this week's Friday Night Fight. */
    private boolean signedUpForFightNight = false;

    /** Total Friday Night Fight wins by the player. */
    private int fridayNightWins = 0;

    /** Whether the underground white-collar circuit has been unlocked. */
    private boolean circuitUnlocked = false;

    /** Day on which the BOXING_CLUB ban expires (-1 = no ban). */
    private int banExpiry = -1;

    /** Whether the trophy quest has been completed. */
    private boolean trophyQuestComplete = false;

    /** Whether the player has read the noticeboard clue for the trophy quest. */
    private boolean trophyClueRead = false;

    // ── Construction ─────────────────────────────────────────────────────────

    public BoxingClubSystem() {
        this(new Random());
    }

    public BoxingClubSystem(Random random) {
        this.random = random;
    }

    // ── Static query helpers ─────────────────────────────────────────────────

    /**
     * Returns true if the gym is open at the given hour on the given day of week.
     * Open Mon (0), Wed (2), Fri (4) between {@value #OPEN_HOUR}:00 and
     * {@value #CLOSE_HOUR}:00 (exclusive).
     *
     * @param hour      current in-game hour (float, e.g. 18.5 = 18:30)
     * @param dayOfWeek day-of-week index (0=Mon … 6=Sun)
     * @return true if the gym is open
     */
    public static boolean isGymOpen(float hour, int dayOfWeek) {
        if (dayOfWeek != MONDAY && dayOfWeek != WEDNESDAY && dayOfWeek != FRIDAY) {
            return false;
        }
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if Friday Night Fights are currently active.
     * Active on Friday ({@value #FRIDAY}) between {@value #FRIDAY_FIGHT_START_HOUR}:00
     * and {@value #FRIDAY_FIGHT_END_HOUR}:00 (exclusive).
     *
     * @param hour      current in-game hour
     * @param dayOfWeek day-of-week index
     * @return true if fight night is active
     */
    public static boolean isFightNightActive(float hour, int dayOfWeek) {
        return dayOfWeek == FRIDAY
                && hour >= FRIDAY_FIGHT_START_HOUR
                && hour < FRIDAY_FIGHT_END_HOUR;
    }

    /**
     * Returns true if the underground white-collar circuit is active tonight.
     * Active on alternate Saturdays from {@value #CIRCUIT_START_HOUR}:00.
     * "Alternate Saturday" is determined by whether {@code weekNumber % 2 == 0}.
     *
     * @param hour       current in-game hour
     * @param dayOfWeek  day-of-week index
     * @param weekNumber the current in-game week number (0-based)
     * @return true if the circuit is active
     */
    public static boolean isCircuitNightActive(float hour, int dayOfWeek, int weekNumber) {
        return dayOfWeek == SATURDAY
                && hour >= CIRCUIT_START_HOUR
                && (weekNumber % 2 == 0);
    }

    /**
     * Returns the BOXING skill level from the skill system.
     *
     * @param skillSystem the player's skill system
     * @return raw BOXING skill points
     */
    public static int getBoxingSkillLevel(StreetSkillSystem skillSystem) {
        return skillSystem.getXP(StreetSkillSystem.Skill.BOXING);
    }

    // ── Membership checks ────────────────────────────────────────────────────

    /**
     * Returns true if the player currently has valid membership (permanent or paid).
     *
     * @param currentDay current in-game day
     * @param inventory  player inventory
     * @return true if membership is valid
     */
    public boolean hasMembership(int currentDay, Inventory inventory) {
        if (permanentMember) return true;
        if (inventory.getItemCount(Material.GYM_MEMBERSHIP_CARD) > 0
                && membershipExpiry >= currentDay) {
            return true;
        }
        return false;
    }

    /**
     * Purchase a weekly GYM_MEMBERSHIP_CARD from Tommy.
     * Costs {@value #MEMBERSHIP_COST_PER_WEEK} COIN. Adds 7 days of membership.
     *
     * @param currentDay current in-game day
     * @param inventory  player inventory (must contain ≥ 5 COIN)
     * @return true if purchase succeeded; false if insufficient funds
     */
    public boolean purchaseMembership(int currentDay, Inventory inventory) {
        if (inventory.getItemCount(Material.COIN) < MEMBERSHIP_COST_PER_WEEK) {
            return false;
        }
        inventory.removeItem(Material.COIN, MEMBERSHIP_COST_PER_WEEK);
        if (inventory.getItemCount(Material.GYM_MEMBERSHIP_CARD) == 0) {
            inventory.addItem(Material.GYM_MEMBERSHIP_CARD, 1);
        }
        membershipExpiry = currentDay + 7;
        return true;
    }

    // ── Bag training ─────────────────────────────────────────────────────────

    /**
     * Attempt a bag training session at BOXING_BAG_PROP.
     *
     * <p>Rules:
     * <ul>
     *   <li>Gym must be open (Mon/Wed/Fri 18:00–21:00).</li>
     *   <li>First session is free; thereafter a valid GYM_MEMBERSHIP_CARD is required.</li>
     *   <li>Max {@value #MAX_DAILY_TRAINING_SESSIONS} sessions per in-game day.</li>
     *   <li>On success: +1 BOXING via {@link StreetSkillSystem#awardSkillPoint}.</li>
     *   <li>If this is the 3rd session today: also awards {@link AchievementType#GYM_RAT}.</li>
     * </ul>
     *
     * @param hour        current in-game hour
     * @param dayOfWeek   day-of-week index
     * @param currentDay  absolute in-game day counter
     * @param inventory   player inventory
     * @param skillSystem player skill system
     * @param cb          achievement callback
     * @return {@link TrainingResult}
     */
    public TrainingResult trainOnBag(float hour, int dayOfWeek, int currentDay,
                                     Inventory inventory, StreetSkillSystem skillSystem,
                                     AchievementCallback cb) {
        if (!isGymOpen(hour, dayOfWeek)) {
            return TrainingResult.CLOSED;
        }
        // Membership check: first session is free
        if (hadFirstSession && !hasMembership(currentDay, inventory)) {
            return TrainingResult.NEEDS_MEMBERSHIP;
        }
        // Daily limit
        if (lastTrainingDay == currentDay && sessionsToday >= MAX_DAILY_TRAINING_SESSIONS) {
            return TrainingResult.DAILY_LIMIT_REACHED;
        }
        // Reset daily counter if it's a new day
        if (lastTrainingDay != currentDay) {
            sessionsToday = 0;
            lastTrainingDay = currentDay;
        }
        // Award skill point
        skillSystem.awardSkillPoint(StreetSkillSystem.Skill.BOXING);
        sessionsToday++;
        hadFirstSession = true;

        // GYM_RAT: 3 sessions in one day
        if (sessionsToday == MAX_DAILY_TRAINING_SESSIONS && cb != null) {
            cb.award(AchievementType.GYM_RAT);
        }
        return TrainingResult.SUCCESS;
    }

    // ── Friday Night Fight sign-up ────────────────────────────────────────────

    /**
     * Attempt to sign up for this week's Friday Night Fight at
     * BOXING_NOTICE_BOARD_PROP. Sign-up is open Mon–Thu at any time.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Day of week must be Mon (0) – Thu (3).</li>
     *   <li>BOXING skill ≥ {@value #MIN_BOXING_SKILL_FIGHT}.</li>
     *   <li>Valid GYM_MEMBERSHIP_CARD (or permanent membership).</li>
     *   <li>Not already signed up this week.</li>
     * </ul>
     *
     * <p>On success: adds {@link Material#FIGHT_ENTRY_FORM} to inventory.
     *
     * @param dayOfWeek   day-of-week index
     * @param currentDay  current in-game day
     * @param inventory   player inventory
     * @param skillSystem player skill system
     * @return {@link SignUpResult}
     */
    public SignUpResult signUpForFightNight(int dayOfWeek, int currentDay,
                                            Inventory inventory,
                                            StreetSkillSystem skillSystem) {
        // Sign-up window: Mon–Thu (0–3)
        if (dayOfWeek < MONDAY || dayOfWeek > 3) {
            return SignUpResult.WRONG_TIME;
        }
        if (getBoxingSkillLevel(skillSystem) < MIN_BOXING_SKILL_FIGHT) {
            return SignUpResult.SKILL_TOO_LOW;
        }
        if (!hasMembership(currentDay, inventory)) {
            return SignUpResult.NEEDS_MEMBERSHIP;
        }
        if (signedUpForFightNight) {
            return SignUpResult.ALREADY_SIGNED_UP;
        }
        inventory.addItem(Material.FIGHT_ENTRY_FORM, 1);
        signedUpForFightNight = true;
        return SignUpResult.SUCCESS;
    }

    // ── Friday Night Fight bout ───────────────────────────────────────────────

    /**
     * Resolve a Friday Night Fight bout.
     *
     * <p>Rules:
     * <ul>
     *   <li>Must be Friday fight-night hours and player must be signed up.</li>
     *   <li>If player has {@link Material#LOADED_GLOVE}: {@value #LOADED_GLOVE_CATCH_CHANCE * 100}%
     *       catch chance → {@link BoutResult#DISQUALIFIED} + {@link CriminalRecord.CrimeType#FIGHT_FIXING}
     *       + Notoriety +{@value #NOTORIETY_FIGHT_FIXING} + ban {@value #LOADED_GLOVE_BAN_DAYS} days.</li>
     *   <li>Win (determined by caller/miniGame): reward {@value #FRIDAY_WIN_PRIZE} COIN +
     *       {@value #FRIDAY_WIN_SKILL_POINTS} skill points + STREET_TALENT rumour +
     *       achievements.</li>
     *   <li>Loss: Tommy gives PROTEIN_BAR consolation prize.</li>
     * </ul>
     *
     * @param hour          current in-game hour
     * @param dayOfWeek     current day of week
     * @param currentDay    current in-game day
     * @param playerWon     true if the player won the bout (determined by BattleBarMiniGame)
     * @param inventory     player inventory
     * @param skillSystem   player skill system
     * @param criminalRecord player criminal record
     * @param notoriety     player notoriety system
     * @param npcs          current NPC list (for rumour seeding)
     * @param rumourNetwork the world's rumour network
     * @param cb            achievement callback
     * @return {@link BoutResult}
     */
    public BoutResult resolveFridayNightFight(float hour, int dayOfWeek, int currentDay,
                                               boolean playerWon, Inventory inventory,
                                               StreetSkillSystem skillSystem,
                                               CriminalRecord criminalRecord,
                                               NotorietySystem notoriety,
                                               List<NPC> npcs, RumourNetwork rumourNetwork,
                                               AchievementCallback cb) {
        if (!isFightNightActive(hour, dayOfWeek)) {
            return BoutResult.NOT_AVAILABLE;
        }
        if (!signedUpForFightNight) {
            return BoutResult.NOT_AVAILABLE;
        }
        // Pat-down: check for loaded gloves
        if (inventory.getItemCount(Material.LOADED_GLOVE) > 0) {
            if (random.nextFloat() < LOADED_GLOVE_CATCH_CHANCE) {
                criminalRecord.record(CriminalRecord.CrimeType.FIGHT_FIXING);
                notoriety.addNotoriety(NOTORIETY_FIGHT_FIXING);
                banExpiry = currentDay + LOADED_GLOVE_BAN_DAYS;
                signedUpForFightNight = false;
                if (cb != null) cb.award(AchievementType.LOADED_GLOVES);
                return BoutResult.DISQUALIFIED;
            }
        }
        // First bout achievement
        if (cb != null) cb.award(AchievementType.FIRST_BOUT);
        signedUpForFightNight = false;
        if (playerWon) {
            inventory.addItem(Material.COIN, FRIDAY_WIN_PRIZE);
            for (int i = 0; i < FRIDAY_WIN_SKILL_POINTS; i++) {
                skillSystem.awardSkillPoint(StreetSkillSystem.Skill.BOXING);
            }
            fridayNightWins++;
            // Seed STREET_TALENT rumour
            if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
                NPC seeder = findNPC(npcs, NPCType.BOXING_COACH);
                if (seeder == null) seeder = npcs.get(0);
                rumourNetwork.addRumour(seeder,
                        new Rumour(RumourType.STREET_TALENT,
                                "That lad from Tommy's — proper fighter, word is."));
            }
            // Achievements
            if (cb != null) {
                cb.award(AchievementType.AMATEUR_CHAMPION);
                if (fridayNightWins >= WINS_TO_UNLOCK_CIRCUIT && !circuitUnlocked) {
                    circuitUnlocked = true;
                    // Seed UNDERGROUND_FIGHT rumour
                    if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
                        NPC promoter = findNPC(npcs, NPCType.FIGHT_PROMOTER);
                        if (promoter == null) promoter = npcs.get(0);
                        rumourNetwork.addRumour(promoter,
                                new Rumour(RumourType.UNDERGROUND_FIGHT,
                                        "Heard there's an underground fight night on Saturday — serious money."));
                    }
                }
            }
            return BoutResult.WIN;
        } else {
            // Loss: consolation protein bar from Tommy
            inventory.addItem(Material.PROTEIN_BAR, 1);
            return BoutResult.LOSS;
        }
    }

    // ── Underground White-Collar Circuit ────────────────────────────────────

    /**
     * Attempt to enter the underground white-collar circuit bout.
     * Only available if the circuit has been unlocked (2 fight-night wins) and
     * it is an alternate Saturday at {@value #CIRCUIT_START_HOUR}:00+.
     *
     * @param hour       current in-game hour
     * @param dayOfWeek  current day of week
     * @param weekNumber the current in-game week number (for alternate Saturday check)
     * @param playerWon  true if the player won (determined by BattleBarMiniGame)
     * @param inventory  player inventory
     * @param skillSystem player skill system
     * @param criminalRecord player criminal record
     * @param notoriety   player notoriety system
     * @param npcs        current NPC list
     * @param rumourNetwork the world's rumour network
     * @param cb          achievement callback
     * @return {@link BoutResult}
     */
    public BoutResult resolveCircuitBout(float hour, int dayOfWeek, int weekNumber,
                                          boolean playerWon, Inventory inventory,
                                          StreetSkillSystem skillSystem,
                                          CriminalRecord criminalRecord,
                                          NotorietySystem notoriety,
                                          List<NPC> npcs, RumourNetwork rumourNetwork,
                                          AchievementCallback cb) {
        if (!circuitUnlocked) {
            return BoutResult.NOT_AVAILABLE;
        }
        if (!isCircuitNightActive(hour, dayOfWeek, weekNumber)) {
            return BoutResult.NOT_AVAILABLE;
        }
        // Pat-down: check for loaded gloves
        if (inventory.getItemCount(Material.LOADED_GLOVE) > 0) {
            if (random.nextFloat() < LOADED_GLOVE_CATCH_CHANCE) {
                criminalRecord.record(CriminalRecord.CrimeType.FIGHT_FIXING);
                notoriety.addNotoriety(NOTORIETY_FIGHT_FIXING);
                if (cb != null) cb.award(AchievementType.LOADED_GLOVES);
                return BoutResult.DISQUALIFIED;
            }
        }
        if (playerWon) {
            inventory.addItem(Material.COIN, CIRCUIT_WIN_PRIZE);
            inventory.addItem(Material.FIGHT_PURSE, 1);
            // Seed UNDERGROUND_FIGHT rumour
            if (npcs != null && rumourNetwork != null && !npcs.isEmpty()) {
                NPC promoter = findNPC(npcs, NPCType.FIGHT_PROMOTER);
                if (promoter == null) promoter = npcs.get(0);
                rumourNetwork.addRumour(promoter,
                        new Rumour(RumourType.UNDERGROUND_FIGHT,
                                "Someone just cleaned up at the white-collar night — serious payday."));
            }
            if (cb != null) cb.award(AchievementType.WHITE_COLLAR_WINNER);
            return BoutResult.WIN;
        } else {
            return BoutResult.LOSS;
        }
    }

    // ── Wayne's bout-fixing bribe ────────────────────────────────────────────

    /**
     * Resolve Wayne's bout-fixing bribe offer.
     *
     * <p>The player can accept or decline. If accepted:
     * <ul>
     *   <li>{@value #BOUT_FIX_SUCCESS_CHANCE * 100}% chance: bribe succeeds →
     *       {@link FixResult#FIXED_SUCCESS} + {@link AchievementType#FIXED_FIGHT}.</li>
     *   <li>{@value #BOUT_FIX_GRASS_CHANCE * 100}% chance: Wayne grasses →
     *       {@link CriminalRecord.CrimeType#BOUT_FIXING} + Notoriety
     *       +{@value #NOTORIETY_BOUT_FIXING} → {@link FixResult#GRASSED}.</li>
     * </ul>
     *
     * @param accepted       whether the player accepted the bribe
     * @param criminalRecord player criminal record
     * @param notoriety      player notoriety system
     * @param cb             achievement callback
     * @return {@link FixResult}
     */
    public FixResult resolveWayneBribe(boolean accepted, CriminalRecord criminalRecord,
                                        NotorietySystem notoriety, AchievementCallback cb) {
        if (!accepted) {
            return FixResult.DECLINED;
        }
        if (random.nextFloat() < BOUT_FIX_GRASS_CHANCE) {
            // Wayne grasses
            criminalRecord.record(CriminalRecord.CrimeType.BOUT_FIXING);
            notoriety.addNotoriety(NOTORIETY_BOUT_FIXING);
            return FixResult.GRASSED;
        }
        // Bribe succeeds
        if (cb != null) cb.award(AchievementType.FIXED_FIGHT);
        return FixResult.FIXED_SUCCESS;
    }

    // ── Tommy's Trophy Quest ─────────────────────────────────────────────────

    /**
     * Mark that the player has read the trophy quest clue from the noticeboard.
     */
    public void readTrophyClue() {
        trophyClueRead = true;
    }

    /**
     * Returns true if the player has read the trophy quest clue.
     */
    public boolean hasTrophyClue() {
        return trophyClueRead;
    }

    /**
     * Attempt to return the ABA Trophy to the BOXING_TROPHY_CABINET_PROP.
     * Requires the player to have {@link Material#ABA_TROPHY} in inventory and
     * the trophy quest clue to have been read.
     *
     * <p>On success:
     * <ul>
     *   <li>ABA_TROPHY removed from inventory.</li>
     *   <li>Permanent membership granted.</li>
     *   <li>BOXING +{@value #TROPHY_QUEST_SKILL_REWARD} skill points awarded.</li>
     *   <li>{@link AchievementType#TOMMY_BOY} and
     *       {@link AchievementType#LEGACY_OF_THE_RING} achievements awarded.</li>
     * </ul>
     *
     * @param inventory   player inventory
     * @param skillSystem player skill system
     * @param cb          achievement callback
     * @return {@link TrophyQuestResult}
     */
    public TrophyQuestResult returnTrophy(Inventory inventory, StreetSkillSystem skillSystem,
                                           AchievementCallback cb) {
        if (trophyQuestComplete) {
            return TrophyQuestResult.ALREADY_COMPLETE;
        }
        if (inventory.getItemCount(Material.ABA_TROPHY) == 0) {
            return TrophyQuestResult.NO_TROPHY;
        }
        inventory.removeItem(Material.ABA_TROPHY, 1);
        permanentMember = true;
        for (int i = 0; i < TROPHY_QUEST_SKILL_REWARD; i++) {
            skillSystem.awardSkillPoint(StreetSkillSystem.Skill.BOXING);
        }
        trophyQuestComplete = true;
        if (cb != null) {
            cb.award(AchievementType.TOMMY_BOY);
            cb.award(AchievementType.LEGACY_OF_THE_RING);
        }
        return TrophyQuestResult.COMPLETE;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /** Returns the number of training sessions completed today. */
    public int getSessionsToday() { return sessionsToday; }

    /** Returns the total number of Friday Night Fight wins. */
    public int getFridayNightWins() { return fridayNightWins; }

    /** Returns whether the underground circuit is unlocked. */
    public boolean isCircuitUnlocked() { return circuitUnlocked; }

    /** Returns whether the player has permanent membership. */
    public boolean hasPermanentMembership() { return permanentMember; }

    /** Returns whether the trophy quest has been completed. */
    public boolean isTrophyQuestComplete() { return trophyQuestComplete; }

    /** Returns whether the player is currently signed up for fight night. */
    public boolean isSignedUpForFightNight() { return signedUpForFightNight; }

    /** Returns whether the player is currently banned from the boxing club. */
    public boolean isBanned(int currentDay) {
        return banExpiry >= 0 && currentDay < banExpiry;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Set friday night wins directly (for testing). */
    public void setFridayNightWinsForTesting(int wins) {
        this.fridayNightWins = wins;
        if (wins >= WINS_TO_UNLOCK_CIRCUIT) {
            circuitUnlocked = true;
        }
    }

    /** Set whether player is signed up for fight night (for testing). */
    public void setSignedUpForTesting(boolean signedUp) {
        this.signedUpForFightNight = signedUp;
    }

    /** Set training sessions today (for testing). */
    public void setSessionsTodayForTesting(int sessions, int day) {
        this.sessionsToday = sessions;
        this.lastTrainingDay = day;
    }

    /** Set hadFirstSession (for testing). */
    public void setHadFirstSessionForTesting(boolean had) {
        this.hadFirstSession = had;
    }

    /** Set permanent membership directly (for testing). */
    public void setPermanentMemberForTesting(boolean permanent) {
        this.permanentMember = permanent;
    }

    /** Set trophy clue read state (for testing). */
    public void setTrophyClueReadForTesting(boolean read) {
        this.trophyClueRead = read;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private NPC findNPC(List<NPC> npcs, NPCType type) {
        for (NPC npc : npcs) {
            if (npc.getType() == type) return npc;
        }
        return null;
    }
}
