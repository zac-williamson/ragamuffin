package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.entity.Player;
import ragamuffin.ui.TooltipSystem;
import ragamuffin.ui.TooltipTrigger;

/**
 * Phase 11: Handles player death and respawn mechanics.
 */
public class RespawnSystem {

    public static final String RESPAWN_MESSAGE = "You wake up on a park bench. Again.";
    public static final float RESPAWN_MESSAGE_DURATION = 3.0f;
    public static final Vector3 PARK_CENTRE = new Vector3(0, 1, 0);

    private boolean isRespawning;
    private float respawnTimer;
    private String currentMessage;
    private float spawnY = PARK_CENTRE.y;

    public RespawnSystem() {
        this.isRespawning = false;
        this.respawnTimer = 0;
        this.currentMessage = null;
    }

    /**
     * Set the terrain-aware Y coordinate for respawn.
     * Must be called after world generation so respawn places the player
     * above solid ground rather than inside it.
     */
    public void setSpawnY(float y) {
        this.spawnY = y;
    }

    public float getSpawnY() {
        return spawnY;
    }

    /**
     * Check if player is dead and trigger respawn.
     * @return true if respawn was triggered
     */
    public boolean checkAndTriggerRespawn(Player player, TooltipSystem tooltipSystem) {
        if (player.isDead() && !isRespawning) {
            // Trigger death tooltip
            tooltipSystem.trigger(TooltipTrigger.FIRST_DEATH);

            // Start respawn sequence
            isRespawning = true;
            respawnTimer = RESPAWN_MESSAGE_DURATION;
            currentMessage = RESPAWN_MESSAGE;
            return true;
        }
        return false;
    }

    /**
     * Update respawn timer and perform respawn when ready.
     */
    public void update(float delta, Player player) {
        if (isRespawning) {
            respawnTimer -= delta;

            if (respawnTimer <= 0) {
                // Perform respawn
                performRespawn(player);
                isRespawning = false;
                currentMessage = null;
            }
        }
    }

    /**
     * Respawn the player at park centre with restored stats.
     * Inventory is preserved.
     */
    private void performRespawn(Player player) {
        // Respawn at park centre using terrain-aware Y to avoid spawning inside solid blocks.
        // Use teleport() to atomically sync the AABB to the new position so collision
        // detection is correct on the very first frame after respawn (fixes #184).
        player.teleport(PARK_CENTRE.x, spawnY, PARK_CENTRE.z);
        player.setVerticalVelocity(0f);

        // Restore stats and revive
        player.setHealth(50);
        player.setHunger(50);
        player.setEnergy(100);
        player.revive();
    }

    public boolean isRespawning() {
        return isRespawning;
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public float getRespawnTimer() {
        return respawnTimer;
    }

    /**
     * For testing: manually trigger respawn.
     */
    public void triggerRespawn(Player player) {
        isRespawning = true;
        respawnTimer = RESPAWN_MESSAGE_DURATION;
        currentMessage = RESPAWN_MESSAGE;
    }
}
