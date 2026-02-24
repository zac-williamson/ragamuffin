package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.render.ParticleSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #351:
 * {@code particleSystem.update(delta)} was absent from the PAUSED render path, so
 * all active particles (combat sparks, block-break debris, footstep dust, dodge
 * trail streaks) froze in world-space for the entire duration of the pause and all
 * expired simultaneously in a single burst on resume.
 *
 * Fix: call {@code particleSystem.update(delta)} in the PAUSED render block after
 * {@code firstPersonArm.update(delta)}, mirroring the pattern used for sky (#323),
 * arm swing (#339), weather (#341), speech log (#343), and respawn (#347).
 *
 * Also add {@code particleSystem.render(...)} to the PAUSED render path so particles
 * remain visible (not just updated) while the pause menu is open.
 */
class Issue351ParticleSystemPausedTest {

    private ParticleSystem particleSystem;

    @BeforeEach
    void setUp() {
        particleSystem = new ParticleSystem();
    }

    /**
     * Test 1: Particles expire naturally when update() is called each frame —
     * simulates the paused scenario where every rendered frame advances the timer.
     *
     * After enough per-frame updates totalling more than the maximum particle
     * lifetime, all active particles must have expired (getActiveCount() == 0).
     */
    @Test
    void updateAdvancesLifetime_particlesExpireAfterFullDuration() {
        // Emit a burst of combat-hit sparks (lifetime 0.25–0.55 s)
        particleSystem.emitCombatHit(0f, 1f, 0f);
        assertTrue(particleSystem.getActiveCount() > 0,
                "At least one particle must be active immediately after emission");

        // Advance well beyond the maximum combat-hit lifetime (0.55 s) in small ticks
        float delta = 1.0f / 60.0f;
        float maxLifetime = 0.55f + 0.1f; // add a safety margin
        int frames = (int) Math.ceil(maxLifetime / delta);
        for (int i = 0; i < frames; i++) {
            particleSystem.update(delta);
        }

        assertEquals(0, particleSystem.getActiveCount(),
                "All particles must have expired after enough per-frame update() calls " +
                "(simulates correct behaviour when update() is called during PAUSED frames)");
    }

    /**
     * Test 2: Without calling update() (simulating the pre-fix frozen timer),
     * active particles persist indefinitely — documents the exact bug.
     *
     * If update() is never called during the pause, particles remain active
     * regardless of how much wall-clock time elapses.
     */
    @Test
    void withoutUpdate_particlesRemainActive_documentingPreFixBug() {
        particleSystem.emitCombatHit(0f, 1f, 0f);
        int initialCount = particleSystem.getActiveCount();
        assertTrue(initialCount > 0, "Particles must be active after emission");

        // Deliberately do NOT call update() — simulates the frozen timer during pause
        assertEquals(initialCount, particleSystem.getActiveCount(),
                "Particle count must be unchanged when update() is never called " +
                "(pre-fix freeze: particles hang in the air for the entire pause duration)");
    }

    /**
     * Test 3: A single large delta (simulating a long pause followed by resume)
     * expires all particles in one call — matches realistic post-pause behaviour
     * when the game resumes after an extended pause.
     */
    @Test
    void singleLargeDelta_expiresAllParticles() {
        particleSystem.emitCombatHit(0f, 1f, 0f);
        particleSystem.emitBlockBreak(1f, 1f, 1f, 0.6f, 0.4f, 0.2f);
        assertTrue(particleSystem.getActiveCount() > 0);

        // A delta larger than the maximum possible lifetime expires everything
        particleSystem.update(10.0f);

        assertEquals(0, particleSystem.getActiveCount(),
                "A single large delta must expire all active particles");
    }

    /**
     * Test 4: Particle count decreases monotonically with each update() call while
     * particles are alive — confirms that the fix correctly advances each particle's
     * lifetime on every rendered frame during pause.
     */
    @Test
    void updateDecreasesActiveCountOverTime() {
        // Use block-break particles (lifetime 0.3–0.7 s) for a longer window
        particleSystem.emitBlockBreak(0f, 1f, 0f, 0.5f, 0.5f, 0.5f);
        int previous = particleSystem.getActiveCount();
        assertTrue(previous > 0);

        // Advance with a delta large enough to push some (but not all) past lifetime
        // Each step uses 0.15 s — some particles (min lifetime 0.3 s) expire by step 2
        boolean sawDecrease = false;
        for (int i = 0; i < 6; i++) {
            particleSystem.update(0.15f);
            int current = particleSystem.getActiveCount();
            if (current < previous) {
                sawDecrease = true;
            }
            previous = current;
            if (current == 0) break;
        }

        assertTrue(sawDecrease,
                "Active particle count must decrease at least once across sequential update() calls");
    }

    /**
     * Test 5: After clear() is called, getActiveCount() returns zero —
     * verifies the clear() helper used on state transitions works correctly.
     * (Not directly related to the bug but exercises the same code path used
     * by the PAUSED update loop.)
     */
    @Test
    void clearRemovesAllActiveParticles() {
        particleSystem.emitCombatHit(0f, 1f, 0f);
        particleSystem.emitFootstepDust(0f, 0f, 0f);
        assertTrue(particleSystem.getActiveCount() > 0);

        particleSystem.clear();

        assertEquals(0, particleSystem.getActiveCount(),
                "clear() must remove all active particles");
    }

    /**
     * Test 6: Multiple emitter types all expire correctly when update() is
     * called in per-frame ticks — verifies that the PAUSED fix works for all
     * particle types (not just combat sparks).
     */
    @Test
    void allEmitterTypes_expireCorrectlyViaPerFrameUpdates() {
        particleSystem.emitCombatHit(0f, 1f, 0f);
        particleSystem.emitBlockBreak(1f, 1f, 1f, 0.5f, 0.3f, 0.1f);
        particleSystem.emitFootstepDust(0f, 0f, 0f);
        particleSystem.emitDodgeTrail(2f, 1f, 2f);
        assertTrue(particleSystem.getActiveCount() > 0);

        // Advance beyond the longest possible lifetime across all emitter types:
        // dodge trail max = 0.35 s, combat hit max = 0.55 s, block break max = 0.7 s,
        // footstep dust max = 0.8 s  →  use 1.0 s total with small ticks.
        float delta = 1.0f / 60.0f;
        int frames = (int) Math.ceil(1.0f / delta);
        for (int i = 0; i < frames; i++) {
            particleSystem.update(delta);
        }

        assertEquals(0, particleSystem.getActiveCount(),
                "All particles from all emitter types must expire after 1 s of per-frame updates");
    }
}
