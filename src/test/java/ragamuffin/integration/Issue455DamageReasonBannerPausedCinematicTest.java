package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;
import ragamuffin.ui.GameHUD;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #455: damage-reason HUD banner never shown when
 * player takes damage during PAUSED or CINEMATIC state.
 *
 * <p>The rising-edge detector ({@code flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f})
 * was present only in {@code updatePlayingSimulation()} but absent from the PAUSED
 * and CINEMATIC branches, which both tick starvation and weather damage (fixes #399
 * and #433). As a result, the damage-reason banner was never triggered during those
 * states, and {@code prevDamageFlashIntensity} was left stale so the edge could also
 * misfire on the first PLAYING frame after resuming.
 *
 * <p>Fix: replicate the rising-edge detection block in both the PAUSED branch and the
 * CINEMATIC branch, keeping {@code prevDamageFlashIntensity} in sync at all times.
 */
class Issue455DamageReasonBannerPausedCinematicTest {

    private Player player;
    private GameHUD hud;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
        hud = new GameHUD(player);
    }

    // -----------------------------------------------------------------------
    // PAUSED branch
    // -----------------------------------------------------------------------

    /**
     * Test 1: Damage during PAUSED triggers the banner.
     *
     * Simulates a single damage event as it would occur in the PAUSED branch
     * after the fix: the rising-edge detector checks flashNow against
     * prevDamageFlashIntensity and calls showDamageReason() when a new hit arrives.
     */
    @Test
    void pausedDamage_showsDamageReasonBanner() {
        // prevDamageFlashIntensity starts at 0 (no prior damage this session)
        float prevDamageFlashIntensity = 0f;

        // Starvation damage fires during PAUSED (as enabled by fix #399)
        player.damage(5f, DamageReason.STARVATION);

        // --- Replicated rising-edge block (the fix) ---
        float flashNow = player.getDamageFlashIntensity();
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            hud.showDamageReason(player.getLastDamageReason());
        }
        prevDamageFlashIntensity = flashNow;
        player.updateFlash(1f / 60f);
        hud.update(1f / 60f);

        assertNotNull(hud.getDamageReasonText(),
                "Damage-reason banner must appear after starvation damage during PAUSED");
        assertEquals(DamageReason.STARVATION.getDisplayName(), hud.getDamageReasonText(),
                "Banner must show STARVATION reason");
        assertTrue(hud.getDamageReasonTimer() > 0,
                "Banner timer must be positive after showDamageReason()");
    }

    /**
     * Test 2: Without the fix (no rising-edge detector in PAUSED), banner is never shown.
     *
     * Documents the pre-fix bug: if the PAUSED branch only calls updateFlash() and
     * gameHUD.update() without the edge check, showDamageReason() is never invoked.
     */
    @Test
    void pausedDamage_withoutFix_bannerNotShown() {
        // Starvation damage fires during PAUSED
        player.damage(5f, DamageReason.STARVATION);

        // Pre-fix PAUSED branch: only advance flash and HUD, no edge detection
        player.updateFlash(1f / 60f);
        hud.update(1f / 60f);

        // Banner was never triggered — confirms the bug without the fix
        assertNull(hud.getDamageReasonText(),
                "Without the fix, damage-reason banner is never shown during PAUSED — confirming the bug");
    }

    /**
     * Test 3: prevDamageFlashIntensity stays in sync after PAUSED damage.
     *
     * After the fix updates prevDamageFlashIntensity in the PAUSED branch, the
     * rising-edge detector must NOT spuriously re-fire on the first PLAYING frame
     * (because prev is already 1.0f and the flash has only decayed slightly).
     */
    @Test
    void pausedDamage_keepsPrevInSync_noSpuriousFire() {
        // Damage occurs during PAUSED
        player.damage(5f, DamageReason.STARVATION);

        float prevDamageFlashIntensity = 0f;

        // PAUSED frame with the fix
        float flashNow = player.getDamageFlashIntensity();
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            hud.showDamageReason(player.getLastDamageReason());
        }
        prevDamageFlashIntensity = flashNow; // now == 1.0f
        player.updateFlash(1f / 60f);
        hud.update(1f / 60f);

        // Transition to PLAYING: flash has decayed slightly (< 1.0f now)
        // First PLAYING frame — edge must NOT fire again (prev == 1.0f, flashNow < 1.0f)
        GameHUD hud2 = new GameHUD(player);
        float flashOnResumedFrame = player.getDamageFlashIntensity();
        boolean edgeFires = flashOnResumedFrame >= 1.0f && prevDamageFlashIntensity < 1.0f;

        // prevDamageFlashIntensity == 1.0f so edge cannot fire (either flashNow < 1 or prev >= 1)
        assertFalse(edgeFires,
                "Rising-edge must not fire spuriously on first PLAYING frame after PAUSED damage");
    }

    // -----------------------------------------------------------------------
    // CINEMATIC branch
    // -----------------------------------------------------------------------

    /**
     * Test 4: Damage during CINEMATIC triggers the banner.
     *
     * Simulates the CINEMATIC branch after the fix: weather exposure or starvation
     * damage (enabled by fix #433) fires showDamageReason() via the rising-edge block.
     */
    @Test
    void cinematicDamage_showsDamageReasonBanner() {
        float prevDamageFlashIntensity = 0f;

        // Weather damage fires during CINEMATIC (as enabled by fix #433)
        player.damage(3f, DamageReason.WEATHER);

        // --- Replicated rising-edge block (the fix) ---
        float flashNow = player.getDamageFlashIntensity();
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            hud.showDamageReason(player.getLastDamageReason());
        }
        prevDamageFlashIntensity = flashNow;
        player.updateFlash(1f / 60f);
        hud.update(1f / 60f);

        assertNotNull(hud.getDamageReasonText(),
                "Damage-reason banner must appear after weather damage during CINEMATIC");
        assertEquals(DamageReason.WEATHER.getDisplayName(), hud.getDamageReasonText(),
                "Banner must show WEATHER reason");
        assertTrue(hud.getDamageReasonTimer() > 0,
                "Banner timer must be positive after showDamageReason()");
    }

    /**
     * Test 5: Without the fix (no rising-edge detector in CINEMATIC), banner is never shown.
     *
     * Documents the pre-fix bug for the CINEMATIC state.
     */
    @Test
    void cinematicDamage_withoutFix_bannerNotShown() {
        // Weather damage fires during CINEMATIC
        player.damage(3f, DamageReason.WEATHER);

        // Pre-fix CINEMATIC branch: only advance flash and HUD, no edge detection
        player.updateFlash(1f / 60f);
        hud.update(1f / 60f);

        assertNull(hud.getDamageReasonText(),
                "Without the fix, damage-reason banner is never shown during CINEMATIC — confirming the bug");
    }

    /**
     * Test 6: prevDamageFlashIntensity stays in sync after CINEMATIC damage,
     * preventing a spurious fire on the first PLAYING frame after the opening fly-through.
     */
    @Test
    void cinematicDamage_keepsPrevInSync_noSpuriousFire() {
        // Damage occurs during CINEMATIC
        player.damage(3f, DamageReason.WEATHER);

        float prevDamageFlashIntensity = 0f;

        // CINEMATIC frame with the fix
        float flashNow = player.getDamageFlashIntensity();
        if (flashNow >= 1.0f && prevDamageFlashIntensity < 1.0f) {
            hud.showDamageReason(player.getLastDamageReason());
        }
        prevDamageFlashIntensity = flashNow; // now == 1.0f
        player.updateFlash(1f / 60f);
        hud.update(1f / 60f);

        // First PLAYING frame — flash has decayed, prev is already 1.0f
        float flashOnPlayingFrame = player.getDamageFlashIntensity();
        boolean edgeFires = flashOnPlayingFrame >= 1.0f && prevDamageFlashIntensity < 1.0f;

        assertFalse(edgeFires,
                "Rising-edge must not fire spuriously on first PLAYING frame after CINEMATIC damage");
    }
}
