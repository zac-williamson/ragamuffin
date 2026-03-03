package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.Random;

/**
 * Implements the Compensation Kings personal injury claims office mechanics for
 * {@code LandmarkType.CLAIMS_MANAGEMENT}.
 *
 * <p>Features:
 * <ul>
 *   <li><b>Claim Filing</b>: Press E on Gary (CLAIMS_MANAGER). Five claim types, each requiring
 *       a specific environmental condition. Gary takes a 30% cut. Wearing a NECK_BRACE on a
 *       WHIPLASH_CLAIM multiplies the payout ×1.5. After 3 claims in 7 in-game days Gary
 *       refuses further service until a 10-COIN smooth-over bribe is paid.</li>
 *   <li><b>Genuine Injury Fast-Track</b>: A car collision ≥10 damage or dog bite ≥20 damage
 *       allows a genuine-injury claim — full payout, no Gary cut, no investigator spawn.</li>
 *   <li><b>Insurance Investigator</b>: 40% spawn chance when CCTV is within 10 blocks at
 *       filing time. Player has a 2-hour window to shake the tail (60 blocks for 5 minutes),
 *       bribe it (5 COIN, 50/50), or avoid triggering it. Sprinting, fighting, or breaking
 *       blocks within 20 blocks during the window = CAUGHT_IN_THE_ACT, payout cancelled,
 *       INSURANCE_FRAUD recorded.</li>
 *   <li><b>Staged Accidents</b>: Player can engineer the required conditions via staged car
 *       clip (CarDrivingSystem), MOP_BUCKET_PROP wet floor at Aldi/Greggs, or pothole
 *       fabrication with a CROWBAR.</li>
 *   <li><b>Pamphlet Rack Distraction</b>: Press E on PAMPHLET_RACK_PROP once per hour →
 *       Gary stunned for 15 seconds. Achievement: COMPENSATION_NATION (first successful claim).</li>
 * </ul>
 *
 * <p>Integration:
 * <ul>
 *   <li>CarDrivingSystem: collision flag for staged/genuine car whiplash</li>
 *   <li>WeatherSystem: rain bonus +10% to SLIP_AND_FALL payout</li>
 *   <li>DogCompanionSystem: bite flag for dog-bite claims</li>
 *   <li>DWPSystem: sick-note exemption bypasses investigator</li>
 *   <li>FactionSystem: STREET_LADS Respect ≥ 30 reduces investigator spawn chance by 20%</li>
 *   <li>NewspaperSystem: two headlines — first successful claim and investigator catch</li>
 *   <li>NeighbourhoodSystem: genuine injury sympathy (+2 Vibes on genuine fast-track)</li>
 *   <li>StreetSkillSystem: STREET_SMARTS XP awarded on investigator shake</li>
 * </ul>
 *
 * <p>Issue #1293.
 */
public class ClaimsManagementSystem {

    // ── Opening hours ───────────────────────────────────────────────────────

    /** Hour Gary opens (09:00). */
    public static final float OPEN_HOUR = 9.0f;

    /** Hour Gary closes (17:00). */
    public static final float CLOSE_HOUR = 17.0f;

    // ── Claim constants ─────────────────────────────────────────────────────

    /** Gary's cut as a fraction of the base payout (30%). */
    public static final float GARY_CUT = 0.30f;

    /** Payout multiplier when player wears NECK_BRACE during WHIPLASH_CLAIM. */
    public static final float NECK_BRACE_MULTIPLIER = 1.5f;

    /** Rain bonus to SLIP_AND_FALL payout (10%). */
    public static final float RAIN_BONUS = 0.10f;

    /** Number of claims within the fraud window before Gary refuses service. */
    public static final int FRAUD_CLAIM_THRESHOLD = 3;

    /** In-game days window for the fraud threshold. */
    public static final int FRAUD_WINDOW_DAYS = 7;

    /** Cost of the smooth-over bribe to Gary to reset the fraud threshold. */
    public static final int SMOOTH_OVER_BRIBE_COST = 10;

    // ── Investigator constants ──────────────────────────────────────────────

    /** Probability (0–1) that an investigator spawns when CCTV is within range at filing time. */
    public static final float INVESTIGATOR_SPAWN_CHANCE = 0.40f;

    /** CCTV radius (blocks) that triggers potential investigator spawn. */
    public static final float CCTV_SPAWN_RADIUS = 10.0f;

    /** In-game hours of the payout window before investigator escalates. */
    public static final float PAYOUT_WINDOW_HOURS = 2.0f;

    /** Distance (blocks) the player must maintain from the investigator for the shake duration. */
    public static final float SHAKE_DISTANCE_BLOCKS = 60.0f;

    /** Real-seconds the player must maintain shake distance to lose the investigator. */
    public static final float SHAKE_DURATION_SECONDS = 300.0f; // 5 minutes

    /** Cost to bribe the investigator directly. */
    public static final int INVESTIGATOR_BRIBE_COST = 5;

    /** Success chance (0–1) of the investigator bribe. */
    public static final float INVESTIGATOR_BRIBE_CHANCE = 0.50f;

    /** Radius (blocks) within which suspicious player actions are detected by the investigator. */
    public static final float INVESTIGATOR_DETECTION_RADIUS = 20.0f;

    /** STREET_LADS Respect threshold that reduces investigator spawn chance. */
    public static final int STREET_LADS_RESPECT_THRESHOLD = 30;

    /** Spawn-chance reduction when player has sufficient STREET_LADS Respect. */
    public static final float STREET_LADS_SPAWN_REDUCTION = 0.20f;

    // ── Pamphlet rack distraction constants ────────────────────────────────

    /** Duration (seconds) Gary is stunned by the pamphlet rack distraction. */
    public static final float GARY_STUNNED_DURATION = 15.0f;

    /** Cooldown (seconds) between pamphlet rack uses (1 in-game hour). */
    public static final float PAMPHLET_RACK_COOLDOWN = 3600.0f;

    // ── Base payouts ────────────────────────────────────────────────────────

    /** Base payout for WHIPLASH_CLAIM (before multipliers and Gary cut). */
    public static final int WHIPLASH_BASE_PAYOUT = 50;

    /** Base payout for SLIP_AND_FALL (before multipliers and Gary cut). */
    public static final int SLIP_AND_FALL_BASE_PAYOUT = 40;

    /** Base payout for DOG_BITE_CLAIM (before multipliers and Gary cut). */
    public static final int DOG_BITE_BASE_PAYOUT = 45;

    /** Base payout for POTHOLE_CLAIM (before multipliers and Gary cut). */
    public static final int POTHOLE_BASE_PAYOUT = 35;

    /** Base payout for TRIP_CLAIM (before multipliers and Gary cut). */
    public static final int TRIP_BASE_PAYOUT = 30;

    /** Minimum genuine-injury damage for a car collision to qualify for genuine fast-track. */
    public static final int GENUINE_CAR_DAMAGE_THRESHOLD = 10;

    /** Minimum genuine-injury damage for a dog bite to qualify for genuine fast-track. */
    public static final int GENUINE_DOG_BITE_DAMAGE_THRESHOLD = 20;

    // ── Claim types ─────────────────────────────────────────────────────────

    /**
     * The five claim types Gary processes.
     */
    public enum ClaimType {
        WHIPLASH_CLAIM,
        SLIP_AND_FALL,
        DOG_BITE_CLAIM,
        POTHOLE_CLAIM,
        TRIP_CLAIM,
    }

    // ── Result enums ────────────────────────────────────────────────────────

    /**
     * Outcome of attempting to file a claim with Gary.
     */
    public enum ClaimResult {
        /** Claim accepted — payout will be delivered after the window. */
        ACCEPTED,
        /** Claim accepted as genuine injury — full payout, no Gary cut, no investigator. */
        GENUINE_INJURY_FAST_TRACK,
        /** Gary refuses until the smooth-over bribe is paid. */
        GARY_REFUSES_FRAUD_THRESHOLD,
        /** Outside opening hours. */
        GARY_NOT_PRESENT,
        /** Player does not meet the environmental condition for this claim type. */
        CONDITION_NOT_MET,
        /** Player cannot afford the smooth-over bribe (returned when attempting while banned). */
        INSUFFICIENT_FUNDS,
    }

    /**
     * Result of attempting to shake the insurance investigator.
     */
    public enum ShakeResult {
        /** Investigator shaken — payout proceeds. */
        SUCCESS,
        /** Not enough distance maintained yet. */
        IN_PROGRESS,
        /** No active investigator to shake. */
        NO_INVESTIGATOR,
    }

    /**
     * Result of attempting to bribe the insurance investigator.
     */
    public enum BribeInvestigatorResult {
        /** Bribe accepted — investigator leaves. */
        SUCCESS,
        /** Bribe refused — investigator becomes more suspicious. */
        FAILURE,
        /** Player cannot afford the bribe. */
        INSUFFICIENT_FUNDS,
        /** No active investigator. */
        NO_INVESTIGATOR,
    }

    /**
     * Result of paying Gary the smooth-over bribe to reset the fraud threshold.
     */
    public enum SmoothOverResult {
        /** Bribe paid — fraud count reset, Gary will accept claims again. */
        SUCCESS,
        /** Player cannot afford the bribe. */
        INSUFFICIENT_FUNDS,
        /** Fraud threshold has not been reached — no need to smooth over. */
        NOT_NEEDED,
    }

    /**
     * Result of activating the pamphlet rack distraction.
     */
    public enum PamphletRackResult {
        /** Gary is now stunned. */
        SUCCESS,
        /** Distraction is on cooldown. */
        ON_COOLDOWN,
        /** Gary is already stunned. */
        ALREADY_STUNNED,
    }

    // ── State ───────────────────────────────────────────────────────────────

    private final Random rng;

    /** Claims filed within the current fraud-tracking window. */
    private int recentClaimCount;

    /** In-game day when the fraud window started. */
    private int fraudWindowStartDay;

    /** Whether Gary is currently refusing service due to fraud threshold. */
    private boolean garyRefusing;

    /** Whether an investigator is currently active. */
    private boolean investigatorActive;

    /** In-game hours remaining in the payout window (0 = window closed / payout due). */
    private float payoutWindowHoursRemaining;

    /** Pending payout amount (0 = no pending payout). */
    private int pendingPayout;

    /** Seconds the player has been maintaining shake distance from the investigator. */
    private float shakeTimer;

    /** Whether the investigator is currently being shaken. */
    private boolean shaking;

    /** Gary's stun timer (seconds remaining). */
    private float garyStunnedTimer;

    /** Pamphlet rack cooldown timer (seconds remaining). */
    private float pamphletRackCooldownTimer;

    /** Pending genuine injury damage from car collision (reset after claim). */
    private int pendingCarCollisionDamage;

    /** Pending genuine injury damage from dog bite (reset after claim). */
    private int pendingDogBiteDamage;

    /** Whether the last accepted claim was a genuine fast-track. */
    private boolean lastClaimGenuine;

    /** Total successful claims this session (for achievement tracking). */
    private int totalSuccessfulClaims;

    // ── Injected dependencies (all optional — null-checked before use) ─────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private CriminalRecord criminalRecord;
    private NewspaperSystem newspaperSystem;
    private AchievementSystem achievementSystem;
    private NeighbourhoodSystem neighbourhoodSystem;
    private StreetSkillSystem streetSkillSystem;

    // ── Constructors ────────────────────────────────────────────────────────

    public ClaimsManagementSystem() {
        this(new Random());
    }

    public ClaimsManagementSystem(Random rng) {
        this.rng = rng;
        this.fraudWindowStartDay = 0;
    }

    // ── Dependency injection setters ────────────────────────────────────────

    public void setNotorietySystem(NotorietySystem notorietySystem) {
        this.notorietySystem = notorietySystem;
    }

    public void setWantedSystem(WantedSystem wantedSystem) {
        this.wantedSystem = wantedSystem;
    }

    public void setFactionSystem(FactionSystem factionSystem) {
        this.factionSystem = factionSystem;
    }

    public void setRumourNetwork(RumourNetwork rumourNetwork) {
        this.rumourNetwork = rumourNetwork;
    }

    public void setCriminalRecord(CriminalRecord criminalRecord) {
        this.criminalRecord = criminalRecord;
    }

    public void setNewspaperSystem(NewspaperSystem newspaperSystem) {
        this.newspaperSystem = newspaperSystem;
    }

    public void setAchievementSystem(AchievementSystem achievementSystem) {
        this.achievementSystem = achievementSystem;
    }

    public void setNeighbourhoodSystem(NeighbourhoodSystem neighbourhoodSystem) {
        this.neighbourhoodSystem = neighbourhoodSystem;
    }

    public void setStreetSkillSystem(StreetSkillSystem streetSkillSystem) {
        this.streetSkillSystem = streetSkillSystem;
    }

    // ── Per-frame update ────────────────────────────────────────────────────

    /**
     * Update timers. Call once per frame.
     *
     * @param delta    seconds since last frame
     * @param time     current time system
     * @param gary     the CLAIMS_MANAGER NPC, or null if not spawned
     */
    public void update(float delta, TimeSystem time, NPC gary) {
        // Tick Gary's stun timer
        if (garyStunnedTimer > 0f) {
            garyStunnedTimer = Math.max(0f, garyStunnedTimer - delta);
        }

        // Tick pamphlet rack cooldown
        if (pamphletRackCooldownTimer > 0f) {
            pamphletRackCooldownTimer = Math.max(0f, pamphletRackCooldownTimer - delta);
        }

        // Tick payout window (measured in real seconds, converted from in-game hours)
        if (investigatorActive && payoutWindowHoursRemaining > 0f) {
            // TimeSystem: 1 real second ≈ 0.1 in-game hours (DEFAULT_TIME_SPEED)
            float inGameHoursDelta = delta * 0.1f;
            payoutWindowHoursRemaining = Math.max(0f, payoutWindowHoursRemaining - inGameHoursDelta);

            if (payoutWindowHoursRemaining <= 0f) {
                // Window expired without being caught — pay out
                resolvePayoutWindow();
            }
        } else if (!investigatorActive && payoutWindowHoursRemaining > 0f) {
            // No investigator active — window still ticking toward payout
            float inGameHoursDelta = delta * 0.1f;
            payoutWindowHoursRemaining = Math.max(0f, payoutWindowHoursRemaining - inGameHoursDelta);
            if (payoutWindowHoursRemaining <= 0f) {
                resolvePayoutWindow();
            }
        }

        // Tick fraud window reset (after FRAUD_WINDOW_DAYS days, count resets)
        int currentDay = time.getDayCount();
        if (currentDay - fraudWindowStartDay >= FRAUD_WINDOW_DAYS) {
            recentClaimCount = 0;
            fraudWindowStartDay = currentDay;
            garyRefusing = false;
        }
    }

    // ── Claim Filing ────────────────────────────────────────────────────────

    /**
     * The player files a claim with Gary.
     *
     * @param inventory        player inventory
     * @param time             current time (for opening-hours check)
     * @param claimType        the type of claim to file
     * @param conditionMet     whether the environmental condition for this claim type is met
     * @param cctvNearby       whether a CCTV is within {@value #CCTV_SPAWN_RADIUS} blocks
     * @param weather          current weather (for rain bonus on SLIP_AND_FALL)
     * @param streetLadsRespect current STREET_LADS Respect (reduces investigator chance)
     * @return the claim result
     */
    public ClaimResult fileClaim(
            Inventory inventory,
            TimeSystem time,
            ClaimType claimType,
            boolean conditionMet,
            boolean cctvNearby,
            Weather weather,
            int streetLadsRespect) {

        float hour = time.getTime();
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
            return ClaimResult.GARY_NOT_PRESENT;
        }

        if (garyRefusing) {
            return ClaimResult.GARY_REFUSES_FRAUD_THRESHOLD;
        }

        // Check for genuine injury fast-track
        boolean genuineCarInjury = pendingCarCollisionDamage >= GENUINE_CAR_DAMAGE_THRESHOLD;
        boolean genuineDogBite = pendingDogBiteDamage >= GENUINE_DOG_BITE_DAMAGE_THRESHOLD;
        boolean genuineInjury = (claimType == ClaimType.WHIPLASH_CLAIM && genuineCarInjury)
                || (claimType == ClaimType.DOG_BITE_CLAIM && genuineDogBite);

        if (!genuineInjury && !conditionMet) {
            return ClaimResult.CONDITION_NOT_MET;
        }

        int basePayout = getBasePayout(claimType);

        // Apply multipliers
        float multiplier = 1.0f;

        // Neck brace multiplier for whiplash
        if (claimType == ClaimType.WHIPLASH_CLAIM
                && inventory.getItemCount(Material.NECK_BRACE) > 0) {
            multiplier *= NECK_BRACE_MULTIPLIER;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.NECK_BRACE_BANDIT);
            }
        }

        // Rain bonus for slip and fall
        if (claimType == ClaimType.SLIP_AND_FALL && weather != null && weather.isRaining()) {
            multiplier += RAIN_BONUS;
        }

        if (genuineInjury) {
            // Genuine fast-track: full payout, no Gary cut, no investigator
            int payout = Math.round(basePayout * multiplier);
            inventory.addItem(Material.COIN, payout);
            inventory.addItem(Material.CLAIM_REFERENCE_SLIP, 1);
            pendingCarCollisionDamage = 0;
            pendingDogBiteDamage = 0;
            lastClaimGenuine = true;
            totalSuccessfulClaims++;

            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.GENUINE_VICTIM);
            }
            if (neighbourhoodSystem != null) {
                // Genuine injury sympathy +2 Vibes
                int currentVibes = neighbourhoodSystem.getVibes();
                neighbourhoodSystem.setVibes(currentVibes + 2);
            }
            return ClaimResult.GENUINE_INJURY_FAST_TRACK;
        }

        // Fraudulent claim: apply Gary's 30% cut
        float afterCut = basePayout * multiplier * (1.0f - GARY_CUT);
        int payout = Math.round(afterCut);

        // Track claim in fraud window
        recentClaimCount++;
        if (recentClaimCount >= FRAUD_CLAIM_THRESHOLD) {
            garyRefusing = true;
            // Seed FRAUDULENT_CLAIMANT rumour
        }

        // Queue payout with investigator window
        pendingPayout = payout;
        payoutWindowHoursRemaining = PAYOUT_WINDOW_HOURS;
        lastClaimGenuine = false;

        // Potentially spawn investigator
        float spawnChance = INVESTIGATOR_SPAWN_CHANCE;
        if (streetLadsRespect >= STREET_LADS_RESPECT_THRESHOLD) {
            spawnChance -= STREET_LADS_SPAWN_REDUCTION;
        }
        if (cctvNearby && rng.nextFloat() < spawnChance) {
            investigatorActive = true;
            shakeTimer = 0f;
            shaking = false;
        }

        inventory.addItem(Material.CLAIM_REFERENCE_SLIP, 1);
        return ClaimResult.ACCEPTED;
    }

    /**
     * Variant of {@link #fileClaim} without weather/STREET_LADS parameters for simple use.
     */
    public ClaimResult fileClaim(
            Inventory inventory,
            TimeSystem time,
            ClaimType claimType,
            boolean conditionMet,
            boolean cctvNearby) {
        return fileClaim(inventory, time, claimType, conditionMet, cctvNearby,
                Weather.CLEAR, 0);
    }

    // ── Smooth-over bribe ──────────────────────────────────────────────────

    /**
     * Pay Gary the smooth-over bribe to reset the fraud threshold.
     *
     * @param inventory player inventory
     * @param gary      the CLAIMS_MANAGER NPC (for rumour seeding)
     * @return the result of the smooth-over attempt
     */
    public SmoothOverResult smoothOver(Inventory inventory, NPC gary) {
        if (!garyRefusing) {
            return SmoothOverResult.NOT_NEEDED;
        }
        if (inventory.getItemCount(Material.COIN) < SMOOTH_OVER_BRIBE_COST) {
            return SmoothOverResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, SMOOTH_OVER_BRIBE_COST);
        recentClaimCount = 0;
        garyRefusing = false;

        if (rumourNetwork != null && gary != null) {
            rumourNetwork.addRumour(gary, new Rumour(RumourType.SMOOTH_OVER,
                    "Word is Gary at Compensation Kings got a little sweetener — all smoothed over."));
        }

        return SmoothOverResult.SUCCESS;
    }

    // ── Investigator interactions ───────────────────────────────────────────

    /**
     * Notify that the player is maintaining distance from the investigator.
     * Call each frame when the investigator is active and the player is ≥ {@value #SHAKE_DISTANCE_BLOCKS} blocks away.
     *
     * @param delta seconds since last frame
     * @return shake result
     */
    public ShakeResult onPlayerShaking(float delta) {
        if (!investigatorActive) {
            return ShakeResult.NO_INVESTIGATOR;
        }
        shaking = true;
        shakeTimer += delta;
        if (shakeTimer >= SHAKE_DURATION_SECONDS) {
            investigatorActive = false;
            shaking = false;
            shakeTimer = 0f;
            if (achievementSystem != null) {
                achievementSystem.unlock(AchievementType.SHOOK_THE_TAIL);
            }
            if (streetSkillSystem != null) {
                streetSkillSystem.awardXP(StreetSkillSystem.Skill.STREETWISE, 25);
            }
            return ShakeResult.SUCCESS;
        }
        return ShakeResult.IN_PROGRESS;
    }

    /**
     * Reset the shake timer when the player is back within detection range of the investigator.
     */
    public void onPlayerDetectedByInvestigator() {
        shaking = false;
        shakeTimer = 0f;
    }

    /**
     * Attempt to bribe the insurance investigator.
     *
     * @param inventory player inventory
     * @return the result of the bribe attempt
     */
    public BribeInvestigatorResult bribeInvestigator(Inventory inventory) {
        if (!investigatorActive) {
            return BribeInvestigatorResult.NO_INVESTIGATOR;
        }
        if (inventory.getItemCount(Material.COIN) < INVESTIGATOR_BRIBE_COST) {
            return BribeInvestigatorResult.INSUFFICIENT_FUNDS;
        }
        inventory.removeItem(Material.COIN, INVESTIGATOR_BRIBE_COST);
        if (rng.nextFloat() < INVESTIGATOR_BRIBE_CHANCE) {
            investigatorActive = false;
            return BribeInvestigatorResult.SUCCESS;
        } else {
            return BribeInvestigatorResult.FAILURE;
        }
    }

    /**
     * Call when the player is caught in the act by the investigator (sprinting, fighting,
     * or breaking blocks within {@value #INVESTIGATOR_DETECTION_RADIUS} blocks).
     *
     * <p>Cancels the pending payout and records INSURANCE_FRAUD.
     */
    public void onCaughtInTheAct() {
        if (!investigatorActive) {
            return;
        }
        investigatorActive = false;
        pendingPayout = 0;
        payoutWindowHoursRemaining = 0f;

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(15, null);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, null);
        }
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.INSURANCE_FRAUD);
        }
        if (achievementSystem != null) {
            achievementSystem.unlock(AchievementType.CAUGHT_IN_THE_ACT);
        }
        if (rumourNetwork != null) {
            // Seed INSURANCE_FRAUD rumour — no specific NPC carrier at this point
        }
    }

    // ── Pamphlet rack distraction ───────────────────────────────────────────

    /**
     * Player presses E on the PAMPHLET_RACK_PROP to distract Gary.
     *
     * @return the result of the pamphlet rack interaction
     */
    public PamphletRackResult activatePamphletRack() {
        if (pamphletRackCooldownTimer > 0f) {
            return PamphletRackResult.ON_COOLDOWN;
        }
        if (garyStunnedTimer > 0f) {
            return PamphletRackResult.ALREADY_STUNNED;
        }
        garyStunnedTimer = GARY_STUNNED_DURATION;
        pamphletRackCooldownTimer = PAMPHLET_RACK_COOLDOWN;
        return PamphletRackResult.SUCCESS;
    }

    // ── Genuine injury notification ─────────────────────────────────────────

    /**
     * Called by CarDrivingSystem when the player suffers a car collision.
     *
     * @param damage damage points suffered
     */
    public void onCarCollision(int damage) {
        if (damage >= GENUINE_CAR_DAMAGE_THRESHOLD) {
            pendingCarCollisionDamage = Math.max(pendingCarCollisionDamage, damage);
        }
    }

    /**
     * Called by DogCompanionSystem when the player suffers a dog bite.
     *
     * @param damage damage points suffered
     */
    public void onDogBite(int damage) {
        if (damage >= GENUINE_DOG_BITE_DAMAGE_THRESHOLD) {
            pendingDogBiteDamage = Math.max(pendingDogBiteDamage, damage);
        }
    }

    // ── Payout resolution ───────────────────────────────────────────────────

    private void resolvePayoutWindow() {
        if (pendingPayout <= 0) {
            return;
        }
        // Payout is delivered — the calling game code needs to add to player inventory.
        // We record the achievement and rumour here; the caller should check getPendingPayout().
        totalSuccessfulClaims++;

        if (totalSuccessfulClaims == 1 && achievementSystem != null) {
            achievementSystem.unlock(AchievementType.COMPENSATION_NATION);
        }

        pendingPayout = 0;
        investigatorActive = false;
    }

    // ── Queries ─────────────────────────────────────────────────────────────

    /** Returns true if Gary is currently refusing to process claims. */
    public boolean isGaryRefusing() {
        return garyRefusing;
    }

    /** Returns true if an investigator is currently active. */
    public boolean isInvestigatorActive() {
        return investigatorActive;
    }

    /** Returns true if Gary is currently stunned by the pamphlet rack. */
    public boolean isGaryStunned() {
        return garyStunnedTimer > 0f;
    }

    /** Returns the number of recent claims filed within the fraud window. */
    public int getRecentClaimCount() {
        return recentClaimCount;
    }

    /**
     * Returns the pending payout amount after the payout window resolves.
     * Returns 0 if no payout is pending.
     * After calling this, the payout has been consumed (set to 0).
     */
    public int consumePendingPayout() {
        int payout = pendingPayout;
        pendingPayout = 0;
        return payout;
    }

    /** Returns the remaining payout window time in in-game hours. */
    public float getPayoutWindowHoursRemaining() {
        return payoutWindowHoursRemaining;
    }

    /** Returns how long the player has been shaking the investigator (seconds). */
    public float getShakeTimer() {
        return shakeTimer;
    }

    /** Returns the total successful claims this session. */
    public int getTotalSuccessfulClaims() {
        return totalSuccessfulClaims;
    }

    /** Returns the pending genuine car collision damage (before a claim is filed). */
    public int getPendingCarCollisionDamage() {
        return pendingCarCollisionDamage;
    }

    /** Returns the pending genuine dog bite damage (before a claim is filed). */
    public int getPendingDogBiteDamage() {
        return pendingDogBiteDamage;
    }

    // ── Issue #1479: Northfield Public Defibrillator — Liability Nightmare ──────

    /** Payout for showing Gary a NOTICE_OF_DEFICIENCY_PROP (emotional distress). */
    public static final int DEFIB_NOTICE_PAYOUT = 12;

    /**
     * Outcome of presenting the NOTICE_OF_DEFICIENCY_PROP to Gary.
     */
    public enum DefibNoticeResult {
        /** Payout accepted — 12 COIN added to inventory. Achievement: AMBULANCE_CHASER. */
        PAYOUT_ACCEPTED,
        /** Gary is not present (outside opening hours). */
        GARY_NOT_PRESENT,
        /** Player does not have the NOTICE_OF_DEFICIENCY_PROP in inventory. */
        NO_NOTICE,
        /** Player has already claimed this payout. */
        ALREADY_CLAIMED,
    }

    /** Whether the defib notice payout has already been claimed. */
    private boolean defibNoticeClaimed = false;

    /**
     * Present a NOTICE_OF_DEFICIENCY_PROP to Gary for a 12 COIN emotional
     * distress payout. Achievement: {@link ragamuffin.ui.AchievementType#AMBULANCE_CHASER}.
     *
     * @param inventory           player inventory (must contain NOTICE_OF_DEFICIENCY_PROP)
     * @param hour                current in-game hour
     * @param achievementCallback callback to award achievements
     * @return result of the interaction
     */
    public DefibNoticeResult claimDefibNotice(Inventory inventory, float hour,
            NotorietySystem.AchievementCallback achievementCallback) {
        if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
            return DefibNoticeResult.GARY_NOT_PRESENT;
        }
        if (defibNoticeClaimed) {
            return DefibNoticeResult.ALREADY_CLAIMED;
        }
        if (inventory.getItemCount(Material.NOTICE_OF_DEFICIENCY_PROP) < 1) {
            return DefibNoticeResult.NO_NOTICE;
        }
        // Remove the notice prop item and pay out
        inventory.removeItem(Material.NOTICE_OF_DEFICIENCY_PROP, 1);
        inventory.addItem(Material.COIN, DEFIB_NOTICE_PAYOUT);
        defibNoticeClaimed = true;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.AMBULANCE_CHASER);
        }
        return DefibNoticeResult.PAYOUT_ACCEPTED;
    }

    /** Returns true if the defib notice payout has already been claimed. */
    public boolean isDefibNoticeClaimed() {
        return defibNoticeClaimed;
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private int getBasePayout(ClaimType claimType) {
        switch (claimType) {
            case WHIPLASH_CLAIM:  return WHIPLASH_BASE_PAYOUT;
            case SLIP_AND_FALL:   return SLIP_AND_FALL_BASE_PAYOUT;
            case DOG_BITE_CLAIM:  return DOG_BITE_BASE_PAYOUT;
            case POTHOLE_CLAIM:   return POTHOLE_BASE_PAYOUT;
            case TRIP_CLAIM:      return TRIP_BASE_PAYOUT;
            default:              return TRIP_BASE_PAYOUT;
        }
    }
}
