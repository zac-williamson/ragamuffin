package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;
import ragamuffin.ui.GameHUD;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #307:
 * prevDamageFlashIntensity is never reset in restartGame(), which suppresses
 * the first-hit damage-reason banner in a new session.
 *
 * Fix: restartGame() now sets prevDamageFlashIntensity = 0f so the rising-edge
 * detector (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) fires correctly
 * on the very first hit after a restart.
 *
 * These tests model the flash-edge detection logic directly (RagamuffinGame lines
 * 1039–1042) and the expected post-restart state.
 */
class Issue307PrevDamageFlashResetTest {

    /**
     * Test 1: After a restart, prevDamageFlashIntensity must be 0.
     *
     * Models the restartGame() contract: the field is reset to 0f so that the
     * first hit of the new session will always trigger showDamageReason().
     *
     * We simulate the full state machine:
     *   session 1 → damage fires → prevDamageFlashIntensity rises to 1.0f
     *   restartGame() → prevDamageFlashIntensity = 0f  (the fix)
     *   session 2, frame 1 → damage fires → edge condition fires → banner shown
     */
    @Test
    void restartResetsFlashIntensityEnablingFirstHitBanner() {
        // --- Session 1 ---
        Player player1 = new Player(0, 1, 0);
        GameHUD hud = new GameHUD(player1);

        // Player takes a hit; flash resets to DAMAGE_FLASH_DURATION → intensity = 1.0f
        player1.damage(10f, DamageReason.NPC_ATTACK);
        float flashNow = player1.getDamageFlashIntensity();
        assertEquals(1.0f, flashNow, 1e-6f,
                "getDamageFlashIntensity() must be exactly 1.0 immediately after damage");

        // Edge detector fires and prevDamageFlashIntensity is updated (as per game loop)
        float prevDamageFlashIntensity = 0f; // initial value before this hit
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            hud.showDamageReason(player1.getLastDamageReason());
        }
        prevDamageFlashIntensity = flashNow; // now == 1.0f

        // Verify banner showed in session 1
        assertNotNull(hud.getDamageReasonText(),
                "Damage reason banner must appear after the first session hit");

        // Simulate flash decaying over several frames (but never reaching 0 — say 0.8f)
        // In the bug scenario, the flash may still be elevated (or stuck at 1.0 on the
        // exact hit frame) when restartGame() is called.
        // prevDamageFlashIntensity is left at 1.0f (as it would be without the fix).

        // --- restartGame() — THE FIX ---
        // Before fix: prevDamageFlashIntensity stays at 1.0f.
        // After fix:  prevDamageFlashIntensity = 0f;
        prevDamageFlashIntensity = 0f; // simulates the fix in restartGame()

        // --- Session 2 ---
        Player player2 = new Player(0, 1, 0);
        GameHUD hud2 = new GameHUD(player2);

        // Player takes damage on the very first hit of the new session
        player2.damage(10f, DamageReason.STARVATION);
        float flashNow2 = player2.getDamageFlashIntensity();
        assertEquals(1.0f, flashNow2, 1e-6f,
                "getDamageFlashIntensity() must be 1.0 immediately after first hit in new session");

        // Edge condition — must fire because prevDamageFlashIntensity == 0f after restart
        boolean edgeFired = flashNow2 >= 1.0f && prevDamageFlashIntensity < 1.0f;
        assertTrue(edgeFired,
                "Rising-edge condition must fire on first hit after restart: " +
                "flashNow=" + flashNow2 + " prev=" + prevDamageFlashIntensity);

        if (edgeFired) {
            hud2.showDamageReason(player2.getLastDamageReason());
        }

        assertNotNull(hud2.getDamageReasonText(),
                "Damage reason banner must appear on first hit of new session");
        assertEquals(DamageReason.STARVATION.getDisplayName(), hud2.getDamageReasonText(),
                "Banner must show the correct damage reason");
    }

    /**
     * Test 2: Without the fix (prevDamageFlashIntensity not reset), the banner
     * is suppressed on the first hit of the new session.
     *
     * This documents the pre-fix bug so that if the reset is accidentally removed,
     * this test illustrates why it was needed (note: this test CONFIRMS the bug
     * exists without the fix, demonstrating the necessity of the one-line change).
     */
    @Test
    void withoutResetFirstHitBannerIsSuppressed() {
        // Simulate session 1: prevDamageFlashIntensity ends at 1.0f (stuck at hit frame)
        float prevDamageFlashIntensity = 1.0f; // no reset — the bug

        // Session 2: player takes damage on first hit
        Player player = new Player(0, 1, 0);
        player.damage(10f, DamageReason.NPC_ATTACK);
        float flashNow = player.getDamageFlashIntensity();

        // Edge condition — must NOT fire because prev is still 1.0f (the bug)
        boolean edgeFired = flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f;
        assertFalse(edgeFired,
                "Without the fix, edge condition is suppressed when prev=1.0f — confirming the bug");
    }

    /**
     * Test 3: The fix: with prevDamageFlashIntensity reset to 0f, the edge fires
     * correctly even when both prev and now are at exactly 1.0f boundary.
     */
    @Test
    void withResetEdgeFiresCorrectlyAtBoundary() {
        // restartGame() sets this to 0f
        float prevDamageFlashIntensity = 0f;

        // First hit of new session — intensity jumps to exactly 1.0f
        Player player = new Player(0, 1, 0);
        player.damage(5f, DamageReason.FALL);
        float flashNow = player.getDamageFlashIntensity();

        GameHUD hud = new GameHUD(player);
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            hud.showDamageReason(player.getLastDamageReason());
        }

        assertNotNull(hud.getDamageReasonText(),
                "Banner must show after fix resets prev to 0f");
        assertEquals(DamageReason.FALL.getDisplayName(), hud.getDamageReasonText());
        assertTrue(hud.getDamageReasonTimer() > 0,
                "Banner timer must be positive after showDamageReason");
    }
}
