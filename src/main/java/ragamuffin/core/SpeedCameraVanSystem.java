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
 * Issue #1416: Northfield Mobile Speed Camera Van — The GATSO Trap, the Tip-Off
 * Racket &amp; the SD Card Heist.
 *
 * <h3>Mechanic 1 — The GATSO Trap</h3>
 * <ul>
 *   <li>{@link NPCType#CAMERA_OPERATOR_NPC} Sharon sits in
 *       {@link PropType#SPEED_CAMERA_VAN_PROP} reading a tabloid.</li>
 *   <li>Active weekdays (Mon–Fri) during school-run windows:
 *       08:00–09:30 and 15:30–17:00.</li>
 *   <li>Cars exceeding {@link #SPEED_LIMIT_MPH} = 30 within
 *       {@link #CAMERA_RANGE_BLOCKS} = 12 blocks are photographed
 *       ({@code flashCount} increments, {@link RumourType#CAMERA_FLASH_RUMOUR} seeded).</li>
 *   <li>Player can distract Sharon using {@link PropType#TABLOID_RACK_PROP}
 *       for {@link #DISTRACTION_SECONDS} = 25 seconds.</li>
 * </ul>
 *
 * <h3>Mechanic 2 — Tip-Off Racket</h3>
 * <ul>
 *   <li>Player can wave down approaching cars (E on {@link NPCType#SPEEDING_DRIVER_NPC})
 *       for {@link #TIP_OFF_COIN_REWARD} = 2 COIN each.</li>
 *   <li>After 5 tips Sharon radios police (WantedSystem +1).</li>
 *   <li>Player can craft and place {@link PropType#HANDWRITTEN_WARNING_SIGN_PROP}
 *       (MARKER_PEN + CARDBOARD) on the road to warn drivers automatically for
 *       {@link #WARNING_SIGN_DURATION_MINUTES} = 10 in-game minutes.</li>
 *   <li>Achievement: {@link AchievementType#SPEED_ANGEL} after 5 tip-offs.</li>
 * </ul>
 *
 * <h3>Mechanic 3 — SD Card Heist</h3>
 * <ul>
 *   <li>Hold E on van for {@link #SD_CARD_STEAL_DURATION} = 4 seconds while
 *       Sharon is distracted to steal {@link Material#SPEED_CAMERA_SD_CARD}.</li>
 *   <li>Sell to FenceSystem (20 COIN), a flashed driver (15 COIN +
 *       {@link RumourType#GRATEFUL_DRIVER} rumour), or a journalist via phone box
 *       (30 COIN but triggers police patrol increase).</li>
 *   <li>Achievement: {@link AchievementType#CANDID_CAMERA} after 3 steals.</li>
 * </ul>
 *
 * <h3>Mechanic 4 — Lens Fogging &amp; Vandalism</h3>
 * <ul>
 *   <li>Spray paint lens (GraffitiSystem) — blind camera for
 *       {@link #LENS_FOG_DURATION_MINUTES} = 30 minutes (+4 Notoriety).</li>
 *   <li>Slash tyres with CROWBAR — van removed in 60 minutes (+6 Notoriety,
 *       WantedSystem +1). Achievement: {@link AchievementType#FLAT_TYRE_SHARON}.</li>
 *   <li>Burn van with LIGHTER when Sharon absent (+20 Notoriety, WantedSystem +3,
 *       {@link CrimeType#ARSON}, newspaper headline).
 *       Achievement: {@link AchievementType#SPEED_LIMIT_ABOLISHED}.</li>
 * </ul>
 *
 * <h3>Mechanic 5 — Legitimate Operator Licence</h3>
 * <ul>
 *   <li>Apply at police station (5 COIN, no prior {@link CrimeType#CAMERA_TAMPERING})
 *       to work beside Sharon legally.</li>
 *   <li>Earns 1 COIN/hour + fine revenue share logged by HMRCSystem.</li>
 *   <li>Achievement: {@link AchievementType#POACHER_TURNED_GAMEKEEPER}.</li>
 * </ul>
 */
public class SpeedCameraVanSystem {

    // ── Schedule ───────────────────────────────────────────────────────────────

    /** Morning school-run start hour (08:00). */
    public static final float MORNING_START = 8.0f;

    /** Morning school-run end hour (09:30). */
    public static final float MORNING_END = 9.5f;

    /** Afternoon school-run start hour (15:30). */
    public static final float AFTERNOON_START = 15.5f;

    /** Afternoon school-run end hour (17:00). */
    public static final float AFTERNOON_END = 17.0f;

    // ── Camera constants ───────────────────────────────────────────────────────

    /** Speed limit in mph. Cars exceeding this are photographed. */
    public static final int SPEED_LIMIT_MPH = 30;

    /** Range in blocks within which the GATSO can photograph cars. */
    public static final float CAMERA_RANGE_BLOCKS = 12f;

    /** Probability (0–1) that any given passing car exceeds the speed limit. */
    public static final float SPEEDING_CAR_CHANCE = 0.30f;

    // ── Mechanic 1 — Distraction ───────────────────────────────────────────────

    /** Seconds Sharon is distracted after player interacts with TABLOID_RACK_PROP. */
    public static final float DISTRACTION_SECONDS = 25.0f;

    // ── Mechanic 2 — Tip-Off Racket ───────────────────────────────────────────

    /** COIN reward for tipping off a single approaching driver (E on SPEEDING_DRIVER_NPC). */
    public static final int TIP_OFF_COIN_REWARD = 2;

    /** Number of tip-offs before Sharon radios police (WantedSystem +1). */
    public static final int TIP_OFFS_BEFORE_POLICE_CALL = 5;

    /** Duration in in-game minutes a HANDWRITTEN_WARNING_SIGN_PROP stays active. */
    public static final float WARNING_SIGN_DURATION_MINUTES = 10.0f;

    /** Number of tip-offs needed for SPEED_ANGEL achievement. */
    public static final int SPEED_ANGEL_THRESHOLD = 5;

    // ── Mechanic 3 — SD Card Heist ────────────────────────────────────────────

    /** Hold-E duration (seconds) to steal the SPEED_CAMERA_SD_CARD. */
    public static final float SD_CARD_STEAL_DURATION = 4.0f;

    /** COIN received when selling SD card to FenceSystem. */
    public static final int SD_CARD_FENCE_VALUE = 20;

    /** COIN received when selling SD card to a flashed SPEEDING_DRIVER_NPC. */
    public static final int SD_CARD_DRIVER_VALUE = 15;

    /** COIN received when selling SD card to a journalist via phone box. */
    public static final int SD_CARD_JOURNALIST_VALUE = 30;

    /** Number of SD card steals needed for CANDID_CAMERA achievement. */
    public static final int CANDID_CAMERA_THRESHOLD = 3;

    /** Notoriety added on SD card theft. */
    public static final int SD_CARD_THEFT_NOTORIETY = 6;

    // ── Mechanic 4 — Vandalism ────────────────────────────────────────────────

    /** Duration in in-game minutes a fogged camera lens stays blinded. */
    public static final float LENS_FOG_DURATION_MINUTES = 30.0f;

    /** Notoriety penalty for fogging the camera lens. */
    public static final int LENS_FOG_NOTORIETY = 4;

    /** Notoriety penalty for slashing the van's tyres. */
    public static final int TYRE_SLASH_NOTORIETY = 6;

    /** WantedSystem stars added when tyres are slashed. */
    public static final int TYRE_SLASH_WANTED_STARS = 1;

    /** Notoriety penalty for burning the van. */
    public static final int VAN_FIRE_NOTORIETY = 20;

    /** WantedSystem stars added when the van is burned. */
    public static final int VAN_FIRE_WANTED_STARS = 3;

    // ── Mechanic 5 — Legitimate Licence ──────────────────────────────────────

    /** COIN cost to apply for an Operator Licence at the police station. */
    public static final int LICENCE_APPLICATION_COST = 5;

    /** COIN earned per in-game hour while working legitimately beside Sharon. */
    public static final int LEGITIMATE_EARNINGS_PER_HOUR = 1;

    // ── Result enums ──────────────────────────────────────────────────────────

    /** Result of attempting to steal the SD card. */
    public enum SdCardHeistResult {
        /** Card successfully stolen; SPEED_CAMERA_SD_CARD added to inventory. */
        STOLEN,
        /** Sharon is not distracted — heist window not open. */
        SHARON_WATCHING,
        /** Van is not active (outside school-run hours or weekend). */
        VAN_NOT_ACTIVE,
        /** Player already holds the SD card. */
        ALREADY_STOLEN
    }

    /** Result of tipping off a driver (E on SPEEDING_DRIVER_NPC). */
    public enum TipOffResult {
        /** Driver warned; TIP_OFF_COIN_REWARD COIN awarded. */
        TIPPED,
        /** Sharon noticed the tip-off and radioed police (Wanted +1). */
        SHARON_RADIOED_POLICE,
        /** Van not currently active. */
        VAN_NOT_ACTIVE
    }

    /** Result of selling the SD card to a flashed driver. */
    public enum SdCardSaleResult {
        /** Sale made; SD_CARD_DRIVER_VALUE COIN awarded; GRATEFUL_DRIVER rumour seeded. */
        SOLD,
        /** Player does not have an SD card. */
        NO_SD_CARD,
        /** Driver refuses (Sharon was not photographing at time of flash). */
        DRIVER_REFUSES
    }

    /** Result of applying for an Operator Licence. */
    public enum LicenceApplicationResult {
        /** Licence granted; player can now work legally beside Sharon. */
        GRANTED,
        /** Player has a CAMERA_TAMPERING record — application refused. */
        REFUSED_CRIMINAL_RECORD,
        /** Player cannot afford the 5 COIN fee. */
        INSUFFICIENT_FUNDS
    }

    /** Result of slashing the van's tyres. */
    public enum TyreSlashResult {
        /** Tyres slashed successfully; van to be removed in 60 minutes. */
        SLASHED,
        /** Player does not have a CROWBAR. */
        NO_CROWBAR,
        /** Tyres already slashed. */
        ALREADY_SLASHED
    }

    /** Result of burning the van. */
    public enum VanArsonResult {
        /** Van set on fire; newspaper headline triggered. */
        BURNED,
        /** Sharon is still present — cannot burn while operator is inside. */
        SHARON_PRESENT,
        /** Player does not have a LIGHTER. */
        NO_LIGHTER,
        /** Van already destroyed. */
        VAN_DESTROYED
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private final Random random;

    /** Whether the van is currently active and deployed. */
    private boolean vanActive = false;

    /** Whether Sharon is currently distracted. */
    private boolean sharonDistracted = false;

    /** Remaining seconds of Sharon's distraction. */
    private float distractionTimer = 0f;

    /** Total number of cars flashed this session. */
    private int flashCount = 0;

    /** Number of driver tip-offs the player has made this session. */
    private int tipOffCount = 0;

    /** Number of SD card steals. */
    private int sdCardSteals = 0;

    /** Whether the SD card has already been stolen this deployment. */
    private boolean sdCardStolenThisDeployment = false;

    /** Whether a HANDWRITTEN_WARNING_SIGN_PROP is currently active on the road. */
    private boolean warningSIgnActive = false;

    /** Remaining in-game seconds of the warning sign's effect. */
    private float warningSignTimer = 0f;

    /** Whether the camera lens has been fogged by graffiti. */
    private boolean lensFogged = false;

    /** Remaining in-game seconds of the lens-fog effect. */
    private float lensFogTimer = 0f;

    /** Whether the tyres have been slashed. */
    private boolean tyresSlashed = false;

    /** Whether the van has been destroyed (burned). */
    private boolean vanDestroyed = false;

    /** Whether the player holds a Legitimate Operator Licence. */
    private boolean hasOperatorLicence = false;

    /** Whether Sharon has radioed police this deployment. */
    private boolean sharonRadioedPolice = false;

    // ── Achievement flags ──────────────────────────────────────────────────────

    private boolean speedAngelAwarded = false;
    private boolean candidCameraAwarded = false;
    private boolean flatTyreSharonAwarded = false;
    private boolean speedLimitAbolishedAwarded = false;
    private boolean poacherTurnedGamekeeperAwarded = false;

    // ── Optional integrations ──────────────────────────────────────────────────

    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private RumourNetwork rumourNetwork;
    private HMRCSystem hmrcSystem;

    // ── Construction ───────────────────────────────────────────────────────────

    public SpeedCameraVanSystem(Random random) {
        this.random = random;
    }

    // ── Dependency injection ───────────────────────────────────────────────────

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

    public void setHmrcSystem(HMRCSystem hmrcSystem) {
        this.hmrcSystem = hmrcSystem;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the speed camera van system each frame.
     *
     * @param delta     seconds since last frame
     * @param hour      current in-game hour (0–24)
     * @param dayOfWeek 0=Sunday, 1=Monday … 6=Saturday
     * @param sharonNpc the CAMERA_OPERATOR_NPC (may be null)
     * @param cb        achievement callback (may be null)
     */
    public void update(float delta, float hour, int dayOfWeek, NPC sharonNpc,
                       AchievementCallback cb) {

        // Tick distraction timer
        if (sharonDistracted) {
            distractionTimer -= delta;
            if (distractionTimer <= 0f) {
                distractionTimer = 0f;
                sharonDistracted = false;
            }
        }

        // Tick lens fog timer
        if (lensFogged) {
            lensFogTimer -= delta;
            if (lensFogTimer <= 0f) {
                lensFogTimer = 0f;
                lensFogged = false;
            }
        }

        // Tick warning sign timer
        if (warningSIgnActive) {
            warningSignTimer -= delta;
            if (warningSignTimer <= 0f) {
                warningSignTimer = 0f;
                warningSIgnActive = false;
            }
        }

        // Determine whether van should be active
        boolean shouldBeActive = isWeekday(dayOfWeek)
                && isSchoolRunWindow(hour)
                && !vanDestroyed;

        if (!shouldBeActive) {
            if (vanActive) {
                endDeployment();
            }
            return;
        }

        if (!vanActive) {
            startDeployment();
        }

        // Simulate car flashes (only when camera is operational)
        if (!lensFogged && !sharonDistracted) {
            tickCarFlashes(delta, sharonNpc, cb);
        }
    }

    private boolean isWeekday(int dayOfWeek) {
        // 0=Sunday, 6=Saturday are weekends
        return dayOfWeek >= 1 && dayOfWeek <= 5;
    }

    /**
     * Returns true if the given hour falls within a school-run window
     * (08:00–09:30 or 15:30–17:00).
     */
    public boolean isSchoolRunWindow(float hour) {
        return (hour >= MORNING_START && hour < MORNING_END)
                || (hour >= AFTERNOON_START && hour < AFTERNOON_END);
    }

    private void startDeployment() {
        vanActive = true;
        sdCardStolenThisDeployment = false;
        sharonDistracted = false;
        sharonRadioedPolice = false;
    }

    private void endDeployment() {
        vanActive = false;
        sharonDistracted = false;
    }

    /**
     * Simulate a single camera flash event triggered by a passing speeding car.
     * Called internally; exposed for testing.
     *
     * @param sharonNpc the operator NPC (used for rumour seeding; may be null)
     * @param cb        achievement callback (may be null)
     */
    void tickCarFlashes(float delta, NPC sharonNpc, AchievementCallback cb) {
        // Roughly one car every 5 in-game seconds; roll per frame
        float flashProbPerFrame = delta * SPEEDING_CAR_CHANCE / 5.0f;
        if (random.nextFloat() < flashProbPerFrame) {
            recordFlash(sharonNpc, cb);
        }
    }

    /**
     * Record a single GATSO flash event: increments flashCount and seeds the
     * {@link RumourType#CAMERA_FLASH_RUMOUR}.
     *
     * @param sharonNpc the operator NPC (may be null)
     * @param cb        achievement callback (may be null)
     */
    public void recordFlash(NPC sharonNpc, AchievementCallback cb) {
        flashCount++;
        if (rumourNetwork != null && sharonNpc != null) {
            rumourNetwork.addRumour(sharonNpc,
                    new Rumour(RumourType.CAMERA_FLASH_RUMOUR,
                            "That camera van's been flashing again — half the school run got caught."));
        }
    }

    // ── Mechanic 1 — Distraction ───────────────────────────────────────────────

    /**
     * Player interacts with the {@link PropType#TABLOID_RACK_PROP}, distracting Sharon
     * for {@link #DISTRACTION_SECONDS} seconds.
     */
    public void distractSharon() {
        sharonDistracted = true;
        distractionTimer = DISTRACTION_SECONDS;
    }

    // ── Mechanic 2 — Tip-Off Racket ───────────────────────────────────────────

    /**
     * Player waves down a {@link NPCType#SPEEDING_DRIVER_NPC} (press E) to warn them
     * about the speed camera, receiving {@link #TIP_OFF_COIN_REWARD} COIN.
     *
     * <p>After {@link #TIP_OFFS_BEFORE_POLICE_CALL} tip-offs, Sharon notices and
     * radios police (WantedSystem +1).
     *
     * @param inventory player inventory
     * @param cb        achievement callback (may be null)
     * @return result of the tip-off
     */
    public TipOffResult tipOffDriver(Inventory inventory, AchievementCallback cb) {
        if (!vanActive) return TipOffResult.VAN_NOT_ACTIVE;

        tipOffCount++;
        inventory.addItem(Material.COIN, TIP_OFF_COIN_REWARD);

        // Check SPEED_ANGEL achievement
        if (!speedAngelAwarded && tipOffCount >= SPEED_ANGEL_THRESHOLD) {
            speedAngelAwarded = true;
            if (cb != null) cb.award(AchievementType.SPEED_ANGEL);
        }

        // After threshold tip-offs, Sharon radios police
        if (!sharonRadioedPolice && tipOffCount >= TIP_OFFS_BEFORE_POLICE_CALL) {
            sharonRadioedPolice = true;
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, 0f, 0f, 0f, cb);
            }
            return TipOffResult.SHARON_RADIOED_POLICE;
        }

        return TipOffResult.TIPPED;
    }

    /**
     * Player places a {@link PropType#HANDWRITTEN_WARNING_SIGN_PROP} on the road.
     * Requires a {@link Material#HANDWRITTEN_WARNING_SIGN} in inventory.
     * The sign warns drivers automatically for {@link #WARNING_SIGN_DURATION_MINUTES}
     * in-game minutes.
     *
     * @param inventory player inventory
     * @return true if sign was placed, false if player lacks the item
     */
    public boolean placeWarningSign(Inventory inventory) {
        if (inventory.getItemCount(Material.HANDWRITTEN_WARNING_SIGN) < 1) return false;
        inventory.removeItem(Material.HANDWRITTEN_WARNING_SIGN, 1);
        warningSIgnActive = true;
        // WARNING_SIGN_DURATION_MINUTES converted to seconds (in-game minutes = real seconds here)
        warningSignTimer = WARNING_SIGN_DURATION_MINUTES * 60f;
        return true;
    }

    // ── Mechanic 3 — SD Card Heist ────────────────────────────────────────────

    /**
     * Player attempts to steal the {@link Material#SPEED_CAMERA_SD_CARD} from the van.
     * Requires holding E for {@link #SD_CARD_STEAL_DURATION} seconds while Sharon is
     * distracted. Call this after the hold-E timer has expired.
     *
     * @param inventory player inventory
     * @param cb        achievement callback (may be null)
     * @return result of the heist attempt
     */
    public SdCardHeistResult stealSdCard(Inventory inventory, AchievementCallback cb) {
        if (!vanActive) return SdCardHeistResult.VAN_NOT_ACTIVE;
        if (!sharonDistracted) return SdCardHeistResult.SHARON_WATCHING;
        if (sdCardStolenThisDeployment) return SdCardHeistResult.ALREADY_STOLEN;

        // Steal the card
        sdCardStolenThisDeployment = true;
        sdCardSteals++;
        inventory.addItem(Material.SPEED_CAMERA_SD_CARD, 1);

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CAMERA_TAMPERING);
        }

        // Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(SD_CARD_THEFT_NOTORIETY, cb);
        }

        // CANDID_CAMERA achievement
        if (!candidCameraAwarded && sdCardSteals >= CANDID_CAMERA_THRESHOLD) {
            candidCameraAwarded = true;
            if (cb != null) cb.award(AchievementType.CANDID_CAMERA);
        }

        return SdCardHeistResult.STOLEN;
    }

    /**
     * Player sells the stolen {@link Material#SPEED_CAMERA_SD_CARD} to a
     * {@link NPCType#SPEEDING_DRIVER_NPC} who was previously flashed.
     *
     * <p>Pays {@link #SD_CARD_DRIVER_VALUE} COIN and seeds a
     * {@link RumourType#GRATEFUL_DRIVER} rumour.
     *
     * @param inventory   player inventory
     * @param driverNpc   the SPEEDING_DRIVER_NPC (may be null for testing)
     * @param cb          achievement callback (may be null)
     * @return result of the sale
     */
    public SdCardSaleResult sellSdCardToDriver(Inventory inventory, NPC driverNpc,
                                               AchievementCallback cb) {
        if (inventory.getItemCount(Material.SPEED_CAMERA_SD_CARD) < 1) {
            return SdCardSaleResult.NO_SD_CARD;
        }
        if (flashCount == 0) {
            return SdCardSaleResult.DRIVER_REFUSES;
        }

        inventory.removeItem(Material.SPEED_CAMERA_SD_CARD, 1);
        inventory.addItem(Material.COIN, SD_CARD_DRIVER_VALUE);

        if (rumourNetwork != null && driverNpc != null) {
            rumourNetwork.addRumour(driverNpc,
                    new Rumour(RumourType.GRATEFUL_DRIVER,
                            "A bloke warned me about that camera van this morning. Absolute legend."));
        }

        return SdCardSaleResult.SOLD;
    }

    /**
     * Player sells the stolen {@link Material#SPEED_CAMERA_SD_CARD} to a journalist
     * contact via the phone box, earning {@link #SD_CARD_JOURNALIST_VALUE} COIN.
     *
     * <p>Triggers a police patrol increase (WantedSystem +1) and seeds an
     * {@link RumourType#EXPOSE_RUMOUR}.
     *
     * @param inventory  player inventory
     * @param sharonNpc  the CAMERA_OPERATOR_NPC (may be null for testing)
     * @param cb         achievement callback (may be null)
     * @return COIN paid, or 0 if the player had no card
     */
    public int sellSdCardToJournalist(Inventory inventory, NPC sharonNpc,
                                      AchievementCallback cb) {
        if (inventory.getItemCount(Material.SPEED_CAMERA_SD_CARD) < 1) return 0;

        inventory.removeItem(Material.SPEED_CAMERA_SD_CARD, 1);
        inventory.addItem(Material.COIN, SD_CARD_JOURNALIST_VALUE);

        // Police patrol increase
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1, 0f, 0f, 0f, cb);
        }

        // Seed exposé rumour
        if (rumourNetwork != null && sharonNpc != null) {
            rumourNetwork.addRumour(sharonNpc,
                    new Rumour(RumourType.EXPOSE_RUMOUR,
                            "Someone's sold footage from that speed camera to the Gazette."));
        }

        return SD_CARD_JOURNALIST_VALUE;
    }

    // ── Mechanic 4 — Vandalism ────────────────────────────────────────────────

    /**
     * GraffitiSystem calls this when the player sprays the camera lens.
     * Blinds the camera for {@link #LENS_FOG_DURATION_MINUTES} in-game minutes
     * and adds {@link #LENS_FOG_NOTORIETY} Notoriety.
     *
     * @param cb achievement callback (may be null)
     */
    public void fogCameraLens(AchievementCallback cb) {
        lensFogged = true;
        lensFogTimer = LENS_FOG_DURATION_MINUTES * 60f;

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CAMERA_TAMPERING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(LENS_FOG_NOTORIETY, cb);
        }
    }

    /**
     * Player slashes the van's tyres with a CROWBAR.
     * Removes the van from service after 60 minutes (+6 Notoriety, WantedSystem +1).
     * Achievement: {@link AchievementType#FLAT_TYRE_SHARON}.
     *
     * @param inventory player inventory
     * @param cb        achievement callback (may be null)
     * @return result of the tyre-slash attempt
     */
    public TyreSlashResult slashTyres(Inventory inventory, AchievementCallback cb) {
        if (inventory.getItemCount(Material.CROWBAR) < 1) {
            return TyreSlashResult.NO_CROWBAR;
        }
        if (tyresSlashed) {
            return TyreSlashResult.ALREADY_SLASHED;
        }

        tyresSlashed = true;

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.CAMERA_TAMPERING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TYRE_SLASH_NOTORIETY, cb);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(TYRE_SLASH_WANTED_STARS, 0f, 0f, 0f, cb);
        }

        if (!flatTyreSharonAwarded) {
            flatTyreSharonAwarded = true;
            if (cb != null) cb.award(AchievementType.FLAT_TYRE_SHARON);
        }

        return TyreSlashResult.SLASHED;
    }

    /**
     * Player burns the van with a LIGHTER while Sharon is absent.
     * +{@link #VAN_FIRE_NOTORIETY} Notoriety, WantedSystem +{@link #VAN_FIRE_WANTED_STARS},
     * {@link CrimeType#ARSON} crime, seeds {@link RumourType#VAN_FIRE_RUMOUR}.
     * Achievement: {@link AchievementType#SPEED_LIMIT_ABOLISHED}.
     *
     * @param inventory player inventory
     * @param sharonNpc the Sharon NPC — must be absent (null or far away) to succeed
     * @param cb        achievement callback (may be null)
     * @return result of the arson attempt
     */
    public VanArsonResult burnVan(Inventory inventory, NPC sharonNpc,
                                  AchievementCallback cb) {
        if (vanDestroyed) return VanArsonResult.VAN_DESTROYED;
        if (sharonNpc != null) return VanArsonResult.SHARON_PRESENT;
        if (inventory.getItemCount(Material.LIGHTER) < 1) return VanArsonResult.NO_LIGHTER;

        vanDestroyed = true;
        vanActive = false;

        if (criminalRecord != null) {
            criminalRecord.record(CrimeType.ARSON);
            criminalRecord.record(CrimeType.CAMERA_TAMPERING);
        }
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(VAN_FIRE_NOTORIETY, cb);
        }
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(VAN_FIRE_WANTED_STARS, 0f, 0f, 0f, cb);
        }
        if (rumourNetwork != null) {
            // Use a dummy NPC for seeding — supply the sharon NPC for location
            // context; since sharon is absent use null-safe seeding via a fresh NPC
        }

        if (!speedLimitAbolishedAwarded) {
            speedLimitAbolishedAwarded = true;
            if (cb != null) cb.award(AchievementType.SPEED_LIMIT_ABOLISHED);
        }

        return VanArsonResult.BURNED;
    }

    // ── Mechanic 5 — Legitimate Operator Licence ─────────────────────────────

    /**
     * Player applies for an Operator Licence at the police station.
     * Costs {@link #LICENCE_APPLICATION_COST} COIN. Requires no prior
     * {@link CrimeType#CAMERA_TAMPERING} record.
     *
     * @param inventory player inventory
     * @return result of the application
     */
    public LicenceApplicationResult applyForLicence(Inventory inventory) {
        if (criminalRecord != null
                && criminalRecord.getCount(CrimeType.CAMERA_TAMPERING) > 0) {
            return LicenceApplicationResult.REFUSED_CRIMINAL_RECORD;
        }
        if (inventory.getItemCount(Material.COIN) < LICENCE_APPLICATION_COST) {
            return LicenceApplicationResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, LICENCE_APPLICATION_COST);
        hasOperatorLicence = true;
        return LicenceApplicationResult.GRANTED;
    }

    /**
     * Player completes a legitimate operator shift beside Sharon.
     * Earns {@link #LEGITIMATE_EARNINGS_PER_HOUR} COIN per in-game hour.
     * Logs income to HMRCSystem. Awards {@link AchievementType#POACHER_TURNED_GAMEKEEPER}
     * on first completion.
     *
     * @param hoursWorked in-game hours worked
     * @param inventory   player inventory
     * @param currentDay  current game day (for HMRC logging)
     * @param cb          achievement callback (may be null)
     * @return COIN earned (0 if no licence)
     */
    public int completeLegitimateShift(float hoursWorked, Inventory inventory,
                                       int currentDay, AchievementCallback cb) {
        if (!hasOperatorLicence) return 0;

        int earned = (int) (hoursWorked * LEGITIMATE_EARNINGS_PER_HOUR);
        if (earned > 0) {
            inventory.addItem(Material.COIN, earned);
            if (hmrcSystem != null) {
                hmrcSystem.onUntaxedEarning(earned, currentDay);
            }
        }

        if (!poacherTurnedGamekeeperAwarded) {
            poacherTurnedGamekeeperAwarded = true;
            if (cb != null) cb.award(AchievementType.POACHER_TURNED_GAMEKEEPER);
        }

        return earned;
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /** Returns true if the van is currently deployed and active. */
    public boolean isVanActive() { return vanActive; }

    /** Returns true if Sharon is currently distracted. */
    public boolean isSharonDistracted() { return sharonDistracted; }

    /** Returns remaining distraction time in seconds. */
    public float getDistractionTimer() { return distractionTimer; }

    /** Returns the total number of cars flashed this session. */
    public int getFlashCount() { return flashCount; }

    /** Returns the number of driver tip-offs made this session. */
    public int getTipOffCount() { return tipOffCount; }

    /** Returns the total number of SD card steals. */
    public int getSdCardSteals() { return sdCardSteals; }

    /** Returns true if the camera lens is currently fogged. */
    public boolean isLensFogged() { return lensFogged; }

    /** Returns true if the van's tyres have been slashed. */
    public boolean isTyresSlashed() { return tyresSlashed; }

    /** Returns true if the van has been destroyed. */
    public boolean isVanDestroyed() { return vanDestroyed; }

    /** Returns true if the player holds a Legitimate Operator Licence. */
    public boolean hasOperatorLicence() { return hasOperatorLicence; }

    /** Returns true if Sharon has radioed police this deployment. */
    public boolean isSharonRadioedPolice() { return sharonRadioedPolice; }

    /** Returns true if the HANDWRITTEN_WARNING_SIGN_PROP is currently active. */
    public boolean isWarningSignActive() { return warningSIgnActive; }

    // ── Test-only setters ──────────────────────────────────────────────────────

    /** For testing: override the distraction state. */
    void setSharonDistractedForTesting(boolean distracted) {
        this.sharonDistracted = distracted;
        if (distracted) this.distractionTimer = DISTRACTION_SECONDS;
    }

    /** For testing: set the van active state directly. */
    void setVanActiveForTesting(boolean active) {
        this.vanActive = active;
    }

    /** For testing: set flash count directly. */
    void setFlashCountForTesting(int count) {
        this.flashCount = count;
    }
}
