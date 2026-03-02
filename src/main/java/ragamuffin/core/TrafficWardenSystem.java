package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.Car;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * TrafficWardenSystem — Issue #1215: Northfield Traffic Warden.
 *
 * <p>Implements Clive, the town's sole Civil Enforcement Officer, who patrols
 * five council car parks Monday–Saturday 08:00–18:00. On HEAVY_RAIN (RAIN or
 * THUNDERSTORM) or SNOW (FROST) days Clive phones in sick and does not spawn.
 *
 * <p>Escalation:
 * <ol>
 *   <li>First offence — Penalty Charge Notice (PCN) placed on car.
 *       {@code CriminalRecord.CIVIL_ENFORCEMENT_NOTICE} recorded. Notoriety +2.</li>
 *   <li>Second offence (PCN unpaid) — WHEEL_CLAMP applied. Notoriety +3.
 *       Achievement {@code CLAMPED}.</li>
 *   <li>Third offence (clamped, PCN ≥ 1 in-game day old) — car impounded at
 *       {@code VEHICLE_IMPOUND}. Notoriety +5.</li>
 * </ol>
 *
 * <p>Player options:
 * <ul>
 *   <li>Pay 8 COIN fine to remove PCN/clamp.</li>
 *   <li>Bribe Clive (5 COIN, 35% base success).</li>
 *   <li>Use CLIVE_TERMINAL to remove clamp silently (80% success).</li>
 *   <li>Appeal at COUNCIL_OFFICE (base 30%, +bonuses from machine break,
 *       clean record, BENEFIT_APPEAL_LETTER).</li>
 *   <li>Use FORGED_PARKING_TICKET (40% pass, 60% detection triggers FRAUD).</li>
 *   <li>Retrieve impounded car at VEHICLE_IMPOUND for 20 COIN.</li>
 * </ul>
 */
public class TrafficWardenSystem {

    // ── Timing constants ─────────────────────────────────────────────────────

    /** Clive starts work at 08:00. */
    public static final float CLIVE_START_HOUR = 8.0f;
    /** Clive finishes at 18:00. */
    public static final float CLIVE_END_HOUR = 18.0f;
    /** Duration of one Clive patrol circuit in in-game minutes. */
    public static final float CIRCUIT_DURATION_MINUTES = 8.0f;

    // ── Financial constants ──────────────────────────────────────────────────

    /** Fine to pay off a PCN. */
    public static final int PCN_FINE_COIN = 8;
    /** Fine to release a clamped car. */
    public static final int CLAMP_FINE_COIN = 12;
    /** Cost to retrieve a towed car from the impound. */
    public static final int IMPOUND_RETRIEVAL_COIN = 20;
    /** Bribe cost. */
    public static final int BRIBE_COIN_COST = 5;

    // ── Probability constants ────────────────────────────────────────────────

    /** Base success probability for bribing Clive. */
    public static final float BRIBE_BASE_SUCCESS = 0.35f;
    /** Base success probability for PCN appeal. */
    public static final float APPEAL_BASE_SUCCESS = 0.30f;
    /** Appeal bonus when PAY_AND_DISPLAY machine was reported broken. */
    public static final float APPEAL_MACHINE_BROKEN_BONUS = 0.20f;
    /** Appeal bonus when player has no prior PCN offences this week. */
    public static final float APPEAL_CLEAN_RECORD_BONUS = 0.15f;
    /** Appeal bonus when player holds a BENEFIT_APPEAL_LETTER. */
    public static final float APPEAL_CAB_LETTER_BONUS = 0.20f;
    /** Probability that Clive detects a FORGED_PARKING_TICKET. */
    public static final float FORGED_TICKET_DETECTION_CHANCE = 0.60f;
    /** Probability that CLIVE_TERMINAL successfully removes a clamp. */
    public static final float TERMINAL_SUCCESS_CHANCE = 0.80f;

    // ── Notoriety impacts ────────────────────────────────────────────────────

    public static final int NOTORIETY_FIRST_PCN   = 2;
    public static final int NOTORIETY_CLAMP        = 3;
    public static final int NOTORIETY_IMPOUND       = 5;
    public static final int NOTORIETY_BRIBE_FAIL    = 3;
    public static final int NOTORIETY_FRAUD         = 6;

    // ── Day constants (0 = Monday) ───────────────────────────────────────────

    public static final int MONDAY    = 0;
    public static final int TUESDAY   = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY  = 3;
    public static final int FRIDAY    = 4;
    public static final int SATURDAY  = 5;
    public static final int SUNDAY    = 6;

    // ── State ────────────────────────────────────────────────────────────────

    private final Random random;

    /** Singleton Clive NPC (null when off-duty). */
    private NPC clive;

    /** Elapsed time (in in-game minutes) within the current Clive circuit. */
    private float circuitTimer = 0f;

    /** How many complete circuits Clive has made since the car was last ticketed. */
    private int circuitsSincePCN = 0;

    /** True when Clive is on-duty today. */
    private boolean cliveOnDuty = false;

    // ── Car state (per-car; for test simplicity a single tracked car) ────────

    /** Whether the tracked player car currently has a PCN. */
    private boolean carHasPCN = false;

    /** Whether the tracked player car is currently wheel-clamped. */
    private boolean carClamped = false;

    /** Whether the tracked player car has been impounded. */
    private boolean carImpounded = false;

    /** In-game minutes elapsed since the first PCN was issued (used for escalation). */
    private float minutesSincePCN = 0f;

    /** One in-game day ≈ 1440 in-game minutes. */
    private static final float MINUTES_PER_DAY = 1440f;

    // ── Appeal state ─────────────────────────────────────────────────────────

    /** True when an appeal is pending (submitted but not resolved). */
    private boolean appealPending = false;

    /** In-game minutes remaining before appeal result is processed. */
    private float appealTimer = 0f;

    /** Stored appeal success probability while appeal is pending. */
    private float pendingAppealChance = 0f;

    /** Pending appeal inventory reference (to refund/modify on success). */
    private Inventory pendingAppealInventory = null;

    // ── Machine state ─────────────────────────────────────────────────────────

    /** True when the PAY_AND_DISPLAY machine has been reported broken/vandalized. */
    private boolean machineReported = false;

    // ── Bribe state ──────────────────────────────────────────────────────────

    /** True when Clive was successfully bribed this session. */
    private boolean bribed = false;

    // ── Injected dependencies ────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;

    // ── Constructors ─────────────────────────────────────────────────────────

    public TrafficWardenSystem() {
        this(new Random());
    }

    public TrafficWardenSystem(Random random) {
        this.random = random;
    }

    // ── Dependency setters ────────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
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

    // ── Public state queries ──────────────────────────────────────────────────

    /** Whether Clive is currently on duty (spawned and patrolling). */
    public boolean isCliveOnDuty() {
        return cliveOnDuty;
    }

    /** Whether the tracked player car currently has a PCN. */
    public boolean isCarHasPCN() {
        return carHasPCN;
    }

    /** Whether the tracked player car is currently wheel-clamped. */
    public boolean isCarClamped() {
        return carClamped;
    }

    /** Whether the tracked player car has been impounded. */
    public boolean isCarImpounded() {
        return carImpounded;
    }

    /** Whether the PAY_AND_DISPLAY machine has been reported broken. */
    public boolean isMachineReported() {
        return machineReported;
    }

    /** Whether Clive was successfully bribed this session. */
    public boolean isBribed() {
        return bribed;
    }

    // ── Core update ───────────────────────────────────────────────────────────

    /**
     * Called every game frame.
     *
     * @param deltaMinutes   elapsed time in in-game minutes this frame
     * @param hour           current in-game hour (e.g. 10.5 = 10:30)
     * @param dayOfWeek      day index (0 = Monday … 6 = Sunday)
     * @param weather        current weather
     * @param playerCar      the player's car (may be null)
     * @param npcList        mutable NPC list (Clive is added/removed here)
     */
    public void update(float deltaMinutes, float hour, int dayOfWeek, Weather weather,
                       Car playerCar, List<NPC> npcList) {

        // ── 1. Determine if Clive should be on duty ───────────────────────────
        boolean shouldBeOnDuty = shouldCliveSpawn(hour, dayOfWeek, weather);

        if (shouldBeOnDuty && !cliveOnDuty) {
            spawnClive(npcList);
            cliveOnDuty = true;
        } else if (!shouldBeOnDuty && cliveOnDuty) {
            despawnClive(npcList);
            cliveOnDuty = false;
        }

        if (!cliveOnDuty) {
            return;
        }

        // ── 2. Advance circuit timer ──────────────────────────────────────────
        circuitTimer += deltaMinutes;

        // ── 3. Track escalation timer ─────────────────────────────────────────
        if (carHasPCN) {
            minutesSincePCN += deltaMinutes;
        }

        // ── 4. Clive completes a circuit ──────────────────────────────────────
        if (circuitTimer >= CIRCUIT_DURATION_MINUTES) {
            circuitTimer -= CIRCUIT_DURATION_MINUTES;
            onCircuitComplete(playerCar);
        }

        // ── 5. Resolve pending appeal ─────────────────────────────────────────
        if (appealPending) {
            appealTimer -= deltaMinutes;
            if (appealTimer <= 0f) {
                resolveAppeal();
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private boolean shouldCliveSpawn(float hour, int dayOfWeek, Weather weather) {
        // Clive only works Mon–Sat 08:00–18:00
        if (dayOfWeek == SUNDAY) {
            return false;
        }
        if (hour < CLIVE_START_HOUR || hour >= CLIVE_END_HOUR) {
            return false;
        }
        // Clive phones in sick on heavy rain or frost/snow days
        if (weather == Weather.RAIN || weather == Weather.THUNDERSTORM || weather == Weather.FROST) {
            return false;
        }
        return true;
    }

    private void spawnClive(List<NPC> npcList) {
        if (clive == null) {
            clive = new NPC(NPCType.TRAFFIC_WARDEN, 100f, 0f, 100f);
        }
        if (!npcList.contains(clive)) {
            npcList.add(clive);
        }
        if (clive.getState() != NPCState.PATROLLING) {
            clive.setState(NPCState.PATROLLING);
        }
    }

    private void despawnClive(List<NPC> npcList) {
        npcList.remove(clive);
    }

    /**
     * Called once per completed Clive circuit. Checks the player car and escalates
     * parking enforcement if needed.
     */
    private void onCircuitComplete(Car playerCar) {
        if (playerCar == null || !playerCar.isParked()) {
            return;
        }

        AchievementCallback cb = achievementCallback();

        if (!carHasPCN) {
            // First offence — issue PCN
            issuePCN(cb);
        } else if (!carClamped && minutesSincePCN >= MINUTES_PER_DAY) {
            // Second offence — PCN unpaid for ≥ 1 day, clamp car
            applyClamp(cb);
        } else if (carClamped && minutesSincePCN >= MINUTES_PER_DAY) {
            // Third offence — clamped and still not resolved, tow car
            towCar(playerCar, cb);
        }
    }

    private void issuePCN(AchievementCallback cb) {
        carHasPCN = true;
        minutesSincePCN = 0f;
        circuitsSincePCN = 0;

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_FIRST_PCN, cb);
        }
        if (rumourNetwork != null && clive != null) {
            rumourNetwork.addRumour(clive, new Rumour(RumourType.COUNCIL_ENFORCEMENT,
                    "Clive's been busy down the car park again — left a ticket on someone's motor."));
        }
    }

    private void applyClamp(AchievementCallback cb) {
        carClamped = true;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_CLAMP, cb);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CLAMPED);
        }
        if (rumourNetwork != null && clive != null) {
            rumourNetwork.addRumour(clive, new Rumour(RumourType.COUNCIL_ENFORCEMENT,
                    "Clive's gone and clamped someone in the car park. Third time this week."));
        }
    }

    private void towCar(Car playerCar, AchievementCallback cb) {
        carImpounded = true;
        carClamped = false;
        carHasPCN = false;

        if (playerCar != null) {
            playerCar.setImpounded(true);
            playerCar.setClamped(false);
            playerCar.setPCN(false);
        }

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(NOTORIETY_IMPOUND, cb);
        }
        if (rumourNetwork != null && clive != null) {
            rumourNetwork.addRumour(clive, new Rumour(RumourType.COUNCIL_ENFORCEMENT,
                    "Tow truck's been called. Some mug ignored two tickets."));
        }
    }

    // ── Player actions ────────────────────────────────────────────────────────

    /**
     * Result enum for player-initiated interactions.
     */
    public enum Result {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        NO_PCN,
        NOT_CLAMPED,
        NOT_IMPOUNDED,
        NO_TERMINAL_IN_INVENTORY,
        TERMINAL_FAILED,
        APPEAL_ALREADY_PENDING,
        BRIBE_SUCCESS,
        BRIBE_FAILED_NO_COINS,
        BRIBE_FAILED,
        FORGED_TICKET_ACCEPTED,
        FORGED_TICKET_DETECTED,
        NO_TICKET_IN_INVENTORY
    }

    /**
     * Player pays the fine to clear the PCN (and clamp if applicable).
     *
     * @param inventory player's inventory
     * @return SUCCESS or INSUFFICIENT_FUNDS or NO_PCN
     */
    public Result payFine(Inventory inventory) {
        if (!carHasPCN && !carClamped) {
            return Result.NO_PCN;
        }
        int required = carClamped ? CLAMP_FINE_COIN : PCN_FINE_COIN;
        if (inventory.getItemCount(Material.COIN) < required) {
            return Result.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, required);
        carHasPCN = false;
        carClamped = false;
        minutesSincePCN = 0f;
        return Result.SUCCESS;
    }

    /**
     * Player attempts to bribe Clive.
     *
     * @param inventory  player's inventory
     * @param notoriety  current player notoriety (0–100)
     * @return BRIBE_SUCCESS, BRIBE_FAILED, or BRIBE_FAILED_NO_COINS
     */
    public Result attemptBribe(Inventory inventory, int notoriety) {
        if (inventory.getItemCount(Material.COIN) < BRIBE_COIN_COST) {
            return Result.BRIBE_FAILED_NO_COINS;
        }

        float chance = BRIBE_BASE_SUCCESS;
        if (notoriety >= 50) {
            chance -= 0.30f;
        } else if (notoriety >= 30) {
            chance -= 0.20f;
        }
        chance = Math.max(0f, chance);

        if (random.nextFloat() < chance) {
            // Success
            inventory.removeItem(Material.COIN, BRIBE_COIN_COST);
            bribed = true;
            carHasPCN = false;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.VERBAL_WARNING);
            }
            return Result.BRIBE_SUCCESS;
        } else {
            // Failure — PCN still issued, notoriety + wanted
            AchievementCallback cb = achievementCallback();
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.BRIBERY);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NOTORIETY_BRIBE_FAIL, cb);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, cb);
            }
            return Result.BRIBE_FAILED;
        }
    }

    /**
     * Player uses Clive's Terminal to silently remove a wheel clamp.
     * The terminal is consumed on use regardless of success.
     *
     * @param inventory player's inventory
     * @return SUCCESS, TERMINAL_FAILED, NO_TERMINAL_IN_INVENTORY, or NOT_CLAMPED
     */
    public Result attemptTerminalRemoval(Inventory inventory) {
        if (!carClamped) {
            return Result.NOT_CLAMPED;
        }
        if (inventory.getItemCount(Material.CLIVE_TERMINAL) < 1) {
            return Result.NO_TERMINAL_IN_INVENTORY;
        }
        // Terminal is consumed on use (one-shot item)
        inventory.removeItem(Material.CLIVE_TERMINAL, 1);

        if (random.nextFloat() < TERMINAL_SUCCESS_CHANCE) {
            carClamped = false;
            carHasPCN = false;
            minutesSincePCN = 0f;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.CLAMP_EVADER);
            }
            return Result.SUCCESS;
        } else {
            return Result.TERMINAL_FAILED;
        }
    }

    /**
     * Player submits a PCN appeal at the COUNCIL_OFFICE APPEAL_DESK_PROP.
     * Requires PENALTY_CHARGE_NOTICE and BLANK_PAPER in inventory.
     * Appeal resolves after 2 in-game days (2880 minutes).
     *
     * @param inventory        player's inventory
     * @param machineReported  whether the PAY_AND_DISPLAY machine was reported broken
     * @param cleanRecord      whether the player has no prior PCNs this week
     * @param hasCABLetter     whether the player holds a BENEFIT_APPEAL_LETTER
     * @return SUCCESS (pending) or various failure codes
     */
    public Result submitAppeal(Inventory inventory, boolean machineReported,
                               boolean cleanRecord, boolean hasCABLetter) {
        if (!carHasPCN) {
            return Result.NO_PCN;
        }
        if (appealPending) {
            return Result.APPEAL_ALREADY_PENDING;
        }
        if (inventory.getItemCount(Material.PENALTY_CHARGE_NOTICE) < 1) {
            return Result.NO_TICKET_IN_INVENTORY;
        }

        float chance = APPEAL_BASE_SUCCESS;
        if (machineReported)  chance += APPEAL_MACHINE_BROKEN_BONUS;
        if (cleanRecord)      chance += APPEAL_CLEAN_RECORD_BONUS;
        if (hasCABLetter)     chance += APPEAL_CAB_LETTER_BONUS;
        chance = Math.min(1.0f, chance);

        this.machineReported = machineReported;
        appealPending = true;
        appealTimer = MINUTES_PER_DAY * 2f; // 2 in-game days
        pendingAppealChance = chance;
        pendingAppealInventory = inventory;

        return Result.SUCCESS;
    }

    private void resolveAppeal() {
        appealPending = false;
        AchievementCallback cb = achievementCallback();

        if (random.nextFloat() < pendingAppealChance) {
            // Appeal succeeded
            carHasPCN = false;
            minutesSincePCN = 0f;
            // Refund fine
            if (pendingAppealInventory != null) {
                pendingAppealInventory.removeItem(Material.PENALTY_CHARGE_NOTICE, 1);
                pendingAppealInventory.addItem(Material.COIN, PCN_FINE_COIN);
            }
            // Reduce crime count
            if (criminalRecord != null) {
                criminalRecord.clearOne(CriminalRecord.CrimeType.CIVIL_ENFORCEMENT_NOTICE);
            }
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.BUREAUCRAT_BESTED);
            }
        }
        pendingAppealInventory = null;
    }

    /**
     * Check whether a FORGED_PARKING_TICKET passes Clive's inspection.
     *
     * @param inventory player's inventory (must hold FORGED_PARKING_TICKET)
     * @return FORGED_TICKET_ACCEPTED (no PCN), or FORGED_TICKET_DETECTED (FRAUD crime)
     */
    public Result checkForgedTicket(Inventory inventory) {
        if (inventory.getItemCount(Material.FORGED_PARKING_TICKET) < 1) {
            return Result.NO_TICKET_IN_INVENTORY;
        }

        AchievementCallback cb = achievementCallback();

        if (random.nextFloat() < FORGED_TICKET_DETECTION_CHANCE) {
            // Detected
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.FRAUD);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NOTORIETY_FRAUD, cb);
            }
            // PCN issued anyway
            issuePCN(cb);
            return Result.FORGED_TICKET_DETECTED;
        } else {
            // Passed — Clive believes it, no PCN
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.TICKET_FORGER);
            }
            return Result.FORGED_TICKET_ACCEPTED;
        }
    }

    /**
     * Player retrieves their impounded car by paying 20 COIN at the VEHICLE_IMPOUND.
     *
     * @param inventory  player's inventory
     * @param playerCar  the impounded car
     * @return SUCCESS, INSUFFICIENT_FUNDS, or NOT_IMPOUNDED
     */
    public Result retrieveFromImpound(Inventory inventory, Car playerCar) {
        if (!carImpounded) {
            return Result.NOT_IMPOUNDED;
        }
        if (inventory.getItemCount(Material.COIN) < IMPOUND_RETRIEVAL_COIN) {
            return Result.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, IMPOUND_RETRIEVAL_COIN);
        carImpounded = false;
        carHasPCN = false;
        carClamped = false;
        minutesSincePCN = 0f;

        if (playerCar != null) {
            playerCar.setImpounded(false);
            playerCar.setClamped(false);
            playerCar.setPCN(false);
        }
        return Result.SUCCESS;
    }

    // ── Machine vandalism ─────────────────────────────────────────────────────

    /**
     * Record that the PAY_AND_DISPLAY machine has been vandalized/broken.
     * This boosts the appeal success bonus.
     */
    public void reportMachineBroken() {
        machineReported = true;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /** Force-set the circuit timer (for tests). */
    public void setCircuitTimerForTesting(float minutes) {
        this.circuitTimer = minutes;
    }

    /** Force the car to have a PCN already (for escalation tests). */
    public void setCarHasPCNForTesting(boolean hasPCN) {
        this.carHasPCN = hasPCN;
    }

    /** Force minutes-since-PCN (for escalation tests). */
    public void setMinutesSincePCNForTesting(float minutes) {
        this.minutesSincePCN = minutes;
    }

    /** Force the car to be clamped (for terminal/impound tests). */
    public void setCarClampedForTesting(boolean clamped) {
        this.carClamped = clamped;
    }

    /** Force the car to be impounded (for retrieval tests). */
    public void setCarImpoundedForTesting(boolean impounded) {
        this.carImpounded = impounded;
    }

    /** Force appeal pending (for appeal resolution tests). */
    public void setAppealPendingForTesting(boolean pending, float timerMinutes, float chance,
                                           Inventory inventory) {
        this.appealPending = pending;
        this.appealTimer = timerMinutes;
        this.pendingAppealChance = chance;
        this.pendingAppealInventory = inventory;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AchievementCallback achievementCallback() {
        if (achievementSystem == null) return null;
        return type -> achievementSystem.unlock(type);
    }
}
