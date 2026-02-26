package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.DamageReason;
import ragamuffin.entity.Player;
import ragamuffin.world.World;

/**
 * Manages Warmth and Wetness survival mechanics (Issue #698).
 *
 * <p>Warmth drains outdoors in cold/wet weather and causes damage + speed
 * penalty below {@link Player#WARMTH_DANGER_THRESHOLD}. Wetness rises in
 * rain and accelerates warmth drain.
 *
 * <p>Clothing items modulate the rates:
 * <ul>
 *   <li>COAT: reduces warmth drain by 50%</li>
 *   <li>UMBRELLA: reduces wetness accumulation by 80%</li>
 *   <li>WOOLLY_HAT: reduces warmth drain from cold/frost by 40%</li>
 * </ul>
 */
public class WarmthSystem {

    /** Warmth restored per second when near a campfire. */
    public static final float CAMPFIRE_WARMTH_RATE = 15.0f;

    /** Warmth restored per second when sheltered indoors. */
    public static final float INDOOR_WARMTH_RATE = 5.0f;

    /** Warmth restored by drinking a FLASK_OF_TEA. */
    public static final float FLASK_OF_TEA_WARMTH = 30.0f;

    /** Distance from campfire that provides warmth. */
    public static final float CAMPFIRE_WARMTH_RADIUS = 5.0f;

    /**
     * Update warmth and wetness for the player based on current weather and shelter status.
     *
     * @param player        the player
     * @param weather       current weather
     * @param world         the voxel world
     * @param delta         seconds since last frame
     * @param nearCampfire  whether the player is within campfire warmth radius
     * @param inventory     player's inventory (to check clothing)
     */
    public void update(Player player, Weather weather, World world, float delta,
                       boolean nearCampfire, Inventory inventory) {
        boolean sheltered = ShelterDetector.isSheltered(world, player.getPosition());

        // Update wetness
        updateWetness(player, weather, sheltered, delta, inventory);

        // Update warmth
        updateWarmth(player, weather, sheltered, nearCampfire, delta, inventory);

        // Apply damage if warmth is dangerously low
        if (player.isWarmthDangerous()) {
            player.damage(Player.WARMTH_DAMAGE_PER_SECOND * delta, DamageReason.HYPOTHERMIA);
        }
    }

    /**
     * Update wetness: accumulates in rain outdoors, dries when sheltered or indoors.
     */
    private void updateWetness(Player player, Weather weather, boolean sheltered,
                                float delta, Inventory inventory) {
        if (weather.isRaining() && !sheltered) {
            float rate = weather.getWetnessAccumulationRate();
            // UMBRELLA reduces wetness accumulation by 80%
            if (inventory != null && inventory.hasItem(Material.UMBRELLA)) {
                rate *= 0.2f;
            }
            player.increaseWetness(rate * delta);
        } else {
            // Dry off when sheltered or not raining
            player.decreaseWetness(Player.WETNESS_DRY_RATE * delta);
        }
    }

    /**
     * Update warmth: drains outdoors in cold/wet weather, restored by campfire or indoors.
     */
    private void updateWarmth(Player player, Weather weather, boolean sheltered,
                               boolean nearCampfire, float delta, Inventory inventory) {
        if (nearCampfire) {
            // Campfire warms the player
            player.restoreWarmth(CAMPFIRE_WARMTH_RATE * delta);
            return;
        }

        if (sheltered) {
            // Gradually warm up indoors
            player.restoreWarmth(INDOOR_WARMTH_RATE * delta);
            return;
        }

        // Outdoors: drain warmth based on weather
        float drainRate = weather.getWarmthDrainRate();

        // Wetness accelerates warmth drain
        float wetnessRatio = player.getWetness() / Player.MAX_WETNESS;
        drainRate += drainRate * wetnessRatio * Player.WETNESS_WARMTH_DRAIN_MULTIPLIER;

        // COAT reduces warmth drain by 50%
        if (inventory != null && inventory.hasItem(Material.COAT)) {
            drainRate *= 0.5f;
        }

        // WOOLLY_HAT reduces warmth drain from cold/frost by 40%
        if (inventory != null && inventory.hasItem(Material.WOOLLY_HAT)) {
            if (weather == Weather.COLD_SNAP || weather == Weather.FROST) {
                drainRate *= 0.6f;
            }
        }

        if (drainRate > 0) {
            player.drainWarmth(drainRate * delta);
        }
    }

    /**
     * Apply the effect of drinking a FLASK_OF_TEA: instantly restores warmth.
     * Consumes one flask from inventory if available.
     *
     * @param player    the player
     * @param inventory the player's inventory
     * @return true if a flask was consumed
     */
    public boolean drinkFlaskOfTea(Player player, Inventory inventory) {
        if (inventory == null || !inventory.hasItem(Material.FLASK_OF_TEA)) {
            return false;
        }
        inventory.removeItem(Material.FLASK_OF_TEA, 1);
        player.restoreWarmth(FLASK_OF_TEA_WARMTH);
        return true;
    }
}
