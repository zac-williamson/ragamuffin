package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.*;

/**
 * Issue #918: Bus Stop &amp; Public Transport System — 'The Number 47'
 *
 * <p>A functioning bus network connecting three stops around the town with a
 * timetabled single-decker bus (The Number 47) that operates every 10 in-game
 * minutes from 06:00–23:00, and a Night Bus every 60 in-game minutes from
 * 23:00–06:00.
 *
 * <h3>Bus Stops</h3>
 * <ol>
 *   <li>High Street near Greggs ({@link LandmarkType#BUS_STOP_HIGH_STREET})</li>
 *   <li>Park near the pond ({@link LandmarkType#BUS_STOP_PARK})</li>
 *   <li>Industrial Estate near JobCentre ({@link LandmarkType#BUS_STOP_INDUSTRIAL})</li>
 * </ol>
 *
 * <h3>Timetable</h3>
 * <ul>
 *   <li>Day service (06:00–23:00): every {@link #DAY_BUS_INTERVAL_MINUTES} in-game minutes</li>
 *   <li>Night Bus (23:00–06:00): every {@link #NIGHT_BUS_INTERVAL_MINUTES} in-game minutes,
 *       2× fare</li>
 * </ul>
 *
 * <h3>NPC Queue</h3>
 * <ul>
 *   <li>Up to {@link #MAX_QUEUE_SIZE} NPCs wait at each stop.</li>
 *   <li>COMMUTER NPCs spawn during rush hours (07:30, 17:30) and path to the nearest stop.</li>
 *   <li>If no passengers are queuing and the player hasn't pressed F to flag, the bus skips.</li>
 *   <li>Skipping triggers the tooltip: "You have to wave it down. This isn't a taxi."</li>
 * </ul>
 *
 * <h3>Player Boarding</h3>
 * <ul>
 *   <li>Press E to board — costs {@link #BASE_FARE} COIN unless player holds a valid BUS_PASS.</li>
 *   <li>Fare evasion: adds Notoriety +1, {@link #FARE_EVASION_FINE} COIN fine (if caught),
 *       and a {@link CriminalRecord.CrimeType#FARE_EVASION} entry.</li>
 *   <li>Fast-travel: boarding teleports player to the next stop.</li>
 * </ul>
 *
 * <h3>Ticket Inspector</h3>
 * <ul>
 *   <li>30% chance a {@link NPCType#TICKET_INSPECTOR} rides on each journey.</li>
 *   <li>Can be bribed for {@link #INSPECTOR_BRIBE_COST} COIN or beaten for
 *       {@link Material#INSPECTOR_BADGE} loot.</li>
 *   <li>Calls police if notoriety Tier 3+.</li>
 * </ul>
 *
 * <h3>Night Bus</h3>
 * <ul>
 *   <li>Runs 23:00–06:00, once per hour (60 in-game minutes).</li>
 *   <li>2× fare (or 2× BUS_PASS day deduction).</li>
 *   <li>DRUNK NPCs have {@link #DRUNK_BRAWL_CHANCE} chance of brawling on arrival.</li>
 * </ul>
 *
 * <h3>Dynamic Fare Multipliers</h3>
 * <ul>
 *   <li>Rush hour (07:00–09:00, 17:00–19:00): ×{@link #RUSH_HOUR_MULTIPLIER}</li>
 *   <li>COUNCIL_CRACKDOWN market event: ×{@link #COUNCIL_CRACKDOWN_MULTIPLIER}</li>
 *   <li>Night Bus: ×{@link #NIGHT_BUS_MULTIPLIER}</li>
 * </ul>
 *
 * <h3>BUS_PASS</h3>
 * Craftable from 3 COIN + 1 NEWSPAPER. Provides 7-day unlimited travel.
 * Tracks days remaining via {@link #busPassDaysRemaining}.
 *
 * <h3>Achievements</h3>
 * <ul>
 *   <li>{@link AchievementType#MISSED_THE_BUS} — bus left while player was flagging</li>
 *   <li>{@link AchievementType#FARE_DODGER} — boarded without paying</li>
 *   <li>{@link AchievementType#LAST_NIGHT_BUS} — boarded the Night Bus after 23:00</li>
 *   <li>{@link AchievementType#COMMUTER_PICKPOCKET} — pickpocketed a COMMUTER on the bus</li>
 * </ul>
 */
public class BusSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** In-game hour the day service starts. */
    public static final float DAY_SERVICE_START = 6.0f;

    /** In-game hour the day service ends (Night Bus begins). */
    public static final float DAY_SERVICE_END = 23.0f;

    /** Day bus interval in in-game minutes (every 10 minutes). */
    public static final float DAY_BUS_INTERVAL_MINUTES = 10.0f;

    /** Night Bus interval in in-game minutes (every 60 minutes). */
    public static final float NIGHT_BUS_INTERVAL_MINUTES = 60.0f;

    /** Base fare in COIN for a single journey. */
    public static final int BASE_FARE = 2;

    /** Maximum NPCs queuing at any one stop. */
    public static final int MAX_QUEUE_SIZE = 8;

    /** Dwell time in real seconds the bus spends at each stop. */
    public static final float STOP_DWELL_SECONDS = 8.0f;

    /** Journey time (real seconds) between stops. */
    public static final float JOURNEY_SECONDS = 15.0f;

    /** Chance (0–1) that a TICKET_INSPECTOR boards with the bus each journey. */
    public static final float INSPECTOR_CHANCE = 0.30f;

    /** Cost to bribe the TICKET_INSPECTOR. */
    public static final int INSPECTOR_BRIBE_COST = 3;

    /** Fine applied when caught evading fare (on top of +1 notoriety). */
    public static final int FARE_EVASION_FINE = 5;

    /** Notoriety added when the player evades the fare. */
    public static final int FARE_EVASION_NOTORIETY = 1;

    /** Chance (0–1) that a DRUNK NPC brawls on Night Bus arrival. */
    public static final float DRUNK_BRAWL_CHANCE = 0.50f;

    /** Night Bus fare multiplier. */
    public static final float NIGHT_BUS_MULTIPLIER = 2.0f;

    /** Rush-hour fare multiplier (07:00–09:00 and 17:00–19:00). */
    public static final float RUSH_HOUR_MULTIPLIER = 1.5f;

    /** Fare multiplier during COUNCIL_CRACKDOWN market event. */
    public static final float COUNCIL_CRACKDOWN_MULTIPLIER = 1.75f;

    /** Duration of BUS_PASS in in-game days. */
    public static final int BUS_PASS_DURATION_DAYS = 7;

    /** Notoriety tier at or above which the TICKET_INSPECTOR calls police. */
    public static final int INSPECTOR_POLICE_CALL_TIER = 3;

    /** In-game minutes late before NPCs grumble at the stop. */
    public static final float GRUMBLE_LATE_MINUTES = 20.0f;

    /** Rush-hour start: 07:30. */
    public static final float RUSH_HOUR_MORNING = 7.5f;

    /** Rush-hour start: 17:30. */
    public static final float RUSH_HOUR_EVENING = 17.5f;

    /** In-game-minutes window around rush hour to spawn commuters. */
    public static final float RUSH_HOUR_WINDOW_MINUTES = 30.0f;

    // ── Stop identifiers ─────────────────────────────────────────────────────

    /** Index for the High Street stop. */
    public static final int STOP_HIGH_STREET = 0;

    /** Index for the Park stop. */
    public static final int STOP_PARK = 1;

    /** Index for the Industrial Estate stop. */
    public static final int STOP_INDUSTRIAL = 2;

    /** Total number of stops on the route. */
    public static final int NUM_STOPS = 3;

    // ── Grumble messages ──────────────────────────────────────────────────────

    public static final String[] GRUMBLE_LINES = {
        "Been waiting 20 minutes.",
        "It's always bloody late.",
        "Typical. Should've got the taxi.",
        "Every. Single. Day.",
        "They cut the service again, didn't they.",
        "You'd think they'd run it on time at least once."
    };

    // ── Bus state machine ─────────────────────────────────────────────────────

    /** Current operational state of the bus entity. */
    public enum BusState {
        /** Bus is inactive (outside service hours). */
        INACTIVE,
        /** Bus is en route to the next stop. */
        TRAVELLING,
        /** Bus is dwelling at a stop, boarding/alighting passengers. */
        AT_STOP,
        /** Bus is skipping a stop (no passengers, not flagged). */
        SKIPPING
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random rng;

    /** Current bus operational state. */
    private BusState busState = BusState.INACTIVE;

    /** Current stop index (0=High Street, 1=Park, 2=Industrial). */
    private int currentStopIndex = 0;

    /**
     * Timer tracking progress within current state (seconds).
     * For TRAVELLING: counts up to JOURNEY_SECONDS.
     * For AT_STOP: counts up to STOP_DWELL_SECONDS.
     */
    private float stateTimer = 0f;

    /** Whether the current service is the Night Bus (23:00–06:00). */
    private boolean isNightBus = false;

    /**
     * Countdown until the next bus departure (in real seconds).
     * Decremented each frame; when 0, the bus spawns at stop 0.
     */
    private float nextDepartureTimer = 0f;

    /** Whether the bus has been spawned for the current service. */
    private boolean busActive = false;

    /** Whether a TICKET_INSPECTOR is currently on board. */
    private boolean inspectorOnBoard = false;

    /** The TICKET_INSPECTOR NPC (null if none). */
    private NPC inspectorNpc = null;

    /** The BUS_DRIVER NPC (null if bus is inactive). */
    private NPC busDriverNpc = null;

    /**
     * Queues at each stop. Each list contains up to MAX_QUEUE_SIZE entities.
     * Entries are either NPC objects or {@link #PLAYER_QUEUE_ENTRY}.
     */
    private final List<List<Object>> stopQueues = new ArrayList<>();

    /** Sentinel for the player in the queue. */
    public static final Object PLAYER_QUEUE_ENTRY = new Object();

    /** Whether the player is currently on the bus. */
    private boolean playerOnBus = false;

    /** Whether the player has a valid ticket for the current journey. */
    private boolean playerTicketValid = false;

    /** Whether the player has flagged the bus (pressed F). */
    private boolean playerFlaggedBus = false;

    /** Whether the player is flagging at a stop. */
    private boolean playerAtStop = false;

    /** Which stop the player is waiting at (-1 = not at a stop). */
    private int playerStopIndex = -1;

    /** Remaining days on the player's BUS_PASS (0 = no pass). */
    private int busPassDaysRemaining = 0;

    /** In-game day when the pass was issued (to track expiry). */
    private int busPassIssuedDay = 0;

    /** Whether the inspector has already checked the player this journey. */
    private boolean inspectorCheckedPlayer = false;

    /**
     * Minutes late the bus is at the current stop.
     * Used to trigger NPC grumbles.
     */
    private float minutesLate = 0f;

    /**
     * Scheduled in-game time for the current bus to arrive at the current stop.
     * Used to compute lateness.
     */
    private float scheduledArrivalMinutes = 0f;

    /** Whether commuter-spawn has been done for the current morning rush (07:30). */
    private boolean morningRushSpawned = false;

    /** Whether commuter-spawn has been done for the current evening rush (17:30). */
    private boolean eveningRushSpawned = false;

    /** Counter for the current in-game day (to reset rush flags). */
    private int currentDay = 0;

    // ── NPC passengers currently on the bus ───────────────────────────────────

    /** NPCs currently on the bus (boarded). */
    private final List<NPC> onBusNpcs = new ArrayList<>();

    // ── Optional system references ────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private WantedSystem wantedSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public BusSystem() {
        this(new Random());
    }

    public BusSystem(Random rng) {
        this.rng = rng;
        for (int i = 0; i < NUM_STOPS; i++) {
            stopQueues.add(new ArrayList<>());
        }
    }

    // ── System wiring ─────────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem n) {
        this.notorietySystem = n;
    }

    public void setCriminalRecord(CriminalRecord c) {
        this.criminalRecord = c;
    }

    public void setAchievementSystem(AchievementSystem a) {
        this.achievementSystem = a;
    }

    public void setWantedSystem(WantedSystem w) {
        this.wantedSystem = w;
    }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Update the bus system each frame.
     *
     * @param delta             seconds since last frame (real-time)
     * @param hour              current in-game hour (0.0–24.0)
     * @param day               current in-game day number
     * @param activeEvent       active market event or null
     * @param nearbyNpcs        all NPCs near bus stops (for queue auto-join)
     * @param player            the player (may be null)
     * @param playerInventory   player's inventory (may be null)
     */
    public void update(float delta,
                       float hour,
                       int day,
                       MarketEvent activeEvent,
                       List<NPC> nearbyNpcs,
                       Player player,
                       Inventory playerInventory) {

        float normHour = normaliseHour(hour);

        // Track day change — reset rush-hour spawn flags
        if (day != currentDay) {
            currentDay = day;
            morningRushSpawned = false;
            eveningRushSpawned = false;
        }

        // Update bus pass validity
        if (playerInventory != null && busPassDaysRemaining > 0) {
            int elapsedDays = day - busPassIssuedDay;
            if (elapsedDays >= BUS_PASS_DURATION_DAYS) {
                busPassDaysRemaining = 0;
            }
        }

        // Auto-queue commuters at stops during rush hour
        if (nearbyNpcs != null) {
            autoQueueCommutersAtStops(normHour, nearbyNpcs);
        }

        if (!busActive) {
            // Countdown to next departure
            nextDepartureTimer -= delta;
            if (nextDepartureTimer <= 0f) {
                spawnBus(normHour, activeEvent);
            }
            return;
        }

        // Bus is active — advance state machine
        stateTimer += delta;

        switch (busState) {
            case TRAVELLING:
                if (stateTimer >= JOURNEY_SECONDS) {
                    stateTimer = 0f;
                    arriveAtStop(normHour, activeEvent, player, playerInventory);
                }
                break;

            case AT_STOP:
                if (stateTimer >= STOP_DWELL_SECONDS) {
                    stateTimer = 0f;
                    departStop(normHour, activeEvent, player, playerInventory);
                }
                break;

            case SKIPPING:
                // Skip pause — brief pause then move on
                if (stateTimer >= 2.0f) {
                    stateTimer = 0f;
                    advanceToNextStop(normHour, activeEvent, player, playerInventory);
                }
                break;

            default:
                break;
        }

        // Inspector checks the player if on board
        if (playerOnBus && inspectorOnBoard && !inspectorCheckedPlayer && busState == BusState.TRAVELLING) {
            checkPlayerTicket(player, playerInventory, activeEvent);
        }
    }

    // ── Spawn / despawn ───────────────────────────────────────────────────────

    private void spawnBus(float normHour, MarketEvent activeEvent) {
        isNightBus = isNightHour(normHour);
        busActive = true;
        currentStopIndex = STOP_HIGH_STREET;
        stateTimer = 0f;
        onBusNpcs.clear();
        inspectorOnBoard = rng.nextFloat() < INSPECTOR_CHANCE;
        inspectorCheckedPlayer = false;

        busDriverNpc = new NPC(NPCType.BUS_DRIVER, 0f, 1f, 0f);

        if (inspectorOnBoard) {
            inspectorNpc = new NPC(NPCType.TICKET_INSPECTOR, 1f, 1f, 0f);
        } else {
            inspectorNpc = null;
        }

        busState = BusState.AT_STOP;
    }

    private void despawnBus(float normHour, MarketEvent activeEvent) {
        busActive = false;

        // Deliver player to Industrial Estate if they were on the bus
        if (playerOnBus) {
            playerOnBus = false;
            playerTicketValid = false;
        }

        // Disembark all NPC passengers
        for (NPC npc : onBusNpcs) {
            npc.setState(NPCState.WANDERING);
        }
        onBusNpcs.clear();

        if (busDriverNpc != null) {
            busDriverNpc.kill();
            busDriverNpc = null;
        }
        if (inspectorNpc != null) {
            inspectorNpc.kill();
            inspectorNpc = null;
        }

        // Schedule next departure
        float intervalMinutes = isNightBus ? NIGHT_BUS_INTERVAL_MINUTES : DAY_BUS_INTERVAL_MINUTES;
        // Convert in-game minutes to real seconds (time speed: 0.1 hours/real-second = 6 min/real-second)
        nextDepartureTimer = inGameMinutesToRealSeconds(intervalMinutes);
    }

    // ── Stop arrival / departure ───────────────────────────────────────────────

    private void arriveAtStop(float normHour, MarketEvent activeEvent, Player player, Inventory playerInventory) {
        List<Object> queue = stopQueues.get(currentStopIndex);

        boolean playerFlagged = (playerAtStop && playerStopIndex == currentStopIndex && playerFlaggedBus);
        boolean hasPassengers = !queue.isEmpty() || !onBusNpcs.isEmpty() || playerFlagged;

        if (!hasPassengers && !playerFlagged) {
            // Skip this stop — no passengers, player didn't flag
            busState = BusState.SKIPPING;
            if (playerAtStop && playerStopIndex == currentStopIndex && !playerFlaggedBus) {
                // Player was here but didn't flag
                if (achievementSystem != null) {
                    achievementSystem.unlock(AchievementType.MISSED_THE_BUS);
                }
            }
            return;
        }

        busState = BusState.AT_STOP;

        // Board waiting NPCs
        boardNpcsFromStop(queue);

        // Board player if flagging at this stop
        if (playerFlagged) {
            boardPlayer(normHour, activeEvent, player, playerInventory);
        }
    }

    private void departStop(float normHour, MarketEvent activeEvent, Player player, Inventory playerInventory) {
        // Disembark some NPCs at each stop (they have reached their destination)
        disembarkNpcsAtStop(currentStopIndex);

        // Player disembarks if their stop has been reached
        if (playerOnBus) {
            // Fast travel: player teleported to destination stop (simplified)
            playerOnBus = false;
            playerTicketValid = false;
            playerFlaggedBus = false;
            playerAtStop = false;
            playerStopIndex = -1;
        }

        advanceToNextStop(normHour, activeEvent, player, playerInventory);
    }

    private void advanceToNextStop(float normHour, MarketEvent activeEvent, Player player, Inventory playerInventory) {
        currentStopIndex++;
        if (currentStopIndex >= NUM_STOPS) {
            // End of route — despawn
            despawnBus(normHour, activeEvent);
            return;
        }
        busState = BusState.TRAVELLING;
    }

    // ── NPC boarding / alighting ──────────────────────────────────────────────

    private void boardNpcsFromStop(List<Object> queue) {
        List<Object> toBoard = new ArrayList<>(queue);
        queue.clear();
        for (Object entry : toBoard) {
            if (entry instanceof NPC) {
                NPC npc = (NPC) entry;
                npc.setState(NPCState.BOARDING);
                onBusNpcs.add(npc);
            }
        }
    }

    private void disembarkNpcsAtStop(int stopIndex) {
        // Each NPC has roughly equal chance to disembark at any stop
        List<NPC> disembarked = new ArrayList<>();
        for (NPC npc : onBusNpcs) {
            if (rng.nextBoolean()) {
                disembarked.add(npc);
            }
        }
        for (NPC npc : disembarked) {
            onBusNpcs.remove(npc);
            npc.setState(NPCState.WANDERING);

            // Night Bus: DRUNK NPCs may brawl on arrival
            if (isNightBus && npc.getType() == NPCType.DRUNK) {
                if (rng.nextFloat() < DRUNK_BRAWL_CHANCE) {
                    npc.setState(NPCState.ATTACKING);
                }
            }
        }
    }

    // ── Player boarding ───────────────────────────────────────────────────────

    /**
     * Result of a player board action.
     */
    public enum BoardResult {
        SUCCESS,
        BUS_NOT_AT_STOP,
        BUS_NOT_ACTIVE,
        ALREADY_ON_BUS,
        INSUFFICIENT_FUNDS
    }

    /**
     * Player presses E to board the bus at their current stop.
     *
     * @param normHour        current in-game hour (normalised 0–24)
     * @param activeEvent     active market event or null
     * @param playerInventory player's inventory
     * @return result of the board action
     */
    public BoardResult boardPlayer(float normHour, MarketEvent activeEvent, Player player, Inventory playerInventory) {
        if (!busActive) return BoardResult.BUS_NOT_ACTIVE;
        if (busState != BusState.AT_STOP) return BoardResult.BUS_NOT_AT_STOP;
        if (playerOnBus) return BoardResult.ALREADY_ON_BUS;

        // Check if player has a valid BUS_PASS
        boolean hasBusPass = playerInventory != null && busPassDaysRemaining > 0
                && playerInventory.getItemCount(Material.BUS_PASS) > 0;

        int fare = computeFare(normHour, activeEvent);

        if (!hasBusPass) {
            // Deduct fare
            if (playerInventory != null && playerInventory.getItemCount(Material.COIN) < fare) {
                return BoardResult.INSUFFICIENT_FUNDS;
            }
            if (playerInventory != null) {
                playerInventory.removeItem(Material.COIN, fare);
            }
            playerTicketValid = true;
        } else {
            playerTicketValid = true;
        }

        playerOnBus = true;
        playerFlaggedBus = false;
        playerAtStop = false;
        playerStopIndex = -1;
        inspectorCheckedPlayer = false;

        // Night Bus achievement
        if (isNightBus && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.LAST_NIGHT_BUS);
        }

        return BoardResult.SUCCESS;
    }

    /**
     * Player evades the fare (boards without paying).
     * Adds Notoriety, fine, and CriminalRecord entry.
     *
     * @param normHour        current in-game hour
     * @param activeEvent     active market event or null
     * @param player          the player (may be null)
     * @param playerInventory the player's inventory
     */
    public void fareEvasion(float normHour, MarketEvent activeEvent, Player player, Inventory playerInventory) {
        if (!busActive || busState != BusState.AT_STOP) return;

        playerOnBus = true;
        playerTicketValid = false;
        playerFlaggedBus = false;
        playerAtStop = false;
        playerStopIndex = -1;
        inspectorCheckedPlayer = false;

        // Notoriety +1
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FARE_EVASION_NOTORIETY,
                    achievementSystem != null ? type -> achievementSystem.unlock(type) : null);
        }

        // Criminal record
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.FARE_EVASION);
        }

        // Fine (deducted from inventory if caught)
        if (playerInventory != null && playerInventory.getItemCount(Material.COIN) >= FARE_EVASION_FINE) {
            playerInventory.removeItem(Material.COIN, FARE_EVASION_FINE);
        }

        // Achievement
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.FARE_DODGER);
        }

        if (isNightBus && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.LAST_NIGHT_BUS);
        }
    }

    // ── Flag mechanic ─────────────────────────────────────────────────────────

    /**
     * Player presses F to flag the bus at their stop.
     *
     * @param stopIndex the stop where the player is waiting
     */
    public void flagBus(int stopIndex) {
        if (stopIndex < 0 || stopIndex >= NUM_STOPS) return;
        playerAtStop = true;
        playerFlaggedBus = true;
        playerStopIndex = stopIndex;
    }

    /**
     * Player stops flagging (moved away from stop or cancelled).
     */
    public void unflagBus() {
        playerFlaggedBus = false;
        playerAtStop = false;
        playerStopIndex = -1;
    }

    // ── Ticket inspector ──────────────────────────────────────────────────────

    /**
     * Inspector checks the player's ticket.
     * Called internally when inspector is on board and player has boarded.
     */
    private void checkPlayerTicket(Player player, Inventory playerInventory, MarketEvent activeEvent) {
        if (inspectorCheckedPlayer) return;
        inspectorCheckedPlayer = true;

        // Player holding INSPECTOR_BADGE bypasses check
        if (playerInventory != null
                && playerInventory.getItemCount(Material.INSPECTOR_BADGE) > 0) {
            return;
        }

        if (!playerTicketValid) {
            // Caught evading!
            if (inspectorNpc != null) {
                inspectorNpc.setSpeechText("Ticket please. ...No? Right then.", 5f);
            }

            // Additional fine
            if (playerInventory != null && playerInventory.getItemCount(Material.COIN) >= FARE_EVASION_FINE) {
                playerInventory.removeItem(Material.COIN, FARE_EVASION_FINE);
            }

            // Criminal record entry
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.FARE_EVASION);
            }

            // Notoriety
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(FARE_EVASION_NOTORIETY,
                        achievementSystem != null ? type -> achievementSystem.unlock(type) : null);
            }

            // Tier 3+ — inspector calls police
            if (notorietySystem != null && notorietySystem.getTier() >= INSPECTOR_POLICE_CALL_TIER) {
                if (inspectorNpc != null) {
                    inspectorNpc.setSpeechText("I'm calling the police!", 4f);
                }
                if (wantedSystem != null) {
                    wantedSystem.addWantedStars(1, 0f, 1f, 0f, null);
                }
            }
        }
    }

    /**
     * Player attempts to bribe the TICKET_INSPECTOR.
     *
     * @param playerInventory the player's inventory
     * @return true if bribe succeeded
     */
    public boolean bribeInspector(Inventory playerInventory) {
        if (!inspectorOnBoard || inspectorNpc == null) return false;
        if (playerInventory == null) return false;
        if (playerInventory.getItemCount(Material.COIN) < INSPECTOR_BRIBE_COST) return false;

        playerInventory.removeItem(Material.COIN, INSPECTOR_BRIBE_COST);
        playerTicketValid = true; // Bribed — effectively has a pass now
        if (inspectorNpc != null) {
            inspectorNpc.setSpeechText("...Say no more.", 3f);
        }
        return true;
    }

    /**
     * Player beats the TICKET_INSPECTOR — drops INSPECTOR_BADGE loot.
     *
     * @param playerInventory the player's inventory
     */
    public void beatInspector(Inventory playerInventory) {
        if (!inspectorOnBoard || inspectorNpc == null) return;

        inspectorNpc.setState(NPCState.KNOCKED_OUT);
        if (playerInventory != null) {
            playerInventory.addItem(Material.INSPECTOR_BADGE, 1);
        }

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(FARE_EVASION_NOTORIETY,
                    achievementSystem != null ? type -> achievementSystem.unlock(type) : null);
        }

        inspectorOnBoard = false;
    }

    // ── NPC auto-queue at stops ───────────────────────────────────────────────

    private void autoQueueCommutersAtStops(float normHour, List<NPC> nearbyNpcs) {
        for (NPC npc : nearbyNpcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() == NPCState.WAITING_FOR_BUS) continue;
            if (npc.getType() != NPCType.COMMUTER && npc.getType() != NPCType.PUBLIC) continue;

            // Only auto-queue COMMUTER types
            if (npc.getType() != NPCType.COMMUTER) continue;

            // Find nearest stop with space
            int nearestStop = findNearestStopWithSpace();
            if (nearestStop == -1) continue;

            List<Object> queue = stopQueues.get(nearestStop);
            if (queue.size() >= MAX_QUEUE_SIZE) continue;
            if (queue.contains(npc)) continue;

            npc.setState(NPCState.WAITING_FOR_BUS);
            queue.add(npc);
        }
    }

    private int findNearestStopWithSpace() {
        for (int i = 0; i < NUM_STOPS; i++) {
            if (stopQueues.get(i).size() < MAX_QUEUE_SIZE) return i;
        }
        return -1;
    }

    // ── BUS_PASS mechanics ────────────────────────────────────────────────────

    /**
     * Activate a BUS_PASS from the player's inventory.
     *
     * @param playerInventory the player's inventory
     * @param day             the current in-game day
     * @return true if the pass was activated
     */
    public boolean activateBusPass(Inventory playerInventory, int day) {
        if (playerInventory == null) return false;
        if (playerInventory.getItemCount(Material.BUS_PASS) < 1) return false;

        busPassDaysRemaining = BUS_PASS_DURATION_DAYS;
        busPassIssuedDay = day;
        return true;
    }

    // ── Fare calculation ──────────────────────────────────────────────────────

    /**
     * Compute the current fare given hour and market conditions.
     *
     * @param normHour    normalised in-game hour (0–24)
     * @param activeEvent active market event or null
     * @return fare in COIN (minimum 1)
     */
    public int computeFare(float normHour, MarketEvent activeEvent) {
        float multiplier = 1.0f;

        if (isNightBus) {
            multiplier *= NIGHT_BUS_MULTIPLIER;
        }

        // Rush hour: 07:00–09:00 or 17:00–19:00
        if ((normHour >= 7.0f && normHour < 9.0f) || (normHour >= 17.0f && normHour < 19.0f)) {
            multiplier *= RUSH_HOUR_MULTIPLIER;
        }

        // COUNCIL_CRACKDOWN market event
        if (activeEvent == MarketEvent.COUNCIL_CRACKDOWN) {
            multiplier *= COUNCIL_CRACKDOWN_MULTIPLIER;
        }

        return Math.max(1, (int) Math.ceil(BASE_FARE * multiplier));
    }

    // ── NPC grumbling ─────────────────────────────────────────────────────────

    /**
     * Make a random queuing NPC at the given stop grumble about the bus being late.
     * Call when the bus is overdue by more than {@link #GRUMBLE_LATE_MINUTES} minutes.
     *
     * @param stopIndex the stop index
     */
    public void triggerGrumbleAtStop(int stopIndex) {
        if (stopIndex < 0 || stopIndex >= NUM_STOPS) return;
        List<Object> queue = stopQueues.get(stopIndex);
        for (Object entry : queue) {
            if (entry instanceof NPC) {
                NPC npc = (NPC) entry;
                String line = GRUMBLE_LINES[rng.nextInt(GRUMBLE_LINES.length)];
                npc.setSpeechText(line, 4f);
                break; // one grumble per trigger
            }
        }
    }

    // ── Rush hour commuter spawning ───────────────────────────────────────────

    /**
     * Spawn commuter NPCs during rush hours.
     * Returns a list of newly created COMMUTER NPCs to be added to the world.
     *
     * @param normHour normalised in-game hour
     * @return list of new COMMUTER NPCs
     */
    public List<NPC> spawnRushHourCommutersIfNeeded(float normHour) {
        List<NPC> spawned = new ArrayList<>();

        float hourFraction = normHour % 24.0f;

        boolean isMorningRush = (hourFraction >= RUSH_HOUR_MORNING
                && hourFraction < RUSH_HOUR_MORNING + RUSH_HOUR_WINDOW_MINUTES / 60.0f);
        boolean isEveningRush = (hourFraction >= RUSH_HOUR_EVENING
                && hourFraction < RUSH_HOUR_EVENING + RUSH_HOUR_WINDOW_MINUTES / 60.0f);

        if (isMorningRush && !morningRushSpawned) {
            morningRushSpawned = true;
            spawned.addAll(createCommuters(STOP_HIGH_STREET));
        }
        if (isEveningRush && !eveningRushSpawned) {
            eveningRushSpawned = true;
            spawned.addAll(createCommuters(STOP_INDUSTRIAL));
        }

        return spawned;
    }

    private List<NPC> createCommuters(int nearStop) {
        List<NPC> commuters = new ArrayList<>();
        int count = 2 + rng.nextInt(4); // 2–5 commuters
        for (int i = 0; i < count; i++) {
            NPC c = new NPC(NPCType.COMMUTER, i * 1.5f, 1f, nearStop * 30f);
            c.setState(NPCState.WAITING_FOR_BUS);
            List<Object> queue = stopQueues.get(nearStop);
            if (queue.size() < MAX_QUEUE_SIZE) {
                queue.add(c);
            }
            commuters.add(c);
        }
        return commuters;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isNightHour(float normHour) {
        return normHour >= DAY_SERVICE_END || normHour < DAY_SERVICE_START;
    }

    private float normaliseHour(float hour) {
        return hour % 24.0f;
    }

    /**
     * Convert in-game minutes to real-time seconds.
     * Time speed: 0.1 hours / real second = 6 in-game minutes / real second.
     */
    public static float inGameMinutesToRealSeconds(float inGameMinutes) {
        return inGameMinutes / 6.0f;
    }

    /**
     * Force-spawn the bus at the given stop for testing.
     *
     * @param stopIndex   which stop to spawn at
     * @param nightBus    whether to treat as Night Bus
     * @param withInspector whether to include a TICKET_INSPECTOR
     */
    public void forceSpawnBus(int stopIndex, boolean nightBus, boolean withInspector) {
        isNightBus = nightBus;
        busActive = true;
        currentStopIndex = stopIndex;
        stateTimer = 0f;
        onBusNpcs.clear();
        inspectorOnBoard = withInspector;
        inspectorCheckedPlayer = false;
        busState = BusState.AT_STOP;
        busDriverNpc = new NPC(NPCType.BUS_DRIVER, 0f, 1f, 0f);
        if (withInspector) {
            inspectorNpc = new NPC(NPCType.TICKET_INSPECTOR, 1f, 1f, 0f);
        } else {
            inspectorNpc = null;
        }
    }

    /**
     * Force-set next departure timer (for testing).
     */
    public void setNextDepartureTimer(float seconds) {
        this.nextDepartureTimer = seconds;
    }

    /**
     * Add an NPC to the queue at a specific stop (for testing).
     */
    public void addNpcToStopQueue(int stopIndex, NPC npc) {
        if (stopIndex < 0 || stopIndex >= NUM_STOPS) return;
        List<Object> queue = stopQueues.get(stopIndex);
        if (queue.size() < MAX_QUEUE_SIZE) {
            queue.add(npc);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Whether the bus is currently active. */
    public boolean isBusActive() {
        return busActive;
    }

    /** Current bus state. */
    public BusState getBusState() {
        return busState;
    }

    /** Current stop index. */
    public int getCurrentStopIndex() {
        return currentStopIndex;
    }

    /** Whether the player is currently on the bus. */
    public boolean isPlayerOnBus() {
        return playerOnBus;
    }

    /** Whether the player has flagged the bus. */
    public boolean isPlayerFlaggedBus() {
        return playerFlaggedBus;
    }

    /** Whether the current service is the Night Bus. */
    public boolean isNightBus() {
        return isNightBus;
    }

    /** Whether a TICKET_INSPECTOR is on board. */
    public boolean isInspectorOnBoard() {
        return inspectorOnBoard;
    }

    /** The TICKET_INSPECTOR NPC (may be null). */
    public NPC getInspectorNpc() {
        return inspectorNpc;
    }

    /** The BUS_DRIVER NPC (may be null). */
    public NPC getBusDriverNpc() {
        return busDriverNpc;
    }

    /** Queue at the specified stop (unmodifiable). */
    public List<Object> getStopQueue(int stopIndex) {
        if (stopIndex < 0 || stopIndex >= NUM_STOPS) return Collections.emptyList();
        return Collections.unmodifiableList(stopQueues.get(stopIndex));
    }

    /** Whether the player's current ticket is valid. */
    public boolean isPlayerTicketValid() {
        return playerTicketValid;
    }

    /** Remaining days on the player's BUS_PASS (0 = no pass). */
    public int getBusPassDaysRemaining() {
        return busPassDaysRemaining;
    }

    /** Number of NPC passengers currently on the bus. */
    public int getOnBusNpcCount() {
        return onBusNpcs.size();
    }

    /** NPC passengers on the bus (unmodifiable). */
    public List<NPC> getOnBusNpcs() {
        return Collections.unmodifiableList(onBusNpcs);
    }

    /** Countdown until the next departure (real seconds). */
    public float getNextDepartureTimer() {
        return nextDepartureTimer;
    }
}
