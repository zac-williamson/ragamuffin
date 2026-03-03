package ragamuffin.integration;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.core.CampfireSystem;
import ragamuffin.core.LightingSystem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1464: Add lighting system.
 *
 * Verifies that LightingSystem manages dynamic PointLights for campfires —
 * adding them when campfires are registered, updating intensity on flicker,
 * and removing them when campfires are extinguished.
 */
class Issue1464LightingSystemTest {

    private Environment environment;
    private LightingSystem lightingSystem;

    private static Environment buildEnvironment() {
        Environment env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        return env;
    }

    @BeforeEach
    void setUp() {
        environment = buildEnvironment();
        lightingSystem = new LightingSystem(environment);
    }

    /**
     * Test 1: No campfires → no point lights in environment.
     */
    @Test
    void noPointLightsWhenNoCampfires() {
        lightingSystem.updatePointLights(List.of(), CampfireSystem.BASE_LIGHT_INTENSITY);

        assertTrue(lightingSystem.getPointLights().isEmpty(),
                "No point lights should be registered when there are no campfires");
    }

    /**
     * Test 2: A single campfire adds exactly one PointLight at the campfire position.
     */
    @Test
    void singleCampfireAddsOnePointLight() {
        Vector3 campfirePos = new Vector3(10f, 1f, 10f);
        lightingSystem.updatePointLights(List.of(campfirePos), CampfireSystem.BASE_LIGHT_INTENSITY);

        List<PointLight> lights = lightingSystem.getPointLights();
        assertEquals(1, lights.size(), "Exactly one point light should be added for one campfire");

        PointLight light = lights.get(0);
        assertEquals(campfirePos.x, light.position.x, 0.01f, "Point light X must match campfire X");
        assertEquals(campfirePos.z, light.position.z, 0.01f, "Point light Z must match campfire Z");
        assertEquals(LightingSystem.CAMPFIRE_LIGHT_RADIUS, light.intensity, 0.01f,
                "Point light radius must equal CAMPFIRE_LIGHT_RADIUS");
    }

    /**
     * Test 3: Multiple campfires produce multiple point lights.
     */
    @Test
    void multipleCampfiresAddMultiplePointLights() {
        List<Vector3> campfires = List.of(
                new Vector3(5f, 1f, 5f),
                new Vector3(20f, 1f, 20f),
                new Vector3(-10f, 1f, 15f)
        );
        lightingSystem.updatePointLights(campfires, CampfireSystem.BASE_LIGHT_INTENSITY);

        assertEquals(3, lightingSystem.getPointLights().size(),
                "Three campfires must produce three point lights");
    }

    /**
     * Test 4: Updating point lights replaces old ones (no accumulation).
     * Calling updatePointLights twice must not double the light count.
     */
    @Test
    void updatePointLightsReplacesExistingLights() {
        Vector3 pos = new Vector3(10f, 1f, 10f);
        lightingSystem.updatePointLights(List.of(pos), CampfireSystem.BASE_LIGHT_INTENSITY);
        lightingSystem.updatePointLights(List.of(pos), CampfireSystem.BASE_LIGHT_INTENSITY);

        assertEquals(1, lightingSystem.getPointLights().size(),
                "Calling updatePointLights twice must not accumulate duplicate lights");
    }

    /**
     * Test 5: Campfire flicker — light intensity scales with the supplied value.
     * At minimum flicker the colour components should be dimmer than at maximum.
     */
    @Test
    void campfireFlickerAffectsLightIntensity() {
        Vector3 pos = new Vector3(10f, 1f, 10f);
        float minIntensity = CampfireSystem.BASE_LIGHT_INTENSITY - CampfireSystem.FLICKER_AMPLITUDE;
        float maxIntensity = CampfireSystem.BASE_LIGHT_INTENSITY + CampfireSystem.FLICKER_AMPLITUDE;

        lightingSystem.updatePointLights(List.of(pos), maxIntensity);
        float brightR = lightingSystem.getPointLights().get(0).color.r;

        lightingSystem.updatePointLights(List.of(pos), minIntensity);
        float dimR = lightingSystem.getPointLights().get(0).color.r;

        assertTrue(brightR > dimR,
                "Light should be brighter at max flicker intensity than at min flicker intensity");
    }

    /**
     * Test 6: clearPointLights() removes all managed point lights.
     */
    @Test
    void clearPointLightsRemovesAll() {
        lightingSystem.updatePointLights(
                List.of(new Vector3(1f, 1f, 1f), new Vector3(2f, 1f, 2f)),
                CampfireSystem.BASE_LIGHT_INTENSITY
        );
        assertEquals(2, lightingSystem.getPointLights().size());

        lightingSystem.clearPointLights();

        assertTrue(lightingSystem.getPointLights().isEmpty(),
                "clearPointLights() must remove all managed point lights");
    }

    /**
     * Test 7: dispose() removes point lights as well as the directional light.
     * After dispose(), getPointLights() must return an empty list.
     */
    @Test
    void disposeAlsoClearsPointLights() {
        lightingSystem.updatePointLights(
                List.of(new Vector3(5f, 1f, 5f)),
                CampfireSystem.BASE_LIGHT_INTENSITY
        );
        assertFalse(lightingSystem.getPointLights().isEmpty());

        lightingSystem.dispose();

        assertTrue(lightingSystem.getPointLights().isEmpty(),
                "dispose() must clear all point lights");
    }

    /**
     * Test 8: Extinguishing campfire (empty list) removes point light from environment.
     * After extinguishing, calling updatePointLights with an empty list leaves no lights.
     */
    @Test
    void extinguishingCampfireRemovesPointLight() {
        Vector3 pos = new Vector3(10f, 1f, 10f);
        lightingSystem.updatePointLights(List.of(pos), CampfireSystem.BASE_LIGHT_INTENSITY);
        assertEquals(1, lightingSystem.getPointLights().size(), "Light should exist before extinguish");

        // Campfire extinguished — pass empty list
        lightingSystem.updatePointLights(List.of(), CampfireSystem.BASE_LIGHT_INTENSITY);
        assertTrue(lightingSystem.getPointLights().isEmpty(),
                "Point light must be removed when campfire is extinguished");
    }
}
