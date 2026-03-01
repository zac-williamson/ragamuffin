package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.*;
import ragamuffin.core.PirateRadioSystem.BroadcastAction;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;
import ragamuffin.ui.AchievementSystem;
import ragamuffin.ui.AchievementType;
import ragamuffin.ui.RadioUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #990: Integration tests for the RadioUI broadcast action overlay.
 *
 * <p>Verifies that the radio interface clearly displays the available broadcast
 * options to the player so they always know what keys 1-4 do while on-air.
 *
 * <p>Scenarios:
 * <ol>
 *   <li>RadioUI shows all 4 options while broadcasting.</li>
 *   <li>RadioUI shows action-pending prompt when a choice is due.</li>
 *   <li>RadioUI highlights the selected action.</li>
 *   <li>RadioUI hides when broadcast stops.</li>
 *   <li>RadioUI summary includes triangulation warning at ≥ 80%.</li>
 * </ol>
 */
class Issue990RadioUITest {

    private PirateRadioSystem radioSystem;
    private RadioUI radioUI;
    private FactionSystem factionSystem;
    private RumourNetwork rumourNetwork;
    private NotorietySystem notorietySystem;
    private AchievementSystem achievementSystem;
    private List<NPC> npcs;

    @BeforeEach
    void setUp() {
        radioSystem = new PirateRadioSystem(new Random(42));
        factionSystem = new FactionSystem();
        rumourNetwork = new RumourNetwork(new Random(7));
        notorietySystem = new NotorietySystem();
        achievementSystem = new AchievementSystem();

        radioSystem.setFactionSystem(factionSystem);
        radioSystem.setRumourNetwork(rumourNetwork);
        radioSystem.setNotorietySystem(notorietySystem);
        radioSystem.setAchievementCallback(type -> achievementSystem.unlock(type));

        radioSystem.onTransmitterPlaced(10f, 1f, 10f);

        radioUI = new RadioUI();

        npcs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            npcs.add(new NPC(NPCType.PUBLIC, 10f + i, 1f, 10f + i));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: RadioUI shows all 4 broadcast options while broadcasting
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 1: Start a broadcast. Show the RadioUI. Verify all four broadcast
     * actions are present in the summary text with their key numbers (1-4) and
     * display names. The player must always be able to see their options while
     * on-air, not just when a choice is pending.
     */
    @Test
    void scenario1_allFourOptionsVisibleWhileBroadcasting() {
        // Start broadcasting
        boolean started = radioSystem.startBroadcast(10f, 1f, 10f);
        assertTrue(started, "Broadcast should start when transmitter is placed");
        assertTrue(radioSystem.isBroadcasting(), "System should report broadcasting");

        // Show the radio UI
        radioUI.show();
        assertTrue(radioUI.isVisible(), "RadioUI should be visible after show()");
        assertTrue(radioUI.isActionListVisible(), "Action list should be visible while UI is shown");

        // Build summary and check all 4 actions are represented
        String summary = radioUI.buildSummary(radioSystem);

        // Key numbers
        assertTrue(summary.contains("[1]"), "Summary should contain [1] for Big Up the Area");
        assertTrue(summary.contains("[2]"), "Summary should contain [2] for Slag Off");
        assertTrue(summary.contains("[3]"), "Summary should contain [3] for Black Market Shout-Out");
        assertTrue(summary.contains("[4]"), "Summary should contain [4] for Council Diss Track");

        // Action names
        assertTrue(summary.contains("Big Up the Area"),
                "Summary should include 'Big Up the Area'");
        assertTrue(summary.contains("Slag Off a Faction"),
                "Summary should include 'Slag Off a Faction'");
        assertTrue(summary.contains("Black Market Shout-Out"),
                "Summary should include 'Black Market Shout-Out'");
        assertTrue(summary.contains("Council Diss Track"),
                "Summary should include 'Council Diss Track'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Action-pending prompt appears when a choice is due
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 2: Start broadcasting. Advance time by 10+ seconds so an action
     * becomes pending. Verify RadioUI reports action-prompt active and the summary
     * includes the "CHOOSE BROADCAST ACTION" heading.
     */
    @Test
    void scenario2_actionPendingPromptShown() {
        radioSystem.startBroadcast(10f, 1f, 10f);
        radioUI.show();

        // No action pending yet
        assertFalse(radioUI.isActionPromptActive(radioSystem),
                "Action prompt should not be active before timer expires");

        // Advance 11 seconds — action interval is 10s
        radioSystem.update(11f, npcs, false);

        assertTrue(radioSystem.isActionPending(), "Action should be pending after 10s");
        assertTrue(radioUI.isActionPromptActive(radioSystem),
                "RadioUI should report action prompt active when system has pending action");

        String summary = radioUI.buildSummary(radioSystem);
        assertTrue(summary.contains("CHOOSE BROADCAST ACTION"),
                "Summary should show 'CHOOSE BROADCAST ACTION' heading when action is pending");
        assertTrue(summary.contains("Press 1-4 to broadcast"),
                "Summary should prompt the player to press 1-4");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Highlighted action reflects selection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 3: Show the RadioUI. Navigate through the four actions using
     * nextAction() / prevAction(). Verify the highlighted index updates correctly
     * and the summary text marks the highlighted action with "> ".
     */
    @Test
    void scenario3_highlightedActionNavigatesCorrectly() {
        radioSystem.startBroadcast(10f, 1f, 10f);
        radioUI.show();

        // Default highlight is action 0
        assertEquals(0, radioUI.getHighlightedAction(), "Default highlight should be 0");

        // Navigate forward
        radioUI.nextAction();
        assertEquals(1, radioUI.getHighlightedAction(), "After nextAction(), highlight should be 1");

        radioUI.nextAction();
        assertEquals(2, radioUI.getHighlightedAction(), "After 2x nextAction(), highlight should be 2");

        radioUI.nextAction();
        assertEquals(3, radioUI.getHighlightedAction(), "After 3x nextAction(), highlight should be 3");

        // Should not go past 3
        radioUI.nextAction();
        assertEquals(3, radioUI.getHighlightedAction(), "Highlight should cap at 3");

        // Navigate backward
        radioUI.prevAction();
        assertEquals(2, radioUI.getHighlightedAction(), "After prevAction(), highlight should be 2");

        radioUI.prevAction();
        radioUI.prevAction();
        assertEquals(0, radioUI.getHighlightedAction(), "After 2x prevAction(), highlight should be 0");

        // Should not go below 0
        radioUI.prevAction();
        assertEquals(0, radioUI.getHighlightedAction(), "Highlight should not go below 0");

        // Set via direct index and verify summary marks it
        radioUI.setHighlightedAction(2);
        String summary = radioUI.buildSummary(radioSystem);
        assertTrue(summary.contains("> "), "Summary should contain '> ' marker for highlighted action");

        // The highlighted action (index 2 = BLACK_MARKET_SHOUTOUT) should be marked
        // while index 0 should not have the '>' prefix
        int idxBigUp = summary.indexOf("Big Up the Area");
        int idxBlackMarket = summary.indexOf("Black Market Shout-Out");
        assertTrue(idxBigUp >= 0, "Big Up the Area must appear");
        assertTrue(idxBlackMarket >= 0, "Black Market Shout-Out must appear");
        // The '>' prefix comes before the action name — check that '> ' appears before Black Market
        // and not as a prefix immediately before Big Up
        String beforeBigUp = summary.substring(0, idxBigUp);
        String lastLineStart = beforeBigUp.contains("\n")
                ? beforeBigUp.substring(beforeBigUp.lastIndexOf('\n') + 1)
                : beforeBigUp;
        assertFalse(lastLineStart.startsWith("> "),
                "Non-highlighted 'Big Up the Area' should not have '> ' prefix");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: RadioUI hides when broadcast stops
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 4: Start broadcasting and show RadioUI. Stop the broadcast.
     * Call hide() (as the game loop does when stopBroadcast is called). Verify
     * RadioUI is no longer visible and isActionPromptActive returns false.
     */
    @Test
    void scenario4_radioUIHidesWhenBroadcastStops() {
        radioSystem.startBroadcast(10f, 1f, 10f);
        radioUI.show();
        assertTrue(radioUI.isVisible(), "RadioUI should be visible while broadcasting");

        // Stop broadcast (game loop also calls radioUI.hide())
        radioSystem.stopBroadcast();
        radioUI.hide();

        assertFalse(radioUI.isVisible(), "RadioUI should be hidden after hide()");
        assertFalse(radioUI.isActionPromptActive(radioSystem),
                "Action prompt should not be active when UI is hidden");
        assertFalse(radioSystem.isBroadcasting(), "System should not be broadcasting");

        // Summary when not broadcasting should indicate off-air
        String summary = radioUI.buildSummary(radioSystem);
        assertTrue(summary.contains("OFF AIR"), "Summary should say OFF AIR when not broadcasting");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Triangulation warning appears at ≥ 80%
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scenario 5: Start broadcasting. Force triangulation to 85%. Show the
     * RadioUI. Verify the summary includes the signal warning text. Then force
     * triangulation to 50% and verify the warning is absent.
     */
    @Test
    void scenario5_triangulationWarningAppearsAtThreshold() {
        radioSystem.startBroadcast(10f, 1f, 10f);
        radioUI.show();

        // Below warning threshold — no warning
        radioSystem.setTriangulation(50f);
        assertFalse(radioSystem.isTriangulationWarning(),
                "No warning below 80% triangulation");
        String summaryOk = radioUI.buildSummary(radioSystem);
        assertFalse(summaryOk.contains("SIGNAL WARNING"),
                "Summary should not contain signal warning below 80%");

        // At warning threshold — warning should appear
        radioSystem.setTriangulation(85f);
        assertTrue(radioSystem.isTriangulationWarning(),
                "Warning flag should be set at 85% triangulation");
        String summaryWarn = radioUI.buildSummary(radioSystem);
        assertTrue(summaryWarn.contains("SIGNAL WARNING"),
                "Summary should contain '*** SIGNAL WARNING ***' at 85% triangulation");

        // Triangulation percentage shown in both cases
        assertTrue(summaryOk.contains("50%") || summaryOk.contains("50"),
                "Summary should show triangulation percentage");
        assertTrue(summaryWarn.contains("85%") || summaryWarn.contains("85"),
                "Summary should show triangulation percentage at warning level");
    }
}
