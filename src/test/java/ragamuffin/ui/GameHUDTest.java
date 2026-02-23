package ragamuffin.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.StreetReputation;
import ragamuffin.core.Weather;
import ragamuffin.entity.DamageReason;
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

    // ====== Target reticule label (Issue #189) ======

    @Test
    void testTargetNameNullByDefault() {
        assertNull(hud.getTargetName(), "Target name should be null by default (nothing targeted)");
    }

    @Test
    void testSetTargetNameBlock() {
        hud.setTargetName("Tree Trunk");
        assertEquals("Tree Trunk", hud.getTargetName());
    }

    @Test
    void testSetTargetNameNPC() {
        hud.setTargetName("Youth Gang");
        assertEquals("Youth Gang", hud.getTargetName());
    }

    @Test
    void testSetTargetNameNull() {
        hud.setTargetName("Brick");
        hud.setTargetName(null);
        assertNull(hud.getTargetName(), "Setting null should clear the target name");
    }

    @Test
    void testSetTargetNameEmpty() {
        hud.setTargetName("Brick");
        hud.setTargetName("");
        assertNull(hud.getTargetName(), "Setting empty string should clear the target name");
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

    // ====== Damage reason banner (Issue #216) ======

    @Test
    void testDamageReasonNullByDefault() {
        assertNull(hud.getDamageReasonText(), "No damage reason should show by default");
    }

    @Test
    void testShowDamageReasonDisplaysText() {
        hud.showDamageReason(DamageReason.FALL);
        assertEquals("Fall damage", hud.getDamageReasonText(),
            "Should display FALL damage reason text");
    }

    @Test
    void testShowDamageReasonNPCAttack() {
        hud.showDamageReason(DamageReason.NPC_ATTACK);
        assertEquals("Attacked", hud.getDamageReasonText());
    }

    @Test
    void testShowDamageReasonStarvation() {
        hud.showDamageReason(DamageReason.STARVATION);
        assertEquals("Starving", hud.getDamageReasonText());
    }

    @Test
    void testShowDamageReasonWeather() {
        hud.showDamageReason(DamageReason.WEATHER);
        assertEquals("Exposure", hud.getDamageReasonText());
    }

    @Test
    void testDamageReasonTimerSetOnShow() {
        hud.showDamageReason(DamageReason.FALL);
        assertTrue(hud.getDamageReasonTimer() > 0, "Timer should be positive after showDamageReason");
    }

    @Test
    void testDamageReasonTimerDecreases() {
        hud.showDamageReason(DamageReason.FALL);
        float timerBefore = hud.getDamageReasonTimer();
        hud.update(0.5f);
        assertTrue(hud.getDamageReasonTimer() < timerBefore, "Timer should decrease after update");
    }

    @Test
    void testDamageReasonTextClearsAfterTimer() {
        hud.showDamageReason(DamageReason.FALL);
        // Advance past full duration
        hud.update(10f);
        assertNull(hud.getDamageReasonText(), "Damage reason text should clear after timer expires");
    }

    @Test
    void testDamageReasonCanBeUpdated() {
        hud.showDamageReason(DamageReason.FALL);
        hud.showDamageReason(DamageReason.NPC_ATTACK);
        assertEquals("Attacked", hud.getDamageReasonText(),
            "Newer damage reason should overwrite the previous one");
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
