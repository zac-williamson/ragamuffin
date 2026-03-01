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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1022: Northfield GP Surgery — NHS Appointments, Sick Notes,
 * Repeat Prescriptions &amp; the Waiting Room Economy.
 *
 * <h3>Opening hours</h3>
 * <ul>
 *   <li>Mon–Fri 08:00–17:59</li>
 *   <li>Sat 08:30–11:59</li>
 *   <li>Closed Sunday</li>
 * </ul>
 *
 * <h3>Walk-in hours</h3>
 * Mon–Fri 08:00–10:29 only.
 *
 * <h3>Appointment system</h3>
 * Book via RECEPTION_DESK_PROP for a slot 1–3 days ahead (09:00–17:00).
 * Sitting in WAITING_CHAIR_PROP fast-forwards wait time at 10×.
 *
 * <h3>Consultation logic</h3>
 * {@link #consult(float, int, int)} returns one of five outcomes based on
 * player health and aggregated need levels:
 * <ul>
 *   <li>REFERRAL          — health &lt; 30</li>
 *   <li>SICK_NOTE         — health &lt; 60 or desperateLevel &ge; 50</li>
 *   <li>PRESCRIPTION_MEDS — desperateLevel &ge; 50</li>
 *   <li>ANTIDEPRESSANTS   — boredLevel &ge; 40 AND desperateLevel &ge; 40</li>
 *   <li>LIFESTYLE_ADVICE  — otherwise</li>
 * </ul>
 *
 * <h3>Repeat prescription</h3>
 * Collected once per in-game week at PHARMACY_HATCH_PROP.
 * Abuse detection (more than once per week) → Notoriety +5 and 2-week block.
 *
 * <h3>Drug cabinet raid</h3>
 * MEDICINE_CABINET_PROP: lockpick required, 4 hits; yields PRESCRIPTION_MEDS +
 * ANTIDEPRESSANTS; Wanted Tier 2; Notoriety +10.
 * DRUG_SAFE_PROP: crowbar required, 12 hits; yields PRESCRIPTION_MEDS ×8;
 * Wanted Tier 3; triggers NoiseSystem alarm; Notoriety +20.
 *
 * <h3>Waiting room economy</h3>
 * GP_PATIENT NPCs share rumours; 40 % carry a fetch-prescription quest rewarding
 * 3 COIN + LOCALS Respect +5.  LEAFLET_RACK_PROP yields NEON_LEAFLET items.
 *
 * <h3>Integrations</h3>
 * JobCentreSystem (sick note activation), FactionSystem (STREET_LADS used as
 * proxy for LOCALS +5 per completed fetch quest; MARCHETTI_CREW gains interest
 * in DRUG_SAFE loot), RumourNetwork (weekly LOCAL_HEALTH rumour from Dr. Nair),
 * NotorietySystem, WantedSystem, WeatherSystem (+2 patients when COLD_SNAP/FROST/RAIN).
 */
public class GPSurgerySystem {

    // ── Opening-hours constants ────────────────────────────────────────────────

    /** Weekday opening hour (08:00). */
    public static final float WEEKDAY_OPEN_HOUR  = 8.0f;
    /** Weekday closing hour (18:00, exclusive). */
    public static final float WEEKDAY_CLOSE_HOUR = 18.0f;
    /** Saturday opening hour (08:30). */
    public static final float SATURDAY_OPEN_HOUR = 8.5f;
    /** Saturday closing hour (12:00, exclusive). */
    public static final float SATURDAY_CLOSE_HOUR = 12.0f;
    /** Day index for Sunday (0 = Monday, 6 = Sunday using getDayCount % 7). */
    public static final int DAY_SUNDAY_INDEX = 6;
    public static final int DAY_SATURDAY_INDEX = 5;

    // ── Walk-in constants ──────────────────────────────────────────────────────

    /** Walk-in window opens (Mon–Fri only). */
    public static final float WALK_IN_OPEN_HOUR  = 8.0f;
    /** Walk-in window closes (exclusive upper bound). */
    public static final float WALK_IN_CLOSE_HOUR = 10.5f;

    // ── Notoriety thresholds ───────────────────────────────────────────────────

    /** Brenda refuses entry when player notoriety is at or above this value. */
    public static final int REFUSED_ENTRY_NOTORIETY = 70;

    // ── Consultation thresholds ───────────────────────────────────────────────

    public static final float HEALTH_REFERRAL_THRESHOLD     = 30f;
    public static final float HEALTH_SICK_NOTE_THRESHOLD    = 60f;
    public static final int   DESPERATE_SICK_NOTE_LEVEL     = 50;
    public static final int   DESPERATE_PRESCRIPTION_LEVEL  = 50;
    public static final int   BORED_ANTIDEPRESSANTS_LEVEL   = 40;
    public static final int   DESPERATE_ANTIDEPRESSANTS_LEVEL = 40;

    // ── Repeat prescription constants ─────────────────────────────────────────

    /** Days between allowed repeat-prescription collections. */
    public static final int REPEAT_PRESCRIPTION_INTERVAL_DAYS = 7;

    /** Days the player is blocked after abuse detection. */
    public static final int REPEAT_PRESCRIPTION_BLOCK_DAYS = 14;

    /** Notoriety penalty for repeat-prescription abuse. */
    public static final int REPEAT_PRESCRIPTION_ABUSE_NOTORIETY = 5;

    // ── Raid constants ────────────────────────────────────────────────────────

    /** Notoriety gain for raiding MEDICINE_CABINET_PROP. */
    public static final int MEDICINE_CABINET_RAID_NOTORIETY = 10;

    /** Wanted stars added for MEDICINE_CABINET_PROP raid. */
    public static final int MEDICINE_CABINET_WANTED_TIER = 2;

    /** Notoriety gain for cracking DRUG_SAFE_PROP. */
    public static final int DRUG_SAFE_RAID_NOTORIETY = 20;

    /** Wanted stars added for DRUG_SAFE_PROP crack. */
    public static final int DRUG_SAFE_WANTED_TIER = 3;

    /** Number of PRESCRIPTION_MEDS yielded by the drug safe. */
    public static final int DRUG_SAFE_MEDS_COUNT = 8;

    // ── Waiting room constants ────────────────────────────────────────────────

    /** Minimum GP_PATIENT NPCs in the waiting room. */
    public static final int WAITING_ROOM_MIN_PATIENTS = 2;

    /** Maximum GP_PATIENT NPCs in the waiting room. */
    public static final int WAITING_ROOM_MAX_PATIENTS = 5;

    /** Extra patients added when weather is cold or rainy. */
    public static final int COLD_WEATHER_EXTRA_PATIENTS = 2;

    /** Fraction of patients carrying a fetch-prescription quest. */
    public static final float FETCH_QUEST_CHANCE = 0.40f;

    /** Reward for completing a fetch-prescription quest. */
    public static final int FETCH_QUEST_COIN_REWARD = 3;

    /** Street Lads respect gain per completed fetch quest (proxy for LOCALS). */
    public static final int FETCH_QUEST_RESPECT = 5;

    /** Waiting-chair time multiplier (fast-forwards wait at 10×). */
    public static final float WAITING_CHAIR_TIME_MULT = 10.0f;

    /** Minutes of in-game waiting needed for WAITING_LIST achievement. */
    public static final float WAITING_LIST_MINUTES_REQUIRED = 30.0f;

    // ── Weekly rumour interval ─────────────────────────────────────────────────

    /** In-game days between LOCAL_HEALTH rumour seeds from Dr. Nair. */
    public static final int LOCAL_HEALTH_RUMOUR_INTERVAL_DAYS = 7;

    // ── Appointment constants ─────────────────────────────────────────────────

    /** Minimum days ahead an appointment can be booked. */
    public static final int APPOINTMENT_MIN_DAYS_AHEAD = 1;

    /** Maximum days ahead an appointment can be booked. */
    public static final int APPOINTMENT_MAX_DAYS_AHEAD = 3;

    /** Hour at which booked appointments occur (09:00). */
    public static final float APPOINTMENT_HOUR = 9.0f;

    // ── Consultation result enum ──────────────────────────────────────────────

    /**
     * Possible outcomes from a GP consultation.
     */
    public enum ConsultationResult {
        /** Immediate referral to hospital; health critically low (< 30). */
        REFERRAL,
        /** Sick note issued; health < 60 or desperateLevel ≥ 50. */
        SICK_NOTE,
        /** Prescription issued; desperateLevel ≥ 50. */
        PRESCRIPTION_MEDS,
        /** Antidepressants prescribed; boredLevel ≥ 40 AND desperateLevel ≥ 40. */
        ANTIDEPRESSANTS,
        /** Dr. Nair recommends lifestyle improvements. */
        LIFESTYLE_ADVICE
    }

    // ── Appointment booking result enum ──────────────────────────────────────

    public enum BookingResult {
        /** Appointment successfully booked. */
        SUCCESS,
        /** Reception closed or outside booking hours. */
        CLOSED,
        /** Player's notoriety is too high (≥ 70). */
        REFUSED,
        /** Player already has an active appointment. */
        ALREADY_BOOKED
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final TimeSystem timeSystem;
    private final NotorietySystem notorietySystem;
    private final JobCentreSystem jobCentreSystem;
    private final FactionSystem factionSystem;
    private final RumourNetwork rumourNetwork;
    private final WantedSystem wantedSystem;
    private final NPCManager npcManager;
    private final Random random;

    /** Day on which the player has a booked appointment (-1 = none). */
    private int appointmentDay = -1;

    /** Last day the player collected a repeat prescription (-1 = never). */
    private int lastRepeatPrescriptionDay = -1;

    /** Day on which the repeat-prescription block expires (-1 = no block). */
    private int repeatPrescriptionBlockExpiry = -1;

    /** Total appointments attended (for HYPOCHONDRIAC achievement). */
    private int appointmentsAttended = 0;

    /** Whether the HYPOCHONDRIAC achievement has been awarded. */
    private boolean hypochondriacAwarded = false;

    /** Total fetch-prescription quests completed (for GOOD_SAMARITAN achievement). */
    private int fetchQuestsCompleted = 0;

    /** Whether the GOOD_SAMARITAN achievement has been awarded. */
    private boolean goodSamaritanAwarded = false;

    /** Total in-game minutes spent sitting in the waiting room. */
    private float waitingRoomMinutes = 0f;

    /** Whether the WAITING_LIST achievement has been awarded. */
    private boolean waitingListAwarded = false;

    /** Day on which Dr. Nair last seeded the LOCAL_HEALTH rumour (-1 = never). */
    private int lastLocalHealthRumourDay = -1;

    /** Whether the patient NPCs have been spawned this session. */
    private boolean patientsSpawned = false;

    // ── Achievement callback ───────────────────────────────────────────────────

    /** Callback for awarding achievements (matches NotorietySystem pattern). */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public GPSurgerySystem(
            TimeSystem timeSystem,
            NotorietySystem notorietySystem,
            JobCentreSystem jobCentreSystem,
            FactionSystem factionSystem,
            RumourNetwork rumourNetwork,
            WantedSystem wantedSystem,
            NPCManager npcManager,
            Random random) {
        this.timeSystem       = timeSystem;
        this.notorietySystem  = notorietySystem;
        this.jobCentreSystem  = jobCentreSystem;
        this.factionSystem    = factionSystem;
        this.rumourNetwork    = rumourNetwork;
        this.wantedSystem     = wantedSystem;
        this.npcManager       = npcManager;
        this.random           = random;
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Call once per frame.
     *
     * @param delta    seconds since last frame
     * @param player   the player
     * @param allNpcs  all active NPCs
     * @param achievementCallback  callback for unlocking achievements (may be null)
     */
    public void update(float delta, Player player, List<NPC> allNpcs,
                       AchievementCallback achievementCallback) {
        // Weekly LOCAL_HEALTH rumour from Dr. Nair
        int currentDay = timeSystem.getDayCount();
        if (lastLocalHealthRumourDay < 0 ||
                currentDay - lastLocalHealthRumourDay >= LOCAL_HEALTH_RUMOUR_INTERVAL_DAYS) {
            if (isOpen()) {
                seedLocalHealthRumour(allNpcs);
                lastLocalHealthRumourDay = currentDay;
            }
        }
    }

    // ── Opening-hours logic ────────────────────────────────────────────────────

    /**
     * Returns true if the surgery is currently open based on the time system's
     * current day-of-week and time-of-day.
     */
    public boolean isOpen() {
        return isOpenAt(timeSystem.getDayCount(), timeSystem.getTime());
    }

    /**
     * Returns true if the surgery is open on the given day and at the given hour.
     *
     * @param dayCount running day counter from TimeSystem
     * @param hour     fractional hour (e.g. 9.5 = 09:30)
     */
    public boolean isOpenAt(int dayCount, float hour) {
        int dayOfWeek = dayCount % 7; // 0=Mon … 5=Sat, 6=Sun
        if (dayOfWeek == DAY_SUNDAY_INDEX) return false;
        if (dayOfWeek == DAY_SATURDAY_INDEX) {
            return hour >= SATURDAY_OPEN_HOUR && hour < SATURDAY_CLOSE_HOUR;
        }
        // Monday–Friday
        return hour >= WEEKDAY_OPEN_HOUR && hour < WEEKDAY_CLOSE_HOUR;
    }

    // ── Walk-in availability ───────────────────────────────────────────────────

    /**
     * Returns true if walk-in appointments are available right now
     * (Mon–Fri, 08:00–10:29).
     */
    public boolean isWalkInAvailable() {
        return isWalkInAvailableAt(timeSystem.getDayCount(), timeSystem.getTime());
    }

    /**
     * Returns true if walk-in appointments are available at the given day and hour.
     */
    public boolean isWalkInAvailableAt(int dayCount, float hour) {
        int dayOfWeek = dayCount % 7;
        if (dayOfWeek == DAY_SATURDAY_INDEX || dayOfWeek == DAY_SUNDAY_INDEX) return false;
        return hour >= WALK_IN_OPEN_HOUR && hour < WALK_IN_CLOSE_HOUR;
    }

    // ── Appointment booking ────────────────────────────────────────────────────

    /**
     * Attempt to book an appointment at the RECEPTION_DESK_PROP.
     *
     * @param notoriety current player notoriety score (raw, not tier)
     * @return a {@link BookingResult} describing the outcome
     */
    public BookingResult bookAppointment(int notoriety) {
        if (!isOpen()) {
            return BookingResult.CLOSED;
        }
        if (notoriety >= REFUSED_ENTRY_NOTORIETY) {
            return BookingResult.REFUSED;
        }
        if (appointmentDay >= 0) {
            return BookingResult.ALREADY_BOOKED;
        }
        int daysAhead = APPOINTMENT_MIN_DAYS_AHEAD
                + random.nextInt(APPOINTMENT_MAX_DAYS_AHEAD - APPOINTMENT_MIN_DAYS_AHEAD + 1);
        appointmentDay = timeSystem.getDayCount() + daysAhead;
        return BookingResult.SUCCESS;
    }

    /**
     * Returns the day on which the player has a booked appointment, or -1 if none.
     */
    public int getAppointmentDay() {
        return appointmentDay;
    }

    /**
     * Returns true if the player has a valid appointment for today.
     */
    public boolean hasAppointmentToday() {
        return appointmentDay >= 0 && appointmentDay == timeSystem.getDayCount();
    }

    // ── Consultation ──────────────────────────────────────────────────────────

    /**
     * Determine what Dr. Nair prescribes based on the player's condition.
     *
     * <p>Priority order (first match wins):
     * <ol>
     *   <li>REFERRAL if {@code health < 30}</li>
     *   <li>SICK_NOTE if {@code health < 60} OR {@code desperateLevel >= 50}</li>
     *   <li>PRESCRIPTION_MEDS if {@code desperateLevel >= 50}</li>
     *   <li>ANTIDEPRESSANTS if {@code boredLevel >= 40} AND {@code desperateLevel >= 40}</li>
     *   <li>LIFESTYLE_ADVICE otherwise</li>
     * </ol>
     *
     * @param health        player health (0–100)
     * @param desperateLevel aggregated desperation need level (0–100)
     * @param boredLevel    aggregated boredom need level (0–100)
     * @return the {@link ConsultationResult}
     */
    public ConsultationResult consult(float health, int desperateLevel, int boredLevel) {
        if (health < HEALTH_REFERRAL_THRESHOLD) {
            return ConsultationResult.REFERRAL;
        }
        if (health < HEALTH_SICK_NOTE_THRESHOLD || desperateLevel >= DESPERATE_SICK_NOTE_LEVEL) {
            return ConsultationResult.SICK_NOTE;
        }
        if (desperateLevel >= DESPERATE_PRESCRIPTION_LEVEL) {
            return ConsultationResult.PRESCRIPTION_MEDS;
        }
        if (boredLevel >= BORED_ANTIDEPRESSANTS_LEVEL
                && desperateLevel >= DESPERATE_ANTIDEPRESSANTS_LEVEL) {
            return ConsultationResult.ANTIDEPRESSANTS;
        }
        return ConsultationResult.LIFESTYLE_ADVICE;
    }

    /**
     * Perform a full consultation: add items to inventory, record sign-on exemption
     * if SICK_NOTE issued, and consume appointment booking.
     *
     * @param player               the player
     * @param inventory            the player's inventory
     * @param desperateLevel       aggregated desperation need level (0–100)
     * @param boredLevel           aggregated boredom need level (0–100)
     * @param achievementCallback  callback for achievements (may be null)
     * @return the consultation result
     */
    public ConsultationResult performConsultation(
            Player player, Inventory inventory,
            int desperateLevel, int boredLevel,
            AchievementCallback achievementCallback) {

        ConsultationResult result = consult(player.getHealth(), desperateLevel, boredLevel);

        // Dispense items
        switch (result) {
            case SICK_NOTE:
                inventory.addItem(Material.SICK_NOTE, 1);
                // Activate sick note benefit in JobCentreSystem
                if (jobCentreSystem != null) {
                    jobCentreSystem.activateSickNote(timeSystem.getDayCount());
                }
                break;
            case PRESCRIPTION_MEDS:
                inventory.addItem(Material.PRESCRIPTION_MEDS, 1);
                break;
            case ANTIDEPRESSANTS:
                inventory.addItem(Material.ANTIDEPRESSANTS, 1);
                break;
            case REFERRAL:
            case LIFESTYLE_ADVICE:
            default:
                // No physical item — narrative outcome
                break;
        }

        // Consume appointment
        appointmentDay = -1;

        // Track attendance for HYPOCHONDRIAC achievement
        appointmentsAttended++;
        if (achievementCallback != null && !hypochondriacAwarded
                && appointmentsAttended >= 5) {
            hypochondriacAwarded = true;
            achievementCallback.award(AchievementType.HYPOCHONDRIAC);
        }

        return result;
    }

    // ── Sick note → JobCentre integration ────────────────────────────────────

    /**
     * Check whether the player currently has an active sick note in the JobCentre.
     * Used by integration tests and UI to display the sick-note status.
     */
    public boolean isSickNoteActiveInJobCentre() {
        return jobCentreSystem != null && jobCentreSystem.isSickNoteActive();
    }

    // ── Repeat prescription ───────────────────────────────────────────────────

    /**
     * Whether the player can collect a repeat prescription right now.
     *
     * @param currentDay the current in-game day
     * @return true if the player is eligible
     */
    public boolean canCollectRepeatPrescription(int currentDay) {
        if (repeatPrescriptionBlockExpiry >= 0 && currentDay < repeatPrescriptionBlockExpiry) {
            return false; // Blocked
        }
        if (lastRepeatPrescriptionDay < 0) return true; // Never collected
        return currentDay - lastRepeatPrescriptionDay >= REPEAT_PRESCRIPTION_INTERVAL_DAYS;
    }

    /**
     * Collect a repeat prescription at the PHARMACY_HATCH_PROP.
     *
     * <p>If the player is not yet eligible (more than once per week),
     * abuse is detected: Notoriety +5 and a 2-week block is applied.
     *
     * @param inventory            the player's inventory
     * @param currentDay           the current in-game day
     * @param achievementCallback  callback for achievements (may be null)
     * @return true if the prescription was collected successfully
     */
    public boolean collectRepeatPrescription(Inventory inventory, int currentDay,
                                             AchievementCallback achievementCallback) {
        if (!canCollectRepeatPrescription(currentDay)) {
            // Abuse detected
            notorietySystem.addNotoriety(
                    REPEAT_PRESCRIPTION_ABUSE_NOTORIETY,
                    achievementCallback != null
                            ? achievementCallback::award : null);
            repeatPrescriptionBlockExpiry = currentDay + REPEAT_PRESCRIPTION_BLOCK_DAYS;
            return false;
        }
        inventory.addItem(Material.PRESCRIPTION_MEDS, 1);
        lastRepeatPrescriptionDay = currentDay;
        return true;
    }

    // ── Drug cabinet raid ─────────────────────────────────────────────────────

    /**
     * Handle raiding the MEDICINE_CABINET_PROP (lockpick required, 4 hits).
     * Yields PRESCRIPTION_MEDS + ANTIDEPRESSANTS. Triggers Wanted Tier 2 and
     * Notoriety +10.
     *
     * @param inventory            the player's inventory
     * @param player               the player (for wanted-star position)
     * @param achievementCallback  callback for achievements (may be null)
     */
    public void raidMedicineCabinet(Inventory inventory, Player player,
                                    AchievementCallback achievementCallback) {
        inventory.addItem(Material.PRESCRIPTION_MEDS, 1);
        inventory.addItem(Material.ANTIDEPRESSANTS, 1);

        notorietySystem.addNotoriety(MEDICINE_CABINET_RAID_NOTORIETY,
                achievementCallback != null ? achievementCallback::award : null);

        if (wantedSystem != null) {
            wantedSystem.addWantedStars(MEDICINE_CABINET_WANTED_TIER,
                    player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    null);
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.SELF_MEDICATING);
        }
    }

    /**
     * Handle cracking the DRUG_SAFE_PROP (crowbar required, 12 hits).
     * Yields PRESCRIPTION_MEDS ×8. Triggers Wanted Tier 3, NoiseSystem alarm,
     * and Notoriety +20. Marchetti Crew gain interest (MARCH faction bump via
     * LOOT_TIP rumour).
     *
     * @param inventory            the player's inventory
     * @param player               the player
     * @param allNpcs              all active NPCs (for rumour seeding)
     * @param achievementCallback  callback for achievements (may be null)
     */
    public void raidDrugSafe(Inventory inventory, Player player, List<NPC> allNpcs,
                             AchievementCallback achievementCallback) {
        inventory.addItem(Material.PRESCRIPTION_MEDS, DRUG_SAFE_MEDS_COUNT);

        notorietySystem.addNotoriety(DRUG_SAFE_RAID_NOTORIETY,
                achievementCallback != null ? achievementCallback::award : null);

        if (wantedSystem != null) {
            wantedSystem.addWantedStars(DRUG_SAFE_WANTED_TIER,
                    player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    null);
        }

        // Marchetti Crew gains interest in the surgery loot
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, 5);
        }

        // Seed a LOOT_TIP rumour for Marchetti NPCs
        if (rumourNetwork != null && allNpcs != null) {
            Rumour rumour = new Rumour(RumourType.LOOT_TIP,
                    "Heard the surgery drug safe got cracked — Marchetti boys are interested");
            int seeded = 0;
            for (NPC npc : allNpcs) {
                if (!npc.isAlive()) continue;
                rumourNetwork.addRumour(npc, rumour);
                if (++seeded >= 3) break;
            }
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.SURGERY_RAIDER);
        }
    }

    // ── Waiting room economy ──────────────────────────────────────────────────

    /**
     * Compute how many GP_PATIENT NPCs should be in the waiting room.
     *
     * <p>Base count: random in [{@link #WAITING_ROOM_MIN_PATIENTS},
     * {@link #WAITING_ROOM_MAX_PATIENTS}].  If weather is COLD_SNAP, FROST, or
     * RAIN, add {@link #COLD_WEATHER_EXTRA_PATIENTS}.
     *
     * @param weatherSystem the current weather system (may be null)
     * @return patient count
     */
    public int computeWaitingRoomCount(WeatherSystem weatherSystem) {
        int base = WAITING_ROOM_MIN_PATIENTS
                + random.nextInt(WAITING_ROOM_MAX_PATIENTS - WAITING_ROOM_MIN_PATIENTS + 1);
        if (weatherSystem != null) {
            Weather w = weatherSystem.getCurrentWeather();
            if (w == Weather.COLD_SNAP
                    || w == Weather.FROST
                    || w == Weather.RAIN) {
                base += COLD_WEATHER_EXTRA_PATIENTS;
            }
        }
        return base;
    }

    /**
     * Update the waiting-room timer.  Call each frame when the player is seated
     * in a WAITING_CHAIR_PROP.
     *
     * @param delta               real seconds this frame
     * @param playerIsSeated      true if the player is currently sitting
     * @param achievementCallback callback for achievements (may be null)
     */
    public void tickWaitingRoom(float delta, boolean playerIsSeated,
                                AchievementCallback achievementCallback) {
        if (!playerIsSeated) return;
        // Convert real seconds to in-game minutes (time runs at 1:50 real→game)
        waitingRoomMinutes += delta * (timeSystem.getTimeSpeed() / 60.0f) * 60.0f;
        if (achievementCallback != null && !waitingListAwarded
                && waitingRoomMinutes >= WAITING_LIST_MINUTES_REQUIRED) {
            waitingListAwarded = true;
            achievementCallback.award(AchievementType.WAITING_LIST);
        }
    }

    /**
     * Complete a fetch-prescription quest for a GP_PATIENT NPC.
     * Rewards 3 COIN and +5 Street Lads Respect (proxy for LOCALS).
     *
     * @param inventory           the player's inventory
     * @param achievementCallback callback for achievements (may be null)
     */
    public void completeFetchPrescriptionQuest(Inventory inventory,
                                               AchievementCallback achievementCallback) {
        inventory.addItem(Material.COIN, FETCH_QUEST_COIN_REWARD);

        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, FETCH_QUEST_RESPECT);
        }

        fetchQuestsCompleted++;
        if (achievementCallback != null && !goodSamaritanAwarded
                && fetchQuestsCompleted >= 3) {
            goodSamaritanAwarded = true;
            achievementCallback.award(AchievementType.GOOD_SAMARITAN);
        }

        // Also track OFF_SICK if using sick note to dodge sign-on
        // (done in performConsultation when SICK_NOTE is issued)
    }

    /**
     * Award the OFF_SICK achievement when the player presents a SICK_NOTE at the
     * JobCentre to avoid a sanction.
     *
     * @param achievementCallback callback for achievements (may be null)
     */
    public void onSickNoteUsedAtJobCentre(AchievementCallback achievementCallback) {
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.OFF_SICK);
        }
    }

    // ── NPC spawning helpers ───────────────────────────────────────────────────

    /**
     * Spawn the core named NPCs for the GP Surgery (Brenda, Dr. Nair, Nurse Pat)
     * near the given world-space position.
     *
     * @param x  centre X of the surgery building
     * @param y  floor Y
     * @param z  centre Z of the surgery building
     */
    public void spawnSurgeryNpcs(float x, float y, float z) {
        if (npcManager == null) return;

        // Brenda — receptionist
        NPC brenda = npcManager.spawnNPC(NPCType.GP_RECEPTIONIST, x - 2f, y, z - 1f);
        if (brenda != null) {
            brenda.setName("Brenda");
            brenda.setBuildingType(LandmarkType.GP_SURGERY);
            brenda.setState(NPCState.WANDERING);
            brenda.setSpeechText("Have you got an appointment?", 4f);
        }

        // Dr. Nair — GP
        NPC drNair = npcManager.spawnNPC(NPCType.GP_DOCTOR, x + 1f, y, z + 1f);
        if (drNair != null) {
            drNair.setName("Dr. Nair");
            drNair.setBuildingType(LandmarkType.GP_SURGERY);
            drNair.setState(NPCState.WANDERING);
            drNair.setSpeechText("Right, what seems to be the trouble?", 4f);
        }

        // Nurse Pat — present Mon/Wed/Fri
        int dayOfWeek = timeSystem.getDayCount() % 7;
        boolean nursePresent = (dayOfWeek == 0 || dayOfWeek == 2 || dayOfWeek == 4);
        if (nursePresent) {
            NPC nurse = npcManager.spawnNPC(NPCType.GP_NURSE, x, y, z + 2f);
            if (nurse != null) {
                nurse.setName("Nurse Pat");
                nurse.setBuildingType(LandmarkType.GP_SURGERY);
                nurse.setState(NPCState.WANDERING);
                nurse.setSpeechText("Just a little scratch.", 3f);
            }
        }
    }

    /**
     * Spawn patient NPCs in the waiting room.
     *
     * @param x             centre X of the waiting area
     * @param y             floor Y
     * @param z             centre Z of the waiting area
     * @param weatherSystem current weather (for occupancy calculation)
     */
    public void spawnPatients(float x, float y, float z, WeatherSystem weatherSystem) {
        if (npcManager == null) return;
        int count = computeWaitingRoomCount(weatherSystem);
        for (int i = 0; i < count; i++) {
            float px = x + (random.nextFloat() - 0.5f) * 4f;
            float pz = z + (random.nextFloat() - 0.5f) * 2f;
            NPC patient = npcManager.spawnNPC(NPCType.GP_PATIENT, px, y, pz);
            if (patient != null) {
                patient.setBuildingType(LandmarkType.GP_SURGERY);
                patient.setState(NPCState.WANDERING);
                patient.setSpeechText(getRandomPatientSpeech(), 3f);
                // 40% chance of fetch quest
                boolean hasFetchQuest = random.nextFloat() < FETCH_QUEST_CHANCE;
                patient.setName(hasFetchQuest ? "patient_fetch_" + i : "patient_" + i);
            }
        }
        patientsSpawned = true;
    }

    /**
     * Returns true if patient NPCs have already been spawned this session.
     */
    public boolean arePatientsSpawned() {
        return patientsSpawned;
    }

    // ── Rumour seeding ────────────────────────────────────────────────────────

    /**
     * Seed the weekly LOCAL_HEALTH rumour from Dr. Nair into nearby NPCs.
     */
    private void seedLocalHealthRumour(List<NPC> allNpcs) {
        if (rumourNetwork == null || allNpcs == null) return;
        String[] texts = {
            "Doctor Nair reckons half the street's got stress-related conditions — not surprised",
            "The surgery's packed every Monday morning — something in the water round here",
            "Nurse Pat's been doing flu jabs all week — queue was out the door",
            "Heard the GP's got a two-week wait now — better not get ill"
        };
        Rumour rumour = new Rumour(RumourType.LOCAL_HEALTH,
                texts[random.nextInt(texts.length)]);
        int seeded = 0;
        for (NPC npc : allNpcs) {
            if (!npc.isAlive()) continue;
            rumourNetwork.addRumour(npc, rumour);
            if (++seeded >= 3) break;
        }
    }

    // ── Receptionist gate-keeping ─────────────────────────────────────────────

    /**
     * Check whether Brenda would refuse the player entry right now.
     *
     * @param notoriety the player's raw notoriety score
     * @return true if the player is refused
     */
    public boolean isBrendaRefusingEntry(int notoriety) {
        return notoriety >= REFUSED_ENTRY_NOTORIETY;
    }

    /**
     * Trigger Brenda calling the police during a raid.
     * Adds a Wanted star and spawns a POLICE NPC near the surgery.
     *
     * @param player               the player
     * @param achievementCallback  callback for achievements (may be null)
     */
    public void onRaidDetectedByBrenda(Player player, AchievementCallback achievementCallback) {
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(1,
                    player.getPosition().x, player.getPosition().y, player.getPosition().z,
                    null);
        }
        if (npcManager != null) {
            NPC police = npcManager.spawnNPC(NPCType.POLICE,
                    player.getPosition().x + 10f, player.getPosition().y,
                    player.getPosition().z);
            if (police != null) {
                police.setState(NPCState.CHASING_PLAYER);
                police.setSpeechText("Oi! Step away from that cabinet!", 4f);
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String getRandomPatientSpeech() {
        String[] lines = {
            "Been waiting forty minutes.",
            "You got an appointment?",
            "Hope it's not serious.",
            "The receptionist won't tell me anything.",
            "Been here since eight o'clock.",
            "Is it always this busy?"
        };
        return lines[random.nextInt(lines.length)];
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getAppointmentsAttended() { return appointmentsAttended; }
    public int getFetchQuestsCompleted()  { return fetchQuestsCompleted; }
    public float getWaitingRoomMinutes()  { return waitingRoomMinutes; }
    public int getLastRepeatPrescriptionDay() { return lastRepeatPrescriptionDay; }
    public int getRepeatPrescriptionBlockExpiry() { return repeatPrescriptionBlockExpiry; }

    /** Force-set the appointment day (for testing). */
    public void setAppointmentDayForTesting(int day) { this.appointmentDay = day; }

    /** Force-set last repeat prescription day (for testing). */
    public void setLastRepeatPrescriptionDayForTesting(int day) {
        this.lastRepeatPrescriptionDay = day;
    }

    /** Force-set repeat prescription block expiry (for testing). */
    public void setRepeatPrescriptionBlockExpiryForTesting(int day) {
        this.repeatPrescriptionBlockExpiry = day;
    }

    /** Force-set waiting room minutes (for testing). */
    public void setWaitingRoomMinutesForTesting(float minutes) {
        this.waitingRoomMinutes = minutes;
    }

    /** Force-set appointments attended (for testing). */
    public void setAppointmentsAttendedForTesting(int count) {
        this.appointmentsAttended = count;
    }

    /** Force-set fetch quests completed (for testing). */
    public void setFetchQuestsCompletedForTesting(int count) {
        this.fetchQuestsCompleted = count;
    }
}
