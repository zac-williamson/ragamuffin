package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #1039: Kosta's Barbers — Fade, Trim &amp; the Underground Identity Swap.
 *
 * <p>A haircut changes the player's {@link ragamuffin.entity.HairstyleType} and
 * grants a timed NPC-recognition reduction bonus that integrates directly with
 * {@link WantedSystem} and {@link NotorietySystem}.
 *
 * <h3>Recognition window</h3>
 * After a cut, for {@code durationMinutes} in-game minutes:
 * <ul>
 *   <li>Police require 30% more LOS time to lock on (multiplied by the cut's
 *       {@link CutType#getRecognitionReduction()} fraction on top).</li>
 *   <li>NPCs perceive the player as 5 notoriety points less threatening
 *       (effective notoriety display −5 while window is active).</li>
 * </ul>
 *
 * <h3>Integrations</h3>
 * <ul>
 *   <li>{@link WantedSystem} — refused service at ≥ 2 stars;
 *       {@link #getRecognitionReduction()} consumed by WantedSystem's LOS calculations.</li>
 *   <li>{@link NotorietySystem} — −{@link #NOTORIETY_DISPLAY_REDUCTION} effective
 *       notoriety display during the active window.</li>
 *   <li>{@link DisguiseSystem} — {@code getPassiveRecognitionModifier()} updated
 *       post-cut so that a disguise + fresh cut compound.</li>
 *   <li>{@link FactionSystem} — Marchetti Crew Respect ≥ 75 grants a free trim
 *       (once per session).</li>
 *   <li>{@link RumourNetwork} — Kosta and Wayne share local rumours; each cut
 *       seeds one {@link RumourType#LOCAL_EVENT} rumour.</li>
 *   <li>{@link NeighbourhoodSystem} — shop boards up its window when vibes &lt; 30.</li>
 *   <li>{@link NewspaperSystem} — hairstyle propagated to crime descriptions.</li>
 * </ul>
 *
 * <h3>Open hours</h3>
 * <ul>
 *   <li>BARBER_OWNER (Kosta): Mon–Sat 09:00–18:00 (dayCount % 7 ≠ 0 for Sunday)</li>
 *   <li>BARBER_APPRENTICE (Wayne): Tue–Fri 12:00–18:00</li>
 * </ul>
 *
 * <h3>Queue mechanic</h3>
 * Up to 3 NPCs wait on the {@code WAITING_BENCH_PROP}. If the player jumps the
 * queue (calls {@link #attemptQueueJump}), the first waiting NPC becomes hostile
 * and a {@link RumourType#ANTISOCIAL_BEHAVIOUR} rumour is seeded.
 */
public class BarberSystem {

    // ── Day-of-week constants (dayCount % 7; matches ChurchSystem convention) ─
    // 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
    private static final int MONDAY    = 0;
    private static final int TUESDAY   = 1;
    private static final int WEDNESDAY = 2;
    private static final int THURSDAY  = 3;
    private static final int FRIDAY    = 4;
    private static final int SATURDAY  = 5;
    private static final int SUNDAY    = 6;

    // ── Open-hour constants ───────────────────────────────────────────────────

    /** Kosta (BARBER_OWNER) opening hour Mon–Sat. */
    public static final float OWNER_OPEN_HOUR  = 9.0f;
    /** Kosta (BARBER_OWNER) closing hour Mon–Sat. */
    public static final float OWNER_CLOSE_HOUR = 18.0f;

    /** Wayne (BARBER_APPRENTICE) opening hour Tue–Fri. */
    public static final float APPRENTICE_OPEN_HOUR  = 12.0f;
    /** Wayne (BARBER_APPRENTICE) closing hour Tue–Fri. */
    public static final float APPRENTICE_CLOSE_HOUR = 18.0f;

    // ── WantedSystem integration ──────────────────────────────────────────────

    /**
     * Maximum wanted stars at which the barber will still serve the player.
     * At {@code WANTED_STARS_REFUSE_THRESHOLD} or above, service is refused.
     */
    public static final int WANTED_STARS_REFUSE_THRESHOLD = 2;

    // ── Recognition window constants ──────────────────────────────────────────

    /**
     * Flat police LOS-time extension factor granted by any non-zero cut
     * (on top of the cut's own reduction). Police require 30 % more time
     * to confirm recognition during the window.
     */
    public static final float BASE_LOS_EXTENSION = 0.30f;

    /**
     * Effective notoriety display reduction while recognition window is active.
     * NotorietySystem consumers subtract this when calculating the displayed tier.
     */
    public static final int NOTORIETY_DISPLAY_REDUCTION = 5;

    // ── FactionSystem: free trim ──────────────────────────────────────────────

    /**
     * Marchetti Crew Respect threshold at or above which the player receives a
     * free trim (once per session; {@link CutType#TRIM}).
     */
    public static final int MARCHETTI_FREE_TRIM_RESPECT = 75;

    // ── Queue constants ───────────────────────────────────────────────────────

    /** Maximum number of NPCs allowed on the waiting bench. */
    public static final int MAX_QUEUE_SIZE = 3;

    // ── NeighbourhoodSystem: boarding up ──────────────────────────────────────

    /**
     * Neighbourhood vibes threshold below which the shop boards up its window.
     * At {@code NeighbourhoodSystem} vibes &lt; BOARD_UP_VIBES_THRESHOLD, the
     * window block is replaced with BOARDED_WOOD.
     */
    public static final int BOARD_UP_VIBES_THRESHOLD = 30;

    // ── Visit tracking (for KOSTA_REGULAR achievement) ────────────────────────

    /** Number of visits required to unlock the KOSTA_REGULAR achievement. */
    public static final int REGULAR_VISIT_COUNT = 5;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether the free trim has been used in this session. */
    private boolean freeTrimUsedThisSession = false;

    /** Total visits the player has made to the barber (for KOSTA_REGULAR). */
    private int totalVisits = 0;

    /** Remaining in-game minutes for which the recognition window is active. */
    private float recognitionWindowRemaining = 0f;

    /** The recognition reduction fraction currently in effect (0 if no window). */
    private float activeRecognitionReduction = 0f;

    /** The queue of waiting NPC types (max {@link #MAX_QUEUE_SIZE}). */
    private final List<NPC> waitingQueue = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    public BarberSystem() {
        // default no-arg constructor; dependencies injected per-call
    }

    // ── Open hours ────────────────────────────────────────────────────────────

    /**
     * Returns true if the barber shop is open at the given hour and day, for
     * the given NPC type (BARBER_OWNER or BARBER_APPRENTICE).
     *
     * @param npcType   {@link NPCType#BARBER_OWNER} or {@link NPCType#BARBER_APPRENTICE}
     * @param hour      current in-game hour (0–24)
     * @param dayOfWeek day of week derived from {@code timeSystem.getDayCount() % 7}
     *                  (0=Mon, …, 5=Sat, 6=Sun)
     * @return true if the given barber is on duty
     */
    public boolean isOpen(NPCType npcType, float hour, int dayOfWeek) {
        if (npcType == NPCType.BARBER_OWNER) {
            // Kosta: Mon–Sat 09:00–18:00; closed Sunday
            if (dayOfWeek == SUNDAY) return false;
            return hour >= OWNER_OPEN_HOUR && hour < OWNER_CLOSE_HOUR;
        } else if (npcType == NPCType.BARBER_APPRENTICE) {
            // Wayne: Tue–Fri 12:00–18:00
            if (dayOfWeek != TUESDAY && dayOfWeek != WEDNESDAY
                    && dayOfWeek != THURSDAY && dayOfWeek != FRIDAY) return false;
            return hour >= APPRENTICE_OPEN_HOUR && hour < APPRENTICE_CLOSE_HOUR;
        }
        return false;
    }

    /**
     * Convenience overload: returns true if the shop is open at all (either
     * Kosta or Wayne is on duty) at the given hour and day.
     *
     * @param hour      current in-game hour
     * @param dayOfWeek 0=Mon … 5=Sat, 6=Sun
     * @return true if at least one barber is on duty
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        return isOpen(NPCType.BARBER_OWNER, hour, dayOfWeek)
                || isOpen(NPCType.BARBER_APPRENTICE, hour, dayOfWeek);
    }

    // ── Service eligibility ────────────────────────────────────────────────────

    /**
     * Returns true if the barber can serve the player, considering wanted stars.
     * Service is refused if {@code wantedStars >= }{@link #WANTED_STARS_REFUSE_THRESHOLD}.
     *
     * @param player      the player (unused; reserved for future clothing checks)
     * @param wantedStars the player's current wanted star count
     * @return true if service is available
     */
    public boolean canServePlayer(Player player, int wantedStars) {
        return wantedStars < WANTED_STARS_REFUSE_THRESHOLD;
    }

    // ── Core cut mechanic ─────────────────────────────────────────────────────

    /**
     * Result of a {@link #performCut} call.
     */
    public enum CutResult {
        /** Cut completed successfully. */
        SUCCESS,
        /** Player could not afford the cut (not enough COIN). */
        INSUFFICIENT_FUNDS,
        /** Service refused due to wanted level. */
        REFUSED_WANTED,
        /** Shop is closed at this time/day. */
        SHOP_CLOSED,
        /** Barber provided a free trim (Marchetti Respect ≥ 75). */
        FREE_TRIM
    }

    /**
     * Perform a haircut on the player.
     *
     * <p>On success:
     * <ol>
     *   <li>Deducts COIN from inventory (or waives cost if free trim applies).</li>
     *   <li>Updates the player's hairstyle to {@link CutType#getResultingHairstyle()}.</li>
     *   <li>Starts the recognition reduction window.</li>
     *   <li>Seeds a {@link RumourType#LOCAL_EVENT} rumour via the barber NPC.</li>
     *   <li>Unlocks relevant achievements.</li>
     *   <li>Increments the visit counter.</li>
     * </ol>
     *
     * @param cut             the type of cut requested
     * @param player          the player
     * @param inventory       the player's inventory (must have enough COIN)
     * @param wantedStars     player's current wanted stars
     * @param hour            current in-game hour
     * @param dayOfWeek       0=Mon … 5=Sat, 6=Sun
     * @param factionSystem   used to check Marchetti Crew Respect for free trim
     * @param rumourNetwork   receives the LOCAL_EVENT rumour post-cut (may be null)
     * @param barberNpc       the NPC performing the cut (for rumour origin; may be null)
     * @param achievementCb   callback for awarding achievements (may be null)
     * @return the result of the cut attempt
     */
    public CutResult performCut(CutType cut,
                                Player player,
                                Inventory inventory,
                                int wantedStars,
                                float hour,
                                int dayOfWeek,
                                FactionSystem factionSystem,
                                RumourNetwork rumourNetwork,
                                NPC barberNpc,
                                NotorietySystem.AchievementCallback achievementCb) {

        // Validate shop is open
        if (!isOpen(hour, dayOfWeek)) {
            return CutResult.SHOP_CLOSED;
        }

        // Validate wanted level
        if (!canServePlayer(player, wantedStars)) {
            return CutResult.REFUSED_WANTED;
        }

        // Check for free trim via Marchetti Crew Respect
        boolean isFree = false;
        if (!freeTrimUsedThisSession
                && cut == CutType.TRIM
                && factionSystem != null
                && factionSystem.getRespect(Faction.MARCHETTI_CREW) >= MARCHETTI_FREE_TRIM_RESPECT) {
            isFree = true;
            freeTrimUsedThisSession = true;
        }

        // Deduct cost unless free
        if (!isFree) {
            int cost = cut.getCostCoin();
            if (!inventory.hasItem(Material.COIN, cost)) {
                return CutResult.INSUFFICIENT_FUNDS;
            }
            inventory.removeItem(Material.COIN, cost);
        }

        // Apply hairstyle to player
        player.setHairstyle(cut.getResultingHairstyle());

        // Start recognition window
        if (cut.getDurationMinutes() > 0) {
            recognitionWindowRemaining = cut.getDurationMinutes();
            activeRecognitionReduction = cut.getRecognitionReduction();
        } else {
            recognitionWindowRemaining = 0f;
            activeRecognitionReduction = 0f;
        }

        // Seed rumour
        if (rumourNetwork != null && barberNpc != null) {
            String text = "Someone got a fresh cut at Kosta's — new look, new man";
            rumourNetwork.addRumour(barberNpc, new Rumour(RumourType.LOCAL_EVENT, text));
        }

        // Track visit count + achievements
        totalVisits++;

        if (achievementCb != null) {
            achievementCb.award(AchievementType.FRESH_CUT);

            if (cut.getRecognitionReduction() > 0f && wantedStars >= 1) {
                achievementCb.award(AchievementType.UNDERCOVER_FADE);
            }

            if (cut == CutType.HEAD_SHAVE || cut == CutType.GRADE_1_BUZZCUT) {
                achievementCb.award(AchievementType.NEW_MAN);
            }

            if (totalVisits >= REGULAR_VISIT_COUNT) {
                achievementCb.award(AchievementType.KOSTA_REGULAR);
            }

            if (isFree) {
                achievementCb.award(AchievementType.FREE_FROM_KOSTA);
            }
        }

        return isFree ? CutResult.FREE_TRIM : CutResult.SUCCESS;
    }

    // ── Recognition window ────────────────────────────────────────────────────

    /**
     * Returns the recognition reduction fraction currently in effect (0–1).
     * Zero means no active window (no cut effect). Consumed by WantedSystem
     * when calculating police LOS duration.
     */
    public float getRecognitionReduction() {
        return recognitionWindowRemaining > 0f ? activeRecognitionReduction : 0f;
    }

    /**
     * Returns true if the recognition reduction window is currently active.
     */
    public boolean isRecognitionWindowActive() {
        return recognitionWindowRemaining > 0f;
    }

    /**
     * Returns the remaining in-game minutes for the recognition window.
     */
    public float getRecognitionWindowRemaining() {
        return recognitionWindowRemaining;
    }

    /**
     * Returns the effective notoriety display reduction while the window is active.
     * Returns 0 when no window is active.
     */
    public int getEffectiveNotorietyReduction() {
        return recognitionWindowRemaining > 0f ? NOTORIETY_DISPLAY_REDUCTION : 0;
    }

    /**
     * Returns the passive recognition modifier for integration with
     * {@link DisguiseSystem}. Equals {@link #getRecognitionReduction()} when
     * the window is active; 0 otherwise.
     */
    public float getPassiveRecognitionModifier() {
        return getRecognitionReduction();
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the barber system each frame. Ticks down the recognition window.
     *
     * @param delta          seconds since last frame (real time)
     * @param timeSystem     used to convert real time to in-game minutes
     */
    public void update(float delta, TimeSystem timeSystem) {
        if (recognitionWindowRemaining > 0f) {
            // Convert real seconds to in-game minutes using the time speed multiplier.
            // TimeSystem advances 0.1 in-game hours per real second by default,
            // which is 6 in-game minutes per real second.
            float inGameMinutesPerRealSecond = timeSystem.getTimeSpeed() * 60f;
            recognitionWindowRemaining -= delta * inGameMinutesPerRealSecond;
            if (recognitionWindowRemaining < 0f) {
                recognitionWindowRemaining = 0f;
                activeRecognitionReduction = 0f;
            }
        }
    }

    // ── Queue mechanics ────────────────────────────────────────────────────────

    /**
     * Add an NPC to the waiting queue (up to {@link #MAX_QUEUE_SIZE}).
     *
     * @param npc the NPC joining the queue
     * @return true if the NPC was added; false if the queue is full
     */
    public boolean joinQueue(NPC npc) {
        if (waitingQueue.size() >= MAX_QUEUE_SIZE) return false;
        waitingQueue.add(npc);
        return true;
    }

    /**
     * Attempt to jump the queue. If there is at least one NPC waiting, the first
     * NPC in the queue becomes hostile and an {@link RumourType#ANTISOCIAL_BEHAVIOUR}
     * rumour is seeded. The QUEUE_JUMPER achievement is awarded.
     *
     * @param player        the player attempting the jump
     * @param rumourNetwork rumour network for the hostility rumour (may be null)
     * @param achievementCb callback for awarding achievements (may be null)
     * @return true if the jump was successful (queue non-empty and player moved to front)
     */
    public boolean attemptQueueJump(Player player,
                                    RumourNetwork rumourNetwork,
                                    NotorietySystem.AchievementCallback achievementCb) {
        if (waitingQueue.isEmpty()) return false;

        // First NPC in queue becomes hostile
        NPC offended = waitingQueue.get(0);
        offended.setState(NPCState.AGGRESSIVE);
        offended.setSpeechText("Oi! There's a queue, mate!", 4f);

        // Seed antisocial behaviour rumour
        if (rumourNetwork != null) {
            rumourNetwork.addRumour(offended,
                    new Rumour(RumourType.ANTISOCIAL_BEHAVIOUR, "Someone jumped the queue at Kosta's"));
        }

        // Award achievement
        if (achievementCb != null) {
            achievementCb.award(AchievementType.QUEUE_JUMPER);
        }

        return true;
    }

    /**
     * Returns an unmodifiable view of the current waiting queue.
     */
    public List<NPC> getWaitingQueue() {
        return java.util.Collections.unmodifiableList(waitingQueue);
    }

    /**
     * Remove the first NPC from the queue (called when service is completed
     * for a waiting NPC).
     *
     * @return the NPC that was at the front, or null if the queue was empty
     */
    public NPC dequeueNext() {
        if (waitingQueue.isEmpty()) return null;
        return waitingQueue.remove(0);
    }

    // ── Session management ────────────────────────────────────────────────────

    /**
     * Reset the per-session state (free trim flag). Call at the start of each
     * new in-game day.
     */
    public void resetSession() {
        freeTrimUsedThisSession = false;
    }

    /** Returns true if the Marchetti free trim has been used this session. */
    public boolean isFreeTrimUsedThisSession() {
        return freeTrimUsedThisSession;
    }

    /** Returns the total number of visits the player has made to the barber. */
    public int getTotalVisits() {
        return totalVisits;
    }

    // ── Testing helpers ───────────────────────────────────────────────────────

    /**
     * Directly set the recognition window remaining (for testing).
     *
     * @param minutes remaining in-game minutes
     * @param reduction recognition reduction fraction
     */
    public void setRecognitionWindowForTesting(float minutes, float reduction) {
        this.recognitionWindowRemaining = minutes;
        this.activeRecognitionReduction = reduction;
    }

    /**
     * Directly set the total visit count (for testing KOSTA_REGULAR achievement).
     *
     * @param count visit count to set
     */
    public void setTotalVisitsForTesting(int count) {
        this.totalVisits = count;
    }
}
