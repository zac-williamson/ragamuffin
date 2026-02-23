package ragamuffin.core;

import com.badlogic.gdx.math.Vector3;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;

import java.util.*;

/**
 * Handles police arrest mechanics.
 * When caught by police, the player is teleported to the park,
 * has their health and hunger reduced, and loses confiscated items.
 *
 * This makes the dodge mechanic meaningful: successfully dodging police
 * is the only way to avoid losing your hard-scavenged gear.
 */
public class ArrestSystem {

    /** How many item stacks the police confiscate per arrest. */
    private static final int CONFISCATE_SLOTS = 3;

    /** Health after arrest — "roughed up in the back of the van". */
    private static final float HEALTH_AFTER_ARREST = 30f;

    /** Hunger after arrest — skipped a meal sitting in a cell. */
    private static final float HUNGER_AFTER_ARREST = 20f;

    /** Respawn position X/Z — dumped back on the park bench. */
    public static final Vector3 ARREST_RESPAWN = new Vector3(0, 1, 0);

    private final Random random;
    private float respawnY = ARREST_RESPAWN.y;

    public ArrestSystem() {
        this.random = new Random();
    }

    /**
     * Set the terrain-aware Y coordinate for arrest respawn.
     * Must be called after world generation so arrest places the player
     * above solid ground rather than inside it.
     */
    public void setRespawnY(float y) {
        this.respawnY = y;
    }

    public float getRespawnY() {
        return respawnY;
    }

    /**
     * Apply arrest consequences to the player.
     *
     * Confiscates up to CONFISCATE_SLOTS non-empty inventory slots chosen at random,
     * reduces health and hunger, then teleports the player back to the park.
     *
     * @param player    the player being arrested
     * @param inventory the player's inventory (items will be removed)
     * @return list of confiscated material names for display purposes
     */
    public List<String> arrest(Player player, Inventory inventory) {
        List<String> confiscated = confiscateItems(inventory);

        // Roughed up and processed — not at full health, and hungry from the wait
        player.setHealth(HEALTH_AFTER_ARREST);
        player.setHunger(HUNGER_AFTER_ARREST);

        // Dumped back at the park using terrain-aware Y to avoid spawning inside solid blocks
        player.getPosition().set(ARREST_RESPAWN.x, respawnY, ARREST_RESPAWN.z);
        player.setVerticalVelocity(0f);

        return confiscated;
    }

    /**
     * Confiscate a random selection of items from the player's inventory.
     * Prefers high-value items (diamond, tools) but is ultimately random —
     * the police aren't that organised.
     *
     * @return list of display names of confiscated items
     */
    public List<String> confiscateItems(Inventory inventory) {
        // Collect all non-empty slots
        List<Material> occupiedSlots = new ArrayList<>();
        for (Material material : Material.values()) {
            if (inventory.getItemCount(material) > 0) {
                occupiedSlots.add(material);
            }
        }

        if (occupiedSlots.isEmpty()) {
            return Collections.emptyList();
        }

        // Shuffle and take up to CONFISCATE_SLOTS
        Collections.shuffle(occupiedSlots, random);
        int toTake = Math.min(CONFISCATE_SLOTS, occupiedSlots.size());

        List<String> confiscated = new ArrayList<>();
        for (int i = 0; i < toTake; i++) {
            Material material = occupiedSlots.get(i);
            int count = inventory.getItemCount(material);
            // Confiscate half (minimum 1)
            int amount = Math.max(1, count / 2);
            inventory.removeItem(material, amount);
            confiscated.add(material.getDisplayName());
        }

        return confiscated;
    }

    /**
     * Build the arrest message for display to the player.
     */
    public static String buildArrestMessage(List<String> confiscated) {
        if (confiscated.isEmpty()) {
            return "Oi! You're nicked! Dumped back at the park with a caution.";
        }
        StringBuilder sb = new StringBuilder("You're nicked! They confiscated your ");
        for (int i = 0; i < confiscated.size(); i++) {
            if (i > 0) sb.append(i == confiscated.size() - 1 ? " and " : ", ");
            sb.append(confiscated.get(i));
        }
        sb.append(".");
        return sb.toString();
    }

    public static float getHealthAfterArrest() {
        return HEALTH_AFTER_ARREST;
    }

    public static float getHungerAfterArrest() {
        return HUNGER_AFTER_ARREST;
    }
}
