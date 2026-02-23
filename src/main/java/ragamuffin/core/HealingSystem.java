package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.Player;

/**
 * Phase 11: Handles player healing mechanics.
 * Player heals 5 HP/s when resting (stationary + hunger > 50).
 */
public class HealingSystem {

    public static final float HEAL_RATE_PER_SECOND = 5.0f;
    public static final float MIN_HUNGER_FOR_HEALING = 50.0f;
    public static final float MOVEMENT_THRESHOLD = 0.5f; // Below this speed (blocks/s) = resting
    public static final float RESTING_DURATION_REQUIRED = 5.0f; // Must rest for 5 seconds

    private float restingTime;
    private Vector3 lastPosition;

    public HealingSystem() {
        this.restingTime = 0;
        this.lastPosition = new Vector3();
    }

    /**
     * Update healing system. Player heals if resting and hunger > 50.
     */
    public void update(float delta, Player player) {
        // Check if player is resting (not moving significantly)
        Vector3 currentPos = player.getPosition();
        float speed = (delta > 0) ? currentPos.dst(lastPosition) / delta : 0f;

        if (speed < MOVEMENT_THRESHOLD) {
            // Player is resting
            restingTime += delta;
        } else {
            // Player is moving
            restingTime = 0;
        }

        lastPosition.set(currentPos);

        // Heal if resting for at least the required duration and has enough hunger
        if (restingTime >= RESTING_DURATION_REQUIRED && player.getHunger() > MIN_HUNGER_FOR_HEALING) {
            float healAmount = HEAL_RATE_PER_SECOND * delta;
            player.heal(healAmount);
        }
    }

    /**
     * Reset resting time (e.g., when player moves significantly).
     */
    public void resetRestingTime() {
        restingTime = 0;
    }

    /**
     * Reset resting timer and update lastPosition to the given position.
     * Call this after any teleport (respawn, arrest) so the next update()
     * does not compute a spurious speed from the teleport distance.
     */
    public void resetPosition(Vector3 newPosition) {
        restingTime = 0;
        lastPosition.set(newPosition);
    }

    public float getRestingTime() {
        return restingTime;
    }
}
