package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.BettingUI;

import java.util.List;
import java.util.Random;

/**
 * Issue #1220: Northfield Ladbrokes — BettingShopSystem, FOBT Terminal &amp; the Race Fix.
 *
 * <p>Brings the {@code BETTING_SHOP} landmark to life, wiring up the existing
 * {@link HorseRacingSystem} and {@link GreyhoundRacingSystem} to a full betting-shop
 * experience: over-counter horse bets, FOBT roulette, a Marchetti race fix, after-hours
 * break-ins, and debt-spiral mechanics.
 *
 * <h3>Opening hours</h3>
 * Mon–Sat 09:00–22:00, Sun 10:00–18:00.
 *
 * <h3>Horse Racing (over-counter)</h3>
 * <ul>
 *   <li>Player presses E on Derek (BOOKIES_CLERK) or the counter to open BettingUI.</li>
 *   <li>Stake 1–50 COIN (100 on BENEFIT_DAY).</li>
 *   <li>{@link #placeBet} deducts COIN and adds BET_SLIP; returns a {@link BetResult}.</li>
 *   <li>{@link #resolveBets} is called when a race resolves; winning bets pay
 *       {@code stake × oddsNumerator}; RACING_CERT and FIRST_FLUTTER achievements awarded.</li>
 * </ul>
 *
 * <h3>FOBT Terminal</h3>
 * <ul>
 *   <li>Stake 1–20 COIN per spin. Red/Black: 18/38 → 2× stake. Single number: 1/38 → 36× stake.</li>
 *   <li>Session loss ≥ 10 COIN → {@code FOBT_RAGE} achievement.</li>
 *   <li>Session loss ≥ 30 COIN → LOAN_SHARK NPC spawns outside.</li>
 *   <li>GAMBLING XP +1 per spin via {@link StreetSkillSystem}.</li>
 * </ul>
 *
 * <h3>Race Fix</h3>
 * <ul>
 *   <li>MARCHETTI_LIEUTENANT offer (weekly); player calls {@link #acceptRaceFix}.</li>
 *   <li>Calls {@link GreyhoundRacingSystem#setRaceFixed(boolean)} — next race is rigged.</li>
 *   <li>Win → SURE_THING achievement + MARCHETTI_CREW respect +10 + Notoriety +5.</li>
 *   <li>20% chance Derek spots it → FRAUD crime + WantedSystem +1 star.</li>
 *   <li>Grass to police → WantedSystem −1 star + MARCHETTI_CREW −20 + GRASS achievement.</li>
 * </ul>
 *
 * <h3>After-hours break-in</h3>
 * Requires SCREWDRIVER; 10-second hold. Float tray drops 5–15 COIN + BETTING_SLIP_BLANK.
 * BURGLARY crime, Notoriety +8, FOLDED achievement, NeighbourhoodWatchSystem CCTV trigger.
 *
 * <h3>Integration points</h3>
 * HorseRacingSystem, GreyhoundRacingSystem, StreetEconomySystem, FactionSystem,
 * WantedSystem, NotorietySystem, CriminalRecord, RumourNetwork,
 * NeighbourhoodWatchSystem, StreetSkillSystem, TimeSystem, BettingUI, NPCManager.
 */
public class BettingShopSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** FOBT minimum stake (coins). */
    public static final int FOBT_MIN_STAKE = 1;

    /** FOBT maximum stake (coins). */
    public static final int FOBT_MAX_STAKE = 20;

    /** FOBT cumulative session loss that triggers FOBT_RAGE achievement. */
    public static final int FOBT_RAGE_THRESHOLD = 10;

    /** FOBT cumulative session loss that triggers LOAN_SHARK spawn. */
    public static final int FOBT_LOAN_SHARK_THRESHOLD = 30;

    /** Max stake on fixed race (Marchetti offer). */
    public static final int RACE_FIX_MAX_STAKE = 30;

    /** Probability roulette spin hits Red or Black (18/38). */
    private static final float FOBT_RED_BLACK_PROB = 18f / 38f;

    /** Probability roulette spin hits a single specific number (1/38). */
    private static final float FOBT_NUMBER_PROB = 1f / 38f;

    /** 20% chance Derek spots race fix. */
    private static final float RACE_FIX_SPOTTED_PROB = 0.20f;

    /** Notoriety gained on race fix win. */
    public static final int NOTORIETY_RACE_FIX_WIN = 5;

    /** Notoriety gained on after-hours break-in. */
    public static final int NOTORIETY_BREAK_IN = 8;

    /** Notoriety gained on FOBT smash. */
    public static final int NOTORIETY_FOBT_SMASH = 5;

    /** GAMBLING XP per FOBT spin. */
    public static final int GAMBLING_XP_FOBT = 1;

    /** GAMBLING XP per horse bet placed. */
    public static final int GAMBLING_XP_HORSE_BET = 2;

    /** Win amount threshold for RACING_CERT and BIG_WIN_AT_BOOKIES rumour. */
    public static final int BIG_WIN_THRESHOLD = 10;

    /** Number of NPCs to seed the BIG_WIN_AT_BOOKIES rumour into. */
    private static final int BIG_WIN_RUMOUR_SEED_COUNT = 3;

    /** Float tray minimum drop (coins). */
    public static final int FLOAT_TRAY_MIN = 5;

    /** Float tray maximum drop (coins). */
    public static final int FLOAT_TRAY_MAX = 15;

    // ── FOBT bet type ─────────────────────────────────────────────────────────

    /**
     * The type of FOBT roulette bet the player places.
     */
    public enum FOBTBetType {
        /** Bet on Red (covers 18 numbers). */
        RED,
        /** Bet on Black (covers 18 numbers). */
        BLACK,
        /** Bet on a single number (1/38 win chance). */
        NUMBER
    }

    /** Result of a single FOBT spin. */
    public enum FOBTResult {
        /** Player won: 2× stake (Red/Black) or 36× stake (Number). */
        WIN,
        /** Player lost the spin. */
        LOSS
    }

    // ── Bet result ────────────────────────────────────────────────────────────

    /**
     * Result codes returned by {@link #placeBet}.
     */
    public enum BetResult {
        /** Bet accepted; COIN deducted; BET_SLIP added. */
        ACCEPTED,
        /** Player has insufficient COIN. */
        INSUFFICIENT_FUNDS,
        /** Race is already resolved (too late to bet). */
        RACE_CLOSED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final HorseRacingSystem horseRacingSystem;
    private final GreyhoundRacingSystem greyhoundRacingSystem;

    /** Active bets placed via this system (parallel to HorseRacingSystem's own tracking). */
    private final java.util.List<PendingBet> pendingBets = new java.util.ArrayList<>();

    /** Form guide bonus active for the remainder of the day. */
    private boolean formGuideActive = false;

    /** In-game day on which formGuide was activated. */
    private int formGuideDay = -1;

    /** Whether the player has bet for the first time ever. */
    private boolean firstBetPlaced = false;

    /** Cumulative FOBT net loss in the current session. */
    private int fobtSessionLoss = 0;

    /** Whether FOBT_RAGE has been awarded this session. */
    private boolean fobtRageAwarded = false;

    /** Whether FOBT loan shark has been spawned this session. */
    private boolean fobtLoanSharkSpawned = false;

    /** Whether a race fix offer is currently pending from Marchetti Lieutenant. */
    private boolean raceFixPending = false;

    /** Whether FOLDED achievement has been awarded. */
    private boolean foldedAwarded = false;

    /** Whether SURE_THING has been awarded. */
    private boolean sureThingAwarded = false;

    // ── Pending bet record ────────────────────────────────────────────────────

    /** Tracks a horse bet placed through BettingShopSystem. */
    private static class PendingBet {
        final int raceIndex;
        final int horseIndex;
        final int stake;
        final boolean formBonusApplied;

        PendingBet(int raceIndex, int horseIndex, int stake, boolean formBonusApplied) {
            this.raceIndex = raceIndex;
            this.horseIndex = horseIndex;
            this.stake = stake;
            this.formBonusApplied = formBonusApplied;
        }
    }

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Create a BettingShopSystem wrapping the given race systems.
     */
    public BettingShopSystem(HorseRacingSystem horseRacingSystem,
                              GreyhoundRacingSystem greyhoundRacingSystem) {
        this.horseRacingSystem = horseRacingSystem;
        this.greyhoundRacingSystem = greyhoundRacingSystem;
    }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the betting shop is currently open.
     * Mon–Sat: 09:00–22:00. Sun (day % 7 == 0): 10:00–18:00.
     *
     * @param timeSystem current game time
     */
    public boolean isOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        int day = timeSystem.getDayCount();
        boolean isSunday = (day % 7 == 0);
        if (isSunday) {
            return hour >= 10f && hour < 18f;
        } else {
            return hour >= 9f && hour < 22f;
        }
    }

    // ── Horse Racing — over-counter betting ───────────────────────────────────

    /**
     * Place a bet on a horse race via Derek's counter.
     *
     * @param race      the race to bet on (from {@link HorseRacingSystem#getTodaysRaces()})
     * @param horseIndex index of the horse to back (0–5)
     * @param stake     amount of COIN to stake (1–50, or 1–100 on BENEFIT_DAY)
     * @param inventory player inventory
     * @param isBenefitDay {@code true} if BENEFIT_DAY market event is active
     * @param timeSystem current time (used for form-guide day tracking)
     * @param streetSkillSystem for GAMBLING XP award (may be null)
     * @param achievementCallback for FIRST_FLUTTER (may be null)
     * @return {@link BetResult} indicating outcome
     */
    public BetResult placeBet(HorseRacingSystem.Race race,
                               int horseIndex,
                               int stake,
                               Inventory inventory,
                               boolean isBenefitDay,
                               TimeSystem timeSystem,
                               StreetSkillSystem streetSkillSystem,
                               NotorietySystem.AchievementCallback achievementCallback) {

        if (race == null || race.isResolved()) {
            return BetResult.RACE_CLOSED;
        }

        int maxStake = isBenefitDay ? HorseRacingSystem.MAX_STAKE_BENEFIT_DAY
                                    : HorseRacingSystem.MAX_STAKE_NORMAL;
        if (stake < HorseRacingSystem.MIN_STAKE || stake > maxStake) {
            return BetResult.RACE_CLOSED; // invalid stake treated as closed
        }

        int coinCount = inventory.getItemCount(Material.COIN);
        if (coinCount < stake) {
            return BetResult.INSUFFICIENT_FUNDS;
        }

        // Determine if form bonus applies (favourite odds ≤ 4/1)
        boolean applyFormBonus = false;
        if (formGuideActive && timeSystem != null
                && timeSystem.getDayCount() == formGuideDay) {
            HorseRacingSystem.Horse horse = race.getHorses().get(horseIndex);
            if (horse.getOddsNumerator() <= 4) {
                applyFormBonus = true;
                formGuideActive = false; // consume the bonus
            }
        }

        inventory.removeItem(Material.COIN, stake);
        inventory.addItem(Material.BET_SLIP, 1);

        pendingBets.add(new PendingBet(race.getRaceIndex(), horseIndex, stake, applyFormBonus));

        // GAMBLING XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.GAMBLING, GAMBLING_XP_HORSE_BET);
        }

        // FIRST_FLUTTER
        if (!firstBetPlaced) {
            firstBetPlaced = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FIRST_FLUTTER);
            }
        }

        return BetResult.ACCEPTED;
    }

    /**
     * Resolve outstanding bets against the current race results.
     * Call this each frame (or when race results are known) to settle winnings.
     *
     * @param timeSystem         current time
     * @param inventory          player inventory
     * @param rumourNetwork      for seeding BIG_WIN_AT_BOOKIES (may be null)
     * @param allNpcs            NPC list for rumour seeding (may be null)
     * @param achievementCallback for RACING_CERT, BETTING_SLIP_BLUES (may be null)
     */
    public void resolveBets(TimeSystem timeSystem,
                             Inventory inventory,
                             RumourNetwork rumourNetwork,
                             List<NPC> allNpcs,
                             NotorietySystem.AchievementCallback achievementCallback) {

        if (pendingBets.isEmpty()) return;

        List<HorseRacingSystem.Race> races = horseRacingSystem.getTodaysRaces();

        int losingSlipsBeforeResolve = 0;
        int resolvedCount = 0;
        int losingCount = 0;

        java.util.Iterator<PendingBet> it = pendingBets.iterator();
        while (it.hasNext()) {
            PendingBet bet = it.next();

            // Find the matching race
            HorseRacingSystem.Race race = null;
            for (HorseRacingSystem.Race r : races) {
                if (r.getRaceIndex() == bet.raceIndex) {
                    race = r;
                    break;
                }
            }

            if (race == null || !race.isResolved()) {
                continue; // not yet resolved
            }

            // Settle
            inventory.removeItem(Material.BET_SLIP, 1);
            it.remove();
            resolvedCount++;

            boolean won = (race.getWinnerIndex() == bet.horseIndex);
            if (won) {
                HorseRacingSystem.Horse horse = race.getHorses().get(bet.horseIndex);
                int payout = bet.stake * horse.getOddsNumerator();
                if (bet.formBonusApplied) {
                    // +5% bonus payout (rounded down)
                    payout = (int) (payout * 1.05f);
                }
                // Return stake + winnings
                inventory.addItem(Material.COIN, payout + bet.stake);

                // RACING_CERT on win ≥ BIG_WIN_THRESHOLD
                if (payout >= BIG_WIN_THRESHOLD && achievementCallback != null) {
                    achievementCallback.award(AchievementType.RACING_CERT);
                }

                // Seed BIG_WIN_AT_BOOKIES rumour
                if (payout >= BIG_WIN_THRESHOLD && rumourNetwork != null && allNpcs != null) {
                    Rumour rumour = new Rumour(RumourType.BIG_WIN_AT_BOOKIES,
                            "Someone just had a proper big win at Ladbrokes — lucky git!");
                    int seeded = 0;
                    for (NPC npc : allNpcs) {
                        if (npc.isAlive() && seeded < BIG_WIN_RUMOUR_SEED_COUNT) {
                            rumourNetwork.addRumour(npc, rumour);
                            seeded++;
                        }
                    }
                }
            } else {
                losingCount++;
            }
        }

        // BETTING_SLIP_BLUES: 3 losing slips in one resolve batch
        if (losingCount >= 3 && achievementCallback != null) {
            achievementCallback.award(AchievementType.BETTING_SLIP_BLUES);
        }
    }

    // ── Reading the Racing Post ───────────────────────────────────────────────

    /**
     * Player presses E on the {@code RACING_POST_PROP}.
     * Activates form-guide bonus for the rest of the in-game day.
     *
     * @param timeSystem current game time (for day tracking)
     */
    public void readRacingPost(TimeSystem timeSystem) {
        formGuideActive = true;
        if (timeSystem != null) {
            formGuideDay = timeSystem.getDayCount();
        }
    }

    /** Returns {@code true} if the form-guide bonus is currently active. */
    public boolean isFormGuideActive() {
        return formGuideActive;
    }

    // ── FOBT Terminal ─────────────────────────────────────────────────────────

    /**
     * Execute one FOBT roulette spin.
     *
     * <p>Odds:
     * <ul>
     *   <li>RED / BLACK: 18/38 chance → player wins stake (net profit = stake).</li>
     *   <li>NUMBER: 1/38 chance → player wins 35× stake (net profit = 35×stake).</li>
     *   <li>Otherwise: player loses the stake.</li>
     * </ul>
     *
     * @param betType   RED, BLACK, or NUMBER
     * @param stake     amount to bet (1–20 COIN)
     * @param inventory player inventory
     * @param random    RNG (use seeded for tests)
     * @param streetSkillSystem for GAMBLING XP (may be null)
     * @param achievementCallback for FOBT_RAGE (may be null)
     * @return WIN or LOSS
     */
    public FOBTResult playFOBT(FOBTBetType betType,
                                int stake,
                                Inventory inventory,
                                Random random,
                                StreetSkillSystem streetSkillSystem,
                                NotorietySystem.AchievementCallback achievementCallback) {

        if (stake < FOBT_MIN_STAKE || stake > FOBT_MAX_STAKE) {
            return FOBTResult.LOSS;
        }
        if (inventory.getItemCount(Material.COIN) < stake) {
            return FOBTResult.LOSS;
        }

        // Deduct stake
        inventory.removeItem(Material.COIN, stake);

        float roll = random.nextFloat();

        boolean win;
        int netGain;

        if (betType == FOBTBetType.NUMBER) {
            win = roll < FOBT_NUMBER_PROB;
            netGain = win ? (stake * 35) : 0; // payout is 36× stake; stake already removed
        } else {
            // RED or BLACK — same probability (18/38)
            win = roll < FOBT_RED_BLACK_PROB;
            netGain = win ? stake : 0; // 2× stake payout; stake already removed
        }

        if (win) {
            // Return stake + net gain
            inventory.addItem(Material.COIN, stake + netGain);
            // Reduce session loss tracking (partial recovery)
            // We only track losses, so a win just means we didn't lose this spin
        } else {
            fobtSessionLoss += stake;
        }

        // GAMBLING XP
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.GAMBLING, GAMBLING_XP_FOBT);
        }

        // FOBT_RAGE: session loss reached threshold
        if (!fobtRageAwarded && fobtSessionLoss >= FOBT_RAGE_THRESHOLD) {
            fobtRageAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FOBT_RAGE);
            }
        }

        return win ? FOBTResult.WIN : FOBTResult.LOSS;
    }

    /**
     * Returns the cumulative net FOBT loss for the current session.
     * Resets when {@link #resetFOBTSession()} is called (e.g. player leaves bookies).
     */
    public int getFOBTSessionLoss() {
        return fobtSessionLoss;
    }

    /**
     * Returns {@code true} if the LOAN_SHARK should spawn outside (FOBT loss ≥ 30).
     */
    public boolean shouldSpawnFOBTLoanShark() {
        return fobtSessionLoss >= FOBT_LOAN_SHARK_THRESHOLD && !fobtLoanSharkSpawned;
    }

    /**
     * Mark that the FOBT loan shark has been spawned (to avoid repeat spawns).
     */
    public void markFOBTLoanSharkSpawned() {
        fobtLoanSharkSpawned = true;
    }

    /**
     * Reset FOBT session state (call when player leaves the bookies area).
     */
    public void resetFOBTSession() {
        fobtSessionLoss = 0;
        fobtRageAwarded = false;
        fobtLoanSharkSpawned = false;
    }

    // ── Race Fix ──────────────────────────────────────────────────────────────

    /**
     * Set whether a Marchetti race-fix offer is currently pending.
     * Called by NPCManager when MARCHETTI_LIEUTENANT approaches player.
     */
    public void setRaceFixPending(boolean pending) {
        this.raceFixPending = pending;
    }

    /** Returns {@code true} if a race-fix offer from the Marchetti Lieutenant is pending. */
    public boolean isRaceFixPending() {
        return raceFixPending;
    }

    /**
     * Player accepts the Marchetti race-fix offer.
     *
     * <p>Sets {@link GreyhoundRacingSystem#setRaceFixed(boolean)} to {@code true} and
     * seeds a {@link RumourType#DODGY_DEAL} rumour. Optionally: 20% chance Derek
     * spots the deal → FRAUD crime + WantedSystem +1 star.
     *
     * @param random             RNG for fix-spotted roll
     * @param rumourNetwork      for DODGY_DEAL rumour (may be null)
     * @param allNpcs            NPC list for rumour seeding (may be null)
     * @param criminalRecord     for FRAUD crime (may be null)
     * @param wantedSystem       for +1 star if spotted (may be null)
     * @param notorietySystem    for Notoriety +5 on win (stored; applied on resolution)
     * @param achievementCallback for SURE_THING on win (may be null)
     * @param factionSystem      for MARCHETTI_CREW respect +10 on win (may be null)
     * @return {@code true} if the fix was set successfully; {@code false} if spotted by Derek
     */
    public boolean acceptRaceFix(Random random,
                                  RumourNetwork rumourNetwork,
                                  List<NPC> allNpcs,
                                  CriminalRecord criminalRecord,
                                  WantedSystem wantedSystem,
                                  NotorietySystem notorietySystem,
                                  NotorietySystem.AchievementCallback achievementCallback,
                                  FactionSystem factionSystem) {
        raceFixPending = false;

        // 20% chance Derek spots it
        if (random.nextFloat() < RACE_FIX_SPOTTED_PROB) {
            // Spotted: cancel fix
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.FRAUD);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0, 0, 0, null);
            }
            return false;
        }

        // Fix accepted — rig the greyhound race
        greyhoundRacingSystem.setRaceFixed(true);

        // Seed DODGY_DEAL rumour
        if (rumourNetwork != null && allNpcs != null) {
            Rumour rumour = new Rumour(RumourType.DODGY_DEAL,
                    "Word is someone's got a proper bent race going at the dogs tonight.");
            int seeded = 0;
            for (NPC npc : allNpcs) {
                if (npc.isAlive() && seeded < 2) {
                    rumourNetwork.addRumour(npc, rumour);
                    seeded++;
                }
            }
        }

        return true;
    }

    /**
     * Called after the player wins a fixed greyhound race.
     * Awards SURE_THING, Notoriety +5, MARCHETTI_CREW respect +10.
     *
     * @param notorietySystem    for Notoriety +5 (may be null)
     * @param achievementCallback for SURE_THING (may be null)
     * @param factionSystem      for MARCHETTI_CREW respect +10 (may be null)
     */
    public void onRaceFixWin(NotorietySystem notorietySystem,
                              NotorietySystem.AchievementCallback achievementCallback,
                              FactionSystem factionSystem) {
        if (!sureThingAwarded) {
            sureThingAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.SURE_THING);
            }
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_RACE_FIX_WIN, achievementCallback);
        }
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, 10);
        }
    }

    /**
     * Player grasses the race fix to police before accepting.
     * WantedSystem −1 star, MARCHETTI_CREW respect −20, GRASS achievement.
     *
     * @param wantedSystem       for star reduction (may be null)
     * @param factionSystem      for MARCHETTI_CREW respect −20 (may be null)
     * @param achievementCallback for GRASS achievement (may be null)
     */
    public void grassRaceFix(WantedSystem wantedSystem,
                              FactionSystem factionSystem,
                              NotorietySystem.AchievementCallback achievementCallback) {
        raceFixPending = false;
        if (wantedSystem != null) {
            // Reduce wanted stars by 1 (WantedSystem has bribeStar mechanism;
            // use addWantedStars with negative value if the field supports it,
            // otherwise track internally).
            int currentStars = wantedSystem.getWantedStars();
            if (currentStars > 0) {
                // Bribe cost path not appropriate; use internal star adjustment
                wantedSystem.addWantedStars(-1, 0, 0, 0, null);
            }
        }
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -20);
        }
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.GRASS);
        }
    }

    // ── After-hours break-in ──────────────────────────────────────────────────

    /**
     * Execute an after-hours break-in of the float tray.
     * Requires SCREWDRIVER in inventory and shop to be closed.
     *
     * <p>Drops 5–15 COIN + 1 BETTING_SLIP_BLANK. Records BURGLARY crime,
     * adds Notoriety +8, awards FOLDED achievement, triggers
     * {@link NeighbourhoodWatchSystem#onVisibleCrime()}.
     *
     * @param inventory               player inventory
     * @param random                  RNG for coin amount (5–15)
     * @param timeSystem              current time (shop must be closed)
     * @param criminalRecord          for BURGLARY record (may be null)
     * @param notorietySystem         for Notoriety +8 (may be null)
     * @param neighbourhoodWatchSystem for CCTV trigger (may be null)
     * @param achievementCallback     for FOLDED achievement (may be null)
     * @return {@code true} if break-in succeeded; {@code false} if shop is open
     *         or player lacks SCREWDRIVER
     */
    public boolean breakIn(Inventory inventory,
                            Random random,
                            TimeSystem timeSystem,
                            CriminalRecord criminalRecord,
                            NotorietySystem notorietySystem,
                            NeighbourhoodWatchSystem neighbourhoodWatchSystem,
                            NotorietySystem.AchievementCallback achievementCallback) {

        // Shop must be closed
        if (isOpen(timeSystem)) {
            return false;
        }
        // Player must have SCREWDRIVER
        if (inventory.getItemCount(Material.SCREWDRIVER) < 1) {
            return false;
        }

        // Drop float tray contents
        int coins = FLOAT_TRAY_MIN + random.nextInt(FLOAT_TRAY_MAX - FLOAT_TRAY_MIN + 1);
        inventory.addItem(Material.COIN, coins);
        inventory.addItem(Material.BETTING_SLIP_BLANK, 1);

        // Crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.BURGLARY);
        }

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_BREAK_IN, achievementCallback);
        }

        // CCTV trigger
        if (neighbourhoodWatchSystem != null) {
            neighbourhoodWatchSystem.onVisibleCrime();
        }

        // FOLDED achievement
        if (!foldedAwarded && achievementCallback != null) {
            foldedAwarded = true;
            achievementCallback.award(AchievementType.FOLDED);
        }

        return true;
    }

    // ── FOBT smash ────────────────────────────────────────────────────────────

    /**
     * Called when a FOBT terminal is smashed (8 hits).
     * Drops 0–8 COIN + SCRAP_METAL, adds Notoriety +5, triggers
     * {@link NeighbourhoodWatchSystem} ANGER_SMASH_EXTERIOR.
     *
     * @param inventory               for coin/scrap drops
     * @param random                  RNG for coin amount (0–8)
     * @param notorietySystem         for Notoriety +5 (may be null)
     * @param neighbourhoodWatchSystem for anger trigger (may be null)
     * @param achievementCallback     for notoriety level-up checks (may be null)
     */
    public void onFOBTSmash(Inventory inventory,
                             Random random,
                             NotorietySystem notorietySystem,
                             NeighbourhoodWatchSystem neighbourhoodWatchSystem,
                             NotorietySystem.AchievementCallback achievementCallback) {
        int coins = random.nextInt(9); // 0–8
        if (coins > 0) {
            inventory.addItem(Material.COIN, coins);
        }
        inventory.addItem(Material.SCRAP_METAL, 1);

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_FOBT_SMASH, achievementCallback);
        }
        if (neighbourhoodWatchSystem != null) {
            neighbourhoodWatchSystem.onSmashExterior();
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns {@code true} if the first bet has been placed. */
    public boolean isFirstBetPlaced() {
        return firstBetPlaced;
    }

    /** Returns the number of pending (unresolved) bets held by this system. */
    public int getPendingBetCount() {
        return pendingBets.size();
    }

    /** Returns the underlying HorseRacingSystem. */
    public HorseRacingSystem getHorseRacingSystem() {
        return horseRacingSystem;
    }

    /** Returns the underlying GreyhoundRacingSystem. */
    public GreyhoundRacingSystem getGreyhoundRacingSystem() {
        return greyhoundRacingSystem;
    }
}
