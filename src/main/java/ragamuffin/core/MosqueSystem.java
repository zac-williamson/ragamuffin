package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Issue #1030: Al-Noor Mosque — Friday Prayers, Iftar Table &amp; Community Sanctuary.
 *
 * <p>Brings the {@code MOSQUE} landmark to life with five daily prayers, Friday Jumu'ah,
 * a Ramadan Iftar table, a sanctuary mechanic, wudu room, outdoor tap, and a takings box.
 *
 * <h3>Prayer Schedule</h3>
 * <ul>
 *   <li>Fajr:    05:30–05:45</li>
 *   <li>Dhuhr:   13:00–13:15</li>
 *   <li>Asr:     16:00–16:15</li>
 *   <li>Maghrib: 19:30–19:45</li>
 *   <li>Isha:    21:00–21:15</li>
 * </ul>
 * During prayer, 4–8 WORSHIPPER NPCs kneel; combat is suppressed; noise ≥ 3 triggers
 * gentle remonstration from the nearest WORSHIPPER. Repeat offence → −10 Community
 * Respect + ejection.
 *
 * <h3>Friday Jumu'ah</h3>
 * Friday 13:00–14:00: 10–16 worshippers, Imam sermon. Player attendance (shoes removed,
 * within 6 blocks of MIHRAB_PROP for full hour) gives +3 Community Respect and unlocks
 * JUMU_AH_REGULAR (3× attendance). After prayer, worshippers share NEIGHBOURHOOD rumours.
 *
 * <h3>Iftar Table (Ramadan)</h3>
 * 30-day random window per play-through. At Maghrib, FOLD_TABLE_PROP is set up; gives
 * DATE_FRUIT×3, FLATBREAD×1, SOUP_CUP×1 once per session. Seeds IFTAR_TONIGHT rumour.
 *
 * <h3>Sanctuary</h3>
 * −1 Wanted star per 2 in-game minutes inside (shoes removed). Police hesitate 15s
 * before entering outside prayer hours; police won't enter during active prayer if
 * Wanted ≤ 3. At Wanted ≥ 4, police enter regardless.
 *
 * <h3>Outdoor Tap</h3>
 * Press E on OUTDOOR_TAP_PROP: +10 Thirst, always accessible.
 *
 * <h3>Wudu Room</h3>
 * Press E on ABLUTION_TAP_PROP: +3 Warmth, removes DIRTY status.
 *
 * <h3>Takings Box Robbery</h3>
 * Destroying TAKINGS_BOX_PROP yields 5–15 COIN but triggers COMMUNITY_OUTRAGE rumour,
 * +3 Notoriety, −20 Community Respect, THEFT_FROM_PLACE_OF_WORSHIP, and permanent
 * sanctuary revocation.
 */
public class MosqueSystem {

    // ── Day-of-week constants (dayCount % 7, same convention as ChurchSystem) ────
    // Game start = day 1 = Wednesday, day%7: 0=Mon,1=Tue,2=Wed,3=Thu,4=Fri,5=Sat,6=Sun
    private static final int FRIDAY = 4;

    // ── Prayer time constants ─────────────────────────────────────────────────

    /** Fajr prayer start hour. */
    public static final float FAJR_START   = 5.5f;   // 05:30
    /** Fajr prayer end hour. */
    public static final float FAJR_END     = 5.75f;  // 05:45

    /** Dhuhr prayer start hour. */
    public static final float DHUHR_START  = 13.0f;
    /** Dhuhr prayer end hour. */
    public static final float DHUHR_END    = 13.25f; // 13:15

    /** Asr prayer start hour. */
    public static final float ASR_START    = 16.0f;
    /** Asr prayer end hour. */
    public static final float ASR_END      = 16.25f; // 16:15

    /** Maghrib prayer start hour. */
    public static final float MAGHRIB_START = 19.5f; // 19:30
    /** Maghrib prayer end hour. */
    public static final float MAGHRIB_END   = 19.75f; // 19:45

    /** Isha prayer start hour. */
    public static final float ISHA_START   = 21.0f;
    /** Isha prayer end hour. */
    public static final float ISHA_END     = 21.25f; // 21:15

    /** Friday Jumu'ah start hour (replaces Dhuhr). */
    public static final float JUMUAH_START = 13.0f;
    /** Friday Jumu'ah end hour. */
    public static final float JUMUAH_END   = 14.0f;

    // ── Sanctuary constants ───────────────────────────────────────────────────

    /** Seconds inside mosque (shoes removed) to lose one Wanted star. */
    public static final float SANCTUARY_DECAY_TIME = 120.0f; // 2 in-game minutes

    /** Hesitation time (seconds) before police enter outside prayer hours. */
    public static final float POLICE_HESITATION_TIME = 15.0f;

    /** Maximum Wanted stars for sanctuary protection during active prayer. */
    public static final int SANCTUARY_MAX_WANTED = 3;

    // ── Noise constants ───────────────────────────────────────────────────────

    /** Noise level that triggers a gentle remonstration from worshippers (0–1 scale). */
    public static final float NOISE_REMONSTRATION_THRESHOLD = 0.3f; // noise ≥ 3

    /** Community Respect penalty for repeat noise offence during prayer. */
    public static final int NOISE_REPEAT_RESPECT_PENALTY = 10;

    // ── Jumu'ah constants ─────────────────────────────────────────────────────

    /** Community Respect bonus for full Jumu'ah attendance. */
    public static final int JUMUAH_RESPECT_BONUS = 3;

    /** Distance (blocks) from MIHRAB_PROP required for Jumu'ah attendance. */
    public static final float JUMUAH_MIHRAB_DISTANCE = 6.0f;

    /** Jumu'ah attendance count required for JUMU_AH_REGULAR achievement. */
    public static final int JUMUAH_REGULAR_THRESHOLD = 3;

    // ── Takings box constants ─────────────────────────────────────────────────

    /** Notoriety gain from robbing the takings box. */
    public static final int TAKINGS_BOX_NOTORIETY = 3;

    /** Community Respect penalty for robbing the takings box. */
    public static final int TAKINGS_BOX_RESPECT_PENALTY = 20;

    // ── Iftar constants ───────────────────────────────────────────────────────

    /** Number of DATE_FRUIT given at the Iftar table. */
    public static final int IFTAR_DATE_FRUIT_COUNT = 3;

    // ── Donation constants ────────────────────────────────────────────────────

    /** Community Respect per 2 COIN donated to the takings box. */
    public static final int DONATION_RESPECT_PER_TWO_COIN = 5;

    /** Number of donations required to unlock COMMUNITY_PILLAR achievement. */
    public static final int COMMUNITY_PILLAR_THRESHOLD = 5;

    /** THE_COUNCIL Respect bonus per 3 donations. */
    public static final int COUNCIL_RESPECT_PER_THREE_DONATIONS = 1;

    // ── Warmth constants ──────────────────────────────────────────────────────

    /** Warmth gained per second inside the prayer hall during prayer (5/min = ~0.083/s). */
    public static final float PRAYER_HALL_WARMTH_RATE = 5.0f / 60.0f;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Active worshipper NPCs during prayer. */
    private final List<NPC> worshipperNpcs = new ArrayList<>();

    /** The IMAM NPC (Hassan). */
    private NPC imamNpc = null;
    private boolean imamSpawned = false;

    /** Whether the player is currently inside the mosque. */
    private boolean playerInsideMosque = false;

    /** Whether the player has removed their shoes. */
    private boolean shoesRemoved = false;

    /** Timer tracking how long the player has been inside (for sanctuary decay). */
    private float sanctuaryTimer = 0f;

    /** Whether sanctuary has been permanently revoked (due to takings box robbery). */
    private boolean sanctuaryRevoked = false;

    /** Whether the player has collected Iftar food this session. */
    private boolean iftarCollectedThisSession = false;

    /** Current Ramadan day window start (day index). Randomised once per play-through. */
    private int ramadanStartDay = -1;
    /** Duration of the Ramadan window in days. */
    private static final int RAMADAN_DURATION = 30;

    /** Number of Jumu'ah attendances this play-through. */
    private int jumuahAttendanceCount = 0;

    /** Last Jumu'ah day attended (to avoid double-counting). */
    private int lastJumuahDayAttended = -1;

    /** Whether the player has been inside during the current Jumu'ah (tracking time). */
    private float jumuahAttendanceTimer = 0f;

    /** Whether this Jumu'ah attendance has been counted. */
    private boolean jumuahCountedThisWeek = false;

    /** Number of donations made (for COMMUNITY_PILLAR). */
    private int donationCount = 0;

    /** Number of times player has been noisy during prayer (for repeat-offence tracking). */
    private int noiseOffenceCount = 0;

    /** Mosque position and bounds. */
    private float mosqueX = 60f;
    private float mosqueZ = 120f;
    private float mosqueWidth  = 10f;
    private float mosqueDepth  = 16f;

    private final Random random;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /** Callback for awarding achievements. */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public MosqueSystem() {
        this(new Random());
    }

    public MosqueSystem(Random random) {
        this.random = random;
        // Randomise the Ramadan window: starts between day 10 and day 100
        this.ramadanStartDay = 10 + random.nextInt(90);
    }

    // ── Mosque bounds ─────────────────────────────────────────────────────────

    /**
     * Set the world position and size of the mosque (for inside/outside checks).
     */
    public void setMosqueBounds(float x, float z, float width, float depth) {
        this.mosqueX = x;
        this.mosqueZ = z;
        this.mosqueWidth = width;
        this.mosqueDepth = depth;
    }

    /**
     * Check whether a world position is inside the mosque bounds.
     */
    public boolean isInsideMosque(float x, float z) {
        return x >= mosqueX && x <= mosqueX + mosqueWidth
            && z >= mosqueZ && z <= mosqueZ + mosqueDepth;
    }

    // ── Prayer schedule queries ────────────────────────────────────────────────

    /**
     * Returns true if any daily prayer (including Jumu'ah) is currently active.
     *
     * @param dayCount  the current day count from TimeSystem
     * @param hour      the current hour (0–24) from TimeSystem
     */
    public boolean isPrayerActive(int dayCount, float hour) {
        if (isJumuahActive(dayCount, hour)) return true;
        return isDailyPrayerHour(hour);
    }

    /**
     * Returns true if it is Friday Jumu'ah time (Friday 13:00–14:00).
     */
    public boolean isJumuahActive(int dayCount, float hour) {
        return dayCount % 7 == FRIDAY
            && hour >= JUMUAH_START && hour < JUMUAH_END;
    }

    /**
     * Returns true if any of the five daily prayers is currently active
     * (excluding Friday Jumu'ah which extends Dhuhr).
     */
    private boolean isDailyPrayerHour(float hour) {
        return (hour >= FAJR_START    && hour < FAJR_END)
            || (hour >= DHUHR_START   && hour < DHUHR_END)
            || (hour >= ASR_START     && hour < ASR_END)
            || (hour >= MAGHRIB_START && hour < MAGHRIB_END)
            || (hour >= ISHA_START    && hour < ISHA_END);
    }

    /**
     * Returns true if the mosque is within its open hours (08:00–21:15).
     */
    public boolean isOpen(float hour) {
        return hour >= 8.0f && hour < 21.25f;
    }

    /**
     * Returns true if it is currently Ramadan (30-day random window).
     */
    public boolean isRamadan(int dayCount) {
        return dayCount >= ramadanStartDay && dayCount < ramadanStartDay + RAMADAN_DURATION;
    }

    /**
     * Returns true if the Iftar table should be active (Ramadan + Maghrib period).
     */
    public boolean isIftarTableActive(int dayCount, float hour) {
        return isRamadan(dayCount)
            && hour >= MAGHRIB_START && hour < ISHA_START;
    }

    /**
     * Returns the expected worshipper count at the current time.
     *
     * @param dayCount  the current day count
     * @param hour      the current hour
     * @return 10–16 during Jumu'ah, 4–8 during regular prayers, 0 otherwise
     */
    public int getWorshipperCount(int dayCount, float hour) {
        if (isJumuahActive(dayCount, hour)) {
            return 10 + random.nextInt(7); // 10–16
        }
        if (isDailyPrayerHour(hour)) {
            return 4 + random.nextInt(5);  // 4–8
        }
        return 0;
    }

    /**
     * Returns whether sanctuary is currently active for the player.
     * Sanctuary is revoked permanently if the player robbed the takings box.
     *
     * @param prayerActive  whether prayer is currently ongoing
     * @param wantedStars   the player's current wanted stars
     * @return true if the mosque provides sanctuary
     */
    public boolean isSanctuaryActive(boolean prayerActive, int wantedStars) {
        if (sanctuaryRevoked) return false;
        if (prayerActive && wantedStars <= SANCTUARY_MAX_WANTED) return true;
        // Outside prayer hours: still provides sanctuary (police hesitate, stars decay)
        return !sanctuaryRevoked;
    }

    /**
     * Returns whether police should enter the mosque right now.
     * Police won't enter during active prayer if Wanted ≤ 3; always enter if Wanted ≥ 4.
     *
     * @param prayerActive  whether prayer is currently ongoing
     * @param wantedStars   the player's current wanted stars
     * @return true if police may enter
     */
    public boolean canPoliceEnter(boolean prayerActive, int wantedStars) {
        if (wantedStars >= 4) return true;
        if (prayerActive) return false; // blocked during prayer if Wanted ≤ 3
        return true; // outside prayer: hesitate but can enter
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the mosque system each frame.
     *
     * @param delta             seconds since last frame
     * @param timeSystem        the TimeSystem
     * @param npcs              all living NPCs
     * @param wantedSystem      the WantedSystem (null-safe)
     * @param notorietySystem   the NotorietySystem (null-safe)
     * @param factionSystem     the FactionSystem (null-safe)
     * @param achievementCallback callback for awarding achievements
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs,
                       WantedSystem wantedSystem, NotorietySystem notorietySystem,
                       FactionSystem factionSystem,
                       AchievementCallback achievementCallback) {
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();

        boolean prayerActive = isPrayerActive(dayCount, hour);

        // Spawn / despawn worshippers
        manageWorshippers(dayCount, hour, prayerActive, npcs);

        // Spawn / despawn Imam
        manageImam(hour, npcs);

        // Reset Iftar session flag when leaving Iftar window
        if (!isIftarTableActive(dayCount, hour)) {
            iftarCollectedThisSession = false;
        }

        // Sanctuary decay
        if (playerInsideMosque && shoesRemoved && !sanctuaryRevoked) {
            sanctuaryTimer += delta;
            if (sanctuaryTimer >= SANCTUARY_DECAY_TIME && wantedSystem != null
                    && wantedSystem.getWantedStars() > 0) {
                wantedSystem.setWantedStarsForTesting(
                        Math.max(0, wantedSystem.getWantedStars() - 1));
                sanctuaryTimer = 0f;
                if (achievementCallback != null) {
                    achievementCallback.award(AchievementType.SANCTUARY_SEEKER);
                }
            }
        } else if (!playerInsideMosque) {
            sanctuaryTimer = 0f;
        }

        // Jumu'ah attendance timer
        if (playerInsideMosque && shoesRemoved && isJumuahActive(dayCount, hour)
                && !jumuahCountedThisWeek) {
            jumuahAttendanceTimer += delta;
            // Award after attending for at least 30 real seconds (representing the full hour)
            if (jumuahAttendanceTimer >= 30.0f) {
                recordJumuahAttendance(dayCount, notorietySystem, factionSystem, achievementCallback);
            }
        }

        // Reset Jumu'ah counting when day changes
        if (dayCount != lastJumuahDayAttended && dayCount % 7 != FRIDAY) {
            jumuahCountedThisWeek = false;
            jumuahAttendanceTimer = 0f;
        }
    }

    /**
     * Manage worshipper NPC spawning and despawning.
     */
    private void manageWorshippers(int dayCount, float hour, boolean prayerActive, List<NPC> npcs) {
        if (prayerActive) {
            int targetCount = getWorshipperCount(dayCount, hour);
            while (worshipperNpcs.size() < targetCount) {
                NPC worshipper = new NPC(NPCType.WORSHIPPER,
                        mosqueX + 1f + random.nextFloat() * (mosqueWidth - 2f),
                        0f,
                        mosqueZ + 1f + random.nextFloat() * (mosqueDepth - 2f));
                worshipper.setState(NPCState.IDLE);
                worshipperNpcs.add(worshipper);
                npcs.add(worshipper);
            }
        } else {
            if (!worshipperNpcs.isEmpty()) {
                npcs.removeAll(worshipperNpcs);
                worshipperNpcs.clear();
            }
        }
    }

    /**
     * Manage the IMAM NPC presence during open hours.
     */
    private void manageImam(float hour, List<NPC> npcs) {
        boolean imamHours = hour >= 8.0f && hour < 21.0f;
        if (imamHours && !imamSpawned) {
            imamNpc = new NPC(NPCType.IMAM, "Hassan",
                    mosqueX + mosqueWidth / 2f, 0f, mosqueZ + mosqueDepth - 3f);
            imamNpc.setState(NPCState.IDLE);
            imamSpawned = true;
            npcs.add(imamNpc);
        } else if (!imamHours && imamSpawned) {
            if (imamNpc != null) {
                npcs.remove(imamNpc);
            }
            imamNpc = null;
            imamSpawned = false;
        }
    }

    /**
     * Record a Jumu'ah attendance for the player.
     */
    private void recordJumuahAttendance(int dayCount, NotorietySystem notorietySystem,
                                         FactionSystem factionSystem,
                                         AchievementCallback achievementCallback) {
        if (jumuahCountedThisWeek) return;
        jumuahCountedThisWeek = true;
        lastJumuahDayAttended = dayCount;
        jumuahAttendanceCount++;

        if (achievementCallback != null) {
            // Progress toward JUMU_AH_REGULAR (3×)
            achievementCallback.award(AchievementType.JUMU_AH_REGULAR);

            // THE_COUNCIL Respect: +1 per 3 donations/attendances
            if (factionSystem != null && jumuahAttendanceCount % 3 == 0) {
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL, COUNCIL_RESPECT_PER_THREE_DONATIONS);
            }
        }
    }

    // ── Player actions ─────────────────────────────────────────────────────────

    /**
     * Player enters the mosque bounds.
     */
    public void onPlayerEnterMosque(float playerX, float playerZ) {
        if (isInsideMosque(playerX, playerZ)) {
            playerInsideMosque = true;
            sanctuaryTimer = 0f;
        }
    }

    /**
     * Player leaves the mosque.
     */
    public void onPlayerLeaveMosque() {
        playerInsideMosque = false;
        sanctuaryTimer = 0f;
        shoesRemoved = false;
    }

    /**
     * Player interacts with the SHOE_RACK_PROP (press E) to remove/replace shoes.
     *
     * @return true if shoes were toggled
     */
    public boolean interactWithShoeRack() {
        shoesRemoved = !shoesRemoved;
        return true;
    }

    /**
     * Player uses the OUTDOOR_TAP_PROP. Always accessible; grants +10 Thirst.
     *
     * @param inventory  the player's inventory (thirst tracked externally; here we
     *                   return the thirst delta for the calling system to apply)
     * @return thirst amount restored (always 10)
     */
    public int useOutdoorTap() {
        return 10;
    }

    /**
     * Player uses the ABLUTION_TAP_PROP (wudu room).
     * Grants +3 Warmth and removes DIRTY status (handled externally).
     *
     * @return warmth amount restored (always 3)
     */
    public int useAblutionTap() {
        return 3;
    }

    /**
     * Player presses E on the FOLD_TABLE_PROP during Iftar.
     * Gives DATE_FRUIT×3, FLATBREAD×1, SOUP_CUP×1 once per session.
     * Seeds IFTAR_TONIGHT rumour to nearby NPCs.
     *
     * @param timeSystem        the TimeSystem
     * @param inventory         the player's inventory
     * @param rumourNetwork     the RumourNetwork (null-safe)
     * @param nearbyNpc         an NPC near the table to carry the rumour (null-safe)
     * @param achievementCallback callback for achievements
     * @return true if food was given
     */
    public boolean collectIftarFood(TimeSystem timeSystem, Inventory inventory,
                                    RumourNetwork rumourNetwork, NPC nearbyNpc,
                                    AchievementCallback achievementCallback) {
        int dayCount = timeSystem.getDayCount();
        float hour = timeSystem.getTime();

        if (!isIftarTableActive(dayCount, hour)) return false;
        if (iftarCollectedThisSession) return false;
        if (inventory == null) return false;

        inventory.addItem(Material.DATE_FRUIT, IFTAR_DATE_FRUIT_COUNT);
        inventory.addItem(Material.FLATBREAD, 1);
        inventory.addItem(Material.SOUP_CUP, 1);
        iftarCollectedThisSession = true;

        // Seed IFTAR_TONIGHT rumour
        if (rumourNetwork != null && nearbyNpc != null) {
            rumourNetwork.addRumour(nearbyNpc,
                    new Rumour(RumourType.IFTAR_TONIGHT,
                            "The mosque is doing an Iftar tonight — free food for everyone, come along."));
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.IFTAR_GUEST);
        }

        return true;
    }

    /**
     * Player donates COIN to the TAKINGS_BOX_PROP.
     * +5 Community Respect per 2 COIN donated (handled by FactionSystem).
     *
     * @param coins               number of COIN to donate
     * @param inventory           the player's inventory
     * @param factionSystem       the FactionSystem (null-safe)
     * @param achievementCallback callback for achievements
     * @return true if donation succeeded
     */
    public boolean donateToTakingsBox(int coins, Inventory inventory,
                                       FactionSystem factionSystem,
                                       AchievementCallback achievementCallback) {
        if (inventory == null || inventory.getItemCount(Material.COIN) < coins) return false;
        if (coins <= 0) return false;

        inventory.removeItem(Material.COIN, coins);
        donationCount++;

        // +5 Community Respect per 2 COIN donated (via THE_COUNCIL as "community" faction)
        if (factionSystem != null) {
            int respectGain = (coins / 2) * DONATION_RESPECT_PER_TWO_COIN;
            if (respectGain > 0) {
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL, respectGain);
            }

            // THE_COUNCIL +1 per 3 donations
            if (donationCount % 3 == 0) {
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL, COUNCIL_RESPECT_PER_THREE_DONATIONS);
            }
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.COMMUNITY_PILLAR);
        }

        return true;
    }

    /**
     * Player robs the TAKINGS_BOX_PROP (destroys it).
     * Yields 5–15 COIN; triggers COMMUNITY_OUTRAGE rumour (+3 Notoriety,
     * −20 Community Respect, THEFT_FROM_PLACE_OF_WORSHIP, sanctuary revoked).
     *
     * @param inventory           the player's inventory
     * @param notorietySystem     the NotorietySystem (null-safe)
     * @param wantedSystem        the WantedSystem (null-safe)
     * @param criminalRecord      the CriminalRecord (null-safe)
     * @param rumourNetwork       the RumourNetwork (null-safe)
     * @param nearbyNpc           a nearby NPC to carry the COMMUNITY_OUTRAGE rumour (null-safe)
     * @param factionSystem       the FactionSystem (null-safe)
     * @param playerX             player world X
     * @param playerY             player world Y
     * @param playerZ             player world Z
     * @param achievementCallback callback for achievements
     * @return coin amount stolen
     */
    public int robTakingsBox(Inventory inventory, NotorietySystem notorietySystem,
                              WantedSystem wantedSystem, CriminalRecord criminalRecord,
                              RumourNetwork rumourNetwork, NPC nearbyNpc,
                              FactionSystem factionSystem,
                              float playerX, float playerY, float playerZ,
                              AchievementCallback achievementCallback) {
        int stolen = 5 + random.nextInt(11); // 5–15
        if (inventory != null) {
            inventory.addItem(Material.COIN, stolen);
        }

        // Permanently revoke sanctuary
        sanctuaryRevoked = true;

        // +3 Notoriety
        if (notorietySystem != null) {
            notorietySystem.addNotoriety(TAKINGS_BOX_NOTORIETY,
                    achievementCallback != null ? type -> achievementCallback.award(type) : null);
        }

        // −20 Community Respect (via THE_COUNCIL)
        if (factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -TAKINGS_BOX_RESPECT_PENALTY);
        }

        // Record crime
        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT_FROM_PLACE_OF_WORSHIP);
        }

        // Add Wanted stars
        if (wantedSystem != null) {
            wantedSystem.addWantedStars(2, playerX, playerY, playerZ,
                    achievementCallback != null ? type -> achievementCallback.award(type) : null);
        }

        // Seed COMMUNITY_OUTRAGE rumour within 50 blocks
        if (rumourNetwork != null && nearbyNpc != null) {
            rumourNetwork.addRumour(nearbyNpc,
                    new Rumour(RumourType.COMMUNITY_OUTRAGE,
                            "Someone robbed the mosque collection box. Absolute disgrace."));
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.LOWEST_OF_THE_LOW);
        }

        return stolen;
    }

    /**
     * Player makes noise during prayer (noise level ≥ 3).
     * First offence: gentle NPC remonstration (returned as a message).
     * Second+ offence: −10 Community Respect + ejection flag.
     *
     * @param noiseLevel        current noise level (0–1 scale; 0.3 = threshold)
     * @param factionSystem     the FactionSystem (null-safe)
     * @return a remonstration message, or null if noise is below threshold
     */
    public String onNoiseInMosque(float noiseLevel, FactionSystem factionSystem) {
        if (noiseLevel < NOISE_REMONSTRATION_THRESHOLD) return null;

        noiseOffenceCount++;
        if (noiseOffenceCount == 1) {
            return "Please — we're in prayer. Could you keep it down?";
        } else {
            // Repeat offence: −10 Community Respect
            if (factionSystem != null) {
                factionSystem.applyRespectDelta(Faction.THE_COUNCIL, -NOISE_REPEAT_RESPECT_PENALTY);
            }
            return "I'm going to have to ask you to leave.";
        }
    }

    /**
     * Player reads the NOTICEBOARD_PROP inside the mosque.
     * Awards +5 STREETWISE XP.
     *
     * @param streetSkillSystem  the StreetSkillSystem (null-safe)
     */
    public void readNoticeboard(StreetSkillSystem streetSkillSystem) {
        if (streetSkillSystem != null) {
            streetSkillSystem.awardXP(StreetSkillSystem.Skill.STREETWISE, 5);
        }
    }

    /**
     * Player interacts with the Imam for community rumours or food.
     * Returns a NEIGHBOURHOOD rumour text if available, or offers FLATBREAD if hungry.
     *
     * @param rumourNetwork  the RumourNetwork (null-safe)
     * @param npcs           all living NPCs
     * @param hungry         true if player hunger &lt; 30
     * @param inventory      the player's inventory (null-safe)
     * @param lastImamFoodDay last day Imam provided food (for once-per-day check)
     * @param currentDay     the current day count
     * @return a message string, or null if nothing to offer
     */
    public String interactWithImam(RumourNetwork rumourNetwork, List<NPC> npcs,
                                    boolean hungry, Inventory inventory,
                                    int lastImamFoodDay, int currentDay) {
        if (imamNpc == null) return null;

        // Offer NEIGHBOURHOOD rumour
        if (rumourNetwork != null && npcs != null) {
            for (NPC npc : npcs) {
                if (!npc.isAlive()) continue;
                for (Rumour r : npc.getRumours()) {
                    if (r.getType() == RumourType.NEIGHBOURHOOD) {
                        return r.getText();
                    }
                }
            }
        }

        // Offer FLATBREAD if hungry (once per day)
        if (hungry && inventory != null && lastImamFoodDay != currentDay) {
            inventory.addItem(Material.FLATBREAD, 1);
            return "Here, take this — you look like you could use it.";
        }

        return "Salaam alaikum. You're always welcome here, friend.";
    }

    // ── Getters for state ──────────────────────────────────────────────────────

    public List<NPC> getWorshipperNpcs() {
        return worshipperNpcs;
    }

    public NPC getImamNpc() {
        return imamNpc;
    }

    public boolean isPlayerInsideMosque() {
        return playerInsideMosque;
    }

    public void setPlayerInsideMosque(boolean inside) {
        this.playerInsideMosque = inside;
        if (!inside) sanctuaryTimer = 0f;
    }

    public boolean isShoesRemoved() {
        return shoesRemoved;
    }

    public void setShoesRemovedForTesting(boolean removed) {
        this.shoesRemoved = removed;
    }

    public float getSanctuaryTimer() {
        return sanctuaryTimer;
    }

    public void setSanctuaryTimerForTesting(float t) {
        this.sanctuaryTimer = t;
    }

    public boolean isSanctuaryRevoked() {
        return sanctuaryRevoked;
    }

    public boolean isIftarCollectedThisSession() {
        return iftarCollectedThisSession;
    }

    public int getJumuahAttendanceCount() {
        return jumuahAttendanceCount;
    }

    public int getDonationCount() {
        return donationCount;
    }

    public boolean isImamSpawned() {
        return imamSpawned;
    }

    public void setRamadanStartDayForTesting(int day) {
        this.ramadanStartDay = day;
    }

    public int getNoiseOffenceCount() {
        return noiseOffenceCount;
    }

    public void resetNoiseOffencesForTesting() {
        this.noiseOffenceCount = 0;
    }
}
