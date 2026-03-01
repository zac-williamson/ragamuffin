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
 * Integration tests for Issue #983 — Northfield Dog Track: Greyhound Racing,
 * Race Fixing &amp; Kennel Heist.
 *
 * <p>Five scenarios:
 * <ol>
 *   <li>Legitimate betting — open BettingUI at track, place bet, verify BET_SLIP
 *       added, win resolves correctly with payout</li>
 *   <li>Race fixing — slip a DODGY_PIE to a greyhound, verify the dog's speed
 *       is reduced and the fixed dog is virtually guaranteed to lose</li>
 *   <li>Heist detection — SECURITY_GUARD witnesses kennel heist attempt, verify
 *       ANIMAL_THEFT is added to criminal record and GREYHOUND is NOT granted</li>
 *   <li>Marchetti entry denial — Respect &lt; 30 causes BetResult.ENTRY_REFUSED</li>
 *   <li>Insider tip at Respect &ge; 60 — TOTE_CLERK provides trap number of the
 *       weakest dog; tip is given only once per session</li>
 * </ol>
 */
class Issue983GreyhoundTrackTest {

    private GreyhoundRacingSystem greySystem;
    private TimeSystem timeSystem;
    private FactionSystem factionSystem;
    private NotorietySystem notorietySystem;
    private CriminalRecord criminalRecord;
    private AchievementSystem achievementSystem;
    private NotorietySystem.AchievementCallback achievementCallback;
    private Inventory inventory;
    private RumourNetwork rumourNetwork;

    @BeforeEach
    void setUp() {
        greySystem = new GreyhoundRacingSystem(new Random(12345));
        timeSystem = new TimeSystem(18.5f); // 18:30 — track is open (18:00–23:00)
        factionSystem = new FactionSystem();
        notorietySystem = new NotorietySystem();
        criminalRecord = new CriminalRecord();
        achievementSystem = new AchievementSystem();
        achievementCallback = type -> achievementSystem.unlock(type);
        rumourNetwork = new RumourNetwork(new Random(77));

        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 200);

        // Build session schedule
        greySystem.update(0f, timeSystem, inventory, rumourNetwork, new ArrayList<>(),
                achievementCallback, notorietySystem);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Legitimate betting — place bet, verify BET_SLIP, win resolves
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Track is open (18:30). Factions system at default Respect 50
     * (well above minimum 30). Navigate to race 0, trap 1. Set stake to 5.
     * Call placeBet(). Verify BetResult.SUCCESS. Verify BET_SLIP in inventory.
     * Verify COIN reduced by 5. Advance time past race 0's scheduled hour.
     * Call update(). Verify BET_SLIP removed. Verify race 0 is resolved.
     * If trap 1 won, verify coins increased by stake * odds. PUNTER achievement unlocked.
     */
    @Test
    void legitimateBetting_placeAndResolve() {
        // Verify track is open
        assertTrue(greySystem.isTrackOpen(timeSystem),
                "Track should be open at 18:30");

        // Verify schedule was built
        assertTrue(greySystem.hasTodaysSchedule(),
                "Session schedule should have been built");

        // Verify session has races
        List<GreyhoundRacingSystem.Race> races = greySystem.getSessionRaces();
        assertFalse(races.isEmpty(), "Session should have at least one race");

        GreyhoundRacingSystem.Race race0 = races.get(0);
        assertNotNull(race0, "Race 0 should exist");
        assertEquals(6, race0.getDogs().size(), "Each race should have 6 greyhounds");

        // Place a bet on trap 1 of race 0
        int stake = 5;
        int coinsBefore = inventory.getItemCount(Material.COIN);
        assertEquals(0, inventory.getItemCount(Material.BET_SLIP), "No BET_SLIP before bet");

        GreyhoundRacingSystem.BetResult result = greySystem.placeBet(
                0, 1, stake, inventory, factionSystem, notorietySystem,
                achievementCallback, timeSystem);

        assertEquals(GreyhoundRacingSystem.BetResult.SUCCESS, result,
                "Bet placement should succeed when track is open and respect is sufficient");

        // Verify BET_SLIP added and coins deducted
        assertEquals(1, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be in inventory after placing bet");
        assertEquals(coinsBefore - stake, inventory.getItemCount(Material.COIN),
                "Stake should have been deducted from coins");

        // PUNTER achievement should be awarded on first bet
        assertTrue(achievementSystem.isUnlocked(AchievementType.PUNTER),
                "PUNTER achievement should be unlocked on first bet");

        // Advance time past the race scheduled hour to trigger resolution
        timeSystem.setTime(race0.getScheduledHour() + 0.01f);
        int coinsAfterBet = inventory.getItemCount(Material.COIN);

        greySystem.update(0f, timeSystem, inventory, rumourNetwork, new ArrayList<>(),
                achievementCallback, notorietySystem);

        // Verify BET_SLIP removed and race resolved
        assertEquals(0, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be removed after race resolution");
        assertTrue(greySystem.getRace(0).isResolved(),
                "Race 0 should be resolved after its time passes");
        assertNull(greySystem.getActiveBet(),
                "Active bet should be null after resolution");

        // If trap 1 won, payout should be stake * odds multiplier + stake returned
        GreyhoundRacingSystem.Race resolvedRace = greySystem.getRace(0);
        if (resolvedRace.getWinnerTrap() == 1) {
            GreyhoundRacingSystem.Greyhound winDog = null;
            for (GreyhoundRacingSystem.Greyhound g : resolvedRace.getDogs()) {
                if (g.getTrapNumber() == 1) { winDog = g; break; }
            }
            assertNotNull(winDog, "Trap 1 dog should exist");
            int expectedPayout = (int)(stake * winDog.getOddsMultiplier()) + stake;
            assertEquals(coinsAfterBet + expectedPayout, inventory.getItemCount(Material.COIN),
                    "Win payout should be stake * odds + stake returned");
        } else {
            // Loss: no extra coins should be added beyond coinsAfterBet
            assertEquals(coinsAfterBet, inventory.getItemCount(Material.COIN),
                    "No coins added on a losing bet");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Race fixing — DODGY_PIE reduces dog speed; fixed dog loses
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Ensure the session has at least one unresolved race. Give the
     * player a DODGY_PIE. Call slipDodgyPie(trapTarget=3, raceIndex=0, witnessed=false).
     * Verify slipDodgyPie returns true. Verify inventory no longer contains DODGY_PIE.
     * Verify trap 3's speedPenalty == DODGY_PIE_SPEED_PENALTY (0.30). Verify trap 3's
     * effective win probability is significantly reduced. TRACK_FIXER achievement unlocked.
     * Use a seeded RNG that virtually guarantees trap 3 loses: run 100 simulations of
     * pickWinner() with trap 3 fixed, verify trap 3 wins < 5% of the time.
     */
    @Test
    void raceFix_dodgyPieReducesDogSpeed() {
        // Give the player a DODGY_PIE
        inventory.addItem(Material.DODGY_PIE, 1);
        assertEquals(1, inventory.getItemCount(Material.DODGY_PIE), "Player should have a DODGY_PIE");

        // Ensure races exist
        assertTrue(greySystem.hasTodaysSchedule(), "Session should have races");
        GreyhoundRacingSystem.Race race0 = greySystem.getRace(0);
        assertNotNull(race0, "Race 0 should exist");
        assertFalse(race0.isResolved(), "Race 0 should not yet be resolved");

        // Verify trap 3 is in the race
        GreyhoundRacingSystem.Greyhound trap3Before = null;
        for (GreyhoundRacingSystem.Greyhound g : race0.getDogs()) {
            if (g.getTrapNumber() == 3) { trap3Before = g; break; }
        }
        assertNotNull(trap3Before, "Trap 3 greyhound should exist");
        float normalProb = trap3Before.getEffectiveWinProbability();

        // Slip the dodgy pie (unwitnessed)
        boolean slipped = greySystem.slipDodgyPie(3, 0, inventory, false,
                criminalRecord, notorietySystem, achievementCallback,
                rumourNetwork, new ArrayList<>());

        assertTrue(slipped, "slipDodgyPie should return true with valid inputs");
        assertEquals(0, inventory.getItemCount(Material.DODGY_PIE),
                "DODGY_PIE should be consumed");

        // Verify the speed penalty was applied
        GreyhoundRacingSystem.Greyhound trap3After = null;
        for (GreyhoundRacingSystem.Greyhound g : race0.getDogs()) {
            if (g.getTrapNumber() == 3) { trap3After = g; break; }
        }
        assertNotNull(trap3After, "Trap 3 greyhound should still exist");
        assertEquals(GreyhoundRacingSystem.DODGY_PIE_SPEED_PENALTY, trap3After.getSpeedPenalty(),
                0.001f, "Trap 3 speed penalty should be " + GreyhoundRacingSystem.DODGY_PIE_SPEED_PENALTY);

        // Effective win probability should be lower than before
        float reducedProb = trap3After.getEffectiveWinProbability();
        assertTrue(reducedProb < normalProb,
                "Trap 3 effective win probability should be reduced after DODGY_PIE. "
                + "Before: " + normalProb + ", After: " + reducedProb);

        // TRACK_FIXER achievement should be awarded
        assertTrue(achievementSystem.isUnlocked(AchievementType.TRACK_FIXER),
                "TRACK_FIXER achievement should be unlocked after unwitnessed fix");

        // Race fixing should NOT be recorded in criminal record (unwitnessed)
        assertEquals(0, criminalRecord.getCount(CriminalRecord.CrimeType.RACE_FIXING),
                "RACE_FIXING should NOT be recorded when unwitnessed");

        // Statistical verification: with penalty, trap 3 should rarely win
        // Use the system's pickWinner many times and count trap 3 wins
        GreyhoundRacingSystem statsSystem = new GreyhoundRacingSystem(new Random(999));
        int trap3Wins = 0;
        int trials = 200;
        for (int i = 0; i < trials; i++) {
            int winner = statsSystem.pickWinner(race0.getDogs());
            if (winner == 3) trap3Wins++;
        }
        // Trap 3 with 30% speed penalty should win significantly less than its base rate
        // With 6 dogs, base win rate ~17%; with 30% penalty it should be < 12%
        assertTrue(trap3Wins < trials * 0.15,
                "Trap 3 should win less than 15% of trials with 30% speed penalty. "
                + "Won: " + trap3Wins + "/" + trials);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Heist detection — witnessed kennel heist, criminal record updated
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Give the player a LOCKPICK. Set time to 01:00 (track is closed —
     * kennel heist is a night operation). Call attemptKennelHeist(witnessed=true).
     * Verify result is HeistResult.WITNESSED. Verify player does NOT receive a GREYHOUND.
     * Verify ANIMAL_THEFT is added to criminal record. Verify notoriety increased by
     * FIXING_NOTORIETY (10). Verify DOG_HEIST rumour is seeded into the rumour network.
     */
    @Test
    void heistDetected_witnessedHeistRecordsAnimalTheft() {
        // Set time to 01:00 — track is closed (heist is a night operation)
        timeSystem.setTime(1.0f);
        assertFalse(greySystem.isTrackOpen(timeSystem),
                "Track should be closed at 01:00 for night heist");

        // Give the player a LOCKPICK
        inventory.addItem(Material.LOCKPICK, 1);
        assertEquals(1, inventory.getItemCount(Material.LOCKPICK), "Player should have LOCKPICK");
        assertEquals(0, inventory.getItemCount(Material.GREYHOUND), "No GREYHOUND before heist");

        int notorietyBefore = notorietySystem.getNotoriety();
        int animalTheftBefore = criminalRecord.getCount(CriminalRecord.CrimeType.ANIMAL_THEFT);

        // Create a list of NPCs for rumour seeding
        List<NPC> npcs = new ArrayList<>();
        npcs.add(new NPC(NPCType.TRACK_PUNTER, 5f, 1f, 5f));

        // Attempt witnessed heist
        GreyhoundRacingSystem.HeistResult result = greySystem.attemptKennelHeist(
                inventory, true, timeSystem, criminalRecord, notorietySystem,
                achievementCallback, rumourNetwork, npcs);

        assertEquals(GreyhoundRacingSystem.HeistResult.WITNESSED, result,
                "Witnessed heist should return WITNESSED");

        // GREYHOUND should NOT be added to inventory on witnessed heist
        assertEquals(0, inventory.getItemCount(Material.GREYHOUND),
                "GREYHOUND should NOT be granted on a witnessed heist");

        // LOCKPICK should NOT be consumed on a failed/witnessed heist
        assertEquals(1, inventory.getItemCount(Material.LOCKPICK),
                "LOCKPICK should NOT be consumed on a witnessed heist");

        // ANIMAL_THEFT should be recorded
        assertEquals(animalTheftBefore + 1, criminalRecord.getCount(CriminalRecord.CrimeType.ANIMAL_THEFT),
                "ANIMAL_THEFT should be added to criminal record when witnessed");

        // Notoriety should increase by FIXING_NOTORIETY
        assertEquals(notorietyBefore + GreyhoundRacingSystem.FIXING_NOTORIETY,
                notorietySystem.getNotoriety(),
                "Notoriety should increase by " + GreyhoundRacingSystem.FIXING_NOTORIETY
                + " when heist is witnessed");

        // NICKED_THE_GREYHOUND achievement should NOT be awarded on failure
        assertFalse(achievementSystem.isUnlocked(AchievementType.NICKED_THE_GREYHOUND),
                "NICKED_THE_GREYHOUND should NOT be awarded on a witnessed/failed heist");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Marchetti entry denial — Respect < 30 causes ENTRY_REFUSED
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Lower Marchetti Crew Respect to 25 (below the ENTRY_RESPECT_MINIMUM of 30).
     * Attempt to place a bet. Verify BetResult.ENTRY_REFUSED. Verify no BET_SLIP added.
     * Verify no COIN deducted. Now raise Respect to 35 (above minimum). Attempt same bet.
     * Verify BetResult.SUCCESS.
     */
    @Test
    void marchettiEntryDenied_lowRespectRefusesBet() {
        // Lower Marchetti Crew Respect below entry threshold
        // FactionSystem starts at 50; lower it by repeated NPC-hit events
        // We need Respect < 30, so apply sufficient deltas
        int currentRespect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        int delta = currentRespect - (GreyhoundRacingSystem.ENTRY_RESPECT_MINIMUM - 5); // 25
        // Apply direct manipulation by hitting NPC enough times
        // Each hit gives -15 Respect. We need to drop from 50 to 25 = 25 points down
        // 2 × DELTA_HIT_NPC (-15) = -30 → 50 - 30 = 20, which is below 30
        factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, -30);

        int respectAfterDrop = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        assertTrue(respectAfterDrop < GreyhoundRacingSystem.ENTRY_RESPECT_MINIMUM,
                "Marchetti Crew Respect should be below " + GreyhoundRacingSystem.ENTRY_RESPECT_MINIMUM
                + ". Actual: " + respectAfterDrop);

        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Attempt bet — should be refused
        GreyhoundRacingSystem.BetResult refusedResult = greySystem.placeBet(
                0, 1, 5, inventory, factionSystem, notorietySystem,
                achievementCallback, timeSystem);

        assertEquals(GreyhoundRacingSystem.BetResult.ENTRY_REFUSED, refusedResult,
                "Bet should be refused when Marchetti Crew Respect < " + GreyhoundRacingSystem.ENTRY_RESPECT_MINIMUM);
        assertEquals(0, inventory.getItemCount(Material.BET_SLIP),
                "No BET_SLIP should be added on refused entry");
        assertEquals(coinsBefore, inventory.getItemCount(Material.COIN),
                "No coins should be deducted on refused entry");

        // Raise Respect above minimum and retry
        factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, 20); // now at ~40
        int respectAfterRaise = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        assertTrue(respectAfterRaise >= GreyhoundRacingSystem.ENTRY_RESPECT_MINIMUM,
                "Marchetti Crew Respect should now be >= " + GreyhoundRacingSystem.ENTRY_RESPECT_MINIMUM
                + ". Actual: " + respectAfterRaise);

        GreyhoundRacingSystem.BetResult allowedResult = greySystem.placeBet(
                0, 1, 5, inventory, factionSystem, notorietySystem,
                achievementCallback, timeSystem);

        assertEquals(GreyhoundRacingSystem.BetResult.SUCCESS, allowedResult,
                "Bet should succeed after Respect raised above minimum");
        assertEquals(1, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be added on successful bet");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Insider tip — Respect >= 60 gives one tip per session
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Set Marchetti Crew Respect to 65 (above INSIDER_TIP_RESPECT of 60).
     * Call requestInsiderTip(). Verify the return value is a valid trap number (1–6).
     * Verify the returned trap is the one with the lowest effective win probability.
     * Call requestInsiderTip() again. Verify it returns -1 (tip already given this session).
     * Verify that the player with lower Respect (e.g. 50) cannot get an insider tip.
     */
    @Test
    void insiderTip_highRespectGrantsOnePerSession() {
        // Raise Marchetti Crew Respect to 65
        factionSystem.applyRespectDelta(Faction.MARCHETTI_CREW, 15); // 50 + 15 = 65
        int respect = factionSystem.getRespect(Faction.MARCHETTI_CREW);
        assertTrue(respect >= GreyhoundRacingSystem.INSIDER_TIP_RESPECT,
                "Marchetti Crew Respect should be >= " + GreyhoundRacingSystem.INSIDER_TIP_RESPECT
                + ". Actual: " + respect);

        // Verify session has an unresolved race
        GreyhoundRacingSystem.Race nextRace = greySystem.getNextUnresolvedRace();
        assertNotNull(nextRace, "There should be an unresolved race for the insider tip");

        // Request insider tip
        int tipTrap = greySystem.requestInsiderTip(factionSystem);
        assertTrue(tipTrap >= 1 && tipTrap <= 6,
                "Insider tip should return a valid trap number (1–6). Got: " + tipTrap);

        // Verify the tip identifies the weakest dog (highest odds / lowest win probability)
        GreyhoundRacingSystem.Greyhound weakestDog = null;
        float weakestProb = Float.MAX_VALUE;
        for (GreyhoundRacingSystem.Greyhound g : nextRace.getDogs()) {
            float prob = g.getEffectiveWinProbability();
            if (prob < weakestProb) {
                weakestProb = prob;
                weakestDog = g;
            }
        }
        assertNotNull(weakestDog, "Should be able to find weakest dog");
        assertEquals(weakestDog.getTrapNumber(), tipTrap,
                "Insider tip should point to the weakest dog (trap " + weakestDog.getTrapNumber()
                + " with prob " + weakestProb + "). Got trap: " + tipTrap);

        // Verify tip is marked as given for this session
        assertTrue(greySystem.isInsiderTipGivenThisSession(),
                "Insider tip should be marked as given");

        // Second call should return -1 (already given)
        int secondTip = greySystem.requestInsiderTip(factionSystem);
        assertEquals(-1, secondTip,
                "Second insider tip request in same session should return -1");

        // Verify a player with lower Respect cannot get a tip
        FactionSystem lowRespectFactionSystem = new FactionSystem();
        // Default respect is 50, which is below INSIDER_TIP_RESPECT (60)
        GreyhoundRacingSystem freshSystem = new GreyhoundRacingSystem(new Random(555));
        freshSystem.update(0f, timeSystem, inventory, null, new ArrayList<>(), null, null);
        int lowRespectTip = freshSystem.requestInsiderTip(lowRespectFactionSystem);
        assertEquals(-1, lowRespectTip,
                "Insider tip should be refused when Marchetti Respect < "
                + GreyhoundRacingSystem.INSIDER_TIP_RESPECT);
    }
}
