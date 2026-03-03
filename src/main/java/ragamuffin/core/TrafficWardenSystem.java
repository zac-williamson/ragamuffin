package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1329 — Northfield Traffic Warden: Clive's Rounds, Wheel Clamping &amp;
 * the PCN Appeal Hustle.
 *
 * <h3>Mechanic 1 — Clive's Patrol</h3>
 * <ul>
 *   <li>Clive ({@code NPCType.TRAFFIC_WARDEN}) patrols {@code COUNCIL_CAR_PARK} and
 *       surrounding streets on a 20-block circuit, Mon–Sat 08:00–18:00.</li>
 *   <li>Issues {@code PENALTY_CHARGE_NOTICE} for unticketed vehicles.</li>
 *   <li>Applies {@code WHEEL_CLAMP_PROP} on second violation; triggers BAILIFF tow on third.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Pay-and-Display Machine</h3>
 * <ul>
 *   <li>1 COIN/hr, max 4 hrs. Player presses E on their car to place ticket on dashboard.</li>
 *   <li>Machine breakable (6 hits); broken machine creates reasonable-doubt radius of 10 blocks.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Wheel Clamp Removal</h3>
 * <ul>
 *   <li>Pay {@link #CLAMP_RELEASE_COST} COIN at {@code COUNCIL_OFFICE}.</li>
 *   <li>Grind with {@code SCREWDRIVER}: {@link #CLAMP_GRIND_SUCCESS_CHANCE} success,
 *       drops {@code WHEEL_CLAMP} item, adds {@code CRIMINAL_DAMAGE} + WantedSystem +1.</li>
 *   <li>Steal Clive's terminal by knocking him out: {@link #TERMINAL_VOID_CHANCE} void success.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — PCN Appeal at COUNCIL_OFFICE</h3>
 * <ul>
 *   <li>Submit {@code PENALTY_CHARGE_NOTICE} + {@code BLANK_PAPER} at {@code APPEAL_DESK_PROP}.</li>
 *   <li>{@link #APPEAL_WAIT_DAYS} day wait. Base {@link #APPEAL_BASE_CHANCE} success.</li>
 *   <li>Modifiers: broken machine (+{@link #APPEAL_BROKEN_MACHINE_BONUS}),
 *       no prior offences (+{@link #APPEAL_CLEAN_RECORD_BONUS}),
 *       DEBT_ADVICE_LETTER (+{@link #APPEAL_ADVICE_LETTER_BONUS}).</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Forged Parking Ticket Hustle</h3>
 * <ul>
 *   <li>Craft {@code FORGED_PARKING_TICKET} at {@code PHOTOCOPIER_PROP}
 *       ({@code PAY_AND_DISPLAY_TICKET} + {@code BLANK_PAPER}, GRAFTING &ge; Apprentice).</li>
 *   <li>{@link #FORGED_TICKET_DETECT_CHANCE} Clive detection &rarr; {@code PARKING_TICKET_FRAUD}
 *       + Notoriety +{@link #FORGED_DETECT_NOTORIETY}.</li>
 *   <li>Sell to commuters for {@link #FORGED_TICKET_SALE_COIN} COIN;
 *       {@link #TICKET_TOUT_DAILY_THRESHOLD} sales/day fires {@code TICKET_TOUT}.</li>
 * </ul>
 */
public class TrafficWardenSystem {

    // ── Patrol hours ───────────────────────────────────────────────────────────

    /** Hour Clive begins patrol (Mon–Sat). */
    public static final float PATROL_START_HOUR = 8.0f;

    /** Hour Clive ends patrol (Mon–Sat). */
    public static final float PATROL_END_HOUR = 18.0f;

    /** Patrol circuit length in blocks. */
    public static final int PATROL_CIRCUIT_BLOCKS = 20;

    // ── PCN / violation constants ──────────────────────────────────────────────

    /** Notoriety added when a PCN is issued. */
    public static final int PCN_NOTORIETY = 3;

    /** Notoriety added when a PARKING_TICKET_FRAUD crime is recorded. */
    public static final int FORGED_DETECT_NOTORIETY = 6;

    /** Wanted stars added when PARKING_TICKET_FRAUD is detected. */
    public static final int FORGED_DETECT_WANTED_STARS = 1;

    // ── Pay-and-display machine ────────────────────────────────────────────────

    /** Cost per hour of parking (COIN). */
    public static final int TICKET_COST_PER_HOUR = 1;

    /** Maximum hours a pay-and-display ticket can cover. */
    public static final int TICKET_MAX_HOURS = 4;

    /** Number of hits to break the PAY_AND_DISPLAY_MACHINE_PROP. */
    public static final int MACHINE_HP = 6;

    /** Radius (blocks) within which a broken machine creates reasonable doubt. */
    public static final float BROKEN_MACHINE_DOUBT_RADIUS = 10.0f;

    // ── Clamp removal ──────────────────────────────────────────────────────────

    /** COIN cost to release a wheel clamp at COUNCIL_OFFICE. */
    public static final int CLAMP_RELEASE_COST = 8;

    /** Chance (0–1) of successfully grinding a clamp with SCREWDRIVER. */
    public static final float CLAMP_GRIND_SUCCESS_CHANCE = 0.70f;

    /** Chance (0–1) of voiding a PCN using CLIVE_TERMINAL. */
    public static final float TERMINAL_VOID_CHANCE = 0.80f;

    // ── PCN appeal constants ───────────────────────────────────────────────────

    /** Number of in-game days to wait for appeal result. */
    public static final int APPEAL_WAIT_DAYS = 2;

    /** Base chance (0–1) an appeal is upheld. */
    public static final float APPEAL_BASE_CHANCE = 0.30f;

    /** Bonus chance if the pay-and-display machine was broken during violation. */
    public static final float APPEAL_BROKEN_MACHINE_BONUS = 0.20f;

    /** Bonus chance if the player has no prior PARKING_OFFENCE on record. */
    public static final float APPEAL_CLEAN_RECORD_BONUS = 0.15f;

    /** Bonus chance if the player submits with a DEBT_ADVICE_LETTER (Citizens Advice). */
    public static final float APPEAL_ADVICE_LETTER_BONUS = 0.10f;

    // ── Forged ticket hustle ───────────────────────────────────────────────────

    /** Chance (0–1) Clive detects a FORGED_PARKING_TICKET on inspection. */
    public static final float FORGED_TICKET_DETECT_CHANCE = 0.60f;

    /** COIN per forged ticket sold to a commuter. */
    public static final int FORGED_TICKET_SALE_COIN = 1;

    /** Number of forged tickets sold in one day to earn TICKET_TOUT achievement. */
    public static final int TICKET_TOUT_DAILY_THRESHOLD = 5;

    // ── Speech lines ───────────────────────────────────────────────────────────

    public static final String CLIVE_GREETING             = "Morning. Display your ticket please.";
    public static final String CLIVE_PCN_ISSUED           = "I'm issuing a Penalty Charge Notice. You've got 28 days.";
    public static final String CLIVE_CLAMP_APPLIED        = "Second offence. I'm clamping this vehicle.";
    public static final String CLIVE_FORGED_DETECTED      = "Hang on — that ticket's a forgery. I'm calling it in.";
    public static final String CLIVE_OFF_DUTY             = "I'm off duty. You're lucky today.";
    public static final String APPEAL_DESK_WELCOME        = "Fill in the form and we'll be in touch within two working days.";
    public static final String APPEAL_UPHELD              = "Your appeal has been upheld. The PCN has been cancelled.";
    public static final String APPEAL_REJECTED            = "Your appeal has been rejected. Payment is now due.";
    public static final String CLAMP_RELEASE_PAID         = "Clamp removed. Try not to park here without a ticket next time.";
    public static final String CLAMP_NO_COIN              = "You'll need eight coin to release the clamp. Come back when you have it.";

    // ── Achievement callback ───────────────────────────────────────────────────

    /**
     * Callback interface for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Result enums ───────────────────────────────────────────────────────────

    /**
     * Result of Clive inspecting a vehicle.
     */
    public enum PatrolInspectResult {
        /** Vehicle has a valid pay-and-display ticket — no action. */
        TICKET_VALID,
        /** No ticket — PCN issued (first violation). */
        PCN_ISSUED,
        /** No ticket, second violation — PCN issued and wheel clamp applied. */
        CLAMPED,
        /** Third violation — BAILIFF tow triggered. */
        TOWED,
        /** Forged ticket detected — FRAUD crime recorded. */
        FORGED_DETECTED,
        /** Vehicle ticketed with a forged ticket and Clive didn't notice. */
        FORGED_UNDETECTED,
        /** Machine is broken — reasonable doubt, no PCN issued. */
        MACHINE_BROKEN_DOUBT
    }

    /**
     * Result of attempting to buy a pay-and-display ticket.
     */
    public enum BuyTicketResult {
        /** Ticket purchased — PAY_AND_DISPLAY_TICKET added to inventory. */
        SUCCESS,
        /** Not enough COIN. */
        NO_COIN,
        /** Requested hours out of range (must be 1–4). */
        INVALID_HOURS,
        /** Machine is broken — cannot purchase. */
        MACHINE_BROKEN
    }

    /**
     * Result of attempting to remove a wheel clamp.
     */
    public enum ClampRemovalResult {
        /** Clamp released by paying at COUNCIL_OFFICE — WHEEL_CLAMP dropped. */
        RELEASED_PAID,
        /** Not enough COIN to pay at COUNCIL_OFFICE. */
        NOT_ENOUGH_COIN,
        /** Clamp ground off with SCREWDRIVER — WHEEL_CLAMP item dropped, crime recorded. */
        GROUND_OFF,
        /** Screwdriver grind failed — clamp still on. */
        GRIND_FAILED,
        /** PCN voided via CLIVE_TERMINAL — CLIVE_TERMINAL consumed. */
        TERMINAL_VOID,
        /** CLIVE_TERMINAL void failed. */
        TERMINAL_FAILED,
        /** Player doesn't have the required tool/coin. */
        MISSING_REQUIREMENT,
        /** Vehicle is not currently clamped. */
        NOT_CLAMPED
    }

    /**
     * Result of submitting a PCN appeal.
     */
    public enum AppealResult {
        /** Appeal submitted — waiting for result. */
        SUBMITTED,
        /** Appeal already pending. */
        ALREADY_PENDING,
        /** Player lacks required items (PCN + BLANK_PAPER). */
        MISSING_ITEMS,
        /** Appeal decision: upheld. */
        UPHELD,
        /** Appeal decision: rejected. */
        REJECTED,
        /** No pending appeal to resolve. */
        NO_PENDING_APPEAL,
        /** Appeal result not yet ready (still within wait period). */
        NOT_READY
    }

    /**
     * Result of attempting to sell a forged parking ticket.
     */
    public enum ForgedTicketSaleResult {
        /** Sold to a commuter for 1 COIN. */
        SOLD,
        /** Clive detected the forged ticket — crime recorded. */
        DETECTED_BY_CLIVE,
        /** Player does not have a FORGED_PARKING_TICKET to sell. */
        NO_TICKET,
        /** GRAFTING skill too low to craft forged ticket. */
        SKILL_TOO_LOW
    }

    /**
     * Result of crafting a FORGED_PARKING_TICKET at a PHOTOCOPIER_PROP.
     */
    public enum ForgedCraftResult {
        /** FORGED_PARKING_TICKET crafted — materials consumed. */
        CRAFTED,
        /** Missing PAY_AND_DISPLAY_TICKET or BLANK_PAPER. */
        MISSING_MATERIALS,
        /** GRAFTING skill below Apprentice level. */
        SKILL_TOO_LOW
    }

    // ── Dependencies (injected) ────────────────────────────────────────────────

    private WantedSystem wantedSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** The Clive NPC (null if not currently spawned). */
    private NPC cliveNpc = null;

    /** Number of violations the player has accumulated (resets on tow). */
    private int violationCount = 0;

    /** Whether the player's vehicle is currently clamped. */
    private boolean vehicleClamped = false;

    /** Whether the pay-and-display machine is currently broken. */
    private boolean machineIsBroken = false;

    /** Remaining HP of the pay-and-display machine. */
    private int machineHp = MACHINE_HP;

    /** Day number on which the current PCN appeal was submitted (-1 = none pending). */
    private int appealSubmittedDay = -1;

    /** Whether the machine was broken at the time of the pending appeal. */
    private boolean appealMachineBroken = false;

    /** Whether the player had no prior offences at the time of the appeal. */
    private boolean appealCleanRecord = false;

    /** Whether a DEBT_ADVICE_LETTER was submitted with the appeal. */
    private boolean appealWithAdviceLetter = false;

    /** Number of forged tickets sold today. */
    private int forgedTicketsSoldToday = 0;

    /** Day number of last forged ticket count reset. */
    private int lastForgedResetDay = -1;

    /** Whether the TICKET_TOUT achievement has been awarded. */
    private boolean ticketToutAwarded = false;

    /** Whether the CLAMPED achievement has been awarded. */
    private boolean clampedAwarded = false;

    // ── Constructor ────────────────────────────────────────────────────────────

    public TrafficWardenSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection setters ───────────────────────────────────────────

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
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

    // ── NPC management ─────────────────────────────────────────────────────────

    /**
     * Force-spawn Clive for testing purposes.
     */
    public void forceSpawnClive() {
        cliveNpc = new NPC(NPCType.TRAFFIC_WARDEN, "Clive", 0f, 0f, 0f);
    }

    /** Returns the Clive NPC (may be null if not spawned). */
    public NPC getClive() {
        return cliveNpc;
    }

    // ── Opening hours / patrol window ──────────────────────────────────────────

    /**
     * Returns true if Clive is on duty (Mon–Sat 08:00–18:00).
     *
     * @param hour       current in-game hour (0–24)
     * @param dayOfWeek  0=Sunday … 6=Saturday
     */
    public boolean isOnDuty(float hour, int dayOfWeek) {
        if (dayOfWeek == 0) return false; // Closed Sunday
        return hour >= PATROL_START_HOUR && hour < PATROL_END_HOUR;
    }

    // ── Pay-and-display machine ────────────────────────────────────────────────

    /**
     * Player presses E on the PAY_AND_DISPLAY_MACHINE_PROP to buy a parking ticket.
     *
     * @param inventory  player's inventory
     * @param hours      number of hours requested (1–4)
     * @return result of the purchase attempt
     */
    public BuyTicketResult buyTicket(Inventory inventory, int hours) {
        if (machineIsBroken) {
            return BuyTicketResult.MACHINE_BROKEN;
        }
        if (hours < 1 || hours > TICKET_MAX_HOURS) {
            return BuyTicketResult.INVALID_HOURS;
        }
        int cost = TICKET_COST_PER_HOUR * hours;
        if (inventory.getItemCount(Material.COIN) < cost) {
            return BuyTicketResult.NO_COIN;
        }
        inventory.removeItem(Material.COIN, cost);
        inventory.addItem(Material.PAY_AND_DISPLAY_TICKET, 1);
        return BuyTicketResult.SUCCESS;
    }

    /**
     * Player punches the PAY_AND_DISPLAY_MACHINE_PROP.
     * Machine breaks after {@link #MACHINE_HP} hits.
     *
     * @return true if the machine has just been destroyed
     */
    public boolean hitMachine() {
        if (machineIsBroken) return false;
        machineHp--;
        if (machineHp <= 0) {
            machineIsBroken = true;
            return true;
        }
        return false;
    }

    /** Returns whether the pay-and-display machine is currently broken. */
    public boolean isMachineBroken() {
        return machineIsBroken;
    }

    /** Returns remaining machine HP (0 when broken). */
    public int getMachineHp() {
        return machineHp;
    }

    // ── Patrol inspection ──────────────────────────────────────────────────────

    /**
     * Clive inspects the player's vehicle. Called during patrol when Clive reaches
     * a vehicle in the COUNCIL_CAR_PARK or surrounding streets.
     *
     * @param inventory          player's inventory
     * @param npcs               all active NPCs
     * @param px                 player X position
     * @param py                 player Y position
     * @param pz                 player Z position
     * @param achievementCallback callback for achievements (may be null)
     * @return the result of Clive's inspection
     */
    public PatrolInspectResult inspectVehicle(
            Inventory inventory,
            List<NPC> npcs,
            float px, float py, float pz,
            AchievementCallback achievementCallback) {

        // Broken machine: reasonable doubt — no PCN
        if (machineIsBroken) {
            return PatrolInspectResult.MACHINE_BROKEN_DOUBT;
        }

        // Check for valid ticket
        if (inventory.hasItem(Material.PAY_AND_DISPLAY_TICKET)) {
            return PatrolInspectResult.TICKET_VALID;
        }

        // Check for forged ticket
        if (inventory.hasItem(Material.FORGED_PARKING_TICKET)) {
            if (random.nextFloat() < FORGED_TICKET_DETECT_CHANCE) {
                // Detected — remove forged ticket, record crime
                inventory.removeItem(Material.FORGED_PARKING_TICKET, 1);
                if (criminalRecord != null) {
                    criminalRecord.record(CrimeType.PARKING_TICKET_FRAUD);
                }
                if (notorietySystem != null) {
                    notorietySystem.addNotoriety(FORGED_DETECT_NOTORIETY, null);
                }
                if (wantedSystem != null) {
                    wantedSystem.addWantedStars(FORGED_DETECT_WANTED_STARS, px, py, pz,
                            achievementCallback != null
                                    ? t -> achievementCallback.award(t)
                                    : null);
                }
                // Also issue PCN
                issuepcn(inventory, npcs, px, py, pz, achievementCallback);
                return PatrolInspectResult.FORGED_DETECTED;
            } else {
                // Not detected — treat as valid
                return PatrolInspectResult.FORGED_UNDETECTED;
            }
        }

        // No ticket — issue PCN and escalate
        violationCount++;
        issuepcn(inventory, npcs, px, py, pz, achievementCallback);

        if (violationCount >= 3) {
            // Third violation — tow
            violationCount = 0;
            vehicleClamped = false;
            return PatrolInspectResult.TOWED;
        } else if (violationCount == 2) {
            // Second violation — clamp
            vehicleClamped = true;
            if (!clampedAwarded && achievementCallback != null) {
                clampedAwarded = true;
                achievementCallback.award(AchievementType.CLAMPED);
            }
            return PatrolInspectResult.CLAMPED;
        } else {
            return PatrolInspectResult.PCN_ISSUED;
        }
    }

    /**
     * Issue a PENALTY_CHARGE_NOTICE to the player.
     */
    private void issuepcn(Inventory inventory, List<NPC> npcs, float px, float py, float pz,
                           AchievementCallback achievementCallback) {
        inventory.addItem(Material.PENALTY_CHARGE_NOTICE, 1);
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.PARKING_OFFENCE);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(PCN_NOTORIETY, null);
        }
    }

    // ── Wheel clamp removal ────────────────────────────────────────────────────

    /**
     * Player pays the release fee at COUNCIL_OFFICE.
     *
     * @param inventory          player's inventory
     * @param achievementCallback callback for achievements
     * @return result of the release attempt
     */
    public ClampRemovalResult payClampRelease(Inventory inventory,
                                               AchievementCallback achievementCallback) {
        if (!vehicleClamped) {
            return ClampRemovalResult.NOT_CLAMPED;
        }
        if (inventory.getItemCount(Material.COIN) < CLAMP_RELEASE_COST) {
            return ClampRemovalResult.NOT_ENOUGH_COIN;
        }
        inventory.removeItem(Material.COIN, CLAMP_RELEASE_COST);
        vehicleClamped = false;
        return ClampRemovalResult.RELEASED_PAID;
    }

    /**
     * Player uses a SCREWDRIVER to grind the clamp off.
     *
     * @param inventory          player's inventory
     * @param px                 player X position
     * @param py                 player Y position
     * @param pz                 player Z position
     * @param achievementCallback callback for achievements
     * @return result of the grind attempt
     */
    public ClampRemovalResult grindClampOff(Inventory inventory, float px, float py, float pz,
                                             AchievementCallback achievementCallback) {
        if (!vehicleClamped) {
            return ClampRemovalResult.NOT_CLAMPED;
        }
        if (!inventory.hasItem(Material.SCREWDRIVER)) {
            return ClampRemovalResult.MISSING_REQUIREMENT;
        }

        if (random.nextFloat() < CLAMP_GRIND_SUCCESS_CHANCE) {
            vehicleClamped = false;
            inventory.addItem(Material.WHEEL_CLAMP, 1);
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.CRIMINAL_DAMAGE);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, px, py, pz,
                        achievementCallback != null ? t -> achievementCallback.award(t) : null);
            }
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CLAMP_DODGER);
            }
            return ClampRemovalResult.GROUND_OFF;
        } else {
            return ClampRemovalResult.GRIND_FAILED;
        }
    }

    /**
     * Player uses a CLIVE_TERMINAL (stolen from knocked-out Clive) to void the PCN
     * and remove the clamp.
     *
     * @param inventory          player's inventory
     * @param achievementCallback callback for achievements
     * @return result of the terminal use attempt
     */
    public ClampRemovalResult useCliveTerminal(Inventory inventory,
                                                AchievementCallback achievementCallback) {
        if (!vehicleClamped) {
            return ClampRemovalResult.NOT_CLAMPED;
        }
        if (!inventory.hasItem(Material.CLIVE_TERMINAL)) {
            return ClampRemovalResult.MISSING_REQUIREMENT;
        }

        // Consume the terminal (one-use)
        inventory.removeItem(Material.CLIVE_TERMINAL, 1);

        if (random.nextFloat() < TERMINAL_VOID_CHANCE) {
            vehicleClamped = false;
            // Remove any outstanding PCN from inventory
            if (inventory.hasItem(Material.PENALTY_CHARGE_NOTICE)) {
                inventory.removeItem(Material.PENALTY_CHARGE_NOTICE, 1);
            }
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.CLAMP_DODGER);
            }
            return ClampRemovalResult.TERMINAL_VOID;
        } else {
            return ClampRemovalResult.TERMINAL_FAILED;
        }
    }

    /**
     * Clive is knocked out — drop CLIVE_TERMINAL.
     *
     * @param inventory          player's inventory
     * @param achievementCallback callback for achievements
     */
    public void onCliveKnockedOut(Inventory inventory, AchievementCallback achievementCallback) {
        inventory.addItem(Material.CLIVE_TERMINAL, 1);
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.TERMINAL_THIEF);
        }
        // Despawn Clive
        if (cliveNpc != null) {
            cliveNpc.setState(NPCState.FLEEING);
        }
    }

    // ── PCN appeal ─────────────────────────────────────────────────────────────

    /**
     * Player submits a PCN appeal at APPEAL_DESK_PROP (COUNCIL_OFFICE).
     *
     * @param inventory   player's inventory
     * @param currentDay  current in-game day number
     * @return result of the submission
     */
    public AppealResult submitAppeal(Inventory inventory, int currentDay) {
        if (appealSubmittedDay >= 0) {
            return AppealResult.ALREADY_PENDING;
        }
        if (!inventory.hasItem(Material.PENALTY_CHARGE_NOTICE)
                || !inventory.hasItem(Material.BLANK_PAPER)) {
            return AppealResult.MISSING_ITEMS;
        }

        // Consume materials
        inventory.removeItem(Material.PENALTY_CHARGE_NOTICE, 1);
        inventory.removeItem(Material.BLANK_PAPER, 1);

        // Record appeal context for modifiers
        appealMachineBroken = machineIsBroken;
        appealCleanRecord = (criminalRecord == null
                || criminalRecord.getCount(CrimeType.PARKING_OFFENCE) <= 1);

        // Consume advice letter if present
        if (inventory.hasItem(Material.DEBT_ADVICE_LETTER)) {
            inventory.removeItem(Material.DEBT_ADVICE_LETTER, 1);
            appealWithAdviceLetter = true;
        } else {
            appealWithAdviceLetter = false;
        }

        appealSubmittedDay = currentDay;
        return AppealResult.SUBMITTED;
    }

    /**
     * Check the appeal result (called on each day tick after submission).
     *
     * @param inventory          player's inventory
     * @param currentDay         current in-game day number
     * @param achievementCallback callback for achievements
     * @return the appeal decision, or NOT_READY if still pending
     */
    public AppealResult resolveAppeal(Inventory inventory, int currentDay,
                                       AchievementCallback achievementCallback) {
        if (appealSubmittedDay < 0) {
            return AppealResult.NO_PENDING_APPEAL;
        }
        if ((currentDay - appealSubmittedDay) < APPEAL_WAIT_DAYS) {
            return AppealResult.NOT_READY;
        }

        // Calculate success chance with modifiers
        float chance = APPEAL_BASE_CHANCE;
        if (appealMachineBroken)      chance += APPEAL_BROKEN_MACHINE_BONUS;
        if (appealCleanRecord)        chance += APPEAL_CLEAN_RECORD_BONUS;
        if (appealWithAdviceLetter)   chance += APPEAL_ADVICE_LETTER_BONUS;

        // Reset appeal state
        appealSubmittedDay = -1;

        if (random.nextFloat() < chance) {
            // Upheld — clear the parking offence
            if (criminalRecord != null && criminalRecord.getCount(CrimeType.PARKING_OFFENCE) > 0) {
                criminalRecord.clearOne(CrimeType.PARKING_OFFENCE);
            }
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.APPEAL_SUCCESS);
            }
            return AppealResult.UPHELD;
        } else {
            return AppealResult.REJECTED;
        }
    }

    /** Returns true if an appeal is currently pending. */
    public boolean isAppealPending() {
        return appealSubmittedDay >= 0;
    }

    /** Returns the day the current appeal was submitted (-1 if none). */
    public int getAppealSubmittedDay() {
        return appealSubmittedDay;
    }

    // ── Forged parking ticket crafting ─────────────────────────────────────────

    /**
     * Player crafts a FORGED_PARKING_TICKET at a PHOTOCOPIER_PROP.
     * Requires PAY_AND_DISPLAY_TICKET + BLANK_PAPER, GRAFTING &ge; Apprentice.
     *
     * @param inventory        player's inventory
     * @param graftingTierLevel current GRAFTING tier level (0=Novice, 1=Apprentice, …)
     * @return result of the crafting attempt
     */
    public ForgedCraftResult craftForgedTicket(Inventory inventory, int graftingTierLevel) {
        if (graftingTierLevel < StreetSkillSystem.Tier.APPRENTICE.getLevel()) {
            return ForgedCraftResult.SKILL_TOO_LOW;
        }
        if (!inventory.hasItem(Material.PAY_AND_DISPLAY_TICKET)
                || !inventory.hasItem(Material.BLANK_PAPER)) {
            return ForgedCraftResult.MISSING_MATERIALS;
        }
        inventory.removeItem(Material.PAY_AND_DISPLAY_TICKET, 1);
        inventory.removeItem(Material.BLANK_PAPER, 1);
        inventory.addItem(Material.FORGED_PARKING_TICKET, 1);
        return ForgedCraftResult.CRAFTED;
    }

    // ── Forged ticket sales ────────────────────────────────────────────────────

    /**
     * Player attempts to sell a FORGED_PARKING_TICKET to a commuter NPC.
     *
     * @param inventory          player's inventory
     * @param currentDay         current in-game day number
     * @param px                 player X position (for Clive proximity check)
     * @param py                 player Y position
     * @param pz                 player Z position
     * @param cliveNearby        whether Clive is within detection radius
     * @param achievementCallback callback for achievements
     * @return result of the sale attempt
     */
    public ForgedTicketSaleResult sellForgedTicket(
            Inventory inventory,
            int currentDay,
            float px, float py, float pz,
            boolean cliveNearby,
            AchievementCallback achievementCallback) {

        if (!inventory.hasItem(Material.FORGED_PARKING_TICKET)) {
            return ForgedTicketSaleResult.NO_TICKET;
        }

        // Reset daily counter on new day
        if (currentDay != lastForgedResetDay) {
            forgedTicketsSoldToday = 0;
            lastForgedResetDay = currentDay;
        }

        // Clive nearby: detection chance applies
        if (cliveNearby && random.nextFloat() < FORGED_TICKET_DETECT_CHANCE) {
            inventory.removeItem(Material.FORGED_PARKING_TICKET, 1);
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.PARKING_TICKET_FRAUD);
            }
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(FORGED_DETECT_NOTORIETY, null);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(FORGED_DETECT_WANTED_STARS, px, py, pz,
                        achievementCallback != null ? t -> achievementCallback.award(t) : null);
            }
            return ForgedTicketSaleResult.DETECTED_BY_CLIVE;
        }

        // Successful sale
        inventory.removeItem(Material.FORGED_PARKING_TICKET, 1);
        inventory.addItem(Material.COIN, FORGED_TICKET_SALE_COIN);
        forgedTicketsSoldToday++;

        // Achievement: 5 sales in one day
        if (!ticketToutAwarded && forgedTicketsSoldToday >= TICKET_TOUT_DAILY_THRESHOLD
                && achievementCallback != null) {
            ticketToutAwarded = true;
            achievementCallback.award(AchievementType.TICKET_TOUT);
        }

        return ForgedTicketSaleResult.SOLD;
    }

    // ── State getters ──────────────────────────────────────────────────────────

    /** Returns the current number of parking violations recorded against the player. */
    public int getViolationCount() {
        return violationCount;
    }

    /** Returns whether the player's vehicle is currently clamped. */
    public boolean isVehicleClamped() {
        return vehicleClamped;
    }

    /** Returns the number of forged tickets sold today. */
    public int getForgedTicketsSoldToday() {
        return forgedTicketsSoldToday;
    }

    /** Force-set vehicle clamped state (for testing). */
    public void setVehicleClamped(boolean clamped) {
        this.vehicleClamped = clamped;
    }

    /** Force-set violation count (for testing). */
    public void setViolationCount(int count) {
        this.violationCount = count;
    }

    /** Force-break the machine (for testing). */
    public void setMachineBroken(boolean broken) {
        this.machineIsBroken = broken;
        if (broken) this.machineHp = 0;
    }
}
