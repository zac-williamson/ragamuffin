package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1491: Northfield Annual Dog Show — Clive's Rosettes, the Pedigree Fraud
 * &amp; the Trophy Cabinet Heist.
 *
 * <p>A park-based annual dog show running on day 15 (mod 28) from 09:00–14:00.
 *
 * <h3>Mechanic 1 — Entry &amp; Grooming (09:00–10:00)</h3>
 * Player must have an active dog companion. Pay 3 COIN to Clive ({@code JUDGE_NPC})
 * to enter. Dog's grooming recency and known tricks contribute to the judging score.
 *
 * <h3>Mechanic 2 — Judging (10:30–12:00)</h3>
 * Clive scores each dog: Grooming (0–30), Bond level (0–40), Performance/tricks (0–30).
 * Winston the Marchetti-associated Staffy always scores 85 (fixed).
 * Beat 85 legitimately → {@code LEGITIMATE_CHAMPION} + {@code BEST_IN_SHOW_ROSETTE_PROP} + 10 COIN.
 * Runner-up: {@code RESERVE_ROSETTE_PROP}. Third: {@code THIRD_PLACE_ROSETTE_PROP}.
 *
 * <h3>Mechanic 3 — Bribing Clive</h3>
 * Approach Clive pre-judging with 15 COIN. 60% success adds +20 to player score.
 * Failure spawns show steward and adds WantedLevel. {@code BENT_JUDGE} awarded on win.
 * Newspaper exposé via {@link NewspaperSystem} awards {@code WHISTLEBLOWER} and
 * costs Marchetti −15 reputation.
 *
 * <h3>Mechanic 4 — Trophy Cabinet Heist</h3>
 * {@code DOG_SHOW_TROPHY_CABINET_PROP} contains last year's {@code DOG_SHOW_ROSETTE}.
 * Lockpick during 13:30–14:00 heist window when Clive has left.
 * {@code KENNEL_HAND} watches within 8 blocks outside window.
 * Unwitnessed = no crime. Witnessed = {@code CrimeType.THEFT} + Notoriety +6 + WantedLevel +1.
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link DogCompanionSystem} — dog must exist; bond/grooming affect score.</li>
 *   <li>{@link NotorietySystem} — bribery/witnessed heist add notoriety.</li>
 *   <li>{@link WantedSystem} — failed bribe and witnessed heist add wanted stars.</li>
 *   <li>{@link FactionSystem} — MARCHETTI_CREW −15 after WHISTLEBLOWER exposé.</li>
 *   <li>{@link NewspaperSystem} — tipOffJournalist path for WHISTLEBLOWER.</li>
 *   <li>{@link RumourNetwork} — DOG_SHOW_FIXED and DOG_SHOW_RIGGING rumours.</li>
 *   <li>{@link CriminalRecord} — THEFT on witnessed heist; SHOW_RIGGING on bribery.</li>
 * </ul>
 */
public class DogShowSystem {

    // ── Schedule constants ─────────────────────────────────────────────────

    /** Show day within a 28-day cycle (day 15). */
    public static final int SHOW_DAY_OF_CYCLE = 15;

    /** Length of the repeating show cycle in days. */
    public static final int SHOW_CYCLE_DAYS = 28;

    /** Show opening hour (09:00). */
    public static final float SHOW_OPEN_HOUR = 9.0f;

    /** Entry window closes (10:00). */
    public static final float ENTRY_CLOSE_HOUR = 10.0f;

    /** Judging starts (10:30). */
    public static final float JUDGING_START_HOUR = 10.5f;

    /** Judging ends (12:00). */
    public static final float JUDGING_END_HOUR = 12.0f;

    /** Heist window opens — Clive leaves (13:30). */
    public static final float HEIST_WINDOW_OPEN_HOUR = 13.5f;

    /** Show closes / heist window closes (14:00). */
    public static final float SHOW_CLOSE_HOUR = 14.0f;

    // ── Entry &amp; judging constants ────────────────────────────────────────

    /** Cost to enter the dog show. */
    public static final int ENTRY_COST = 3;

    /** Winston's fixed score — the Marchetti-associated Staffy always scores 85. */
    public static final int WINSTON_FIXED_SCORE = 85;

    /** Bribery cost. */
    public static final int BRIBE_COST = 15;

    /** Probability (0–1) that a bribery attempt succeeds. */
    public static final float BRIBE_SUCCESS_PROBABILITY = 0.60f;

    /** Score bonus added to the player's total on successful bribe. */
    public static final int BRIBE_SCORE_BONUS = 20;

    /** Prize for winning Best in Show. */
    public static final int BEST_IN_SHOW_PRIZE_COIN = 10;

    /** KENNEL_HAND watch radius during heist window. */
    public static final float KENNEL_HAND_WATCH_RADIUS = 8f;

    /** Notoriety gain for a witnessed heist. */
    public static final int HEIST_NOTORIETY = 6;

    /** Wanted star gain for a witnessed heist. */
    public static final int HEIST_WANTED_STARS = 1;

    /** Marchetti Crew reputation cost when the WHISTLEBLOWER exposé is published. */
    public static final int WHISTLEBLOWER_MARCHETTI_REPUTATION_HIT = -15;

    // ── Result enums ───────────────────────────────────────────────────────

    /** Result codes for entering the dog show. */
    public enum EntryResult {
        SUCCESS,
        SHOW_NOT_OPEN,
        ENTRY_WINDOW_CLOSED,
        NO_DOG,
        ALREADY_ENTERED,
        INSUFFICIENT_FUNDS
    }

    /** Result codes for bribing Clive. */
    public enum BribeResult {
        SUCCESS,
        FAILED_CAUGHT,
        ALREADY_JUDGED,
        SHOW_NOT_OPEN,
        INSUFFICIENT_FUNDS,
        CLIVE_NOT_PRESENT,
        JUDGING_ALREADY_STARTED
    }

    /** Result codes for the trophy cabinet heist. */
    public enum HeistResult {
        SUCCESS,
        NO_LOCKPICK,
        OUTSIDE_HEIST_WINDOW,
        WITNESSED,
        ALREADY_LOOTED
    }

    /** Judging outcome for the player's dog. */
    public enum JudgingPlacement {
        BEST_IN_SHOW,
        RESERVE,
        THIRD_PLACE,
        UNPLACED
    }

    // ── State ──────────────────────────────────────────────────────────────

    private final Random random;

    /** Day index of the current/last show we've set up. */
    private int lastShowDay = -1;

    /** Whether the player has entered this year's show. */
    private boolean playerEntered = false;

    /** Whether judging has been completed this show. */
    private boolean judgingComplete = false;

    /** Whether the trophy cabinet has been looted this show. */
    private boolean cabinetLooted = false;

    /** Player's score before any bribery modifier. */
    private int playerBaseScore = 0;

    /** Bonus applied by successful bribery. */
    private int bribeBonus = 0;

    /** Whether the player bribed Clive (for crime/achievement tracking). */
    private boolean playerBribed = false;

    /** Whether the SHOW_DAY achievement has been awarded. */
    private boolean showDayAwarded = false;

    /** Whether the LEGITIMATE_CHAMPION achievement has been awarded. */
    private boolean legitimateChampionAwarded = false;

    /** Whether the BENT_JUDGE achievement has been awarded. */
    private boolean bentJudgeAwarded = false;

    /** Whether the WHISTLEBLOWER achievement has been awarded. */
    private boolean whistleblowerAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────

    public DogShowSystem() {
        this(new Random());
    }

    public DogShowSystem(Random random) {
        this.random = random;
    }

    // ── Show schedule ─────────────────────────────────────────────────────

    /**
     * Returns true if the dog show is active on the given day and hour.
     * Show runs on day ({@code dayCount % SHOW_CYCLE_DAYS == SHOW_DAY_OF_CYCLE})
     * between {@link #SHOW_OPEN_HOUR} and {@link #SHOW_CLOSE_HOUR}.
     */
    public boolean isShowActive(TimeSystem timeSystem) {
        int day = timeSystem.getDayCount();
        float hour = timeSystem.getTime();
        return (day % SHOW_CYCLE_DAYS == SHOW_DAY_OF_CYCLE)
                && hour >= SHOW_OPEN_HOUR
                && hour < SHOW_CLOSE_HOUR;
    }

    /**
     * Returns true if the entry window is open (show active and before 10:00).
     */
    public boolean isEntryWindowOpen(TimeSystem timeSystem) {
        return isShowActive(timeSystem) && timeSystem.getTime() < ENTRY_CLOSE_HOUR;
    }

    /**
     * Returns true if judging is in progress (10:30–12:00).
     */
    public boolean isJudgingInProgress(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return isShowActive(timeSystem) && hour >= JUDGING_START_HOUR && hour < JUDGING_END_HOUR;
    }

    /**
     * Returns true if the heist window is open (13:30–14:00, Clive has left).
     */
    public boolean isHeistWindowOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        return isShowActive(timeSystem) && hour >= HEIST_WINDOW_OPEN_HOUR && hour < SHOW_CLOSE_HOUR;
    }

    // ── Per-frame update ───────────────────────────────────────────────────

    /**
     * Call once per frame to advance the dog show simulation.
     *
     * <p>Handles automatic judging when the judging window starts (if player entered)
     * and resets state at the start of each new show day.
     *
     * @param delta               seconds since last frame
     * @param timeSystem          game time
     * @param dogCompanionSystem  player's dog companion system (for scoring)
     * @param playerInventory     player's inventory
     * @param allNpcs             all NPCs in the world
     * @param achievementCallback achievement callback (may be null)
     * @param notorietySystem     for notoriety tracking (may be null)
     * @param rumourNetwork       for seeding rumours (may be null)
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       DogCompanionSystem dogCompanionSystem,
                       Inventory playerInventory,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback,
                       NotorietySystem notorietySystem,
                       RumourNetwork rumourNetwork) {

        int day = timeSystem.getDayCount();

        // Reset state when a new show day begins
        if (day % SHOW_CYCLE_DAYS == SHOW_DAY_OF_CYCLE && day != lastShowDay) {
            resetShowState(day);
        }

        if (!isShowActive(timeSystem)) return;

        // Trigger judging automatically when judging window opens
        if (!judgingComplete && playerEntered && isJudgingInProgress(timeSystem)) {
            resolveJudging(dogCompanionSystem, playerInventory, allNpcs,
                    achievementCallback, notorietySystem, rumourNetwork);
        }
    }

    // ── Mechanic 1: Entry ─────────────────────────────────────────────────

    /**
     * Player pays to enter the dog show.
     *
     * <p>Requirements: show entry window open, dog companion present, not already entered,
     * and sufficient COIN.
     *
     * @param timeSystem          for checking schedule
     * @param dogCompanionSystem  must have a dog
     * @param inventory           player inventory (3 COIN deducted on success)
     * @param achievementCallback for SHOW_DAY achievement
     * @return EntryResult describing outcome
     */
    public EntryResult enterShow(TimeSystem timeSystem,
                                  DogCompanionSystem dogCompanionSystem,
                                  Inventory inventory,
                                  NotorietySystem.AchievementCallback achievementCallback) {

        if (!isShowActive(timeSystem)) return EntryResult.SHOW_NOT_OPEN;
        if (!isEntryWindowOpen(timeSystem)) return EntryResult.ENTRY_WINDOW_CLOSED;
        if (dogCompanionSystem == null || !dogCompanionSystem.hasDog()) return EntryResult.NO_DOG;
        if (playerEntered) return EntryResult.ALREADY_ENTERED;
        if (inventory.getItemCount(Material.COIN) < ENTRY_COST) return EntryResult.INSUFFICIENT_FUNDS;

        inventory.removeItem(Material.COIN, ENTRY_COST);
        playerEntered = true;

        // SHOW_DAY achievement: first time entering the show
        if (!showDayAwarded && achievementCallback != null) {
            showDayAwarded = true;
            achievementCallback.award(AchievementType.SHOW_DAY);
        }

        return EntryResult.SUCCESS;
    }

    // ── Mechanic 2: Judging ───────────────────────────────────────────────

    /**
     * Calculate the player's dog judging score based on dog attributes.
     *
     * <p>Score breakdown:
     * <ul>
     *   <li>Grooming score (0–30): {@code groomingRecent} adds 20, base 10</li>
     *   <li>Bond score (0–40): proportional to bond level (bond/100 × 40)</li>
     *   <li>Performance score (0–30): 10 per known trick (capped at 30)</li>
     * </ul>
     *
     * @param dogCompanionSystem the dog companion system to score
     * @param groomingRecent     whether the dog was groomed recently (within show day)
     * @return calculated score 0–100
     */
    public int calculatePlayerScore(DogCompanionSystem dogCompanionSystem,
                                     boolean groomingRecent) {
        if (dogCompanionSystem == null || !dogCompanionSystem.hasDog()) return 0;

        // Grooming: fresh groom adds +20 to base 10
        int groomingScore = groomingRecent ? 30 : 10;

        // Bond: 0–40, proportional to bond level
        int bondLevel = dogCompanionSystem.getDogBondLevel();
        int bondScore = Math.min(40, (int)(bondLevel / 100.0f * 40f));

        // Performance/tricks: 10 per trick, capped at 30
        int tricksKnown = dogCompanionSystem.getTricksLearned().size();
        int performanceScore = Math.min(30, tricksKnown * 10);

        return groomingScore + bondScore + performanceScore;
    }

    /**
     * Resolve judging: score the player's dog, compare to Winston (85), determine placement.
     *
     * @param dogCompanionSystem  player's dog system
     * @param playerInventory     for prize money
     * @param allNpcs             for rumour seeding
     * @param achievementCallback for achievements
     * @param notorietySystem     (currently unused in judging; reserved for future)
     * @param rumourNetwork       for seeding post-show rumours
     * @return the player's placement
     */
    public JudgingPlacement resolveJudging(DogCompanionSystem dogCompanionSystem,
                                            Inventory playerInventory,
                                            List<NPC> allNpcs,
                                            NotorietySystem.AchievementCallback achievementCallback,
                                            NotorietySystem notorietySystem,
                                            RumourNetwork rumourNetwork) {
        if (judgingComplete) return JudgingPlacement.UNPLACED;
        judgingComplete = true;

        // If player didn't enter, no placement
        if (!playerEntered || dogCompanionSystem == null || !dogCompanionSystem.hasDog()) {
            return JudgingPlacement.UNPLACED;
        }

        // Calculate base score (assume not groomed recently by default; caller may
        // set groomingRecent via overload — here we use playerBaseScore if pre-set)
        if (playerBaseScore == 0) {
            // No pre-set score: compute fresh (no recent groom assumed)
            playerBaseScore = calculatePlayerScore(dogCompanionSystem, false);
        }

        int finalScore = playerBaseScore + bribeBonus;

        JudgingPlacement placement;
        if (finalScore > WINSTON_FIXED_SCORE) {
            // BEST IN SHOW
            placement = JudgingPlacement.BEST_IN_SHOW;
            if (playerInventory != null) {
                playerInventory.addItem(Material.BEST_IN_SHOW_ROSETTE_PROP, 1);
                playerInventory.addItem(Material.COIN, BEST_IN_SHOW_PRIZE_COIN);
            }
            // LEGITIMATE_CHAMPION only if player did not bribe
            if (!playerBribed && !legitimateChampionAwarded && achievementCallback != null) {
                legitimateChampionAwarded = true;
                achievementCallback.award(AchievementType.LEGITIMATE_CHAMPION);
            }
            // Seed a positive show rumour
            seedRumour(rumourNetwork, allNpcs, RumourType.COMMUNITY_WIN,
                    "Someone's dog just beat Winston at the dog show! Clive looked furious.");
        } else if (finalScore >= WINSTON_FIXED_SCORE - 10) {
            // RESERVE (within 10 of Winston)
            placement = JudgingPlacement.RESERVE;
            if (playerInventory != null) {
                playerInventory.addItem(Material.RESERVE_ROSETTE_PROP, 1);
            }
        } else if (finalScore >= WINSTON_FIXED_SCORE - 25) {
            // THIRD PLACE (within 25 of Winston)
            placement = JudgingPlacement.THIRD_PLACE;
            if (playerInventory != null) {
                playerInventory.addItem(Material.THIRD_PLACE_ROSETTE_PROP, 1);
            }
        } else {
            placement = JudgingPlacement.UNPLACED;
        }

        return placement;
    }

    /**
     * Resolve judging with explicit grooming status.
     */
    public JudgingPlacement resolveJudging(DogCompanionSystem dogCompanionSystem,
                                            boolean groomingRecent,
                                            Inventory playerInventory,
                                            List<NPC> allNpcs,
                                            NotorietySystem.AchievementCallback achievementCallback,
                                            NotorietySystem notorietySystem,
                                            RumourNetwork rumourNetwork) {
        if (!judgingComplete && playerEntered && dogCompanionSystem != null
                && dogCompanionSystem.hasDog()) {
            playerBaseScore = calculatePlayerScore(dogCompanionSystem, groomingRecent);
        }
        return resolveJudging(dogCompanionSystem, playerInventory, allNpcs,
                achievementCallback, notorietySystem, rumourNetwork);
    }

    // ── Mechanic 3: Bribery ───────────────────────────────────────────────

    /**
     * Attempt to bribe Clive pre-judging to add +20 to the player's dog score.
     *
     * <p>Requirements: show active, pre-judging, player entered, Clive (JUDGE_NPC)
     * present among {@code allNpcs}, and 15 COIN in inventory.
     *
     * <p>60% chance of success. On failure: SHOW_RIGGING crime recorded,
     * WantedSystem +1 star, DOG_SHOW_FIXED rumour seeded.
     *
     * @param timeSystem          for schedule checking
     * @param inventory           player inventory (15 COIN deducted on attempt)
     * @param cliveNearby         true if the JUDGE_NPC is within interaction range
     * @param allNpcs             all NPCs (for rumour seeding)
     * @param achievementCallback for BENT_JUDGE
     * @param criminalRecord      for SHOW_RIGGING crime on failure
     * @param wantedSystem        for adding wanted stars on failure
     * @param notorietySystem     (reserved)
     * @param rumourNetwork       for seeding DOG_SHOW_FIXED on failure
     * @return BribeResult describing outcome
     */
    public BribeResult bribeClive(TimeSystem timeSystem,
                                   Inventory inventory,
                                   boolean cliveNearby,
                                   List<NPC> allNpcs,
                                   NotorietySystem.AchievementCallback achievementCallback,
                                   CriminalRecord criminalRecord,
                                   WantedSystem wantedSystem,
                                   NotorietySystem notorietySystem,
                                   RumourNetwork rumourNetwork) {

        if (!isShowActive(timeSystem)) return BribeResult.SHOW_NOT_OPEN;
        if (isJudgingInProgress(timeSystem) || judgingComplete) return BribeResult.JUDGING_ALREADY_STARTED;
        if (!cliveNearby) return BribeResult.CLIVE_NOT_PRESENT;
        if (inventory.getItemCount(Material.COIN) < BRIBE_COST) return BribeResult.INSUFFICIENT_FUNDS;

        inventory.removeItem(Material.COIN, BRIBE_COST);

        boolean success = random.nextFloat() < BRIBE_SUCCESS_PROBABILITY;

        if (success) {
            bribeBonus = BRIBE_SCORE_BONUS;
            playerBribed = true;

            // BENT_JUDGE achievement
            if (!bentJudgeAwarded && achievementCallback != null) {
                bentJudgeAwarded = true;
                achievementCallback.award(AchievementType.BENT_JUDGE);
            }

            // Seed DOG_SHOW_FIXED rumour — others start to gossip
            seedRumour(rumourNetwork, allNpcs, RumourType.DOG_SHOW_FIXED,
                    "Word is Clive took a bung — the judging was fixed. Winston shouldn't have won.");

            return BribeResult.SUCCESS;
        } else {
            // Caught — record crime, add wanted star
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.SHOW_RIGGING);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(1);
            }
            seedRumour(rumourNetwork, allNpcs, RumourType.DOG_SHOW_FIXED,
                    "Someone tried to bribe Clive at the dog show. Show steward involved now.");
            return BribeResult.FAILED_CAUGHT;
        }
    }

    // ── Whistleblower path ────────────────────────────────────────────────

    /**
     * Tip off the journalist at The Daily Ragamuffin to expose the bribery,
     * awarding {@code WHISTLEBLOWER} and costing Marchetti −15 reputation.
     *
     * <p>Requires that the player previously bribed Clive (i.e. {@link #playerBribed}
     * is true). Uses {@link NewspaperSystem#tipOffJournalist} with an InfamyEvent.
     *
     * @param inventory           player inventory (tip-off costs handled by NewspaperSystem)
     * @param newspaperSystem     the newspaper system
     * @param factionSystem       for Marchetti reputation hit
     * @param achievementCallback for WHISTLEBLOWER
     * @param allNpcs             for rumour seeding
     * @param rumourNetwork       for DOG_SHOW_RIGGING rumour
     * @return true if the exposé was submitted, false otherwise
     */
    public boolean exposeToNewspaper(Inventory inventory,
                                      NewspaperSystem newspaperSystem,
                                      FactionSystem factionSystem,
                                      NotorietySystem.AchievementCallback achievementCallback,
                                      List<NPC> allNpcs,
                                      RumourNetwork rumourNetwork) {
        if (!playerBribed) return false;
        if (newspaperSystem == null) return false;

        // Build an infamy event for the exposé
        NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
                "DOG_SHOW_RIGGING",
                "Northfield Dog Show",
                null,
                "Clive",
                0,
                "WHISTLEBLOWER",
                Faction.MARCHETTI_CREW,
                5
        );

        boolean tipped = newspaperSystem.tipOffJournalist(event, inventory);
        if (!tipped) return false;

        // Marchetti reputation hit
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW,
                    WHISTLEBLOWER_MARCHETTI_REPUTATION_HIT);
        }

        // WHISTLEBLOWER achievement
        if (!whistleblowerAwarded && achievementCallback != null) {
            whistleblowerAwarded = true;
            achievementCallback.award(AchievementType.WHISTLEBLOWER);
        }

        // Seed DOG_SHOW_RIGGING rumour
        seedRumour(rumourNetwork, allNpcs, RumourType.DOG_SHOW_RIGGING,
                "Someone from The Daily Ragamuffin is looking into last year's dog show. "
                + "Clive's been dodgy for years.");

        return true;
    }

    // ── Mechanic 4: Trophy Cabinet Heist ─────────────────────────────────

    /**
     * Attempt the trophy cabinet heist — steal last year's {@code DOG_SHOW_ROSETTE}
     * from the {@code DOG_SHOW_TROPHY_CABINET_PROP} using a LOCKPICK.
     *
     * <p>Only available during the heist window (13:30–14:00) when Clive has left.
     * Outside this window the KENNEL_HAND watches within 8 blocks.
     *
     * <p>Unwitnessed = no crime, LOCKPICK consumed, DOG_SHOW_ROSETTE granted.
     * Witnessed = {@code CrimeType.THEFT} + Notoriety +6 + WantedLevel +1.
     *
     * @param timeSystem          for heist window check
     * @param inventory           player inventory (LOCKPICK consumed on success)
     * @param witnessed           true if KENNEL_HAND is within 8 blocks
     * @param allNpcs             for rumour seeding
     * @param achievementCallback (unused currently; reserved)
     * @param criminalRecord      for THEFT crime on witnessed heist
     * @param notorietySystem     for +6 notoriety on witnessed heist
     * @param wantedSystem        for +1 wanted star on witnessed heist
     * @param rumourNetwork       for DOG_SHOW_RIGGING rumour on witness
     * @return HeistResult describing outcome
     */
    public HeistResult attemptTrophyCabinetHeist(TimeSystem timeSystem,
                                                   Inventory inventory,
                                                   boolean witnessed,
                                                   List<NPC> allNpcs,
                                                   NotorietySystem.AchievementCallback achievementCallback,
                                                   CriminalRecord criminalRecord,
                                                   NotorietySystem notorietySystem,
                                                   WantedSystem wantedSystem,
                                                   RumourNetwork rumourNetwork) {

        if (!isShowActive(timeSystem) && !isHeistWindowOpen(timeSystem)) {
            // Allow if heist window is open even if show is technically ending
        }
        if (!isHeistWindowOpen(timeSystem)) return HeistResult.OUTSIDE_HEIST_WINDOW;
        if (cabinetLooted) return HeistResult.ALREADY_LOOTED;
        if (inventory.getItemCount(Material.LOCKPICK) < 1) return HeistResult.NO_LOCKPICK;

        if (witnessed) {
            // Crime committed even when witnessed
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.THEFT);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(HEIST_NOTORIETY, achievementCallback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(HEIST_WANTED_STARS);
            }
            seedRumour(rumourNetwork, allNpcs, RumourType.DOG_SHOW_RIGGING,
                    "Someone tried to nick the dog show trophy cabinet. Kennel hand caught 'em red-handed.");
            return HeistResult.WITNESSED;
        }

        // Unwitnessed success — no crime
        inventory.removeItem(Material.LOCKPICK, 1);
        inventory.addItem(Material.DOG_SHOW_ROSETTE, 1);
        cabinetLooted = true;

        return HeistResult.SUCCESS;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    /** @return true if the player has entered the current show. */
    public boolean isPlayerEntered() {
        return playerEntered;
    }

    /** @return true if judging has been resolved for the current show. */
    public boolean isJudgingComplete() {
        return judgingComplete;
    }

    /** @return true if the trophy cabinet has been looted this show. */
    public boolean isCabinetLooted() {
        return cabinetLooted;
    }

    /** @return the player's base score (before bribe bonus). */
    public int getPlayerBaseScore() {
        return playerBaseScore;
    }

    /** @return the bribe bonus applied to the player's score. */
    public int getBribeBonus() {
        return bribeBonus;
    }

    /** @return the player's total score (base + bribe bonus). */
    public int getPlayerTotalScore() {
        return playerBaseScore + bribeBonus;
    }

    /** @return true if the player successfully bribed Clive this show. */
    public boolean isPlayerBribed() {
        return playerBribed;
    }

    // ── Testing helpers ────────────────────────────────────────────────────

    /** Force-set player entered status for testing. */
    public void setPlayerEnteredForTesting(boolean entered) {
        this.playerEntered = entered;
    }

    /** Force-set the player base score for testing. */
    public void setPlayerBaseScoreForTesting(int score) {
        this.playerBaseScore = score;
    }

    /** Force-set bribe bonus for testing. */
    public void setBribeBonusForTesting(int bonus) {
        this.bribeBonus = bonus;
    }

    /** Force-set cabinet looted state for testing. */
    public void setCabinetLootedForTesting(boolean looted) {
        this.cabinetLooted = looted;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void resetShowState(int day) {
        lastShowDay = day;
        playerEntered = false;
        judgingComplete = false;
        cabinetLooted = false;
        playerBaseScore = 0;
        bribeBonus = 0;
        playerBribed = false;
    }

    private void seedRumour(RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             RumourType type,
                             String text) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(type, text);
        for (NPC npc : allNpcs) {
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                break;
            }
        }
    }
}
