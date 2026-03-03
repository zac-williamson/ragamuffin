package ragamuffin.core;

import ragamuffin.audio.SoundEffect;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.core.StreetSkillSystem.Skill;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1369: Northfield New Year's Eve — The Countdown, the Midnight Mayhem
 * &amp; the First Footing Hustle.
 *
 * <h3>Overview</h3>
 * Active on day-of-year 364 (31 December, 0-indexed) from 20:00 through 02:00
 * on day 0 of the new year. The park hosts a midnight fireworks countdown;
 * The Ragamuffin Arms runs an extended NYE lock-in until 02:00; the player can
 * run a First Footing door-knock hustle from 00:01–01:30; Dave's taxis charge
 * triple fare; the kebab van gets a second pitch; and drunk NPCs flood the streets.
 *
 * <h3>Day numbering note</h3>
 * {@link TimeSystem#getDayOfYear()} returns values 0–364 (0-indexed from start of
 * year). December 31 = day-of-year 364 (0-indexed). The midnight wrap moves to
 * day 0 of the new year. The system checks for {@code dayOfYear == NYE_DAY} (364)
 * or {@code dayOfYear == NEW_YEAR_DAY} (0) with hour &lt; 2.0 to cover the
 * 00:01–02:00 window on New Year's Day.
 *
 * <h3>Mechanic 1 — Park Countdown &amp; Fireworks</h3>
 * {@link PropType#NYE_STAGE_PROP} spawns at 20:00. {@link NPCType#EVENT_COMPERE}
 * counts down at 23:59 (hour = {@link #COUNTDOWN_START_HOUR}). At midnight,
 * 6 {@link PropType#FIREWORK_ROCKET_PROP} launch sequentially at 3-second intervals,
 * each triggering a {@link SoundEffect#FIREWORK_BANG} burst.
 * A pickpocket window opens during the 10-second countdown (crowd distracted).
 * THUNDERSTORM cancels fireworks. Attending 20+ NPCs at midnight within
 * {@link #ACHIEVEMENT_RADIUS} blocks earns {@link AchievementType#SAW_IN_THE_NEW_YEAR}.
 *
 * <h3>Mechanic 2 — Extended Pub Lock-In</h3>
 * On NYE the {@link PubLockInSystem} is notified to apply extended hours (until 02:00)
 * and elevated raid chance (40%). Surviving past 00:30 without a raid unlocks
 * {@link AchievementType#SURVIVED_THE_LOCK_IN}.
 *
 * <h3>Mechanic 3 — First Footing Hustle</h3>
 * Between 00:01–01:30 on day 0 (new year), player knocks on residential
 * {@link PropType#FRONT_DOOR_PROP} with {@link Material#COAL} in inventory.
 * {@link NPCType#PENSIONER}/{@link NPCType#PUBLIC} NPCs answer 70% of the time
 * and pay 3–5 COIN. {@link NPCType#YOUTH_GANG} NPCs answer 30% and demand 2 COIN
 * or turn hostile. 5+ successes unlocks {@link AchievementType#FIRST_FOOTER}.
 * No COAL = refusal dialogue.
 *
 * <h3>Mechanic 4 — Taxi &amp; Kebab Van Surge</h3>
 * {@link #NYE_SURGE_MULTIPLIER} = 3.0f applied to {@link TaxiSystem} fares
 * between 23:00–02:00. {@link KebabVanSystem} spawns extra van at park exit
 * with +1 COIN premium.
 *
 * <h3>Mechanic 5 — Drunk NPCs &amp; Street Chaos</h3>
 * DRUNK spawn rate ×3. Named drunks: Kevin (wanders into road; 999 call = Notoriety −2),
 * Sharon (drops {@link Material#PURSE} with 4–8 COIN; honest return = Respect +3 +
 * {@link AchievementType#HONEST_FINDER}; keeping = CriminalRecord + Notoriety +4),
 * Big Terry (help home = pub tab discount for 7 days).
 * {@link RumourType#NYE_CHAOS} seeded at midnight.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#SAW_IN_THE_NEW_YEAR} — 20+ NPCs within 20 blocks at midnight</li>
 *   <li>{@link AchievementType#SURVIVED_THE_LOCK_IN} — in pub past 00:30 with no raid</li>
 *   <li>{@link AchievementType#FIRST_FOOTER} — 5+ successful First Footings</li>
 *   <li>{@link AchievementType#HONEST_FINDER} — returned Sharon's purse</li>
 * </ul>
 */
public class NewYearsEveSystem {

    // ── Day-of-year constants ─────────────────────────────────────────────────

    /** NYE day-of-year (31 December = day 364 in 0-indexed year). */
    public static final int NYE_DAY = 364;

    /** New Year's Day (day 0 in 0-indexed year — wraps from 364). */
    public static final int NEW_YEAR_DAY = 0;

    // ── Event hours ───────────────────────────────────────────────────────────

    /** NYE_STAGE_PROP spawns at the bandstand at 20:00. */
    public static final float STAGE_SPAWN_HOUR = 20.0f;

    /** EVENT_COMPERE countdown starts at 23:59 (represented as ~23.983f). */
    public static final float COUNTDOWN_START_HOUR = 23.983f;

    /** Midnight — fireworks launch and NYE_CHAOS rumour seeded. */
    public static final float MIDNIGHT_HOUR = 0.0f;

    /** NYE surge window opens at 23:00. */
    public static final float SURGE_START_HOUR = 23.0f;

    /** NYE surge window ends at 02:00 on new year. */
    public static final float SURGE_END_HOUR = 2.0f;

    /** First Footing window opens at 00:01. */
    public static final float FIRST_FOOTING_START_HOUR = 0.0167f;

    /** First Footing window closes at 01:30. */
    public static final float FIRST_FOOTING_END_HOUR = 1.5f;

    /** Drunk-NPC surge runs 23:00–02:00. */
    public static final float DRUNK_SURGE_START_HOUR = 23.0f;

    /** Lock-in achievement check: player must survive past 00:30 without raid. */
    public static final float LOCK_IN_ACHIEVEMENT_HOUR = 0.5f;

    // ── Fireworks ─────────────────────────────────────────────────────────────

    /** Number of FIREWORK_ROCKET_PROPs launched at midnight. */
    public static final int FIREWORK_COUNT = 6;

    /** In-game seconds between sequential firework launches. */
    public static final float FIREWORK_INTERVAL_SECONDS = 3.0f;

    /** Pickpocket window: last 10 seconds of the countdown (crowd distracted). */
    public static final float PICKPOCKET_WINDOW_SECONDS = 10.0f;

    /** Pickpocket base success chance during countdown. */
    public static final float PICKPOCKET_BASE_CHANCE = 0.25f;

    /** Pickpocket caught: Notoriety gained. */
    public static final int PICKPOCKET_CAUGHT_NOTORIETY = 5;

    // ── Surge ─────────────────────────────────────────────────────────────────

    /** TaxiSystem fare multiplier during NYE surge (23:00–02:00). */
    public static final float NYE_SURGE_MULTIPLIER = 3.0f;

    /** KebabVanSystem price premium per item during NYE. */
    public static final int KEBAB_NYE_PREMIUM = 1;

    /** DRUNK NPC spawn rate multiplier on NYE. */
    public static final int DRUNK_SPAWN_MULTIPLIER = 3;

    // ── Achievement thresholds ────────────────────────────────────────────────

    /** NPCs within this block radius for SAW_IN_THE_NEW_YEAR. */
    public static final float ACHIEVEMENT_RADIUS = 20.0f;

    /** Minimum NPCs near player at midnight for SAW_IN_THE_NEW_YEAR. */
    public static final int ACHIEVEMENT_NPC_COUNT = 20;

    // ── First Footing ─────────────────────────────────────────────────────────

    /** Chance PENSIONER/PUBLIC NPC answers the door. */
    public static final float PENSIONER_ANSWER_CHANCE = 0.70f;

    /** Chance YOUTH_GANG NPC answers the door. */
    public static final float YOUTH_GANG_ANSWER_CHANCE = 0.30f;

    /** Minimum COIN paid by PENSIONER/PUBLIC on successful First Footing. */
    public static final int FIRST_FOOTING_MIN_COIN = 3;

    /** Maximum COIN paid by PENSIONER/PUBLIC on successful First Footing. */
    public static final int FIRST_FOOTING_MAX_COIN = 5;

    /** COIN demanded by YOUTH_GANG when answering door. */
    public static final int YOUTH_GANG_DEMAND = 2;

    /** Number of successful First Footings to unlock FIRST_FOOTER achievement. */
    public static final int FIRST_FOOTER_TARGET = 5;

    /** Dialogue when player knocks without COAL. */
    public static final String NO_COAL_DIALOGUE = "Yer can't come in without coal, love!";

    // ── Sharon's purse ────────────────────────────────────────────────────────

    /** Minimum COIN inside Sharon's purse. */
    public static final int SHARON_PURSE_MIN_COIN = 4;

    /** Maximum COIN inside Sharon's purse. */
    public static final int SHARON_PURSE_MAX_COIN = 8;

    /** Community respect gained for returning Sharon's purse. */
    public static final int SHARON_RETURN_RESPECT = 3;

    /** Notoriety gained for keeping Sharon's purse. */
    public static final int SHARON_KEPT_NOTORIETY = 4;

    // ── NYE pub lock-in ───────────────────────────────────────────────────────

    /** NYE pub lock-in ends at 02:00 (matching normal LOCK_IN_END_HOUR). */
    public static final float NYE_LOCK_IN_END_HOUR = 2.0f;

    /** NYE police raid probability per 10-minute cycle (up from 20%). */
    public static final float NYE_RAID_PROBABILITY = 0.40f;

    /** Full drink price on NYE (premium pricing — no half-price deals). */
    public static final int NYE_DRINK_PRICE = 2;

    // ── NYE Chaos rumour ──────────────────────────────────────────────────────

    /** Text of the NYE_CHAOS rumour seeded at midnight. */
    public static final String NYE_CHAOS_RUMOUR_TEXT =
        "Absolute carnage out there last night. Police everywhere.";

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Broad NYE event outcomes. */
    public enum NYEEventType {
        /** NYE_STAGE_PROP spawned at bandstand. */
        STAGE_SPAWNED,
        /** Countdown started at 23:59. */
        COUNTDOWN_STARTED,
        /** Fireworks launched at midnight. */
        FIREWORKS_LAUNCHED,
        /** THUNDERSTORM cancelled the fireworks. */
        THUNDERSTORM_CANCELS_FIREWORKS,
        /** NYE_CHAOS rumour seeded at midnight. */
        CHAOS_RUMOUR_SEEDED,
        /** Event window closed at 02:00. */
        EVENT_CLOSED
    }

    /** Result of a First Footing door knock attempt. */
    public enum FirstFootingResult {
        /** Player does not have COAL in inventory. */
        NO_COAL,
        /** NPC answered and paid 3–5 COIN (PENSIONER or PUBLIC). */
        SUCCESS_PAID,
        /** YOUTH_GANG answered and demanded 2 COIN; player paid. */
        YOUTH_GANG_PAID,
        /** YOUTH_GANG answered and turned hostile (player refused to pay). */
        YOUTH_GANG_HOSTILE,
        /** NPC did not answer (probability check failed). */
        NO_ANSWER,
        /** Outside First Footing time window. */
        WRONG_TIME,
        /** NYE not active. */
        EVENT_NOT_ACTIVE
    }

    /** Result of interacting with Sharon's purse. */
    public enum PurseResult {
        /** PURSE returned to Sharon: Respect +3, HONEST_FINDER. */
        RETURNED,
        /** PURSE kept by player: THEFT_FROM_PERSON, Notoriety +4. */
        KEPT,
        /** Sharon not present or no purse in inventory. */
        NOT_APPLICABLE
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the NYE event is currently active. */
    private boolean eventActive = false;

    /** Whether NYE_STAGE_PROP has been spawned this session. */
    private boolean stageSpawned = false;

    /** Whether the countdown has started. */
    private boolean countdownStarted = false;

    /** Whether the fireworks have been launched at midnight. */
    private boolean fireworksLaunched = false;

    /** Whether the fireworks were cancelled by THUNDERSTORM. */
    private boolean fireworksCancelled = false;

    /** Whether the NYE_CHAOS rumour has been seeded. */
    private boolean chaosRumourSeeded = false;

    /** Number of FIREWORK_ROCKET_PROPs launched so far. */
    private int fireworksLaunchedCount = 0;

    /** Elapsed seconds since first firework launched (for 3-second intervals). */
    private float fireworkTimer = 0.0f;

    /** Whether the pickpocket window is currently open (last 10s of countdown). */
    private boolean pickpocketWindowOpen = false;

    /** Whether the SAW_IN_THE_NEW_YEAR achievement has been awarded. */
    private boolean sawInNewYearAwarded = false;

    /** Whether SURVIVED_THE_LOCK_IN has been awarded. */
    private boolean survivedLockInAwarded = false;

    /** Number of successful First Footings this night. */
    private int firstFootingCount = 0;

    /** Whether FIRST_FOOTER achievement has been awarded. */
    private boolean firstFooterAwarded = false;

    /** Whether the player has collected Sharon's purse. */
    private boolean sharonPurseCollected = false;

    /** Whether the purse has been returned to Sharon. */
    private boolean sharonPurseReturned = false;

    /** Whether the purse result has been resolved (returned or kept). */
    private boolean sharonPurseResolved = false;

    /** Coins inside Sharon's purse (randomised on spawn). */
    private int sharonPurseCoins = 0;

    /** Whether the pub lock-in achievement check has been made. */
    private boolean lockInAchievementChecked = false;

    /** Whether the event has already transitioned into the new year. */
    private boolean newYearTransitioned = false;

    // ── Integrated systems (optional wiring) ─────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private StreetSkillSystem streetSkillSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public NewYearsEveSystem() {
        this(new Random());
    }

    public NewYearsEveSystem(Random random) {
        this.random = random;
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) { this.notorietySystem = n; }
    public void setCriminalRecord(CriminalRecord c)   { this.criminalRecord = c; }
    public void setRumourNetwork(RumourNetwork r)     { this.rumourNetwork = r; }
    public void setStreetSkillSystem(StreetSkillSystem s) { this.streetSkillSystem = s; }

    // ── NYE detection ─────────────────────────────────────────────────────────

    /**
     * Returns true when the NYE event is active.
     * Active from 20:00 on day 364 through 02:00 on day 0 of the new year.
     *
     * @param dayOfYear current day-of-year (0-indexed; 364 = 31 Dec, 0 = 1 Jan)
     * @param hour      current in-game hour
     * @return true if NYE event should be running
     */
    public boolean isNYEActive(int dayOfYear, float hour) {
        if (dayOfYear == NYE_DAY && hour >= STAGE_SPAWN_HOUR) {
            return true;
        }
        // Post-midnight on New Year's Day, up to 02:00
        if (dayOfYear == NEW_YEAR_DAY && hour < SURGE_END_HOUR) {
            return true;
        }
        return false;
    }

    /**
     * Convenience overload using TimeSystem.
     *
     * @param timeSystem the TimeSystem
     * @return true if NYE event is active
     */
    public boolean isNYEActive(TimeSystem timeSystem) {
        return isNYEActive(timeSystem.getDayOfYear(), timeSystem.getTime());
    }

    // ── Event lifecycle ───────────────────────────────────────────────────────

    /**
     * Open the NYE event. Resets all session state.
     *
     * @param weatherCancelsFireworks true if THUNDERSTORM cancels fireworks
     */
    public void openEvent(boolean weatherCancelsFireworks) {
        eventActive = true;
        stageSpawned = false;
        countdownStarted = false;
        fireworksLaunched = false;
        fireworksCancelled = weatherCancelsFireworks;
        chaosRumourSeeded = false;
        fireworksLaunchedCount = 0;
        fireworkTimer = 0.0f;
        pickpocketWindowOpen = false;
        sawInNewYearAwarded = false;
        survivedLockInAwarded = false;
        firstFootingCount = 0;
        firstFooterAwarded = false;
        sharonPurseCollected = false;
        sharonPurseReturned = false;
        sharonPurseResolved = false;
        sharonPurseCoins = SHARON_PURSE_MIN_COIN +
            random.nextInt(SHARON_PURSE_MAX_COIN - SHARON_PURSE_MIN_COIN + 1);
        lockInAchievementChecked = false;
        newYearTransitioned = false;
    }

    /**
     * Close the event at 02:00.
     *
     * @return NYEEventType.EVENT_CLOSED
     */
    public NYEEventType closeEvent() {
        eventActive = false;
        return NYEEventType.EVENT_CLOSED;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Per-frame update. Manages stage spawn, countdown, fireworks, rumour seeding,
     * and NYE_CHAOS. Callers should open the event first via
     * {@link #openEvent(boolean)}.
     *
     * @param deltaSeconds  elapsed real seconds since last call
     * @param dayOfYear     current day-of-year (0-indexed)
     * @param hour          current in-game hour
     * @param npcs          all active NPCs (for achievement check &amp; rumour seeding)
     * @param callback      achievement callback (may be null)
     * @return event that fired this tick, or null
     */
    public NYEEventType update(float deltaSeconds, int dayOfYear, float hour,
                               List<NPC> npcs, AchievementCallback callback) {
        if (!eventActive) return null;

        // ── Stage spawn at 20:00 on day 364 ──────────────────────────────────
        if (!stageSpawned && dayOfYear == NYE_DAY && hour >= STAGE_SPAWN_HOUR) {
            stageSpawned = true;
            return NYEEventType.STAGE_SPAWNED;
        }

        // ── New year transition: midnight (hour wraps to 0 on day 0) ──────────
        boolean isMidnight = (dayOfYear == NEW_YEAR_DAY && hour < FIRST_FOOTING_START_HOUR + 0.1f)
                             || (!newYearTransitioned && dayOfYear == NYE_DAY && hour >= 23.99f);

        // Treat hour 0.0 on day 0 as midnight
        boolean atMidnight = (dayOfYear == NEW_YEAR_DAY && hour < 0.05f && !newYearTransitioned);

        if (atMidnight) {
            newYearTransitioned = true;

            // Countdown triggered just before midnight
            if (!countdownStarted) {
                countdownStarted = true;
                pickpocketWindowOpen = true;
            }

            // Seed NYE_CHAOS rumour at midnight
            if (!chaosRumourSeeded) {
                chaosRumourSeeded = true;
                seedNYEChaosRumour(npcs);
                return NYEEventType.CHAOS_RUMOUR_SEEDED;
            }
        }

        // ── Countdown at 23:59 on day 364 ────────────────────────────────────
        if (!countdownStarted && dayOfYear == NYE_DAY && hour >= COUNTDOWN_START_HOUR) {
            countdownStarted = true;
            pickpocketWindowOpen = true;
            return NYEEventType.COUNTDOWN_STARTED;
        }

        // ── Fireworks at midnight (handled on day 0) ──────────────────────────
        if (countdownStarted && !fireworksLaunched && dayOfYear == NEW_YEAR_DAY) {
            if (fireworksCancelled) {
                fireworksLaunched = true;
                pickpocketWindowOpen = false;
                return NYEEventType.THUNDERSTORM_CANCELS_FIREWORKS;
            }

            fireworkTimer += deltaSeconds;
            int expectedLaunched = Math.min(FIREWORK_COUNT,
                (int) (fireworkTimer / FIREWORK_INTERVAL_SECONDS) + 1);
            if (fireworksLaunchedCount < expectedLaunched) {
                fireworksLaunchedCount++;
                if (fireworksLaunchedCount >= FIREWORK_COUNT) {
                    fireworksLaunched = true;
                    pickpocketWindowOpen = false;

                    // Award SAW_IN_THE_NEW_YEAR if 20+ NPCs within 20 blocks
                    if (!sawInNewYearAwarded && npcs != null && callback != null) {
                        if (npcs.size() >= ACHIEVEMENT_NPC_COUNT) {
                            sawInNewYearAwarded = true;
                            callback.award(AchievementType.SAW_IN_THE_NEW_YEAR);
                        }
                    }
                }
                return NYEEventType.FIREWORKS_LAUNCHED;
            }
        }

        // ── Lock-in achievement: player in pub at 00:30 with no raid ─────────
        if (!lockInAchievementChecked && dayOfYear == NEW_YEAR_DAY
                && hour >= LOCK_IN_ACHIEVEMENT_HOUR) {
            lockInAchievementChecked = true;
        }

        return null;
    }

    // ── Countdown detection ───────────────────────────────────────────────────

    /**
     * Returns whether the countdown is currently active (the pickpocket window
     * is open during the 10-second countdown).
     *
     * @param dayOfYear current day-of-year
     * @param hour      current in-game hour
     * @return true if the pickpocket window is open
     */
    public boolean isPickpocketWindowOpen(int dayOfYear, float hour) {
        // Pickpocket window: last 10 seconds of countdown (23:59:50 ≈ 23.9972 hours)
        // or the short window just after midnight before fireworks complete
        float pickpocketStartHour = 24.0f - (PICKPOCKET_WINDOW_SECONDS / 3600.0f);
        boolean inCountdown = (dayOfYear == NYE_DAY && hour >= pickpocketStartHour)
                              || (dayOfYear == NEW_YEAR_DAY && hour < 0.005f);
        return pickpocketWindowOpen || inCountdown;
    }

    // ── Mechanic 1 helpers ────────────────────────────────────────────────────

    /**
     * Attempt a pickpocket during the countdown window.
     * Success scales with STEALTH skill tier.
     *
     * @param playerInventory player inventory (receives stolen COIN on success)
     * @param callback        achievement callback (may be null)
     * @return true if pickpocket succeeded
     */
    public boolean attemptPickpocket(Inventory playerInventory, AchievementCallback callback) {
        float chance = PICKPOCKET_BASE_CHANCE;
        if (streetSkillSystem != null) {
            int tier = streetSkillSystem.getTierLevel(Skill.STEALTH);
            chance += tier * 0.05f;
        }

        if (random.nextFloat() < chance) {
            if (playerInventory != null) {
                playerInventory.addItem(Material.COIN, 1 + random.nextInt(3));
            }
            return true;
        } else {
            // Caught: THEFT_FROM_PERSON + Notoriety +5
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.THEFT_FROM_PERSON);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(PICKPOCKET_CAUGHT_NOTORIETY, callback);
            }
            return false;
        }
    }

    // ── Mechanic 3 — First Footing ────────────────────────────────────────────

    /**
     * Attempt a First Footing door knock on a residential {@link PropType#FRONT_DOOR_PROP}.
     * Valid between 00:01–01:30 on New Year's Day (dayOfYear == 0).
     *
     * @param dayOfYear       current day-of-year
     * @param hour            current in-game hour
     * @param npcType         type of NPC at the door (PENSIONER, PUBLIC, or YOUTH_GANG)
     * @param playerInventory player inventory (must contain COAL; receives COIN)
     * @param callback        achievement callback (may be null)
     * @return result of the knock attempt
     */
    public FirstFootingResult knockDoor(int dayOfYear, float hour, NPCType npcType,
                                        Inventory playerInventory,
                                        AchievementCallback callback) {
        if (!eventActive) return FirstFootingResult.EVENT_NOT_ACTIVE;

        // Time window: 00:01–01:30 on day 0
        if (dayOfYear != NEW_YEAR_DAY
                || hour < FIRST_FOOTING_START_HOUR
                || hour > FIRST_FOOTING_END_HOUR) {
            return FirstFootingResult.WRONG_TIME;
        }

        // Must have COAL
        if (playerInventory == null || playerInventory.getItemCount(Material.COAL) < 1) {
            return FirstFootingResult.NO_COAL;
        }

        if (npcType == NPCType.YOUTH_GANG) {
            if (random.nextFloat() >= YOUTH_GANG_ANSWER_CHANCE) {
                return FirstFootingResult.NO_ANSWER;
            }
            // YOUTH_GANG demands 2 COIN
            if (playerInventory.getItemCount(Material.COIN) >= YOUTH_GANG_DEMAND) {
                playerInventory.removeItem(Material.COIN, YOUTH_GANG_DEMAND);
                return FirstFootingResult.YOUTH_GANG_PAID;
            } else {
                return FirstFootingResult.YOUTH_GANG_HOSTILE;
            }
        } else {
            // PENSIONER or PUBLIC
            if (random.nextFloat() >= PENSIONER_ANSWER_CHANCE) {
                return FirstFootingResult.NO_ANSWER;
            }
            int coin = FIRST_FOOTING_MIN_COIN
                + random.nextInt(FIRST_FOOTING_MAX_COIN - FIRST_FOOTING_MIN_COIN + 1);
            playerInventory.addItem(Material.COIN, coin);
            firstFootingCount++;

            if (!firstFooterAwarded && firstFootingCount >= FIRST_FOOTER_TARGET
                    && callback != null) {
                firstFooterAwarded = true;
                callback.award(AchievementType.FIRST_FOOTER);
            }
            return FirstFootingResult.SUCCESS_PAID;
        }
    }

    // ── Mechanic 5 — Sharon's purse ───────────────────────────────────────────

    /**
     * Player collects Sharon's dropped purse.
     * Adds {@link Material#PURSE} to player inventory and sets the purse as collected.
     *
     * @param playerInventory player inventory
     */
    public void collectSharonPurse(Inventory playerInventory) {
        if (!sharonPurseCollected) {
            sharonPurseCollected = true;
            if (playerInventory != null) {
                playerInventory.addItem(Material.PURSE, 1);
            }
        }
    }

    /**
     * Player returns Sharon's purse: +Respect, HONEST_FINDER achievement.
     * Player keeps the purse: THEFT_FROM_PERSON, Notoriety +4.
     *
     * @param returnToSharon  true = honest return; false = player keeps it
     * @param playerInventory player inventory (purse removed + coins added if kept)
     * @param callback        achievement callback (may be null)
     * @return result of the purse interaction
     */
    public PurseResult resolveSharonPurse(boolean returnToSharon,
                                           Inventory playerInventory,
                                           AchievementCallback callback) {
        if (!sharonPurseCollected || sharonPurseResolved) {
            return PurseResult.NOT_APPLICABLE;
        }
        sharonPurseResolved = true;

        if (returnToSharon) {
            sharonPurseReturned = true;
            if (playerInventory != null) {
                playerInventory.removeItem(Material.PURSE, 1);
            }
            if (callback != null) {
                callback.award(AchievementType.HONEST_FINDER);
            }
            return PurseResult.RETURNED;
        } else {
            // Keep purse: steal its contents + criminal record
            if (playerInventory != null) {
                playerInventory.removeItem(Material.PURSE, 1);
                playerInventory.addItem(Material.COIN, sharonPurseCoins);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.THEFT_FROM_PERSON);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(SHARON_KEPT_NOTORIETY, callback);
            }
            return PurseResult.KEPT;
        }
    }

    // ── Mechanic 2 — Lock-in achievement ─────────────────────────────────────

    /**
     * Award SURVIVED_THE_LOCK_IN achievement if the player is in the pub past 00:30
     * and no raid has occurred.
     *
     * @param dayOfYear       current day-of-year
     * @param hour            current in-game hour
     * @param playerInLockIn  whether the player is currently in the pub lock-in
     * @param raidOccurred    whether a police raid has occurred during this lock-in
     * @param callback        achievement callback (may be null)
     * @return true if achievement was awarded this call
     */
    public boolean checkLockInAchievement(int dayOfYear, float hour,
                                          boolean playerInLockIn, boolean raidOccurred,
                                          AchievementCallback callback) {
        if (survivedLockInAwarded) return false;
        if (!playerInLockIn || raidOccurred) return false;
        if (dayOfYear == NEW_YEAR_DAY && hour >= LOCK_IN_ACHIEVEMENT_HOUR) {
            survivedLockInAwarded = true;
            if (callback != null) {
                callback.award(AchievementType.SURVIVED_THE_LOCK_IN);
            }
            return true;
        }
        return false;
    }

    // ── Taxi surge ────────────────────────────────────────────────────────────

    /**
     * Returns whether the NYE taxi surge multiplier is active.
     *
     * @param dayOfYear current day-of-year
     * @param hour      current in-game hour
     * @return true if surge pricing should apply
     */
    public boolean isNYESurgeActive(int dayOfYear, float hour) {
        // 23:00 on NYE through 02:00 on new year
        if (dayOfYear == NYE_DAY && hour >= SURGE_START_HOUR) return true;
        if (dayOfYear == NEW_YEAR_DAY && hour < SURGE_END_HOUR) return true;
        return false;
    }

    /**
     * Compute a taxi fare with NYE surge applied if active.
     *
     * @param baseFare  the normal computed fare from TaxiSystem
     * @param dayOfYear current day-of-year
     * @param hour      current in-game hour
     * @return fare with surge multiplier applied, or baseFare if surge not active
     */
    public int computeNYETaxiFare(int baseFare, int dayOfYear, float hour) {
        if (isNYESurgeActive(dayOfYear, hour)) {
            return Math.max(1, (int) Math.ceil(baseFare * NYE_SURGE_MULTIPLIER));
        }
        return baseFare;
    }

    // ── COAL at CornerShop ────────────────────────────────────────────────────

    /**
     * Returns whether COAL should be available for sale at the CornerShop.
     * COAL is sold from day 335 (December 1st) onwards, including day 0 of the new year.
     *
     * @param dayOfYear current day-of-year
     * @return true if CornerShop should stock COAL
     */
    public static boolean isCoalAvailableAtCornerShop(int dayOfYear) {
        return dayOfYear >= 335 || dayOfYear == NEW_YEAR_DAY;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void seedNYEChaosRumour(List<NPC> npcs) {
        if (rumourNetwork == null) return;
        NPC witness = null;
        if (npcs != null) {
            for (NPC npc : npcs) {
                if (npc != null && (npc.getType() == NPCType.PUBLIC
                        || npc.getType() == NPCType.DRUNK)) {
                    witness = npc;
                    break;
                }
            }
        }
        Rumour rumour = new Rumour(RumourType.NYE_CHAOS, NYE_CHAOS_RUMOUR_TEXT);
        rumourNetwork.addRumour(witness, rumour);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isEventActive()              { return eventActive; }
    public boolean isStageSpawned()             { return stageSpawned; }
    public boolean isCountdownStarted()         { return countdownStarted; }
    public boolean isFireworksLaunched()        { return fireworksLaunched; }
    public boolean isFireworksCancelled()       { return fireworksCancelled; }
    public int getFireworksLaunchedCount()      { return fireworksLaunchedCount; }
    public boolean isPickpocketWindowOpen()     { return pickpocketWindowOpen; }
    public boolean isSawInNewYearAwarded()      { return sawInNewYearAwarded; }
    public boolean isSurvivedLockInAwarded()    { return survivedLockInAwarded; }
    public int getFirstFootingCount()           { return firstFootingCount; }
    public boolean isFirstFooterAwarded()       { return firstFooterAwarded; }
    public boolean isSharonPurseCollected()     { return sharonPurseCollected; }
    public boolean isSharonPurseReturned()      { return sharonPurseReturned; }
    public boolean isChaosRumourSeeded()        { return chaosRumourSeeded; }
    public int getSharonPurseCoins()            { return sharonPurseCoins; }

    /** Force-open the event for testing without going through update(). */
    public void forceOpenEvent(boolean thunderstorm) {
        openEvent(thunderstorm);
    }

    /** Force-set the fireworks launched state (for testing). */
    public void forceFireworksLaunched() {
        fireworksLaunched = true;
        fireworksLaunchedCount = FIREWORK_COUNT;
    }

    /** Force-set countdownStarted and pickpocketWindowOpen (for testing). */
    public void forceCountdownStarted() {
        countdownStarted = true;
        pickpocketWindowOpen = true;
    }

    /** Force-set Sharon's purse as collected (for testing). */
    public void forceCollectSharonPurse(Inventory playerInventory) {
        collectSharonPurse(playerInventory);
    }
}
