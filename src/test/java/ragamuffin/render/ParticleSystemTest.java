package ragamuffin.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParticleSystem}.
 *
 * These tests exercise the particle simulation logic only (emit, update, clear,
 * active count), without touching LibGDX rendering or the headless backend.
 */
class ParticleSystemTest {

    private ParticleSystem ps;

    @BeforeEach
    void setup() {
        ps = new ParticleSystem();
    }

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    void initiallyEmpty() {
        assertEquals(0, ps.getActiveCount(), "No particles should be active at construction");
    }

    // -----------------------------------------------------------------------
    // emitCombatHit
    // -----------------------------------------------------------------------

    @Test
    void emitCombatHitProducesParticles() {
        ps.emitCombatHit(0f, 0f, 0f);
        assertTrue(ps.getActiveCount() > 0, "emitCombatHit should produce at least one active particle");
    }

    @Test
    void emitCombatHitProducesExpectedCount() {
        ps.emitCombatHit(0f, 0f, 0f);
        // Implementation emits 8 per call
        assertEquals(8, ps.getActiveCount());
    }

    // -----------------------------------------------------------------------
    // emitBlockBreak
    // -----------------------------------------------------------------------

    @Test
    void emitBlockBreakProducesParticles() {
        ps.emitBlockBreak(5f, 3f, 7f, 0.5f, 0.3f, 0.1f);
        assertTrue(ps.getActiveCount() > 0, "emitBlockBreak should produce at least one active particle");
    }

    @Test
    void emitBlockBreakProducesExpectedCount() {
        ps.emitBlockBreak(0f, 0f, 0f, 1f, 0f, 0f);
        // Implementation emits 12 per call
        assertEquals(12, ps.getActiveCount());
    }

    // -----------------------------------------------------------------------
    // emitFootstepDust
    // -----------------------------------------------------------------------

    @Test
    void emitFootstepDustProducesParticles() {
        ps.emitFootstepDust(0f, 0f, 0f);
        assertTrue(ps.getActiveCount() > 0, "emitFootstepDust should produce at least one active particle");
    }

    @Test
    void emitFootstepDustProducesExpectedCount() {
        ps.emitFootstepDust(0f, 0f, 0f);
        // Implementation emits 4 per call
        assertEquals(4, ps.getActiveCount());
    }

    // -----------------------------------------------------------------------
    // emitDodgeTrail
    // -----------------------------------------------------------------------

    @Test
    void emitDodgeTrailProducesParticles() {
        ps.emitDodgeTrail(0f, 1f, 0f);
        assertTrue(ps.getActiveCount() > 0, "emitDodgeTrail should produce at least one active particle");
    }

    @Test
    void emitDodgeTrailProducesExpectedCount() {
        ps.emitDodgeTrail(0f, 1f, 0f);
        // Implementation emits 3 per call
        assertEquals(3, ps.getActiveCount());
    }

    // -----------------------------------------------------------------------
    // Multiple emits accumulate
    // -----------------------------------------------------------------------

    @Test
    void multipleEmitsAccumulateParticles() {
        ps.emitCombatHit(0f, 0f, 0f);    // 8
        ps.emitBlockBreak(1f, 1f, 1f, 1f, 0f, 0f); // 12
        ps.emitFootstepDust(2f, 0f, 2f); // 4
        ps.emitDodgeTrail(0f, 1f, 0f);  // 3
        assertEquals(27, ps.getActiveCount(), "Total active should be sum of all emitted particles");
    }

    // -----------------------------------------------------------------------
    // update() — particles expire over time
    // -----------------------------------------------------------------------

    @Test
    void particlesExpireAfterLifetime() {
        ps.emitCombatHit(0f, 0f, 0f);
        int initial = ps.getActiveCount();
        assertTrue(initial > 0, "Should have active particles after emit");

        // Advance well past the maximum lifetime (0.55 s for combat hits)
        ps.update(1.0f);

        assertEquals(0, ps.getActiveCount(), "All particles should have expired after 1 second");
    }

    @Test
    void particlesPartiallyActiveBeforeExpiry() {
        ps.emitCombatHit(0f, 0f, 0f);
        // Advance a very short time — particles should still be alive
        ps.update(0.01f);
        assertTrue(ps.getActiveCount() > 0, "Particles should still be active after a tiny delta");
    }

    @Test
    void updateDoesNothingWhenEmpty() {
        // No particles — update should not throw
        assertDoesNotThrow(() -> ps.update(1.0f));
        assertEquals(0, ps.getActiveCount());
    }

    // -----------------------------------------------------------------------
    // clear()
    // -----------------------------------------------------------------------

    @Test
    void clearRemovesAllParticles() {
        ps.emitCombatHit(0f, 0f, 0f);
        ps.emitBlockBreak(0f, 0f, 0f, 1f, 0f, 0f);
        assertTrue(ps.getActiveCount() > 0, "Should have active particles before clear");

        ps.clear();

        assertEquals(0, ps.getActiveCount(), "clear() should remove all active particles");
    }

    @Test
    void canEmitAfterClear() {
        ps.emitCombatHit(0f, 0f, 0f);
        ps.clear();
        ps.emitFootstepDust(0f, 0f, 0f);
        assertEquals(4, ps.getActiveCount(), "Should be able to emit again after clear");
    }

    // -----------------------------------------------------------------------
    // Pool limit (512 particles)
    // -----------------------------------------------------------------------

    @Test
    void poolLimitNotExceededByExcessiveEmission() {
        // Each emitCombatHit emits 8 particles; 70 calls = 560 > 512 pool limit
        for (int i = 0; i < 70; i++) {
            ps.emitCombatHit(0f, 0f, 0f);
        }
        // Active count must not exceed MAX_PARTICLES
        assertTrue(ps.getActiveCount() <= 512,
            "Active particle count should not exceed pool limit of 512, was " + ps.getActiveCount());
    }
}
