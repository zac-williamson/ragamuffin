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
 * Issue #928: Public Library System — Skill Books, Free Internet &amp; Rough Sleeping.
 *
 * <h3>Core features</h3>
 * <ul>
 *   <li>Player can read books on {@code BOOKSHELF} props to gain StreetSkill XP:
 *       TRADING, CONSTRUCTION, STREETWISE, or HORTICULTURE — +15 XP per 60-second
 *       session, maximum 3 sessions per in-game day.</li>
 *   <li>Free {@code INTERNET_TERMINAL} lets the player scout fence prices, check
 *       their criminal record, or pre-register for JobCentre jobs.</li>
 *   <li>After closing time (17:30), a broken back window allows the player to sneak
 *       in to sleep rough — restores full health and warmth, advances time to 07:00,
 *       20% chance of a police check.</li>
 *   <li>Stern {@code LIBRARIAN} NPC patrols shelves, shushes sprinting players
 *       (−30% speed for 5s), ejects repeat offenders (10-minute door lock, seeds
 *       {@code LIBRARY_BAN} rumour).</li>
 *   <li>Being inside during opening hours doubles the player's Notoriety decay rate
 *       (police give pass if Notoriety &lt; 60).</li>
 * </ul>
 *
 * <h3>System integrations</h3>
 * <ul>
 *   <li>StreetSkillSystem — XP for book reading sessions</li>
 *   <li>NotorietySystem — doubled decay rate while inside; reading STREET_LAW_PAMPHLET
 *       lowers notoriety visibility</li>
 *   <li>WarmthSystem — shelter zone, +{@value #SHELTER_WARMTH_RATE} warmth/s inside</li>
 *   <li>TimeSystem — closed Sundays, opening hours 09:00–17:30</li>
 *   <li>JobCentreSystem — internet terminal pre-registration</li>
 *   <li>RumourNetwork — LIBRARY_BAN rumour on ejection</li>
 *   <li>NewspaperSystem — free daily paper from NEWSPAPER_STAND reveals active MarketEvent</li>
 *   <li>FactionSystem — Street Law reading gives Street Lads Respect +5</li>
 * </ul>
 *
 * <h3>Achievements</h3>
 * BOOKWORM, NIGHT_OWL, SELF_IMPROVEMENT, SHUSHED, EJECTED_FROM_LIBRARY, FLASK_OF_SYMPATHY
 */
public class LibrarySystem {

    // ── Opening hours ─────────────────────────────────────────────────────────

    /** Hour the library opens. */
    public static final float OPEN_HOUR = 9.0f;

    /** Hour the library closes (exclusive). */
    public static final float CLOSE_HOUR = 17.5f; // 17:30

    /** Day-of-week index for Sunday (dayCount offset; game starts June 1 2026 = Tuesday). */
    private static final int SUNDAY_DAY_OFFSET = 5; // (dayCount + SUNDAY_DAY_OFFSET) % 7 == 0 → Sunday

    // ── Book reading constants ─────────────────────────────────────────────────

    /** Real seconds for a full reading session. */
    public static final float READING_SESSION_DURATION = 60.0f;

    /** XP awarded per completed reading session. */
    public static final int READING_SESSION_XP = 15;

    /** Maximum reading sessions allowed per in-game day. */
    public static final int MAX_SESSIONS_PER_DAY = 3;

    // ── Shush / ejection constants ─────────────────────────────────────────────

    /** Player speed multiplier during the shush debuff. */
    public static final float SHUSH_SPEED_MULT = 0.70f; // −30%

    /** Duration of the shush speed debuff in real seconds. */
    public static final float SHUSH_DURATION = 5.0f;

    /** Number of shush events before the player is ejected. */
    public static final int SHUSH_EJECT_THRESHOLD = 2;

    /** Duration of the door lock after ejection, in real seconds (10 in-game minutes). */
    public static final float EJECTION_LOCK_DURATION = 100.0f; // ~10 in-game minutes

    /** Player sprint speed threshold (blocks/s). If speed above this, librarian shushes. */
    public static final float SPRINT_SPEED_THRESHOLD = 5.0f;

    // ── Rough sleeping constants ───────────────────────────────────────────────

    /** Chance of police check when sleeping rough (0.0–1.0). */
    public static final float ROUGH_SLEEP_POLICE_CHANCE = 0.20f;

    /** Time the player wakes to after sleeping rough. */
    public static final float ROUGH_SLEEP_WAKE_HOUR = 7.0f;

    // ── Notoriety / warmth constants ──────────────────────────────────────────

    /** Notoriety threshold below which police give the player a pass inside the library. */
    public static final int NOTORIETY_POLICE_PASS_THRESHOLD = 60;

    /** Notoriety decay rate multiplier while inside the library during opening hours. */
    public static final float NOTORIETY_DECAY_MULTIPLIER = 2.0f;

    /** Warmth restored per second while inside the library (shelter zone). */
    public static final float SHELTER_WARMTH_RATE = 3.0f;

    // ── Internet terminal constants ───────────────────────────────────────────

    /** Notoriety added when checking criminal record on the internet terminal. */
    public static final int TERMINAL_RECORD_CHECK_NOTORIETY = 0;

    // ── FactionSystem respects ────────────────────────────────────────────────

    /** Street Lads Respect awarded on reading STREET_LAW_PAMPHLET. */
    public static final int STREET_LAW_RESPECT = 5;

    // ── Achievement thresholds ────────────────────────────────────────────────

    /** Sessions needed for SELF_IMPROVEMENT achievement. */
    public static final int SELF_IMPROVEMENT_TARGET = 5;

    /** Sessions needed for BOOKWORM achievement (in one day). */
    public static final int BOOKWORM_TARGET = 3;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Whether a reading session is currently in progress. */
    private boolean readingActive = false;

    /** Remaining real seconds in the current reading session. */
    private float readingTimer = 0f;

    /** The skill the current reading session will award XP to. */
    private StreetSkillSystem.Skill currentReadingSkill = null;

    /** The material of the book currently being read (or null if no specific book). */
    private Material currentBook = null;

    /** Number of reading sessions completed today. */
    private int sessionsToday = 0;

    /** The in-game day on which sessions were last counted (for resetting daily). */
    private int lastSessionDay = -1;

    /** Remaining real seconds of the shush speed debuff (0 = not active). */
    private float shushTimer = 0f;

    /** Number of times the player has been shushed during the current visit. */
    private int shushCount = 0;

    /** Remaining real seconds of the ejection door lock (0 = not locked). */
    private float ejectionLockTimer = 0f;

    /** Whether the free daily newspaper has been collected today. */
    private boolean dailyPaperCollected = false;

    /** The in-game day on which the paper was last collected (for resetting daily). */
    private int lastPaperDay = -1;

    /** Whether the LIBRARIAN NPC has been spawned. */
    private boolean librarianSpawned = false;

    /** The active LIBRARIAN NPC, or null if none. */
    private NPC librarian = null;

    // ── Achievement tracking ──────────────────────────────────────────────────

    /** Total reading sessions ever completed (for SELF_IMPROVEMENT). */
    private int totalSessionsCompleted = 0;

    /** Whether NIGHT_OWL has been awarded. */
    private boolean nightOwlAwarded = false;

    /** Whether SHUSHED has been awarded. */
    private boolean shushedAwarded = false;

    /** Whether EJECTED_FROM_LIBRARY has been awarded. */
    private boolean ejectedAwarded = false;

    /** Whether FLASK_OF_SYMPATHY has been awarded. */
    private boolean flaskOfSympathyAwarded = false;

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final Random random;

    // ── Construction ──────────────────────────────────────────────────────────

    public LibrarySystem() {
        this(new Random());
    }

    public LibrarySystem(Random random) {
        this.random = random;
    }

    // ── Per-frame update ──────────────────────────────────────────────────────

    /**
     * Update the library system each frame.
     *
     * @param delta              seconds since last frame
     * @param timeSystem         game time (for opening hours, Sunday check, session resets)
     * @param player             the player
     * @param npcManager         NPC manager (for spawning LIBRARIAN)
     * @param streetSkillSystem  street skill system (for XP awards)
     * @param notorietySystem    notoriety system (for decay bonus, rumour seeding)
     * @param rumourNetwork      rumour network (for LIBRARY_BAN rumour on ejection)
     * @param factionSystem      faction system (for Street Lads Respect on street law reading)
     * @param allNpcs            all active NPCs (for rumour seeding)
     * @param achievementCallback achievement callback
     */
    public void update(float delta,
                       TimeSystem timeSystem,
                       Player player,
                       NPCManager npcManager,
                       StreetSkillSystem streetSkillSystem,
                       NotorietySystem notorietySystem,
                       RumourNetwork rumourNetwork,
                       FactionSystem factionSystem,
                       List<NPC> allNpcs,
                       NotorietySystem.AchievementCallback achievementCallback) {

        float hour = (timeSystem != null) ? timeSystem.getTime() : 12.0f;
        int day = (timeSystem != null) ? timeSystem.getDayCount() : 1;
        boolean isOpen = isOpen(hour, day);

        // Reset daily reading session count
        if (day != lastSessionDay) {
            sessionsToday = 0;
            lastSessionDay = day;
        }

        // Reset daily newspaper
        if (day != lastPaperDay) {
            dailyPaperCollected = false;
        }

        // Manage LIBRARIAN NPC (only while open)
        if (isOpen && !librarianSpawned) {
            spawnLibrarian(player, npcManager);
        } else if (!isOpen && librarianSpawned) {
            despawnLibrarian();
        }

        // Advance reading session timer
        if (readingActive) {
            readingTimer -= delta;
            if (readingTimer <= 0f) {
                readingTimer = 0f;
                completeReadingSession(streetSkillSystem, factionSystem, achievementCallback);
            }
        }

        // Advance shush timer
        if (shushTimer > 0f) {
            shushTimer = Math.max(0f, shushTimer - delta);
        }

        // Advance ejection lock timer
        if (ejectionLockTimer > 0f) {
            ejectionLockTimer = Math.max(0f, ejectionLockTimer - delta);
        }
    }

    // ── Book reading ──────────────────────────────────────────────────────────

    /**
     * Result of interacting with a {@code BOOKSHELF} prop.
     */
    public enum BookshelfResult {
        /** Reading session started. */
        READING_STARTED,
        /** Library is closed. */
        CLOSED,
        /** Daily session limit reached. */
        SESSION_LIMIT_REACHED,
        /** A reading session is already in progress. */
        ALREADY_READING,
        /** Player is currently banned (ejection lock active). */
        BANNED
    }

    /**
     * Called when the player interacts (E) with a {@code BOOKSHELF} prop.
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day
     * @param skill    the skill associated with this bookshelf
     * @param book     the material book being read (e.g. DIY_MANUAL), or null
     * @return result of the interaction
     */
    public BookshelfResult interactWithBookshelf(float hour, int dayCount,
                                                  StreetSkillSystem.Skill skill,
                                                  Material book) {
        if (!isOpen(hour, dayCount)) {
            return BookshelfResult.CLOSED;
        }
        if (ejectionLockTimer > 0f) {
            return BookshelfResult.BANNED;
        }
        if (readingActive) {
            return BookshelfResult.ALREADY_READING;
        }
        // Reset daily sessions if day changed
        if (dayCount != lastSessionDay) {
            sessionsToday = 0;
            lastSessionDay = dayCount;
        }
        if (sessionsToday >= MAX_SESSIONS_PER_DAY) {
            return BookshelfResult.SESSION_LIMIT_REACHED;
        }

        readingActive = true;
        readingTimer = READING_SESSION_DURATION;
        currentReadingSkill = skill;
        currentBook = book;
        return BookshelfResult.READING_STARTED;
    }

    /**
     * Complete the current reading session: award XP and achievements.
     */
    private void completeReadingSession(StreetSkillSystem streetSkillSystem,
                                         FactionSystem factionSystem,
                                         NotorietySystem.AchievementCallback achievementCallback) {
        readingActive = false;
        readingTimer = 0f;
        sessionsToday++;
        totalSessionsCompleted++;

        // Award XP
        if (streetSkillSystem != null && currentReadingSkill != null) {
            streetSkillSystem.awardXP(currentReadingSkill, READING_SESSION_XP);
        }

        // Street Law Pamphlet → Street Lads Respect
        if (currentBook == Material.STREET_LAW_PAMPHLET && factionSystem != null) {
            factionSystem.applyRespectDelta(Faction.STREET_LADS, STREET_LAW_RESPECT);
        }

        currentReadingSkill = null;
        currentBook = null;

        // Achievements
        if (achievementCallback != null) {
            if (sessionsToday >= BOOKWORM_TARGET) {
                achievementCallback.award(AchievementType.BOOKWORM);
            }
            if (totalSessionsCompleted >= SELF_IMPROVEMENT_TARGET) {
                achievementCallback.award(AchievementType.SELF_IMPROVEMENT);
            }
        }
    }

    /**
     * Cancel an in-progress reading session (e.g. player moves away or is shushed).
     */
    public void cancelReading() {
        readingActive = false;
        readingTimer = 0f;
        currentReadingSkill = null;
        currentBook = null;
    }

    // ── Librarian shush / ejection ────────────────────────────────────────────

    /**
     * Called when the LIBRARIAN detects the player sprinting inside the library.
     * Applies the shush speed debuff. If the player has been shushed too many times,
     * triggers ejection.
     *
     * @param rumourNetwork      rumour network for LIBRARY_BAN seeding
     * @param allNpcs            all active NPCs
     * @param achievementCallback achievement callback
     * @return true if the player was ejected (rather than just shushed)
     */
    public boolean onLibrarianShush(RumourNetwork rumourNetwork,
                                     List<NPC> allNpcs,
                                     NotorietySystem.AchievementCallback achievementCallback) {
        shushCount++;
        shushTimer = SHUSH_DURATION;

        // Award SHUSHED achievement on first shush
        if (!shushedAwarded && achievementCallback != null) {
            shushedAwarded = true;
            achievementCallback.award(AchievementType.SHUSHED);
        }

        // Set librarian speech
        if (librarian != null) {
            librarian.setSpeechText("Shhhhh!", 3f);
        }

        // Eject on repeat offence
        if (shushCount >= SHUSH_EJECT_THRESHOLD) {
            return ejectPlayer(rumourNetwork, allNpcs, achievementCallback);
        }
        return false;
    }

    /**
     * Eject the player from the library. Locks the door for {@value #EJECTION_LOCK_DURATION}
     * real seconds and seeds a LIBRARY_BAN rumour.
     *
     * @param rumourNetwork      rumour network for LIBRARY_BAN seeding
     * @param allNpcs            all active NPCs
     * @param achievementCallback achievement callback
     * @return true (ejection always succeeds when called)
     */
    public boolean ejectPlayer(RumourNetwork rumourNetwork,
                                List<NPC> allNpcs,
                                NotorietySystem.AchievementCallback achievementCallback) {
        ejectionLockTimer = EJECTION_LOCK_DURATION;
        shushCount = 0;
        cancelReading();

        if (librarian != null) {
            librarian.setSpeechText("Out! And don't come back!", 5f);
        }

        // Seed LIBRARY_BAN rumour
        if (rumourNetwork != null && allNpcs != null) {
            for (NPC npc : allNpcs) {
                if (npc.getType() == NPCType.PUBLIC && npc.isAlive()) {
                    rumourNetwork.addRumour(npc, new Rumour(
                            RumourType.LIBRARY_BAN,
                            "Heard someone got chucked out of the library — librarian's on the warpath."));
                    break;
                }
            }
        }

        // Award ejection achievement
        if (!ejectedAwarded && achievementCallback != null) {
            ejectedAwarded = true;
            achievementCallback.award(AchievementType.EJECTED_FROM_LIBRARY);
        }

        return true;
    }

    // ── Rough sleeping ────────────────────────────────────────────────────────

    /**
     * Result of attempting to sleep rough in the library after closing time.
     */
    public enum SleepResult {
        /** Slept successfully; health and warmth restored, time advanced to 07:00. */
        SUCCESS,
        /** Library is still open. */
        LIBRARY_OPEN,
        /** Player has been banned (ejection lock active). */
        BANNED,
        /** Police check triggered (20% chance). Player still slept. */
        POLICE_CHECK
    }

    /**
     * Called when the player sleeps rough inside the library after closing time.
     * Restores full health and warmth. Advances time to 07:00 (next morning).
     * 20% chance of a police check.
     *
     * @param hour       current in-game hour
     * @param dayCount   current in-game day
     * @param player     the player
     * @param timeSystem the time system (to advance time)
     * @param achievementCallback achievement callback
     * @return the result of the sleep attempt
     */
    public SleepResult sleepRough(float hour, int dayCount, Player player,
                                   TimeSystem timeSystem,
                                   NotorietySystem.AchievementCallback achievementCallback) {
        if (isOpen(hour, dayCount)) {
            return SleepResult.LIBRARY_OPEN;
        }
        if (ejectionLockTimer > 0f) {
            return SleepResult.BANNED;
        }

        // Restore full health and warmth
        if (player != null) {
            player.setHealth(Player.MAX_HEALTH);
            player.setWarmth(Player.MAX_WARMTH);
        }

        // Advance time to 07:00 next morning
        if (timeSystem != null) {
            timeSystem.setTime(ROUGH_SLEEP_WAKE_HOUR);
            // If time was already past 07:00, also advance the day count
            if (hour >= ROUGH_SLEEP_WAKE_HOUR) {
                timeSystem.advanceTime(24.0f - (hour - ROUGH_SLEEP_WAKE_HOUR));
                // setTime already set to 7:00; we just need day to advance
                // advanceTime may not keep exactly 7:00 — instead use setTime after day bump
                timeSystem.setTime(ROUGH_SLEEP_WAKE_HOUR);
            }
        }

        // Achievements
        if (!nightOwlAwarded && achievementCallback != null) {
            nightOwlAwarded = true;
            achievementCallback.award(AchievementType.NIGHT_OWL);
        }
        if (!flaskOfSympathyAwarded && achievementCallback != null) {
            flaskOfSympathyAwarded = true;
            achievementCallback.award(AchievementType.FLASK_OF_SYMPATHY);
        }

        // 20% chance of police check
        if (random.nextFloat() < ROUGH_SLEEP_POLICE_CHANCE) {
            return SleepResult.POLICE_CHECK;
        }

        return SleepResult.SUCCESS;
    }

    // ── Internet terminal ─────────────────────────────────────────────────────

    /**
     * Result of interacting with the {@code INTERNET_TERMINAL} prop.
     */
    public enum TerminalAction {
        /** Scout fence prices — shows a brief price forecast. */
        SCOUT_FENCE_PRICES,
        /** Check criminal record summary. */
        CHECK_CRIMINAL_RECORD,
        /** Pre-register for a JobCentre job. */
        PREREGISTER_JOB_CENTRE
    }

    /**
     * Result of using the internet terminal.
     */
    public enum TerminalResult {
        /** Action completed successfully. */
        SUCCESS,
        /** Library is closed. */
        CLOSED,
        /** Player is banned. */
        BANNED
    }

    /**
     * Called when the player interacts with the {@code INTERNET_TERMINAL} prop.
     *
     * @param hour       current in-game hour
     * @param dayCount   current in-game day
     * @param action     the action to perform
     * @return result of the interaction
     */
    public TerminalResult useInternetTerminal(float hour, int dayCount, TerminalAction action) {
        if (!isOpen(hour, dayCount)) {
            return TerminalResult.CLOSED;
        }
        if (ejectionLockTimer > 0f) {
            return TerminalResult.BANNED;
        }
        // The action itself is handled by the caller (UI layer);
        // this method just validates access.
        return TerminalResult.SUCCESS;
    }

    // ── Free daily newspaper ──────────────────────────────────────────────────

    /**
     * Result of interacting with the {@code NEWSPAPER_STAND} prop.
     */
    public enum NewspaperResult {
        /** Free newspaper collected (inventory updated by caller). */
        COLLECTED,
        /** Library is closed. */
        CLOSED,
        /** Player has already collected today's paper. */
        ALREADY_COLLECTED
    }

    /**
     * Called when the player interacts with the {@code NEWSPAPER_STAND} prop.
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day
     * @param inventory player inventory (NEWSPAPER added if result is COLLECTED)
     * @return result of the interaction
     */
    public NewspaperResult collectDailyPaper(float hour, int dayCount, Inventory inventory) {
        if (!isOpen(hour, dayCount)) {
            return NewspaperResult.CLOSED;
        }
        if (dayCount == lastPaperDay && dailyPaperCollected) {
            return NewspaperResult.ALREADY_COLLECTED;
        }

        dailyPaperCollected = true;
        lastPaperDay = dayCount;

        if (inventory != null) {
            inventory.addItem(Material.NEWSPAPER, 1);
        }

        return NewspaperResult.COLLECTED;
    }

    // ── Notoriety decay bonus ─────────────────────────────────────────────────

    /**
     * Returns true if the player is inside the library during opening hours and
     * can benefit from doubled Notoriety decay.
     *
     * @param hour     current in-game hour
     * @param dayCount current in-game day
     * @return true if inside-library Notoriety decay bonus applies
     */
    public boolean isNotorietyDecayBonusActive(float hour, int dayCount) {
        return isOpen(hour, dayCount) && ejectionLockTimer <= 0f;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the library is open at the given in-game hour and day.
     * The library is closed on Sundays.
     *
     * @param hour     in-game hour (0–24)
     * @param dayCount current in-game day count
     * @return true if open
     */
    public boolean isOpen(float hour, int dayCount) {
        if (isSunday(dayCount)) {
            return false;
        }
        return hour >= OPEN_HOUR && hour < CLOSE_HOUR;
    }

    /**
     * Returns true if the given day is a Sunday.
     * The game starts on June 1 2026 (Monday, dayCount=1).
     * Sunday = dayCount mod 7 == 0 (day 7, 14, 21, ... are Sundays).
     *
     * @param dayCount current day count (1-based)
     * @return true if Sunday
     */
    public boolean isSunday(int dayCount) {
        // Day 1 = Monday (June 1 2026), so day 7 = Sunday, day 14 = Sunday, etc.
        return dayCount % 7 == 0;
    }

    // ── LIBRARIAN NPC management ──────────────────────────────────────────────

    private void spawnLibrarian(Player player, NPCManager npcManager) {
        if (librarianSpawned || npcManager == null || player == null) return;

        librarianSpawned = true;
        float x = player.getPosition().x + 3f;
        float y = player.getPosition().y;
        float z = player.getPosition().z + 3f;
        librarian = npcManager.spawnNPC(NPCType.LIBRARIAN, x, y, z);
        if (librarian != null) {
            librarian.setName("Mrs Hartley");
            librarian.setBuildingType(LandmarkType.LIBRARY);
            librarian.setState(NPCState.PATROLLING);
            librarian.setSpeechText("Quiet in here, please.", 3f);
        }
    }

    private void despawnLibrarian() {
        if (librarian != null) {
            librarian.setState(NPCState.FLEEING);
            librarian = null;
        }
        librarianSpawned = false;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Whether a reading session is currently in progress. */
    public boolean isReadingActive() { return readingActive; }

    /** Remaining real seconds in the current reading session (0 if not active). */
    public float getReadingTimer() { return readingTimer; }

    /** The skill the current reading session will award XP to (null if not reading). */
    public StreetSkillSystem.Skill getCurrentReadingSkill() { return currentReadingSkill; }

    /** The material of the book currently being read (null if not reading or no specific book). */
    public Material getCurrentBook() { return currentBook; }

    /** Number of reading sessions completed today. */
    public int getSessionsToday() { return sessionsToday; }

    /** Total reading sessions ever completed. */
    public int getTotalSessionsCompleted() { return totalSessionsCompleted; }

    /** Whether the shush speed debuff is currently active. */
    public boolean isShushed() { return shushTimer > 0f; }

    /** Remaining real seconds of the shush speed debuff. */
    public float getShushTimer() { return shushTimer; }

    /** Player movement speed multiplier (1.0 normally; reduced when shushed). */
    public float getMovementSpeedMultiplier() {
        return isShushed() ? SHUSH_SPEED_MULT : 1.0f;
    }

    /** Number of times the player has been shushed during the current visit. */
    public int getShushCount() { return shushCount; }

    /** Whether the ejection door lock is currently active. */
    public boolean isEjectionLockActive() { return ejectionLockTimer > 0f; }

    /** Remaining real seconds of the ejection door lock. */
    public float getEjectionLockTimer() { return ejectionLockTimer; }

    /** Whether today's free newspaper has been collected. */
    public boolean isDailyPaperCollected() { return dailyPaperCollected; }

    /** The active LIBRARIAN NPC (null if not spawned). */
    public NPC getLibrarian() { return librarian; }

    /** Whether the LIBRARIAN NPC is currently spawned. */
    public boolean isLibrarianSpawned() { return librarianSpawned; }

    // ── Force-set methods for testing ─────────────────────────────────────────

    /** Force-set reading session state (for testing). */
    public void setReadingActiveForTesting(boolean active, float timer,
                                            StreetSkillSystem.Skill skill) {
        this.readingActive = active;
        this.readingTimer = timer;
        this.currentReadingSkill = skill;
    }

    /** Force-set sessions today (for testing). */
    public void setSessionsTodayForTesting(int count) {
        this.sessionsToday = count;
    }

    /** Force-set total sessions completed (for testing). */
    public void setTotalSessionsCompletedForTesting(int count) {
        this.totalSessionsCompleted = count;
    }

    /** Force-set shush timer (for testing). */
    public void setShushTimerForTesting(float seconds) {
        this.shushTimer = seconds;
    }

    /** Force-set shush count (for testing). */
    public void setShushCountForTesting(int count) {
        this.shushCount = count;
    }

    /** Force-set ejection lock timer (for testing). */
    public void setEjectionLockTimerForTesting(float seconds) {
        this.ejectionLockTimer = seconds;
    }

    /** Force-set the last session day (for testing). */
    public void setLastSessionDayForTesting(int day) {
        this.lastSessionDay = day;
    }

    /** Force-set the librarian NPC (for testing). */
    public void setLibrarianForTesting(NPC npc) {
        this.librarian = npc;
        this.librarianSpawned = npc != null;
    }

    /** Force-set daily paper collected state (for testing). */
    public void setDailyPaperCollectedForTesting(boolean collected) {
        this.dailyPaperCollected = collected;
    }

    /** Force-set last paper day (for testing). */
    public void setLastPaperDayForTesting(int day) {
        this.lastPaperDay = day;
    }
}
