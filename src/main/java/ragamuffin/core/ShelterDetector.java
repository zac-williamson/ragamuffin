package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.world.BlockType;
import ragamuffin.world.World;

/**
 * Detects if a player is inside a shelter (protected from weather and police).
 * A shelter requires a roof + at least 3 walls around the player.
 */
public class ShelterDetector {

    /**
     * Check if the player is sheltered at their current position.
     * Requires:
     * - A solid block above (roof)
     * - At least 3 solid blocks around (left, right, front, back)
     */
    public static boolean isSheltered(World world, Vector3 playerPosition) {
        int x = Math.round(playerPosition.x);
        int y = Math.round(playerPosition.y);
        int z = Math.round(playerPosition.z);

        // Check for roof (block above)
        BlockType above = world.getBlock(x, y + 2, z); // 2 blocks up (player height)
        if (!above.isSolid()) {
            return false;
        }

        // Check for walls around the player
        int wallCount = 0;

        // Left (negative x)
        BlockType left = world.getBlock(x - 1, y, z);
        if (left.isSolid()) wallCount++;

        // Right (positive x)
        BlockType right = world.getBlock(x + 1, y, z);
        if (right.isSolid()) wallCount++;

        // Front (negative z)
        BlockType front = world.getBlock(x, y, z - 1);
        if (front.isSolid()) wallCount++;

        // Back (positive z)
        BlockType back = world.getBlock(x, y, z + 1);
        if (back.isSolid()) wallCount++;

        // Need at least 3 walls to be considered sheltered
        return wallCount >= 3;
    }
}
