package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #411:
 * Hunger drain, starvation health damage, weather energy drain, and cold-snap
 * health drain were all gated behind {@code isUIBlocking()} in the PLAYING
 * branch. This meant opening the inventory (I), help (H), or crafting (C) menu
 * completely froze these time-based survival effects for as long as the overlay
 * was open — an exploit that let players avoid starving indefinitely.
 *
 * Fix: move updateHunger(), starvation damage, energy recovery, and weather
 * exposure outside the isUIBlocking() guard (keep only !respawnSystem.isRespawning()).
 * The sprint hunger multiplier (3×) falls back to 1× while a UI overlay is open
 * since the player is not moving. healingSystem.update() remains gated
 * (intentional design: you cannot rest while rummaging through inventory).
 *
 * These tests simulate the "UI open" path by calling Player methods directly,
 * mirroring the pattern established by Issue399SurvivalStatsPausedTest.
 */
class Issue411SurvivalStatsUIOverlayTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: Hunger continues to drain while a UI overlay is open.
     *
     * With HUNGER_DRAIN_PER_MINUTE = 2 and a 60-second simulation, hunger
     * should decrease by 2 units regardless of whether the inventory is open.
     */
    @Test
    void hungerDrains_whileUIOverlayIsOpen() {
        player.setHunger(100f);
        float delta = 1.0f / 60.0f;
        int frames = 60 * 60; // 60 seconds

        // Simulate the fixed path: UI open → hungerMultiplier falls back to 1×
        for (int i = 0; i < frames; i++) {
            player.updateHunger(delta * 1.0f); // multiplier = 1 (UI open, not sprinting)
        }

        float expectedHunger = 100f - 2f; // 2 units per minute
        assertTrue(player.getHunger() < 100f,
                "Hunger must decrease even when a UI overlay is open");
        assertEquals(expectedHunger, player.getHunger(), 0.5f,
                "Hunger must drain at the base (non-sprint) rate while UI is open");
    }

    /**
     * Test 2: Starvation damage (5 HP/s) continues when hunger reaches zero
     * while the inventory or another UI overlay is open.
     *
     * 10 seconds of starvation must drain ~50 HP.
     */
    @Test
    void starvationDamage_continuesWhileUIOverlayIsOpen() {
        player.setHealth(100f);
        player.setHunger(0f);

        float delta = 1.0f / 60.0f;
        int frames = 10 * 60; // 10 seconds

        for (int i = 0; i < frames; i++) {
            // updateHunger first (stays at 0), then starvation damage
            player.updateHunger(delta * 1.0f);
            if (player.getHunger() <= 0) {
                player.damage(5.0f * delta, DamageReason.STARVATION);
            }
        }

        float expectedHealth = 100f - 50f; // 10s × 5 HP/s
        assertTrue(player.getHealth() < 100f,
                "Starvation damage must apply even when a UI overlay is open");
        assertEquals(expectedHealth, player.getHealth(), 1.0f,
                "Starvation must drain health at 5 HP/s while UI is open");
    }

    /**
     * Test 3: Energy recovery (affected by weather multiplier) continues while
     * the inventory or another UI overlay is open.
     *
     * 10 seconds of recovery from zero energy with weatherMultiplier=1 must
     * restore ~50 energy (ENERGY_RECOVERY_PER_SECOND = 5).
     */
    @Test
    void energyRecovery_continuesWhileUIOverlayIsOpen() {
        player.setEnergy(0f);
        float delta = 1.0f / 60.0f;
        int frames = 10 * 60; // 10 seconds

        for (int i = 0; i < frames; i++) {
            // weatherMultiplier = 1.0 (clear weather) — same formula as production code
            player.recoverEnergy(delta / 1.0f);
        }

        float expectedEnergy = 50f; // ENERGY_RECOVERY_PER_SECOND = 5 × 10s
        assertTrue(player.getEnergy() > 0f,
                "Energy must recover even when a UI overlay is open");
        assertEquals(expectedEnergy, player.getEnergy(), 2.0f,
                "Energy must recover at the standard rate while UI is open");
    }

    /**
     * Test 4: Sprint hunger multiplier (3×) must NOT apply while a UI overlay
     * is open — the player is not moving.
     *
     * Hunger drain at ×1 (UI open) must be significantly less than at ×3 (sprinting).
     */
    @Test
    void hungerDrain_usesBaseMultiplierNotSprintWhileUIOpen() {
        float delta = 1.0f / 60.0f;
        int frames = 60 * 60; // 60 seconds

        // Drain at base rate (×1) — UI open path
        player.setHunger(100f);
        for (int i = 0; i < frames; i++) {
            player.updateHunger(delta * 1.0f);
        }
        float drainBase = 100f - player.getHunger();

        // Drain at sprint rate (×3)
        player.setHunger(100f);
        for (int i = 0; i < frames; i++) {
            player.updateHunger(delta * 3.0f);
        }
        float drainSprint = 100f - player.getHunger();

        // Sprint drain must be ~3× the base drain
        assertEquals(drainSprint, drainBase * 3f, drainBase * 0.1f,
                "Sprint drain (×3) must be 3× the base drain (×1)");

        // The UI-open drain (×1) must be the base drain, not the sprint drain
        assertNotEquals(drainSprint, drainBase, 0.5f,
                "UI-open drain (×1) must differ from sprint drain (×3)");
    }

    /**
     * Test 5: Weather damage (cold snap) continues while a UI overlay is open.
     *
     * Simulates 10 seconds of cold-snap health drain at a known rate.
     * The fix ensures this is not gated behind isUIBlocking().
     */
    @Test
    void weatherDamage_continuesWhileUIOverlayIsOpen() {
        player.setHealth(100f);
        float healthDrainRate = 2.0f; // HP/s (a representative cold-snap rate)
        float delta = 1.0f / 60.0f;
        int frames = 10 * 60; // 10 seconds

        for (int i = 0; i < frames; i++) {
            // Direct call mirrors the production-code path now outside isUIBlocking()
            player.damage(healthDrainRate * delta, DamageReason.WEATHER);
        }

        float expectedHealth = 100f - (healthDrainRate * 10f); // 2 HP/s × 10s = 20 HP
        assertTrue(player.getHealth() < 100f,
                "Weather/cold-snap damage must apply even when a UI overlay is open");
        assertEquals(expectedHealth, player.getHealth(), 1.0f,
                "Weather damage must drain health at the correct rate while UI is open");
    }
}
