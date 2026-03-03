package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.core.NotorietySystem.AchievementCallback;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.Random;

/**
 * Issue #1455: Northfield Dodgy Roofer — Kenny's Round, the Van Raid
 * &amp; the Trading Standards Trap.
 *
 * <h3>Mechanic 1 — Kenny's Round (Weekdays 09:00–16:00)</h3>
 * <ul>
 *   <li>Kenny ({@link NPCType#DODGY_ROOFER}) drives his {@link PropType#ROOFER_VAN_PROP}
 *       to {@link #PROPERTIES_PER_DAY} residential terraced-house stops.</li>
 *   <li>Spends {@link #PITCH_DURATION_SECONDS} at each doorstep pitching fabricated faults
 *       to {@link NPCType#PENSIONER} NPCs ({@link #PENSIONER_ACCEPT_RATE} acceptance).</li>
 *   <li>On acceptance: collects {@link #SCAM_COIN_REWARD} COIN, places
 *       {@link PropType#LADDER_PROP}, 'works' {@link #WORK_DURATION_SECONDS} seconds,
 *       seeds {@link RumourType#KENNY_SCAM} and {@link RumourType#BOTCHED_JOB} rumours.</li>
 *   <li>Van is unattended (break-in window) while Kenny is mid-pitch or on the ladder.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Van Raid</h3>
 * <ul>
 *   <li>Player breaks into {@link PropType#ROOFER_VAN_PROP} with
 *       {@link #VAN_BREAK_IN_HOLD_DURATION}s hold-E while Kenny is mid-pitch (BUSY).</li>
 *   <li>Loot: {@link Material#BUCKET_OF_SEALANT} always, {@link Material#SCAFFOLDING_SPANNER}
 *       50%, {@link Material#INVOICE_PAD} 35%, {@link Material#CASH_ENVELOPE} +
 *       {@link #CASH_ENVELOPE_MIN_COIN}–{@link #CASH_ENVELOPE_MAX_COIN} COIN 20%,
 *       {@link Material#ROOF_SLATE_BAG} 15%.</li>
 *   <li>Kenny spotting the player: HOSTILE + WantedSystem +{@link #VAN_RAID_WANTED_STARS}.</li>
 *   <li>Achievement: {@link AchievementType#TOOLS_DOWN} on first successful raid.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — Rival Round</h3>
 * <ul>
 *   <li>Player with {@link Material#BUCKET_OF_SEALANT} + {@link Material#LATEX_GLOVES}
 *       can cold-call pensioners at residential doors ({@link #RIVAL_ACCEPT_RATE},
 *       {@link #RIVAL_COIN_REWARD} COIN per call).</li>
 *   <li>If Kenny within {@link #TURF_DISPUTE_RADIUS} blocks: {@link RumourType#TURF_DISPUTE}
 *       + Kenny goes HOSTILE.</li>
 *   <li>Achievement: {@link AchievementType#UNDERCUTTING_KENNY} after
 *       {@link #UNDERCUTTING_KENNY_THRESHOLD} undetected calls.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Invoice Fraud</h3>
 * <ul>
 *   <li>Player holding {@link Material#INVOICE_PAD} presses E on a recently worked house
 *       (within {@link #INVOICE_FRAUD_WINDOW_SECONDS} seconds).</li>
 *   <li>{@link #INVOICE_FRAUD_SUCCESS_CHANCE} success → {@link #INVOICE_FRAUD_COIN_REWARD} COIN;
 *       failure → Notoriety +{@link #INVOICE_FRAUD_FAIL_NOTORIETY} +
 *       {@link CrimeType#FRAUD}.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Report to Trading Standards</h3>
 * <ul>
 *   <li>Every Friday a {@link NPCType#TRADING_STANDARDS_OFFICER} checks Kenny's van at
 *       {@link #TRADING_STANDARDS_CHECK_HOUR}:00.</li>
 *   <li>Player can report from PHONE_BOX_HIGH_STREET before
 *       {@link #TRADING_STANDARDS_REPORT_DEADLINE} to guarantee impoundment.</li>
 *   <li>Achievement {@link AchievementType#PUBLIC_SPIRITED} at low notoriety
 *       (≤ {@link #PUBLIC_SPIRITED_NOTORIETY_THRESHOLD}) / {@link AchievementType#CIVIC_MINDED}
 *       on impound. {@link RumourType#SNITCH} makes YOUTH_GANG temporarily hostile.</li>
 * </ul>
 *
 * <h3>Mechanic 6 — Tip Off Kenny</h3>
 * <ul>
 *   <li>Warn Kenny before {@link #TRADING_STANDARDS_REPORT_DEADLINE} Friday → he moves the
 *       van, player earns {@link #TIP_OFF_COIN_REWARD} COIN tip.</li>
 *   <li>Achievement: {@link AchievementType#TIP_OFF_KENNY}.</li>
 * </ul>
 */
public class DodgyRooferSystem {

    // ── Round schedule ────────────────────────────────────────────────────────

    /** Hour Kenny starts his round (09:00). */
    public static final float ROUND_START_HOUR = 9.0f;

    /** Hour Kenny ends his round (16:00). */
    public static final float ROUND_END_HOUR = 16.0f;

    /** Number of residential properties Kenny visits per weekday. */
    public static final int PROPERTIES_PER_DAY = 8;

    /** In-game seconds Kenny spends at each doorstep pitching. */
    public static final float PITCH_DURATION_SECONDS = 45.0f;

    /** In-game seconds Kenny 'works' after acceptance before moving on. */
    public static final float WORK_DURATION_SECONDS = 30.0f;

    /** Probability a PENSIONER accepts Kenny's pitch (0–1). */
    public static final float PENSIONER_ACCEPT_RATE = 0.35f;

    /** COIN Kenny collects from a successful scam. */
    public static final int SCAM_COIN_REWARD = 6;

    // ── Weather offsets ───────────────────────────────────────────────────────

    /** Extra delay (seconds) before Kenny starts in FROST weather. */
    public static final float FROST_DELAY_SECONDS = 30f * 60f; // 30 in-game minutes

    // ── Mechanic 2 — Van Raid ─────────────────────────────────────────────────

    /** Hold-E duration (seconds) required to break into ROOFER_VAN_PROP. */
    public static final float VAN_BREAK_IN_HOLD_DURATION = 3.0f;

    /** Notoriety added on van break-in. */
    public static final int VAN_RAID_NOTORIETY = 6;

    /** WantedSystem stars added on van break-in. */
    public static final int VAN_RAID_WANTED_STARS = 1;

    /** Probability of SCAFFOLDING_SPANNER in van loot (0–1). */
    public static final float VAN_LOOT_SPANNER_CHANCE = 0.50f;

    /** Probability of INVOICE_PAD in van loot (0–1). */
    public static final float VAN_LOOT_INVOICE_CHANCE = 0.35f;

    /** Probability of CASH_ENVELOPE in van loot (0–1). */
    public static final float VAN_LOOT_CASH_CHANCE = 0.20f;

    /** Probability of ROOF_SLATE_BAG in van loot (0–1). */
    public static final float VAN_LOOT_SLATES_CHANCE = 0.15f;

    /** Minimum COIN in the CASH_ENVELOPE. */
    public static final int CASH_ENVELOPE_MIN_COIN = 10;

    /** Maximum COIN in the CASH_ENVELOPE (inclusive). */
    public static final int CASH_ENVELOPE_MAX_COIN = 20;

    // ── Mechanic 3 — Rival Round ──────────────────────────────────────────────

    /** Probability a PENSIONER accepts the player's rival pitch (0–1). */
    public static final float RIVAL_ACCEPT_RATE = 0.40f;

    /** COIN awarded per successful rival cold-call. */
    public static final int RIVAL_COIN_REWARD = 4;

    /** Radius (blocks) within which Kenny triggers TURF_DISPUTE if player is cold-calling. */
    public static final float TURF_DISPUTE_RADIUS = 20.0f;

    /** Number of undetected rival calls needed for UNDERCUTTING_KENNY achievement. */
    public static final int UNDERCUTTING_KENNY_THRESHOLD = 6;

    // ── Mechanic 4 — Invoice Fraud ────────────────────────────────────────────

    /** Success chance (0–1) when player uses INVOICE_PAD on a recently worked house. */
    public static final float INVOICE_FRAUD_SUCCESS_CHANCE = 0.60f;

    /** COIN reward on successful invoice fraud. */
    public static final int INVOICE_FRAUD_COIN_REWARD = 3;

    /** Notoriety penalty on failed invoice fraud attempt. */
    public static final int INVOICE_FRAUD_FAIL_NOTORIETY = 5;

    /** Seconds after Kenny works a house during which the invoice window is open. */
    public static final float INVOICE_FRAUD_WINDOW_SECONDS = 24f * 3600f; // 1 in-game day

    // ── Mechanic 5 — Trading Standards ───────────────────────────────────────

    /** In-game hour at which Trading Standards checks Kenny's van (Friday 11:00). */
    public static final float TRADING_STANDARDS_CHECK_HOUR = 11.0f;

    /** Deadline (hour) before which the player must report/tip-off (10:50). */
    public static final float TRADING_STANDARDS_REPORT_DEADLINE = 10.8333f; // 10:50

    /** Notoriety threshold for PUBLIC_SPIRITED achievement (report at low notoriety). */
    public static final int PUBLIC_SPIRITED_NOTORIETY_THRESHOLD = 25;

    // ── Mechanic 6 — Tip Off ─────────────────────────────────────────────────

    /** COIN reward Kenny pays for a Friday tip-off. */
    public static final int TIP_OFF_COIN_REWARD = 10;

    // ── Speech lines ──────────────────────────────────────────────────────────

    public static final String KENNY_PITCH        = "Now love, I was just walking past and I couldn't help noticing — those ridge tiles are shot.";
    public static final String KENNY_HOSTILE       = "Oi! Get away from my van! I'm calling the police!";
    public static final String KENNY_TIP_OFF_ACK   = "Cheers mate. I'll have the van round the back. There's a tenner in it for ya.";
    public static final String KENNY_TURF_DISPUTE  = "What do you think you're playing at? This is MY patch!";

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to break into Kenny's ROOFER_VAN_PROP. */
    public enum VanRaidResult {
        /** Loot collected; VEHICLE_BREAK_IN recorded. */
        LOOTED,
        /** Kenny spotted the player; player HOSTILE-flagged. */
        CAUGHT_BY_KENNY,
        /** Van is not currently unattended (Kenny not mid-pitch/work). */
        VAN_NOT_UNATTENDED,
        /** Van has already been raided this stop. */
        ALREADY_RAIDED
    }

    /** Result of a rival cold-call attempt. */
    public enum RivalCallResult {
        /** Householder accepted — COIN awarded. */
        ACCEPTED,
        /** Householder refused. */
        REFUSED,
        /** Kenny spotted the player; TURF_DISPUTE seeded; Kenny HOSTILE. */
        CAUGHT_BY_KENNY,
        /** Player lacks BUCKET_OF_SEALANT or LATEX_GLOVES. */
        MISSING_ITEMS
    }

    /** Result of invoice fraud attempt. */
    public enum InvoiceFraudResult {
        /** Success — COIN awarded. */
        SUCCESS,
        /** Failure — Notoriety penalty + FRAUD recorded. */
        FAILED,
        /** Player does not have an INVOICE_PAD. */
        NO_INVOICE_PAD,
        /** No recently worked house at this location. */
        NO_RECENT_WORK
    }

    /** Result of reporting to Trading Standards. */
    public enum TradingStandardsReportResult {
        /** Reported; van will be impounded at the Friday check. */
        REPORTED,
        /** Too late — past {@link #TRADING_STANDARDS_REPORT_DEADLINE}. */
        TOO_LATE,
        /** Not a Friday, or Trading Standards not yet due. */
        NOT_FRIDAY
    }

    /** Result of tipping off Kenny before the Trading Standards visit. */
    public enum TipOffResult {
        /** Kenny tipped; van moved; player rewarded. */
        TIPPED,
        /** Too late — past {@link #TRADING_STANDARDS_REPORT_DEADLINE}. */
        TOO_LATE,
        /** Not a Friday. */
        NOT_FRIDAY,
        /** Kenny already tipped this week. */
        ALREADY_TIPPED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether Kenny's round is currently active. */
    private boolean roundActive = false;

    /** Current property index (0–7). */
    private int currentPropertyIndex = 0;

    /** Seconds Kenny has spent at the current property in PITCH phase. */
    private float pitchTimer = 0f;

    /** Seconds Kenny has spent working at the current property (after acceptance). */
    private float workTimer = 0f;

    /** Whether Kenny is in the WORK phase (accepted pitch, ladder placed). */
    private boolean kennyWorking = false;

    /** Whether the pensioner at the current stop accepted Kenny's pitch. */
    private boolean pitchAccepted = false;

    /** Whether the van is currently unattended (breakable). */
    private boolean vanUnattended = false;

    /** Whether the van has been raided at the current stop. */
    private boolean vanRaidedThisStop = false;

    /** Whether there is a recently worked house available for invoice fraud. */
    private boolean invoiceWindowOpen = false;

    /** Countdown timer for the invoice fraud window (seconds). */
    private float invoiceWindowTimer = 0f;

    /** Whether Kenny is currently hostile to the player. */
    private boolean kennyHostile = false;

    /** Countdown (seconds) on Kenny's hostile state. */
    private float kennyHostileTimer = 0f;

    /** Whether the player has reported Kenny to Trading Standards this Friday. */
    private boolean tradingStandardsReported = false;

    /** Whether the van has been impounded by Trading Standards. */
    private boolean vanImpounded = false;

    /** Whether Kenny has been tipped off this Friday. */
    private boolean kennyTippedOff = false;

    /** Whether the van has been moved (after tip-off). */
    private boolean vanMoved = false;

    /** Number of undetected rival cold-calls by the player. */
    private int rivalCallsUndetected = 0;

    /** Whether this is a FROST-delayed start (van moves 30 min late). */
    private boolean frostDelayApplied = false;

    // ── Achievement flags ─────────────────────────────────────────────────────

    private boolean toolsDownAwarded = false;
    private boolean undercuttingKennyAwarded = false;
    private boolean publicSpiritedAwarded = false;
    private boolean civicMindedAwarded = false;
    private boolean tipOffKennyAwarded = false;

    // ── Testing hooks ─────────────────────────────────────────────────────────

    /** Force Kenny into van-unattended state for testing. */
    private boolean vanUnattendedOverride = false;

    // ── Optional integrations ─────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private WeatherSystem weatherSystem;

    // ── Construction ──────────────────────────────────────────────────────────

    public DodgyRooferSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ──────────────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem ns)   { this.notorietySystem = ns; }
    public void setWantedSystem(WantedSystem ws)         { this.wantedSystem = ws; }
    public void setCriminalRecord(CriminalRecord cr)     { this.criminalRecord = cr; }
    public void setRumourNetwork(RumourNetwork rn)       { this.rumourNetwork = rn; }
    public void setWeatherSystem(WeatherSystem ws)       { this.weatherSystem = ws; }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Update the dodgy roofer system each frame.
     *
     * @param delta          seconds since last frame
     * @param hour           current in-game hour (0–24)
     * @param dayOfWeek      0=Sunday, 1=Monday … 6=Saturday
     * @param nearbyNpc      nearest NPC witness (or null)
     * @param cb             achievement callback (may be null)
     */
    public void update(float delta, float hour, int dayOfWeek,
                       NPC nearbyNpc, AchievementCallback cb) {

        // Tick hostile timer
        if (kennyHostile && kennyHostileTimer > 0f) {
            kennyHostileTimer -= delta;
            if (kennyHostileTimer <= 0f) {
                kennyHostileTimer = 0f;
                kennyHostile = false;
            }
        }

        // Tick invoice window
        if (invoiceWindowOpen && invoiceWindowTimer > 0f) {
            invoiceWindowTimer -= delta;
            if (invoiceWindowTimer <= 0f) {
                invoiceWindowTimer = 0f;
                invoiceWindowOpen = false;
            }
        }

        // Determine effective round start (with frost delay)
        float effectiveStartHour = ROUND_START_HOUR;
        if (weatherSystem != null
                && weatherSystem.getCurrentWeather() != null
                && weatherSystem.getCurrentWeather().causesFrost()) {
            effectiveStartHour = ROUND_START_HOUR + (FROST_DELAY_SECONDS / 3600f);
        }

        boolean isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;
        boolean isRaining = weatherSystem != null
                && weatherSystem.getCurrentWeather() != null
                && weatherSystem.getCurrentWeather().isRaining();

        boolean shouldBeActive = isWeekday
                && hour >= effectiveStartHour
                && hour < ROUND_END_HOUR
                && !isRaining
                && !vanImpounded;

        if (!shouldBeActive) {
            if (roundActive) {
                endRound();
            }
            return;
        }

        if (!roundActive) {
            startRound();
        }

        // Advance pitch or work timer
        if (kennyWorking) {
            workTimer += delta;
            vanUnattended = true;
            if (workTimer >= WORK_DURATION_SECONDS) {
                workTimer = 0f;
                kennyWorking = false;
                vanUnattended = false;
                // Seed botched-job rumour
                if (rumourNetwork != null && nearbyNpc != null) {
                    rumourNetwork.addRumour(nearbyNpc,
                            new Rumour(RumourType.BOTCHED_JOB,
                                    "That guttering job on Oak Avenue. Fell off within a week. Right cowboy outfit."));
                }
                // Open invoice fraud window
                invoiceWindowOpen = true;
                invoiceWindowTimer = INVOICE_FRAUD_WINDOW_SECONDS;
                advanceToNextProperty(nearbyNpc, cb);
            }
        } else {
            pitchTimer += delta;
            vanUnattended = true; // van is always unattended during pitch
            if (pitchTimer >= PITCH_DURATION_SECONDS) {
                pitchTimer = 0f;
                resolvePitch(nearbyNpc, cb);
            }
        }
    }

    private void startRound() {
        roundActive = true;
        currentPropertyIndex = 0;
        pitchTimer = 0f;
        workTimer = 0f;
        kennyWorking = false;
        pitchAccepted = false;
        vanUnattended = false;
        vanRaidedThisStop = false;
        frostDelayApplied = false;
    }

    private void endRound() {
        roundActive = false;
        vanUnattended = false;
        kennyWorking = false;
        // Reset Friday flags at end of Friday round
    }

    private void resolvePitch(NPC nearbyNpc, AchievementCallback cb) {
        pitchAccepted = random.nextFloat() < PENSIONER_ACCEPT_RATE;
        if (pitchAccepted) {
            // Seed KENNY_SCAM rumour
            if (rumourNetwork != null && nearbyNpc != null) {
                rumourNetwork.addRumour(nearbyNpc,
                        new Rumour(RumourType.KENNY_SCAM,
                                "That roofer's been at it again — told old Mrs Patel her ridge tiles were shot."));
            }
            // Start work phase
            kennyWorking = true;
            workTimer = 0f;
        } else {
            // No acceptance — move on
            advanceToNextProperty(nearbyNpc, cb);
        }
    }

    private void advanceToNextProperty(NPC nearbyNpc, AchievementCallback cb) {
        vanRaidedThisStop = false;
        pitchAccepted = false;
        vanUnattended = false;
        currentPropertyIndex++;
        if (currentPropertyIndex >= PROPERTIES_PER_DAY) {
            endRound();
        } else {
            pitchTimer = 0f;
            workTimer = 0f;
            kennyWorking = false;
        }
    }

    // ── Mechanic 2 — Van Raid ─────────────────────────────────────────────────

    /**
     * Player attempts to break into Kenny's ROOFER_VAN_PROP.
     *
     * <p>Called after the player has held E for {@link #VAN_BREAK_IN_HOLD_DURATION}
     * seconds next to the van while it is unattended.
     *
     * @param inventory          player's inventory (items added on success)
     * @param kennyDistToPlayer  distance from Kenny to the player (blocks)
     * @param witnessNpc         NPC witness for rumour seeding (or null)
     * @param cb                 achievement callback
     * @return result of the van raid attempt
     */
    public VanRaidResult raidVan(Inventory inventory, float kennyDistToPlayer,
                                  NPC witnessNpc, AchievementCallback cb) {
        if (!isVanUnattended()) return VanRaidResult.VAN_NOT_UNATTENDED;
        if (vanRaidedThisStop) return VanRaidResult.ALREADY_RAIDED;

        // Check if Kenny spots the player (within hostile radius during work)
        if (kennyDistToPlayer <= 8.0f && kennyWorking) {
            triggerKennyHostile();
            if (wantedSystem != null) {
                wantedSystem.increaseWantedStars(VAN_RAID_WANTED_STARS);
            }
            if (witnessNpc != null && rumourNetwork != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.KENNY_ARGUMENT,
                                "Kenny had a right go at some lad on the street. Caught nicking out of his van."));
            }
            return VanRaidResult.CAUGHT_BY_KENNY;
        }

        // Successful raid
        vanRaidedThisStop = true;

        // Always get BUCKET_OF_SEALANT
        inventory.addItem(Material.BUCKET_OF_SEALANT, 1);

        // 50% SCAFFOLDING_SPANNER
        if (random.nextFloat() < VAN_LOOT_SPANNER_CHANCE) {
            inventory.addItem(Material.SCAFFOLDING_SPANNER, 1);
        }

        // 35% INVOICE_PAD
        if (random.nextFloat() < VAN_LOOT_INVOICE_CHANCE) {
            inventory.addItem(Material.INVOICE_PAD, 1);
        }

        // 20% CASH_ENVELOPE + coin
        if (random.nextFloat() < VAN_LOOT_CASH_CHANCE) {
            inventory.addItem(Material.CASH_ENVELOPE, 1);
            int coinAmount = CASH_ENVELOPE_MIN_COIN
                    + random.nextInt(CASH_ENVELOPE_MAX_COIN - CASH_ENVELOPE_MIN_COIN + 1);
            inventory.addItem(Material.COIN, coinAmount);
        }

        // 15% ROOF_SLATE_BAG
        if (random.nextFloat() < VAN_LOOT_SLATES_CHANCE) {
            inventory.addItem(Material.ROOF_SLATE_BAG, 1);
        }

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.VEHICLE_BREAK_IN);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(VAN_RAID_NOTORIETY, cb);
        }
        if (wantedSystem != null) {
            wantedSystem.increaseWantedStars(VAN_RAID_WANTED_STARS);
        }

        // Award achievement on first raid
        if (!toolsDownAwarded && cb != null) {
            toolsDownAwarded = true;
            cb.award(AchievementType.TOOLS_DOWN);
        }

        return VanRaidResult.LOOTED;
    }

    // ── Mechanic 3 — Rival Round ──────────────────────────────────────────────

    /**
     * Player attempts a rival cold-call at a residential doorstep.
     *
     * @param inventory          player's inventory (must have BUCKET_OF_SEALANT + LATEX_GLOVES)
     * @param kennyDistToPlayer  distance from Kenny to the player (blocks)
     * @param witnessNpc         NPC witness for rumour seeding (or null)
     * @param cb                 achievement callback
     * @return result of the rival call attempt
     */
    public RivalCallResult attemptRivalCall(Inventory inventory, float kennyDistToPlayer,
                                             NPC witnessNpc, AchievementCallback cb) {
        if (inventory.getItemCount(Material.BUCKET_OF_SEALANT) < 1
                || inventory.getItemCount(Material.LATEX_GLOVES) < 1) {
            return RivalCallResult.MISSING_ITEMS;
        }

        // Seed RIVAL_ROOFER rumour
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.RIVAL_ROOFER,
                            "Someone's been going round doing dodgy roofing work. Undercutting the bloke in the van."));
        }

        // Check if Kenny is nearby → TURF_DISPUTE
        if (kennyDistToPlayer <= TURF_DISPUTE_RADIUS) {
            triggerKennyHostile();
            if (rumourNetwork != null && witnessNpc != null) {
                rumourNetwork.addRumour(witnessNpc,
                        new Rumour(RumourType.TURF_DISPUTE,
                                "Two roofers on the same street. One of them's going to kick off soon."));
            }
            return RivalCallResult.CAUGHT_BY_KENNY;
        }

        // Roll for acceptance
        if (random.nextFloat() < RIVAL_ACCEPT_RATE) {
            inventory.addItem(Material.COIN, RIVAL_COIN_REWARD);
            rivalCallsUndetected++;
            checkUndercuttingKenny(cb);
            return RivalCallResult.ACCEPTED;
        } else {
            return RivalCallResult.REFUSED;
        }
    }

    private void checkUndercuttingKenny(AchievementCallback cb) {
        if (!undercuttingKennyAwarded
                && rivalCallsUndetected >= UNDERCUTTING_KENNY_THRESHOLD
                && cb != null) {
            undercuttingKennyAwarded = true;
            cb.award(AchievementType.UNDERCUTTING_KENNY);
        }
    }

    // ── Mechanic 4 — Invoice Fraud ────────────────────────────────────────────

    /**
     * Player presses E on a recently worked house with INVOICE_PAD in hand.
     *
     * @param inventory   player's inventory
     * @param witnessNpc  NPC witness (or null)
     * @param cb          achievement callback
     * @return result of the fraud attempt
     */
    public InvoiceFraudResult attemptInvoiceFraud(Inventory inventory,
                                                   NPC witnessNpc,
                                                   AchievementCallback cb) {
        if (inventory.getItemCount(Material.INVOICE_PAD) < 1) {
            return InvoiceFraudResult.NO_INVOICE_PAD;
        }
        if (!invoiceWindowOpen) {
            return InvoiceFraudResult.NO_RECENT_WORK;
        }

        if (random.nextFloat() < INVOICE_FRAUD_SUCCESS_CHANCE) {
            inventory.addItem(Material.COIN, INVOICE_FRAUD_COIN_REWARD);
            return InvoiceFraudResult.SUCCESS;
        } else {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(INVOICE_FRAUD_FAIL_NOTORIETY, cb);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CrimeType.FRAUD);
            }
            return InvoiceFraudResult.FAILED;
        }
    }

    // ── Mechanic 5 — Report to Trading Standards ──────────────────────────────

    /**
     * Player reports Kenny to Trading Standards from the phone box.
     *
     * @param hour           current in-game hour
     * @param dayOfWeek      0=Sunday … 6=Saturday (5=Friday)
     * @param playerNotoriety raw player notoriety score
     * @param witnessNpc     NPC witness for rumour seeding (or null)
     * @param cb             achievement callback
     * @return result of the report attempt
     */
    public TradingStandardsReportResult reportToTradingStandards(
            float hour, int dayOfWeek, int playerNotoriety,
            NPC witnessNpc, AchievementCallback cb) {

        if (dayOfWeek != 5) return TradingStandardsReportResult.NOT_FRIDAY;
        if (hour >= TRADING_STANDARDS_REPORT_DEADLINE) return TradingStandardsReportResult.TOO_LATE;

        tradingStandardsReported = true;

        // Seed KENNY_REPORTED rumour
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.KENNY_REPORTED,
                            "Someone reported that van parked on Northfield Road to Trading Standards. About time."));
        }

        // Seed SNITCH rumour (makes YOUTH_GANG hostile)
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.SNITCH,
                            "Someone's been talking to Trading Standards. Word gets round."));
        }

        // Award PUBLIC_SPIRITED at low notoriety
        if (playerNotoriety <= PUBLIC_SPIRITED_NOTORIETY_THRESHOLD && !publicSpiritedAwarded && cb != null) {
            publicSpiritedAwarded = true;
            cb.award(AchievementType.PUBLIC_SPIRITED);
        }

        return TradingStandardsReportResult.REPORTED;
    }

    /**
     * Called when the Trading Standards Officer arrives at the van on Friday
     * ({@link #TRADING_STANDARDS_CHECK_HOUR}). If a report was filed, the van is impounded.
     *
     * @param witnessNpc NPC witness (or null)
     * @param cb         achievement callback
     * @return true if the van was impounded
     */
    public boolean processTradingStandardsCheck(NPC witnessNpc, AchievementCallback cb) {
        if (!tradingStandardsReported) return false;

        vanImpounded = true;
        roundActive = false;

        // Seed KENNY_FINED rumour
        if (rumourNetwork != null && witnessNpc != null) {
            rumourNetwork.addRumour(witnessNpc,
                    new Rumour(RumourType.KENNY_FINED,
                            "Trading Standards had Kenny's van away. He was doing his nut outside the chippy."));
        }

        // Award CIVIC_MINDED
        if (!civicMindedAwarded && cb != null) {
            civicMindedAwarded = true;
            cb.award(AchievementType.CIVIC_MINDED);
        }

        return true;
    }

    // ── Mechanic 6 — Tip Off Kenny ────────────────────────────────────────────

    /**
     * Player warns Kenny about the upcoming Trading Standards visit.
     *
     * @param inventory  player's inventory (COIN added on success)
     * @param hour       current in-game hour
     * @param dayOfWeek  0=Sunday … 6=Saturday (5=Friday)
     * @param cb         achievement callback
     * @return result of the tip-off attempt
     */
    public TipOffResult tipOffKenny(Inventory inventory, float hour,
                                    int dayOfWeek, AchievementCallback cb) {
        if (dayOfWeek != 5) return TipOffResult.NOT_FRIDAY;
        if (hour >= TRADING_STANDARDS_REPORT_DEADLINE) return TipOffResult.TOO_LATE;
        if (kennyTippedOff) return TipOffResult.ALREADY_TIPPED;

        kennyTippedOff = true;
        vanMoved = true;

        inventory.addItem(Material.COIN, TIP_OFF_COIN_REWARD);

        if (!tipOffKennyAwarded && cb != null) {
            tipOffKennyAwarded = true;
            cb.award(AchievementType.TIP_OFF_KENNY);
        }

        return TipOffResult.TIPPED;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void triggerKennyHostile() {
        kennyHostile = true;
        kennyHostileTimer = 24f * 3600f; // hostile for remainder of in-game day
    }

    // ── Getters (for tests and UI) ────────────────────────────────────────────

    public boolean isRoundActive()              { return roundActive; }
    public boolean isKennyWorking()             { return kennyWorking; }
    public boolean isVanUnattended()            { return vanUnattended || vanUnattendedOverride; }
    public boolean isVanRaidedThisStop()        { return vanRaidedThisStop; }
    public boolean isKennyHostile()             { return kennyHostile; }
    public boolean isPitchAccepted()            { return pitchAccepted; }
    public boolean isInvoiceWindowOpen()        { return invoiceWindowOpen; }
    public boolean isTradingStandardsReported() { return tradingStandardsReported; }
    public boolean isVanImpounded()             { return vanImpounded; }
    public boolean isKennyTippedOff()           { return kennyTippedOff; }
    public boolean isVanMoved()                 { return vanMoved; }
    public int     getCurrentPropertyIndex()    { return currentPropertyIndex; }
    public int     getRivalCallsUndetected()    { return rivalCallsUndetected; }
    public float   getPitchTimer()              { return pitchTimer; }
    public float   getWorkTimer()               { return workTimer; }

    /** Testing hook: force van into unattended state. */
    public void setVanUnattendedForTesting(boolean unattended) {
        this.vanUnattendedOverride = unattended;
    }

    /** Testing hook: force the invoice window open. */
    public void setInvoiceWindowOpenForTesting(boolean open) {
        this.invoiceWindowOpen = open;
        if (open) this.invoiceWindowTimer = INVOICE_FRAUD_WINDOW_SECONDS;
    }

    /** Testing hook: set round active without resetting state. */
    public void setRoundActiveForTesting(boolean active) {
        this.roundActive = active;
    }

    /** Testing hook: force Kenny into the working phase. */
    public void setKennyWorkingForTesting(boolean working) {
        this.kennyWorking = working;
        this.vanUnattended = working;
        this.pitchAccepted = working;
    }

    /** Testing hook: reset van impound state. */
    public void resetVanImpoundedForTesting() {
        this.vanImpounded = false;
        this.tradingStandardsReported = false;
        this.kennyTippedOff = false;
        this.vanMoved = false;
    }
}
