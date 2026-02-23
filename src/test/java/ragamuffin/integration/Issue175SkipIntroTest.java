package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.ui.MainMenuScreen;
import ragamuffin.ui.OpeningSequence;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #175:
 * Players should have the ability to skip the intro sequence after starting a new game.
 *
 * Verifies that:
 * 1. The main menu has a "Skip Intro" toggle option.
 * 2. Skip Intro defaults to OFF.
 * 3. Selecting the Skip Intro option toggles it ON/OFF.
 * 4. When Skip Intro is ON, the opening sequence is immediately completed after start().
 * 5. When Skip Intro is OFF, the opening sequence plays normally.
 * 6. Navigation wraps correctly through all three options (New Game, Skip Intro, Quit).
 */
class Issue175SkipIntroTest {

    private MainMenuScreen mainMenuScreen;
    private OpeningSequence openingSequence;

    @BeforeEach
    void setUp() {
        mainMenuScreen = new MainMenuScreen();
        openingSequence = new OpeningSequence();
    }

    /**
     * Test 1: Main menu has a Skip Intro toggle option.
     * Verify that navigating down from New Game reaches the Skip Intro option.
     */
    @Test
    void mainMenuHasSkipIntroOption() {
        // Default is New Game
        assertTrue(mainMenuScreen.isNewGameSelected(), "Default selection must be New Game");
        assertFalse(mainMenuScreen.isSkipIntroSelected(), "Skip Intro must not be selected by default");

        // Navigate down once to reach Skip Intro
        mainMenuScreen.selectNext();

        assertTrue(mainMenuScreen.isSkipIntroSelected(), "Skip Intro must be reachable via selectNext()");
        assertFalse(mainMenuScreen.isNewGameSelected(), "New Game must not be selected after selectNext()");
    }

    /**
     * Test 2: Skip Intro defaults to OFF.
     */
    @Test
    void skipIntroDefaultsToOff() {
        assertFalse(mainMenuScreen.isSkipIntroEnabled(), "Skip Intro must default to OFF");
    }

    /**
     * Test 3: Toggling Skip Intro switches it ON then OFF.
     */
    @Test
    void toggleSkipIntroSwitchesState() {
        assertFalse(mainMenuScreen.isSkipIntroEnabled(), "Skip Intro should start OFF");

        mainMenuScreen.toggleSkipIntro();
        assertTrue(mainMenuScreen.isSkipIntroEnabled(), "Skip Intro should be ON after first toggle");

        mainMenuScreen.toggleSkipIntro();
        assertFalse(mainMenuScreen.isSkipIntroEnabled(), "Skip Intro should be OFF after second toggle");
    }

    /**
     * Test 4: When Skip Intro is ON, the opening sequence is immediately completed.
     * Simulates what RagamuffinGame.startNewGame() does when skipIntro is enabled:
     * calls openingSequence.start() then openingSequence.skip().
     */
    @Test
    void skipIntroEnabledCompletesSequenceImmediately() {
        mainMenuScreen.toggleSkipIntro(); // Enable skip intro
        assertTrue(mainMenuScreen.isSkipIntroEnabled(), "Skip Intro must be enabled");

        // Simulate startNewGame() with skip enabled
        openingSequence.start();
        if (mainMenuScreen.isSkipIntroEnabled()) {
            openingSequence.skip();
        }

        assertFalse(openingSequence.isActive(), "Opening sequence must not be active when skip intro is ON");
        assertTrue(openingSequence.isCompleted(), "Opening sequence must be completed when skip intro is ON");
    }

    /**
     * Test 5: When Skip Intro is OFF, the opening sequence plays normally.
     * Simulates what RagamuffinGame.startNewGame() does when skipIntro is disabled.
     */
    @Test
    void skipIntroDisabledPlaysSequenceNormally() {
        assertFalse(mainMenuScreen.isSkipIntroEnabled(), "Skip Intro must be OFF");

        // Simulate startNewGame() with skip disabled
        openingSequence.start();
        if (mainMenuScreen.isSkipIntroEnabled()) {
            openingSequence.skip();
        }

        assertTrue(openingSequence.isActive(), "Opening sequence must be active when skip intro is OFF");
        assertFalse(openingSequence.isCompleted(), "Opening sequence must not be completed immediately when skip intro is OFF");
    }

    /**
     * Test 6: All three menu options are accessible and navigation wraps correctly.
     * New Game -> Skip Intro -> Quit -> New Game (wrap).
     */
    @Test
    void menuOptionsWrapCorrectly() {
        assertTrue(mainMenuScreen.isNewGameSelected(), "Default must be New Game");

        mainMenuScreen.selectNext();
        assertTrue(mainMenuScreen.isSkipIntroSelected(), "After one down: Skip Intro");

        mainMenuScreen.selectNext();
        assertTrue(mainMenuScreen.isQuitSelected(), "After two down: Quit");

        mainMenuScreen.selectNext(); // wrap around
        assertTrue(mainMenuScreen.isNewGameSelected(), "After wrap: New Game");

        // Also check upward navigation
        mainMenuScreen.selectPrevious();
        assertTrue(mainMenuScreen.isQuitSelected(), "Wrap up from New Game should reach Quit");
    }
}
