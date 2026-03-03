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
 * Integration tests for Issue #1473 — Northfield Community Litter Pick:
 * Janet's Tidy Streets, the Quota Dodge &amp; the Crack Pipe Incident.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Signing up before deadline gives equipment.</li>
 *   <li>Collecting 8 litter items and handing in reduces notoriety.</li>
 *   <li>Crack pipe in bag triggers scandal (police, POSSESSION, Notoriety +12).</li>
 *   <li>Pickpocketing a distracted volunteer succeeds at ~60%.</li>
 *   <li>Fly-tipping triggers Environmental Health monitoring and volunteer watcher.</li>
 * </ol>
 */
class Issue1473LitterPickTest {

    private LitterPickSystem litterPick;
    private NotorietySystem notorietySystem;
    private WantedSystem wantedSystem;
    private CriminalRecord criminalRecord;
    private EnvironmentalHealthSystem environmentalHealthSystem;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private RumourNetwork rumourNetwork;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        litterPick = new LitterPickSystem(new Random(42));
        notorietySystem = new NotorietySystem();
        wantedSystem = new WantedSystem(new Random(99));
        criminalRecord = new CriminalRecord();
        environmentalHealthSystem = new EnvironmentalHealthSystem();
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        rumourNetwork = new RumourNetwork(new Random(77));
        inventory = new Inventory(36);

        litterPick.setNotorietySystem(notorietySystem);
        litterPick.setWantedSystem(wantedSystem);
        litterPick.setCriminalRecord(criminalRecord);
        litterPick.setRumourNetwork(rumourNetwork);
        litterPick.setEnvironmentalHealthSystem(environmentalHealthSystem);

        // Start the event with 3 volunteers
        List<NPC> volunteers = new ArrayList<>();
        volunteers.add(new NPC(NPCType.VOLUNTEER_PICKER, "Vol1", 5f, 1f, 5f));
        volunteers.add(new NPC(NPCType.VOLUNTEER_PICKER, "Vol2", 7f, 1f, 5f));
        volunteers.add(new NPC(NPCType.VOLUNTEER_PICKER, "Vol3", 9f, 1f, 5f));
        litterPick.startEvent(volunteers);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Signing up before deadline gives equipment
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Create LitterPickSystem; set time to Saturday 09:10.
     * Call signUp() with notoriety = 0.
     * Verify result == SIGNED_UP.
     * Verify LITTER_PICKER_STICK in inventory.
     * Verify COUNCIL_RUBBISH_BAG in inventory.
     * Also verify late arrival and high-notoriety refusals.
     */
    @Test
    void signUp_beforeDeadline_givesEquipment() {
        float currentHour = 9.10f; // 09:06 — before the 09:15 deadline
        int notoriety = 0;

        LitterPickSystem.SignUpResult result = litterPick.signUp(
                inventory, currentHour, notoriety, achievementCallback);

        assertEquals(LitterPickSystem.SignUpResult.SIGNED_UP, result,
                "Player should be signed up before the 09:15 deadline");
        assertEquals(1, inventory.getItemCount(Material.LITTER_PICKER_STICK),
                "Player should receive LITTER_PICKER_STICK on sign-up");
        assertEquals(1, inventory.getItemCount(Material.COUNCIL_RUBBISH_BAG),
                "Player should receive COUNCIL_RUBBISH_BAG on sign-up");
        assertTrue(litterPick.isPlayerSignedUp(), "isPlayerSignedUp() should be true");
    }

    @Test
    void signUp_afterDeadline_returnsTooLate() {
        float currentHour = 9.30f; // 09:18 — after the 09:15 deadline
        LitterPickSystem.SignUpResult result = litterPick.signUp(
                inventory, currentHour, 0, achievementCallback);
        assertEquals(LitterPickSystem.SignUpResult.TOO_LATE, result,
                "Player after deadline should be turned away");
        assertFalse(litterPick.isPlayerSignedUp(), "isPlayerSignedUp() should remain false");
    }

    @Test
    void signUp_highNotoriety_refusedNotoriety() {
        float currentHour = 9.0f; // on time
        int notoriety = LitterPickSystem.NOTORIETY_REFUSAL_THRESHOLD; // exactly at threshold
        LitterPickSystem.SignUpResult result = litterPick.signUp(
                inventory, currentHour, notoriety, achievementCallback);
        assertEquals(LitterPickSystem.SignUpResult.REFUSED_NOTORIETY, result,
                "Player with notoriety >= threshold should be refused");
        assertFalse(litterPick.isPlayerSignedUp(), "isPlayerSignedUp() should remain false");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Collecting 8 litter items and handing in reduces notoriety
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Sign up the player.
     * Call collectLitterItem() 8 times.
     * Verify playerPickedCount == 8.
     * Call handInBag() and verify HandInResult.QUOTA_MET.
     * Verify notoriety was reduced by GOOD_CITIZEN_NOTORIETY_REDUCTION.
     */
    @Test
    void collectEightItems_handIn_reducesNotoriety() {
        // Sign up first
        litterPick.signUp(inventory, 9.0f, 0, achievementCallback);

        // Seed some notoriety so we can verify reduction
        notorietySystem.addNotoriety(50, achievementCallback);
        int notorietyBefore = notorietySystem.getNotoriety();

        // Collect exactly the target
        for (int i = 0; i < LitterPickSystem.LITTER_TARGET; i++) {
            assertTrue(litterPick.collectLitterItem(),
                    "collectLitterItem() should return true while litter is available");
        }
        assertEquals(LitterPickSystem.LITTER_TARGET, litterPick.getPlayerPickedCount(),
                "Player picked count should equal LITTER_TARGET after " + LitterPickSystem.LITTER_TARGET + " collects");

        // Hand in
        LitterPickSystem.HandInResult result = litterPick.handInBag(inventory, achievementCallback);

        assertEquals(LitterPickSystem.HandInResult.QUOTA_MET, result,
                "HandInResult should be QUOTA_MET when quota is reached");

        int notorietyAfter = notorietySystem.getNotoriety();
        assertEquals(notorietyBefore - LitterPickSystem.GOOD_CITIZEN_NOTORIETY_REDUCTION,
                notorietyAfter,
                "Notoriety should decrease by GOOD_CITIZEN_NOTORIETY_REDUCTION on quota met");

        assertTrue(achievementSystem.isUnlocked(AchievementType.TIDY_STREETS),
                "TIDY_STREETS achievement should be awarded on quota met");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Crack pipe in bag triggers scandal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Sign up player. Add CRACK_PIPE to inventory.
     * Call handInBag(). Verify HandInResult.CRACK_PIPE_INCIDENT.
     * Verify wantedSystem.getWantedStars() >= 2.
     * Verify criminalRecord has POSSESSION.
     * Verify notoriety increased by CRACK_PIPE_NOTORIETY.
     * Verify event is no longer active.
     */
    @Test
    void crackPipeInBag_triggersScandal() {
        // Sign up
        litterPick.signUp(inventory, 9.0f, 0, achievementCallback);

        int notorietyBefore = notorietySystem.getNotoriety();

        // Slip a crack pipe into the bag
        inventory.addItem(Material.CRACK_PIPE, 1);

        // Hand in
        LitterPickSystem.HandInResult result = litterPick.handInBag(inventory, achievementCallback);

        assertEquals(LitterPickSystem.HandInResult.CRACK_PIPE_INCIDENT, result,
                "Handing in a bag with a crack pipe should trigger the incident");

        assertTrue(wantedSystem.getWantedStars() >= LitterPickSystem.CRACK_PIPE_WANTED_STARS,
                "WantedSystem should gain at least " + LitterPickSystem.CRACK_PIPE_WANTED_STARS + " stars");

        assertTrue(criminalRecord.hasCrime(CriminalRecord.CrimeType.POSSESSION),
                "CriminalRecord should contain POSSESSION after crack pipe incident");

        assertEquals(notorietyBefore + LitterPickSystem.CRACK_PIPE_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by CRACK_PIPE_NOTORIETY");

        assertFalse(litterPick.isEventActive(),
                "Event should end after the crack pipe incident");

        assertTrue(litterPick.isCrackPipeIncidentFired(),
                "isCrackPipeIncidentFired() should be true after the incident");

        assertTrue(achievementSystem.isUnlocked(AchievementType.JANET_S_MORNING),
                "JANET_S_MORNING achievement should be awarded");

        // Verify SCANDAL_RUMOUR was seeded
        assertTrue(rumourNetwork.getAllRumourTypes().contains(RumourType.SCANDAL_RUMOUR),
                "SCANDAL_RUMOUR should be seeded in the rumour network");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Pickpocketing a distracted volunteer succeeds at ~60%
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Sign up player. Manually set a volunteer to bent-down state.
     * Call attemptPickpocket() multiple times with a seeded RNG.
     * Verify the outcome distribution is consistent with ~60% success.
     * Also verify WATCHED_AUTO_FAIL works.
     */
    @Test
    void pickpocket_distractedVolunteer_sixtyPercentSuccess() {
        // Sign up
        litterPick.signUp(inventory, 9.0f, 0, achievementCallback);

        LitterPickSystem.VolunteerState vol = litterPick.getVolunteers().get(0);
        vol.isBentDown = true;

        // Run many attempts to check distribution (seeded)
        // Use a fresh system with known seed for determinism
        LitterPickSystem testSystem = new LitterPickSystem(new Random(999));
        testSystem.setNotorietySystem(notorietySystem);
        testSystem.setRumourNetwork(rumourNetwork);
        List<NPC> vols = new ArrayList<>();
        vols.add(new NPC(NPCType.VOLUNTEER_PICKER, "TestVol", 5f, 1f, 5f));
        testSystem.startEvent(vols);
        testSystem.signUp(inventory, 9.0f, 0, achievementCallback);

        LitterPickSystem.VolunteerState testVol = testSystem.getVolunteers().get(0);
        testVol.isBentDown = true;

        int successes = 0;
        int total = 100;
        for (int i = 0; i < total; i++) {
            testVol.isBentDown = true; // reset each time
            LitterPickSystem.PickpocketResult r = testSystem.attemptPickpocket(
                    new Inventory(36), testVol, null);
            if (r == LitterPickSystem.PickpocketResult.SUCCESS) successes++;
        }

        // Expect roughly 60% ± 15% with seed 999 over 100 trials
        assertTrue(successes >= 40 && successes <= 80,
                "Expected ~60% pickpocket success rate over 100 trials, got " + successes + "/100");
    }

    @Test
    void pickpocket_watcherAssigned_autoFails() {
        litterPick.signUp(inventory, 9.0f, 0, achievementCallback);

        // Mark a volunteer as watcher
        LitterPickSystem.VolunteerState vol = litterPick.getVolunteers().get(0);
        vol.isBentDown = true;
        vol.isWatcher = true;

        LitterPickSystem.PickpocketResult result = litterPick.attemptPickpocket(
                inventory, vol, achievementCallback);

        assertEquals(LitterPickSystem.PickpocketResult.WATCHED_AUTO_FAIL, result,
                "Pickpocket should auto-fail when a volunteer watcher is present");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Fly-tipping triggers EHS monitoring and volunteer watcher
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Sign up player. Call dropBinBag() twice.
     * Verify flyTipCount == 2.
     * Verify EnvironmentalHealthSystem.getParkFlyTipCount() == 2.
     * Verify isPlayerBeingWatched() == true (volunteer watcher assigned after 2 fly-tips).
     * Call attemptPickpocket() again; verify result == WATCHED_AUTO_FAIL.
     */
    @Test
    void flyTip_twice_triggersEhsAndWatcher() {
        litterPick.signUp(inventory, 9.0f, 0, achievementCallback);

        assertFalse(litterPick.isPlayerBeingWatched(),
                "Player should not be watched before any fly-tip");
        assertEquals(0, environmentalHealthSystem.getParkFlyTipCount(),
                "EHS park fly-tip count should be 0 initially");

        // First fly-tip
        litterPick.dropBinBag(achievementCallback);
        assertEquals(1, litterPick.getFlyTipCount(), "flyTipCount should be 1 after first drop");
        assertEquals(1, environmentalHealthSystem.getParkFlyTipCount(),
                "EHS should record first park fly-tip");
        assertFalse(litterPick.isPlayerSuspected(),
                "Player should not be suspected after just 1 fly-tip");

        // Second fly-tip — triggers threshold
        litterPick.dropBinBag(achievementCallback);
        assertEquals(2, litterPick.getFlyTipCount(), "flyTipCount should be 2 after second drop");
        assertEquals(2, environmentalHealthSystem.getParkFlyTipCount(),
                "EHS should record second park fly-tip");
        assertTrue(litterPick.isPlayerSuspected(),
                "Player should be suspected after " + LitterPickSystem.FLY_TIP_BEFORE_SUSPECTED + " fly-tips");
        assertTrue(litterPick.isPlayerBeingWatched(),
                "A volunteer watcher should be assigned after threshold fly-tips");

        // Pickpocket should now auto-fail
        LitterPickSystem.VolunteerState vol = litterPick.getVolunteers().get(0);
        vol.isBentDown = true;
        LitterPickSystem.PickpocketResult result = litterPick.attemptPickpocket(
                inventory, vol, achievementCallback);
        assertEquals(LitterPickSystem.PickpocketResult.WATCHED_AUTO_FAIL, result,
                "Pickpocket should auto-fail while a watcher is present");
    }

    @Test
    void flyTip_litterRespawns() {
        litterPick.signUp(inventory, 9.0f, 0, achievementCallback);

        int litterBefore = litterPick.getLitterCount();
        litterPick.dropBinBag(achievementCallback);
        int litterAfter = litterPick.getLitterCount();

        assertEquals(litterBefore + LitterPickSystem.FLY_TIP_LITTER_SPAWN, litterAfter,
                "Fly-tip should respawn " + LitterPickSystem.FLY_TIP_LITTER_SPAWN + " litter props");
    }
}
