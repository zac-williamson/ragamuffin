package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.*;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #928 — Public Library System: Skill Books, Free Internet
 * &amp; Rough Sleeping.
 *
 * <p>Six scenarios:
 * <ol>
 *   <li>Reading a bookshelf awards StreetSkill XP and respects daily session limit</li>
 *   <li>Librarian shush mechanic — speed debuff applied; ejection on repeat offence</li>
 *   <li>Rough sleeping after closing time — health/warmth restored, time advances</li>
 *   <li>Internet terminal access — validates open/closed and banned states</li>
 *   <li>Free daily newspaper — collected once per day; unavailable when closed</li>
 *   <li>Library closed on Sundays and during evening/night hours</li>
 * </ol>
 */
class Issue928LibrarySystemTest {

    private LibrarySystem librarySystem;
    private TimeSystem timeSystem;
    private StreetSkillSystem streetSkillSystem;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        librarySystem = new LibrarySystem(new Random(42));
        timeSystem = new TimeSystem(10.0f); // 10:00 — library open
        streetSkillSystem = new StreetSkillSystem(new Random(1));
        factionSystem = new FactionSystem(new Random(2));
        rumourNetwork = new RumourNetwork(new Random(3));
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 50);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Book reading awards XP and respects daily session limit
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1a: Interact with a BOOKSHELF during opening hours. Verify
     * READING_STARTED is returned. Simulate the 60-second reading session
     * completing (update with delta = 60). Verify TRADING XP increased by 15.
     * Verify sessionsToday == 1.
     */
    @Test
    void bookshelf_readingSessionAwardsXP() {
        float hour = 11.0f;
        int day = 1;

        // Start reading session on TRADING bookshelf
        LibrarySystem.BookshelfResult result = librarySystem.interactWithBookshelf(
                hour, day, StreetSkillSystem.Skill.TRADING, Material.NEGOTIATION_BOOK);

        assertEquals(LibrarySystem.BookshelfResult.READING_STARTED, result,
                "Should be able to start a reading session during opening hours");
        assertTrue(librarySystem.isReadingActive(), "Reading should be active");

        int tradingXpBefore = streetSkillSystem.getXP(StreetSkillSystem.Skill.TRADING);

        // Simulate 60 real seconds passing (completes the session)
        librarySystem.update(LibrarySystem.READING_SESSION_DURATION, timeSystem, null, null,
                streetSkillSystem, null, null, null, null, achievementCallback);

        assertFalse(librarySystem.isReadingActive(), "Reading session should be complete");
        assertEquals(1, librarySystem.getSessionsToday(), "Sessions today should be 1");

        int tradingXpAfter = streetSkillSystem.getXP(StreetSkillSystem.Skill.TRADING);
        assertEquals(tradingXpBefore + LibrarySystem.READING_SESSION_XP, tradingXpAfter,
                "TRADING XP should increase by " + LibrarySystem.READING_SESSION_XP + " after reading session");
    }

    /**
     * Scenario 1b: After 3 sessions today, interacting with a bookshelf returns
     * SESSION_LIMIT_REACHED. Verify no further XP is awarded.
     */
    @Test
    void bookshelf_dailySessionLimitEnforced() {
        float hour = 11.0f;
        int day = 2; // day 2 (non-Sunday: 2 % 7 != 0)

        // Set sessions today to max
        librarySystem.setSessionsTodayForTesting(LibrarySystem.MAX_SESSIONS_PER_DAY);
        librarySystem.setLastSessionDayForTesting(day);

        LibrarySystem.BookshelfResult result = librarySystem.interactWithBookshelf(
                hour, day, StreetSkillSystem.Skill.TRADING, null);

        assertEquals(LibrarySystem.BookshelfResult.SESSION_LIMIT_REACHED, result,
                "Should reject reading after " + LibrarySystem.MAX_SESSIONS_PER_DAY + " sessions today");
    }

    /**
     * Scenario 1c: Reading a STREET_LAW_PAMPHLET also awards Street Lads Respect.
     * Verify FactionSystem receives a positive delta for STREET_LADS.
     */
    @Test
    void bookshelf_streetLawPamphletGrantsStreetLadsRespect() {
        float hour = 11.0f;
        int day = 1;

        int respectBefore = factionSystem.getRespect(Faction.STREET_LADS);

        // Start and complete a STREETWISE reading session with STREET_LAW_PAMPHLET
        librarySystem.interactWithBookshelf(hour, day, StreetSkillSystem.Skill.STREETWISE,
                Material.STREET_LAW_PAMPHLET);

        // Complete the session
        librarySystem.update(LibrarySystem.READING_SESSION_DURATION, timeSystem, null, null,
                streetSkillSystem, null, null, factionSystem, null, achievementCallback);

        int respectAfter = factionSystem.getRespect(Faction.STREET_LADS);
        assertTrue(respectAfter >= respectBefore + LibrarySystem.STREET_LAW_RESPECT,
                "Reading Street Law Pamphlet should grant Street Lads Respect ≥ "
                + LibrarySystem.STREET_LAW_RESPECT + " (was " + respectBefore
                + ", now " + respectAfter + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Librarian shush — speed debuff and ejection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2a: Call onLibrarianShush() once. Verify isShushed() is true.
     * Verify getMovementSpeedMultiplier() returns SHUSH_SPEED_MULT (0.70).
     * Verify SHUSHED achievement is awarded. Verify player was NOT ejected.
     */
    @Test
    void librarian_firstShushAppliesSpeedDebuff() {
        assertFalse(librarySystem.isShushed(), "Should not be shushed at start");

        boolean ejected = librarySystem.onLibrarianShush(rumourNetwork, new ArrayList<>(),
                achievementCallback);

        assertFalse(ejected, "First shush should NOT eject the player");
        assertTrue(librarySystem.isShushed(), "Player should be shushed after first shush");
        assertEquals(LibrarySystem.SHUSH_SPEED_MULT, librarySystem.getMovementSpeedMultiplier(),
                0.001f, "Speed should be reduced to " + LibrarySystem.SHUSH_SPEED_MULT
                + " when shushed");
        assertTrue(achievementSystem.isUnlocked(AchievementType.SHUSHED),
                "SHUSHED achievement should be awarded on first shush");
    }

    /**
     * Scenario 2b: Call onLibrarianShush() twice (at or above SHUSH_EJECT_THRESHOLD).
     * Verify isEjectionLockActive() is true. Verify EJECTED_FROM_LIBRARY achievement
     * is awarded. Verify a LIBRARY_BAN rumour is seeded to a PUBLIC NPC.
     */
    @Test
    void librarian_repeatShushEjectsPlayer() {
        List<NPC> npcs = new ArrayList<>();
        NPC publicNpc = new NPC(NPCType.PUBLIC, 0f, 1f, 0f);
        npcs.add(publicNpc);

        // First shush — no ejection
        librarySystem.onLibrarianShush(rumourNetwork, npcs, achievementCallback);
        assertFalse(librarySystem.isEjectionLockActive(), "Not ejected after first shush");

        // Second shush — ejection (threshold = 2)
        boolean ejected = librarySystem.onLibrarianShush(rumourNetwork, npcs, achievementCallback);

        assertTrue(ejected, "Second shush should eject the player");
        assertTrue(librarySystem.isEjectionLockActive(), "Ejection lock should be active after ejection");
        assertTrue(librarySystem.getEjectionLockTimer() > 0f, "Ejection lock timer should be positive");

        assertTrue(achievementSystem.isUnlocked(AchievementType.EJECTED_FROM_LIBRARY),
                "EJECTED_FROM_LIBRARY achievement should be awarded on ejection");

        // Verify ejection door lock prevents re-entry
        LibrarySystem.BookshelfResult bannedResult = librarySystem.interactWithBookshelf(
                11.0f, 1, StreetSkillSystem.Skill.TRADING, null);
        assertEquals(LibrarySystem.BookshelfResult.BANNED, bannedResult,
                "Banned player should get BANNED result on bookshelf interaction");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Rough sleeping after closing time
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Set time to 22:00 (after closing). Call sleepRough(). Verify
     * the result is SUCCESS or POLICE_CHECK (both are valid outcomes).
     * Verify player health is MAX_HEALTH. Verify player warmth is MAX_WARMTH.
     * Verify time is now 07:00. Verify NIGHT_OWL achievement is awarded.
     */
    @Test
    void roughSleeping_restoresHealthAndWarmthAdvancesTime() {
        // Use a deterministic random that returns > 0.20 to avoid police check
        LibrarySystem deterministicLibrary = new LibrarySystem(new Random(0) {
            @Override
            public float nextFloat() {
                return 0.99f; // avoid police check
            }
        });

        ragamuffin.entity.Player player = new ragamuffin.entity.Player();
        player.setHealth(30f);
        player.setWarmth(10f);

        TimeSystem ts = new TimeSystem(22.0f); // 22:00 — library closed
        AchievementSystem ach = new AchievementSystem();

        LibrarySystem.SleepResult result = deterministicLibrary.sleepRough(
                22.0f, 1, player, ts, type -> ach.unlock(type));

        assertEquals(LibrarySystem.SleepResult.SUCCESS, result,
                "Sleeping rough with no police check should return SUCCESS");

        assertEquals(ragamuffin.entity.Player.MAX_HEALTH, player.getHealth(), 0.001f,
                "Player health should be fully restored after sleeping rough");
        assertEquals(ragamuffin.entity.Player.MAX_WARMTH, player.getWarmth(), 0.001f,
                "Player warmth should be fully restored after sleeping rough");

        assertEquals(LibrarySystem.ROUGH_SLEEP_WAKE_HOUR, ts.getTime(), 0.001f,
                "Time should be 07:00 after sleeping rough");

        assertTrue(ach.isUnlocked(AchievementType.NIGHT_OWL),
                "NIGHT_OWL achievement should be awarded after sleeping rough");
        assertTrue(ach.isUnlocked(AchievementType.FLASK_OF_SYMPATHY),
                "FLASK_OF_SYMPATHY achievement should be awarded after sleeping rough");
    }

    /**
     * Scenario 3b: Attempt to sleep rough while the library is still open (10:00).
     * Verify SleepResult.LIBRARY_OPEN is returned. Player health is unchanged.
     */
    @Test
    void roughSleeping_refusedWhenLibraryIsOpen() {
        ragamuffin.entity.Player player = new ragamuffin.entity.Player();
        player.setHealth(50f);

        TimeSystem ts = new TimeSystem(10.0f); // 10:00 — library open
        LibrarySystem.SleepResult result = librarySystem.sleepRough(10.0f, 1, player, ts,
                achievementCallback);

        assertEquals(LibrarySystem.SleepResult.LIBRARY_OPEN, result,
                "Cannot sleep rough while library is open");
        assertEquals(50f, player.getHealth(), 0.001f,
                "Player health should be unchanged when sleep attempt is rejected");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Internet terminal access validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Verify internet terminal returns SUCCESS during opening hours.
     * Verify it returns CLOSED outside opening hours. Verify it returns BANNED
     * when the ejection lock is active.
     */
    @Test
    void internetTerminal_accessValidation() {
        // Open during hours
        LibrarySystem.TerminalResult openResult = librarySystem.useInternetTerminal(
                11.0f, 1, LibrarySystem.TerminalAction.SCOUT_FENCE_PRICES);
        assertEquals(LibrarySystem.TerminalResult.SUCCESS, openResult,
                "Terminal should be accessible during opening hours");

        // Closed outside hours
        LibrarySystem.TerminalResult closedResult = librarySystem.useInternetTerminal(
                20.0f, 1, LibrarySystem.TerminalAction.CHECK_CRIMINAL_RECORD);
        assertEquals(LibrarySystem.TerminalResult.CLOSED, closedResult,
                "Terminal should return CLOSED after closing time");

        // Banned when ejected
        librarySystem.setEjectionLockTimerForTesting(LibrarySystem.EJECTION_LOCK_DURATION);
        LibrarySystem.TerminalResult bannedResult = librarySystem.useInternetTerminal(
                11.0f, 1, LibrarySystem.TerminalAction.PREREGISTER_JOB_CENTRE);
        assertEquals(LibrarySystem.TerminalResult.BANNED, bannedResult,
                "Terminal should return BANNED when ejection lock is active");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Free daily newspaper — one per day, unavailable when closed
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5a: Interact with NEWSPAPER_STAND during opening hours. Verify
     * COLLECTED is returned and NEWSPAPER is added to inventory.
     * Interact again on the same day. Verify ALREADY_COLLECTED is returned.
     */
    @Test
    void newspaperStand_collectOncePerDay() {
        float hour = 11.0f;
        int day = 1;

        int papersBefore = inventory.getItemCount(Material.NEWSPAPER);

        LibrarySystem.NewspaperResult first = librarySystem.collectDailyPaper(hour, day, inventory);
        assertEquals(LibrarySystem.NewspaperResult.COLLECTED, first,
                "First collection should succeed");
        assertEquals(papersBefore + 1, inventory.getItemCount(Material.NEWSPAPER),
                "Inventory should have one more NEWSPAPER after collection");

        // Second attempt same day
        LibrarySystem.NewspaperResult second = librarySystem.collectDailyPaper(hour, day, inventory);
        assertEquals(LibrarySystem.NewspaperResult.ALREADY_COLLECTED, second,
                "Second collection on same day should return ALREADY_COLLECTED");
        assertEquals(papersBefore + 1, inventory.getItemCount(Material.NEWSPAPER),
                "Inventory should still have only one extra NEWSPAPER");
    }

    /**
     * Scenario 5b: Attempt to collect newspaper when library is closed (20:00).
     * Verify CLOSED is returned and NEWSPAPER is NOT added to inventory.
     */
    @Test
    void newspaperStand_unavailableWhenClosed() {
        float hour = 20.0f; // After closing time
        int day = 1;

        int papersBefore = inventory.getItemCount(Material.NEWSPAPER);
        LibrarySystem.NewspaperResult result = librarySystem.collectDailyPaper(hour, day, inventory);

        assertEquals(LibrarySystem.NewspaperResult.CLOSED, result,
                "Newspaper should be unavailable when library is closed");
        assertEquals(papersBefore, inventory.getItemCount(Material.NEWSPAPER),
                "Inventory should be unchanged when newspaper is unavailable");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Opening hours and Sunday closure
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6a: Verify library is open at 10:00 on a Monday (day 1).
     * Verify library is closed at 08:00 (before opening). Verify library is
     * closed at 18:00 (after closing). Verify library is closed on Sunday (day 7).
     */
    @Test
    void openingHours_correctlyEnforced() {
        // Open during hours on a weekday (day 1 = Monday)
        assertTrue(librarySystem.isOpen(10.0f, 1), "Should be open at 10:00 on Monday");
        assertTrue(librarySystem.isOpen(LibrarySystem.OPEN_HOUR, 1),
                "Should be open exactly at opening hour");
        assertTrue(librarySystem.isOpen(LibrarySystem.CLOSE_HOUR - 0.01f, 1),
                "Should be open just before closing hour");

        // Closed before/after hours
        assertFalse(librarySystem.isOpen(LibrarySystem.OPEN_HOUR - 0.01f, 1),
                "Should be closed just before opening hour");
        assertFalse(librarySystem.isOpen(LibrarySystem.CLOSE_HOUR, 1),
                "Should be closed at exactly closing hour");
        assertFalse(librarySystem.isOpen(20.0f, 1), "Should be closed at 20:00");
        assertFalse(librarySystem.isOpen(3.0f, 1), "Should be closed at 03:00");

        // Closed on Sunday (day 7 = Sunday: 7 % 7 == 0)
        assertTrue(librarySystem.isSunday(7), "Day 7 should be Sunday");
        assertFalse(librarySystem.isOpen(11.0f, 7),
                "Library should be closed on Sunday even during normal hours");

        // Non-Sunday day (day 14+1=15)
        assertFalse(librarySystem.isSunday(1), "Day 1 should not be Sunday");
        assertFalse(librarySystem.isSunday(14), "Day 14 is a Sunday (7*2=14)");
        assertTrue(librarySystem.isSunday(14), "Day 14 should be Sunday");
    }

    /**
     * Scenario 6b: Attempting to read a bookshelf when library is closed returns CLOSED.
     * Attempting to sleep rough when library is open returns LIBRARY_OPEN.
     */
    @Test
    void librarySystem_closedBlocksInteractions() {
        // Closed after hours — bookshelf interaction should fail
        LibrarySystem.BookshelfResult closedResult = librarySystem.interactWithBookshelf(
                20.0f, 1, StreetSkillSystem.Skill.TRADING, null);
        assertEquals(LibrarySystem.BookshelfResult.CLOSED, closedResult,
                "Bookshelf should return CLOSED when library is closed");

        // During hours — sleep rough should return LIBRARY_OPEN
        ragamuffin.entity.Player player = new ragamuffin.entity.Player();
        LibrarySystem.SleepResult sleepResult = librarySystem.sleepRough(11.0f, 1, player, timeSystem,
                achievementCallback);
        assertEquals(LibrarySystem.SleepResult.LIBRARY_OPEN, sleepResult,
                "Sleeping rough during opening hours should return LIBRARY_OPEN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: BOOKWORM achievement and SELF_IMPROVEMENT achievement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 7: Read 3 books in one day (MAX_SESSIONS_PER_DAY = 3).
     * After each session completes, verify sessionsToday increments.
     * After the 3rd session, verify BOOKWORM achievement is awarded.
     * Set total sessions completed to 5 before final session, verify
     * SELF_IMPROVEMENT achievement is awarded.
     */
    @Test
    void achievements_bookwormAndSelfImprovement() {
        float hour = 10.0f;
        int day = 1;

        // Set up for SELF_IMPROVEMENT (needs 5 total, we'll be at 4 before this test)
        librarySystem.setTotalSessionsCompletedForTesting(
                LibrarySystem.SELF_IMPROVEMENT_TARGET - 1);

        for (int i = 0; i < LibrarySystem.MAX_SESSIONS_PER_DAY; i++) {
            LibrarySystem.BookshelfResult result = librarySystem.interactWithBookshelf(
                    hour, day, StreetSkillSystem.Skill.TRADING, null);
            assertEquals(LibrarySystem.BookshelfResult.READING_STARTED, result,
                    "Session " + (i + 1) + " should start successfully");

            // Complete the session
            librarySystem.update(LibrarySystem.READING_SESSION_DURATION, timeSystem, null, null,
                    streetSkillSystem, null, null, null, null, achievementCallback);

            assertEquals(i + 1, librarySystem.getSessionsToday(),
                    "Sessions today should be " + (i + 1) + " after session " + (i + 1));
        }

        // After 3 sessions in one day, BOOKWORM should be awarded
        assertTrue(achievementSystem.isUnlocked(AchievementType.BOOKWORM),
                "BOOKWORM achievement should be awarded after " + LibrarySystem.BOOKWORM_TARGET
                + " sessions in one day");

        // SELF_IMPROVEMENT should be awarded (4 + 3 = 7 >= 5)
        assertTrue(achievementSystem.isUnlocked(AchievementType.SELF_IMPROVEMENT),
                "SELF_IMPROVEMENT achievement should be awarded after "
                + LibrarySystem.SELF_IMPROVEMENT_TARGET + " total sessions");
    }
}
