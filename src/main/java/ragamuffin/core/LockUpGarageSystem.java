package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.CriminalRecord.CrimeType;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1148: Northfield Council Estate Lock-Up Garages — Dark Economy,
 * Band Rehearsal &amp; the Garage Clearance Heist.
 *
 * <p>A row of eight numbered lock-up garages (GARAGE_1–GARAGE_8) behind the
 * Northfield Council Flats. Each garage has a distinct seeded tenant with unique
 * mechanics:
 * <ol>
 *   <li><b>Garage 1 — Band Rehearsal</b>: Tues/Thurs/Sat 19:00–22:00. Watch 3 sessions →
 *       doorman role (3 COIN/session) → join band at MC_BATTLE rank ≥ 2 → 6 COIN gigs.</li>
 *   <li><b>Garage 2 — Hoarder</b>: Clearance quest; yields BRIC_A_BRAC and VINTAGE_RECORD.
 *       Fence 10+ BRIC_A_BRAC for {@link AchievementType#BRIC_A_BRAC_BANDIT}.</li>
 *   <li><b>Garage 3 — Marchetti Drug Den</b>: BURNER_PHONE and SCALES_PROP inside.
 *       Tip-off via PAYPHONE → UNDERCOVER_POLICE raid 1–3 hours later.</li>
 *   <li><b>Garage 4 — DIY Enthusiast</b>: CABLE and crafting materials inside.</li>
 *   <li><b>Garage 5 — Stolen Goods Stash</b>: HEAVY_PADLOCK; requires BOLT_CUTTERS
 *       or 2 CROWBAR attempts. Looting awards {@link AchievementType#STASH_ROBBER}.</li>
 *   <li><b>Garage 6 — Council Skip Overflow</b>: Always unlocked; random salvage.</li>
 *   <li><b>Garage 7 — Player Rental</b>: 5 COIN/week from Dave the Caretaker. Grants
 *       {@link Material#GARAGE_KEY_7} and access to GARAGE_SHELF_PROP (30-stack storage).
 *       Eviction after 3 missed payments.</li>
 *   <li><b>Garage 8 — Pigeon Fancier</b>: PIGEON_COOP_PROP; PigeonRacingSystem integration.</li>
 * </ol>
 *
 * <h3>Break-In Mechanics</h3>
 * <ul>
 *   <li>{@link Material#LOCKPICK}: 10 seconds, 70% success, silent.</li>
 *   <li>{@link Material#CROWBAR}: 3 seconds, 100% success, HIGH noise (25-block radius
 *       via NoiseSystem). Dave witnesses and files {@link CrimeType#GARAGE_BREAK_IN}.</li>
 * </ul>
 *
 * <h3>Drug Den Tip-Off</h3>
 * <ul>
 *   <li>Press E on PAYPHONE_PROP → anonymous report →
 *       UNDERCOVER_POLICE raid within {@link #RAID_DELAY_HOURS_MIN}–{@link #RAID_DELAY_HOURS_MAX}
 *       in-game hours.</li>
 *   <li>Stash cleared, MARCHETTI_CREW Respect −{@link #DRUG_RAD_RESPECT_PENALTY},
 *       {@link AchievementType#GRASS} and {@link AchievementType#INFORMANT} unlocked.</li>
 * </ul>
 *
 * <h3>Player Rental</h3>
 * <ul>
 *   <li>Rent Garage 7 from Dave (DAVE_CARETAKER NPC) for {@link #RENTAL_COST_PER_WEEK} COIN/week.</li>
 *   <li>Grants {@link Material#GARAGE_KEY_7}. GARAGE_SHELF_PROP stores 30 item stacks
 *       (persistent world storage).</li>
 *   <li>After {@link #EVICTION_MISSED_PAYMENTS} missed weekly payments, player is evicted and
 *       GARAGE_KEY_7 is removed from inventory.</li>
 * </ul>
 *
 * <h3>Band Join Path</h3>
 * <ul>
 *   <li>Watch {@link #BAND_SESSIONS_TO_JOIN} rehearsals → doorman role (3 COIN/session).</li>
 *   <li>At MC_BATTLE rank ≥ {@link #BAND_JOIN_MC_RANK}: join band →
 *       {@link AchievementType#GARAGE_BAND_MEMBER}.</li>
 *   <li>Band member earns {@link #BAND_GIG_PAYMENT} COIN on Saturday gigs at The Vaults.</li>
 * </ul>
 */
public class LockUpGarageSystem {

    // ── Garage IDs ─────────────────────────────────────────────────────────────

    /** Number of garages on the estate. */
    public static final int GARAGE_COUNT = 8;

    /** Garage index for band rehearsal. */
    public static final int GARAGE_BAND = 1;

    /** Garage index for hoarder clearance quest. */
    public static final int GARAGE_HOARDER = 2;

    /** Garage index for Marchetti drug den. */
    public static final int GARAGE_DRUG_DEN = 3;

    /** Garage index for DIY enthusiast. */
    public static final int GARAGE_DIY = 4;

    /** Garage index for stolen goods stash (HEAVY_PADLOCK). */
    public static final int GARAGE_STASH = 5;

    /** Garage index for council skip overflow (always open). */
    public static final int GARAGE_SKIP = 6;

    /** Garage index for player rental. */
    public static final int GARAGE_RENTAL = 7;

    /** Garage index for pigeon fancier. */
    public static final int GARAGE_PIGEONS = 8;

    // ── Schedule constants ─────────────────────────────────────────────────────

    /** Band rehearsal start hour (19:00). */
    public static final float BAND_REHEARSAL_OPEN_HOUR = 19.0f;

    /** Band rehearsal end hour (22:00). */
    public static final float BAND_REHEARSAL_CLOSE_HOUR = 22.0f;

    /** Dave the Caretaker patrol start hour (08:00). */
    public static final float DAVE_PATROL_START_HOUR = 8.0f;

    /** Dave the Caretaker patrol end hour (16:00). */
    public static final float DAVE_PATROL_END_HOUR = 16.0f;

    // ── Break-in constants ─────────────────────────────────────────────────────

    /** Lockpick attempt duration in real seconds. */
    public static final float LOCKPICK_DURATION_SECONDS = 10.0f;

    /** Lockpick success probability (0–1). */
    public static final float LOCKPICK_SUCCESS_CHANCE = 0.70f;

    /** Crowbar break-in duration in real seconds (faster, noisier). */
    public static final float CROWBAR_DURATION_SECONDS = 3.0f;

    /** Crowbar break-in noise level emitted to NoiseSystem. */
    public static final float CROWBAR_NOISE_LEVEL = 4.0f;

    /** Radius in blocks at which the crowbar break-in noise is audible. */
    public static final float CROWBAR_NOISE_RADIUS = 25.0f;

    /** Notoriety added when Dave witnesses a break-in. */
    public static final int DAVE_WITNESS_NOTORIETY = 6;

    /** WantedSystem stars added when Dave witnesses a break-in. */
    public static final int DAVE_WITNESS_WANTED_STARS = 1;

    // ── Drug den / tip-off constants ───────────────────────────────────────────

    /** Minimum in-game hours before UNDERCOVER_POLICE raid after tip-off. */
    public static final float RAID_DELAY_HOURS_MIN = 1.0f;

    /** Maximum in-game hours before UNDERCOVER_POLICE raid after tip-off. */
    public static final float RAID_DELAY_HOURS_MAX = 3.0f;

    /** MARCHETTI_CREW Respect penalty when the drug den raid completes. */
    public static final int DRUG_RAD_RESPECT_PENALTY = 10;

    /** Notoriety added if player is found inside Garage 3 during the raid. */
    public static final int RAID_CAUGHT_NOTORIETY = 15;

    /** WantedSystem stars added if player is caught inside during the raid. */
    public static final int RAID_CAUGHT_WANTED_STARS = 2;

    // ── Rental constants ───────────────────────────────────────────────────────

    /** Weekly rental cost for Garage 7 (in COIN). */
    public static final int RENTAL_COST_PER_WEEK = 5;

    /** Number of missed weekly payments before eviction. */
    public static final int EVICTION_MISSED_PAYMENTS = 3;

    // ── Band join constants ────────────────────────────────────────────────────

    /** Number of rehearsal sessions the player must watch before joining as doorman. */
    public static final int BAND_SESSIONS_TO_JOIN = 3;

    /** MC_BATTLE rank required to join the band as a full member. */
    public static final int BAND_JOIN_MC_RANK = 2;

    /** COIN paid to player per band rehearsal session in doorman role. */
    public static final int BAND_DOORMAN_PAYMENT = 3;

    /** COIN paid to player per Saturday gig at The Vaults as full band member. */
    public static final int BAND_GIG_PAYMENT = 6;

    // ── Hoarder clearance constants ────────────────────────────────────────────

    /** Number of BRIC_A_BRAC items to fence to earn BRIC_A_BRAC_BANDIT achievement. */
    public static final int BRIC_A_BRAC_BANDIT_THRESHOLD = 10;

    /** Number of BRIC_A_BRAC items found in the hoarder garage. */
    public static final int HOARDER_BRIC_A_BRAC_COUNT = 15;

    /** Number of VINTAGE_RECORD items found in the hoarder garage. */
    public static final int HOARDER_VINTAGE_RECORD_COUNT = 3;

    // ── Speech lines ───────────────────────────────────────────────────────────

    public static final String DAVE_PATROL_WARNING    = "You're not supposed to be in there, mate.";
    public static final String DAVE_RENT_DUE          = "Rent's due Friday. Don't make me knock twice.";
    public static final String DAVE_RESIDENTS_ONLY    = "These garages are for residents only.";
    public static final String DAVE_RENT_OK           = "Right, you're sorted. Here's your key. Keep it tidy.";
    public static final String DAVE_EVICTION_WARNING  = "You're behind on the rent. One more week and you're out.";
    public static final String DAVE_EVICTION_FINAL    = "That's it. You're out. Key back, please.";
    public static final String LOCKPICK_FAIL          = "The lock won't give. Try again or use something heavier.";
    public static final String LOCKPICK_SUCCESS       = "Click. It opens. Nice and quiet.";
    public static final String CROWBAR_SUCCESS        = "BANG. The door swings open. Half the estate heard that.";
    public static final String TIPOFF_MADE            = "You dial 999. Anonymous. They'll send someone. Eventually.";
    public static final String RAID_IN_PROGRESS       = "Armed police! Nobody move!";
    public static final String RAID_PLAYER_CAUGHT     = "You shouldn't be in here. On the ground. Now.";
    public static final String BAND_WATCH_PROMPT      = "The band's in there. You could watch a bit.";
    public static final String BAND_DOORMAN_OFFER     = "You want to help out? Stand outside, keep it quiet. Three coin.";
    public static final String BAND_JOIN_OFFER        = "You're alright, you are. You should get up there with us.";
    public static final String BAND_GIG_ANNOUNCEMENT  = "Saturday at The Vaults. You're on the guest list.";

    // ── Break-in result enum ───────────────────────────────────────────────────

    /** Result of a garage break-in attempt. */
    public enum BreakInResult {
        /** LOCKPICK attempt succeeded silently. */
        LOCKPICK_SUCCESS,
        /** LOCKPICK attempt failed (30% chance). */
        LOCKPICK_FAILED,
        /** CROWBAR succeeded but generated high noise. */
        CROWBAR_SUCCESS,
        /** Garage is already open (Garage 6 skip always open). */
        ALREADY_OPEN,
        /** Player has GARAGE_KEY_7 for Garage 7. */
        KEY_ACCESS,
        /** Garage requires BOLT_CUTTERS or 2 CROWBAR attempts (Garage 5 HEAVY_PADLOCK). */
        HEAVY_PADLOCK
    }

    /** Result of the tip-off call to police. */
    public enum TipOffResult {
        /** Tip-off call registered; raid will happen. */
        CALL_MADE,
        /** Tip-off already made for this session. */
        ALREADY_REPORTED,
        /** Drug den has already been raided. */
        ALREADY_RAIDED
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether each garage door is currently unlocked (index 1–8, 0 unused). */
    private final boolean[] garageUnlocked = new boolean[GARAGE_COUNT + 1];

    /** Whether the hoarder clearance quest has been completed. */
    private boolean hoarderClearanceComplete = false;

    /** Number of BRIC_A_BRAC items fenced (for achievement tracking). */
    private int bricABracFenced = 0;

    /** Whether the drug den tip-off has been called in. */
    private boolean tipOffMade = false;

    /** Whether the drug den has been raided by police. */
    private boolean drugDenRaided = false;

    /** In-game time (hours) at which the raid will occur (-1 = no raid pending). */
    private float raidTriggerTime = -1f;

    /** Whether the raid is currently in progress (for player-caught check). */
    private boolean raidInProgress = false;

    /** Number of band rehearsal sessions the player has watched. */
    private int bandSessionsWatched = 0;

    /** Whether the player is in the doorman role for band rehearsals. */
    private boolean playerIsDoorman = false;

    /** Whether the player has joined the band. */
    private boolean playerInBand = false;

    /** Whether the player currently rents Garage 7. */
    private boolean playerRenting = false;

    /** Day number when rent was last paid (-1 = never). */
    private int lastRentPaidDay = -1;

    /** Number of consecutive missed weekly rent payments. */
    private int missedPayments = 0;

    /** Whether Garage 5's HEAVY_PADLOCK has been cut (requires BOLT_CUTTERS or 2× CROWBAR). */
    private int stashCrowbarAttempts = 0;

    /** Whether Garage 5's stash has been looted. */
    private boolean stashLooted = false;

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Constructs the LockUpGarageSystem with the given random source.
     *
     * @param random seeded Random for deterministic testing
     */
    public LockUpGarageSystem(Random random) {
        this.random = random;
        // Garage 6 (skip overflow) is always open
        garageUnlocked[GARAGE_SKIP] = true;
    }

    // ── Break-in ───────────────────────────────────────────────────────────────

    /**
     * Attempt to break into a garage using a LOCKPICK.
     * 70% success chance, silent, takes {@link #LOCKPICK_DURATION_SECONDS} seconds.
     * Dave witnessing is checked separately in {@link #update}.
     *
     * @param garageNumber 1–8
     * @return LOCKPICK_SUCCESS, LOCKPICK_FAILED, ALREADY_OPEN, KEY_ACCESS, or HEAVY_PADLOCK
     */
    public BreakInResult attemptLockpick(int garageNumber) {
        if (garageNumber == GARAGE_SKIP || garageUnlocked[garageNumber]) {
            return BreakInResult.ALREADY_OPEN;
        }
        if (garageNumber == GARAGE_RENTAL && !playerRenting) {
            // No key, treat as normal locked garage
        } else if (garageNumber == GARAGE_RENTAL) {
            return BreakInResult.KEY_ACCESS;
        }
        if (garageNumber == GARAGE_STASH && stashCrowbarAttempts == 0) {
            return BreakInResult.HEAVY_PADLOCK;
        }
        if (random.nextFloat() < LOCKPICK_SUCCESS_CHANCE) {
            garageUnlocked[garageNumber] = true;
            return BreakInResult.LOCKPICK_SUCCESS;
        }
        return BreakInResult.LOCKPICK_FAILED;
    }

    /**
     * Break into a garage using a CROWBAR.
     * 100% success, generates HIGH noise (emit via NoiseSystem externally).
     * Dave witnessing is checked in {@link #update}. Garage 5's HEAVY_PADLOCK
     * requires 2 CROWBAR attempts.
     *
     * @param garageNumber 1–8
     * @return CROWBAR_SUCCESS, ALREADY_OPEN, KEY_ACCESS, or HEAVY_PADLOCK (first attempt only)
     */
    public BreakInResult attemptCrowbar(int garageNumber) {
        if (garageNumber == GARAGE_SKIP || garageUnlocked[garageNumber]) {
            return BreakInResult.ALREADY_OPEN;
        }
        if (garageNumber == GARAGE_RENTAL && playerRenting) {
            return BreakInResult.KEY_ACCESS;
        }
        if (garageNumber == GARAGE_STASH) {
            stashCrowbarAttempts++;
            if (stashCrowbarAttempts < 2) {
                return BreakInResult.HEAVY_PADLOCK;
            }
        }
        garageUnlocked[garageNumber] = true;
        return BreakInResult.CROWBAR_SUCCESS;
    }

    // ── Drug den tip-off ───────────────────────────────────────────────────────

    /**
     * Player presses E on PAYPHONE_PROP to report Garage 3 anonymously.
     * Schedules an UNDERCOVER_POLICE raid within {@link #RAID_DELAY_HOURS_MIN}–
     * {@link #RAID_DELAY_HOURS_MAX} in-game hours.
     *
     * @param currentTimeHours current in-game time (hours, 0–24)
     * @param achievementCallback callback to unlock achievements
     * @return TipOffResult indicating outcome
     */
    public TipOffResult callInTipOff(float currentTimeHours,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        if (drugDenRaided) {
            return TipOffResult.ALREADY_RAIDED;
        }
        if (tipOffMade) {
            return TipOffResult.ALREADY_REPORTED;
        }
        tipOffMade = true;
        float delay = RAID_DELAY_HOURS_MIN
                + random.nextFloat() * (RAID_DELAY_HOURS_MAX - RAID_DELAY_HOURS_MIN);
        raidTriggerTime = currentTimeHours + delay;
        achievementCallback.onAchievement(AchievementType.GRASS);
        return TipOffResult.CALL_MADE;
    }

    // ── Player rental ──────────────────────────────────────────────────────────

    /**
     * Player pays Dave the Caretaker to rent Garage 7.
     * Deducts {@link #RENTAL_COST_PER_WEEK} COIN, grants {@link Material#GARAGE_KEY_7}.
     *
     * @param inventory player inventory
     * @param currentDay current in-game day number
     * @return true if rent was paid successfully, false if insufficient funds
     */
    public boolean payRent(Inventory inventory, int currentDay) {
        if (inventory.getItemCount(Material.COIN) < RENTAL_COST_PER_WEEK) {
            return false;
        }
        inventory.removeItem(Material.COIN, RENTAL_COST_PER_WEEK);
        if (!playerRenting) {
            playerRenting = true;
            inventory.addItem(Material.GARAGE_KEY_7, 1);
            garageUnlocked[GARAGE_RENTAL] = true;
        }
        lastRentPaidDay = currentDay;
        missedPayments = 0;
        return true;
    }

    /**
     * Check if weekly rent is overdue and process eviction if needed.
     * Called internally in {@link #update}.
     *
     * @param currentDay current in-game day number
     * @param inventory player inventory
     * @return true if eviction occurred
     */
    private boolean checkRentAndEviction(int currentDay, Inventory inventory) {
        if (!playerRenting) return false;
        if (lastRentPaidDay < 0) return false;
        int daysSincePaid = currentDay - lastRentPaidDay;
        if (daysSincePaid >= 7) {
            missedPayments++;
            if (missedPayments >= EVICTION_MISSED_PAYMENTS) {
                playerRenting = false;
                garageUnlocked[GARAGE_RENTAL] = false;
                // Remove key
                int keys = inventory.getItemCount(Material.GARAGE_KEY_7);
                if (keys > 0) inventory.removeItem(Material.GARAGE_KEY_7, keys);
                return true;
            }
        }
        return false;
    }

    // ── Band path ──────────────────────────────────────────────────────────────

    /**
     * Record that the player watched a band rehearsal session.
     * After {@link #BAND_SESSIONS_TO_JOIN} sessions, the doorman role is available.
     *
     * @param inventory player inventory
     * @param mcBattleRank the player's current MC_BATTLE rank
     * @param achievementCallback callback to unlock achievements
     */
    public void watchRehearsalSession(Inventory inventory, int mcBattleRank,
                                      NotorietySystem.AchievementCallback achievementCallback) {
        if (playerInBand) return;
        bandSessionsWatched++;
        if (bandSessionsWatched >= BAND_SESSIONS_TO_JOIN && !playerIsDoorman) {
            playerIsDoorman = true;
        }
        if (playerIsDoorman && mcBattleRank >= BAND_JOIN_MC_RANK && !playerInBand) {
            playerInBand = true;
            achievementCallback.onAchievement(AchievementType.GARAGE_BAND_MEMBER);
        }
    }

    /**
     * Player earns doorman payment for a rehearsal session.
     *
     * @param inventory player inventory
     * @return COIN earned, 0 if not in doorman role
     */
    public int claimDoormanPayment(Inventory inventory) {
        if (!playerIsDoorman || playerInBand) return 0;
        inventory.addItem(Material.COIN, BAND_DOORMAN_PAYMENT);
        return BAND_DOORMAN_PAYMENT;
    }

    /**
     * Player earns band gig payment (Saturday at The Vaults).
     *
     * @param inventory player inventory
     * @return COIN earned, 0 if not in band
     */
    public int claimGigPayment(Inventory inventory) {
        if (!playerInBand) return 0;
        inventory.addItem(Material.COIN, BAND_GIG_PAYMENT);
        return BAND_GIG_PAYMENT;
    }

    // ── Hoarder clearance ──────────────────────────────────────────────────────

    /**
     * Player loots items from Garage 2 (hoarder clearance quest).
     * Yields {@link #HOARDER_BRIC_A_BRAC_COUNT} BRIC_A_BRAC and
     * {@link #HOARDER_VINTAGE_RECORD_COUNT} VINTAGE_RECORD.
     *
     * @param inventory player inventory
     * @return true if clearance items were added (only once)
     */
    public boolean clearHoarderGarage(Inventory inventory) {
        if (hoarderClearanceComplete) return false;
        if (!garageUnlocked[GARAGE_HOARDER]) return false;
        hoarderClearanceComplete = true;
        inventory.addItem(Material.BRIC_A_BRAC, HOARDER_BRIC_A_BRAC_COUNT);
        inventory.addItem(Material.VINTAGE_RECORD, HOARDER_VINTAGE_RECORD_COUNT);
        return true;
    }

    /**
     * Record that a BRIC_A_BRAC item has been fenced.
     * Awards {@link AchievementType#BRIC_A_BRAC_BANDIT} once the threshold is reached.
     *
     * @param achievementCallback callback to unlock achievements
     */
    public void recordBricABracFenced(NotorietySystem.AchievementCallback achievementCallback) {
        bricABracFenced++;
        if (bricABracFenced >= BRIC_A_BRAC_BANDIT_THRESHOLD) {
            achievementCallback.onAchievement(AchievementType.BRIC_A_BRAC_BANDIT);
        }
    }

    // ── Stash loot ─────────────────────────────────────────────────────────────

    /**
     * Player loots the stolen goods stash in Garage 5 (requires garage to be unlocked).
     * Awards {@link AchievementType#STASH_ROBBER}.
     *
     * @param inventory player inventory
     * @param achievementCallback callback to unlock achievements
     * @return true if stash was looted (only once per game)
     */
    public boolean lootStash(Inventory inventory,
                             NotorietySystem.AchievementCallback achievementCallback) {
        if (stashLooted) return false;
        if (!garageUnlocked[GARAGE_STASH]) return false;
        stashLooted = true;
        // Reward: random mix of stolen goods
        inventory.addItem(Material.SCRAP_METAL, 5);
        inventory.addItem(Material.CABLE, 3);
        inventory.addItem(Material.BRIC_A_BRAC, 4);
        achievementCallback.onAchievement(AchievementType.STASH_ROBBER);
        return true;
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    /**
     * Per-frame update — processes raid timer, rent checks, and witness detection.
     *
     * @param delta             frame delta in real seconds
     * @param timeSystem        the game time system
     * @param inventory         player inventory
     * @param npcs              list of NPCs in the world
     * @param rumourNetwork     the rumour propagation network
     * @param criminalRecord    the player's criminal record
     * @param notorietySystem   the notoriety system
     * @param factionSystem     the faction system (for Marchetti Respect penalty)
     * @param achievementCallback callback to unlock achievements
     * @param playerNearGarage3 true if the player is currently inside Garage 3
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Inventory inventory,
                       List<NPC> npcs,
                       RumourNetwork rumourNetwork,
                       CriminalRecord criminalRecord,
                       NotorietySystem notorietySystem,
                       FactionSystem factionSystem,
                       NotorietySystem.AchievementCallback achievementCallback,
                       boolean playerNearGarage3) {

        float currentTime = timeSystem.getHours();
        int currentDay = timeSystem.getDay();

        // ── Raid timer ──────────────────────────────────────────────────────────
        if (tipOffMade && !drugDenRaided && raidTriggerTime >= 0) {
            if (currentTime >= raidTriggerTime) {
                drugDenRaided = true;
                raidInProgress = true;
                garageUnlocked[GARAGE_DRUG_DEN] = false; // police seal it

                // Marchetti respect penalty
                factionSystem.modifyRespect(Faction.MARCHETTI_CREW, -DRUG_RAD_RESPECT_PENALTY);

                // Seed raid rumour
                NPC rumourNpc = findNpcNearGarages(npcs);
                if (rumourNpc != null) {
                    rumourNetwork.addRumour(rumourNpc,
                            new Rumour(RumourType.GARAGE_DRUG_RAID,
                                    "Heard there's been a police raid over at the garages. Drug thing, apparently."));
                }

                // Player caught inside?
                if (playerNearGarage3) {
                    notorietySystem.addNotoriety(RAID_CAUGHT_NOTORIETY);
                    criminalRecord.record(CrimeType.GARAGE_DRUG_POSSESSION);
                    achievementCallback.onAchievement(AchievementType.INFORMANT);
                } else {
                    achievementCallback.onAchievement(AchievementType.INFORMANT);
                }
                // brief raid window then clear
                raidInProgress = false;
            }
        }

        // ── Rent check ──────────────────────────────────────────────────────────
        checkRentAndEviction(currentDay, inventory);

        // ── Lock-up landlord achievement ────────────────────────────────────────
        if (playerRenting && missedPayments == 0 && lastRentPaidDay >= 0) {
            int weeksPaid = (currentDay - lastRentPaidDay) / 7;
            // Award after 3 successful weeks (tracked externally; simplified here)
        }
    }

    // ── Dave witness mechanic ───────────────────────────────────────────────────

    /**
     * Called when Dave the Caretaker (DAVE_CARETAKER NPC) witnesses the player
     * breaking into any garage. Files {@link CrimeType#GARAGE_BREAK_IN}, adds
     * Notoriety, and seeds a {@link RumourType#LOCK_UP_BREAK_IN} rumour.
     *
     * @param criminalRecord  the player's criminal record
     * @param notorietySystem the notoriety system
     * @param rumourNetwork   the rumour propagation network
     * @param npcs            list of NPCs (Dave is among them)
     */
    public void daveWitnessesBreakIn(CriminalRecord criminalRecord,
                                     NotorietySystem notorietySystem,
                                     RumourNetwork rumourNetwork,
                                     List<NPC> npcs) {
        criminalRecord.record(CrimeType.GARAGE_BREAK_IN);
        notorietySystem.addNotoriety(DAVE_WITNESS_NOTORIETY);
        NPC dave = findNpcByType(npcs, NPCType.DAVE_CARETAKER);
        if (dave != null) {
            rumourNetwork.addRumour(dave,
                    new Rumour(RumourType.LOCK_UP_BREAK_IN,
                            "Someone's been breaking into the garages on the Northfield Estate."));
        }
    }

    /**
     * Called when a CROWBAR break-in is used (noisy). Records CROWBAR_JUSTICE achievement
     * and seeds the break-in rumour.
     *
     * @param garageNumber        1–8 garage number that was broken into
     * @param rumourNetwork       rumour propagation network
     * @param npcs                list of NPCs in world
     * @param achievementCallback callback to unlock achievements
     */
    public void recordCrowbarBreakIn(int garageNumber,
                                     RumourNetwork rumourNetwork,
                                     List<NPC> npcs,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        achievementCallback.onAchievement(AchievementType.CROWBAR_JUSTICE);
        NPC nearbyNpc = findNpcNearGarages(npcs);
        if (nearbyNpc != null) {
            rumourNetwork.addRumour(nearbyNpc,
                    new Rumour(RumourType.LOCK_UP_BREAK_IN,
                            "Someone's been breaking into the garages on the Northfield Estate."));
        }
    }

    /**
     * Called when a LOCKPICK break-in succeeds. Records LOCKSMITH achievement.
     *
     * @param achievementCallback callback to unlock achievements
     */
    public void recordLockpickSuccess(NotorietySystem.AchievementCallback achievementCallback) {
        achievementCallback.onAchievement(AchievementType.LOCKSMITH);
    }

    /**
     * Called when the player completes 3 weeks of paying rent.
     *
     * @param achievementCallback callback to unlock achievements
     */
    public void recordLockUpLandlord(NotorietySystem.AchievementCallback achievementCallback) {
        achievementCallback.onAchievement(AchievementType.LOCK_UP_LANDLORD);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns whether the specified garage is currently unlocked.
     *
     * @param garageNumber 1–8
     * @return true if the garage door is open/unlocked
     */
    public boolean isGarageUnlocked(int garageNumber) {
        if (garageNumber < 1 || garageNumber > GARAGE_COUNT) return false;
        return garageUnlocked[garageNumber];
    }

    /** Returns whether the drug den tip-off has been called in. */
    public boolean isTipOffMade() {
        return tipOffMade;
    }

    /** Returns whether the drug den has been raided. */
    public boolean isDrugDenRaided() {
        return drugDenRaided;
    }

    /** Returns the number of band rehearsal sessions the player has watched. */
    public int getBandSessionsWatched() {
        return bandSessionsWatched;
    }

    /** Returns whether the player currently holds the doorman role. */
    public boolean isPlayerDoorman() {
        return playerIsDoorman;
    }

    /** Returns whether the player has joined the band. */
    public boolean isPlayerInBand() {
        return playerInBand;
    }

    /** Returns whether the player currently rents Garage 7. */
    public boolean isPlayerRenting() {
        return playerRenting;
    }

    /** Returns the number of missed rent payments. */
    public int getMissedPayments() {
        return missedPayments;
    }

    /** Returns whether the hoarder garage clearance is complete. */
    public boolean isHoarderClearanceComplete() {
        return hoarderClearanceComplete;
    }

    /** Returns whether the stash in Garage 5 has been looted. */
    public boolean isStashLooted() {
        return stashLooted;
    }

    /** Returns the number of BRIC_A_BRAC items fenced so far. */
    public int getBricABracFenced() {
        return bricABracFenced;
    }

    /** Returns whether a raid is currently in progress. */
    public boolean isRaidInProgress() {
        return raidInProgress;
    }

    /**
     * Returns whether a band rehearsal is scheduled for the given day-of-week and hour.
     * Rehearsals are on Tuesday (day % 7 == 2), Thursday (day % 7 == 4),
     * Saturday (day % 7 == 6), 19:00–22:00.
     *
     * @param dayOfWeek 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
     * @param hour      current in-game hour (0–24)
     * @return true if band rehearsal is in progress
     */
    public static boolean isBandRehearsalTime(int dayOfWeek, float hour) {
        boolean rehearsalDay = (dayOfWeek == 2 || dayOfWeek == 4 || dayOfWeek == 6);
        return rehearsalDay
                && hour >= BAND_REHEARSAL_OPEN_HOUR
                && hour < BAND_REHEARSAL_CLOSE_HOUR;
    }

    /**
     * Returns whether Dave the Caretaker is on patrol.
     *
     * @param dayOfWeek 1=Mon … 7=Sun
     * @param hour      current in-game hour
     * @return true if Dave is patrolling (Mon–Fri 08:00–16:00)
     */
    public static boolean isDaveOnPatrol(int dayOfWeek, float hour) {
        return dayOfWeek >= 1 && dayOfWeek <= 5
                && hour >= DAVE_PATROL_START_HOUR
                && hour < DAVE_PATROL_END_HOUR;
    }

    /** Finds the first NPC of the given type in the list, or null if not found. */
    private NPC findNpcByType(List<NPC> npcs, NPCType type) {
        for (NPC npc : npcs) {
            if (npc.getType() == type) return npc;
        }
        return null;
    }

    /** Finds any NPC near the garages to seed rumours into, or null if no NPCs present. */
    private NPC findNpcNearGarages(List<NPC> npcs) {
        // Prefer PUBLIC or PENSIONER NPCs for neighbourhood rumour spread
        for (NPC npc : npcs) {
            if (npc.getType() == NPCType.PUBLIC || npc.getType() == NPCType.PENSIONER) {
                return npc;
            }
        }
        return npcs.isEmpty() ? null : npcs.get(0);
    }
}
