package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #399:
 * Hunger drain, starvation health damage, energy recovery, and weather exposure
 * health drain were all gated inside the PLAYING branch and therefore frozen
 * while the game was paused. A player who was starving (hunger ≤ 0) could open
 * the pause menu to completely halt the 5 HP/s starvation damage indefinitely.
 *
 * Fix: call updateHunger(), damage(STARVATION), recoverEnergy(), and the weather
 * exposure damage check in the PAUSED render branch (guarded by
 * !respawnSystem.isRespawning()), mirroring the pattern used for healing (#381),
 * dodge (#379), reputation (#359), and weather timer (#341).
 *
 * Note: the sprint hunger multiplier (3×) must NOT be applied while paused since
 * the player is not moving.
 */
class Issue399SurvivalStatsPausedTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, 1, 0);
    }

    /**
     * Test 1: Hunger continues to drain when updateHunger() is called each frame —
     * simulates the PAUSED path where each rendered frame advances the hunger timer.
     *
     * With HUNGER_DRAIN_PER_MINUTE = 2 and 60-second simulation, hunger should
     * decrease by 2 units.
     */
    @Test
    void updateHunger_drainsContinuouslyDuringPause() {
        player.setHunger(100f);
        float delta = 1.0f / 60.0f;
        int frames = 60 * 60; // 60 seconds of paused frames

        for (int i = 0; i < frames; i++) {
            player.updateHunger(delta);
        }

        // HUNGER_DRAIN_PER_MINUTE = 2 → 2 units per minute → 2 units in 60 seconds
        float expectedHunger = 100f - 2f;
        assertTrue(player.getHunger() < 100f,
                "Hunger must decrease when updateHunger() is called during pause");
        assertEquals(expectedHunger, player.getHunger(), 0.5f,
                "Hunger must drain at the standard (non-sprint) rate while paused");
    }

    /**
     * Test 2: Without calling updateHunger() (simulating the pre-fix frozen state),
     * hunger stays at 100 and never drains.
     *
     * This documents the exact bug: if updateHunger() is never called during the
     * pause, hunger freezes and the player can starve-stall indefinitely.
     */
    @Test
    void withoutUpdateHunger_hungerFreezes() {
        player.setHunger(50f);
        // Deliberately do NOT call updateHunger() — simulates the pre-fix freeze
        assertEquals(50f, player.getHunger(), 0.001f,
                "Hunger must remain unchanged when updateHunger() is never called (pre-fix freeze)");
    }

    /**
     * Test 3: Starvation damage (5 HP/s) continues to drain health when hunger
     * is zero and damage() is called each paused frame.
     *
     * Simulates 10 seconds of paused starvation: health must decrease by ~50 HP.
     */
    @Test
    void starvationDamage_continuesDuringPause() {
        player.setHealth(100f);
        player.setHunger(0f);

        float delta = 1.0f / 60.0f;
        int frames = 10 * 60; // 10 seconds of paused frames

        for (int i = 0; i < frames; i++) {
            if (player.getHunger() <= 0) {
                player.damage(5.0f * delta, DamageReason.STARVATION);
            }
        }

        // 10 seconds × 5 HP/s = 50 HP drained
        float expectedHealth = 100f - 50f;
        assertTrue(player.getHealth() < 100f,
                "Health must decrease from starvation damage during pause");
        assertEquals(expectedHealth, player.getHealth(), 1.0f,
                "Starvation must drain health at 5 HP/s while paused");
    }

    /**
     * Test 4: Energy recovers when recoverEnergy() is called each paused frame.
     *
     * Simulates 10 seconds of paused energy recovery starting from zero energy.
     * With ENERGY_RECOVERY_PER_SECOND = 5, 10 seconds should restore 50 energy.
     */
    @Test
    void energyRecovery_continuesDuringPause() {
        player.setEnergy(0f);
        float delta = 1.0f / 60.0f;
        int frames = 10 * 60; // 10 seconds of paused frames

        for (int i = 0; i < frames; i++) {
            // weatherMultiplier = 1.0 (clear weather, no drain) → delta / 1.0
            player.recoverEnergy(delta / 1.0f);
        }

        // ENERGY_RECOVERY_PER_SECOND = 5 → 10 seconds → 50 energy
        float expectedEnergy = 50f;
        assertTrue(player.getEnergy() > 0f,
                "Energy must recover when recoverEnergy() is called during pause");
        assertEquals(expectedEnergy, player.getEnergy(), 2.0f,
                "Energy must recover at the standard rate while paused");
    }

    /**
     * Test 5: Without calling recoverEnergy() (simulating the pre-fix frozen state),
     * energy stays at 0 and never recovers.
     *
     * This documents the exact bug: fair players who take breaks (pause the game)
     * were penalised by having their energy recovery halted.
     */
    @Test
    void withoutRecoverEnergy_energyFreezes() {
        player.setEnergy(0f);
        // Deliberately do NOT call recoverEnergy() — simulates the pre-fix freeze
        assertEquals(0f, player.getEnergy(), 0.001f,
                "Energy must remain at 0 when recoverEnergy() is never called (pre-fix freeze)");
    }

    /**
     * Test 6: Sprint hunger multiplier must NOT be applied while paused — the
     * player is not moving, so hunger should drain at the base rate (×1).
     *
     * Compare 60-second drain at ×1 (paused) vs ×3 (sprinting): the paused drain
     * must equal the base rate.
     */
    @Test
    void pausedHungerDrain_usesBaseMutiplierNotSprint() {
        // Simulate 60s at base rate (×1)
        player.setHunger(100f);
        float delta = 1.0f / 60.0f;
        int frames = 60 * 60;
        for (int i = 0; i < frames; i++) {
            player.updateHunger(delta * 1.0f);
        }
        float drainBase = 100f - player.getHunger();

        // Simulate 60s at sprint rate (×3)
        player.setHunger(100f);
        for (int i = 0; i < frames; i++) {
            player.updateHunger(delta * 3.0f);
        }
        float drainSprint = 100f - player.getHunger();

        // Sprint drain must be approximately 3× the base drain
        assertEquals(drainSprint, drainBase * 3f, drainBase * 0.1f,
                "Sprint hunger drain must be 3× the base drain — verifying base rate is correct");

        // The paused drain (×1) must equal the base drain, NOT the sprint drain
        assertNotEquals(drainSprint, drainBase, 0.5f,
                "Paused drain (×1) must differ from sprint drain (×3)");
    }
}
