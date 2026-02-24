package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.World;

/**
 * Manages exposure effects on the player based on weather and shelter status.
 *
 * Indoor shelter (detected by ShelterDetector) protects the player from all
 * weather-related exposure effects:
 *   - RAIN: energy drain multiplier is not applied when sheltered
 *   - COLD_SNAP at night: health drain does not apply when sheltered
 */
public class ExposureSystem {

    /**
     * Get the effective energy drain multiplier for the current weather,
     * taking shelter into account. Sheltered players are not affected by
     * rain's energy drain penalty.
     *
     * @param weather       the current weather
     * @param world         the voxel world
     * @param playerPosition the player's current position
     * @return 1.0f if sheltered (or weather has no penalty), otherwise the
     *         weather's energy drain multiplier
     */
    public float getEffectiveEnergyDrainMultiplier(Weather weather, World world, Vector3 playerPosition) {
        float multiplier = weather.getEnergyDrainMultiplier();
        if (multiplier <= 1.0f) {
            return multiplier;
        }
        // Sheltered players are shielded from adverse weather energy effects
        if (ShelterDetector.isSheltered(world, playerPosition)) {
            return 1.0f;
        }
        return multiplier;
    }

    /**
     * Check whether cold-snap health drain should apply to the player.
     * Returns true only when:
     *   - the weather causes health drain at night, AND
     *   - it is currently night, AND
     *   - the player is not sheltered
     *
     * @param weather       the current weather
     * @param isNight       whether it is currently night
     * @param world         the voxel world
     * @param playerPosition the player's current position
     * @return true if health drain should be applied
     */
    public boolean isExposedToWeatherDamage(Weather weather, boolean isNight, World world, Vector3 playerPosition) {
        if (!weather.drainsHealthAtNight() || !isNight) {
            return false;
        }
        return !ShelterDetector.isSheltered(world, playerPosition);
    }
}
