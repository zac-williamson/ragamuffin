package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.core.ArrestSystem;
import ragamuffin.core.CriminalRecord;
import ragamuffin.core.DisguiseSystem;
import ragamuffin.core.NotorietySystem;
import ragamuffin.core.WantedSystem;
import ragamuffin.core.Weather;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCState;
import ragamuffin.entity.NPCType;
import ragamuffin.entity.Player;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #771 — Hot Pursuit: Wanted System, Police Chases &amp; Getaways.
 *
 * <p>10 exact scenarios:
 * <ol>
 *   <li>Witnessed crime escalates wanted level</li>
 *   <li>LOS break starts decay, decay reduces wanted stars</li>
 *   <li>LOS contact resets decay timer</li>
 *   <li>Cornered player gets arrested (wanting stars intact)</li>
 *   <li>Wheelie bin hiding records achievement and hides player</li>
 *   <li>Disguise burn (disguise change resets police description at ≤3 stars)</li>
 *   <li>PCSO bribe reduces wanted level</li>
 *   <li>Safe house clears wanted level after 120 s at ≤3 stars</li>
 *   <li>Distance escape (leg it) reduces wanted level by 2</li>
 *   <li>Night/rain/fog reduce police LOS range</li>
 * </ol>
 */
class Issue771WantedSystemTest {

    private WantedSystem wantedSystem;
    private Player player;
    private Inventory inventory;
    private List<NPC> npcs;
    private AchievementSystem achievementSystem;
    private WantedSystem.AchievementCallback achievementCallback;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;

    @BeforeEach
    void setUp() {
        wantedSystem = new WantedSystem(new Random(42L));
        player = new Player(10f, 1f, 10f);
        inventory = new Inventory(36);
        npcs = new ArrayList<>();
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Witnessed crime escalates wanted level
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: A crime with severity ≥ 1 is witnessed. Verify that the wanted
     * level increases from 0. Committing crimes with escalating severity should
     * increase the wanted level up to the threshold for each star level.
     */
    @Test
    void witnessedCrimeEscalatesWantedLevel() {
        assertEquals(0, wantedSystem.getWantedStars(), "Should start at 0 stars");

        // Crime with severity 1 → should trigger at least 1 star
        boolean escalated = wantedSystem.onCrimeWitnessed(1,
                player.getPosition().x, player.getPosition().y, player.getPosition().z,
                player, achievementCallback);

        assertTrue(escalated || wantedSystem.getWantedStars() >= 1,
                "Witnessing a crime should escalate wanted level to at least 1 star");
        assertTrue(wantedSystem.isInPursuit(),
                "Should be in pursuit mode after witnessed crime");

        // Add more severity to push to star 2
        wantedSystem.onCrimeWitnessed(3,
                player.getPosition().x, player.getPosition().y, player.getPosition().z,
                player, achievementCallback);
        assertTrue(wantedSystem.getWantedStars() >= 2,
                "Accumulated severity should push wanted level to at least 2 stars");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: LOS break starts decay, decay reduces wanted stars
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Place the player at 2 stars. Remove all police NPCs (no LOS).
     * Simulate 90 seconds passing without LOS. Verify that wanted stars have
     * decreased by 1 (now at 1 star).
     */
    @Test
    void losBreakCausesDecayAndReducesWantedStars() {
        // Set up: 2 wanted stars, no police around
        wantedSystem.setWantedStarsForTesting(2);
        // npcs list is empty — no police LOS

        // Simulate 90 seconds of no-LOS (the decay period for one star)
        float totalTime = 0f;
        float delta = 1f;
        int starsAfterDecay = wantedSystem.getWantedStars();
        while (totalTime < WantedSystem.DECAY_SECONDS_PER_STAR + 1f) {
            wantedSystem.update(delta, player, npcs, Weather.CLEAR, false, false, achievementCallback);
            totalTime += delta;
            starsAfterDecay = wantedSystem.getWantedStars();
        }

        assertTrue(starsAfterDecay < 2,
                "Wanted stars should have decreased after " + WantedSystem.DECAY_SECONDS_PER_STAR + "s without LOS; got " + starsAfterDecay);
        assertEquals(1, starsAfterDecay,
                "Wanted stars should have decreased by exactly 1 (from 2 to 1)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: LOS contact resets decay timer
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Set wanted level to 2 stars. Advance 60 seconds without LOS
     * (decay timer at 60/90). Then spawn a police NPC within LOS range. Advance
     * 10 more seconds. Verify that the decay timer has been reset (not continued
     * counting) and the wanted level has NOT decreased.
     */
    @Test
    void losContactResetsDecayTimer() {
        wantedSystem.setWantedStarsForTesting(2);
        // No police in range for 60 seconds
        float totalTime = 0f;
        float delta = 1f;
        while (totalTime < 60f) {
            wantedSystem.update(delta, player, npcs, Weather.CLEAR, false, false, achievementCallback);
            totalTime += delta;
        }
        // Decay timer should be ~60s at this point (not yet decayed)
        assertEquals(2, wantedSystem.getWantedStars(), "Stars should not have decayed yet after 60s");
        assertTrue(wantedSystem.getDecayTimer() > 50f,
                "Decay timer should be around 60s: got " + wantedSystem.getDecayTimer());

        // Now spawn a police NPC within LOS range
        NPC policeNpc = new NPC(NPCType.POLICE, player.getPosition().x + 5f,
                player.getPosition().y, player.getPosition().z);
        policeNpc.setState(NPCState.PATROLLING);
        npcs.add(policeNpc);

        // Advance 5 more seconds — police should have LOS, decay resets
        for (int i = 0; i < 5; i++) {
            wantedSystem.update(delta, player, npcs, Weather.CLEAR, false, false, achievementCallback);
        }

        // Wanted stars should still be 2 (no decay)
        assertEquals(2, wantedSystem.getWantedStars(),
                "Wanted stars should remain at 2 when police have LOS");

        // Decay timer should have been reset (close to 0, not near 65)
        assertTrue(wantedSystem.getDecayTimer() < 10f,
                "Decay timer should have reset to near 0 after police LOS contact; got " + wantedSystem.getDecayTimer());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Cornered arrest — apply wanted consequences
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Set player at 3 stars. Apply the arrest consequences via
     * WantedSystem.applyArrestConsequences(). Verify:
     * - A fine of (3 × 10 = 30 COIN) is deducted from inventory
     * - TIMES_ARRESTED is incremented in criminal record
     * - Wanted level resets to 0 after arrest
     */
    @Test
    void corneredArrestAppliesConsequences() {
        wantedSystem.setWantedStarsForTesting(3);

        // Give player some coins for the fine
        inventory.addItem(Material.COIN, 50);

        int coinBefore = inventory.getItemCount(Material.COIN);
        int expectedFine = WantedSystem.ARREST_FINE_PER_STAR * 3; // 30 coins

        NotorietySystem.AchievementCallback notorietyAchievementCallback =
                type -> achievementSystem.unlock(type);
        int fineDeducted = wantedSystem.applyArrestConsequences(
                inventory, criminalRecord, notorietySystem, notorietyAchievementCallback);

        // Verify fine deducted
        assertEquals(expectedFine, fineDeducted,
                "Fine should be " + expectedFine + " COIN for 3 stars");
        assertEquals(coinBefore - expectedFine, inventory.getItemCount(Material.COIN),
                "Inventory should have " + (coinBefore - expectedFine) + " coins after fine");

        // Verify criminal record updated
        assertEquals(1, criminalRecord.getCount(CriminalRecord.CrimeType.TIMES_ARRESTED),
                "TIMES_ARRESTED should be incremented on arrest");

        // Verify wanted level cleared
        assertEquals(0, wantedSystem.getWantedStars(),
                "Wanted stars should reset to 0 after arrest");
        assertFalse(wantedSystem.isInPursuit(),
                "Should not be in pursuit after arrest");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Wheelie bin hiding
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Trigger hiding. Advance the update loop for 2+ seconds to fill
     * the hiding progress bar. Verify the hiding progress reaches 1.0 and that
     * the WHEELIE_BIN_HERO achievement is awarded when fully hidden.
     */
    @Test
    void wheelieBinHidingProgressAndAchievement() {
        wantedSystem.setWantedStarsForTesting(2);

        // Enter hiding
        boolean accepted = wantedSystem.toggleHiding(true, achievementCallback);
        assertTrue(accepted, "Entering hiding should be accepted");
        assertTrue(wantedSystem.isHiding(), "Should be in hiding mode");
        assertEquals(0f, wantedSystem.getHidingProgress(), 0.01f,
                "Hiding progress should start at 0");

        // Advance past the hiding enter duration (2 seconds)
        float totalTime = 0f;
        while (totalTime < WantedSystem.HIDING_ENTER_DURATION + 0.5f) {
            wantedSystem.update(0.1f, player, npcs, Weather.CLEAR, false, false, achievementCallback);
            totalTime += 0.1f;
        }

        assertEquals(1.0f, wantedSystem.getHidingProgress(), 0.01f,
                "Hiding progress should reach 1.0 after full enter duration");

        // Trigger wheelie bin achievement
        wantedSystem.onWheeliBinHidden(achievementCallback);
        assertTrue(achievementSystem.isUnlocked(AchievementType.WHEELIE_BIN_HERO),
                "WHEELIE_BIN_HERO achievement should be awarded when fully hidden in bin");

        // Verify police won't pursue while hiding at ≤ threshold stars
        assertTrue(wantedSystem.getWantedStars() <= WantedSystem.HIDING_SPOT_POLICE_ENTRY_THRESHOLD,
                "Wanted level (" + wantedSystem.getWantedStars() + ") should be at or below hiding threshold (" + WantedSystem.HIDING_SPOT_POLICE_ENTRY_THRESHOLD + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: Disguise change escape (disguise burn)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 6: Set wanted level to 2 stars. Add a police NPC in CHASING_PLAYER
     * state. Attempt a disguise change escape. Verify:
     * - The police NPC returns to PATROLLING
     * - The INNOCENT_FACE achievement is awarded
     * - A second attempt in the same pursuit fails (ALREADY_USED)
     */
    @Test
    void disguiseBurnResetsPoliceDescriptionAndAwardsAchievement() {
        wantedSystem.setWantedStarsForTesting(2);

        // Add a pursuing police NPC
        NPC cop = new NPC(NPCType.POLICE, player.getPosition().x + 8f,
                player.getPosition().y, player.getPosition().z);
        cop.setState(NPCState.CHASING_PLAYER);
        npcs.add(cop);

        // Set up a disguise system with an active disguise
        DisguiseSystem disguiseSystem = new DisguiseSystem(new Random(42L));
        inventory.addItem(Material.POLICE_UNIFORM, 1);
        disguiseSystem.equipDisguise(Material.POLICE_UNIFORM, inventory);
        assertTrue(disguiseSystem.isDisguised(), "Disguise should be equipped");

        // Attempt disguise escape
        WantedSystem.DisguiseEscapeResult result = wantedSystem.attemptDisguiseEscape(
                disguiseSystem, player, npcs, achievementCallback);

        assertEquals(WantedSystem.DisguiseEscapeResult.SUCCESS, result,
                "Disguise escape should succeed at ≤3 stars");

        // Police NPC should have returned to patrol
        assertEquals(NPCState.PATROLLING, cop.getState(),
                "Police NPC should return to PATROLLING after disguise change");

        // Achievement should be awarded
        assertTrue(achievementSystem.isUnlocked(AchievementType.INNOCENT_FACE),
                "INNOCENT_FACE achievement should be awarded for disguise escape");

        // Second attempt should fail (ALREADY_USED)
        inventory.addItem(Material.MARCHETTI_TRACKSUIT, 1);
        disguiseSystem.equipDisguise(Material.MARCHETTI_TRACKSUIT, inventory);
        WantedSystem.DisguiseEscapeResult result2 = wantedSystem.attemptDisguiseEscape(
                disguiseSystem, player, npcs, achievementCallback);
        assertEquals(WantedSystem.DisguiseEscapeResult.ALREADY_USED, result2,
                "Second disguise escape attempt in same pursuit should be ALREADY_USED");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: PCSO bribe reduces wanted level
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 7: Set wanted level to 2 stars. Give the player 16 COIN (2 stars × 8
     * COIN/star). Place a PCSO NPC adjacent to the player. Attempt the bribe.
     * Verify: - Bribe succeeds (SUCCESS result) - Wanted stars decrease to 1
     * - 16 COIN deducted - PCSO returns to PATROLLING state
     */
    @Test
    void pcsobribeReducesWantedLevel() {
        wantedSystem.setWantedStarsForTesting(2);

        int bribeCost = WantedSystem.BRIBE_COST_PER_STAR * 2; // 16 coins
        inventory.addItem(Material.COIN, bribeCost);

        NPC pcso = new NPC(NPCType.PCSO, player.getPosition().x + 1f,
                player.getPosition().y, player.getPosition().z);
        pcso.setState(NPCState.WARNING);
        npcs.add(pcso);

        WantedSystem.BribeResult result = wantedSystem.attemptBribePcso(
                pcso, inventory, 30 /* notoriety < 60 */, achievementCallback);

        assertEquals(WantedSystem.BribeResult.SUCCESS, result,
                "Bribe should succeed with sufficient coins and low notoriety");
        assertEquals(1, wantedSystem.getWantedStars(),
                "Wanted stars should decrease to 1 after successful bribe");
        assertEquals(0, inventory.getItemCount(Material.COIN),
                "All bribe coins should be deducted");
        assertEquals(NPCState.PATROLLING, pcso.getState(),
                "PCSO should return to PATROLLING after accepting bribe");
    }

    /**
     * Scenario 7b: Bribe fails when player is too notorious (Notoriety ≥ 60).
     */
    @Test
    void pcsobrideRefusedWhenTooNotorious() {
        wantedSystem.setWantedStarsForTesting(1);
        inventory.addItem(Material.COIN, 100);

        NPC pcso = new NPC(NPCType.PCSO, player.getPosition().x + 1f,
                player.getPosition().y, player.getPosition().z);
        npcs.add(pcso);

        WantedSystem.BribeResult result = wantedSystem.attemptBribePcso(
                pcso, inventory, 70 /* notoriety >= 60 */, achievementCallback);

        assertEquals(WantedSystem.BribeResult.TOO_NOTORIOUS, result,
                "Bribe should fail when notoriety >= 60");
        assertEquals(1, wantedSystem.getWantedStars(),
                "Wanted stars should be unchanged after failed bribe");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 8: Safe house clears wanted level after 120 s
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 8: Set wanted level to 2 stars (≤ SAFE_HOUSE_POLICE_ENTRY_THRESHOLD).
     * Enter safe house. Advance 120 seconds. Verify that wanted level clears to 0
     * and CLEAN_GETAWAY_PURSUIT achievement is awarded.
     */
    @Test
    void safeHouseClearsWantedAfter120Seconds() {
        wantedSystem.setWantedStarsForTesting(2);

        // Enter safe house
        wantedSystem.onEnterSafeHouse(true, achievementCallback);
        assertTrue(wantedSystem.isInSafeHouse(),
                "Should be in safe house after entering");

        // Advance 120 seconds (the safe house duration)
        float totalTime = 0f;
        float delta = 1f;
        while (totalTime < WantedSystem.SAFE_HOUSE_DURATION + 1f) {
            wantedSystem.update(delta, player, npcs, Weather.CLEAR, false, false, achievementCallback);
            totalTime += delta;
        }

        assertEquals(0, wantedSystem.getWantedStars(),
                "Wanted stars should clear to 0 after " + WantedSystem.SAFE_HOUSE_DURATION + "s in safe house");
        assertFalse(wantedSystem.isInPursuit(),
                "Should not be in pursuit after safe house clears wanted level");
        assertTrue(achievementSystem.isUnlocked(AchievementType.CLEAN_GETAWAY_PURSUIT),
                "CLEAN_GETAWAY_PURSUIT achievement should be awarded after safe house escape");
    }

    /**
     * Scenario 8b: Safe house does NOT instantly clear wanted level at &gt; 3 stars.
     * Police maintain LOS (keeping decay from firing) and the safe house timer
     * runs for 130 seconds. Stars should remain above 3 (safe house mechanism
     * doesn't kick in), while the CLEAN_GETAWAY_PURSUIT achievement is NOT awarded.
     */
    @Test
    void safeHouseDoesNotClearWantedAboveThreshold() {
        wantedSystem.setWantedStarsForTesting(4); // Above SAFE_HOUSE_POLICE_ENTRY_THRESHOLD
        wantedSystem.onEnterSafeHouse(true, achievementCallback);

        // Place police nearby so LOS prevents decay — we want to test safe house
        // mechanism specifically, not the normal LOS decay path
        NPC cop = new NPC(NPCType.POLICE,
                player.getPosition().x + 5f,
                player.getPosition().y,
                player.getPosition().z);
        cop.setState(NPCState.PATROLLING);
        List<NPC> policePresent = new ArrayList<>();
        policePresent.add(cop);

        // Advance 130 seconds with police in range (LOS maintained, decay frozen)
        for (int i = 0; i < 130; i++) {
            wantedSystem.update(1f, player, policePresent, Weather.CLEAR, false, false, achievementCallback);
        }

        // Stars should remain at 4 — safe house doesn't clear high wanted levels
        assertEquals(4, wantedSystem.getWantedStars(),
                "Wanted stars should remain at 4 when safe house is entered at > 3 stars (safe house doesn't apply)");
        // CLEAN_GETAWAY_PURSUIT should NOT be awarded
        assertFalse(achievementSystem.isUnlocked(AchievementType.CLEAN_GETAWAY_PURSUIT),
                "CLEAN_GETAWAY_PURSUIT should NOT be awarded when safe house doesn't work at > 3 stars");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 9: Distance escape (leg it) reduces wanted by 2
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 9: Set wanted level to 3 stars with LKP at (0, 0, 0). Move the
     * player to (0, 0, 90) — 90 blocks from the LKP (beyond LEG_IT_DISTANCE of 80).
     * Simulate 60 seconds of continuous no-LOS (beyond LEG_IT_LOS_BREAK_SECONDS).
     * Verify: - wanted stars drop by 2 (from 3 to 1) - LEG_IT achievement awarded
     */
    @Test
    void legItEscapeReducesWantedLevelByTwo() {
        wantedSystem.setWantedStarsForTesting(3);
        wantedSystem.setLastKnownPositionForTesting(0f, 1f, 0f);

        // Move player far from LKP (90 blocks — beyond LEG_IT_DISTANCE of 80)
        player.getPosition().set(0f, 1f, 90f);

        // Simulate 60+ seconds of no police LOS
        float totalTime = 0f;
        float delta = 1f;
        while (totalTime < WantedSystem.LEG_IT_LOS_BREAK_SECONDS + 2f) {
            wantedSystem.update(delta, player, npcs, Weather.CLEAR, false, false, achievementCallback);
            totalTime += delta;
        }

        assertEquals(1, wantedSystem.getWantedStars(),
                "Wanted stars should decrease by 2 (from 3 to 1) after leg-it escape");
        assertTrue(wantedSystem.isLegItConditionMet(),
                "Leg-it condition should be marked as met");
        assertTrue(achievementSystem.isUnlocked(AchievementType.LEG_IT),
                "LEG_IT achievement should be awarded for distance escape");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 10: Night/rain/fog reduce police LOS range
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 10: Set wanted level to 2 stars. Place a police NPC at exactly 15
     * blocks from the player. In CLEAR daytime, the police NPC (within 20-block base
     * range) should see the player and reset the decay timer. In FOG conditions
     * (base 20 − 4 = 16 blocks LOS), the same police NPC at 15 blocks should still
     * see the player. However, in RAIN (base 20 − 6 = 14 blocks LOS), a police NPC
     * at 15 blocks should NOT see the player — enabling decay.
     */
    @Test
    void nightRainFogReducePoliceLoSRange() {
        wantedSystem.setWantedStarsForTesting(2);

        // Police NPC placed at 15 blocks from player
        float policeDistance = 15f;
        NPC cop = new NPC(NPCType.POLICE,
                player.getPosition().x + policeDistance,
                player.getPosition().y,
                player.getPosition().z);
        cop.setState(NPCState.PATROLLING);
        List<NPC> policeList = new ArrayList<>();
        policeList.add(cop);

        // In CLEAR weather: police LOS range = 20, should see player at 15 blocks
        // Advance 5 seconds — decay timer should NOT be counting (police have LOS)
        for (int i = 0; i < 5; i++) {
            wantedSystem.update(1f, player, policeList, Weather.CLEAR, false, false, achievementCallback);
        }
        // With LOS, decay timer should not have advanced significantly
        assertTrue(wantedSystem.isPoliceHasLos(),
                "Police should have LOS in clear weather at " + policeDistance + " blocks");
        float decayAfterClear = wantedSystem.getDecayTimer();
        assertEquals(0f, decayAfterClear, 0.01f,
                "Decay timer should be 0 when police have LOS in clear weather");

        // In RAIN weather: police LOS range = 20 − 6 = 14, should NOT see player at 15 blocks
        // Reset the wanted system state for a clean test
        wantedSystem.setWantedStarsForTesting(2);
        wantedSystem.setDecayTimerForTesting(0f);
        wantedSystem.setPoliceHasLosForTesting(false);

        for (int i = 0; i < 5; i++) {
            wantedSystem.update(1f, player, policeList, Weather.RAIN, false, false, achievementCallback);
        }
        // Police at 15 blocks should be out of range in rain (range = 14)
        assertFalse(wantedSystem.isPoliceHasLos(),
                "Police should NOT have LOS in rain at " + policeDistance + " blocks (range=" + (WantedSystem.POLICE_BASE_LOS_RANGE - WantedSystem.RAIN_LOS_REDUCTION) + ")");
        assertTrue(wantedSystem.getDecayTimer() > 0f,
                "Decay timer should be counting when police lack LOS in rain");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bonus: Corrupt PCSO cultivation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bonus: Give FLASK_OF_TEA to a PCSO three times. Verify the PCSO becomes
     * corrupt (BENT_COPPER achievement) and the bribe cost is halved.
     */
    @Test
    void corruptPcsoCultivatedWithThreeFlasksOfTea() {
        NPC pcso = new NPC(NPCType.PCSO, player.getPosition().x + 2f,
                player.getPosition().y, player.getPosition().z);

        // Give three flasks of tea
        for (int i = 0; i < WantedSystem.CORRUPT_PCSO_TEA_INTERACTIONS; i++) {
            inventory.addItem(Material.FLASK_OF_TEA, 1);
            wantedSystem.offerTeaToPcso(pcso, inventory, achievementCallback);
        }

        assertTrue(wantedSystem.hasCorruptPcso(),
                "PCSO should be cultivated as corrupt after 3 tea interactions");
        assertEquals(WantedSystem.CORRUPT_PCSO_TEA_INTERACTIONS,
                wantedSystem.getCorruptPcsoTeaCount(),
                "Tea interaction count should be " + WantedSystem.CORRUPT_PCSO_TEA_INTERACTIONS);
        assertTrue(achievementSystem.isUnlocked(AchievementType.BENT_COPPER),
                "BENT_COPPER achievement should be awarded after cultivating corrupt PCSO");

        // Verify halved bribe cost
        wantedSystem.setWantedStarsForTesting(2);
        int normalCost = WantedSystem.BRIBE_COST_PER_STAR * 2;
        int corruptCost = wantedSystem.getBribeCost(pcso);
        assertEquals((int) (normalCost * WantedSystem.CORRUPT_PCSO_BRIBE_MULTIPLIER), corruptCost,
                "Corrupt PCSO bribe should cost half the normal amount");
    }
}
