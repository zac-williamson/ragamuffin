package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages campfire blocks in the world (Issue #698).
 *
 * <p>Campfires are crafted from WOOD and placed as CAMPFIRE blocks.
 * They:
 * <ul>
 *   <li>Emit warmth to players within {@link WarmthSystem#CAMPFIRE_WARMTH_RADIUS} blocks</li>
 *   <li>Are extinguished by rain (DRIZZLE, RAIN, or THUNDERSTORM weather)</li>
 *   <li>Have flickering intensity via a sine-wave light pattern</li>
 *   <li>Attract police attention when lit near roads</li>
 * </ul>
 */
public class CampfireSystem {

    /** Frequency of campfire flicker in Hz. */
    public static final float FLICKER_FREQUENCY = 2.5f;

    /** Base light intensity of a campfire. */
    public static final float BASE_LIGHT_INTENSITY = 0.8f;

    /** Amplitude of flicker variation (±). */
    public static final float FLICKER_AMPLITUDE = 0.2f;

    private final List<Vector3> campfirePositions = new ArrayList<>();
    private float flickerTime = 0f;

    /**
     * Register a campfire at the given world position.
     */
    public void addCampfire(Vector3 position) {
        campfirePositions.add(new Vector3(position));
    }

    /**
     * Remove a campfire at the given world position (within 0.5 blocks).
     */
    public void removeCampfire(Vector3 position) {
        campfirePositions.removeIf(p -> p.dst(position) < 0.5f);
    }

    /**
     * Update campfire state each frame.
     * Extinguishes campfires when it is raining.
     *
     * @param world   the voxel world
     * @param weather current weather
     * @param delta   seconds since last frame
     */
    public void update(World world, Weather weather, float delta) {
        flickerTime += delta;

        if (weather.isRaining()) {
            // Extinguish all campfires when raining — replace CAMPFIRE block with AIR
            Iterator<Vector3> iter = campfirePositions.iterator();
            while (iter.hasNext()) {
                Vector3 pos = iter.next();
                int bx = (int) Math.floor(pos.x);
                int by = (int) Math.floor(pos.y);
                int bz = (int) Math.floor(pos.z);
                if (world.getBlock(bx, by, bz) == BlockType.CAMPFIRE) {
                    world.setBlock(bx, by, bz, BlockType.AIR);
                }
                iter.remove();
            }
        } else {
            // Sync: remove positions where block is no longer CAMPFIRE
            campfirePositions.removeIf(pos -> {
                int bx = (int) Math.floor(pos.x);
                int by = (int) Math.floor(pos.y);
                int bz = (int) Math.floor(pos.z);
                return world.getBlock(bx, by, bz) != BlockType.CAMPFIRE;
            });
        }
    }

    /**
     * Get the current flickering light intensity based on sine-wave oscillation.
     */
    public float getCurrentLightIntensity() {
        float flicker = (float) Math.sin(flickerTime * FLICKER_FREQUENCY * 2 * Math.PI);
        return BASE_LIGHT_INTENSITY + flicker * FLICKER_AMPLITUDE;
    }

    /**
     * Check if a given position is within campfire warmth radius.
     *
     * @param position the position to check
     * @return true if any active campfire is within {@link WarmthSystem#CAMPFIRE_WARMTH_RADIUS}
     */
    public boolean isNearCampfire(Vector3 position) {
        for (Vector3 campfire : campfirePositions) {
            if (campfire.dst(position) <= WarmthSystem.CAMPFIRE_WARMTH_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all active campfire positions (for rendering and police AI).
     */
    public List<Vector3> getCampfirePositions() {
        return campfirePositions;
    }

    /**
     * Whether there are any active campfires.
     */
    public boolean hasCampfires() {
        return !campfirePositions.isEmpty();
    }
}
