package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1433: Northfield Easter Weekend — the Egg Hunt, the Hot Cross Bun Run &amp;
 * the Charity Motorbike Parade.
 *
 * <p>Active on days 91–94 (Good Friday through Easter Monday,
 * {@code dayOfYear % 365 == 91..94}).
 *
 * <h3>Mechanic 1 — Good Friday (day 91, 08:00–10:00): Greggs Hot Cross Bun Queue</h3>
 * 12 {@link Material#HOT_CROSS_BUN}s available. {@link NPCType#PUBLIC} and
 * {@link NPCType#PENSIONER} NPCs queue from 07:45. Player joins queue via E; pushing
 * in triggers {@link CrimeType#QUEUE_JUMP} crime if witnessed. Greggs closes
 * 10:00–12:00. Church runs extended Good Friday service 11:00–12:30 (doubled
 * congregation). Achievement: {@link AchievementType#HOT_CROSS_HERO}.
 *
 * <h3>Mechanic 2 — Easter Saturday (day 92, 10:00–13:00): Council Egg Hunt</h3>
 * {@link NPCType#EASTER_EGG_HUNT_WARDEN} Brenda hides 15 {@link PropType#EASTER_EGG_PROP}s.
 * {@link NPCType#SCHOOL_KID} NPCs compete. Player presses E within 1 block to collect
 * {@link Material#FOIL_EASTER_EGG}s. {@link NPCType#EASTER_BUNNY_NPC} (council volunteer
 * in costume) is pickpocketable for {@link Material#CHOCOLATE_EGG} ×3
 * (NeighbourhoodWatch anger +10, Notoriety +4). Achievement:
 * {@link AchievementType#EASTER_EGG_BARON} (5+ eggs).
 *
 * <h3>Mechanic 3 — Easter Sunday (day 93, 11:00–14:00): Easter Egg Run Motorbike Parade</h3>
 * 12 {@link NPCType#BIKER_NPC} NPCs parade from industrial estate along HIGH_STREET to
 * park. {@link PropType#CHARITY_BUCKET_PROP} at park entrance; player can donate
 * (StreetRep +3, BIKER respect +5) or steal contents (Notoriety +8, Wanted +1, all
 * bikers HOSTILE). Each parked {@link PropType#MOTORBIKE_PROP} has stealable
 * {@link Material#BIKER_JACKET} (STEALTH ≥ 2, E hold 3 s). Achievements:
 * {@link AchievementType#CHARITY_SKIMMER}, {@link AchievementType#BIKER_BLAG}.
 * THUNDERSTORM cancels the egg hunt (day 92) but not the parade.
 *
 * <h3>Mechanic 4 — Easter Monday (day 94, all day): Everything Is Shut</h3>
 * All shops closed (CornerShop, PoundShop, Iceland, Supermarket, OffLicence,
 * CharityShop). Kebab van and Wetherspoons (from 12:00) remain open. Attempting 5+
 * closed shops unlocks {@link AchievementType#NOTHING_IS_OPEN}. Off-licence has
 * breakable {@code CLOSED_SIGN_PROP} (PETTY_THEFT, Notoriety +3). HUNGRY need rate
 * is doubled for NPCs on this day.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#HOT_CROSS_HERO} — obtain a hot cross bun on Good Friday.</li>
 *   <li>{@link AchievementType#EASTER_EGG_BARON} — collect 5+ foil Easter eggs.</li>
 *   <li>{@link AchievementType#CHARITY_SKIMMER} — steal the charity bucket.</li>
 *   <li>{@link AchievementType#BIKER_BLAG} — steal a biker jacket from a parked motorbike.</li>
 *   <li>{@link AchievementType#NOTHING_IS_OPEN} — attempt 5+ closed shops on Easter Monday.</li>
 * </ul>
 */
public class EasterSystem {

    // ─────────────────────────────────────────────────────────────────────────
    // Day-of-year constants (days 91–94: Good Friday through Easter Monday)
    // ─────────────────────────────────────────────────────────────────────────

    /** Day-of-year for Good Friday (1 April in a typical year). */
    public static final int GOOD_FRIDAY_DAY = 91;
    /** Day-of-year for Easter Saturday. */
    public static final int EASTER_SATURDAY_DAY = 92;
    /** Day-of-year for Easter Sunday. */
    public static final int EASTER_SUNDAY_DAY = 93;
    /** Day-of-year for Easter Monday. */
    public static final int EASTER_MONDAY_DAY = 94;

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 1 — Good Friday hot cross bun queue constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Hour at which PUBLIC/PENSIONER NPCs begin forming the Greggs queue. */
    public static final float QUEUE_FORM_HOUR = 7.75f;  // 07:45
    /** Hour at which the Greggs queue opens and buns go on sale. */
    public static final float BUN_QUEUE_OPEN_HOUR = 8.0f;
    /** Hour at which the Greggs bun supply is exhausted / queue closes. */
    public static final float BUN_QUEUE_CLOSE_HOUR = 10.0f;
    /** Hour at which Greggs closes for the rest of the morning. */
    public static final float GREGGS_REOPEN_HOUR = 12.0f;
    /** Total hot cross buns available on Good Friday morning. */
    public static final int HOT_CROSS_BUN_STOCK = 12;
    /** Number of PUBLIC/PENSIONER NPCs that queue up for buns. */
    public static final int QUEUE_NPC_COUNT = 8;

    /** Hour at which the extended Good Friday church service begins (11:00). */
    public static final float GOOD_FRIDAY_SERVICE_START = 11.0f;
    /** Hour at which the extended Good Friday church service ends (12:30). */
    public static final float GOOD_FRIDAY_SERVICE_END = 12.5f;
    /**
     * Multiplier applied to normal Sunday congregation count for the extended
     * Good Friday service (doubled congregation).
     */
    public static final int GOOD_FRIDAY_CONGREGATION_MULTIPLIER = 2;

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 2 — Easter Saturday egg hunt constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Hour at which the council egg hunt opens. */
    public static final float EGG_HUNT_OPEN_HOUR = 10.0f;
    /** Hour at which the council egg hunt closes. */
    public static final float EGG_HUNT_CLOSE_HOUR = 13.0f;
    /** Number of EASTER_EGG_PROPs hidden by Brenda. */
    public static final int EGG_COUNT = 15;
    /** Number of foil eggs required to unlock EASTER_EGG_BARON. */
    public static final int EASTER_EGG_BARON_TARGET = 5;
    /** CHOCOLATE_EGGs yielded per successful pickpocket of EASTER_BUNNY_NPC. */
    public static final int BUNNY_PICKPOCKET_YIELD = 3;
    /** NeighbourhoodWatch anger increase when Easter Bunny is pickpocketed. */
    public static final int BUNNY_PICKPOCKET_WATCH_ANGER = 10;
    /** Notoriety gain when Easter Bunny is pickpocketed. */
    public static final int BUNNY_PICKPOCKET_NOTORIETY = 4;

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 3 — Easter Sunday motorbike parade constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Hour at which the charity motorbike parade begins. */
    public static final float PARADE_START_HOUR = 11.0f;
    /** Hour at which the charity motorbike parade ends. */
    public static final float PARADE_END_HOUR = 14.0f;
    /** Number of BIKER_NPC NPCs participating in the parade. */
    public static final int BIKER_COUNT = 12;
    /** Notoriety gain for stealing the charity bucket. */
    public static final int CHARITY_THEFT_NOTORIETY = 8;
    /** Wanted stars gained for stealing the charity bucket. */
    public static final int CHARITY_THEFT_WANTED = 1;
    /** StreetRep gained for donating to the charity bucket. */
    public static final int CHARITY_DONATION_STREET_REP = 3;
    /** BIKER faction respect gained for donating to the charity bucket. */
    public static final int CHARITY_DONATION_BIKER_RESPECT = 5;
    /** Minimum STEALTH skill level required to steal a biker jacket. */
    public static final int JACKET_STEAL_STEALTH_MIN = 2;
    /** Hold-E duration in seconds required to steal a biker jacket. */
    public static final float JACKET_STEAL_HOLD_SECONDS = 3.0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 4 — Easter Monday closed shops constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Number of closed-shop attempts that unlocks NOTHING_IS_OPEN. */
    public static final int NOTHING_IS_OPEN_TARGET = 5;
    /** Notoriety gain for breaking the off-licence CLOSED_SIGN_PROP. */
    public static final int CLOSED_SIGN_NOTORIETY = 3;
    /** Hour from which Wetherspoons opens on Easter Monday. */
    public static final float WETHERSPOONS_EASTER_OPEN_HOUR = 12.0f;
    /** Hungry need rate multiplier for NPCs on Easter Monday (shops shut). */
    public static final float EASTER_MONDAY_HUNGRY_MULTIPLIER = 2.0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Result enums
    // ─────────────────────────────────────────────────────────────────────────

    /** Outcome of joining the Greggs hot cross bun queue. */
    public enum QueueResult {
        /** Player joined the back of the queue successfully. */
        JOINED_QUEUE,
        /** Player received a hot cross bun (was at the front of the queue). */
        RECEIVED_BUN,
        /** Player pushed in; crime recorded if witness was present. */
        PUSHED_IN,
        /** Queue is not yet open (before 08:00). */
        NOT_OPEN_YET,
        /** Queue has closed; buns are sold out. */
        SOLD_OUT,
        /** Greggs is closed for restocking (10:00–12:00). */
        GREGGS_CLOSED
    }

    /** Outcome of collecting an Easter egg from the park. */
    public enum EggCollectResult {
        /** Player collected a foil Easter egg. */
        EGG_COLLECTED,
        /** No egg prop is within 1 block of the player. */
        NO_EGG_NEARBY,
        /** The egg hunt is not active (wrong day or wrong hour). */
        HUNT_NOT_ACTIVE,
        /** The egg hunt was cancelled due to THUNDERSTORM. */
        HUNT_CANCELLED
    }

    /** Outcome of pickpocketing the Easter Bunny NPC. */
    public enum BunnyPickpocketResult {
        /** Player successfully pickpocketed the Easter Bunny. */
        SUCCESS,
        /** Easter Bunny is not present (wrong day/hour). */
        BUNNY_NOT_PRESENT,
        /** Player has no pickpocket skill. */
        NO_SKILL
    }

    /** Outcome of interacting with the charity bucket. */
    public enum CharityBucketResult {
        /** Player donated to the charity bucket. */
        DONATED,
        /** Player stole the charity bucket contents. */
        STOLEN,
        /** Charity bucket is not present (wrong day/hour). */
        BUCKET_NOT_PRESENT
    }

    /** Outcome of attempting to steal a biker jacket from a parked motorbike. */
    public enum JacketStealResult {
        /** Jacket steal initiated (still holding E — not complete yet). */
        HOLD_IN_PROGRESS,
        /** Jacket successfully stolen after 3-second hold. */
        STOLEN,
        /** Player's STEALTH skill is too low. */
        INSUFFICIENT_STEALTH,
        /** No motorbike prop in range. */
        NO_MOTORBIKE_NEARBY,
        /** Parade not active. */
        PARADE_NOT_ACTIVE
    }

    /** Outcome of attempting to enter a closed shop on Easter Monday. */
    public enum ShopAccessResult {
        /** Shop is open as normal. */
        OPEN,
        /** Shop is closed for Easter Monday. */
        CLOSED_EASTER
    }

    /** Internal event types fired by {@link #update}. */
    public enum EventType {
        /** Good Friday queue opened. */
        QUEUE_OPENED,
        /** Good Friday queue sold out. */
        QUEUE_SOLD_OUT,
        /** Egg hunt opened (Saturday). */
        EGG_HUNT_OPENED,
        /** Egg hunt closed (Saturday). */
        EGG_HUNT_CLOSED,
        /** Egg hunt cancelled due to THUNDERSTORM. */
        EGG_HUNT_THUNDERSTORM_CANCELLED,
        /** Parade started (Sunday). */
        PARADE_STARTED,
        /** Parade ended (Sunday). */
        PARADE_ENDED,
        /** Easter Monday — shops all closed. */
        MONDAY_SHOPS_CLOSED,
        /** No event currently active. */
        NONE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session state
    // ─────────────────────────────────────────────────────────────────────────

    /** Remaining hot cross buns in the Greggs stock. */
    private int bunsRemaining;
    /** Whether the Greggs queue is currently open (day 91, 08:00–10:00). */
    private boolean queueOpen;
    /** Whether the Greggs queue has been opened at least once this session. */
    private boolean queueOpenedThisDay;
    /** Whether the queue has sold out. */
    private boolean queueSoldOut;

    /** Whether the egg hunt has been opened this Saturday. */
    private boolean eggHuntOpen;
    /** Whether the egg hunt was cancelled due to THUNDERSTORM. */
    private boolean eggHuntCancelled;
    /** Whether the egg hunt has been opened at least once this session. */
    private boolean eggHuntOpenedThisDay;
    /** Whether the egg hunt has been closed at least once this session. */
    private boolean eggHuntClosedThisDay;
    /** Number of foil Easter eggs collected by the player this session. */
    private int foilEggsCollected;
    /** Number of EASTER_EGG_PROPs remaining in the park. */
    private int eggsRemaining;

    /** Whether the parade is currently active (day 93, 11:00–14:00). */
    private boolean paradeActive;
    /** Whether the parade has started at least once this session. */
    private boolean paradeStartedThisDay;
    /** Whether the parade has ended at least once this session. */
    private boolean paradeEndedThisDay;
    /** Whether the charity bucket has been interacted with. */
    private boolean charityBucketInteracted;
    /** Whether the charity bucket was stolen. */
    private boolean charityBucketStolen;
    /** Number of biker jackets stolen this session. */
    private int bikerJacketsStolen;
    /** Current hold-E elapsed time in seconds (jacket steal). */
    private float jacketStealHoldTime;

    /** Whether Easter Monday shop closures have been announced this session. */
    private boolean mondayShopsAnnouncedThisDay;
    /** Number of closed shop attempts made by the player on Easter Monday. */
    private int closedShopAttempts;
    /** Whether the off-licence closed sign has been broken. */
    private boolean closedSignBroken;

    // Achievement guard flags
    private boolean hotCrossHeroAwarded;
    private boolean easterEggBaronAwarded;
    private boolean charitySkimmerAwarded;
    private boolean bikerBlagAwarded;
    private boolean nothingIsOpenAwarded;

    // List of spawned parade bikers (for hostility management)
    private final List<NPC> paradeNpcs = new ArrayList<>();
    // List of queue NPCs
    private final List<NPC> queueNpcs = new ArrayList<>();

    // Optional wired systems
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private NeighbourhoodWatchSystem neighbourhoodWatchSystem;

    private final Random random;

    public EasterSystem() {
        this.random = new Random();
        resetSessionState();
    }

    public EasterSystem(Random random) {
        this.random = random;
        resetSessionState();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Optional system wiring
    // ─────────────────────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem s) { this.notorietySystem = s; }
    public void setWantedSystem(WantedSystem s)       { this.wantedSystem = s; }
    public void setCriminalRecord(CriminalRecord s)   { this.criminalRecord = s; }
    public void setRumourNetwork(RumourNetwork s)     { this.rumourNetwork = s; }
    public void setNeighbourhoodWatchSystem(NeighbourhoodWatchSystem s) {
        this.neighbourhoodWatchSystem = s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Day-of-year helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if the given day-of-year is Good Friday (day 91). */
    public boolean isGoodFriday(int dayOfYear) {
        return dayOfYear % 365 == GOOD_FRIDAY_DAY;
    }

    /** Returns true if the given day-of-year is Easter Saturday (day 92). */
    public boolean isEasterSaturday(int dayOfYear) {
        return dayOfYear % 365 == EASTER_SATURDAY_DAY;
    }

    /** Returns true if the given day-of-year is Easter Sunday (day 93). */
    public boolean isEasterSunday(int dayOfYear) {
        return dayOfYear % 365 == EASTER_SUNDAY_DAY;
    }

    /** Returns true if the given day-of-year is Easter Monday (day 94). */
    public boolean isEasterMonday(int dayOfYear) {
        return dayOfYear % 365 == EASTER_MONDAY_DAY;
    }

    /** Returns true if the given day-of-year falls within Easter weekend (days 91–94). */
    public boolean isEasterWeekend(int dayOfYear) {
        int d = dayOfYear % 365;
        return d >= GOOD_FRIDAY_DAY && d <= EASTER_MONDAY_DAY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Window helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** True if the Greggs bun queue is open (day 91, 08:00–10:00). */
    public boolean isBunQueueHour(float hour) {
        return hour >= BUN_QUEUE_OPEN_HOUR && hour < BUN_QUEUE_CLOSE_HOUR;
    }

    /** True if Greggs is closed for restocking (day 91, 10:00–12:00). */
    public boolean isGreggsMorningClosed(float hour) {
        return hour >= BUN_QUEUE_CLOSE_HOUR && hour < GREGGS_REOPEN_HOUR;
    }

    /** True if the Good Friday extended church service is active (11:00–12:30). */
    public boolean isGoodFridayServiceHour(float hour) {
        return hour >= GOOD_FRIDAY_SERVICE_START && hour < GOOD_FRIDAY_SERVICE_END;
    }

    /** True if the Easter Saturday egg hunt is active (10:00–13:00). */
    public boolean isEggHuntHour(float hour) {
        return hour >= EGG_HUNT_OPEN_HOUR && hour < EGG_HUNT_CLOSE_HOUR;
    }

    /** True if the Easter Sunday motorbike parade is active (11:00–14:00). */
    public boolean isParadeHour(float hour) {
        return hour >= PARADE_START_HOUR && hour < PARADE_END_HOUR;
    }

    /**
     * Returns the Good Friday congregation count for ChurchSystem integration.
     * The extended service runs 11:00–12:30 with a doubled congregation.
     *
     * @param baseCongregantCount the count ChurchSystem would normally return
     * @param dayOfYear           current day of year
     * @param hour                current hour
     * @return adjusted congregant count
     */
    public int getGoodFridayCongregantCount(int baseCongregantCount, int dayOfYear, float hour) {
        if (isGoodFriday(dayOfYear) && isGoodFridayServiceHour(hour)) {
            return baseCongregantCount * GOOD_FRIDAY_CONGREGATION_MULTIPLIER;
        }
        return baseCongregantCount;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Easter Monday shop closure helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the named shop is closed on Easter Monday.
     * The kebab van and Wetherspoons (from 12:00) remain open.
     *
     * @param shopName   canonical shop name (e.g. "CornerShop", "OffLicence")
     * @param dayOfYear  current day of year
     * @param hour       current hour
     * @return true if the shop is closed
     */
    public boolean isShopClosedEasterMonday(String shopName, int dayOfYear, float hour) {
        if (!isEasterMonday(dayOfYear)) return false;
        switch (shopName) {
            case "KebabVan":
                return false;
            case "Wetherspoons":
                return hour < WETHERSPOONS_EASTER_OPEN_HOUR;
            case "CornerShop":
            case "PoundShop":
            case "Iceland":
            case "Supermarket":
            case "OffLicence":
            case "CharityShop":
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the HUNGRY need rate multiplier for NPCs on Easter Monday.
     * Returns {@link #EASTER_MONDAY_HUNGRY_MULTIPLIER} when active, otherwise 1.0f.
     *
     * @param dayOfYear current day of year
     * @return multiplier for HUNGRY need accumulation
     */
    public float getEasterMondayHungryMultiplier(int dayOfYear) {
        return isEasterMonday(dayOfYear) ? EASTER_MONDAY_HUNGRY_MULTIPLIER : 1.0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update loop — fires timed events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Per-frame update. Call once per game tick with the current day-of-year and hour.
     * Fires queued open/close events, spawns NPCs, and seeds rumours as appropriate.
     *
     * @param dayOfYear   current day-of-year
     * @param currentHour current game hour (0–24)
     * @param isThunderstorm whether weather is currently THUNDERSTORM
     * @param witnessNpc  a nearby NPC for rumour seeding (may be null)
     * @param callback    achievement callback
     * @return the event that fired this tick, or {@link EventType#NONE}
     */
    public EventType update(int dayOfYear, float currentHour, boolean isThunderstorm,
                            NPC witnessNpc, AchievementCallback callback) {

        // ── Good Friday ──────────────────────────────────────────────────────
        if (isGoodFriday(dayOfYear)) {
            if (!queueOpenedThisDay && isBunQueueHour(currentHour)) {
                queueOpenedThisDay = true;
                queueOpen = true;
                bunsRemaining = HOT_CROSS_BUN_STOCK;
                spawnQueueNpcs();
                return EventType.QUEUE_OPENED;
            }
            if (queueOpenedThisDay && !queueSoldOut && currentHour >= BUN_QUEUE_CLOSE_HOUR) {
                queueOpen = false;
                queueSoldOut = true;
                queueNpcs.clear();
                return EventType.QUEUE_SOLD_OUT;
            }
        }

        // ── Easter Saturday ──────────────────────────────────────────────────
        if (isEasterSaturday(dayOfYear)) {
            if (!eggHuntOpenedThisDay && isEggHuntHour(currentHour)) {
                eggHuntOpenedThisDay = true;
                if (isThunderstorm) {
                    eggHuntCancelled = true;
                    if (witnessNpc != null && rumourNetwork != null) {
                        rumourNetwork.addRumour(witnessNpc,
                            new Rumour(RumourType.EASTER_EGG_HIDDEN,
                                "Heard the egg hunt got cancelled — too rainy."));
                    }
                    return EventType.EGG_HUNT_THUNDERSTORM_CANCELLED;
                }
                eggHuntOpen = true;
                eggsRemaining = EGG_COUNT;
                if (witnessNpc != null && rumourNetwork != null) {
                    rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.EASTER_EGG_HIDDEN,
                            "I heard Brenda hid some of the Easter eggs near the pond — good spot."));
                }
                return EventType.EGG_HUNT_OPENED;
            }
            if (eggHuntOpen && !eggHuntClosedThisDay && currentHour >= EGG_HUNT_CLOSE_HOUR) {
                eggHuntClosedThisDay = true;
                eggHuntOpen = false;
                return EventType.EGG_HUNT_CLOSED;
            }
        }

        // ── Easter Sunday ────────────────────────────────────────────────────
        if (isEasterSunday(dayOfYear)) {
            if (!paradeStartedThisDay && isParadeHour(currentHour)) {
                paradeStartedThisDay = true;
                paradeActive = true;
                spawnParadeNpcs();
                if (witnessNpc != null && rumourNetwork != null) {
                    rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.BIKER_PARADE,
                            "There's a load of bikers coming through Northfield Sunday for charity."));
                }
                return EventType.PARADE_STARTED;
            }
            if (paradeActive && !paradeEndedThisDay && currentHour >= PARADE_END_HOUR) {
                paradeEndedThisDay = true;
                paradeActive = false;
                paradeNpcs.clear();
                return EventType.PARADE_ENDED;
            }
        }

        // ── Easter Monday ────────────────────────────────────────────────────
        if (isEasterMonday(dayOfYear) && !mondayShopsAnnouncedThisDay) {
            mondayShopsAnnouncedThisDay = true;
            return EventType.MONDAY_SHOPS_CLOSED;
        }

        return EventType.NONE;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 1 — Good Friday queue interactions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player attempts to join (or push into) the Greggs hot cross bun queue.
     *
     * @param inventory     player's inventory
     * @param currentHour   current game hour
     * @param dayOfYear     current day of year
     * @param pushIn        true if the player is pushing in rather than joining back
     * @param witnessNpc    nearby NPC witness (for queue-jump crime detection); may be null
     * @param callback      achievement callback
     * @return the result of the queue interaction
     */
    public QueueResult joinBunQueue(Inventory inventory, float currentHour, int dayOfYear,
                                    boolean pushIn, NPC witnessNpc, AchievementCallback callback) {
        if (!isGoodFriday(dayOfYear)) {
            return QueueResult.NOT_OPEN_YET;
        }
        if (currentHour < BUN_QUEUE_OPEN_HOUR) {
            return QueueResult.NOT_OPEN_YET;
        }
        if (isGreggsMorningClosed(currentHour) || currentHour >= GREGGS_REOPEN_HOUR) {
            return QueueResult.GREGGS_CLOSED;
        }
        if (bunsRemaining <= 0 || queueSoldOut) {
            return QueueResult.SOLD_OUT;
        }

        if (pushIn) {
            if (witnessNpc != null) {
                if (criminalRecord != null) {
                    criminalRecord.record(CrimeType.DISORDERLY_CONDUCT);
                }
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(2, callback);
                }
            }
            // Still get the bun on a push-in
            bunsRemaining--;
            inventory.addItem(Material.HOT_CROSS_BUN, 1);
            checkHotCrossHero(callback);
            if (bunsRemaining <= 0) {
                queueSoldOut = true;
                queueOpen = false;
                queueNpcs.clear();
            }
            return QueueResult.PUSHED_IN;
        }

        // Normal queue join — only award bun if player is at front (simplified: always award one)
        bunsRemaining--;
        inventory.addItem(Material.HOT_CROSS_BUN, 1);
        checkHotCrossHero(callback);
        if (bunsRemaining <= 0) {
            queueSoldOut = true;
            queueOpen = false;
            queueNpcs.clear();
        }
        return QueueResult.RECEIVED_BUN;
    }

    private void checkHotCrossHero(AchievementCallback callback) {
        if (!hotCrossHeroAwarded && callback != null) {
            hotCrossHeroAwarded = true;
            callback.award(AchievementType.HOT_CROSS_HERO);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 2 — Easter Saturday egg hunt interactions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player presses E within 1 block to collect a foil Easter egg.
     *
     * @param inventory   player's inventory
     * @param currentHour current game hour
     * @param dayOfYear   current day of year
     * @param eggNearby   true if an EASTER_EGG_PROP is within 1 block of the player
     * @param callback    achievement callback
     * @return the result of the egg collect attempt
     */
    public EggCollectResult collectEgg(Inventory inventory, float currentHour, int dayOfYear,
                                       boolean eggNearby, AchievementCallback callback) {
        if (!isEasterSaturday(dayOfYear) || !isEggHuntHour(currentHour)) {
            return EggCollectResult.HUNT_NOT_ACTIVE;
        }
        if (eggHuntCancelled) {
            return EggCollectResult.HUNT_CANCELLED;
        }
        if (!eggNearby || eggsRemaining <= 0) {
            return EggCollectResult.NO_EGG_NEARBY;
        }
        eggsRemaining--;
        foilEggsCollected++;
        inventory.addItem(Material.FOIL_EASTER_EGG, 1);
        checkEasterEggBaron(callback);
        return EggCollectResult.EGG_COLLECTED;
    }

    private void checkEasterEggBaron(AchievementCallback callback) {
        if (!easterEggBaronAwarded && foilEggsCollected >= EASTER_EGG_BARON_TARGET && callback != null) {
            easterEggBaronAwarded = true;
            callback.award(AchievementType.EASTER_EGG_BARON);
        }
    }

    /**
     * Player pickpockets the Easter Bunny NPC.
     * Yields {@link #BUNNY_PICKPOCKET_YIELD} CHOCOLATE_EGGs.
     * Triggers NeighbourhoodWatch anger +10 and Notoriety +4.
     *
     * @param inventory          player's inventory
     * @param dayOfYear          current day of year
     * @param currentHour        current game hour
     * @param bunnyNpc           the EASTER_BUNNY_NPC (may be null if not present)
     * @param playerHasSkill     true if player has the pickpocket skill
     * @param callback           achievement callback
     * @return result of the pickpocket attempt
     */
    public BunnyPickpocketResult pickpocketEasterBunny(Inventory inventory, int dayOfYear,
                                                        float currentHour, NPC bunnyNpc,
                                                        boolean playerHasSkill,
                                                        AchievementCallback callback) {
        if (!isEasterSaturday(dayOfYear) || !isEggHuntHour(currentHour) || bunnyNpc == null) {
            return BunnyPickpocketResult.BUNNY_NOT_PRESENT;
        }
        if (!playerHasSkill) {
            return BunnyPickpocketResult.NO_SKILL;
        }
        inventory.addItem(Material.CHOCOLATE_EGG, BUNNY_PICKPOCKET_YIELD);
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(BUNNY_PICKPOCKET_NOTORIETY, callback);
        }
        if (neighbourhoodWatchSystem != null) {
            neighbourhoodWatchSystem.addAnger(BUNNY_PICKPOCKET_WATCH_ANGER);
        }
        return BunnyPickpocketResult.SUCCESS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 3 — Easter Sunday parade interactions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player interacts with the charity bucket (donate or steal).
     *
     * @param inventory   player's inventory
     * @param dayOfYear   current day of year
     * @param currentHour current game hour
     * @param steal       true if the player is stealing; false to donate
     * @param callback    achievement callback
     * @return result of the charity bucket interaction
     */
    public CharityBucketResult interactWithCharityBucket(Inventory inventory, int dayOfYear,
                                                          float currentHour, boolean steal,
                                                          AchievementCallback callback) {
        if (!isEasterSunday(dayOfYear) || !isParadeHour(currentHour) || charityBucketInteracted) {
            return CharityBucketResult.BUCKET_NOT_PRESENT;
        }
        charityBucketInteracted = true;
        if (steal) {
            charityBucketStolen = true;
            inventory.addItem(Material.CHARITY_BUCKET_EASTER, 1);
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(CHARITY_THEFT_NOTORIETY, callback);
            }
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(CHARITY_THEFT_WANTED);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.THEFT);
            }
            // Make all parade bikers hostile
            for (NPC biker : paradeNpcs) {
                biker.setState(NPCState.HOSTILE_TO_PLAYER);
            }
            if (!charitySkimmerAwarded && callback != null) {
                charitySkimmerAwarded = true;
                callback.award(AchievementType.CHARITY_SKIMMER);
            }
            return CharityBucketResult.STOLEN;
        } else {
            // Donation — grant voucher as token of appreciation
            inventory.addItem(Material.BIKERS_VOUCHER, 1);
            return CharityBucketResult.DONATED;
        }
    }

    /**
     * Player holds E on a parked MOTORBIKE_PROP to steal a biker jacket.
     * Requires STEALTH ≥ 2 and a 3-second hold.
     *
     * @param inventory      player's inventory
     * @param dayOfYear      current day of year
     * @param currentHour    current game hour
     * @param motorbikeNearby true if a MOTORBIKE_PROP is within range
     * @param playerStealth  player's current STEALTH skill level
     * @param holdDelta      time in seconds the player has been holding E this tick
     * @param callback       achievement callback
     * @return result of the jacket steal attempt
     */
    public JacketStealResult stealBikerJacket(Inventory inventory, int dayOfYear,
                                               float currentHour, boolean motorbikeNearby,
                                               int playerStealth, float holdDelta,
                                               AchievementCallback callback) {
        if (!isEasterSunday(dayOfYear) || !isParadeHour(currentHour)) {
            return JacketStealResult.PARADE_NOT_ACTIVE;
        }
        if (!motorbikeNearby) {
            return JacketStealResult.NO_MOTORBIKE_NEARBY;
        }
        if (playerStealth < JACKET_STEAL_STEALTH_MIN) {
            return JacketStealResult.INSUFFICIENT_STEALTH;
        }
        jacketStealHoldTime += holdDelta;
        if (jacketStealHoldTime < JACKET_STEAL_HOLD_SECONDS) {
            return JacketStealResult.HOLD_IN_PROGRESS;
        }
        // Hold complete — steal the jacket
        jacketStealHoldTime = 0f;
        bikerJacketsStolen++;
        inventory.addItem(Material.BIKER_JACKET, 1);
        if (!bikerBlagAwarded && callback != null) {
            bikerBlagAwarded = true;
            callback.award(AchievementType.BIKER_BLAG);
        }
        return JacketStealResult.STOLEN;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mechanic 4 — Easter Monday shop closures
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Player attempts to enter a shop on Easter Monday.
     * Records the failed attempt and may unlock {@link AchievementType#NOTHING_IS_OPEN}.
     *
     * @param shopName  canonical shop name (e.g. "CornerShop")
     * @param dayOfYear current day of year
     * @param hour      current game hour
     * @param callback  achievement callback
     * @return whether the shop is open or closed
     */
    public ShopAccessResult tryEnterShop(String shopName, int dayOfYear, float hour,
                                          AchievementCallback callback) {
        if (isShopClosedEasterMonday(shopName, dayOfYear, hour)) {
            closedShopAttempts++;
            checkNothingIsOpen(callback);
            return ShopAccessResult.CLOSED_EASTER;
        }
        return ShopAccessResult.OPEN;
    }

    /**
     * Player breaks the off-licence CLOSED_SIGN_PROP on Easter Monday.
     * Triggers PETTY_THEFT crime and Notoriety +3.
     *
     * @param dayOfYear current day of year
     * @param callback  achievement callback
     * @return true if the sign was breakable (Easter Monday, not yet broken)
     */
    public boolean breakClosedSign(int dayOfYear, AchievementCallback callback) {
        if (!isEasterMonday(dayOfYear) || closedSignBroken) {
            return false;
        }
        closedSignBroken = true;
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PETTY_THEFT);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CLOSED_SIGN_NOTORIETY, null);
        }
        return true;
    }

    private void checkNothingIsOpen(AchievementCallback callback) {
        if (!nothingIsOpenAwarded && closedShopAttempts >= NOTHING_IS_OPEN_TARGET && callback != null) {
            nothingIsOpenAwarded = true;
            callback.award(AchievementType.NOTHING_IS_OPEN);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NPC spawning helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void spawnQueueNpcs() {
        queueNpcs.clear();
        for (int i = 0; i < QUEUE_NPC_COUNT; i++) {
            NPCType type = (i % 2 == 0) ? NPCType.PUBLIC : NPCType.PENSIONER;
            queueNpcs.add(new NPC(type, 0f, 0f, 0f));
        }
    }

    private void spawnParadeNpcs() {
        paradeNpcs.clear();
        for (int i = 0; i < BIKER_COUNT; i++) {
            paradeNpcs.add(new NPC(NPCType.BIKER_NPC, 0f, 0f, 0f));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State accessors
    // ─────────────────────────────────────────────────────────────────────────

    public int getBunsRemaining()        { return bunsRemaining; }
    public boolean isQueueOpen()         { return queueOpen; }
    public boolean isQueueSoldOut()      { return queueSoldOut; }
    public boolean isEggHuntOpen()       { return eggHuntOpen; }
    public boolean isEggHuntCancelled()  { return eggHuntCancelled; }
    public int getFoilEggsCollected()    { return foilEggsCollected; }
    public int getEggsRemaining()        { return eggsRemaining; }
    public boolean isParadeActive()      { return paradeActive; }
    public boolean isCharityBucketStolen() { return charityBucketStolen; }
    public int getBikerJacketsStolen()   { return bikerJacketsStolen; }
    public int getClosedShopAttempts()   { return closedShopAttempts; }
    public boolean isClosedSignBroken()  { return closedSignBroken; }
    public List<NPC> getParadeNpcs()     { return paradeNpcs; }
    public List<NPC> getQueueNpcs()      { return queueNpcs; }

    // Achievement flags (for testing)
    public boolean isHotCrossHeroAwarded()    { return hotCrossHeroAwarded; }
    public boolean isEasterEggBaronAwarded()  { return easterEggBaronAwarded; }
    public boolean isCharitySkimmerAwarded()  { return charitySkimmerAwarded; }
    public boolean isBikerBlagAwarded()       { return bikerBlagAwarded; }
    public boolean isNothingIsOpenAwarded()   { return nothingIsOpenAwarded; }

    // ─────────────────────────────────────────────────────────────────────────
    // ForTesting setters
    // ─────────────────────────────────────────────────────────────────────────

    public void setBunsRemainingForTesting(int n)        { this.bunsRemaining = n; }
    public void setQueueOpenForTesting(boolean b)        { this.queueOpen = b; this.queueOpenedThisDay = b; }
    public void setEggHuntOpenForTesting(boolean b)      { this.eggHuntOpen = b; this.eggHuntOpenedThisDay = b; }
    public void setEggsRemainingForTesting(int n)        { this.eggsRemaining = n; }
    public void setFoilEggsCollectedForTesting(int n)    { this.foilEggsCollected = n; }
    public void setParadeActiveForTesting(boolean b)     { this.paradeActive = b; this.paradeStartedThisDay = b; }
    public void setParadeNpcsForTesting(List<NPC> npcs)  { this.paradeNpcs.clear(); this.paradeNpcs.addAll(npcs); }
    public void setClosedShopAttemptsForTesting(int n)   { this.closedShopAttempts = n; }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void resetSessionState() {
        bunsRemaining = 0;
        queueOpen = false;
        queueOpenedThisDay = false;
        queueSoldOut = false;
        eggHuntOpen = false;
        eggHuntCancelled = false;
        eggHuntOpenedThisDay = false;
        eggHuntClosedThisDay = false;
        foilEggsCollected = 0;
        eggsRemaining = 0;
        paradeActive = false;
        paradeStartedThisDay = false;
        paradeEndedThisDay = false;
        charityBucketInteracted = false;
        charityBucketStolen = false;
        bikerJacketsStolen = 0;
        jacketStealHoldTime = 0f;
        mondayShopsAnnouncedThisDay = false;
        closedShopAttempts = 0;
        closedSignBroken = false;
        hotCrossHeroAwarded = false;
        easterEggBaronAwarded = false;
        charitySkimmerAwarded = false;
        bikerBlagAwarded = false;
        nothingIsOpenAwarded = false;
        paradeNpcs.clear();
        queueNpcs.clear();
    }
}
