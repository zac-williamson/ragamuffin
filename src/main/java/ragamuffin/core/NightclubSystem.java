package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1325 — Northfield Nightclub: The Vaults.
 *
 * <p>Manages all mechanics for The Vaults nightclub:
 * <ul>
 *   <li><b>Mechanic 1 — Entry &amp; Big Dave</b>: Open Thu–Sun 22:00–03:00. Entry costs 3 COIN.
 *       Refused if WantedSystem stars ≥ 2 (bypass with DisguiseSystem). Notoriety ≥ 50 seeds
 *       {@link RumourType#HARD_NUT_IN_TOWN}. Achievement {@link AchievementType#FIRST_TIMER}
 *       on first entry.</li>
 *   <li><b>Mechanic 2 — Bar &amp; Dancefloor</b>: Bar sells {@link Material#CHEAP_LAGER} (2 COIN,
 *       +5 HP, TIPSY) and {@link Material#DOUBLE_VODKA} (3 COIN, +3 HP, DRUNK; stacks ×3).
 *       {@link DrunkLevel} enum wears off at 20 min/drink.</li>
 *   <li><b>Mechanic 3 — Dancefloor Brawl</b>: Random brawl every 30–60 in-game minutes.
 *       Win = BOUNCER_RESPECT +1; start one = NIGHTCLUB_AFFRAY + WantedSystem +1 + ejected.</li>
 *   <li><b>Mechanic 4 — Manager's Safe Heist</b>: Pickpocket Terry for
 *       {@link Material#NIGHTCLUB_MASTER_KEY}, or 3× LOCKPICK at 30% each. Safe requires
 *       {@link Material#STETHOSCOPE} held 6 seconds → 50–100 COIN + {@link Material#PILLS}.</li>
 *   <li><b>Mechanic 5 — VIP Booth &amp; Marchetti</b>: Marchetti Respect ≥ 50 for PRIVATE_BOOTH_PROP.
 *       Overhearing Marchetti seeds ORGANISED_CRIME + NotorietySystem +5.</li>
 *   <li><b>Mechanic 6 — Closing Time</b>: At 03:00 triggers TaxiSystem surge + KebabVanSystem
 *       nightclub spawn.</li>
 * </ul>
 */
public class NightclubSystem {

    // ── Opening hours ──────────────────────────────────────────────────────────

    /** First open day (Thursday = 4, where Monday = 1). */
    static final int OPEN_DAY_MIN = 4;
    /** Last open day (Sunday = 7). */
    static final int OPEN_DAY_MAX = 7;
    /** Opening hour (22:00). */
    static final float OPEN_HOUR = 22.0f;
    /** Closing hour (03:00 next day). */
    static final float CLOSE_HOUR = 3.0f;
    /** Closing-time surge trigger hour (03:00). */
    static final float SURGE_HOUR = 3.0f;

    // ── Entry ──────────────────────────────────────────────────────────────────

    /** Entry fee in COIN. */
    static final int ENTRY_FEE = 3;
    /** Notoriety threshold above which HARD_NUT_IN_TOWN rumour is seeded on entry. */
    static final int HARD_NUT_NOTORIETY_THRESHOLD = 50;
    /** WantedSystem stars at or above which entry is denied without a disguise. */
    static final int MAX_STARS_FOR_ENTRY = 2;

    // ── Drinks ────────────────────────────────────────────────────────────────

    /** Price of CHEAP_LAGER in COIN. */
    static final int LAGER_PRICE = 2;
    /** Price of DOUBLE_VODKA in COIN. */
    static final int VODKA_PRICE = 3;
    /** HP gained from CHEAP_LAGER. */
    static final int LAGER_HP = 5;
    /** HP gained from DOUBLE_VODKA. */
    static final int VODKA_HP = 3;
    /** Max DOUBLE_VODKA purchases per session before SMASHED. */
    static final int VODKA_SMASHED_COUNT = 3;
    /** In-game minutes for one DrunkLevel step to wear off. */
    static final float DRUNK_DECAY_MINUTES = 20.0f;

    // ── Brawl ─────────────────────────────────────────────────────────────────

    /** Minimum in-game minutes between random brawls. */
    static final float BRAWL_MIN_MINUTES = 30.0f;
    /** Maximum in-game minutes between random brawls. */
    static final float BRAWL_MAX_MINUTES = 60.0f;
    /** Brawl wins in one night needed for LAST_MAN_STANDING achievement. */
    static final int BRAWL_WINS_FOR_ACHIEVEMENT = 3;

    // ── Safe heist ────────────────────────────────────────────────────────────

    /** Lockpick success probability per attempt (30%). */
    static final float LOCKPICK_CHANCE = 0.30f;
    /** Maximum lockpick attempts allowed. */
    static final int LOCKPICK_MAX_ATTEMPTS = 3;
    /** Seconds to hold E for safe cracking with stethoscope. */
    static final float SAFE_CRACK_HOLD_SECONDS = 6.0f;
    /** Minimum COIN looted from safe. */
    static final int SAFE_LOOT_MIN_COIN = 50;
    /** Maximum COIN looted from safe. */
    static final int SAFE_LOOT_MAX_COIN = 100;
    /** NoiseSystem level triggered when safe is cracked. */
    static final float SAFE_NOISE_LEVEL = 2.0f;

    // ── VIP / Marchetti ───────────────────────────────────────────────────────

    /** Marchetti respect required to access VIP booth. */
    static final int MARCHETTI_VIP_THRESHOLD = 50;
    /** Marchetti respect at which a mission is offered. */
    static final int MARCHETTI_MISSION_THRESHOLD = 60;
    /** NotorietySystem points gained when player overhears Marchetti. */
    static final int MARCHETTI_OVERHEAR_NOTORIETY = 5;
    /** Radius in blocks within which the player overhears Marchetti conversation. */
    static final float MARCHETTI_OVERHEAR_RADIUS = 3.0f;

    // ── Ejection / barred tracking ────────────────────────────────────────────

    /** Number of ejections (across separate nights) for BARRED_FOR_LIFE achievement. */
    static final int EJECTIONS_FOR_BARRED_FOR_LIFE = 3;

    // ── Enum ──────────────────────────────────────────────────────────────────

    /**
     * Drunkenness levels.  Increases on drink purchase; decays at 20 in-game minutes/step.
     */
    public enum DrunkLevel {
        SOBER, TIPSY, DRUNK, SMASHED
    }

    /**
     * Result of an entry attempt at BOUNCER_BOOTH_PROP.
     */
    public enum EntryResult {
        /** Entry granted, fee deducted. */
        ENTERED,
        /** Club not currently open. */
        CLUB_CLOSED,
        /** Player has ≥ 2 wanted stars and no disguise. */
        REFUSED_WANTED,
        /** Player cannot afford the entry fee. */
        REFUSED_NO_MONEY,
        /** Player is barred for tonight due to prior ejection. */
        REFUSED_BARRED_TONIGHT
    }

    /**
     * Result of a bar drink purchase.
     */
    public enum BarResult {
        /** Drink purchased successfully. */
        PURCHASED,
        /** Player cannot afford the drink. */
        INSUFFICIENT_FUNDS,
        /** Player is already at maximum drunkenness (SMASHED). */
        TOO_DRUNK
    }

    /**
     * Result of a dancefloor brawl intervention.
     */
    public enum BrawlResult {
        /** Player won the brawl. */
        WON,
        /** Player avoided the brawl (auto-resolved). */
        AVOIDED,
        /** Player started or escalated the brawl — ejected, NIGHTCLUB_AFFRAY recorded. */
        STARTED_EJECTED
    }

    /**
     * Result of an office door lockpick attempt.
     */
    public enum LockpickResult {
        /** Door unlocked. */
        SUCCESS,
        /** Attempt failed; more attempts remain. */
        FAILED,
        /** All attempts exhausted; door locked permanently for this session. */
        LOCKED_OUT,
        /** Player has no LOCKPICK in inventory. */
        NO_LOCKPICK
    }

    /**
     * Result of safe cracking.
     */
    public enum SafeResult {
        /** Safe cracked; loot added to inventory. */
        CRACKED,
        /** Player does not have a STETHOSCOPE. */
        NO_STETHOSCOPE,
        /** Hold duration not yet complete. */
        IN_PROGRESS,
        /** Terry (NIGHTCLUB_MANAGER) caught the player. */
        CAUGHT
    }

    // ── Dependencies (injected) ───────────────────────────────────────────────

    private WantedSystem wantedSystem;
    private DisguiseSystem disguiseSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private FactionSystem factionSystem;
    private MCBattleSystem mcBattleSystem;
    private NoiseSystem noiseSystem;
    private TaxiSystem taxiSystem;
    private KebabVanSystem kebabVanSystem;
    private NotorietySystem.AchievementCallback achievementCallback;

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the player is currently inside the club. */
    private boolean insideClub = false;

    /** Whether the player has entered the club at least once (for FIRST_TIMER). */
    private boolean firstTimerAwarded = false;

    /** Current drunkenness level. */
    private DrunkLevel drunkLevel = DrunkLevel.SOBER;

    /** In-game minutes accumulated for drunk decay. */
    private float drunkDecayAccumulator = 0f;

    /** Number of DOUBLE_VODKA consumed this session (for SMASHED threshold). */
    private int vodkaCount = 0;

    /** Whether the player is barred for tonight. */
    private boolean barredTonight = false;

    /** Total lifetime ejections across separate nights (for BARRED_FOR_LIFE). */
    private int lifetimeEjections = 0;

    /** The day number on which the most recent ejection occurred (to count separate nights). */
    private int lastEjectionDay = -1;

    /** Brawl wins accumulated in the current night. */
    private int brawlWinsThisNight = 0;

    /** Whether a brawl is currently active on the dancefloor. */
    private boolean brawlActive = false;

    /** In-game minutes until the next random brawl. */
    private float nextBrawlTimer = 0f;

    /** Whether the office door has been unlocked (by key or lockpick). */
    private boolean officeDoorUnlocked = false;

    /** Number of lockpick attempts used on the office door this session. */
    private int lockpickAttemptsUsed = 0;

    /** Accumulated hold time (seconds) for safe cracking. */
    private float safeCrackProgress = 0f;

    /** Whether the safe has already been cracked this session. */
    private boolean safeCracked = false;

    /** Whether CRACKING_THE_VAULTS achievement has been awarded. */
    private boolean crackingVaultsAwarded = false;

    /** Whether the closing-time surge has been triggered this opening session. */
    private boolean surgeFiredThisSession = false;

    /** Whether we are currently in an open session (to reset per-session state). */
    private boolean sessionOpen = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public NightclubSystem() {
        this(new Random());
    }

    public NightclubSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setDisguiseSystem(DisguiseSystem disguiseSystem) {
        this.disguiseSystem = disguiseSystem;
    }

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setMcBattleSystem(MCBattleSystem mcBattleSystem) {
        this.mcBattleSystem = mcBattleSystem;
    }

    public void setNoiseSystem(NoiseSystem noiseSystem) {
        this.noiseSystem = noiseSystem;
    }

    public void setTaxiSystem(TaxiSystem taxiSystem) {
        this.taxiSystem = taxiSystem;
    }

    public void setKebabVanSystem(KebabVanSystem kebabVanSystem) {
        this.kebabVanSystem = kebabVanSystem;
    }

    public void setAchievementCallback(NotorietySystem.AchievementCallback achievementCallback) {
        this.achievementCallback = achievementCallback;
    }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns whether the club is currently open.
     *
     * @param dayOfWeek  current day of week (1=Mon … 7=Sun)
     * @param hour       current in-game hour (0.0–23.99)
     */
    public boolean isOpen(int dayOfWeek, float hour) {
        boolean validDay = (dayOfWeek >= OPEN_DAY_MIN && dayOfWeek <= OPEN_DAY_MAX);
        // Club open from 22:00 until 03:00 the following morning
        // i.e. hour >= 22 OR hour < 3
        boolean validHour = (hour >= OPEN_HOUR || hour < CLOSE_HOUR);
        return validDay && validHour;
    }

    // ── Entry ─────────────────────────────────────────────────────────────────

    /**
     * Attempt entry at the BOUNCER_BOOTH_PROP.
     *
     * @param dayOfWeek    current day of week (1=Mon … 7=Sun)
     * @param hour         current in-game hour (0.0–23.99)
     * @param inventory    player inventory (entry fee deducted on success)
     * @param player       player entity (for position when seeding rumours)
     * @param nearbyNpcs   NPCs near the door (used for rumour seeding)
     * @param dayCount     absolute in-game day counter (for separate-night ejection tracking)
     * @return result of the entry attempt
     */
    public EntryResult tryEnter(int dayOfWeek, float hour, Inventory inventory,
                                Player player, List<NPC> nearbyNpcs, int dayCount) {
        if (!isOpen(dayOfWeek, hour)) {
            return EntryResult.CLUB_CLOSED;
        }
        if (barredTonight) {
            return EntryResult.REFUSED_BARRED_TONIGHT;
        }

        // Wanted check — bypass if disguised
        if (wantedSystem != null && wantedSystem.getWantedStars() >= MAX_STARS_FOR_ENTRY) {
            boolean disguised = (disguiseSystem != null && disguiseSystem.isDisguised());
            if (!disguised) {
                return EntryResult.REFUSED_WANTED;
            }
        }

        // Entry fee
        if (!inventory.removeItem(Material.COIN, ENTRY_FEE)) {
            return EntryResult.REFUSED_NO_MONEY;
        }

        insideClub = true;

        // Seed HARD_NUT_IN_TOWN if notoriety ≥ 50
        if (notorietySystem != null && notorietySystem.getNotoriety() >= HARD_NUT_NOTORIETY_THRESHOLD) {
            if (rumourNetwork != null && nearbyNpcs != null) {
                for (NPC npc : nearbyNpcs) {
                    if (npc.getType() == NPCType.BOUNCER || npc.getType() == NPCType.NIGHTCLUB_PUNTER) {
                        rumourNetwork.addRumour(npc, new Rumour(RumourType.HARD_NUT_IN_TOWN,
                                "Proper hard nut about — Big Dave nearly didn't let them in."));
                        break;
                    }
                }
            }
        }

        // FIRST_TIMER achievement
        if (!firstTimerAwarded) {
            firstTimerAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FIRST_TIMER);
            }
        }

        // Reset per-session night state when entering a new open session
        if (!sessionOpen) {
            sessionOpen = true;
            surgeFiredThisSession = false;
            brawlWinsThisNight = 0;
            scheduleNextBrawl();
        }

        return EntryResult.ENTERED;
    }

    // ── Bar ───────────────────────────────────────────────────────────────────

    /**
     * Purchase CHEAP_LAGER from the NIGHTCLUB_BAR_PROP.
     * Costs 2 COIN; grants +5 HP; sets DrunkLevel to at least TIPSY.
     *
     * @param inventory player inventory
     * @param player    player entity (HP modified on success)
     * @return result of the purchase
     */
    public BarResult buyLager(Inventory inventory, Player player) {
        if (drunkLevel == DrunkLevel.SMASHED) {
            return BarResult.TOO_DRUNK;
        }
        if (!inventory.removeItem(Material.COIN, LAGER_PRICE)) {
            return BarResult.INSUFFICIENT_FUNDS;
        }
        if (player != null) {
            player.heal(LAGER_HP);
        }
        if (drunkLevel == DrunkLevel.SOBER) {
            drunkLevel = DrunkLevel.TIPSY;
        }
        return BarResult.PURCHASED;
    }

    /**
     * Purchase DOUBLE_VODKA from the NIGHTCLUB_BAR_PROP.
     * Costs 3 COIN; grants +3 HP; escalates DrunkLevel.  After 3 vodkas: SMASHED.
     *
     * @param inventory player inventory
     * @param player    player entity (HP modified on success)
     * @return result of the purchase
     */
    public BarResult buyVodka(Inventory inventory, Player player) {
        if (drunkLevel == DrunkLevel.SMASHED) {
            return BarResult.TOO_DRUNK;
        }
        if (!inventory.removeItem(Material.COIN, VODKA_PRICE)) {
            return BarResult.INSUFFICIENT_FUNDS;
        }
        if (player != null) {
            player.heal(VODKA_HP);
        }
        vodkaCount++;
        // Escalate drunk level
        if (vodkaCount >= VODKA_SMASHED_COUNT) {
            drunkLevel = DrunkLevel.SMASHED;
        } else {
            drunkLevel = escalateDrunk(drunkLevel);
        }
        return BarResult.PURCHASED;
    }

    private DrunkLevel escalateDrunk(DrunkLevel level) {
        switch (level) {
            case SOBER: return DrunkLevel.TIPSY;
            case TIPSY: return DrunkLevel.DRUNK;
            case DRUNK:  return DrunkLevel.SMASHED;
            default:     return DrunkLevel.SMASHED;
        }
    }

    // ── Drunk decay ───────────────────────────────────────────────────────────

    /**
     * Decrement drunk level one step (called every {@link #DRUNK_DECAY_MINUTES} in-game minutes).
     */
    private void decayDrunk() {
        switch (drunkLevel) {
            case SMASHED: drunkLevel = DrunkLevel.DRUNK;  break;
            case DRUNK:   drunkLevel = DrunkLevel.TIPSY;  break;
            case TIPSY:   drunkLevel = DrunkLevel.SOBER;  break;
            default:      break;
        }
    }

    // ── Brawl ─────────────────────────────────────────────────────────────────

    /**
     * Schedule the next random brawl at 30–60 in-game minutes from now.
     */
    private void scheduleNextBrawl() {
        nextBrawlTimer = BRAWL_MIN_MINUTES
                + random.nextFloat() * (BRAWL_MAX_MINUTES - BRAWL_MIN_MINUTES);
    }

    /**
     * Resolve a dancefloor brawl from the player's chosen intervention.
     *
     * @param action       {@code WIN} or {@code AVOID} or {@code START}
     * @param player       player entity (position used for wanted stars)
     * @param nearbyNpcs   NPCs on the dancefloor
     * @param dayCount     current in-game day (for ejection tracking)
     * @return result of the brawl interaction
     */
    public BrawlResult resolveBrawl(BrawlAction action, Player player,
                                   List<NPC> nearbyNpcs, int dayCount) {
        brawlActive = false;
        switch (action) {
            case WIN:
                brawlWinsThisNight++;
                if (brawlWinsThisNight >= BRAWL_WINS_FOR_ACHIEVEMENT && achievementCallback != null) {
                    achievementCallback.award(AchievementType.LAST_MAN_STANDING);
                }
                scheduleNextBrawl();
                return BrawlResult.WON;

            case AVOID:
                scheduleNextBrawl();
                return BrawlResult.AVOIDED;

            case START:
                if (criminalRecord != null) {
                    criminalRecord.record(CriminalRecord.CrimeType.NIGHTCLUB_AFFRAY);
                }
                float px = (player != null) ? player.getPosition().x : 0f;
                float py = (player != null) ? player.getPosition().y : 0f;
                float pz = (player != null) ? player.getPosition().z : 0f;
                if (wantedSystem != null) {
                    wantedSystem.addWantedStars(1, px, py, pz, null);
                }
                eject(dayCount);
                scheduleNextBrawl();
                return BrawlResult.STARTED_EJECTED;

            default:
                scheduleNextBrawl();
                return BrawlResult.AVOIDED;
        }
    }

    /**
     * Eject the player from the club: set barredTonight, track lifetime ejections,
     * check BARRED_FOR_LIFE.
     */
    private void eject(int dayCount) {
        insideClub = false;
        barredTonight = true;

        // Only count as a new ejection night if it's a different day
        if (dayCount != lastEjectionDay) {
            lastEjectionDay = dayCount;
            lifetimeEjections++;
            if (lifetimeEjections >= EJECTIONS_FOR_BARRED_FOR_LIFE && achievementCallback != null) {
                achievementCallback.award(AchievementType.BARRED_FOR_LIFE);
            }
        }
    }

    /**
     * Brawl action choices available to the player.
     */
    public enum BrawlAction {
        WIN, AVOID, START
    }

    // ── Office door ───────────────────────────────────────────────────────────

    /**
     * Attempt to open the NIGHTCLUB_OFFICE_DOOR_PROP using the master key.
     *
     * @param inventory player inventory (key consumed on success)
     * @return {@code true} if door was unlocked with the key
     */
    public boolean tryOpenWithKey(Inventory inventory) {
        if (inventory.hasItem(Material.NIGHTCLUB_MASTER_KEY)) {
            inventory.removeItem(Material.NIGHTCLUB_MASTER_KEY, 1);
            officeDoorUnlocked = true;
            return true;
        }
        return false;
    }

    /**
     * Attempt to lockpick the NIGHTCLUB_OFFICE_DOOR_PROP.
     * Each attempt has a 30% success chance; max 3 attempts.
     *
     * @param inventory player inventory (LOCKPICK consumed on each attempt)
     * @return result of the attempt
     */
    public LockpickResult tryLockpick(Inventory inventory) {
        if (!inventory.hasItem(Material.LOCKPICK)) {
            return LockpickResult.NO_LOCKPICK;
        }
        if (lockpickAttemptsUsed >= LOCKPICK_MAX_ATTEMPTS) {
            return LockpickResult.LOCKED_OUT;
        }
        inventory.removeItem(Material.LOCKPICK, 1);
        lockpickAttemptsUsed++;
        if (random.nextFloat() < LOCKPICK_CHANCE) {
            officeDoorUnlocked = true;
            return LockpickResult.SUCCESS;
        }
        if (lockpickAttemptsUsed >= LOCKPICK_MAX_ATTEMPTS) {
            return LockpickResult.LOCKED_OUT;
        }
        return LockpickResult.FAILED;
    }

    // ── Safe heist ────────────────────────────────────────────────────────────

    /**
     * Update the safe-cracking progress while the player holds E.
     *
     * @param delta        seconds elapsed this frame
     * @param holding      whether the player is currently holding E
     * @param inventory    player inventory (stethoscope checked; loot added on crack)
     * @param player       player entity (position for WantedSystem)
     * @param nearbyNpcs   used to check if Terry (NIGHTCLUB_MANAGER) is within catching range
     * @return current state of the safe crack attempt
     */
    public SafeResult updateSafeCrack(float delta, boolean holding, Inventory inventory,
                                      Player player, List<NPC> nearbyNpcs) {
        if (safeCracked) {
            return SafeResult.CRACKED;
        }
        if (!inventory.hasItem(Material.STETHOSCOPE)) {
            return SafeResult.NO_STETHOSCOPE;
        }
        if (!holding) {
            safeCrackProgress = 0f;
            return SafeResult.IN_PROGRESS;
        }

        // Check if Terry is nearby (catching the player)
        if (isTerryNearby(player, nearbyNpcs)) {
            float px = (player != null) ? player.getPosition().x : 0f;
            float py = (player != null) ? player.getPosition().y : 0f;
            float pz = (player != null) ? player.getPosition().z : 0f;
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(2, px, py, pz, null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.TRESPASSING);
                criminalRecord.record(CriminalRecord.CrimeType.BURGLARY);
            }
            safeCrackProgress = 0f;
            return SafeResult.CAUGHT;
        }

        safeCrackProgress += delta;
        if (safeCrackProgress < SAFE_CRACK_HOLD_SECONDS) {
            return SafeResult.IN_PROGRESS;
        }

        // Safe cracked!
        safeCracked = true;
        int coinLoot = SAFE_LOOT_MIN_COIN
                + random.nextInt(SAFE_LOOT_MAX_COIN - SAFE_LOOT_MIN_COIN + 1);
        inventory.addItem(Material.COIN, coinLoot);
        inventory.addItem(Material.PILLS, 1);

        if (noiseSystem != null) {
            noiseSystem.addNoise(SAFE_NOISE_LEVEL);
        }

        if (!crackingVaultsAwarded) {
            crackingVaultsAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CRACKING_THE_VAULTS);
            }
        }

        return SafeResult.CRACKED;
    }

    private boolean isTerryNearby(Player player, List<NPC> nearbyNpcs) {
        if (player == null || nearbyNpcs == null) return false;
        for (NPC npc : nearbyNpcs) {
            if (npc.getType() == NPCType.NIGHTCLUB_MANAGER) {
                float dx = npc.getPosition().x - player.getPosition().x;
                float dz = npc.getPosition().z - player.getPosition().z;
                if (dx * dx + dz * dz <= 9.0f) { // within 3 blocks
                    return true;
                }
            }
        }
        return false;
    }

    // ── VIP / Marchetti ───────────────────────────────────────────────────────

    /**
     * Returns whether the player can access the VIP booth (Marchetti Respect ≥ 50).
     */
    public boolean canAccessVip() {
        if (factionSystem == null) return false;
        return factionSystem.getRespect(Faction.MARCHETTI_CREW) >= MARCHETTI_VIP_THRESHOLD;
    }

    /**
     * Returns whether a Marchetti mission should be offered (Respect ≥ 60).
     */
    public boolean isMarchettiMissionAvailable() {
        if (factionSystem == null) return false;
        return factionSystem.getRespect(Faction.MARCHETTI_CREW) >= MARCHETTI_MISSION_THRESHOLD;
    }

    /**
     * Called each frame while the player is within {@link #MARCHETTI_OVERHEAR_RADIUS} blocks
     * of a NIGHTCLUB_MANAGER (Terry / Marchetti).  Seeds ORGANISED_CRIME rumour and grants
     * +5 notoriety (once per session).
     *
     * @param player     player entity
     * @param nearbyNpcs all nearby NPCs
     */
    public void checkMarchettiOverhear(Player player, List<NPC> nearbyNpcs) {
        if (marchettiOverheardThisSession) return;
        if (player == null || nearbyNpcs == null) return;
        for (NPC npc : nearbyNpcs) {
            if (npc.getType() == NPCType.NIGHTCLUB_MANAGER) {
                float dx = npc.getPosition().x - player.getPosition().x;
                float dz = npc.getPosition().z - player.getPosition().z;
                if (dx * dx + dz * dz <= MARCHETTI_OVERHEAR_RADIUS * MARCHETTI_OVERHEAR_RADIUS) {
                    marchettiOverheardThisSession = true;
                    if (rumourNetwork != null) {
                        rumourNetwork.addRumour(npc, new Rumour(RumourType.ORGANISED_CRIME,
                                "Marchetti lot talking serious business in the VIP."));
                    }
                    if (notorietySystem != null && achievementCallback != null) {
                        notorietySystem.addNotoriety(MARCHETTI_OVERHEAR_NOTORIETY, achievementCallback);
                    }
                    break;
                }
            }
        }
    }

    /** Whether ORGANISED_CRIME has been seeded via overhearing Marchetti this session. */
    private boolean marchettiOverheardThisSession = false;

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Per-frame update.  Handles drunk decay, random brawl scheduling, and closing-time surge.
     *
     * @param delta         seconds elapsed this frame
     * @param inGameMinutes in-game minutes elapsed this frame (delta × time scale)
     * @param hour          current in-game hour (0.0–23.99)
     * @param dayOfWeek     current day of week (1=Mon … 7=Sun)
     */
    public void update(float delta, float inGameMinutes, float hour, int dayOfWeek) {
        // Reset barredTonight at start of next open session
        if (!isOpen(dayOfWeek, hour)) {
            if (sessionOpen) {
                // Club just closed; clean up per-session flags
                sessionOpen = false;
                barredTonight = false;
                insideClub = false;
                marchettiOverheardThisSession = false;
                drunkLevel = DrunkLevel.SOBER;
                vodkaCount = 0;
                brawlWinsThisNight = 0;
                brawlActive = false;
                officeDoorUnlocked = false;
                lockpickAttemptsUsed = 0;
                safeCrackProgress = 0f;
                safeCracked = false;
            }
            return;
        }

        // Drunk decay
        if (drunkLevel != DrunkLevel.SOBER) {
            drunkDecayAccumulator += inGameMinutes;
            if (drunkDecayAccumulator >= DRUNK_DECAY_MINUTES) {
                drunkDecayAccumulator -= DRUNK_DECAY_MINUTES;
                decayDrunk();
            }
        }

        // Random brawl scheduling
        if (insideClub && !brawlActive) {
            nextBrawlTimer -= inGameMinutes;
            if (nextBrawlTimer <= 0f) {
                brawlActive = true;
            }
        }

        // Closing-time surge at 03:00
        if (!surgeFiredThisSession && hour >= SURGE_HOUR && hour < SURGE_HOUR + 0.5f) {
            surgeFiredThisSession = true;
            if (taxiSystem != null) {
                taxiSystem.setClosingTimeSurge(true);
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** @return whether the player is currently inside the club */
    public boolean isInsideClub() {
        return insideClub;
    }

    /** @return current drunkenness level */
    public DrunkLevel getDrunkLevel() {
        return drunkLevel;
    }

    /** @return whether a dancefloor brawl is currently active */
    public boolean isBrawlActive() {
        return brawlActive;
    }

    /** @return whether the office door is unlocked */
    public boolean isOfficeDoorUnlocked() {
        return officeDoorUnlocked;
    }

    /** @return whether the safe has been cracked */
    public boolean isSafeCracked() {
        return safeCracked;
    }

    /** @return current safe crack progress in seconds */
    public float getSafeCrackProgress() {
        return safeCrackProgress;
    }

    /** @return number of brawl wins accumulated this night */
    public int getBrawlWinsThisNight() {
        return brawlWinsThisNight;
    }

    /** @return total lifetime ejections across separate nights */
    public int getLifetimeEjections() {
        return lifetimeEjections;
    }

    /** @return whether the player is barred for tonight */
    public boolean isBarredTonight() {
        return barredTonight;
    }

    /** @return number of lockpick attempts used on the office door */
    public int getLockpickAttemptsUsed() {
        return lockpickAttemptsUsed;
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Force drunk level for testing. */
    public void setDrunkLevelForTesting(DrunkLevel level) {
        this.drunkLevel = level;
    }

    /** Force brawl active state for testing. */
    public void setBrawlActiveForTesting(boolean active) {
        this.brawlActive = active;
    }

    /** Force lifetime ejections for testing. */
    public void setLifetimeEjectionsForTesting(int count) {
        this.lifetimeEjections = count;
    }

    /** Force brawl wins this night for testing. */
    public void setBrawlWinsThisNightForTesting(int count) {
        this.brawlWinsThisNight = count;
    }

    /** Force inside-club state for testing. */
    public void setInsideClubForTesting(boolean inside) {
        this.insideClub = inside;
    }

    /** Force office door unlocked for testing. */
    public void setOfficeDoorUnlockedForTesting(boolean unlocked) {
        this.officeDoorUnlocked = unlocked;
    }
}
