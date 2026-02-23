package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.StreetReputation;
import ragamuffin.core.Weather;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameHUD — Phase 14 additions:
 * dodge cooldown indicator and night warning banner.
 */
class GameHUDTest {

    private Player player;
    private GameHUD hud;

    @BeforeEach
    void setUp() {
        player = new Player(0, 5, 0);
        hud = new GameHUD(player);
    }

    // ====== Night warning ======

    @Test
    void testDefaultIsNotNight() {
        assertFalse(hud.isNight(), "HUD should not show night by default");
    }

    @Test
    void testSetNightTrue() {
        hud.setNight(true);
        assertTrue(hud.isNight(), "HUD should reflect night state");
    }

    @Test
    void testSetNightFalse() {
        hud.setNight(true);
        hud.setNight(false);
        assertFalse(hud.isNight(), "HUD should reflect day state after reset");
    }

    // ====== Dodge cooldown ======

    @Test
    void testDodgeReadyByDefault() {
        assertTrue(player.canDodge(), "Player should be able to dodge at full energy with no cooldown");
    }

    @Test
    void testDodgeCooldownAfterDodge() {
        player.dodge(1, 0);
        assertFalse(player.canDodge(), "Player should not dodge again immediately after dodging");
        assertTrue(player.getDodgeCooldownTimer() > 0, "Cooldown timer should be positive after dodge");
    }

    @Test
    void testDodgeReadyAfterCooldown() {
        player.dodge(1, 0);
        // Advance past full cooldown
        player.updateDodge(Player.DODGE_DURATION + Player.DODGE_COOLDOWN + 0.1f);
        assertTrue(player.canDodge(), "Dodge should be ready again after cooldown expires");
        assertEquals(0f, player.getDodgeCooldownTimer(), 0.01f, "Cooldown timer should be zero after cooldown");
    }

    @Test
    void testDodgeCooldownTimerDecreases() {
        player.dodge(1, 0);
        float timerAfterDodge = player.getDodgeCooldownTimer();
        player.updateDodge(0.3f); // Advance by 0.3s
        float timerLater = player.getDodgeCooldownTimer();
        assertTrue(timerLater < timerAfterDodge, "Cooldown timer should decrease over time");
    }

    @Test
    void testDodgeNotAvailableWithLowEnergy() {
        player.setEnergy(Player.DODGE_ENERGY_COST - 1f);
        assertFalse(player.canDodge(), "Dodge should not be available when energy is below cost");
    }

    // ====== Weather / existing HUD ======

    @Test
    void testWeatherDefaultIsClear() {
        // No direct getter, just verifying setWeather doesn't throw
        hud.setWeather(Weather.RAIN);
        hud.setWeather(Weather.CLEAR);
    }

    @Test
    void testBlockBreakProgressClamped() {
        hud.setBlockBreakProgress(1.5f);
        // No getter, but ensure no exception; rendering would use clamped value
        hud.setBlockBreakProgress(-0.5f);
    }

    @Test
    void testHudVisibility() {
        assertTrue(hud.isVisible(), "HUD should be visible by default");
        hud.hide();
        assertFalse(hud.isVisible());
        hud.show();
        assertTrue(hud.isVisible());
    }

    // ====== Street reputation star count (Issue #160) ======

    @Test
    void testStarCountZeroAtZeroPoints() {
        StreetReputation rep = player.getStreetReputation();
        assertEquals(0, rep.getStarCount(), "0 points should yield 0 stars");
    }

    @Test
    void testStarCountOneAtTenPoints() {
        StreetReputation rep = player.getStreetReputation();
        rep.addPoints(10);
        assertEquals(1, rep.getStarCount(), "10 points (KNOWN threshold) should yield 1 star");
    }

    @Test
    void testStarCountTwoAtTwentyPoints() {
        StreetReputation rep = player.getStreetReputation();
        rep.addPoints(20);
        assertEquals(2, rep.getStarCount(), "20 points should yield 2 stars");
    }

    @Test
    void testStarCountThreeAtThirtyPoints() {
        StreetReputation rep = player.getStreetReputation();
        rep.addPoints(30);
        assertEquals(3, rep.getStarCount(), "30 points (NOTORIOUS threshold) should yield 3 stars");
    }

    @Test
    void testStarCountFourAtFortyFivePoints() {
        StreetReputation rep = player.getStreetReputation();
        rep.addPoints(45);
        assertEquals(4, rep.getStarCount(), "45 points should yield 4 stars");
    }

    @Test
    void testStarCountFiveAtSixtyPoints() {
        StreetReputation rep = player.getStreetReputation();
        rep.addPoints(60);
        assertEquals(5, rep.getStarCount(), "60 points should yield 5 stars (maximum)");
    }
}
