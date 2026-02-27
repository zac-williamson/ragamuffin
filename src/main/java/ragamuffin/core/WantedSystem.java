package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Issue #771: Hot Pursuit — Wanted System, Police Chases &amp; Getaway Mechanics.
 *
 * <p>Implements a dynamic escalating police pursuit system with 0–5 wanted stars
 * that gives every criminal act real stakes and memorable close calls.
 *
 * <h3>Wanted Level (0–5 stars)</h3>
 * <ul>
 *   <li>0 — Clean: police ignore you</li>
 *   <li>1 — Stop &amp; Search: PCSOs approach and question</li>
 *   <li>2 — Active Pursuit: police chase on sight</li>
 *   <li>3 — All Units: multiple officers, fan-out search</li>
 *   <li>4 — ARU Deployed: armed response + helicopter</li>
 *   <li>5 — Council Lockdown: entire borough on alert</li>
 * </ul>
 *
 * <h3>Decay</h3>
 * −1 star per 90 seconds completely outside all police line-of-sight.
 * Any LOS contact resets the decay timer.
 *
 * <h3>Escape Routes</h3>
 * <ol>
 *   <li>Disguise change — resets police description (once per pursuit, ≤3 stars)</li>
 *   <li>Bribe PCSO — costs 8 COIN × wanted level; only if Notoriety &lt; 60</li>
 *   <li>Safe house squat — 120 s inside, police won't enter at ≤3 stars</li>
 *   <li>Leg it — 80 blocks from LKP + 60 s LOS break → −2 stars</li>
 * </ol>
 *
 * <h3>Hiding Mechanics</h3>
 * Player presses SHIFT to enter/exit a hiding spot (WHEELIE_BIN, shop doorway,
 * stairwell, charity shop changing room, pub toilet). Hiding progress bar on HUD.
 * Police will not enter hiding spots at ≤2 stars.
 *
 * <h3>Integrations</h3>
 * WitnessSystem severity → wanted escalation; DisguiseSystem disguise-burn tracking;
 * NotorietySystem; FactionSystem lockdown at 5 stars during COUNCIL_CRACKDOWN;
 * RaveSystem doubles police alert speed at Wanted ≥ 2; StreetEconomySystem dodgy
 * trades are risky at Wanted ≥ 1; night/rain/fog reduce police LOS range.
 *
 * <h3>Corrupt PCSO Sub-System</h3>
 * Cultivate with 3 FLASK_OF_TEA interactions for halved bribe cost and advance
 * warning of approaching patrols.
 */
public class WantedSystem {

    // ── Wanted level constants ─────────────────────────────────────────────────

    /** Maximum wanted stars. */
    public static final int MAX_WANTED_STARS = 5;

    /** Seconds of sustained LOS break required to lose one star. */
    public static final float DECAY_SECONDS_PER_STAR = 90f;

    /** Duration (seconds) police fan out and search after losing LOS. */
    public static final float SEARCH_DURATION_SECONDS = 30f;

    /** Blocks the player must be from the LKP (last-known-position) for the leg-it escape. */
    public static final float LEG_IT_DISTANCE = 80f;

    /** Seconds of continuous LOS break required for the leg-it −2 stars bonus. */
    public static final float LEG_IT_LOS_BREAK_SECONDS = 60f;

    /** Star reduction awarded by the leg-it escape. */
    public static final int LEG_IT_STAR_REDUCTION = 2;

    /** Duration (seconds) inside a safe house required to clear wanted level (≤3 stars). */
    public static final float SAFE_HOUSE_DURATION = 120f;

    /** Maximum wanted stars at which police won't enter a safe house squat. */
    public static final int SAFE_HOUSE_POLICE_ENTRY_THRESHOLD = 3;

    /** Maximum wanted stars at which a disguise change resets police description. */
    public static final int DISGUISE_RESET_MAX_STARS = 3;

    /** Coin cost multiplier per star for bribing a PCSO. */
    public static final int BRIBE_COST_PER_STAR = 8;

    /** Maximum Notoriety score that allows bribing a PCSO. */
    public static final int BRIBE_MAX_NOTORIETY = 60;

    /** Number of FLASK_OF_TEA interactions needed to cultivate a corrupt PCSO. */
    public static final int CORRUPT_PCSO_TEA_INTERACTIONS = 3;

    /** Bribe cost multiplier when using a corrupt PCSO (halved). */
    public static final float CORRUPT_PCSO_BRIBE_MULTIPLIER = 0.5f;

    /** Police base LOS range in blocks. */
    public static final float POLICE_BASE_LOS_RANGE = 20f;

    /** Police LOS range reduction at night (blocks). */
    public static final float NIGHT_LOS_REDUCTION = 8f;

    /** Police LOS range reduction in rain (blocks). */
    public static final float RAIN_LOS_REDUCTION = 6f;

    /** Police LOS range reduction in fog (blocks). */
    public static final float FOG_LOS_REDUCTION = 4f;

    /** Maximum stars at which police won't enter a hiding spot. */
    public static final int HIDING_SPOT_POLICE_ENTRY_THRESHOLD = 2;

    /** Duration (seconds) to fully enter a hiding spot. */
    public static final float HIDING_ENTER_DURATION = 2f;

    /** Fine in COIN per wanted star on arrest. */
    public static final int ARREST_FINE_PER_STAR = 10;

    /** Notoriety gain on arrest. */
    public static final int ARREST_NOTORIETY_GAIN = 5;

    /** Severity thresholds: number of crime severity points needed to gain each star. */
    private static final int[] SEVERITY_THRESHOLDS = {0, 1, 3, 6, 10, 15};

    // ── RaveSystem integration ─────────────────────────────────────────────────

    /** Minimum wanted stars at which RaveSystem doubles police alert speed. */
    public static final int RAVE_DOUBLE_ALERT_MIN_STARS = 2;

    // ── State ──────────────────────────────────────────────────────────────────

    /** Current wanted stars (0–5). */
    private int wantedStars = 0;

    /** Accumulated crime severity this pursuit (resets on wanted level drop to 0). */
    private int accumulatedSeverity = 0;

    /** Last-known-position of the player (updated whenever a police NPC has LOS). */
    private final Vector3 lastKnownPosition = new Vector3();

    /** True if at least one police NPC currently has line-of-sight on the player. */
    private boolean policeHasLos = false;

    /** Timer counting how long the player has been outside all police LOS (for decay). */
    private float losBreakTimer = 0f;

    /** Timer counting how long the player has continuously been in police LOS. */
    private float losContactTimer = 0f;

    /** Timer for current star's decay progress (counts up to DECAY_SECONDS_PER_STAR). */
    private float decayTimer = 0f;

    /** Whether a disguise change has already been used this pursuit. */
    private boolean disguiseUsedThisPursuit = false;

    /** Whether the player is currently hiding in a hiding spot. */
    private boolean isHiding = false;

    /** Progress of entering the current hiding spot (0.0–1.0). */
    private float hidingProgress = 0f;

    /** Timer for how long the player has been inside a safe house. */
    private float safeHouseTimer = 0f;

    /** Whether the player is currently inside a safe house squat. */
    private boolean isInSafeHouse = false;

    /** Whether the current corrupt PCSO relationship has been established. */
    private boolean hasCorruptPcso = false;

    /** Number of FLASK_OF_TEA interactions with the target corrupt PCSO. */
    private int corruptPcsoTeaCount = 0;

    /** The specific PCSO NPC being cultivated, or null. */
    private NPC corruptPcsoNpc = null;

    /** Active searching NPCs during the fan-out search phase. */
    private final List<NPC> searchingNpcs = new ArrayList<>();

    /** Timer for how long the current fan-out search has been active. */
    private float searchTimer = 0f;

    /** Whether a search phase is currently active. */
    private boolean searchPhaseActive = false;

    /** The player's LKP distance from it (for leg-it escape tracking). */
    private float legItCurrentDistance = 0f;

    /** Timer for continuous LOS break (for leg-it escape). */
    private float legItLosBreakTimer = 0f;

    /** Whether the leg-it escape conditions have been met (distance + LOS break). */
    private boolean legItConditionMet = false;

    /** Whether the wanted system is in active pursuit mode. */
    private boolean inPursuit = false;

    /** Whether the FIVE_STAR_NIGHTMARE achievement has been awarded. */
    private boolean fiveStarAchievementAwarded = false;

    /** Whether the CLEAN_GETAWAY_PURSUIT achievement has been awarded this session. */
    private boolean cleanGetawayAwarded = false;

    private final Random random;

    // ── Callbacks ──────────────────────────────────────────────────────────────

    /**
     * Callback for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public WantedSystem() {
        this(new Random());
    }

    public WantedSystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the wanted system each frame.
     *
     * @param delta            seconds since last frame
     * @param player           the player
     * @param npcs             all living NPCs in the world
     * @param weather          current weather condition (null = clear)
     * @param isNight          true if it is currently night time
     * @param isRaveActive     true if a rave is currently active
     * @param achievementCallback callback for achievements (may be null)
     */
    public void update(float delta, Player player, List<NPC> npcs,
                       Weather weather, boolean isNight, boolean isRaveActive,
                       AchievementCallback achievementCallback) {
        if (wantedStars == 0 && !inPursuit) {
            return; // nothing to do
        }

        // Compute effective LOS range for current conditions
        float losRange = computeLosRange(weather, isNight);

        // If a rave is active and wanted >= RAVE_DOUBLE_ALERT_MIN_STARS, police alert faster
        // (this is handled externally by the game loop reading getRaveAlertSpeedMultiplier())

        // Update police LOS status
        updatePoliceLoS(player, npcs, losRange);

        // Update decay / LOS timers
        updateDecayTimers(delta, player, achievementCallback);

        // Update hiding progress
        updateHidingProgress(delta);

        // Update safe house timer
        updateSafeHouseTimer(delta, achievementCallback);

        // Update search phase
        updateSearchPhase(delta, player, npcs);

        // Update leg-it escape tracking
        updateLegItTracking(delta, player, achievementCallback);

        // Update police NPC states based on current wanted level and LKP
        updatePoliceNpcStates(player, npcs, losRange);
    }

    // ── Wanted level escalation ────────────────────────────────────────────────

    /**
     * Called when a crime is witnessed. Escalates the wanted level based on
     * the severity of the crime type.
     *
     * @param severity           severity points of the crime (from WitnessSystem)
     * @param crimeX             world X of the crime
     * @param crimeY             world Y of the crime
     * @param crimeZ             world Z of the crime
     * @param player             the player
     * @param achievementCallback callback for achievements (may be null)
     * @return true if the wanted level increased
     */
    public boolean onCrimeWitnessed(int severity, float crimeX, float crimeY, float crimeZ,
                                     Player player, AchievementCallback achievementCallback) {
        int oldStars = wantedStars;
        accumulatedSeverity += severity;

        // Recompute stars from accumulated severity
        int newStars = computeStarsFromSeverity(accumulatedSeverity);
        if (newStars > wantedStars) {
            wantedStars = Math.min(MAX_WANTED_STARS, newStars);
            inPursuit = true;
            decayTimer = 0f; // reset decay when stars go up

            // Update LKP to crime location
            lastKnownPosition.set(crimeX, crimeY, crimeZ);

            // Award five-star achievement
            if (wantedStars == 5 && !fiveStarAchievementAwarded) {
                fiveStarAchievementAwarded = true;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.FIVE_STAR_NIGHTMARE);
                }
            }
        }
        return wantedStars > oldStars;
    }

    /**
     * Add wanted stars directly (e.g. for hitting police, crimes in police presence).
     *
     * @param stars   number of stars to add (positive)
     * @param lkpX    last-known-position X
     * @param lkpY    last-known-position Y
     * @param lkpZ    last-known-position Z
     * @param achievementCallback callback for achievements (may be null)
     */
    public void addWantedStars(int stars, float lkpX, float lkpY, float lkpZ,
                                AchievementCallback achievementCallback) {
        if (stars <= 0) return;
        int oldStars = wantedStars;
        wantedStars = Math.min(MAX_WANTED_STARS, wantedStars + stars);
        if (wantedStars > 0) {
            inPursuit = true;
            decayTimer = 0f;
            lastKnownPosition.set(lkpX, lkpY, lkpZ);
        }
        if (wantedStars == 5 && oldStars < 5 && !fiveStarAchievementAwarded) {
            fiveStarAchievementAwarded = true;
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.FIVE_STAR_NIGHTMARE);
            }
        }
    }

    // ── Hiding mechanic ────────────────────────────────────────────────────────

    /**
     * Called when the player presses SHIFT to enter or exit a hiding spot.
     *
     * @param entering    true if entering a hiding spot, false if exiting
     * @param achievementCallback callback for achievements (may be null)
     * @return true if the action was accepted
     */
    public boolean toggleHiding(boolean entering, AchievementCallback achievementCallback) {
        if (entering) {
            isHiding = true;
            hidingProgress = 0f;
        } else {
            isHiding = false;
            hidingProgress = 0f;
        }
        return true;
    }

    /**
     * Called when the player successfully fully hides in a WHEELIE_BIN.
     *
     * @param achievementCallback callback for achievements (may be null)
     */
    public void onWheeliBinHidden(AchievementCallback achievementCallback) {
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.WHEELIE_BIN_HERO);
        }
    }

    // ── Escape routes ──────────────────────────────────────────────────────────

    /**
     * Attempt a disguise change escape route. Resets police description and
     * removes LKP reference if at ≤3 stars.
     * Can only be used once per pursuit.
     *
     * @param disguiseSystem the active disguise system (must have a disguise equipped)
     * @param player the player
     * @param npcs   all NPCs
     * @param achievementCallback callback for achievements (may be null)
     * @return the result of the attempt
     */
    public DisguiseEscapeResult attemptDisguiseEscape(DisguiseSystem disguiseSystem,
                                                       Player player, List<NPC> npcs,
                                                       AchievementCallback achievementCallback) {
        if (wantedStars > DISGUISE_RESET_MAX_STARS) {
            return DisguiseEscapeResult.TOO_MANY_STARS;
        }
        if (disguiseUsedThisPursuit) {
            return DisguiseEscapeResult.ALREADY_USED;
        }
        if (disguiseSystem == null || !disguiseSystem.isDisguised()) {
            return DisguiseEscapeResult.NOT_DISGUISED;
        }

        // Reset police description — all chasing NPCs lose the player
        disguiseUsedThisPursuit = true;
        resetAllChasingNpcs(npcs);
        // Do not remove wanted stars — police still know a crime happened
        // but they have lost the description of the suspect
        decayTimer = 0f; // restart decay from now (clean slate for description)

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.INNOCENT_FACE);
        }

        return DisguiseEscapeResult.SUCCESS;
    }

    /**
     * Result of a disguise escape attempt.
     */
    public enum DisguiseEscapeResult {
        SUCCESS,
        TOO_MANY_STARS,
        ALREADY_USED,
        NOT_DISGUISED
    }

    /**
     * Attempt to bribe a PCSO to look the other way.
     *
     * @param pcsoNpc   the PCSO NPC being bribed
     * @param inventory the player's inventory
     * @param notoriety the player's current notoriety score
     * @param achievementCallback callback for achievements (may be null)
     * @return the result of the bribe attempt
     */
    public BribeResult attemptBribePcso(NPC pcsoNpc, Inventory inventory, int notoriety,
                                         AchievementCallback achievementCallback) {
        if (pcsoNpc == null || pcsoNpc.getType() != NPCType.PCSO) {
            return BribeResult.NOT_PCSO;
        }
        if (notoriety >= BRIBE_MAX_NOTORIETY) {
            return BribeResult.TOO_NOTORIOUS;
        }
        if (wantedStars == 0) {
            return BribeResult.NOT_WANTED;
        }

        int baseCost = BRIBE_COST_PER_STAR * wantedStars;
        float multiplier = (hasCorruptPcso && pcsoNpc == corruptPcsoNpc)
                ? CORRUPT_PCSO_BRIBE_MULTIPLIER : 1.0f;
        int cost = Math.max(1, (int) (baseCost * multiplier));

        if (inventory.getItemCount(Material.COIN) < cost) {
            return BribeResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, cost);

        // Bribe succeeds: drop one wanted star
        wantedStars = Math.max(0, wantedStars - 1);
        accumulatedSeverity = Math.max(0, accumulatedSeverity - 1);
        if (wantedStars == 0) {
            clearPursuit();
        }

        // Reset the PCSO to normal
        pcsoNpc.setState(NPCState.PATROLLING);
        pcsoNpc.setSpeechText("Alright, alright... I didn't see nothing.", 4f);

        return BribeResult.SUCCESS;
    }

    /**
     * Result of a PCSO bribe attempt.
     */
    public enum BribeResult {
        SUCCESS,
        NOT_PCSO,
        TOO_NOTORIOUS,
        INSUFFICIENT_FUNDS,
        NOT_WANTED
    }

    /**
     * Called when the player enters their safe house squat while being pursued.
     *
     * @param isSquat       true if this is genuinely the player's squat
     * @param achievementCallback callback for achievements (may be null)
     */
    public void onEnterSafeHouse(boolean isSquat, AchievementCallback achievementCallback) {
        if (isSquat && wantedStars > 0) {
            isInSafeHouse = true;
            safeHouseTimer = 0f;
        }
    }

    /**
     * Called when the player leaves their safe house squat.
     */
    public void onExitSafeHouse() {
        isInSafeHouse = false;
        safeHouseTimer = 0f;
    }

    // ── Corrupt PCSO cultivation ───────────────────────────────────────────────

    /**
     * Called when the player gives a FLASK_OF_TEA to a PCSO NPC.
     * After {@link #CORRUPT_PCSO_TEA_INTERACTIONS} interactions, the PCSO is
     * cultivated as a corrupt contact.
     *
     * @param pcsoNpc the PCSO receiving the tea
     * @param inventory the player's inventory
     * @param achievementCallback callback for achievements (may be null)
     * @return true if the PCSO was newly cultivated as corrupt
     */
    public boolean offerTeaToPcso(NPC pcsoNpc, Inventory inventory,
                                   AchievementCallback achievementCallback) {
        if (pcsoNpc == null || pcsoNpc.getType() != NPCType.PCSO) return false;
        if (inventory.getItemCount(Material.FLASK_OF_TEA) < 1) return false;

        inventory.removeItem(Material.FLASK_OF_TEA, 1);
        pcsoNpc.setSpeechText("Ah, cheers. You're alright, you are.", 4f);

        // Track progress with this specific PCSO
        if (corruptPcsoNpc == null || corruptPcsoNpc == pcsoNpc) {
            corruptPcsoNpc = pcsoNpc;
            corruptPcsoTeaCount++;
            if (corruptPcsoTeaCount >= CORRUPT_PCSO_TEA_INTERACTIONS && !hasCorruptPcso) {
                hasCorruptPcso = true;
                pcsoNpc.setSpeechText(
                    "Listen... if you see the lads coming, I'll give you a heads-up. You owe me.", 5f);
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.BENT_COPPER);
                }
                return true;
            }
        }
        return false;
    }

    // ── Arrest sequence ────────────────────────────────────────────────────────

    /**
     * Apply the wanted-system components of the arrest sequence.
     * Called after ArrestSystem.arrest() to apply the fine and notoriety gain.
     *
     * @param inventory         the player's inventory (fine deducted)
     * @param criminalRecord    the player's criminal record
     * @param notorietySystem   the notoriety system (may be null)
     * @param notorietyCallback callback for notoriety achievements (may be null)
     * @return the fine amount in COIN that was deducted
     */
    public int applyArrestConsequences(Inventory inventory, CriminalRecord criminalRecord,
                                        NotorietySystem notorietySystem,
                                        NotorietySystem.AchievementCallback notorietyCallback) {
        int fine = ARREST_FINE_PER_STAR * wantedStars;

        // Deduct fine (as many coins as available if not enough)
        int coinAvailable = inventory.getItemCount(Material.COIN);
        int coinDeducted = Math.min(fine, coinAvailable);
        if (coinDeducted > 0) {
            inventory.removeItem(Material.COIN, coinDeducted);
        }

        // Criminal record update
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.TIMES_ARRESTED);
        }

        // Notoriety gain
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(ARREST_NOTORIETY_GAIN, notorietyCallback);
        }

        // Clear the pursuit
        clearPursuit();

        return coinDeducted;
    }

    // ── Scenario: build arrest message ────────────────────────────────────────

    /**
     * Build the arrest message for the wanted system component.
     *
     * @param fine the fine actually deducted
     * @return a flavour message
     */
    public static String buildWantedArrestMessage(int fine, int stars) {
        StringBuilder sb = new StringBuilder();
        sb.append("Banged up! ");
        if (stars > 0) {
            sb.append(stars).append("-star collar. ");
        }
        if (fine > 0) {
            sb.append("Fined ").append(fine).append(" coin. ");
        }
        sb.append("Released at the station — with a criminal record update.");
        return sb.toString();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void updatePoliceLoS(Player player, List<NPC> npcs, float losRange) {
        boolean anyLos = false;
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (!isPoliceNpc(npc)) continue;
            if (npc.getState() == NPCState.KNOCKED_OUT) continue;

            float dist = npc.getPosition().dst(player.getPosition());
            if (dist <= losRange) {
                anyLos = true;
                // Update LKP to player's current position
                lastKnownPosition.set(player.getPosition());
                break;
            }
        }
        policeHasLos = anyLos;
    }

    private void updateDecayTimers(float delta, Player player, AchievementCallback achievementCallback) {
        if (wantedStars == 0) return;

        if (policeHasLos) {
            // Reset decay when police have LOS
            decayTimer = 0f;
            losBreakTimer = 0f;
            losContactTimer += delta;
        } else {
            // Count up toward decay
            losBreakTimer += delta;
            losContactTimer = 0f;
            decayTimer += delta;

            if (decayTimer >= DECAY_SECONDS_PER_STAR) {
                decayTimer -= DECAY_SECONDS_PER_STAR;
                wantedStars--;
                accumulatedSeverity = Math.max(0, accumulatedSeverity - 1);

                if (wantedStars == 0) {
                    clearPursuit();
                    if (!cleanGetawayAwarded) {
                        cleanGetawayAwarded = true;
                        if (achievementCallback != null) {
                            achievementCallback.award(AchievementType.CLEAN_GETAWAY_PURSUIT);
                        }
                    }
                }
            }
        }
    }

    private void updateHidingProgress(float delta) {
        if (isHiding && hidingProgress < 1f) {
            hidingProgress = Math.min(1f, hidingProgress + delta / HIDING_ENTER_DURATION);
        }
    }

    private void updateSafeHouseTimer(float delta, AchievementCallback achievementCallback) {
        if (!isInSafeHouse || wantedStars == 0) return;

        // Police won't enter safe house at ≤ threshold stars
        if (wantedStars <= SAFE_HOUSE_POLICE_ENTRY_THRESHOLD) {
            safeHouseTimer += delta;
            if (safeHouseTimer >= SAFE_HOUSE_DURATION) {
                // Wanted level cleared
                clearPursuit();
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.CLEAN_GETAWAY_PURSUIT);
                }
            }
        }
    }

    private void updateSearchPhase(float delta, Player player, List<NPC> npcs) {
        if (searchPhaseActive) {
            searchTimer += delta;
            if (searchTimer >= SEARCH_DURATION_SECONDS) {
                searchPhaseActive = false;
                searchTimer = 0f;
                // Return searching NPCs to patrol state
                for (NPC npc : searchingNpcs) {
                    if (npc.isAlive() && npc.getState() == NPCState.SEARCHING) {
                        npc.setState(NPCState.PATROLLING);
                    }
                }
                searchingNpcs.clear();
            }
        }
    }

    private void updateLegItTracking(float delta, Player player, AchievementCallback achievementCallback) {
        if (wantedStars == 0 || inPursuit && policeHasLos) return;

        // Calculate distance from LKP
        legItCurrentDistance = lastKnownPosition.dst(player.getPosition());

        if (!policeHasLos) {
            legItLosBreakTimer += delta;
        } else {
            legItLosBreakTimer = 0f;
        }

        // Check leg-it conditions
        if (!legItConditionMet
                && legItCurrentDistance >= LEG_IT_DISTANCE
                && legItLosBreakTimer >= LEG_IT_LOS_BREAK_SECONDS) {
            legItConditionMet = true;
            // Apply −2 stars bonus
            int oldStars = wantedStars;
            wantedStars = Math.max(0, wantedStars - LEG_IT_STAR_REDUCTION);
            accumulatedSeverity = Math.max(0, accumulatedSeverity - LEG_IT_STAR_REDUCTION);

            if (wantedStars == 0) {
                clearPursuit();
            }

            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.LEG_IT);
            }
        }
    }

    private void updatePoliceNpcStates(Player player, List<NPC> npcs, float losRange) {
        if (wantedStars == 0) {
            // Clear any residual chase states
            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                if (npc.getState() == NPCState.CHASING_PLAYER
                        || npc.getState() == NPCState.SEARCHING) {
                    npc.setState(NPCState.PATROLLING);
                }
            }
            return;
        }

        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (!isPoliceNpc(npc)) continue;
            if (npc.getState() == NPCState.KNOCKED_OUT
                    || npc.getState() == NPCState.ARRESTING) continue;

            float dist = npc.getPosition().dst(player.getPosition());
            if (dist <= losRange) {
                // Police has LOS — chase
                npc.setState(NPCState.CHASING_PLAYER);
                npc.setTargetPosition(player.getPosition().cpy());
            } else if (npc.getState() == NPCState.CHASING_PLAYER) {
                // Lost LOS — enter search mode
                npc.setState(NPCState.SEARCHING);
                npc.setTargetPosition(lastKnownPosition.cpy());
                if (!searchingNpcs.contains(npc)) {
                    searchingNpcs.add(npc);
                }
                if (!searchPhaseActive) {
                    searchPhaseActive = true;
                    searchTimer = 0f;
                }
            }
        }
    }

    private void resetAllChasingNpcs(List<NPC> npcs) {
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            if (npc.getState() == NPCState.CHASING_PLAYER
                    || npc.getState() == NPCState.SEARCHING
                    || npc.getState() == NPCState.AGGRESSIVE) {
                if (isPoliceNpc(npc)) {
                    npc.setState(NPCState.PATROLLING);
                    npc.setTargetPosition(null);
                }
            }
        }
        searchingNpcs.clear();
        searchPhaseActive = false;
        searchTimer = 0f;
        policeHasLos = false;
        losBreakTimer = 0f;
    }

    private void clearPursuit() {
        wantedStars = 0;
        accumulatedSeverity = 0;
        inPursuit = false;
        decayTimer = 0f;
        losBreakTimer = 0f;
        losContactTimer = 0f;
        legItConditionMet = false;
        legItLosBreakTimer = 0f;
        legItCurrentDistance = 0f;
        searchPhaseActive = false;
        searchTimer = 0f;
        searchingNpcs.clear();
        isInSafeHouse = false;
        safeHouseTimer = 0f;
        disguiseUsedThisPursuit = false;
    }

    private boolean isPoliceNpc(NPC npc) {
        return npc.getType() == NPCType.POLICE
                || npc.getType() == NPCType.PCSO
                || npc.getType() == NPCType.ARMED_RESPONSE;
    }

    private float computeLosRange(Weather weather, boolean isNight) {
        float range = POLICE_BASE_LOS_RANGE;
        if (isNight) range -= NIGHT_LOS_REDUCTION;
        if (weather == Weather.RAIN || weather == Weather.THUNDERSTORM) range -= RAIN_LOS_REDUCTION;
        if (weather == Weather.FOG) range -= FOG_LOS_REDUCTION;
        return Math.max(4f, range); // never less than 4 blocks
    }

    /**
     * Compute wanted stars from accumulated severity points.
     */
    private static int computeStarsFromSeverity(int severity) {
        for (int i = SEVERITY_THRESHOLDS.length - 1; i >= 1; i--) {
            if (severity >= SEVERITY_THRESHOLDS[i]) return i;
        }
        return 0;
    }

    // ── RaveSystem integration ─────────────────────────────────────────────────

    /**
     * Returns the police alert speed multiplier. When a rave is active and
     * wanted ≥ {@link #RAVE_DOUBLE_ALERT_MIN_STARS}, this returns 2.0f.
     *
     * @param isRaveActive true if a rave is currently running
     * @return 2.0f if rave + wanted ≥ 2, otherwise 1.0f
     */
    public float getRaveAlertSpeedMultiplier(boolean isRaveActive) {
        if (isRaveActive && wantedStars >= RAVE_DOUBLE_ALERT_MIN_STARS) {
            return 2.0f;
        }
        return 1.0f;
    }

    // ── StreetEconomy integration ──────────────────────────────────────────────

    /**
     * Returns true if dodgy street trades are currently risky (wanted ≥ 1).
     */
    public boolean isDodgyTradeRisky() {
        return wantedStars >= 1;
    }

    // ── FactionSystem integration (Council Crackdown) ─────────────────────────

    /**
     * Returns true if a Council Crackdown lockdown is in effect.
     * At 5 stars during COUNCIL_CRACKDOWN, all faction NPCs become hostile.
     *
     * @param isCouncilCrackdown true if the COUNCIL_CRACKDOWN event is active
     */
    public boolean isCouncilLockdown(boolean isCouncilCrackdown) {
        return wantedStars >= 5 && isCouncilCrackdown;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    /** Current wanted stars (0–5). */
    public int getWantedStars() {
        return wantedStars;
    }

    /** Whether there is an active pursuit. */
    public boolean isInPursuit() {
        return inPursuit;
    }

    /** Whether police currently have line-of-sight on the player. */
    public boolean isPoliceHasLos() {
        return policeHasLos;
    }

    /** Current timer for decay progress toward losing one star. */
    public float getDecayTimer() {
        return decayTimer;
    }

    /** How long the player has been outside all police LOS. */
    public float getLosBreakTimer() {
        return losBreakTimer;
    }

    /** Last-known-position of the player (as seen by police). */
    public Vector3 getLastKnownPosition() {
        return lastKnownPosition;
    }

    /** Whether the player is currently hiding. */
    public boolean isHiding() {
        return isHiding;
    }

    /** Progress of entering a hiding spot (0.0 = not hidden, 1.0 = fully hidden). */
    public float getHidingProgress() {
        return hidingProgress;
    }

    /** Whether the player is in a safe house. */
    public boolean isInSafeHouse() {
        return isInSafeHouse;
    }

    /** Safe house timer progress (seconds). */
    public float getSafeHouseTimer() {
        return safeHouseTimer;
    }

    /** Whether a corrupt PCSO contact has been cultivated. */
    public boolean hasCorruptPcso() {
        return hasCorruptPcso;
    }

    /** Number of tea interactions with the current PCSO target. */
    public int getCorruptPcsoTeaCount() {
        return corruptPcsoTeaCount;
    }

    /** The corrupt PCSO NPC (or null if not cultivated). */
    public NPC getCorruptPcsoNpc() {
        return corruptPcsoNpc;
    }

    /** Whether a disguise change has been used in this pursuit. */
    public boolean isDisguiseUsedThisPursuit() {
        return disguiseUsedThisPursuit;
    }

    /** Whether a search phase is currently active. */
    public boolean isSearchPhaseActive() {
        return searchPhaseActive;
    }

    /** Remaining search phase duration. */
    public float getSearchTimer() {
        return searchTimer;
    }

    /** Accumulated crime severity this pursuit. */
    public int getAccumulatedSeverity() {
        return accumulatedSeverity;
    }

    /** Current distance from the last-known-position (for leg-it tracking). */
    public float getLegItCurrentDistance() {
        return legItCurrentDistance;
    }

    /** Current leg-it LOS break timer. */
    public float getLegItLosBreakTimer() {
        return legItLosBreakTimer;
    }

    /** Whether the leg-it escape condition has been met this pursuit. */
    public boolean isLegItConditionMet() {
        return legItConditionMet;
    }

    /**
     * Returns the effective bribe cost for the given wanted level, accounting
     * for the corrupt PCSO multiplier.
     *
     * @param targetPcso the PCSO being bribed
     * @return coin cost
     */
    public int getBribeCost(NPC targetPcso) {
        int base = BRIBE_COST_PER_STAR * wantedStars;
        if (hasCorruptPcso && targetPcso != null && targetPcso == corruptPcsoNpc) {
            return Math.max(1, (int) (base * CORRUPT_PCSO_BRIBE_MULTIPLIER));
        }
        return base;
    }

    // ── Force-set for testing ──────────────────────────────────────────────────

    /** Force-set wanted stars (for testing). */
    public void setWantedStarsForTesting(int stars) {
        this.wantedStars = Math.max(0, Math.min(MAX_WANTED_STARS, stars));
        this.inPursuit = this.wantedStars > 0;
    }

    /** Force-set the LKP (for testing). */
    public void setLastKnownPositionForTesting(float x, float y, float z) {
        this.lastKnownPosition.set(x, y, z);
    }

    /** Force-set police LOS (for testing). */
    public void setPoliceHasLosForTesting(boolean hasLos) {
        this.policeHasLos = hasLos;
    }

    /** Force-set the decay timer (for testing). */
    public void setDecayTimerForTesting(float timer) {
        this.decayTimer = timer;
    }

    /** Force-set the LOS break timer (for testing). */
    public void setLosBreakTimerForTesting(float timer) {
        this.losBreakTimer = timer;
    }

    /** Force-set accumulated severity (for testing). */
    public void setAccumulatedSeverityForTesting(int severity) {
        this.accumulatedSeverity = severity;
    }
}
