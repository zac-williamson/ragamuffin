package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;
import ragamuffin.world.LandmarkType;

/**
 * Issue #901: Portal to Bista Village.
 *
 * <p>Bista Village is a historically accurate recreation of the Nepali Gurkha
 * settlement established near Aldershot following the Gurkha Justice Campaign
 * of 2009. After years of legal battles led by Joanna Lumley and the Gurkha
 * Justice Campaign, the UK Home Office granted settlement rights to pre-1997
 * Gurkha veterans in May 2009. Many veterans and their families settled in
 * and around Aldershot — the traditional home of the British Army's Gurkha
 * Brigade — forming tight-knit Nepali communities with their own shops,
 * temples, and cultural centres.
 *
 * <p>Bista Village in-game is a pocket dimension accessible only through the
 * craftable {@link Material#BISTA_VILLAGE_PORTAL} item. When the player
 * right-clicks with the portal stone, this system:
 * <ol>
 *   <li>Removes the portal stone from the inventory (single use).</li>
 *   <li>Records the player's current (return) position.</li>
 *   <li>Teleports the player to the Bista Village spawn point.</li>
 *   <li>Sets the active location to {@link LandmarkType#BISTA_VILLAGE}.</li>
 *   <li>Grants the player a {@link Material#BISTA_VILLAGE_PORTAL return portal stone}
 *       so they can travel back.</li>
 * </ol>
 *
 * <p>The village layout (for rendering / collision purposes) is a 40×40 block
 * area placed at world offset (500, 1, 500) — well beyond the main town
 * boundary of 200×200.  The world generator does not place this area in the
 * normal generation pass; it is instantiated on first portal use.
 */
public class BistaVillageSystem {

    /** World X coordinate of the Bista Village spawn point. */
    public static final float BISTA_SPAWN_X = 510f;
    /** World Y coordinate of the Bista Village spawn point (ground level + 1). */
    public static final float BISTA_SPAWN_Y = 2f;
    /** World Z coordinate of the Bista Village spawn point. */
    public static final float BISTA_SPAWN_Z = 510f;

    /** Whether the player is currently in Bista Village. */
    private boolean inBistaVillage = false;

    /** The player's return position (set when they portal in). */
    private float returnX = 0f;
    private float returnY = 0f;
    private float returnZ = 0f;

    /** Tooltip shown when the portal is used. */
    public static final String PORTAL_ACTIVATION_MESSAGE =
        "The stone hums with ancient energy. The terraced hills of Bista Village shimmer into view.";

    /** Tooltip shown when the return portal is used. */
    public static final String RETURN_PORTAL_MESSAGE =
        "You step back through the shimmer. The streets of Northfield close around you.";

    /**
     * Attempt to activate the Bista Village portal.
     *
     * <p>Called when the player right-clicks while holding
     * {@link Material#BISTA_VILLAGE_PORTAL}.  Consumes the portal stone,
     * saves the return coordinates, teleports the player, and grants a
     * return portal stone.
     *
     * @param player    the player entity
     * @param inventory the player's inventory
     * @return the activation message to display, or {@code null} if the
     *         player does not hold a portal stone
     */
    public String activatePortal(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return null;
        }
        if (inventory.getItemCount(Material.BISTA_VILLAGE_PORTAL) < 1) {
            return null;
        }
        if (inBistaVillage) {
            // Already there — use return portal instead
            return activateReturnPortal(player, inventory);
        }

        // Consume the portal stone
        inventory.removeItem(Material.BISTA_VILLAGE_PORTAL, 1);

        // Record return position
        returnX = player.getPosition().x;
        returnY = player.getPosition().y;
        returnZ = player.getPosition().z;

        // Teleport to Bista Village
        player.teleport(BISTA_SPAWN_X, BISTA_SPAWN_Y, BISTA_SPAWN_Z);

        // Mark as in-village
        inBistaVillage = true;

        // Grant return portal stone
        inventory.addItem(Material.BISTA_VILLAGE_PORTAL, 1);

        return PORTAL_ACTIVATION_MESSAGE;
    }

    /**
     * Activate the return portal (used while the player is already in Bista Village).
     *
     * @param player    the player entity
     * @param inventory the player's inventory
     * @return the return message to display
     */
    public String activateReturnPortal(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return null;
        }

        // Consume the return portal stone
        inventory.removeItem(Material.BISTA_VILLAGE_PORTAL, 1);

        // Teleport back to the return position
        player.teleport(returnX, returnY, returnZ);

        inBistaVillage = false;

        return RETURN_PORTAL_MESSAGE;
    }

    /**
     * Returns {@code true} if the player is currently in Bista Village.
     */
    public boolean isInBistaVillage() {
        return inBistaVillage;
    }

    /**
     * Returns the current active landmark for the player's position.
     * Returns {@link LandmarkType#BISTA_VILLAGE} when in the village,
     * or {@code null} when in the main town.
     */
    public LandmarkType getCurrentLandmark() {
        return inBistaVillage ? LandmarkType.BISTA_VILLAGE : null;
    }
}
