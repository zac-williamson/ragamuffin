package ragamuffin.core;

import ragamuffin.ai.NPCManager;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementType;
import ragamuffin.world.LandmarkType;

import java.util.List;
import java.util.Random;

/**
 * Issue #1163: Northfield NHS Dentist — Six-Month Waits, Toothache Debuffs &amp;
 * the Back-Street Molar Job.
 *
 * <h3>Core mechanic — toothache</h3>
 * A hidden {@code toothachePoints} (0–100) accumulates from sugar consumption:
 * <ul>
 *   <li>{@link Material#FIZZY_DRINK} → +12 per use</li>
 *   <li>{@link Material#HARIBO}      → +15 per use</li>
 * </ul>
 * Above 60 → {@link DebuffLevel#TOOTHACHE}: −10 % speed, +0.5 s interaction delay.<br>
 * Above 85 → {@link DebuffLevel#SEVERE_TOOTHACHE}: −20 % speed, +1.5 s interaction
 * delay, periodic audio twinge every 30 in-game minutes.
 *
 * <p>Using a {@link Material#TOOTHBRUSH} reduces {@code toothachePoints} by 25,
 * once per in-game day.
 *
 * <h3>NHS path</h3>
 * Register with Deborah ({@code DENTAL_RECEPTIONIST}) at {@code RECEPTION_DESK_DENTAL_PROP}
 * Mon–Fri 08:30–17:00 (lunch 13:00–14:00).  Appointment slot opens in 6 in-game days.
 *
 * <p>Waiting-list hustle: lockpick {@code FILING_CABINET_DENTAL_PROP} to obtain
 * {@code WAITING_LIST_LETTER}.  Photocopy at Community Centre to create
 * {@code FORGED_WAITING_LIST_LETTER}.  Present to Deborah to halve remaining wait
 * (25 % catch chance at Notoriety ≥ 30 → {@code PRESCRIPTION_FRAUD} + Notoriety +3 +
 * 3-day NHS ban).
 *
 * <p>Private slot: 15 COIN for next-day appointment.
 *
 * <h3>Mirek path</h3>
 * Unlock via pub rumour chain (2 {@code LOCAL_HEALTH} rumours + 2 pub NPC conversations).
 * Treatment costs 5 COIN; 60 % success, 20 % chance INFECTION debuff, 40 % BOTCHED_JOB
 * (toothachePoints +20 instead of clearing).  Mirek flees if POLICE within 12 blocks.
 *
 * <h3>Integrations</h3>
 * HealingSystem (speed multiplier), GPSurgerySystem (referral −2 days wait),
 * RumourNetwork (Mirek chain), CriminalRecord (PRESCRIPTION_FRAUD),
 * NotorietySystem, WantedSystem.
 */
public class NHSDentistSystem {

    // ── Opening-hours constants ────────────────────────────────────────────────

    /** Weekday opening hour (08:30). */
    public static final float WEEKDAY_OPEN_HOUR   = 8.5f;
    /** Weekday closing hour (17:00, exclusive). */
    public static final float WEEKDAY_CLOSE_HOUR  = 17.0f;
    /** Lunch start (13:00). */
    public static final float LUNCH_START_HOUR    = 13.0f;
    /** Lunch end (14:00, exclusive). */
    public static final float LUNCH_END_HOUR      = 14.0f;
    /** Day-of-week index for Saturday (5 = Sat, using dayCount % 7). */
    public static final int   DAY_SATURDAY_INDEX  = 5;
    /** Day-of-week index for Sunday (6 = Sun). */
    public static final int   DAY_SUNDAY_INDEX    = 6;

    // Day-of-week constants for isOpen(hour, dayOfWeek) API
    public static final int MONDAY    = 0;
    public static final int TUESDAY   = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY  = 3;
    public static final int FRIDAY    = 4;
    public static final int SATURDAY  = 5;
    public static final int SUNDAY    = 6;

    // ── Toothache thresholds ───────────────────────────────────────────────────

    /** toothachePoints threshold for TOOTHACHE debuff. */
    public static final int TOOTHACHE_THRESHOLD        = 60;
    /** toothachePoints threshold for SEVERE_TOOTHACHE debuff. */
    public static final int SEVERE_TOOTHACHE_THRESHOLD = 85;

    // ── Sugar damage values ───────────────────────────────────────────────────

    /** Sugar damage from consuming a FIZZY_DRINK. */
    public static final int SUGAR_DAMAGE_FIZZY_DRINK = 12;
    /** Sugar damage from consuming a HARIBO. */
    public static final int SUGAR_DAMAGE_HARIBO      = 15;
    /** Sugar damage from consuming a generic sugary item (default). */
    public static final int SUGAR_DAMAGE_DEFAULT     = 3;

    // ── Toothbrush ────────────────────────────────────────────────────────────

    /** Points removed by one TOOTHBRUSH use. */
    public static final int TOOTHBRUSH_REDUCTION = 25;

    // ── Speed multipliers (applied by HealingSystem integration) ─────────────

    /** Speed multiplier when TOOTHACHE is active. */
    public static final float SPEED_MULT_TOOTHACHE        = 0.90f;
    /** Speed multiplier when SEVERE_TOOTHACHE is active. */
    public static final float SPEED_MULT_SEVERE_TOOTHACHE = 0.80f;

    // ── NHS wait constants ─────────────────────────────────────────────────────

    /** Standard NHS wait in in-game days. */
    public static final int NHS_WAIT_DAYS = 6;

    /** Days removed by a GP referral note. */
    public static final int GP_REFERRAL_WAIT_REDUCTION = 2;

    /** Cost of a private next-day slot (COIN). */
    public static final int PRIVATE_SLOT_COST = 15;

    // ── Forgery constants ──────────────────────────────────────────────────────

    /** Catch chance for the forged letter (base). */
    public static final float FORGERY_CATCH_CHANCE         = 0.25f;
    /** Notoriety threshold above which the catch check is triggered. */
    public static final int   FORGERY_NOTORIETY_THRESHOLD  = 30;
    /** Notoriety added if caught with a forged letter. */
    public static final int   FORGERY_CAUGHT_NOTORIETY     = 3;
    /** Days the NHS dental is banned after being caught with forgery. */
    public static final int   FORGERY_BAN_DAYS             = 3;

    // ── Mirek constants ────────────────────────────────────────────────────────

    /** Coin cost for Mirek's treatment. */
    public static final int   MIREK_TREATMENT_COST       = 5;
    /** Probability of successful treatment by Mirek. */
    public static final float MIREK_SUCCESS_CHANCE        = 0.60f;
    /** Probability of INFECTION side effect after Mirek success. */
    public static final float MIREK_INFECTION_CHANCE      = 0.20f;
    /** toothachePoints increase on Mirek BOTCHED_JOB. */
    public static final int   MIREK_BOTCH_DAMAGE          = 20;
    /** Distance in blocks within which POLICE triggers Mirek to flee. */
    public static final float MIREK_FLEE_POLICE_RADIUS    = 12.0f;

    // ── Twinge interval ────────────────────────────────────────────────────────

    /** In-game minutes between audio twinges for SEVERE_TOOTHACHE. */
    public static final float SEVERE_TWINGE_INTERVAL_MINUTES = 30.0f;

    // ── Treatment types enum ───────────────────────────────────────────────────

    /**
     * NHS treatment type determined by current toothachePoints when the
     * appointment chair is used.
     */
    public enum TreatmentType {
        /** toothachePoints 60–74: toothachePoints → 20; clears TOOTHACHE. */
        FILLING,
        /** toothachePoints 75–84: toothachePoints → 0; health +5. */
        ROOT_CANAL,
        /** toothachePoints ≥ 85: toothachePoints → 0; seeds LOCAL_HEALTH rumour. */
        EXTRACTION,
        /** toothachePoints < 60: no treatment needed. */
        NONE
    }

    // ── Debuff level enum ──────────────────────────────────────────────────────

    /** Active toothache debuff level. */
    public enum DebuffLevel {
        /** No debuff active (toothachePoints < 60). */
        NONE,
        /** Mild debuff (toothachePoints 60–84). */
        TOOTHACHE,
        /** Severe debuff (toothachePoints ≥ 85). */
        SEVERE_TOOTHACHE
    }

    // ── Registration result enum ───────────────────────────────────────────────

    /** Outcome of attempting to register as an NHS patient. */
    public enum RegistrationResult {
        SUCCESS,
        CLOSED,
        ALREADY_REGISTERED,
        BANNED
    }

    // ── Treatment result enum ──────────────────────────────────────────────────

    /** Outcome of using the dental chair for NHS treatment. */
    public enum TreatmentResult {
        SUCCESS,
        NO_APPOINTMENT,
        NOT_OPEN
    }

    // ── Mirek treatment result enum ────────────────────────────────────────────

    /** Outcome of using Mirek's improvised dental chair. */
    public enum MirekResult {
        SUCCESS,
        SUCCESS_INFECTION,
        BOTCHED,
        POLICE_PRESENT,
        LOCATION_UNKNOWN,
        INSUFFICIENT_FUNDS
    }

    // ── Achievement callback ───────────────────────────────────────────────────

    /** Callback for awarding achievements (matches NotorietySystem pattern). */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final Random random;

    /** Current accumulated sugar damage (0–100+). */
    private int toothachePoints = 0;

    /** Day on which the player has an NHS appointment (-1 = none). */
    private int appointmentDay = -1;

    /** Whether the player is registered as an NHS patient. */
    private boolean nhsRegistered = false;

    /** Day on which the NHS dental ban expires (-1 = no ban). */
    private int nhsBanExpiry = -1;

    /** Day on which the player last used a toothbrush (-1 = never). */
    private int lastToothbrushDay = -1;

    /** Whether Mirek's location has been unlocked via the rumour chain. */
    private boolean mirekLocationUnlocked = false;

    /** Number of LOCAL_HEALTH rumours the player has heard. */
    private int localHealthRumoursHeard = 0;

    /** Number of pub NPC conversations completed for Mirek unlock. */
    private int pubConversationsForMirek = 0;

    /** Accumulated in-game minutes since last SEVERE_TOOTHACHE twinge. */
    private float minutesSinceLastTwinge = 0f;

    /** Whether the SWEET_TOOTH_CONSEQUENCE achievement has been awarded. */
    private boolean sweetToothAwarded = false;

    /** Whether the SIX_MONTH_WAIT achievement has been awarded. */
    private boolean sixMonthWaitAwarded = false;

    // ── Construction ──────────────────────────────────────────────────────────

    public NHSDentistSystem(Random random) {
        this.random = random;
    }

    // ── Static query methods (spec-required) ──────────────────────────────────

    /**
     * Returns the sugar damage added by consuming the given material.
     *
     * @param material the consumed item
     * @return sugar damage value
     */
    public static int getSugarDamage(Material material) {
        if (material == Material.FIZZY_DRINK) return SUGAR_DAMAGE_FIZZY_DRINK;
        if (material == Material.HARIBO)      return SUGAR_DAMAGE_HARIBO;
        return SUGAR_DAMAGE_DEFAULT;
    }

    /**
     * Returns the debuff level for a given toothachePoints value.
     *
     * @param points current toothachePoints
     * @return the {@link DebuffLevel}
     */
    public static DebuffLevel getDebuffLevel(int points) {
        if (points >= SEVERE_TOOTHACHE_THRESHOLD) return DebuffLevel.SEVERE_TOOTHACHE;
        if (points >= TOOTHACHE_THRESHOLD)        return DebuffLevel.TOOTHACHE;
        return DebuffLevel.NONE;
    }

    /**
     * Returns the amount toothachePoints is reduced by a single toothbrush use.
     */
    public static int getToothbrushReduction() {
        return TOOTHBRUSH_REDUCTION;
    }

    /**
     * Returns the NHS treatment type for a given toothachePoints value.
     *
     * @param points current toothachePoints
     * @return the {@link TreatmentType}
     */
    public static TreatmentType getTreatmentType(int points) {
        if (points >= SEVERE_TOOTHACHE_THRESHOLD) return TreatmentType.EXTRACTION;
        if (points >= 75)                         return TreatmentType.ROOT_CANAL;
        if (points >= TOOTHACHE_THRESHOLD)        return TreatmentType.FILLING;
        return TreatmentType.NONE;
    }

    /**
     * Returns the probability of a successful treatment by Mirek.
     */
    public static float getMirekSuccessChance() {
        return MIREK_SUCCESS_CHANCE;
    }

    /**
     * Returns the standard NHS wait in in-game days.
     */
    public static int getNHSWaitDays() {
        return NHS_WAIT_DAYS;
    }

    /**
     * Returns the halved wait after presenting a forged letter
     * (rounded down).
     *
     * @param currentWait the current remaining wait days
     * @return the new wait (half, rounded down)
     */
    public static int getForgeryWaitReduction(int currentWait) {
        return currentWait / 2;
    }

    /**
     * Returns the speed multiplier to apply for the given debuff level.
     *
     * @param level the active debuff level
     * @return speed multiplier (1.0f = no effect)
     */
    public static float getSpeedMultiplier(DebuffLevel level) {
        switch (level) {
            case TOOTHACHE:        return SPEED_MULT_TOOTHACHE;
            case SEVERE_TOOTHACHE: return SPEED_MULT_SEVERE_TOOTHACHE;
            default:               return 1.0f;
        }
    }

    // ── Opening-hours logic ────────────────────────────────────────────────────

    /**
     * Returns true if the NHS Dental Practice is open at the given hour and
     * day-of-week (0=Mon … 6=Sun).
     *
     * @param hour      fractional hour (e.g. 8.5 = 08:30)
     * @param dayOfWeek 0=Mon, 1=Tue, … 5=Sat, 6=Sun
     * @return true if open
     */
    public boolean isOpen(float hour, int dayOfWeek) {
        if (dayOfWeek == SATURDAY || dayOfWeek == SUNDAY) return false;
        if (hour < WEEKDAY_OPEN_HOUR || hour >= WEEKDAY_CLOSE_HOUR) return false;
        // Lunch break
        if (hour >= LUNCH_START_HOUR && hour < LUNCH_END_HOUR) return false;
        return true;
    }

    /**
     * Convenience overload using a TimeSystem.
     */
    public boolean isOpen(TimeSystem timeSystem) {
        int dayOfWeek = timeSystem.getDayCount() % 7;
        return isOpen(timeSystem.getTime(), dayOfWeek);
    }

    // ── Sugar consumption ──────────────────────────────────────────────────────

    /**
     * Process the player consuming a sugary item. Adds sugar damage and
     * applies or escalates the TOOTHACHE debuff if thresholds are crossed.
     *
     * @param material            the item consumed
     * @param achievementCallback callback for achievements (may be null)
     * @return the new toothachePoints value
     */
    public int consumeSugaryItem(Material material, AchievementCallback achievementCallback) {
        int damage = getSugarDamage(material);
        toothachePoints = Math.min(100, toothachePoints + damage);
        checkAndAwardSweetToothAchievement(achievementCallback);
        return toothachePoints;
    }

    private void checkAndAwardSweetToothAchievement(AchievementCallback cb) {
        if (!sweetToothAwarded && toothachePoints >= SEVERE_TOOTHACHE_THRESHOLD) {
            sweetToothAwarded = true;
            if (cb != null) cb.award(AchievementType.SWEET_TOOTH_CONSEQUENCE);
        }
    }

    // ── Toothbrush use ────────────────────────────────────────────────────────

    /**
     * Attempt to use a TOOTHBRUSH from the player's inventory.
     * Reduces toothachePoints by {@link #TOOTHBRUSH_REDUCTION}, once per in-game day.
     *
     * @param inventory  the player's inventory
     * @param currentDay the current in-game day (from TimeSystem.getDayCount())
     * @return true if the toothbrush was used successfully
     */
    public boolean useToothbrush(Inventory inventory, int currentDay) {
        if (inventory.getItemCount(Material.TOOTHBRUSH) <= 0) return false;
        if (lastToothbrushDay >= 0 && lastToothbrushDay == currentDay) return false; // once per day

        toothachePoints = Math.max(0, toothachePoints - TOOTHBRUSH_REDUCTION);
        lastToothbrushDay = currentDay;
        return true;
    }

    // ── NHS registration ───────────────────────────────────────────────────────

    /**
     * Attempt to register as an NHS patient at the RECEPTION_DESK_DENTAL_PROP.
     *
     * @param hour                fractional hour
     * @param dayOfWeek           0=Mon … 6=Sun
     * @param currentDay          current in-game day
     * @param inventory           player's inventory (receives DENTAL_APPOINTMENT_LETTER)
     * @param gpReferralActive    true if the player carries a GP referral note
     *                            (reduces wait by 2 days)
     * @return the {@link RegistrationResult}
     */
    public RegistrationResult registerNHS(float hour, int dayOfWeek, int currentDay,
                                          Inventory inventory, boolean gpReferralActive) {
        if (nhsBanExpiry >= 0 && currentDay < nhsBanExpiry) {
            return RegistrationResult.BANNED;
        }
        if (!isOpen(hour, dayOfWeek)) {
            return RegistrationResult.CLOSED;
        }
        if (nhsRegistered && appointmentDay >= 0) {
            return RegistrationResult.ALREADY_REGISTERED;
        }

        int waitDays = NHS_WAIT_DAYS;
        if (gpReferralActive) {
            waitDays = Math.max(1, waitDays - GP_REFERRAL_WAIT_REDUCTION);
        }
        appointmentDay = currentDay + waitDays;
        nhsRegistered = true;

        inventory.addItem(Material.DENTAL_APPOINTMENT_LETTER, 1);
        return RegistrationResult.SUCCESS;
    }

    /**
     * Returns the number of days until the player's NHS appointment, or -1 if
     * no appointment is booked.
     *
     * @param currentDay current in-game day
     */
    public int getDaysUntilAppointment(int currentDay) {
        if (appointmentDay < 0) return -1;
        return Math.max(0, appointmentDay - currentDay);
    }

    // ── Forged waiting-list letter ─────────────────────────────────────────────

    /**
     * Present a FORGED_WAITING_LIST_LETTER to Deborah to halve the remaining wait.
     * 25 % catch chance when player Notoriety ≥ {@link #FORGERY_NOTORIETY_THRESHOLD}.
     *
     * @param inventory            player's inventory (letter is consumed)
     * @param currentDay           current in-game day
     * @param notoriety            current player notoriety
     * @param notorietySystem      for adding notoriety on catch
     * @param criminalRecord       for recording PRESCRIPTION_FRAUD on catch
     * @param achievementCallback  for awarding QUEUE_JUMPER_DENTAL on success
     * @return true if the letter was accepted (not caught); false if caught
     */
    public boolean presentForgedLetter(
            Inventory inventory, int currentDay,
            int notoriety, NotorietySystem notorietySystem,
            CriminalRecord criminalRecord,
            AchievementCallback achievementCallback) {

        if (inventory.getItemCount(Material.FORGED_WAITING_LIST_LETTER) <= 0) {
            return false;
        }

        // Remove the forged letter
        inventory.removeItem(Material.FORGED_WAITING_LIST_LETTER, 1);

        // Catch check
        boolean caught = false;
        if (notoriety >= FORGERY_NOTORIETY_THRESHOLD) {
            caught = random.nextFloat() < FORGERY_CATCH_CHANCE;
        }

        if (caught) {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(FORGERY_CAUGHT_NOTORIETY,
                        achievementCallback != null ? achievementCallback::award : null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.PRESCRIPTION_FRAUD);
            }
            nhsBanExpiry = currentDay + FORGERY_BAN_DAYS;
            return false;
        }

        // Success — halve remaining wait
        if (appointmentDay >= 0) {
            int remaining = appointmentDay - currentDay;
            if (remaining > 0) {
                appointmentDay = currentDay + getForgeryWaitReduction(remaining);
            }
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.QUEUE_JUMPER_DENTAL);
        }
        return true;
    }

    // ── Private slot ───────────────────────────────────────────────────────────

    /**
     * Purchase a private next-day slot (15 COIN).
     *
     * @param inventory           player's inventory
     * @param currentDay          current in-game day
     * @param achievementCallback for awarding PRIVATE_PATIENT
     * @return true if purchased successfully
     */
    public boolean purchasePrivateSlot(Inventory inventory, int currentDay,
                                       AchievementCallback achievementCallback) {
        if (inventory.getItemCount(Material.COIN) < PRIVATE_SLOT_COST) return false;
        inventory.removeItem(Material.COIN, PRIVATE_SLOT_COST);
        appointmentDay = currentDay + 1;
        nhsRegistered = true;
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.PRIVATE_PATIENT);
        }
        return true;
    }

    // ── NHS treatment at dental chair ──────────────────────────────────────────

    /**
     * Perform NHS treatment at Dr. Rashid's DENTAL_CHAIR_PROP.
     * Requires a valid appointment for today.
     *
     * @param currentDay          current in-game day
     * @param player              the player (for health modification)
     * @param allNpcs             all active NPCs (for rumour seeding on extraction)
     * @param rumourNetwork       for seeding LOCAL_HEALTH rumour on extraction
     * @param achievementCallback for awarding SIX_MONTH_WAIT
     * @return the {@link TreatmentResult}
     */
    public TreatmentResult performNHSTreatment(int currentDay, Player player,
                                               List<NPC> allNpcs,
                                               RumourNetwork rumourNetwork,
                                               AchievementCallback achievementCallback) {
        if (appointmentDay < 0 || appointmentDay > currentDay) {
            return TreatmentResult.NO_APPOINTMENT;
        }

        TreatmentType treatment = getTreatmentType(toothachePoints);

        switch (treatment) {
            case FILLING:
                toothachePoints = 20;
                break;
            case ROOT_CANAL:
                toothachePoints = 0;
                if (player != null) player.heal(5);
                break;
            case EXTRACTION:
                toothachePoints = 0;
                seedExtractionRumour(allNpcs, rumourNetwork);
                break;
            case NONE:
                // No treatment needed — reset anyway
                toothachePoints = Math.max(0, toothachePoints - 10);
                break;
        }

        // Consume appointment
        appointmentDay = -1;

        if (achievementCallback != null && !sixMonthWaitAwarded) {
            sixMonthWaitAwarded = true;
            achievementCallback.award(AchievementType.SIX_MONTH_WAIT);
        }

        return TreatmentResult.SUCCESS;
    }

    private void seedExtractionRumour(List<NPC> allNpcs, RumourNetwork rumourNetwork) {
        if (rumourNetwork == null || allNpcs == null) return;
        Rumour rumour = new Rumour(RumourType.LOCAL_HEALTH,
                "they pulled his tooth at the dentist — looked rough");
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            rumourNetwork.addRumour(npc, rumour);
            if (++seeded >= 3) break;
        }
    }

    // ── Mirek path ────────────────────────────────────────────────────────────

    /**
     * Record that the player has heard a LOCAL_HEALTH rumour (for Mirek unlock chain).
     * After 2 rumours heard AND 2 pub conversations, Mirek's location is unlocked.
     */
    public void onLocalHealthRumourHeard() {
        if (localHealthRumoursHeard < 2) {
            localHealthRumoursHeard++;
        }
        checkMirekUnlock();
    }

    /**
     * Record that the player has completed a pub NPC conversation toward the Mirek chain.
     */
    public void onPubConversationForMirek() {
        if (pubConversationsForMirek < 2) {
            pubConversationsForMirek++;
        }
        checkMirekUnlock();
    }

    private void checkMirekUnlock() {
        if (!mirekLocationUnlocked
                && localHealthRumoursHeard >= 2
                && pubConversationsForMirek >= 2) {
            mirekLocationUnlocked = true;
        }
    }

    /**
     * Attempt treatment by Mirek at his IMPROVISED_DENTAL_CHAIR_PROP.
     *
     * @param inventory           player's inventory (costs 5 COIN)
     * @param policeNearby        true if a POLICE NPC is within 12 blocks
     * @param achievementCallback for awarding BUDGET_MOLAR and BOTCHED_JOB
     * @return the {@link MirekResult}
     */
    public MirekResult seekMirekTreatment(Inventory inventory, boolean policeNearby,
                                          AchievementCallback achievementCallback) {
        if (!mirekLocationUnlocked) {
            return MirekResult.LOCATION_UNKNOWN;
        }
        if (policeNearby) {
            // Mirek flees — nothing changes
            return MirekResult.POLICE_PRESENT;
        }
        if (inventory.getItemCount(Material.COIN) < MIREK_TREATMENT_COST) {
            return MirekResult.INSUFFICIENT_FUNDS;
        }

        inventory.removeItem(Material.COIN, MIREK_TREATMENT_COST);

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.BUDGET_MOLAR);
        }

        float roll = random.nextFloat();
        if (roll < MIREK_SUCCESS_CHANCE) {
            // Treatment succeeded
            toothachePoints = 0;
            // Check for infection side effect (20% of the time)
            if (random.nextFloat() < MIREK_INFECTION_CHANCE) {
                return MirekResult.SUCCESS_INFECTION;
            }
            return MirekResult.SUCCESS;
        } else {
            // Botched — toothache worsens
            toothachePoints = Math.min(100, toothachePoints + MIREK_BOTCH_DAMAGE);
            if (achievementCallback != null) {
                achievementCallback.award(AchievementType.BOTCHED_JOB);
            }
            return MirekResult.BOTCHED;
        }
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame.  Handles the SEVERE_TOOTHACHE twinge timer.
     *
     * @param delta      real seconds since last frame
     * @param timeSystem the time system (for in-game minute calculation)
     */
    public void update(float delta, TimeSystem timeSystem) {
        DebuffLevel level = getDebuffLevel(toothachePoints);
        if (level == DebuffLevel.SEVERE_TOOTHACHE) {
            float inGameMinutesThisFrame = delta * (timeSystem.getTimeSpeed() / 60.0f) * 60.0f;
            minutesSinceLastTwinge += inGameMinutesThisFrame;
            if (minutesSinceLastTwinge >= SEVERE_TWINGE_INTERVAL_MINUTES) {
                minutesSinceLastTwinge = 0f;
                // Twinge registered — caller/audio system can poll isTwingeReady()
                // or the update could trigger an audio callback here
            }
        } else {
            minutesSinceLastTwinge = 0f;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getToothachePoints()             { return toothachePoints; }
    public DebuffLevel getCurrentDebuffLevel()  { return getDebuffLevel(toothachePoints); }
    public int getAppointmentDay()              { return appointmentDay; }
    public boolean isNhsRegistered()            { return nhsRegistered; }
    public boolean isMirekLocationUnlocked()    { return mirekLocationUnlocked; }
    public int getLocalHealthRumoursHeard()     { return localHealthRumoursHeard; }
    public int getPubConversationsForMirek()    { return pubConversationsForMirek; }
    public boolean isNhsBanned(int currentDay)  { return nhsBanExpiry >= 0 && currentDay < nhsBanExpiry; }
    public int getNhsBanExpiry()                { return nhsBanExpiry; }
    public int getLastToothbrushDay()           { return lastToothbrushDay; }

    // ── Testing helpers ────────────────────────────────────────────────────────

    /** Force-set toothachePoints (for testing). */
    public void setToothachePointsForTesting(int points) { this.toothachePoints = points; }
    /** Force-set appointmentDay (for testing). */
    public void setAppointmentDayForTesting(int day)     { this.appointmentDay = day; }
    /** Force-set nhsRegistered (for testing). */
    public void setNhsRegisteredForTesting(boolean v)    { this.nhsRegistered = v; }
    /** Force-set mirekLocationUnlocked (for testing). */
    public void setMirekLocationUnlockedForTesting(boolean v) { this.mirekLocationUnlocked = v; }
    /** Force-set pubConversationsForMirek (for testing). */
    public void setPubConversationsForTesting(int n)     { this.pubConversationsForMirek = n; }
    /** Force-set localHealthRumoursHeard (for testing). */
    public void setLocalHealthRumoursHeardForTesting(int n) { this.localHealthRumoursHeard = n; }
    /** Force-set nhsBanExpiry (for testing). */
    public void setNhsBanExpiryForTesting(int day)       { this.nhsBanExpiry = day; }
    /** Force-set lastToothbrushDay (for testing). */
    public void setLastToothbrushDayForTesting(int day)  { this.lastToothbrushDay = day; }
    /** Force-set sixMonthWaitAwarded (for testing). */
    public void setSixMonthWaitAwardedForTesting(boolean v) { this.sixMonthWaitAwarded = v; }
}
