package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1065: Fix My Phone — Tariq's Repair Shop, Phone Cloning &amp; the IMEI Economy.
 *
 * <h3>Services (press E on counter)</h3>
 * <ul>
 *   <li>Screen repair: {@link Material#BROKEN_PHONE} → {@link Material#STOLEN_PHONE} for 3 COIN</li>
 *   <li>IMEI wipe: {@link Material#STOLEN_PHONE} → {@link Material#WIPED_PHONE} for 5 COIN
 *       (+3 Notoriety; 40% police investigate if {@code POLICE_OFFICER} NPC within
 *       {@link #POLICE_NEARBY_DISTANCE} blocks)</li>
 *   <li>Back-room cloning: {@link Material#STOLEN_PHONE} + {@link Material#SIM_CARD} + 2 COIN
 *       → {@link Material#CLONED_PHONE} (adds {@link CrimeType#THEFT}, +3 Notoriety;
 *       unlocked at Marchetti Respect ≥ 40 or after quest)</li>
 *   <li>Screen protector upsell: 1 COIN → adds {@link Material#SCREEN_PROTECTOR}</li>
 * </ul>
 *
 * <h3>Atmospheric Events (one per 30 in-game minutes while open)</h3>
 * <ol>
 *   <li>{@link EventType#AWKWARD_CUSTOMER} — PUBLIC NPC demands phone back; Tariq deflects</li>
 *   <li>{@link EventType#POLICE_KNOCK} — POLICE_OFFICER enters; player holding STOLEN_PHONE
 *       has 3 seconds to move 6 blocks away</li>
 *   <li>{@link EventType#SMASHED_SCREEN} — YOUTH NPC drops phone; BROKEN_PHONE spawns</li>
 *   <li>{@link EventType#TARIQ_RUMOUR} — Tariq mutters a LOCAL_EVENT rumour; player within
 *       3 blocks hears it</li>
 * </ol>
 *
 * <h3>Police catch in back room</h3>
 * If a {@code POLICE_OFFICER} NPC enters the shop during back-room cloning and the player
 * does not escape within {@link #BACK_ROOM_ESCAPE_WINDOW} seconds:
 * Notoriety +15, +2 wanted stars, newspaper headline fires.
 *
 * <h3>Fence valuations</h3>
 * WIPED_PHONE: 8 COIN; CLONED_PHONE: 5 COIN; BURNED_PHONE: 1 COIN.
 *
 * <h3>CashConverters integration</h3>
 * WIPED_PHONE sells at full rate. Selling STOLEN_PHONE triggers 30% suspicion
 * (Dean refuses and calls police). See {@link #isStolenPhoneSuspicion(Random)}.
 */
public class PhoneRepairSystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the shop opens (Mon–Sat). */
    public static final float OPEN_HOUR = 10.0f;

    /** Hour the shop closes (Mon–Sat). */
    public static final float CLOSE_HOUR = 18.0f;

    // ── Service prices ────────────────────────────────────────────────────────

    /** Cost of screen repair (BROKEN_PHONE → STOLEN_PHONE). */
    public static final int REPAIR_PRICE = 3;

    /** Cost of IMEI wipe (STOLEN_PHONE → WIPED_PHONE). */
    public static final int WIPE_PRICE = 5;

    /** Cost of back-room phone clone recipe (STOLEN_PHONE + SIM_CARD + 2 COIN). */
    public static final int CLONE_PRICE = 2;

    /** Cost of screen protector upsell. */
    public static final int SCREEN_PROTECTOR_PRICE = 1;

    // ── Time-based constants ──────────────────────────────────────────────────

    /** In-game minutes between atmospheric events. */
    public static final float EVENT_INTERVAL_MINUTES = 30.0f;

    /** In-game minutes for IMEI wipe to complete. */
    public static final float WIPE_DURATION_MINUTES = 10.0f;

    /** Real-time seconds the player has to escape the back room when police enter. */
    public static final float BACK_ROOM_ESCAPE_WINDOW = 5.0f;

    /** Real-time seconds the player has to move away during POLICE_KNOCK event. */
    public static final float POLICE_KNOCK_ESCAPE_WINDOW = 3.0f;

    // ── Distance constants ────────────────────────────────────────────────────

    /** Blocks within which police trigger IMEI wipe detection (standard). */
    public static final float POLICE_NEARBY_DISTANCE = 12.0f;

    /** Expanded police detection distance when player wears COUNCIL_JACKET disguise. */
    public static final float POLICE_NEARBY_DISTANCE_COUNCIL_JACKET = 20.0f;

    /** Blocks the player must move away during POLICE_KNOCK event. */
    public static final float POLICE_KNOCK_SAFE_DISTANCE = 6.0f;

    /** Blocks within which the player hears a TARIQ_RUMOUR event. */
    public static final float TARIQ_RUMOUR_HEAR_DISTANCE = 3.0f;

    // ── Notoriety and crime constants ─────────────────────────────────────────

    /** Notoriety added for IMEI wipe. */
    public static final int WIPE_NOTORIETY = 3;

    /** Notoriety added for back-room phone cloning. */
    public static final int CLONE_NOTORIETY = 3;

    /** Notoriety added when caught by police in the back room. */
    public static final int POLICE_CATCH_NOTORIETY = 15;

    /** Wanted stars added when caught by police in the back room. */
    public static final int POLICE_CATCH_WANTED_STARS = 2;

    /** Chance (0–1) police investigate after IMEI wipe with nearby officer. */
    public static final float IMEI_WIPE_POLICE_INVESTIGATE_CHANCE = 0.40f;

    /** Chance (0–1) Cash Converters (Dean) refuses STOLEN_PHONE and calls police. */
    public static final float CASH_CONVERTERS_SUSPICION_CHANCE = 0.30f;

    /** Marchetti Respect threshold needed to unlock back-room cloning. */
    public static final int MARCHETTI_RESPECT_THRESHOLD = 40;

    /** Number of Fix My Phone uses for TARIQ_REGULAR achievement (threshold). */
    public static final int TARIQ_REGULAR_THRESHOLD = 5;

    // ── Fence valuations ──────────────────────────────────────────────────────

    public static final int FENCE_VALUE_WIPED_PHONE   = 8;
    public static final int FENCE_VALUE_CLONED_PHONE  = 5;
    public static final int FENCE_VALUE_BURNED_PHONE  = 1;

    // ── Newspaper headline ────────────────────────────────────────────────────

    /** Newspaper headline fired when the player is caught in the back room. */
    public static final String POLICE_CATCH_HEADLINE =
            "PHONE SHOP RAID — IMEI CLONING GANG SMASHED";

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String TARIQ_GREETING         = "What's wrong with it?";
    public static final String TARIQ_GIVE_US_A_MINUTE = "Give us ten minutes.";
    public static final String TARIQ_SUSPICIOUS       = "That's not exactly yours, is it.";
    public static final String TARIQ_DONT_KNOW        = "I don't want to know, yeah?";
    public static final String TARIQ_CLOSED           = "We're shut. Come back tomorrow.";
    public static final String TARIQ_NO_COIN          = "You haven't got enough, mate.";
    public static final String TARIQ_REPAIR_DONE      = "All sorted. Good as new. Mostly.";
    public static final String TARIQ_WIPE_DONE        = "Clean as a whistle. Don't ask how.";
    public static final String TARIQ_CLONE_DONE       = "There you go. Never happened.";
    public static final String TARIQ_BACK_ROOM_UNLOCK = "Come round the back. Quietly, yeah?";

    // ── Atmospheric event types ───────────────────────────────────────────────

    /** The four possible atmospheric events that can fire while the shop is open. */
    public enum EventType {
        AWKWARD_CUSTOMER,
        POLICE_KNOCK,
        SMASHED_SCREEN,
        TARIQ_RUMOUR
    }

    // ── Service result enums ──────────────────────────────────────────────────

    /** Result of a {@link #repairScreen} call. */
    public enum RepairResult {
        SUCCESS,
        NO_BROKEN_PHONE,
        INSUFFICIENT_COIN,
        CLOSED
    }

    /** Result of a {@link #wipeImei} call. */
    public enum WipeResult {
        SUCCESS,
        NO_STOLEN_PHONE,
        INSUFFICIENT_COIN,
        CLOSED
    }

    /** Result of a {@link #clonePhone} call. */
    public enum CloneResult {
        SUCCESS,
        NOT_UNLOCKED,
        NO_STOLEN_PHONE,
        NO_SIM_CARD,
        INSUFFICIENT_COIN,
        CLOSED
    }

    /** Result of a {@link #buyScreenProtector} call. */
    public enum ScreenProtectorResult {
        SUCCESS,
        INSUFFICIENT_COIN,
        CLOSED
    }

    // ── Surveillance state ────────────────────────────────────────────────────

    /**
     * Tracks an active surveillance plant (CLONED_PHONE planted on an NPC).
     */
    public static class SurveillanceSession {
        private final NPC target;
        private float elapsedHours;
        private int rumoursExtracted;
        private boolean burned;

        public SurveillanceSession(NPC target) {
            this.target = target;
            this.elapsedHours = 0f;
            this.rumoursExtracted = 0;
            this.burned = false;
        }

        public NPC getTarget()         { return target; }
        public float getElapsedHours() { return elapsedHours; }
        public int getRumoursExtracted() { return rumoursExtracted; }
        public boolean isBurned()      { return burned; }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Tariq NPC reference (null if not spawned). */
    private NPC tariq = null;

    /** Whether Tariq has seeded a LOCAL_EVENT rumour this session. */
    private boolean tariqRumourSeededThisSession = false;

    /** Whether back-room cloning has been unlocked via quest completion. */
    private boolean questUnlocked = false;

    /** Total number of Fix My Phone service uses (for TARIQ_REGULAR achievement). */
    private int totalServiceUses = 0;

    /** Whether TARIQ_REGULAR has been awarded. */
    private boolean tariqRegularAwarded = false;

    /** In-game time (minutes) accumulated since last atmospheric event. */
    private float eventTimerMinutes = 0f;

    /** Whether a POLICE_KNOCK event is currently active. */
    private boolean policeKnockActive = false;

    /** Countdown (real seconds) remaining to escape during a POLICE_KNOCK event. */
    private float policeKnockTimer = 0f;

    /** The player position when POLICE_KNOCK fired (to measure escape distance). */
    private float policeKnockPlayerX = 0f;
    private float policeKnockPlayerZ = 0f;

    /** Whether a police alert is active in the back room. */
    private boolean policeAlertActive = false;

    /** Countdown (real seconds) remaining to escape the back room after police alert. */
    private float policeAlertTimer = 0f;

    /** Whether the player is currently in the back room. */
    private boolean playerInBackRoom = false;

    /** Whether WIPE is in progress (time-delayed service). */
    private boolean wipeInProgress = false;

    /** Remaining in-game minutes until WIPE completes. */
    private float wipeRemainingMinutes = 0f;

    /** Active surveillance sessions (CLONED_PHONE planted on NPCs). */
    private final List<SurveillanceSession> surveillanceSessions = new ArrayList<>();

    /** The most recent atmospheric event type that fired. */
    private EventType lastEvent = null;

    // ── Optional system references ─────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WitnessSystem witnessSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private FactionSystem factionSystem;
    private WantedSystem wantedSystem;
    private NewspaperSystem newspaperSystem;
    private DisguiseSystem disguiseSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public PhoneRepairSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ──────────────────────────────────────────

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

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setDisguiseSystem(DisguiseSystem disguiseSystem) {
        this.disguiseSystem = disguiseSystem;
    }

    // ── NPC management ────────────────────────────────────────────────────────

    /** Spawn Tariq at the counter if not already spawned. */
    public NPC forceSpawnTariq(float x, float y, float z) {
        tariq = new NPC(NPCType.PHONE_REPAIR_MAN, "Tariq", x, y, z);
        return tariq;
    }

    /** Returns the Tariq NPC, or null if not spawned. */
    public NPC getTariq() {
        return tariq;
    }

    // ── Opening hours ─────────────────────────────────────────────────────────

    /**
     * Returns true if the shop is open at the given time on the given day of week.
     *
     * @param hour      in-game hour (0.0–24.0)
     * @param dayOfWeek 1=Monday … 7=Sunday (shop closed on Sunday)
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == 7) return false; // Sunday: closed
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Convenience overload using a {@link TimeSystem}.
     */
    public boolean isOpen(TimeSystem timeSystem) {
        float hour = timeSystem.getTime();
        int dayOfWeek = getDayOfWeek(timeSystem);
        return isOpen(hour, dayOfWeek);
    }

    // ── Service: screen repair ─────────────────────────────────────────────────

    /**
     * Repair a BROKEN_PHONE into a STOLEN_PHONE for 3 COIN.
     *
     * <p>Removes BROKEN_PHONE and REPAIR_PRICE COIN from inventory, adds STOLEN_PHONE.
     *
     * @param inventory  the player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  1=Mon … 7=Sun
     * @return result code
     */
    public RepairResult repairScreen(Inventory inventory, float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) return RepairResult.CLOSED;
        if (!inventory.hasItem(Material.BROKEN_PHONE, 1)) return RepairResult.NO_BROKEN_PHONE;
        if (inventory.getItemCount(Material.COIN) < REPAIR_PRICE) return RepairResult.INSUFFICIENT_COIN;

        inventory.removeItem(Material.BROKEN_PHONE, 1);
        inventory.removeItem(Material.COIN, REPAIR_PRICE);
        inventory.addItem(Material.STOLEN_PHONE, 1);

        recordServiceUse();
        return RepairResult.SUCCESS;
    }

    /**
     * Convenience overload using a {@link TimeSystem}.
     */
    public RepairResult repairScreen(Inventory inventory, TimeSystem timeSystem) {
        return repairScreen(inventory, timeSystem.getTime(), getDayOfWeek(timeSystem));
    }

    // ── Service: IMEI wipe ─────────────────────────────────────────────────────

    /**
     * Start an IMEI wipe: STOLEN_PHONE → WIPED_PHONE for 5 COIN.
     *
     * <p>If a POLICE_OFFICER NPC is within {@link #POLICE_NEARBY_DISTANCE} blocks
     * (or {@link #POLICE_NEARBY_DISTANCE_COUNCIL_JACKET} if player has COUNCIL_JACKET),
     * there is a {@link #IMEI_WIPE_POLICE_INVESTIGATE_CHANCE} probability of triggering
     * a CrimeType.THEFT and +3 Notoriety.
     *
     * @param inventory  the player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  1=Mon … 7=Sun
     * @param player     the player (for position checks)
     * @param allNpcs    all living NPCs
     * @param callback   achievement callback (may be null)
     * @return result code
     */
    public WipeResult wipeImei(Inventory inventory, float hour, int dayOfWeek,
                                Player player, List<NPC> allNpcs,
                                NotorietySystem.AchievementCallback callback) {
        if (!isOpen(hour, dayOfWeek)) return WipeResult.CLOSED;
        if (!inventory.hasItem(Material.STOLEN_PHONE, 1)) return WipeResult.NO_STOLEN_PHONE;
        if (inventory.getItemCount(Material.COIN) < WIPE_PRICE) return WipeResult.INSUFFICIENT_COIN;

        inventory.removeItem(Material.STOLEN_PHONE, 1);
        inventory.removeItem(Material.COIN, WIPE_PRICE);
        inventory.addItem(Material.WIPED_PHONE, 1);

        // Notoriety + potential police investigation
        float detectRange = isCouncilJacket() ? POLICE_NEARBY_DISTANCE_COUNCIL_JACKET
                                              : POLICE_NEARBY_DISTANCE;
        boolean policeNearby = isPoliceNearby(player, allNpcs, detectRange);
        if (policeNearby && random.nextFloat() < IMEI_WIPE_POLICE_INVESTIGATE_CHANCE) {
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.THEFT);
            }
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(WIPE_NOTORIETY, callback);
        }

        // SIMSWAPPER achievement (first wipe)
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.SIMSWAPPER);
        }

        recordServiceUse();
        return WipeResult.SUCCESS;
    }

    /**
     * Convenience overload using a {@link TimeSystem}.
     */
    public WipeResult wipeImei(Inventory inventory, TimeSystem timeSystem,
                                Player player, List<NPC> allNpcs,
                                NotorietySystem.AchievementCallback callback) {
        return wipeImei(inventory, timeSystem.getTime(), getDayOfWeek(timeSystem),
                        player, allNpcs, callback);
    }

    // ── Service: back-room cloning ─────────────────────────────────────────────

    /**
     * Returns true if back-room cloning is unlocked (Marchetti Respect ≥ 40 or quest done).
     */
    public boolean isBackRoomUnlocked() {
        if (questUnlocked) return true;
        if (factionSystem != null) {
            return factionSystem.getRespect(Faction.MARCHETTI_CREW) >= MARCHETTI_RESPECT_THRESHOLD;
        }
        return false;
    }

    /**
     * Attempt back-room phone cloning: STOLEN_PHONE + SIM_CARD + 2 COIN → CLONED_PHONE.
     *
     * <p>Adds {@link CrimeType#THEFT} to the criminal record and +3 Notoriety.
     *
     * @param inventory  the player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  1=Mon … 7=Sun
     * @param callback   achievement callback (may be null)
     * @return result code
     */
    public CloneResult clonePhone(Inventory inventory, float hour, int dayOfWeek,
                                   NotorietySystem.AchievementCallback callback) {
        if (!isOpen(hour, dayOfWeek)) return CloneResult.CLOSED;
        if (!isBackRoomUnlocked()) return CloneResult.NOT_UNLOCKED;
        if (!inventory.hasItem(Material.STOLEN_PHONE, 1)) return CloneResult.NO_STOLEN_PHONE;
        if (!inventory.hasItem(Material.SIM_CARD, 1)) return CloneResult.NO_SIM_CARD;
        if (inventory.getItemCount(Material.COIN) < CLONE_PRICE) return CloneResult.INSUFFICIENT_COIN;

        inventory.removeItem(Material.STOLEN_PHONE, 1);
        inventory.removeItem(Material.SIM_CARD, 1);
        inventory.removeItem(Material.COIN, CLONE_PRICE);
        inventory.addItem(Material.CLONED_PHONE, 1);

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.THEFT);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(CLONE_NOTORIETY, callback);
        }

        recordServiceUse();
        return CloneResult.SUCCESS;
    }

    /**
     * Convenience overload using a {@link TimeSystem}.
     */
    public CloneResult clonePhone(Inventory inventory, TimeSystem timeSystem,
                                   NotorietySystem.AchievementCallback callback) {
        return clonePhone(inventory, timeSystem.getTime(), getDayOfWeek(timeSystem), callback);
    }

    // ── Service: screen protector upsell ──────────────────────────────────────

    /**
     * Buy a screen protector for 1 COIN.
     *
     * @param inventory  the player's inventory
     * @param hour       current in-game hour
     * @param dayOfWeek  1=Mon … 7=Sun
     * @return result code
     */
    public ScreenProtectorResult buyScreenProtector(Inventory inventory,
                                                     float hour, int dayOfWeek) {
        if (!isOpen(hour, dayOfWeek)) return ScreenProtectorResult.CLOSED;
        if (inventory.getItemCount(Material.COIN) < SCREEN_PROTECTOR_PRICE) {
            return ScreenProtectorResult.INSUFFICIENT_COIN;
        }
        inventory.removeItem(Material.COIN, SCREEN_PROTECTOR_PRICE);
        inventory.addItem(Material.SCREEN_PROTECTOR, 1);
        recordServiceUse();
        return ScreenProtectorResult.SUCCESS;
    }

    // ── Cloned phone use ──────────────────────────────────────────────────────

    /**
     * Use a CLONED_PHONE to extract 1 rumour from the RumourNetwork.
     * The phone becomes a BURNED_PHONE afterwards.
     *
     * @param inventory      the player's inventory
     * @param allNpcs        NPCs whose rumours may be tapped (pick randomly)
     * @param callback       achievement callback (may be null)
     * @return the extracted Rumour, or null if no rumours available / no cloned phone
     */
    public Rumour useClonedPhone(Inventory inventory, List<NPC> allNpcs,
                                  NotorietySystem.AchievementCallback callback) {
        if (!inventory.hasItem(Material.CLONED_PHONE, 1)) return null;

        inventory.removeItem(Material.CLONED_PHONE, 1);
        inventory.addItem(Material.BURNED_PHONE, 1);

        // Extract a relevant rumour from any NPC that has one
        Rumour extracted = extractRumour(allNpcs);
        return extracted;
    }

    /**
     * Plant a CLONED_PHONE on an NPC for 3-hour passive surveillance.
     *
     * @param inventory  the player's inventory
     * @param target     the NPC to plant the phone on
     * @param callback   achievement callback (may be null)
     * @return true if successfully planted
     */
    public boolean plantSurveillance(Inventory inventory, NPC target,
                                      NotorietySystem.AchievementCallback callback) {
        if (!inventory.hasItem(Material.CLONED_PHONE, 1)) return false;
        if (target == null) return false;

        inventory.removeItem(Material.CLONED_PHONE, 1);
        surveillanceSessions.add(new SurveillanceSession(target));

        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.PLANTED_IT);
        }
        return true;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Call once per frame to advance timers and process active mechanics.
     *
     * @param delta         real-time seconds since last frame
     * @param timeSystem    current game time
     * @param player        the player
     * @param inventory     the player's inventory
     * @param allNpcs       all living NPCs in the world
     * @param callback      achievement callback (may be null)
     */
    public void update(float delta, TimeSystem timeSystem,
                        Player player, Inventory inventory,
                        List<NPC> allNpcs,
                        NotorietySystem.AchievementCallback callback) {

        float hour = timeSystem.getTime();
        int dayOfWeek = getDayOfWeek(timeSystem);

        if (!isOpen(hour, dayOfWeek)) {
            // Reset event timer when closed
            eventTimerMinutes = 0f;
            policeKnockActive = false;
            policeAlertActive = false;
            return;
        }

        // Advance event timer (in-game minutes ≈ real seconds × time speed factor)
        // TimeSystem: DEFAULT_TIME_SPEED = 0.1 hours/s = 6 in-game minutes / real second
        float inGameMinutesPerRealSecond = 6.0f;
        eventTimerMinutes += delta * inGameMinutesPerRealSecond;

        if (eventTimerMinutes >= EVENT_INTERVAL_MINUTES) {
            eventTimerMinutes -= EVENT_INTERVAL_MINUTES;
            triggerRandomEvent(player, inventory, allNpcs, callback);
        }

        // POLICE_KNOCK countdown
        if (policeKnockActive) {
            policeKnockTimer -= delta;
            if (policeKnockTimer <= 0f) {
                policeKnockActive = false;
                // Check if player got away
                // (handled externally via checkPoliceKnockEscape)
            } else {
                // Check if player escaped
                if (player != null && hasMovedFarEnough(player,
                        policeKnockPlayerX, policeKnockPlayerZ,
                        POLICE_KNOCK_SAFE_DISTANCE)) {
                    policeKnockActive = false;
                }
            }
        }

        // Back-room police alert countdown
        if (policeAlertActive && playerInBackRoom) {
            policeAlertTimer -= delta;
            if (policeAlertTimer <= 0f) {
                policeAlertActive = false;
                playerInBackRoom = false;
                // Player caught — apply penalties
                applyPoliceCatchPenalties(player, callback);
            }
        }

        // Advance wipe-in-progress
        if (wipeInProgress) {
            wipeRemainingMinutes -= delta * inGameMinutesPerRealSecond;
            if (wipeRemainingMinutes <= 0f) {
                wipeInProgress = false;
            }
        }

        // Advance surveillance sessions
        updateSurveillanceSessions(delta, allNpcs, inventory, callback);
    }

    // ── Back-room police alert ─────────────────────────────────────────────────

    /**
     * Call when a POLICE_OFFICER NPC enters the shop while the player is in the back room.
     * Starts the escape countdown.
     */
    public void triggerPoliceAlert(Player player) {
        if (playerInBackRoom) {
            policeAlertActive = true;
            policeAlertTimer = BACK_ROOM_ESCAPE_WINDOW;
        }
    }

    /**
     * Call when the player presses E on the BACK_WINDOW_PROP to escape the back room.
     * Cancels the police alert.
     */
    public void escapeBackRoom() {
        policeAlertActive = false;
        playerInBackRoom = false;
        policeAlertTimer = 0f;
    }

    /** Set whether the player is currently in the back room. */
    public void setPlayerInBackRoom(boolean inBackRoom) {
        this.playerInBackRoom = inBackRoom;
    }

    /** Returns true if a back-room police alert is active. */
    public boolean isPoliceAlertActive() {
        return policeAlertActive;
    }

    // ── Quest unlock ──────────────────────────────────────────────────────────

    /** Call when the player completes the BuildingQuestRegistry quest for Fix My Phone. */
    public void completeQuest() {
        questUnlocked = true;
    }

    /** Returns true if the quest has been completed. */
    public boolean isQuestCompleted() {
        return questUnlocked;
    }

    // ── Atmospheric events ────────────────────────────────────────────────────

    /**
     * Returns true if a POLICE_KNOCK event is currently active.
     * (The player needs to move away within {@link #POLICE_KNOCK_ESCAPE_WINDOW} seconds.)
     */
    public boolean isPoliceKnockActive() {
        return policeKnockActive;
    }

    /**
     * Returns the remaining seconds in the active POLICE_KNOCK escape window.
     */
    public float getPoliceKnockTimer() {
        return policeKnockTimer;
    }

    /** Returns the last event type that was triggered, or null if none yet. */
    public EventType getLastEvent() {
        return lastEvent;
    }

    /**
     * Force-trigger a specific atmospheric event (for testing or scripted sequences).
     */
    public void forceEvent(EventType eventType, Player player, Inventory inventory,
                            List<NPC> allNpcs, NotorietySystem.AchievementCallback callback) {
        fireEvent(eventType, player, inventory, allNpcs, callback);
    }

    // ── Cash Converters integration ───────────────────────────────────────────

    /**
     * Returns true if Dean at Cash Converters should refuse a STOLEN_PHONE sale
     * and call police (~30% chance).
     */
    public boolean isStolenPhoneSuspicion(Random rng) {
        return rng.nextFloat() < CASH_CONVERTERS_SUSPICION_CHANCE;
    }

    // ── Test accessors ────────────────────────────────────────────────────────

    /** Force-unlock back-room cloning for testing. */
    public void setQuestUnlockedForTesting(boolean unlocked) {
        this.questUnlocked = unlocked;
    }

    /** Force total service uses for achievement testing. */
    public void setTotalServiceUsesForTesting(int count) {
        this.totalServiceUses = count;
    }

    /** Get total service uses. */
    public int getTotalServiceUses() {
        return totalServiceUses;
    }

    /** Get all active surveillance sessions. */
    public List<SurveillanceSession> getSurveillanceSessions() {
        return surveillanceSessions;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void recordServiceUse() {
        totalServiceUses++;
        if (!tariqRegularAwarded && totalServiceUses >= TARIQ_REGULAR_THRESHOLD) {
            tariqRegularAwarded = true;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.TARIQ_REGULAR);
            }
        }
    }

    private boolean isCouncilJacket() {
        return disguiseSystem != null && disguiseSystem.hasCouncilAccess();
    }

    private boolean isPoliceNearby(Player player, List<NPC> allNpcs, float range) {
        if (player == null || allNpcs == null) return false;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getType() == NPCType.POLICE || npc.getType() == NPCType.PCSO) {
                float dx = npc.getPosition().x - player.getPosition().x;
                float dz = npc.getPosition().z - player.getPosition().z;
                float dist = (float) Math.sqrt(dx * dx + dz * dz);
                if (dist <= range) return true;
            }
        }
        return false;
    }

    private boolean hasMovedFarEnough(Player player, float origX, float origZ, float distance) {
        float dx = player.getPosition().x - origX;
        float dz = player.getPosition().z - origZ;
        return Math.sqrt(dx * dx + dz * dz) >= distance;
    }

    private void triggerRandomEvent(Player player, Inventory inventory,
                                     List<NPC> allNpcs,
                                     NotorietySystem.AchievementCallback callback) {
        EventType[] types = EventType.values();
        EventType chosen = types[random.nextInt(types.length)];
        fireEvent(chosen, player, inventory, allNpcs, callback);
    }

    private void fireEvent(EventType eventType, Player player, Inventory inventory,
                            List<NPC> allNpcs,
                            NotorietySystem.AchievementCallback callback) {
        lastEvent = eventType;
        switch (eventType) {
            case AWKWARD_CUSTOMER:
                handleAwkwardCustomer(allNpcs);
                break;
            case POLICE_KNOCK:
                handlePoliceKnock(player, inventory, allNpcs, callback);
                break;
            case SMASHED_SCREEN:
                handleSmashedScreen(allNpcs);
                break;
            case TARIQ_RUMOUR:
                handleTariqRumour(player, allNpcs);
                break;
        }
    }

    private void handleAwkwardCustomer(List<NPC> allNpcs) {
        // Tariq deflects — find nearest PUBLIC NPC and give them a speech line
        if (tariq != null) {
            tariq.setSpeechText("Leave it out, yeah? It's being dealt with.", 4.0f);
        }
    }

    private void handlePoliceKnock(Player player, Inventory inventory,
                                    List<NPC> allNpcs,
                                    NotorietySystem.AchievementCallback callback) {
        if (player == null) return;
        // If player holds STOLEN_PHONE, start the escape countdown
        if (inventory != null && inventory.hasItem(Material.STOLEN_PHONE, 1)) {
            policeKnockActive = true;
            policeKnockTimer = POLICE_KNOCK_ESCAPE_WINDOW;
            policeKnockPlayerX = player.getX();
            policeKnockPlayerZ = player.getZ();
            // Award CRACKED_SCREEN achievement
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CRACKED_SCREEN);
            }
        }
        if (tariq != null) {
            tariq.setSpeechText("Nothing dodgy here, officer. Just phone repairs.", 5.0f);
        }
    }

    private void handleSmashedScreen(List<NPC> allNpcs) {
        // A YOUTH NPC drops a phone — BROKEN_PHONE would spawn on pavement
        // (prop spawning handled by world layer; we just flag it here)
        if (tariq != null) {
            tariq.setSpeechText("Oi! Watch where you're going!", 3.0f);
        }
    }

    private void handleTariqRumour(Player player, List<NPC> allNpcs) {
        if (rumourNetwork == null || tariq == null) return;
        if (tariqRumourSeededThisSession) return;

        String rumourText = "Word is there's movement round the back of the high street. Draw your own conclusions.";
        Rumour rumour = new Rumour(RumourType.LOCAL_EVENT, rumourText);
        rumourNetwork.addRumour(tariq, rumour);
        tariqRumourSeededThisSession = true;

        // Speech bubble if player is nearby
        if (player != null) {
            float dx = player.getX() - tariq.getPosition().x;
            float dz = player.getZ() - tariq.getPosition().z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= TARIQ_RUMOUR_HEAR_DISTANCE) {
                tariq.setSpeechText(rumourText, 5.0f);
            }
        }
    }

    private void applyPoliceCatchPenalties(Player player,
                                            NotorietySystem.AchievementCallback callback) {
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(POLICE_CATCH_NOTORIETY, callback);
        }
        if (wantedSystem != null && player != null) {
            wantedSystem.addWantedStars(POLICE_CATCH_WANTED_STARS,
                    player.getPosition().x, player.getPosition().y,
                    player.getPosition().z, callback);
        }
        if (newspaperSystem != null) {
            NewspaperSystem.InfamyEvent event = new NewspaperSystem.InfamyEvent(
                    "PHONE_SHOP_RAID", "Fix My Phone", null, "Tariq",
                    POLICE_CATCH_WANTED_STARS, "CAUGHT",
                    Faction.MARCHETTI_CREW, 7);
            newspaperSystem.recordEvent(event);
        }
    }

    private void updateSurveillanceSessions(float delta, List<NPC> allNpcs,
                                             Inventory inventory,
                                             NotorietySystem.AchievementCallback callback) {
        // In-game hours per real second (TimeSystem: 0.1 hours/second)
        float inGameHoursPerRealSecond = 0.1f;
        float hoursElapsed = delta * inGameHoursPerRealSecond;

        boolean anyBurnedThisFrame = false;

        for (SurveillanceSession session : surveillanceSessions) {
            if (session.burned) continue;

            float prevHours = session.elapsedHours;
            session.elapsedHours += hoursElapsed;

            // Extract 1 rumour per in-game hour elapsed
            int prevSlot = (int) prevHours;
            int newSlot = (int) session.elapsedHours;
            if (newSlot > prevSlot && session.rumoursExtracted < 3) {
                Rumour r = extractRumour(allNpcs);
                session.rumoursExtracted++;
            }

            // After 3 in-game hours → phone burns
            if (session.elapsedHours >= 3.0f || session.rumoursExtracted >= 3) {
                session.burned = true;
                anyBurnedThisFrame = true;
            }
        }

        if (anyBurnedThisFrame) {
            // Award BURNED achievement when a surveillance session completes
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.BURNED);
            }
            // Note: BURNED_PHONE is already in inventory from plantSurveillance
            // (it was never given as a physical item when planted — the phone is "on the NPC")
        }
    }

    private Rumour extractRumour(List<NPC> allNpcs) {
        if (allNpcs == null || allNpcs.isEmpty()) return null;
        // Prefer NPCs with LOOT_TIP or GANG_ACTIVITY rumours
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            for (Rumour r : npc.getRumours()) {
                if (r.getType() == RumourType.LOOT_TIP
                        || r.getType() == RumourType.GANG_ACTIVITY) {
                    return r;
                }
            }
        }
        // Fall back to any rumour
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            if (!npc.getRumours().isEmpty()) {
                return npc.getRumours().get(0);
            }
        }
        return null;
    }

    /**
     * Derive 1-indexed day of week from the TimeSystem day count.
     * Day 1 = Monday (game starts on Monday).
     */
    private int getDayOfWeek(TimeSystem timeSystem) {
        // dayCount starts at 1; (dayCount - 1) % 7 gives 0=Mon … 6=Sun
        int dow = ((timeSystem.getDayCount() - 1) % 7) + 1; // 1=Mon … 7=Sun
        return dow;
    }
}
