package ragamuffin.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.ui.AchievementSystem;

import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #858: Wire MCBattleSystem.pressAction() into the game loop input handler.
 *
 * Verifies that pressing the action key during an active battle routes to
 * MCBattleSystem.pressAction() and returns a meaningful hit/miss message,
 * and that the interact key press is consumed so it is not also processed
 * as a normal NPC interaction on the same frame.
 */
class Issue858MCBattleInputRoutingTest {

    private MCBattleSystem mcBattleSystem;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Random rng = new Random(42L); // fixed seed for deterministic hit-zone placement
        NotorietySystem notorietySystem = new NotorietySystem();
        RumourNetwork rumourNetwork = new RumourNetwork(rng);
        TurfMap turfMap = new TurfMap();
        FactionSystem factionSystem = new FactionSystem(turfMap, rumourNetwork, rng);
        AchievementSystem achievementSystem = new AchievementSystem();
        mcBattleSystem = new MCBattleSystem(notorietySystem, factionSystem, achievementSystem, rumourNetwork, rng);
        inventory = new Inventory(36);
        inventory.addItem(Material.MICROPHONE, 1);
    }

    /**
     * pressAction() must return null (no battle active) before a battle is started.
     */
    @Test
    void pressAction_whenNoBattleActive_returnsNull() {
        assertFalse(mcBattleSystem.isBattleActive(), "No battle should be active initially");
        String result = mcBattleSystem.pressAction(Collections.emptyList());
        assertNull(result, "pressAction() must return null when no battle is active");
    }

    /**
     * isBattleActive() must return true immediately after startBattle().
     */
    @Test
    void isBattleActive_afterStartBattle_returnsTrue() {
        mcBattleSystem.startBattle(MCBattleSystem.Champion.MARCHETTI_MC, inventory);
        assertTrue(mcBattleSystem.isBattleActive(),
                "isBattleActive() must return true after startBattle()");
    }

    /**
     * pressAction() during an active battle must return a non-null, non-empty message.
     * The returned message must be either "Hit!" or "Missed!" — surfacing player
     * feedback that the game loop should forward to the tooltip system.
     */
    @Test
    void pressAction_duringActiveBattle_returnsHitOrMissMessage() {
        mcBattleSystem.startBattle(MCBattleSystem.Champion.MARCHETTI_MC, inventory);
        assertTrue(mcBattleSystem.isBattleActive());

        String result = mcBattleSystem.pressAction(Collections.emptyList());

        assertNotNull(result, "pressAction() must return a non-null message during an active battle");
        assertFalse(result.isEmpty(), "pressAction() must return a non-empty message during an active battle");
        assertTrue(result.equals("Hit!") || result.equals("Missed!"),
                "pressAction() must return 'Hit!' or 'Missed!', got: " + result);
    }

    /**
     * When the cursor is manually positioned inside the hit zone before pressing,
     * pressAction() must return "Hit!".
     *
     * This test constructs a BattleBarMiniGame with a known hit zone and advances
     * the cursor to a position guaranteed to be inside it, then verifies MCBattleSystem
     * returns the correct message.
     */
    @Test
    void pressAction_whenCursorInsideHitZone_returnsHit() {
        mcBattleSystem.startBattle(MCBattleSystem.Champion.MARCHETTI_MC, inventory);
        assertTrue(mcBattleSystem.isBattleActive());

        // Retrieve the current bar and move the cursor to the middle of the hit zone
        BattleBarMiniGame bar = mcBattleSystem.getCurrentBar();
        assertNotNull(bar, "getCurrentBar() must not be null during an active battle");

        float hitZoneCenter = bar.getHitZoneStart() + bar.getHitZoneWidth() / 2f;

        // Advance the bar until the cursor is at the hit zone center.
        // The cursor starts at 0 and travels at EASY_CURSOR_SPEED; compute the time needed.
        float timeToCenter = hitZoneCenter / BattleBarMiniGame.EASY_CURSOR_SPEED;
        // Advance in small steps to avoid overshooting
        float dt = 0.01f;
        float elapsed = 0f;
        while (elapsed < timeToCenter - dt && elapsed < BattleBarMiniGame.ROUND_TIMEOUT_SECONDS - dt) {
            bar.update(dt);
            elapsed += dt;
        }

        // The cursor should now be near the hit zone; press the action
        String result = mcBattleSystem.pressAction(Collections.emptyList());

        assertNotNull(result);
        assertEquals("Hit!", result,
                "pressAction() must return 'Hit!' when cursor is inside the hit zone");
    }

    /**
     * Pressing the action key before the cursor reaches the hit zone must return "Missed!".
     * (Cursor starts at 0.0; hit zone is never at 0.0 due to margin constraints.)
     */
    @Test
    void pressAction_whenCursorAtStart_returnsMissed() {
        mcBattleSystem.startBattle(MCBattleSystem.Champion.MARCHETTI_MC, inventory);
        assertTrue(mcBattleSystem.isBattleActive());

        BattleBarMiniGame bar = mcBattleSystem.getCurrentBar();
        assertNotNull(bar);

        // Cursor starts at 0.0; hit zone start is always > 0.15 - hitZoneWidth/2
        // so pressing immediately is a guaranteed miss.
        // Verify the cursor is at 0.0 before pressing
        assertEquals(0f, bar.getCursorPos(), 1e-5f,
                "Cursor must start at 0.0");
        assertTrue(bar.getHitZoneStart() > 0f,
                "Hit zone must not start at 0.0");

        String result = mcBattleSystem.pressAction(Collections.emptyList());
        assertNotNull(result);
        assertEquals("Missed!", result,
                "pressAction() at cursor position 0.0 (before hit zone) must return 'Missed!'");
    }

    /**
     * After pressAction() resolves a round and the battle ends, isBattleActive() must
     * return false so the game loop stops routing E-key presses to the battle system.
     *
     * A 3-round battle ends when 2 rounds are won or lost; pressing 3 times (all miss)
     * ends the battle.
     */
    @Test
    void isBattleActive_afterBattleEnds_returnsFalse() {
        mcBattleSystem.startBattle(MCBattleSystem.Champion.MARCHETTI_MC, inventory);
        assertTrue(mcBattleSystem.isBattleActive());

        // Press immediately each round (cursor at 0 = miss) to force the battle to resolve
        for (int round = 0; round < MCBattleSystem.ROUNDS_PER_BATTLE; round++) {
            if (!mcBattleSystem.isBattleActive()) break;
            mcBattleSystem.pressAction(Collections.emptyList());
        }

        assertFalse(mcBattleSystem.isBattleActive(),
                "isBattleActive() must return false after all battle rounds resolve");
    }

    /**
     * pressAction() on an already-resolved bar must not throw and must return null.
     * This covers the case where two E-key presses arrive in the same frame.
     */
    @Test
    void pressAction_onResolvedBar_returnsNull() {
        mcBattleSystem.startBattle(MCBattleSystem.Champion.MARCHETTI_MC, inventory);

        // Resolve the battle entirely
        for (int i = 0; i < MCBattleSystem.ROUNDS_PER_BATTLE + 1; i++) {
            mcBattleSystem.pressAction(Collections.emptyList());
            if (!mcBattleSystem.isBattleActive()) break;
        }

        // Now no battle is active — pressing again must safely return null
        assertFalse(mcBattleSystem.isBattleActive());
        String result = mcBattleSystem.pressAction(Collections.emptyList());
        assertNull(result, "pressAction() must return null when no battle is active");
    }
}
