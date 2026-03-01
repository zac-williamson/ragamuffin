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
 * Issue #987: Pigeon Racing System — Loft, Training & the Northfield Derby
 *
 * <p>Fully playable pigeon racing mini-game. The player acquires a {@link Material#RACING_PIGEON},
 * trains it by feeding {@link Material#BREAD_CRUST} items, and enters scheduled races
 * (Neighbourhood Sprint, Club Race, Northfield Derby).
 *
 * <h3>Pigeon Acquisition</h3>
 * <ul>
 *   <li>Buy from {@link NPCType#PIGEON_FANCIER} for 8 COIN (once per day).</li>
 *   <li>Catch a wild BIRD NPC within 1 block — 40% success chance.</li>
 *   <li>Community reward: win a Neighbourhood Sprint with Training ≤ 3.</li>
 * </ul>
 *
 * <h3>Training</h3>
 * Feed {@link Material#BREAD_CRUST} to increment Training Level (0–10).
 * Morale ({@link Morale}) affects race speed ±30%.
 *
 * <h3>Race Schedule</h3>
 * Races run every 3 in-game days. Three types:
 * <ul>
 *   <li>Neighbourhood Sprint — free, prize 5 COIN</li>
 *   <li>Club Race — 2 COIN entry, prize 12 COIN</li>
 *   <li>Northfield Derby — 5 COIN entry, prize 30 COIN + PIGEON_TROPHY (requires prior Club win)</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link NewspaperSystem#setPigeonVictoryPending(boolean)} on Derby win.</li>
 *   <li>{@link RumourNetwork} — PIGEON_RACE_DAY seeded the evening before a race;</li>
 *   <li>PIGEON_VICTORY seeded on Derby win.</li>
 *   <li>{@link WeatherSystem} — weather modifies race speed and loft condition.</li>
 *   <li>{@link NotorietySystem.AchievementCallback} for achievements.</li>
 * </ul>
 */
public class PigeonRacingSystem {

    // ── Race schedule constants ───────────────────────────────────────────────

    /** Races occur every N in-game days. */
    public static final int RACE_INTERVAL_DAYS = 3;

    /** Neighbourhood Sprint race starts on day offset 0 of a race-day cycle at 14:00. */
    public static final float RACE_START_HOUR = 14.0f;

    /** Races end at 16:00. */
    public static final float RACE_END_HOUR = 16.0f;

    /** Hour the evening before a race when PIGEON_RACE_DAY rumour is seeded. */
    public static final float RACE_RUMOUR_SEED_HOUR = 18.0f;

    /** How many NPCs receive the PIGEON_RACE_DAY rumour. */
    public static final int RACE_RUMOUR_NPC_COUNT = 3;

    // ── Acquisition constants ─────────────────────────────────────────────────

    /** Cost to buy a pigeon from the PIGEON_FANCIER. */
    public static final int PIGEON_BUY_COST = 8;

    /** Probability of catching a wild BIRD NPC (0.0–1.0). */
    public static final float WILD_CATCH_CHANCE = 0.40f;

    // ── Training constants ────────────────────────────────────────────────────

    /** Maximum training level. */
    public static final int MAX_TRAINING_LEVEL = 10;

    /** At this training level, pigeon has 15% chance of inspired form (+20% speed). */
    public static final int INSPIRED_FORM_THRESHOLD = 8;

    /** Probability of inspired form at high training. */
    public static final float INSPIRED_FORM_CHANCE = 0.15f;

    /** Inspired form speed bonus. */
    public static final float INSPIRED_FORM_BONUS = 0.20f;

    // ── Loft constants ────────────────────────────────────────────────────────

    /** Frost reduces loft condition by this amount per frost period. */
    public static final int FROST_LOFT_DAMAGE = 10;

    /** Loft condition below which pigeon morale drops. */
    public static final int LOFT_CONDITION_MORALE_THRESHOLD = 30;

    // ── Race type definitions ─────────────────────────────────────────────────

    /** Defines a type of pigeon race. */
    public enum RaceType {
        NEIGHBOURHOOD_SPRINT("Neighbourhood Sprint", 0, 5, 3, 0.3f, 0.6f),
        CLUB_RACE("Club Race", 2, 12, 5, 0.5f, 0.8f),
        NORTHFIELD_DERBY("Northfield Derby", 5, 30, 7, 0.65f, 0.95f);

        private final String displayName;
        private final int entryFee;
        private final int prizeMoney;
        private final int opponentCount;
        private final float opponentMinSpeed;
        private final float opponentMaxSpeed;

        RaceType(String displayName, int entryFee, int prizeMoney, int opponentCount,
                 float opponentMinSpeed, float opponentMaxSpeed) {
            this.displayName = displayName;
            this.entryFee = entryFee;
            this.prizeMoney = prizeMoney;
            this.opponentCount = opponentCount;
            this.opponentMinSpeed = opponentMinSpeed;
            this.opponentMaxSpeed = opponentMaxSpeed;
        }

        public String getDisplayName() { return displayName; }
        public int getEntryFee() { return entryFee; }
        public int getPrizeMoney() { return prizeMoney; }
        public int getOpponentCount() { return opponentCount; }
        public float getOpponentMinSpeed() { return opponentMinSpeed; }
        public float getOpponentMaxSpeed() { return opponentMaxSpeed; }
    }

    // ── Morale enum ───────────────────────────────────────────────────────────

    /** Morale state of the pigeon. Affects race speed. */
    public enum Morale {
        MISERABLE(-0.30f),
        NERVOUS(-0.15f),
        STEADY(0.00f),
        CONFIDENT(0.10f),
        ELATED(0.20f);

        private final float speedModifier;

        Morale(float speedModifier) {
            this.speedModifier = speedModifier;
        }

        /** Speed modifier as a multiplier (e.g. -0.30 means 70% of base speed). */
        public float getSpeedModifier() { return speedModifier; }

        /** Return the next higher morale tier, or ELATED if already at top. */
        public Morale increase() {
            int ord = ordinal();
            Morale[] vals = values();
            return ord < vals.length - 1 ? vals[ord + 1] : ELATED;
        }

        /** Return the next lower morale tier, or MISERABLE if already at bottom. */
        public Morale decrease() {
            int ord = ordinal();
            Morale[] vals = values();
            return ord > 0 ? vals[ord - 1] : MISERABLE;
        }
    }

    // ── Pigeon data ───────────────────────────────────────────────────────────

    /** Holds the state of the player's racing pigeon. */
    public static class Pigeon {
        private final String name;
        private int trainingLevel;
        private Morale morale;
        private int wins;
        private int races;

        public Pigeon(String name, int trainingLevel, Morale morale) {
            this.name = name;
            this.trainingLevel = trainingLevel;
            this.morale = morale;
            this.wins = 0;
            this.races = 0;
        }

        public String getName() { return name; }
        public int getTrainingLevel() { return trainingLevel; }
        public Morale getMorale() { return morale; }
        public int getWins() { return wins; }
        public int getRaces() { return races; }

        public void setTrainingLevel(int level) { this.trainingLevel = Math.min(level, MAX_TRAINING_LEVEL); }
        public void setMorale(Morale morale) { this.morale = morale; }
        void recordRace(boolean won) {
            races++;
            if (won) wins++;
        }
    }

    // ── Race result ───────────────────────────────────────────────────────────

    /** Result of resolving a race. */
    public static class RaceResult {
        private final int placement;       // 1 = first, higher = worse
        private final int totalEntrants;
        private final String flavourLine;
        private final int prizeMoney;
        private final boolean postponed;

        public RaceResult(int placement, int totalEntrants, String flavourLine, int prizeMoney) {
            this.placement = placement;
            this.totalEntrants = totalEntrants;
            this.flavourLine = flavourLine;
            this.prizeMoney = prizeMoney;
            this.postponed = false;
        }

        /** Create a postponed result. */
        public static RaceResult postponed() {
            return new RaceResult(0, 0, "Race postponed due to weather.", 0, true);
        }

        private RaceResult(int placement, int totalEntrants, String flavourLine, int prizeMoney, boolean postponed) {
            this.placement = placement;
            this.totalEntrants = totalEntrants;
            this.flavourLine = flavourLine;
            this.prizeMoney = prizeMoney;
            this.postponed = postponed;
        }

        public int getPlacement() { return placement; }
        public int getTotalEntrants() { return totalEntrants; }
        public String getFlavourLine() { return flavourLine; }
        public int getPrizeMoney() { return prizeMoney; }
        public boolean isPostponed() { return postponed; }
        public boolean isWin() { return placement == 1; }
        public boolean isLast() { return placement == totalEntrants; }
    }

    // ── Entry result ──────────────────────────────────────────────────────────

    /** Result codes for race entry attempts. */
    public enum EntryResult {
        SUCCESS,
        NO_PIGEON,
        NO_LOFT,
        INSUFFICIENT_FUNDS,
        DERBY_REQUIRES_CLUB_WIN,
        RACE_NOT_SCHEDULED
    }

    // ── Pigeon name pool ──────────────────────────────────────────────────────

    private static final String[] PIGEON_NAMES = {
        "Northfield Nora",    "Council Estate Carl", "Skip Diver Sally",
        "Benefit Day Bill",   "Greggs Gloria",       "Flat Cap Frankie",
        "Wet Weekend Wendy",  "Allotment Al",        "Pavement Pat",
        "Corner Shop Clara",  "Bus Stop Betty",      "Rotary Rita",
        "Estate Edna",        "Market Day Mick",     "Pound Shop Pearl"
    };

    // ── Flavour lines ─────────────────────────────────────────────────────────

    public static final String FLAVOUR_FIRST  = "She flew like she had somewhere to be. First place!";
    public static final String FLAVOUR_SECOND = "Pipped at the post. Damn close.";
    public static final String FLAVOUR_THIRD  = "Bronze. The pigeon fancier gives you a consolatory nod.";
    public static final String FLAVOUR_LAST   = "Dead last. She took the scenic route.";
    public static final String FLAVOUR_MID    = "Mid-field finish. Not bad, not great.";

    // ── System state ──────────────────────────────────────────────────────────

    private final Random random;

    /** The player's pigeon, or null if they don't have one. */
    private Pigeon playerPigeon;

    /** Loft condition (0–100). Damaged by frost and punches. */
    private int loftCondition;

    /** Whether the player has a placed PIGEON_LOFT. */
    private boolean loftPlaced;

    /** Day on which the PIGEON_FANCIER was last consulted for purchase. */
    private int lastBuyDay = -1;

    /** Whether the player has won at least one Club Race (required for Derby entry). */
    private boolean hasWonClubRace;

    /** Count of BREAD_CRUST items fed total (for BREAD_WINNER achievement). */
    private int totalCrustsUsed;

    /** Count of neighbourhood/club wins with the current pigeon. */
    private int currentPigeonSmallRaceWins;

    /** Last race result, shown in UI. */
    private RaceResult lastResult;

    /** The currently-scheduled race type, or null if none pending. */
    private RaceType scheduledRaceType;

    /** Day of the next scheduled race. */
    private int nextRaceDay;

    /** Whether the race-day rumour has been seeded for the current upcoming race. */
    private boolean raceDayRumourSeeded;

    /** Whether morale has already been dropped this frost period. */
    private boolean frostMoraleDroppedThisPeriod;

    // ── Construction ──────────────────────────────────────────────────────────

    public PigeonRacingSystem() {
        this(new Random());
    }

    public PigeonRacingSystem(Random random) {
        this.random = random;
        this.loftCondition = 100;
        this.loftPlaced = false;
        this.hasWonClubRace = false;
        this.totalCrustsUsed = 0;
        this.currentPigeonSmallRaceWins = 0;
        this.scheduledRaceType = RaceType.NEIGHBOURHOOD_SPRINT;
        this.nextRaceDay = RACE_INTERVAL_DAYS; // First race on day 3
        this.raceDayRumourSeeded = false;
        this.frostMoraleDroppedThisPeriod = false;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the pigeon racing system. Call once per frame.
     *
     * @param delta           seconds since last frame
     * @param timeSystem      game time
     * @param weatherSystem   current weather (may be null)
     * @param playerInventory player's inventory
     * @param allNpcs         all living NPCs (for rumour seeding)
     * @param rumourNetwork   may be null
     * @param newspaperSystem may be null
     * @param achievementCallback may be null
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       WeatherSystem weatherSystem,
                       Inventory playerInventory,
                       List<NPC> allNpcs,
                       RumourNetwork rumourNetwork,
                       NewspaperSystem newspaperSystem,
                       NotorietySystem.AchievementCallback achievementCallback) {

        if (timeSystem == null) return;

        int day = timeSystem.getDayCount();
        float hour = timeSystem.getTime();

        // Apply frost effects once per frost weather change
        if (weatherSystem != null && weatherSystem.getCurrentWeather() == Weather.FROST) {
            applyFrostToLoft(achievementCallback);
        } else {
            frostMoraleDroppedThisPeriod = false;
        }

        // Seed race-day rumour the evening before a race
        if (!raceDayRumourSeeded && nextRaceDay > 0 && day == nextRaceDay - 1
                && hour >= RACE_RUMOUR_SEED_HOUR) {
            seedRaceDayRumour(allNpcs, rumourNetwork);
            raceDayRumourSeeded = true;
        }
    }

    // ── Frost effect ──────────────────────────────────────────────────────────

    /**
     * Apply frost damage to the loft. Called once per frost period.
     * Reduces loft condition by {@link #FROST_LOFT_DAMAGE}.
     * If condition drops below {@link #LOFT_CONDITION_MORALE_THRESHOLD}, pigeon morale drops.
     */
    public void applyFrostToLoft(NotorietySystem.AchievementCallback achievementCallback) {
        if (!loftPlaced) return;
        if (frostMoraleDroppedThisPeriod) return; // Only apply once per frost period

        loftCondition = Math.max(0, loftCondition - FROST_LOFT_DAMAGE);
        frostMoraleDroppedThisPeriod = true;

        if (loftCondition < LOFT_CONDITION_MORALE_THRESHOLD && playerPigeon != null) {
            playerPigeon.setMorale(playerPigeon.getMorale().decrease());
        }
    }

    /**
     * Reset the frost morale flag (called when weather changes away from FROST).
     */
    public void resetFrostFlag() {
        frostMoraleDroppedThisPeriod = false;
    }

    // ── Pigeon acquisition ────────────────────────────────────────────────────

    /**
     * Buy a pigeon from the PIGEON_FANCIER NPC.
     *
     * @param inventory    player inventory
     * @param currentDay   current in-game day
     * @param fancierNpc   the PIGEON_FANCIER NPC being interacted with
     * @param achievementCallback may be null
     * @return true if purchase succeeded
     */
    public boolean buyPigeonFromFancier(Inventory inventory, int currentDay,
                                        NPC fancierNpc,
                                        NotorietySystem.AchievementCallback achievementCallback) {
        if (fancierNpc == null || fancierNpc.getType() != NPCType.PIGEON_FANCIER) return false;
        if (currentDay == lastBuyDay) return false; // Once per day
        if (inventory.getItemCount(Material.COIN) < PIGEON_BUY_COST) return false;

        inventory.removeItem(Material.COIN, PIGEON_BUY_COST);
        lastBuyDay = currentDay;

        setPigeon(new Pigeon(generatePigeonName(), 0, Morale.STEADY));
        inventory.addItem(Material.RACING_PIGEON, 1);
        return true;
    }

    /**
     * Attempt to catch a wild BIRD NPC.
     *
     * @param birdNpc             the BIRD NPC
     * @param achievementCallback may be null
     * @return true if catch succeeded
     */
    public boolean catchWildBird(NPC birdNpc,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (birdNpc == null) return false;
        if (random.nextFloat() >= WILD_CATCH_CHANCE) return false;

        setPigeon(new Pigeon(generatePigeonName(), 0, Morale.NERVOUS));

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.CAUGHT_IT_YERSELF);
        }
        return true;
    }

    /**
     * Offer the community-reward pigeon (given by PIGEON_FANCIER after Neighbourhood win
     * with Training ≤ 3). The caller is responsible for checking eligibility.
     */
    public void acceptCommunityRewardPigeon() {
        setPigeon(new Pigeon(generatePigeonName(), 2, Morale.STEADY));
    }

    /**
     * Replace the current pigeon (old pigeon flies away).
     */
    private void setPigeon(Pigeon newPigeon) {
        if (playerPigeon != null && playerPigeon.getWins() > 0) {
            // Champion flew away — may seed a PIGEON_VICTORY rumour at the call site
        }
        playerPigeon = newPigeon;
        currentPigeonSmallRaceWins = 0;
    }

    // ── Training ──────────────────────────────────────────────────────────────

    /**
     * Feed one BREAD_CRUST to the pigeon. Increments training level by 1 (capped at 10).
     *
     * @param inventory           player inventory
     * @param achievementCallback may be null
     * @return true if feeding succeeded
     */
    public boolean feedBreadCrust(Inventory inventory,
                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (playerPigeon == null) return false;
        if (!loftPlaced) return false;
        if (inventory.getItemCount(Material.BREAD_CRUST) < 1) return false;
        if (playerPigeon.getTrainingLevel() >= MAX_TRAINING_LEVEL) return false;

        inventory.removeItem(Material.BREAD_CRUST, 1);
        playerPigeon.setTrainingLevel(playerPigeon.getTrainingLevel() + 1);
        totalCrustsUsed++;

        if (achievementCallback != null && totalCrustsUsed >= 10) {
            achievementCallback.award(AchievementType.BREAD_WINNER);
        }
        return true;
    }

    // ── Race entry ────────────────────────────────────────────────────────────

    /**
     * Attempt to enter the next scheduled race.
     *
     * @param raceType            the race type to enter
     * @param inventory           player inventory
     * @param achievementCallback may be null
     * @return entry result
     */
    public EntryResult enterRace(RaceType raceType, Inventory inventory,
                                  NotorietySystem.AchievementCallback achievementCallback) {
        if (playerPigeon == null) return EntryResult.NO_PIGEON;
        if (!loftPlaced) return EntryResult.NO_LOFT;
        if (raceType == RaceType.NORTHFIELD_DERBY && !hasWonClubRace) {
            return EntryResult.DERBY_REQUIRES_CLUB_WIN;
        }
        if (inventory.getItemCount(Material.COIN) < raceType.getEntryFee()) {
            return EntryResult.INSUFFICIENT_FUNDS;
        }

        if (raceType.getEntryFee() > 0) {
            inventory.removeItem(Material.COIN, raceType.getEntryFee());
        }
        scheduledRaceType = raceType;
        return EntryResult.SUCCESS;
    }

    // ── Race resolution ───────────────────────────────────────────────────────

    /**
     * Resolve the scheduled race off-screen.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Check weather — THUNDERSTORM postpones; RAIN/DRIZZLE 50% chance of 1-day delay.</li>
     *   <li>Calculate player pigeon's {@code raceSpeed}.</li>
     *   <li>Generate opponent speeds from the race type's distribution.</li>
     *   <li>Rank all entrants; determine placement.</li>
     * </ol>
     *
     * @param raceType            race to resolve
     * @param weather             current weather (may be null)
     * @param inventory           player inventory (receives prize if won)
     * @param allNpcs             for seeding rumours
     * @param rumourNetwork       may be null
     * @param newspaperSystem     may be null
     * @param achievementCallback may be null
     * @return the race result
     */
    public RaceResult resolveRace(RaceType raceType,
                                   Weather weather,
                                   Inventory inventory,
                                   List<NPC> allNpcs,
                                   RumourNetwork rumourNetwork,
                                   NewspaperSystem newspaperSystem,
                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (playerPigeon == null) {
            return RaceResult.postponed();
        }

        // Weather postponement
        if (weather == Weather.THUNDERSTORM) {
            // Postpone: re-seed rumour for tomorrow
            raceDayRumourSeeded = false;
            nextRaceDay++;
            seedRaceDayRumour(allNpcs, rumourNetwork);
            lastResult = RaceResult.postponed();
            return lastResult;
        }

        // Calculate weather speed modifier
        float weatherSpeedModifier = 1.0f;
        boolean fogDelay = false;
        if (weather == Weather.RAIN || weather == Weather.DRIZZLE) {
            weatherSpeedModifier = 0.90f;
        } else if (weather == Weather.FOG) {
            weatherSpeedModifier = 0.95f;
            fogDelay = true;
        } else if (weather == Weather.HEATWAVE) {
            weatherSpeedModifier = 1.10f;
        } else if (weather == Weather.FROST) {
            weatherSpeedModifier = 0.80f;
        }

        // Calculate player pigeon's race speed
        // baseSpeed 0.75 ensures Training 10 + ELATED (×1.20) = 0.90 base,
        // giving a competitive chance against Derby opponents (0.65–0.95).
        float baseSpeed = 0.75f;
        float moraleFactor = 1.0f + playerPigeon.getMorale().getSpeedModifier();
        float trainingFactor = playerPigeon.getTrainingLevel() / (float) MAX_TRAINING_LEVEL;
        float playerSpeed = trainingFactor * moraleFactor * baseSpeed * weatherSpeedModifier;

        // Inspired form bonus
        if (playerPigeon.getTrainingLevel() >= INSPIRED_FORM_THRESHOLD
                && random.nextFloat() < INSPIRED_FORM_CHANCE) {
            playerSpeed *= (1.0f + INSPIRED_FORM_BONUS);
        }

        // Add random variance
        playerSpeed += random.nextFloat() * 0.2f;

        // Generate opponent speeds
        int opponentCount = raceType.getOpponentCount();
        float[] opponentSpeeds = new float[opponentCount];
        for (int i = 0; i < opponentCount; i++) {
            float opponentBase = raceType.getOpponentMinSpeed()
                    + random.nextFloat() * (raceType.getOpponentMaxSpeed() - raceType.getOpponentMinSpeed());
            opponentSpeeds[i] = opponentBase * weatherSpeedModifier;
        }

        // Determine placement (how many opponents are faster)
        int placement = 1;
        for (float oppSpeed : opponentSpeeds) {
            if (oppSpeed > playerSpeed) {
                placement++;
            }
        }

        int totalEntrants = opponentCount + 1;
        String flavour = getFlavourLine(placement, totalEntrants);
        int prize = placement == 1 ? raceType.getPrizeMoney() : 0;

        // Award prize
        if (prize > 0 && inventory != null) {
            inventory.addItem(Material.COIN, prize);
        }

        // Derby trophy
        if (placement == 1 && raceType == RaceType.NORTHFIELD_DERBY && inventory != null) {
            inventory.addItem(Material.PIGEON_TROPHY, 1);
        }

        RaceResult result = new RaceResult(placement, totalEntrants, flavour, prize);
        lastResult = result;

        // Record race on pigeon
        playerPigeon.recordRace(placement == 1);

        // Update win tracking
        if (placement == 1) {
            if (raceType == RaceType.CLUB_RACE) {
                hasWonClubRace = true;
            }
            if (raceType == RaceType.NEIGHBOURHOOD_SPRINT || raceType == RaceType.CLUB_RACE) {
                currentPigeonSmallRaceWins++;
            }
        }

        // Achievements
        if (achievementCallback != null) {
            // HOME_BIRD: first race completed
            achievementCallback.award(AchievementType.HOME_BIRD);

            if (placement == 1 && raceType == RaceType.NORTHFIELD_DERBY) {
                achievementCallback.award(AchievementType.NORTHFIELD_DERBY);
            }

            if (currentPigeonSmallRaceWins >= 3) {
                achievementCallback.award(AchievementType.CHAMPION_OF_THE_LOFT);
            }
        }

        // Northfield Derby win: newspaper + rumour
        if (placement == 1 && raceType == RaceType.NORTHFIELD_DERBY) {
            if (newspaperSystem != null) {
                newspaperSystem.setPigeonVictoryPending(true);
            }
            seedPigeonVictoryRumour(allNpcs, rumourNetwork);
        }

        // Advance schedule
        nextRaceDay += RACE_INTERVAL_DAYS;
        raceDayRumourSeeded = false;

        return result;
    }

    // ── Rumour seeding ────────────────────────────────────────────────────────

    private void seedRaceDayRumour(List<NPC> allNpcs, RumourNetwork rumourNetwork) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.PIGEON_RACE_DAY,
                "Race day tomorrow — lofts are out in Northfield. Should be a good one.");
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (seeded >= RACE_RUMOUR_NPC_COUNT) break;
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                seeded++;
            }
        }
    }

    private void seedPigeonVictoryRumour(List<NPC> allNpcs, RumourNetwork rumourNetwork) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.PIGEON_VICTORY,
                "Heard someone's bird won the Northfield Derby yesterday — brought home the trophy an' all.");
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (seeded >= RACE_RUMOUR_NPC_COUNT) break;
            if (npc.isAlive()) {
                rumourNetwork.addRumour(npc, rumour);
                seeded++;
            }
        }
    }

    // ── Flavour text ──────────────────────────────────────────────────────────

    private String getFlavourLine(int placement, int totalEntrants) {
        if (placement == 1) return FLAVOUR_FIRST;
        if (placement == 2) return FLAVOUR_SECOND;
        if (placement == 3) return FLAVOUR_THIRD;
        if (placement == totalEntrants) return FLAVOUR_LAST;
        return FLAVOUR_MID;
    }

    // ── Pigeon name generation ────────────────────────────────────────────────

    private String generatePigeonName() {
        return PIGEON_NAMES[random.nextInt(PIGEON_NAMES.length)];
    }

    // ── Loft management ───────────────────────────────────────────────────────

    /**
     * Place the pigeon loft (requires 8 WOOD + 2 PLANKS in inventory).
     *
     * @return true if placed successfully
     */
    public boolean placeLoft(Inventory inventory) {
        if (loftPlaced) return false;
        if (inventory.getItemCount(Material.WOOD) < 8) return false;
        if (inventory.getItemCount(Material.PLANKS) < 2) return false;
        inventory.removeItem(Material.WOOD, 8);
        inventory.removeItem(Material.PLANKS, 2);
        loftPlaced = true;
        loftCondition = 100;
        return true;
    }

    /**
     * Mark the loft as placed without consuming resources (for tests/world-gen).
     */
    public void setLoftPlaced(boolean placed) {
        this.loftPlaced = placed;
    }

    /**
     * Deal damage to the loft (e.g. from punching).
     * Loft is destroyed at condition ≤ 0; yields WOOD × 8 via caller.
     *
     * @param amount damage amount
     * @return true if loft was destroyed
     */
    public boolean damageLoft(int amount) {
        if (!loftPlaced) return false;
        loftCondition = Math.max(0, loftCondition - amount);
        if (playerPigeon != null && loftCondition < LOFT_CONDITION_MORALE_THRESHOLD) {
            playerPigeon.setMorale(playerPigeon.getMorale().decrease());
        }
        if (loftCondition <= 0) {
            loftPlaced = false;
            return true;
        }
        return false;
    }

    /**
     * Release the player's pigeon.
     */
    public void releasePigeon() {
        playerPigeon = null;
        currentPigeonSmallRaceWins = 0;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Pigeon getPlayerPigeon() { return playerPigeon; }
    public boolean hasLoft() { return loftPlaced; }
    public int getLoftCondition() { return loftCondition; }
    public void setLoftCondition(int condition) { this.loftCondition = Math.max(0, Math.min(100, condition)); }
    public boolean hasWonClubRace() { return hasWonClubRace; }
    public void setHasWonClubRace(boolean won) { this.hasWonClubRace = won; }
    public RaceResult getLastResult() { return lastResult; }
    public int getTotalCrustsUsed() { return totalCrustsUsed; }
    public int getCurrentPigeonSmallRaceWins() { return currentPigeonSmallRaceWins; }
    public RaceType getScheduledRaceType() { return scheduledRaceType; }
    public int getNextRaceDay() { return nextRaceDay; }
    public void setNextRaceDay(int day) { this.nextRaceDay = day; }
    public boolean isRaceDayRumourSeeded() { return raceDayRumourSeeded; }
    public void setRaceDayRumourSeeded(boolean seeded) { this.raceDayRumourSeeded = seeded; }

    /** Set the player pigeon directly (for testing). */
    public void setPlayerPigeon(Pigeon pigeon) {
        this.playerPigeon = pigeon;
        this.currentPigeonSmallRaceWins = 0;
    }
}
