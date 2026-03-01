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
 * Issue #1008: St. Mary's Church — Sunday Services, Jumble Sales, Sanctuary &amp; Bell Tower.
 *
 * <p>Brings the {@code CHURCH} landmark to life with a full service schedule, congregation
 * spawning, sanctuary mechanic, bell tower, soup kitchen, and jumble sale.
 *
 * <h3>Opening Hours &amp; Services</h3>
 * <ul>
 *   <li>Doors open 08:00–20:00 daily.</li>
 *   <li>Sunday service: 10:00–11:30 (4–10 PENSIONER congregation + VICAR at PULPIT_PROP).</li>
 *   <li>Wednesday service: 19:00–20:00 (2–5 PENSIONER congregation).</li>
 *   <li>CONFESSION_BOOTH_PROP occupied during Wednesday service — press E to eavesdrop rumour.</li>
 * </ul>
 *
 * <h3>Events</h3>
 * <ul>
 *   <li>Soup Kitchen: Mon/Thu 12:00–14:00 (free SOUP_CUP, once per day).</li>
 *   <li>Monthly Jumble Sale: first Saturday of month 10:00–13:00 (3–5 PENSIONER stalls).</li>
 *   <li>Collection plate: circulated 10:30–11:00 Sundays. Donate or steal.</li>
 * </ul>
 *
 * <h3>Sanctuary</h3>
 * POLICE won't enter during active service. Outside service hours they hesitate 10s.
 * Wanted level decays −1 star after 3 in-game minutes inside.
 *
 * <h3>Bell Tower</h3>
 * BELL_ROPE_PROP causes NoiseSystem level-5 event. Night ringing (23:00–06:00): criminal offence.
 */
public class ChurchSystem {

    // ── Day-of-week constants (dayCount % 7) ─────────────────────────────────
    // Based on project convention: day 1 = first game day, game starts ~Wednesday
    // Game start = day 1, dayCount % 7: 0=Mon,1=Tue,2=Wed,3=Thu,4=Fri,5=Sat,6=Sun
    // (PropertySystem: +2 so day 1 maps to Wed=2, meaning day%7==2 is Wed)
    // We use: dayCount % 7: Sunday = 6
    private static final int SUNDAY    = 6;
    private static final int WEDNESDAY = 2;
    private static final int MONDAY    = 0;
    private static final int THURSDAY  = 3;
    private static final int SATURDAY  = 5;

    // ── Service time constants ────────────────────────────────────────────────

    /** Sunday service start hour. */
    public static final float SUNDAY_SERVICE_START = 10.0f;
    /** Sunday service end hour. */
    public static final float SUNDAY_SERVICE_END   = 11.5f;  // 11:30

    /** Wednesday service start hour. */
    public static final float WEDNESDAY_SERVICE_START = 19.0f;
    /** Wednesday service end hour. */
    public static final float WEDNESDAY_SERVICE_END   = 20.0f;

    // ── Collection plate time constants ──────────────────────────────────────

    /** Collection plate circulation start (Sunday only). */
    public static final float COLLECTION_START = 10.5f;  // 10:30
    /** Collection plate circulation end. */
    public static final float COLLECTION_END   = 11.0f;  // 11:00

    // ── Soup kitchen constants ────────────────────────────────────────────────

    /** Soup kitchen start hour. */
    public static final float SOUP_KITCHEN_START = 12.0f;
    /** Soup kitchen end hour. */
    public static final float SOUP_KITCHEN_END   = 14.0f;

    // ── Jumble sale constants ─────────────────────────────────────────────────

    /** Jumble sale start hour. */
    public static final float JUMBLE_SALE_START = 10.0f;
    /** Jumble sale end hour. */
    public static final float JUMBLE_SALE_END   = 13.0f;

    // ── Bell tower constants ──────────────────────────────────────────────────

    /** Noise level emitted by bell ringing (0–1 scale, 1.0 = max). */
    public static final float BELL_NOISE_LEVEL = 1.0f;

    /** Range (blocks) within which NPCs respond to bell ringing. */
    public static final float BELL_INVESTIGATING_RANGE = 40.0f;

    /** Duration (seconds) NPCs remain in INVESTIGATING state after bell ring. */
    public static final float BELL_INVESTIGATING_DURATION = 10.0f;

    /** FOV bonus (degrees) granted to player on bell tower summit. */
    public static final float BELL_TOWER_FOV_BONUS = 15.0f;

    /** Duration (seconds) the FOV bonus lasts. */
    public static final float BELL_TOWER_FOV_DURATION = 60.0f;

    // ── Blessing constants ────────────────────────────────────────────────────

    /** Notoriety reduction from receiving a blessing. */
    public static final int BLESSING_NOTORIETY_REDUCTION = 2;

    // ── Collection plate constants ────────────────────────────────────────────

    /** Notoriety gain from stealing the collection plate. */
    public static final int COLLECTION_THEFT_NOTORIETY = 10;

    /** Max Notoriety reduction from donating to collection plate. */
    public static final int COLLECTION_DONATION_MAX_REDUCTION = 3;

    // ── Sanctuary constants ───────────────────────────────────────────────────

    /** Seconds inside church required to lose one wanted star (sanctuary decay). */
    public static final float SANCTUARY_DECAY_TIME = 180.0f; // 3 in-game minutes

    /** Hesitation time (seconds) before police enter outside service hours. */
    public static final float POLICE_HESITATION_TIME = 10.0f;

    // ── Night bell ringing constants ──────────────────────────────────────────

    /** Night bell ringing notoriety penalty. */
    public static final int NIGHT_BELL_NOTORIETY = 5;

    // ── Warmth constants ──────────────────────────────────────────────────────

    /** Warmth restored per second while inside the church (3/min = 0.05/s). */
    public static final float CHURCH_WARMTH_RATE = 3.0f / 60.0f;  // 3 per minute

    // ── Jumble sale constants ─────────────────────────────────────────────────

    /** Items available at the jumble sale. */
    public static final Material[] JUMBLE_SALE_ITEMS = {
        Material.CANDLE, Material.TABLECLOTH, Material.KNITTING_NEEDLES
    };

    // ── FactionSystem constants ───────────────────────────────────────────────

    /** Respect bonus from STREET_LADS after attending 5+ services (no LOCALS faction). */
    public static final int SERVICE_FACTION_RESPECT_BONUS = 3;

    /** Number of services required to gain faction respect. */
    public static final int SERVICE_FACTION_THRESHOLD = 5;

    // ── StreetSkillSystem constants ───────────────────────────────────────────

    /** Max INFLUENCE XP gain from attending church services. */
    public static final int MAX_SOCIAL_SKILL_GAIN_FROM_CHURCH = 5;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Active congregation NPCs (spawned during service, despawned after). */
    private final List<NPC> congregationNpcs = new ArrayList<>();

    /** Whether a jumble sale stall NPC list is active. */
    private final List<NPC> jumbleSaleNpcs = new ArrayList<>();

    /** The VICAR NPC (Reverend Dave). */
    private NPC vicarNpc = null;

    /** Whether the player is currently inside the church. */
    private boolean playerInsideChurch = false;

    /** Timer tracking how long the player has been inside the church (for sanctuary decay). */
    private float sanctuaryTimer = 0f;

    /** Whether the player has collected soup today. */
    private boolean soupCollectedToday = false;

    /** Last day index when soup was collected (to reset once-per-day). */
    private int lastSoupDay = -1;

    /** Number of Sunday services attended (for REGULAR_PARISHIONER achievement). */
    private int sundayServicesAttended = 0;

    /** Last service day attended (to avoid double-counting). */
    private int lastServiceDayAttended = -1;

    /** Whether REGULAR_PARISHIONER achievement has been awarded. */
    private boolean regularParishionerAwarded = false;

    /** Whether faction respect bonus from 5 services has been given. */
    private boolean factionBonusGiven = false;

    /** Number of INFLUENCE XP points awarded from church attendance so far. */
    private int socialSkillGainedFromChurch = 0;

    /** Whether the VICAR has been spawned this session. */
    private boolean vicarSpawned = false;

    /** Church position (set externally; used for sanctuary checks). */
    private float churchX = 30f;
    private float churchZ = 100f;
    private float churchWidth  = 12f;
    private float churchDepth  = 18f;

    /** Whether the bell has been rung (for BELL_RINGER achievement). */
    private boolean bellRingerAwarded = false;

    private final Random random;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /**
     * Callback for awarding achievements.
     */
    public interface AchievementCallback {
        void award(AchievementType type);
    }

    // ── Construction ──────────────────────────────────────────────────────────

    public ChurchSystem() {
        this(new Random());
    }

    public ChurchSystem(Random random) {
        this.random = random;
    }

    // ── Church bounds ──────────────────────────────────────────────────────────

    /**
     * Set the world position and size of the church (for inside/outside checks).
     */
    public void setChurchBounds(float x, float z, float width, float depth) {
        this.churchX = x;
        this.churchZ = z;
        this.churchWidth = width;
        this.churchDepth = depth;
    }

    /**
     * Check whether a world position is inside the church bounds.
     */
    public boolean isInsideChurch(float x, float z) {
        return x >= churchX && x <= churchX + churchWidth
            && z >= churchZ && z <= churchZ + churchDepth;
    }

    // ── Service schedule queries ───────────────────────────────────────────────

    /**
     * Returns true if a church service is currently active.
     *
     * @param dayCount  the current day count from TimeSystem
     * @param hour      the current hour (0–24) from TimeSystem
     * @return true during Sunday 10:00–11:30 and Wednesday 19:00–20:00
     */
    public boolean isServiceActive(int dayCount, float hour) {
        int dow = dayCount % 7;
        if (dow == SUNDAY) {
            return hour >= SUNDAY_SERVICE_START && hour < SUNDAY_SERVICE_END;
        }
        if (dow == WEDNESDAY) {
            return hour >= WEDNESDAY_SERVICE_START && hour < WEDNESDAY_SERVICE_END;
        }
        return false;
    }

    /**
     * Returns true if the soup kitchen is currently active.
     *
     * @param dayCount  the current day count from TimeSystem
     * @param hour      the current hour (0–24) from TimeSystem
     * @return true on Monday/Thursday 12:00–14:00
     */
    public boolean isSoupKitchenActive(int dayCount, float hour) {
        int dow = dayCount % 7;
        if (dow == MONDAY || dow == THURSDAY) {
            return hour >= SOUP_KITCHEN_START && hour < SOUP_KITCHEN_END;
        }
        return false;
    }

    /**
     * Returns true if the jumble sale is active today.
     *
     * <p>The jumble sale runs on the first Saturday of each in-game month.
     *
     * @param dayOfMonth  the current day-of-month (1-based) from TimeSystem
     * @param dayCount    the current day count from TimeSystem
     * @param hour        the current hour (0–24) from TimeSystem
     * @return true on the first Saturday of the month between 10:00–13:00
     */
    public boolean isJumbleSaleActive(int dayOfMonth, int dayCount, float hour) {
        int dow = dayCount % 7;
        if (dow != SATURDAY) return false;
        // First Saturday: day-of-month in range 1–7
        if (dayOfMonth < 1 || dayOfMonth > 7) return false;
        return hour >= JUMBLE_SALE_START && hour < JUMBLE_SALE_END;
    }

    /**
     * Returns true if the collection plate is being circulated.
     *
     * @param dayCount  the current day count from TimeSystem
     * @param hour      the current hour (0–24)
     */
    public boolean isCollectionPlateActive(int dayCount, float hour) {
        return dayCount % 7 == SUNDAY
            && hour >= COLLECTION_START && hour < COLLECTION_END;
    }

    /**
     * Returns true if sanctuary is active (service in progress and no police inside).
     *
     * @param serviceActive   whether a service is currently active
     * @param policeInside    whether any POLICE NPC is inside the church
     */
    public boolean isSanctuaryActive(boolean serviceActive, boolean policeInside) {
        return serviceActive && !policeInside;
    }

    /**
     * Returns the notoriety reduction from receiving a blessing.
     */
    public int getBlessingNotorietyReduction() {
        return BLESSING_NOTORIETY_REDUCTION;
    }

    /**
     * Returns the number of congregants expected at the current time.
     *
     * @param dayCount the current day count
     * @param hour     the current hour
     * @return 4–10 on Sunday service, 2–5 on Wednesday service, 0 otherwise
     */
    public int getCongregantCount(int dayCount, float hour) {
        int dow = dayCount % 7;
        if (dow == SUNDAY && hour >= SUNDAY_SERVICE_START && hour < SUNDAY_SERVICE_END) {
            return 4 + random.nextInt(7); // 4–10
        }
        if (dow == WEDNESDAY && hour >= WEDNESDAY_SERVICE_START && hour < WEDNESDAY_SERVICE_END) {
            return 2 + random.nextInt(4); // 2–5
        }
        return 0;
    }

    /**
     * Returns true if ringing the bell at the given hour is a criminal offence.
     *
     * @param hour the current hour (0–24)
     * @return true between 23:00–06:00
     */
    public boolean isBellRingingCriminal(float hour) {
        return hour < 6.0f || hour >= 23.0f;
    }

    /**
     * Returns the sanctuary wanted-level decay time in seconds.
     */
    public float computeSanctuaryDecayTime() {
        return SANCTUARY_DECAY_TIME;
    }

    /**
     * Compute the Notoriety reduction from a collection plate donation.
     *
     * @param coinsdonated  number of COIN donated (1 per COIN, max 3)
     * @return Notoriety reduction (0 for 0 coins, –1 per coin, capped at –3)
     */
    public int computeCollectionDonationBonus(int coinsdonated) {
        if (coinsdonated <= 0) return 0;
        return Math.min(coinsdonated, COLLECTION_DONATION_MAX_REDUCTION);
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    /**
     * Update the church system each frame.
     *
     * @param delta             seconds since last frame
     * @param timeSystem        the TimeSystem
     * @param npcs              all living NPCs (for congregation management)
     * @param notorietySystem   the NotorietySystem (sanctuary warmth/decay uses)
     * @param wantedSystem      the WantedSystem
     * @param factionSystem     the FactionSystem
     * @param streetSkillSystem the StreetSkillSystem
     * @param achievementCallback callback for awarding achievements
     */
    public void update(float delta, TimeSystem timeSystem, List<NPC> npcs,
                       NotorietySystem notorietySystem, WantedSystem wantedSystem,
                       FactionSystem factionSystem, StreetSkillSystem streetSkillSystem,
                       AchievementCallback achievementCallback) {
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();

        // Reset soup kitchen daily flag
        int today = dayCount;
        if (today != lastSoupDay) {
            soupCollectedToday = false;
        }

        boolean serviceActive = isServiceActive(dayCount, hour);

        // Spawn / despawn congregation
        manageCongregation(dayCount, hour, serviceActive, npcs);

        // Spawn VICAR during open hours (09:00–19:00)
        manageVicar(hour, npcs);

        // Manage jumble sale NPCs
        manageJumbleSale(timeSystem.getDayOfMonth(), dayCount, hour, npcs);

        // Sanctuary decay
        if (playerInsideChurch) {
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
        } else {
            sanctuaryTimer = 0f;
        }
    }

    /**
     * Manage congregation NPC spawning and despawning.
     */
    private void manageCongregation(int dayCount, float hour, boolean serviceActive, List<NPC> npcs) {
        if (serviceActive) {
            // Ensure congregation is populated
            int targetCount = getCongregantCount(dayCount, hour);
            while (congregationNpcs.size() < targetCount) {
                NPC pensioner = new NPC(NPCType.PENSIONER,
                        churchX + 2f + random.nextFloat() * (churchWidth - 4f),
                        0f,
                        churchZ + 2f + random.nextFloat() * (churchDepth - 4f));
                pensioner.setState(NPCState.IDLE);
                congregationNpcs.add(pensioner);
                npcs.add(pensioner);
            }
        } else {
            // Despawn congregation after service
            if (!congregationNpcs.isEmpty()) {
                npcs.removeAll(congregationNpcs);
                congregationNpcs.clear();
            }
        }
    }

    /**
     * Manage the VICAR NPC presence during open hours.
     */
    private void manageVicar(float hour, List<NPC> npcs) {
        boolean vicarHours = hour >= 9.0f && hour < 19.0f;
        if (vicarHours && !vicarSpawned) {
            vicarNpc = new NPC(NPCType.VICAR, "Reverend Dave",
                    churchX + churchWidth / 2f, 0f, churchZ + churchDepth - 3f);
            vicarNpc.setState(NPCState.IDLE);
            vicarSpawned = true;
            npcs.add(vicarNpc);
        } else if (!vicarHours && vicarSpawned) {
            if (vicarNpc != null) {
                npcs.remove(vicarNpc);
            }
            vicarNpc = null;
            vicarSpawned = false;
        }
    }

    /**
     * Manage jumble sale NPC spawning and despawning.
     */
    private void manageJumbleSale(int dayOfMonth, int dayCount, float hour, List<NPC> npcs) {
        boolean jumbleActive = isJumbleSaleActive(dayOfMonth, dayCount, hour);
        if (jumbleActive) {
            if (jumbleSaleNpcs.isEmpty()) {
                int stallCount = 3 + random.nextInt(3); // 3–5
                for (int i = 0; i < stallCount; i++) {
                    NPC staller = new NPC(NPCType.PENSIONER,
                            churchX + 1f + i * 2f,
                            0f,
                            churchZ + 1f);
                    staller.setState(NPCState.IDLE);
                    jumbleSaleNpcs.add(staller);
                    npcs.add(staller);
                }
            }
        } else {
            if (!jumbleSaleNpcs.isEmpty()) {
                npcs.removeAll(jumbleSaleNpcs);
                jumbleSaleNpcs.clear();
            }
        }
    }

    // ── Player actions ─────────────────────────────────────────────────────────

    /**
     * Player enters the church.
     *
     * @param playerX  player's world X
     * @param playerZ  player's world Z
     */
    public void onPlayerEnterChurch(float playerX, float playerZ) {
        if (isInsideChurch(playerX, playerZ)) {
            playerInsideChurch = true;
            sanctuaryTimer = 0f;
        }
    }

    /**
     * Player leaves the church.
     */
    public void onPlayerLeaveChurch() {
        playerInsideChurch = false;
        sanctuaryTimer = 0f;
    }

    /**
     * Player presses E on Reverend Dave at the PULPIT_PROP during service.
     * Awards a blessing: Notoriety −2.
     *
     * @param timeSystem          the TimeSystem
     * @param notorietySystem     the NotorietySystem
     * @param streetSkillSystem   the StreetSkillSystem (null safe)
     * @param factionSystem       the FactionSystem (null safe)
     * @param achievementCallback callback for achievements
     * @return true if blessing was given
     */
    public boolean pressBlessingAtPulpit(TimeSystem timeSystem,
                                          NotorietySystem notorietySystem,
                                          StreetSkillSystem streetSkillSystem,
                                          FactionSystem factionSystem,
                                          AchievementCallback achievementCallback) {
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();

        if (!isServiceActive(dayCount, hour)) return false;

        // Reduce notoriety
        if (notorietySystem != null) {
            notorietySystem.reduceNotoriety(BLESSING_NOTORIETY_REDUCTION,
                    achievementCallback != null
                            ? type -> achievementCallback.award(type) : null);
        }

        // Award BLESS_YOU achievement on first blessing
        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.BLESS_YOU);
        }

        // Track Sunday service attendance
        if (dayCount % 7 == SUNDAY && dayCount != lastServiceDayAttended) {
            lastServiceDayAttended = dayCount;
            sundayServicesAttended++;

            // REGULAR_PARISHIONER: attend 5 Sunday services
            if (achievementCallback != null) {
                for (int i = 0; i < 1; i++) { // increment achievement progress
                    achievementCallback.award(AchievementType.REGULAR_PARISHIONER);
                }
            }

            // SOCIAL (INFLUENCE) skill +1 per unique Sunday service (max 5)
            if (streetSkillSystem != null && socialSkillGainedFromChurch < MAX_SOCIAL_SKILL_GAIN_FROM_CHURCH) {
                streetSkillSystem.awardXP(StreetSkillSystem.Skill.INFLUENCE, 50);
                socialSkillGainedFromChurch++;
            }

            // FactionSystem: 5+ services → +3 STREET_LADS respect
            if (factionSystem != null && sundayServicesAttended >= SERVICE_FACTION_THRESHOLD
                    && !factionBonusGiven) {
                factionSystem.applyRespectDelta(Faction.STREET_LADS, SERVICE_FACTION_RESPECT_BONUS);
                factionBonusGiven = true;
            }
        }

        return true;
    }

    /**
     * Player donates to the collection plate.
     *
     * @param coins               number of COIN to donate (1–5)
     * @param inventory           player's inventory
     * @param notorietySystem     the NotorietySystem
     * @param achievementCallback callback for achievements
     * @return true if donation succeeded
     */
    public boolean donateToCollectionPlate(int coins, Inventory inventory,
                                            NotorietySystem notorietySystem,
                                            AchievementCallback achievementCallback) {
        if (inventory == null || inventory.getItemCount(Material.COIN) < coins) return false;
        inventory.removeItem(Material.COIN, coins);

        int reduction = computeCollectionDonationBonus(coins);
        if (notorietySystem != null && reduction > 0) {
            notorietySystem.reduceNotoriety(reduction,
                    achievementCallback != null ? type -> achievementCallback.award(type) : null);
        }
        return true;
    }

    /**
     * Player steals the collection plate.
     *
     * @param inventory           player's inventory
     * @param notorietySystem     the NotorietySystem
     * @param wantedSystem        the WantedSystem
     * @param criminalRecord      the CriminalRecord
     * @param witnessed           whether a PENSIONER NPC witnessed the theft
     * @param playerX             player world X (for wanted LKP)
     * @param playerY             player world Y
     * @param playerZ             player world Z
     * @param achievementCallback callback for achievements
     * @return coin amount stolen
     */
    public int stealCollectionPlate(Inventory inventory, NotorietySystem notorietySystem,
                                     WantedSystem wantedSystem, CriminalRecord criminalRecord,
                                     boolean witnessed,
                                     float playerX, float playerY, float playerZ,
                                     AchievementCallback achievementCallback) {
        // Congregation donations: roughly 1 coin per congregant (3–8 coins total)
        int stolen = 3 + random.nextInt(6);
        if (inventory != null) {
            inventory.addItem(Material.COIN, stolen);
        }

        if (notorietySystem != null) {
            notorietySystem.addNotoriety(COLLECTION_THEFT_NOTORIETY,
                    achievementCallback != null ? type -> achievementCallback.award(type) : null);
        }

        if (criminalRecord != null) {
            criminalRecord.record(CriminalRecord.CrimeType.THEFT);
        }

        if (witnessed && wantedSystem != null) {
            wantedSystem.addWantedStars(1, playerX, playerY, playerZ,
                    achievementCallback != null ? type -> achievementCallback.award(type) : null);
        }

        if (achievementCallback != null) {
            achievementCallback.award(AchievementType.COLLECTION_THIEF);
        }

        return stolen;
    }

    /**
     * Player rings the bell (press E on BELL_ROPE_PROP).
     *
     * @param timeSystem          the TimeSystem
     * @param noiseSystem         the NoiseSystem (null safe)
     * @param notorietySystem     the NotorietySystem
     * @param wantedSystem        the WantedSystem
     * @param criminalRecord      the CriminalRecord
     * @param npcs                all living NPCs (for INVESTIGATING state)
     * @param playerX             player world X
     * @param playerY             player world Y
     * @param playerZ             player world Z
     * @param achievementCallback callback for achievements
     */
    public void ringBell(TimeSystem timeSystem, NoiseSystem noiseSystem,
                          NotorietySystem notorietySystem, WantedSystem wantedSystem,
                          CriminalRecord criminalRecord, List<NPC> npcs,
                          float playerX, float playerY, float playerZ,
                          AchievementCallback achievementCallback) {
        float hour = timeSystem.getTime();

        // Emit maximum noise
        if (noiseSystem != null) {
            noiseSystem.setNoiseLevel(BELL_NOISE_LEVEL);
        }

        // All NPCs within 40 blocks → INVESTIGATING
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            float dx = npc.getPosition().x - playerX;
            float dz = npc.getPosition().z - playerZ;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist <= BELL_INVESTIGATING_RANGE) {
                npc.setState(NPCState.INVESTIGATING);
            }
        }

        // Criminal offence if night
        if (isBellRingingCriminal(hour)) {
            if (notorietySystem != null) {
                notorietySystem.addNotoriety(NIGHT_BELL_NOTORIETY,
                        achievementCallback != null ? type -> achievementCallback.award(type) : null);
            }
            if (criminalRecord != null) {
                criminalRecord.record(CriminalRecord.CrimeType.ANTISOCIAL_BEHAVIOUR);
            }
            if (wantedSystem != null) {
                wantedSystem.addWantedStars(1, playerX, playerY, playerZ,
                        achievementCallback != null ? type -> achievementCallback.award(type) : null);
            }
        }

        // Award BELL_RINGER achievement first time
        if (achievementCallback != null && !bellRingerAwarded) {
            bellRingerAwarded = true;
            achievementCallback.award(AchievementType.BELL_RINGER);
        }
    }

    /**
     * Player presses E on Reverend Dave during soup kitchen hours.
     * Grants a free SOUP_CUP (once per day).
     *
     * @param timeSystem  the TimeSystem
     * @param inventory   the player's inventory
     * @return true if soup was given
     */
    public boolean requestSoupKitchen(TimeSystem timeSystem, Inventory inventory) {
        float hour = timeSystem.getTime();
        int dayCount = timeSystem.getDayCount();

        if (!isSoupKitchenActive(dayCount, hour)) return false;

        // Once per day
        if (soupCollectedToday && lastSoupDay == dayCount) return false;

        if (inventory != null) {
            inventory.addItem(Material.SOUP_CUP, 1);
        }
        soupCollectedToday = true;
        lastSoupDay = dayCount;
        return true;
    }

    /**
     * Player presses E on CONFESSION_BOOTH_PROP.
     * Returns a random rumour text from the network (any type).
     *
     * @param rumourNetwork the RumourNetwork (null safe)
     * @param npcs          all living NPCs (rumour source)
     * @return a rumour text, or null if none available
     */
    public String eavesdropConfessionBooth(RumourNetwork rumourNetwork, List<NPC> npcs) {
        if (rumourNetwork == null || npcs == null || npcs.isEmpty()) return null;
        // Find an NPC with a rumour
        List<Rumour> available = new ArrayList<>();
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            available.addAll(npc.getRumours());
        }
        if (available.isEmpty()) return null;
        return available.get(random.nextInt(available.size())).getText();
    }

    /**
     * Player presses E on Reverend Dave outside service hours for gossip.
     * Reveals up to 1 NEIGHBOURHOOD or SHOP_NEWS rumour per conversation (max 2/day).
     *
     * @param rumourNetwork       the RumourNetwork (null safe)
     * @param npcs                all living NPCs
     * @return a rumour text, or null if none available or daily cap reached
     */
    public String interactWithDaveForRumour(RumourNetwork rumourNetwork, List<NPC> npcs) {
        if (rumourNetwork == null || npcs == null || vicarNpc == null) return null;
        // Find NEIGHBOURHOOD or SHOP_NEWS rumour from any NPC
        for (NPC npc : npcs) {
            if (!npc.isAlive()) continue;
            for (Rumour r : npc.getRumours()) {
                if (r.getType() == RumourType.NEIGHBOURHOOD
                        || r.getType() == RumourType.SHOP_NEWS) {
                    return r.getText();
                }
            }
        }
        return null;
    }

    // ── Getters for state ──────────────────────────────────────────────────────

    public List<NPC> getCongregationNpcs() {
        return congregationNpcs;
    }

    public List<NPC> getJumbleSaleNpcs() {
        return jumbleSaleNpcs;
    }

    public NPC getVicarNpc() {
        return vicarNpc;
    }

    public boolean isPlayerInsideChurch() {
        return playerInsideChurch;
    }

    public void setPlayerInsideChurch(boolean inside) {
        this.playerInsideChurch = inside;
        if (!inside) sanctuaryTimer = 0f;
    }

    public float getSanctuaryTimer() {
        return sanctuaryTimer;
    }

    public void setSanctuaryTimerForTesting(float t) {
        this.sanctuaryTimer = t;
    }

    public int getSundayServicesAttended() {
        return sundayServicesAttended;
    }

    public boolean isSoupCollectedToday() {
        return soupCollectedToday;
    }

    public boolean isVicarSpawned() {
        return vicarSpawned;
    }
}
