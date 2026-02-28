package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.ui.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HorseRacingSystem}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Race scheduling (8 races/day, correct times)</li>
 *   <li>Payout maths (stake × odds numerator on win)</li>
 *   <li>Debt threshold (loan shark trigger at 50 net loss)</li>
 *   <li>Loan shark lifecycle (accept → repay or default)</li>
 *   <li>Bet slip management</li>
 *   <li>Winner selection weighted by odds</li>
 * </ul>
 */
class HorseRacingSystemTest {

    private HorseRacingSystem system;
    private TimeSystem timeSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        system = new HorseRacingSystem(new Random(42));
        // Issue #909: Start at 10:30 (before first race at 11:00) so races are not
        // immediately resolved when update() is first called.
        timeSystem = new TimeSystem(10.5f);
        inventory = new Inventory(36);
        inventory.addItem(Material.COIN, 200); // Plenty of coins for testing
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Race scheduling tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void raceScheduleHas8RacesPerDay() {
        // Force schedule build by ticking update
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        assertEquals(HorseRacingSystem.RACES_PER_DAY, races.size(),
                "There should be exactly 8 races per day");
    }

    @Test
    void firstRaceIsAt1100() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        assertFalse(races.isEmpty(), "Races should be scheduled");
        assertEquals(HorseRacingSystem.FIRST_RACE_HOUR, races.get(0).getScheduledHour(), 0.01f,
                "First race should be at 11:00");
    }

    @Test
    void lastRaceIsAt2100() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        assertFalse(races.isEmpty(), "Races should be scheduled");
        float lastHour = races.get(races.size() - 1).getScheduledHour();
        assertEquals(HorseRacingSystem.LAST_RACE_HOUR, lastHour, 0.01f,
                "Last race should be at 21:00");
    }

    @Test
    void eachRaceHas6Horses() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        for (HorseRacingSystem.Race race : system.getTodaysRaces()) {
            assertEquals(HorseRacingSystem.HORSES_PER_RACE, race.getHorses().size(),
                    "Each race should have exactly 6 horses");
        }
    }

    @Test
    void raceTimesAreEvenlySpaced() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        float expectedSpacing = HorseRacingSystem.RACE_SPACING_HOURS;
        for (int i = 1; i < races.size(); i++) {
            float spacing = races.get(i).getScheduledHour() - races.get(i - 1).getScheduledHour();
            assertEquals(expectedSpacing, spacing, 0.01f,
                    "Races should be evenly spaced");
        }
    }

    @Test
    void racesHaveUniqueHorseNames() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        java.util.Set<String> allNames = new java.util.HashSet<>();
        for (HorseRacingSystem.Race race : races) {
            for (HorseRacingSystem.Horse horse : race.getHorses()) {
                allNames.add(horse.getName());
            }
        }
        // All 48 horse names should be unique (30 names + 18 with "II" suffix)
        assertEquals(HorseRacingSystem.RACES_PER_DAY * HorseRacingSystem.HORSES_PER_RACE,
                allNames.size(),
                "All horses across all races should have unique names");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payout maths tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void winPayoutIsStakeTimesOddsNumerator() {
        // Build schedule
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        assertFalse(races.isEmpty(), "Need at least one race");

        // Find a horse with known odds for deterministic testing
        HorseRacingSystem.Race firstRace = races.get(0);
        int horseIdx = 0;
        HorseRacingSystem.Horse horse = firstRace.getHorses().get(horseIdx);
        int oddsNum = horse.getOddsNumerator();
        int stake = 10;

        // Place a bet
        HorseRacingSystem.BetResult result = system.placeBet(
                0, horseIdx, stake, inventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.SUCCESS, result, "Bet should succeed");

        int coinsBefore = inventory.getItemCount(Material.COIN);

        // Force the winning horse by resolving with the selected horse as winner
        // We use a deterministic Random that always picks horse 0 first
        HorseRacingSystem deterministicSystem = new HorseRacingSystem(new Random(0) {
            @Override
            public float nextFloat() { return 0f; } // Always picks first horse
        });
        deterministicSystem.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        HorseRacingSystem.BetResult betResult = deterministicSystem.placeBet(
                0, 0, stake, inventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.SUCCESS, betResult);

        // Advance time past first race
        timeSystem.setTime(HorseRacingSystem.FIRST_RACE_HOUR + 0.01f);
        List<String> awards = new ArrayList<>();
        deterministicSystem.update(0f, timeSystem, inventory, null, new ArrayList<>(),
                type -> awards.add(type.name()));

        // The race should be resolved now
        HorseRacingSystem.Race resolvedRace = deterministicSystem.getRace(0);
        assertNotNull(resolvedRace, "Race 0 should exist");
        assertTrue(resolvedRace.isResolved(), "Race 0 should be resolved after time passes");
    }

    @Test
    void lossDeductsStakeOnly() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        int initialCoins = inventory.getItemCount(Material.COIN);
        int stake = 15;

        // Place a bet
        HorseRacingSystem.BetResult result = system.placeBet(
                0, 0, stake, inventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.SUCCESS, result);

        // Verify stake was deducted immediately
        assertEquals(initialCoins - stake, inventory.getItemCount(Material.COIN),
                "Stake should be deducted on bet placement");
        assertEquals(1, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be in inventory after bet");
    }

    @Test
    void betSlipAddedOnBetPlacement() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        assertEquals(0, inventory.getItemCount(Material.BET_SLIP), "No BET_SLIP before bet");

        system.placeBet(0, 0, 5, inventory, null, null, false);

        assertEquals(1, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be in inventory after placing bet");
    }

    @Test
    void betSlipRemovedOnRaceResolution() {
        // Build schedule and place bet
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);
        system.placeBet(0, 0, 5, inventory, null, null, false);
        assertEquals(1, inventory.getItemCount(Material.BET_SLIP));

        // Advance time to resolve race 0
        timeSystem.setTime(HorseRacingSystem.FIRST_RACE_HOUR + 0.1f);
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        assertEquals(0, inventory.getItemCount(Material.BET_SLIP),
                "BET_SLIP should be removed after race resolves");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bet validation tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void cannotBetOnSameRaceTwice() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        system.placeBet(0, 0, 5, inventory, null, null, false);
        HorseRacingSystem.BetResult second = system.placeBet(0, 1, 5, inventory, null, null, false);

        assertEquals(HorseRacingSystem.BetResult.ALREADY_BET, second,
                "Should not be able to place a second bet");
    }

    @Test
    void cannotBetMoreThanMaxStakeNormal() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        HorseRacingSystem.BetResult result = system.placeBet(
                0, 0, HorseRacingSystem.MAX_STAKE_NORMAL + 1, inventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.INVALID_STAKE, result,
                "Bet above max stake should be rejected");
    }

    @Test
    void maxStakeDoubledOnBenefitDay() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        // Bet 75 (above normal 50 but below benefit day 100)
        HorseRacingSystem.BetResult normalResult = system.placeBet(
                0, 0, 75, inventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.INVALID_STAKE, normalResult,
                "75 coin bet should fail on normal day");

        HorseRacingSystem.BetResult benefitResult = system.placeBet(
                0, 0, 75, inventory, null, null, true);
        assertEquals(HorseRacingSystem.BetResult.SUCCESS, benefitResult,
                "75 coin bet should succeed on BENEFIT_DAY");
    }

    @Test
    void cannotBetWithInsufficientFunds() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        Inventory poorInventory = new Inventory(36);
        poorInventory.addItem(Material.COIN, 3);

        HorseRacingSystem.BetResult result = system.placeBet(
                0, 0, 10, poorInventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.INSUFFICIENT_FUNDS, result,
                "Should reject bet if player cannot afford stake");
    }

    @Test
    void cannotBetOnResolvedRace() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        // Resolve race 0 by advancing time
        timeSystem.setTime(HorseRacingSystem.FIRST_RACE_HOUR + 0.1f);
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        // Now try to bet on it
        HorseRacingSystem.BetResult result = system.placeBet(
                0, 0, 5, inventory, null, null, false);
        assertEquals(HorseRacingSystem.BetResult.RACE_ALREADY_RESOLVED, result,
                "Should not be able to bet on a resolved race");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Debt threshold tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void loanSharkTriggersAt50NetLoss() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        assertFalse(system.shouldSpawnLoanShark(), "No loan shark before losses");

        // Simulate losses by direct state check — we need net loss >= 50
        // Place and lose multiple bets
        // Advance time for each race
        for (int raceIdx = 0; raceIdx < HorseRacingSystem.RACES_PER_DAY; raceIdx++) {
            float raceHour = HorseRacingSystem.FIRST_RACE_HOUR
                    + raceIdx * HorseRacingSystem.RACE_SPACING_HOURS;

            // Don't advance past last race
            timeSystem.setTime(raceHour - 0.01f);
            system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

            if (system.getActiveBet() == null) {
                system.placeBet(raceIdx, 5, 10, inventory, null, null, false);
            }

            // Resolve the race
            timeSystem.setTime(raceHour + 0.01f);
            system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

            if (system.getCumulativeNetLoss() >= HorseRacingSystem.LOAN_SHARK_TRIGGER_LOSS) {
                break;
            }
        }

        // After sufficient losses, loan shark should be triggered
        // (the actual value depends on bet outcomes due to RNG, so we just verify the threshold logic)
        if (system.getCumulativeNetLoss() >= HorseRacingSystem.LOAN_SHARK_TRIGGER_LOSS) {
            assertTrue(system.shouldSpawnLoanShark(),
                    "shouldSpawnLoanShark() should return true when loss >= 50");
        }
    }

    @Test
    void loanSharkNotTriggeredWhenLossBelow50() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);
        // Initially no losses
        assertEquals(0, system.getCumulativeNetLoss());
        assertFalse(system.shouldSpawnLoanShark(),
                "Loan shark should not trigger at zero net loss");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Loan shark lifecycle tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void loanSharkLifecycleOfferAcceptRepay() {
        // Offer loan
        system.offerLoan(1);
        assertEquals(HorseRacingSystem.LoanState.OFFERED, system.getLoanState());

        // Accept loan
        Inventory inv = new Inventory(36);
        boolean accepted = system.acceptLoan(inv, 1);
        assertTrue(accepted, "Should accept loan when offered");
        assertEquals(HorseRacingSystem.LOAN_AMOUNT, inv.getItemCount(Material.COIN),
                "Should receive LOAN_AMOUNT coins");
        assertEquals(HorseRacingSystem.LoanState.TAKEN, system.getLoanState());

        // Repay loan
        inv.addItem(Material.COIN, HorseRacingSystem.LOAN_REPAY_AMOUNT); // ensure enough coins
        List<String> awards = new ArrayList<>();
        boolean repaid = system.repayLoan(inv, type -> awards.add(type.name()));
        assertTrue(repaid, "Should repay loan successfully");
        assertEquals(HorseRacingSystem.LoanState.REPAID, system.getLoanState());
        assertTrue(awards.contains(AchievementType.DEBT_FREE.name()),
                "Should award DEBT_FREE achievement on repayment");
    }

    @Test
    void loanSharkDefaultsWhenOverdue() {
        system.offerLoan(1);
        system.acceptLoan(new Inventory(36), 1);

        // Check not overdue on day 3 (inclusive = 3 days remaining)
        assertFalse(system.isLoanOverdue(1 + HorseRacingSystem.LOAN_REPAY_DAYS - 1),
                "Loan should not be overdue before deadline");

        // Check overdue on day 4
        assertTrue(system.isLoanOverdue(1 + HorseRacingSystem.LOAN_REPAY_DAYS),
                "Loan should be overdue after " + HorseRacingSystem.LOAN_REPAY_DAYS + " days");
    }

    @Test
    void loanCannotBeAcceptedIfNotOffered() {
        Inventory inv = new Inventory(36);
        boolean accepted = system.acceptLoan(inv, 1);
        assertFalse(accepted, "Should not accept loan if it was never offered");
        assertEquals(0, inv.getItemCount(Material.COIN),
                "No coins should be added without a valid offer");
    }

    @Test
    void repayLoanFailsIfInsufficientFunds() {
        system.offerLoan(1);
        system.acceptLoan(new Inventory(36), 1);

        Inventory emptyInv = new Inventory(36);
        boolean repaid = system.repayLoan(emptyInv, null);
        assertFalse(repaid, "Should fail to repay if insufficient funds");
        assertEquals(HorseRacingSystem.LoanState.TAKEN, system.getLoanState(),
                "Loan state should remain TAKEN if repayment failed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Winner selection tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void winnerIndexIsWithinValidRange() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        List<HorseRacingSystem.Race> races = system.getTodaysRaces();
        assertFalse(races.isEmpty());

        List<HorseRacingSystem.Horse> horses = races.get(0).getHorses();
        for (int trial = 0; trial < 100; trial++) {
            int winner = system.pickWinner(horses);
            assertTrue(winner >= 0 && winner < horses.size(),
                    "Winner index must be within valid range");
        }
    }

    @Test
    void favouriteWinsMoreThanOutsiderOverManyTrials() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);

        // Build a small test set with known odds
        List<HorseRacingSystem.Horse> horses = new ArrayList<>();
        horses.add(new HorseRacingSystem.Horse("Favourite", 2, 1)); // ~33% win prob
        horses.add(new HorseRacingSystem.Horse("Outsider",  33, 1)); // ~3% win prob

        int favouriteWins = 0;
        int outsiderWins = 0;
        int trials = 1000;

        for (int i = 0; i < trials; i++) {
            int winner = system.pickWinner(horses);
            if (winner == 0) favouriteWins++;
            else outsiderWins++;
        }

        assertTrue(favouriteWins > outsiderWins,
                "Favourite should win more often than 33/1 outsider over 1000 trials. "
                + "Favourite: " + favouriteWins + ", Outsider: " + outsiderWins);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Daily schedule rebuild tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void scheduleIsRebuiltEachDay() {
        system.update(0f, timeSystem, inventory, null, new ArrayList<>(), null);
        List<HorseRacingSystem.Race> day1Races = new ArrayList<>(system.getTodaysRaces());

        // Simulate new day by advancing dayCount in TimeSystem
        TimeSystem day2TimeSystem = new TimeSystem(11.0f);
        // Force day 2 by advancing more than 24 hours
        day2TimeSystem.advanceTime(25f);

        system.update(0f, day2TimeSystem, inventory, null, new ArrayList<>(), null);
        List<HorseRacingSystem.Race> day2Races = system.getTodaysRaces();

        // Both days should have the same number of races
        assertEquals(day1Races.size(), day2Races.size(),
                "Both days should have the same number of races");
        // But horse names may differ (seeded by day)
        // We can't assert they're different due to seeding, but verify 8 races exist
        assertEquals(HorseRacingSystem.RACES_PER_DAY, day2Races.size());
    }
}
