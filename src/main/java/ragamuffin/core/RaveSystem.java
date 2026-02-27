package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.PropType;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #716: Underground Music Scene — Rave System.
 *
 * <h3>Overview</h3>
 * The player can host an illegal rave in their squat by using a craftable
 * {@link Material#FLYER}. Requirements: Vibe ≥ 40 and MC Rank ≥ 1.
 *
 * <h3>Rave Flow</h3>
 * <ol>
 *   <li>Player uses a FLYER (consumed) — a {@link RumourType#RAVE_ANNOUNCEMENT} rumour
 *       is seeded into nearby NPCs.</li>
 *   <li>NPCs that hear the rumour begin walking toward the squat and become
 *       {@link NPCType#RAVE_ATTENDEE} for the duration of the rave.</li>
 *   <li>Each attendee generates {@link #BASE_INCOME_PER_ATTENDEE_PER_MINUTE}
 *       COIN per in-game minute (delta-accumulated).</li>
 *   <li>After {@link #POLICE_ALERT_SECONDS} (2 in-game minutes) the police are
 *       alerted. If the rave continues past that point the player gains a
 *       noise-offence on their criminal record.</li>
 *   <li>The player can disperse the rave early (press E at the squat door) to avoid
 *       the noise offence and earn the {@link AchievementType#SWERVED_THE_FEDS}
 *       achievement.</li>
 * </ol>
 *
 * <h3>Rave Equipment Props</h3>
 * <ul>
 *   <li>{@link PropType#SPEAKER_STACK}  — +5 max capacity, +1 COIN/attendee/minute</li>
 *   <li>{@link PropType#DISCO_BALL}     — +3 max capacity</li>
 *   <li>{@link PropType#DJ_DECKS}       — enables DJ recruitment (doubles income)</li>
 * </ul>
 *
 * <h3>MC Rank Gating</h3>
 * Each MC Rank above 1 allows one extra rave per session and increases base capacity
 * by {@link #CAPACITY_PER_MC_RANK}.
 */
public class RaveSystem {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Minimum Vibe required to host a rave. */
    public static final int MIN_VIBE_FOR_RAVE = 40;

    /** Minimum MC Rank required to host a rave. */
    public static final int MIN_MC_RANK_FOR_RAVE = 1;

    /** Base maximum attendees before equipment bonuses. */
    public static final int BASE_CAPACITY = 10;

    /** Additional capacity per MC Rank above 1. */
    public static final int CAPACITY_PER_MC_RANK = 3;

    /** Additional capacity per SPEAKER_STACK prop. */
    public static final int SPEAKER_STACK_CAPACITY_BONUS = 5;

    /** Additional capacity per DISCO_BALL prop. */
    public static final int DISCO_BALL_CAPACITY_BONUS = 3;

    /** COIN income per attendee per in-game minute (base). */
    public static final float BASE_INCOME_PER_ATTENDEE_PER_MINUTE = 1.0f;

    /** Additional income per attendee per minute added by each SPEAKER_STACK. */
    public static final float SPEAKER_STACK_INCOME_BONUS = 1.0f;

    /** Multiplier applied to total income when a DJ (STREET_LAD) is recruited. */
    public static final float DJ_INCOME_MULTIPLIER = 2.0f;

    /** Hype-man income bonus (multiplicative per hype-man NPC, additive to total). */
    public static final float HYPE_MAN_INCOME_BONUS_FRACTION = 0.10f;

    /**
     * In-game seconds before police are alerted (2 in-game minutes at 50x time scale).
     * At 50 real-seconds-per-in-game-minute this is 120 seconds.
     * Stored as real seconds.
     */
    public static final float POLICE_ALERT_SECONDS = 120f;

    /** In-game seconds per real second (time scale factor for income calculation). */
    public static final float SECONDS_PER_IN_GAME_MINUTE = 60f;

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean raveActive;

    /** Elapsed real-time seconds since rave started. */
    private float elapsedSeconds;

    /** Whether the police have been alerted for this rave. */
    private boolean policeAlerted;

    /** Whether the player dispersed early (avoiding noise offence). */
    private boolean dispersedEarly;

    /** Current attendee list. */
    private final List<NPC> attendees;

    /** Whether a DJ (STREET_LAD) has been recruited to the decks. */
    private boolean djRecruited;

    /** Number of SPEAKER_STACK props in the squat. */
    private int speakerStacks;

    /** Number of DISCO_BALL props in the squat. */
    private int discoBalls;

    /** Number of HYPE_MAN NPCs near the squat. */
    private int hypeMen;

    /** Accumulated coin income (fractional; integer portion disbursed on update). */
    private float accumulatedIncome;

    private final AchievementSystem achievementSystem;
    private final NotorietySystem notorietySystem;
    private final RumourNetwork rumourNetwork;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RaveSystem(
            AchievementSystem achievementSystem,
            NotorietySystem notorietySystem,
            RumourNetwork rumourNetwork) {
        this.achievementSystem = achievementSystem;
        this.notorietySystem   = notorietySystem;
        this.rumourNetwork     = rumourNetwork;
        this.raveActive        = false;
        this.elapsedSeconds    = 0f;
        this.policeAlerted     = false;
        this.dispersedEarly    = false;
        this.attendees         = new ArrayList<>();
        this.djRecruited       = false;
        this.speakerStacks     = 0;
        this.discoBalls        = 0;
        this.hypeMen           = 0;
        this.accumulatedIncome = 0f;
    }

    // ── Equipment configuration ───────────────────────────────────────────────

    /**
     * Set the number of rave-equipment props currently installed in the squat.
     * Call this whenever props are added or removed.
     */
    public void setEquipment(int speakerStacks, int discoBalls, boolean djDecksPresent) {
        this.speakerStacks = Math.max(0, speakerStacks);
        this.discoBalls    = Math.max(0, discoBalls);
        // DJ decks presence is handled via djRecruited separately
    }

    /** @param count number of HYPE_MAN NPCs near the squat */
    public void setHypeMenCount(int count) {
        this.hypeMen = Math.max(0, count);
    }

    // ── Rave start / stop ─────────────────────────────────────────────────────

    /**
     * Attempt to start a rave. Consumes one FLYER from the player's inventory.
     *
     * @param inventory      player's inventory
     * @param currentVibe    current squat Vibe score
     * @param mcRank         player's current MC Rank
     * @param nearbyNpcs     list of NPCs nearby (for rumour seeding)
     * @return status message
     */
    public String startRave(Inventory inventory, int currentVibe, int mcRank,
                            List<NPC> nearbyNpcs) {
        if (raveActive) {
            return "There's already a rave going on, fam.";
        }
        if (currentVibe < MIN_VIBE_FOR_RAVE) {
            return "Vibe's too low for a rave — sort the gaff out first.";
        }
        if (mcRank < MIN_MC_RANK_FOR_RAVE) {
            return "You need to win an MC Battle before you can host a rave.";
        }
        if (inventory.getItemCount(Material.FLYER) < 1) {
            return "You need a Flyer to announce the rave.";
        }

        // Consume the flyer
        inventory.removeItem(Material.FLYER, 1);

        // Seed rave announcement rumour into nearby NPCs
        if (nearbyNpcs != null) {
            Rumour raveRumour = new Rumour(
                    RumourType.RAVE_ANNOUNCEMENT,
                    "Rave on tonight — some MC's squat. Gonna go mental, apparently."
            );
            for (NPC npc : nearbyNpcs) {
                if (npc.getType() != NPCType.POLICE
                        && npc.getType() != NPCType.ARMED_RESPONSE
                        && npc.getType() != NPCType.PCSO) {
                    rumourNetwork.addRumour(npc, raveRumour.spread());
                }
            }
        }

        raveActive      = true;
        elapsedSeconds  = 0f;
        policeAlerted   = false;
        dispersedEarly  = false;
        accumulatedIncome = 0f;
        attendees.clear();

        achievementSystem.unlock(AchievementType.ILLEGAL_RAVE);
        return "Rave started! Word is spreading — people are on their way.";
    }

    /**
     * Disperse the rave early (player presses E at the squat door).
     * Awards SWERVED_THE_FEDS if police have not yet been alerted.
     *
     * @param inventory player's inventory (accumulated coins are paid out)
     * @return status message
     */
    public String disperseRave(Inventory inventory) {
        if (!raveActive) {
            return "There's no rave to disperse.";
        }
        boolean awardedSwerve = !policeAlerted;
        endRave(inventory);
        dispersedEarly = true;

        if (awardedSwerve) {
            achievementSystem.unlock(AchievementType.SWERVED_THE_FEDS);
            return "Rave dispersed before the feds arrived. Swerved it.";
        }
        return "Rave dispersed. Too late — police have already been called.";
    }

    /**
     * Advance the rave by {@code delta} real seconds.
     * Disburses accumulated income into the inventory and fires police alert
     * if the time threshold is passed.
     *
     * @param delta          seconds since last frame
     * @param inventory      player's inventory (coins are added here)
     * @param attendeeCount  current number of attendees in the squat
     */
    public void update(float delta, Inventory inventory, int attendeeCount) {
        if (!raveActive) return;

        elapsedSeconds += delta;

        // Police alert threshold
        if (!policeAlerted && elapsedSeconds >= POLICE_ALERT_SECONDS) {
            policeAlerted = true;
            // Police alert is signalled via getPoliceAlerted(); game logic handles spawning
        }

        // Income accumulation
        float incomePerSecond = computeIncomePerSecond(attendeeCount);
        accumulatedIncome += incomePerSecond * delta;

        // Disburse whole coins
        int coins = (int) accumulatedIncome;
        if (coins > 0) {
            inventory.addItem(Material.COIN, coins);
            accumulatedIncome -= coins;
        }
    }

    /**
     * Compute income in COIN per real second based on current attendees and equipment.
     *
     * @param attendeeCount number of attendees currently present
     * @return COIN/second rate
     */
    public float computeIncomePerSecond(int attendeeCount) {
        if (attendeeCount <= 0) return 0f;

        // Base income rate per attendee per real second
        // (1 COIN/in-game-minute = 1/60 COIN/real-second at 1:1 timescale)
        float basePerAttendeePerSecond = BASE_INCOME_PER_ATTENDEE_PER_MINUTE / SECONDS_PER_IN_GAME_MINUTE;

        // Speaker stack bonus
        float speakerBonus = speakerStacks * (SPEAKER_STACK_INCOME_BONUS / SECONDS_PER_IN_GAME_MINUTE);

        float perAttendeePerSecond = basePerAttendeePerSecond + speakerBonus;

        float total = perAttendeePerSecond * attendeeCount;

        // DJ multiplier
        if (djRecruited) {
            total *= DJ_INCOME_MULTIPLIER;
        }

        // Hype man bonus
        if (hypeMen > 0) {
            total *= (1f + hypeMen * HYPE_MAN_INCOME_BONUS_FRACTION);
        }

        return total;
    }

    /** Internal cleanup when the rave ends. */
    private void endRave(Inventory inventory) {
        // Disburse any remaining fractional income
        int remaining = (int) accumulatedIncome;
        if (remaining > 0 && inventory != null) {
            inventory.addItem(Material.COIN, remaining);
        }
        accumulatedIncome = 0f;
        raveActive        = false;
        attendees.clear();
    }

    // ── DJ management ─────────────────────────────────────────────────────────

    /**
     * Attempt to recruit a STREET_LAD NPC as resident DJ.
     * Requires DJ_DECKS to be present in the squat.
     *
     * @param dj             the STREET_LAD NPC to recruit
     * @param djDecksPresent whether DJ_DECKS prop is installed
     * @return {@code true} if successfully recruited
     */
    public boolean recruitDj(NPC dj, boolean djDecksPresent) {
        if (!djDecksPresent) return false;
        if (dj == null || dj.getType() != NPCType.STREET_LAD) return false;
        djRecruited = true;
        return true;
    }

    // ── Capacity ──────────────────────────────────────────────────────────────

    /**
     * Compute the maximum number of attendees for the current equipment + MC Rank.
     *
     * @param mcRank current MC Rank
     * @return maximum attendee capacity
     */
    public int computeCapacity(int mcRank) {
        int capacity = BASE_CAPACITY;
        capacity += Math.max(0, mcRank - 1) * CAPACITY_PER_MC_RANK;
        capacity += speakerStacks * SPEAKER_STACK_CAPACITY_BONUS;
        capacity += discoBalls * DISCO_BALL_CAPACITY_BONUS;
        return capacity;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** @return {@code true} if a rave is currently active */
    public boolean isRaveActive() { return raveActive; }

    /** @return elapsed real-time seconds since the rave started */
    public float getElapsedSeconds() { return elapsedSeconds; }

    /** @return {@code true} if police have been alerted for this rave */
    public boolean isPoliceAlerted() { return policeAlerted; }

    /** @return remaining seconds before police are alerted (0 if already alerted or no rave) */
    public float getSecondsUntilPolice() {
        if (!raveActive || policeAlerted) return 0f;
        return Math.max(0f, POLICE_ALERT_SECONDS - elapsedSeconds);
    }

    /** @return accumulated (fractional) income not yet disbursed */
    public float getAccumulatedIncome() { return accumulatedIncome; }

    /** @return {@code true} if a DJ has been recruited to the decks */
    public boolean isDjRecruited() { return djRecruited; }

    /** Number of SPEAKER_STACK props configured. */
    public int getSpeakerStacks() { return speakerStacks; }

    /** Number of DISCO_BALL props configured. */
    public int getDiscoBalls() { return discoBalls; }

    /**
     * Issue #787: Trigger a flash rave at the player's location (SURVIVAL Legend perk).
     * Starts the rave immediately regardless of Vibe/MC Rank prerequisites.
     */
    public void setFlashRaveActive(boolean active) {
        this.raveActive = active;
        if (active) {
            this.elapsedSeconds = 0f;
            this.accumulatedIncome = 0f;
            this.policeAlerted = false;
        }
    }
}
