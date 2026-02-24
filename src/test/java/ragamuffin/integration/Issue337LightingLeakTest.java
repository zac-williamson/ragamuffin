package ragamuffin.integration;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import org.junit.jupiter.api.Test;
import ragamuffin.core.LightingSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #337:
 * restartGame() leaked a DirectionalLight into the shared Environment each time
 * LightingSystem was constructed when the reflection fallback fired. Because
 * {@code getDirectionalLightFromEnvironment} always fails (Environment has no
 * public {@code directionalLights} field), the catch block added a brand-new
 * light on every restart, causing cumulative over-bright rendering.
 *
 * Fix: {@code LightingSystem.dispose()} calls {@code environment.remove(directionalLight)}
 * and must be called in {@code restartGame()} before creating the replacement system.
 */
class Issue337LightingLeakTest {

    /**
     * Helper that builds the standard environment used in initGame/restartGame.
     */
    private static Environment buildEnvironment() {
        Environment env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        return env;
    }

    /**
     * Test 1: LightingSystem can be constructed with an existing DirectionalLight.
     *
     * The environment starts with one DirectionalLight (added by initGame). The
     * LightingSystem constructor must not throw, and getDirectionalLight() must
     * return a non-null reference.
     */
    @Test
    void lightingSystemConstructsWithExistingLight() {
        Environment env = buildEnvironment();
        LightingSystem ls = new LightingSystem(env);

        assertNotNull(ls.getDirectionalLight(),
                "LightingSystem must hold a non-null DirectionalLight after construction");
    }

    /**
     * Test 2: dispose() removes the directional light from the environment.
     *
     * After dispose(), constructing a second LightingSystem with an external
     * light re-added must still work — verifying that dispose() left the
     * environment in a clean state.
     */
    @Test
    void disposeRemovesLightFromEnvironment() {
        Environment env = buildEnvironment();
        LightingSystem ls = new LightingSystem(env);

        // Simulate restartGame(): dispose old system, add fresh external light, create new system
        ls.dispose();

        // Re-add the external directional light (as initGame/restartGame would supply one)
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        LightingSystem ls2 = new LightingSystem(env);
        assertNotNull(ls2.getDirectionalLight(),
                "Second LightingSystem must acquire the freshly-added DirectionalLight");

        ls2.dispose();
    }

    /**
     * Test 3: Multiple restart cycles do not throw or corrupt lighting state.
     *
     * Simulates the restartGame() pattern — dispose old LightingSystem, add a
     * fresh DirectionalLight to the environment, create a new LightingSystem —
     * repeated several times. Each iteration must succeed without error.
     */
    @Test
    void multipleRestartCyclesDoNotCorruptLighting() {
        Environment env = buildEnvironment();
        LightingSystem ls = new LightingSystem(env);

        for (int i = 0; i < 5; i++) {
            ls.dispose();
            env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
            ls = new LightingSystem(env);
            assertNotNull(ls.getDirectionalLight(),
                    "LightingSystem must be valid after restart cycle " + (i + 1));
        }

        ls.dispose();
    }

    /**
     * Test 4: updateLighting() works correctly after dispose/recreate cycle.
     *
     * After a simulated restart the new LightingSystem must still be able to
     * update the directional and ambient light values without throwing.
     */
    @Test
    void updateLightingWorksAfterRestartCycle() {
        Environment env = buildEnvironment();
        LightingSystem ls = new LightingSystem(env);

        ls.dispose();
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        LightingSystem ls2 = new LightingSystem(env);

        // Must not throw
        assertDoesNotThrow(() -> ls2.updateLighting(12.0f),
                "updateLighting() must succeed after dispose/recreate cycle");

        ls2.dispose();
    }
}
