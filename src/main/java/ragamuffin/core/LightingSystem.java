package ragamuffin.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;

/**
 * Manages dynamic lighting based on time of day.
 * Adjusts directional light (sun/moon) and ambient light to simulate day/night cycle.
 */
public class LightingSystem {

    private final Environment environment;
    private final DirectionalLight directionalLight;

    // Lighting colors for different times of day
    private static final Color DAY_DIRECTIONAL = new Color(1.0f, 1.0f, 0.95f, 1.0f);      // Bright white-yellow
    private static final Color NIGHT_DIRECTIONAL = new Color(0.15f, 0.15f, 0.25f, 1.0f);   // Dark blue
    private static final Color SUNRISE_DIRECTIONAL = new Color(1.0f, 0.7f, 0.5f, 1.0f);    // Orange
    private static final Color SUNSET_DIRECTIONAL = new Color(1.0f, 0.6f, 0.4f, 1.0f);     // Red-orange

    private static final Color DAY_AMBIENT = new Color(0.4f, 0.4f, 0.4f, 1.0f);            // Grey
    private static final Color NIGHT_AMBIENT = new Color(0.1f, 0.1f, 0.15f, 1.0f);          // Dark blue
    private static final Color DUSK_AMBIENT = new Color(0.25f, 0.2f, 0.3f, 1.0f);          // Purple

    // Sun direction vectors
    private static final Vector3 NOON_DIRECTION = new Vector3(-0.3f, -1.0f, -0.2f).nor();
    private static final Vector3 MIDNIGHT_DIRECTION = new Vector3(0.3f, 1.0f, 0.2f).nor();

    public LightingSystem(Environment environment) {
        this.environment = environment;

        // Get the existing directional light from environment
        // Assume the environment has one directional light already
        this.directionalLight = getDirectionalLightFromEnvironment(environment);

        if (directionalLight == null) {
            throw new IllegalStateException("Environment must have a DirectionalLight");
        }
    }

    /**
     * Extract the directional light from the environment.
     * Note: LibGDX doesn't expose directionalLights directly, so we need to
     * iterate through all attributes to find it.
     */
    @SuppressWarnings("unchecked")
    private DirectionalLight getDirectionalLightFromEnvironment(Environment env) {
        // Use reflection or iterate through attributes
        // For now, we'll assume there's at least one directional light
        // and access it via the shadow field (which is public in Array)
        try {
            // Access the directionalLights field using LibGDX's public API
            com.badlogic.gdx.utils.Array<DirectionalLight> lights =
                    (com.badlogic.gdx.utils.Array<DirectionalLight>)
                    env.getClass().getField("directionalLights").get(env);

            if (lights != null && lights.size > 0) {
                return lights.get(0);
            }
        } catch (Exception e) {
            // Fallback: create a new directional light and add it
            DirectionalLight light = new DirectionalLight();
            light.set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
            env.add(light);
            return light;
        }
        return null;
    }

    /**
     * Update lighting based on current time (0-24 hours) with seasonal sunrise/sunset.
     */
    public void updateLighting(float time) {
        updateLighting(time, 4.72f, 21.35f); // Default summer times
    }

    /**
     * Update lighting based on current time with explicit sunrise/sunset times.
     */
    public void updateLighting(float time, float sunrise, float sunset) {
        updateDirectionalLight(time, sunrise, sunset);
        updateAmbientLight(time, sunrise, sunset);
    }

    /**
     * Update the directional light (sun/moon) based on time and season.
     */
    private void updateDirectionalLight(float time, float sunrise, float sunset) {
        Color targetColor = new Color();
        Vector3 targetDirection = new Vector3();

        // Dawn transition: 1 hour before sunrise to 1 hour after
        float dawnStart = sunrise - 0.5f;
        float dawnEnd = sunrise + 1.0f;
        // Dusk transition: 1 hour before sunset to 1 hour after
        float duskStart = sunset - 1.0f;
        float duskEnd = sunset + 0.5f;

        if (time >= dawnStart && time < dawnEnd) {
            // Sunrise
            float t = (time - dawnStart) / (dawnEnd - dawnStart);
            targetColor.set(SUNRISE_DIRECTIONAL).lerp(DAY_DIRECTIONAL, t);
            targetDirection.set(MIDNIGHT_DIRECTION).lerp(NOON_DIRECTION, t);

        } else if (time >= dawnEnd && time < duskStart) {
            // Daytime — sun arcs east to west
            targetColor.set(DAY_DIRECTIONAL);
            float dayProgress = (time - dawnEnd) / (duskStart - dawnEnd);
            float angle = dayProgress * (float) Math.PI;
            targetDirection.set(
                    (float) Math.sin(angle) * -0.3f,
                    -1.0f,
                    (float) Math.cos(angle) * -0.2f
            ).nor();

        } else if (time >= duskStart && time < duskEnd) {
            // Sunset
            float t = (time - duskStart) / (duskEnd - duskStart);
            targetColor.set(DAY_DIRECTIONAL).lerp(SUNSET_DIRECTIONAL, t);
            targetDirection.set(NOON_DIRECTION).lerp(MIDNIGHT_DIRECTION, t);

        } else {
            // Night
            targetColor.set(NIGHT_DIRECTIONAL);
            targetDirection.set(MIDNIGHT_DIRECTION);
        }

        directionalLight.color.set(targetColor);
        directionalLight.direction.set(targetDirection);
    }

    /**
     * Update ambient lighting based on time and season.
     */
    private void updateAmbientLight(float time, float sunrise, float sunset) {
        ColorAttribute ambientAttr = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
        if (ambientAttr == null) {
            ambientAttr = new ColorAttribute(ColorAttribute.AmbientLight, DAY_AMBIENT);
            environment.set(ambientAttr);
        }

        Color targetColor = new Color();

        float dawnStart = sunrise - 0.5f;
        float dawnEnd = sunrise + 1.0f;
        float duskStart = sunset - 1.0f;
        float duskEnd = sunset + 0.5f;

        if (time >= dawnStart && time < dawnEnd) {
            // Dawn
            float t = (time - dawnStart) / (dawnEnd - dawnStart);
            targetColor.set(NIGHT_AMBIENT).lerp(DAY_AMBIENT, t);

        } else if (time >= dawnEnd && time < duskStart) {
            // Day
            targetColor.set(DAY_AMBIENT);

        } else if (time >= duskStart && time < duskEnd) {
            // Dusk
            float t = (time - duskStart) / (duskEnd - duskStart);
            targetColor.set(DAY_AMBIENT).lerp(DUSK_AMBIENT, t);

        } else if (time >= duskEnd || time < (dawnStart > 2.0f ? dawnStart - 2.0f : dawnStart)) {
            // Deep night
            targetColor.set(NIGHT_AMBIENT);

        } else {
            // Pre-dawn
            targetColor.set(NIGHT_AMBIENT);
        }

        ambientAttr.color.set(targetColor);
    }

    /**
     * Get the directional light for testing/inspection.
     */
    public DirectionalLight getDirectionalLight() {
        return directionalLight;
    }

    /**
     * Get current ambient light color for testing.
     */
    public Color getAmbientColor() {
        ColorAttribute ambientAttr = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
        if (ambientAttr != null) {
            return new Color(ambientAttr.color);
        }
        return new Color(Color.BLACK);
    }
}
