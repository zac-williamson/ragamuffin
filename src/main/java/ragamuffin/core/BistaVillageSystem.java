package ragamuffin.core;

import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.entity.Player;
import ragamuffin.world.BlockType;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

/**
 * Issue #901: Portal to Bista Village.
 *
 * <p>Bista Village is a historically accurate recreation of the Nepali Gurkha
 * settlement established near Aldershot following the Gurkha Justice Campaign
 * of 2009.  Many veterans and their families settled in and around Aldershot
 * — the traditional home of the British Army's Gurkha Brigade — forming
 * tight-knit Nepali communities with their own shops, temples, and cultural
 * centres.
 *
 * <p>In-game, the village is a backrooms-style pocket dimension: a 56-by-56
 * block volume of monotone cream walls, green institutional linoleum, and
 * buzzing fluorescent ceiling strips.  A 7-by-7 grid of identical 8-block
 * room cells contains eerily identical Nepali shop interiors (counter, shelves,
 * coloured sign).  The centre cell is a temple with statue columns and carpet.
 * Roughly 25 percent of inter-room doorways are bricked up, creating dead-end
 * corridors that reinforce the liminal aesthetic.
 *
 * <p>The village is regenerated each time the player portals in because the
 * chunks (outside normal world bounds) are unloaded when the player returns
 * to the main town.
 */
public class BistaVillageSystem {

    /** World X coordinate of the Bista Village spawn point. */
    public static final float BISTA_SPAWN_X = 510f;
    /** World Y coordinate of the Bista Village spawn point (ground level + 1). */
    public static final float BISTA_SPAWN_Y = 2f;
    /** World Z coordinate of the Bista Village spawn point. */
    public static final float BISTA_SPAWN_Z = 510f;

    // ── Village generation constants ─────────────────────────────────────────

    /** Origin X of the village volume (chosen so spawn is centred in cell 3,3). */
    private static final int V_X0 = 482;
    /** Origin Z of the village volume. */
    private static final int V_Z0 = 482;
    /** Side length of the village in blocks (7 cells of 8). */
    private static final int V_SIZE = 56;
    /** Room cell size in blocks — walls lie on multiples of this value. */
    private static final int CELL = 8;
    /** Y level of the walkable floor. */
    private static final int FLOOR_Y = 1;
    /** Y level of the ceiling slab. */
    private static final int CEIL_Y = 5;

    // ── Instance state ───────────────────────────────────────────────────────

    /** Whether the player is currently in Bista Village. */
    private boolean inBistaVillage = false;

    /** The player's return position (set when they portal in). */
    private float returnX = 0f;
    private float returnY = 0f;
    private float returnZ = 0f;

    /** Reference to the game world for block placement. */
    private World world;

    /** Tooltip shown when the portal is used. */
    public static final String PORTAL_ACTIVATION_MESSAGE =
        "The stone hums with ancient energy. The terraced hills of Bista Village shimmer into view.";

    /** Tooltip shown when the return portal is used. */
    public static final String RETURN_PORTAL_MESSAGE =
        "You step back through the shimmer. The streets of Northfield close around you.";

    // ── World injection ──────────────────────────────────────────────────────

    /**
     * Provide the world reference so the village can place blocks.
     */
    public void setWorld(World world) {
        this.world = world;
    }

    // ── Portal activation ────────────────────────────────────────────────────

    /**
     * Attempt to activate the Bista Village portal.
     *
     * <p>Called when the player right-clicks while holding
     * {@link Material#BISTA_VILLAGE_PORTAL}.  Consumes the portal stone,
     * saves the return coordinates, generates the village blocks, teleports
     * the player, and grants a return portal stone.
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

        // Generate the village blocks (re-generated each visit because the
        // chunks are outside normal world bounds and get unloaded on return)
        generateVillage();

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

    // ══════════════════════════════════════════════════════════════════════════
    // Village generation — backrooms-style liminal Nepali settlement
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generate the entire Bista Village volume.
     *
     * <p>Layout: a sealed 56-by-56 box divided into a 7-by-7 grid of 8-block
     * cells.  Cream-rendered walls at every grid line.  Green institutional
     * lino floor.  Concrete ceiling with fluorescent yellow strips every 4th
     * column.  Doorways punched between most cells; roughly 25 percent are
     * omitted to create dead-end corridors.  The centre cell (3,3) is a temple.
     * Every other cell is an identical Nepali shop.
     */
    private void generateVillage() {
        if (world == null) return;

        final int X0 = V_X0, Z0 = V_Z0, SZ = V_SIZE;
        final int gridCount = SZ / CELL; // 7

        // ── Phase 1: Foundation, floor, air, ceiling ─────────────────────
        for (int x = X0; x < X0 + SZ; x++) {
            for (int z = Z0; z < Z0 + SZ; z++) {
                world.setBlock(x, 0, z, BlockType.BEDROCK);
                world.setBlock(x, FLOOR_Y, z, BlockType.LINO_GREEN);
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
                world.setBlock(x, CEIL_Y, z, BlockType.CONCRETE);
                // Extra slab above ceiling to stop sky bleed
                world.setBlock(x, CEIL_Y + 1, z, BlockType.CONCRETE);
            }
        }

        // ── Phase 2: Fluorescent light strips on ceiling ─────────────────
        for (int x = X0 + 2; x < X0 + SZ; x += 4) {
            for (int z = Z0; z < Z0 + SZ; z++) {
                world.setBlock(x, CEIL_Y, z, BlockType.SIGN_YELLOW);
            }
        }

        // ── Phase 3: Wall grid (cream render on every grid line) ─────────
        for (int x = X0; x < X0 + SZ; x++) {
            for (int z = Z0; z < Z0 + SZ; z++) {
                int rx = x - X0, rz = z - Z0;
                boolean edge = rx == 0 || rx == SZ - 1 || rz == 0 || rz == SZ - 1;
                if (edge || rx % CELL == 0 || rz % CELL == 0) {
                    for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                        world.setBlock(x, y, z, BlockType.RENDER_CREAM);
                    }
                }
            }
        }

        // ── Phase 4: Punch doorways (~75 % of interior walls) ────────────
        // Doors through east-west walls (walls along z-axis grid lines)
        for (int gx = 0; gx < gridCount; gx++) {
            for (int gz = 1; gz < gridCount; gz++) {
                int wallZ = Z0 + gz * CELL;
                int doorX = X0 + gx * CELL + CELL / 2;
                if (doorHash(gx, gz, 31, 17) % 4 == 0) continue; // dead end
                punchDoor(doorX, wallZ, true);
            }
        }
        // Doors through north-south walls (walls along x-axis grid lines)
        for (int gx = 1; gx < gridCount; gx++) {
            for (int gz = 0; gz < gridCount; gz++) {
                int wallX = X0 + gx * CELL;
                int doorZ = Z0 + gz * CELL + CELL / 2;
                if (doorHash(gx, gz, 13, 29) % 4 == 0) continue; // dead end
                punchDoor(wallX, doorZ, false);
            }
        }

        // ── Phase 5: Central temple (cell 3,3 — the spawn room) ─────────
        buildTemple(X0 + 3 * CELL, Z0 + 3 * CELL);

        // ── Phase 6: Identical Nepali shop interiors ─────────────────────
        for (int gx = 0; gx < gridCount; gx++) {
            for (int gz = 0; gz < gridCount; gz++) {
                if (gx == 3 && gz == 3) continue; // temple
                furnishShop(X0 + gx * CELL, Z0 + gz * CELL, gx, gz);
            }
        }

        // ── Phase 7: Guarantee spawn area is clear ───────────────────────
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    world.setBlock((int) BISTA_SPAWN_X + dx, y,
                                   (int) BISTA_SPAWN_Z + dz, BlockType.AIR);
                }
            }
        }

        // ── Phase 8: Mark affected chunks dirty ──────────────────────────
        markVillageChunksDirty();
    }

    // ── Generation helpers ───────────────────────────────────────────────────

    /** Deterministic hash for deciding which doorways to omit. */
    private static int doorHash(int a, int b, int p1, int p2) {
        return ((a + 1) * p1 + (b + 1) * p2 + 7) & 0xFF;
    }

    /**
     * Punch a 2-wide, 2-tall doorway at the given wall position.
     *
     * @param x      world X of the door's first block
     * @param z      world Z of the door's first block
     * @param alongX if true the door extends in +X; otherwise +Z
     */
    private void punchDoor(int x, int z, boolean alongX) {
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 2; y++) {
            world.setBlock(x, y, z, BlockType.AIR);
            if (alongX) {
                world.setBlock(x + 1, y, z, BlockType.AIR);
            } else {
                world.setBlock(x, y, z + 1, BlockType.AIR);
            }
        }
    }

    /**
     * Build the central temple in the cell starting at (tX, tZ).
     * Clears the interior, lays carpet, places statue columns at the four
     * interior corners, and forces doorways open on all four sides.
     */
    private void buildTemple(int tX, int tZ) {
        // Clear interior, lay carpet, fully lit ceiling
        for (int x = tX + 1; x < tX + CELL; x++) {
            for (int z = tZ + 1; z < tZ + CELL; z++) {
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
                world.setBlock(x, FLOOR_Y, z, BlockType.CARPET);
                world.setBlock(x, CEIL_Y, z, BlockType.SIGN_YELLOW);
            }
        }

        // Statue columns at interior corners
        int[] off = {2, CELL - 2};
        for (int cx : off) {
            for (int cz : off) {
                for (int y = FLOOR_Y + 1; y < CEIL_Y; y++) {
                    world.setBlock(tX + cx, y, tZ + cz, BlockType.STATUE);
                }
            }
        }

        // Bookshelf altar at the back wall
        world.setBlock(tX + 3, FLOOR_Y + 1, tZ + CELL - 1, BlockType.BOOKSHELF);
        world.setBlock(tX + 4, FLOOR_Y + 1, tZ + CELL - 1, BlockType.BOOKSHELF);
        world.setBlock(tX + 3, FLOOR_Y + 2, tZ + CELL - 1, BlockType.BOOKSHELF);
        world.setBlock(tX + 4, FLOOR_Y + 2, tZ + CELL - 1, BlockType.BOOKSHELF);

        // Force doorways open on all four cell walls
        int mid = CELL / 2;
        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 2; y++) {
            // West wall (x = tX)
            world.setBlock(tX, y, tZ + mid, BlockType.AIR);
            world.setBlock(tX, y, tZ + mid + 1, BlockType.AIR);
            // East wall (x = tX + CELL)
            world.setBlock(tX + CELL, y, tZ + mid, BlockType.AIR);
            world.setBlock(tX + CELL, y, tZ + mid + 1, BlockType.AIR);
            // South wall (z = tZ)
            world.setBlock(tX + mid, y, tZ, BlockType.AIR);
            world.setBlock(tX + mid + 1, y, tZ, BlockType.AIR);
            // North wall (z = tZ + CELL)
            world.setBlock(tX + mid, y, tZ + CELL, BlockType.AIR);
            world.setBlock(tX + mid + 1, y, tZ + CELL, BlockType.AIR);
        }
    }

    /**
     * Place identical shop furniture in a room cell.
     * Every shop has the same counter, shelves, and a coloured sign — the
     * uncanny repetition is the whole point.
     */
    private void furnishShop(int rx, int rz, int gx, int gz) {
        // Counter along far (north) interior wall
        world.setBlock(rx + 2, FLOOR_Y + 1, rz + CELL - 2, BlockType.COUNTER);
        world.setBlock(rx + 3, FLOOR_Y + 1, rz + CELL - 2, BlockType.COUNTER);
        world.setBlock(rx + 4, FLOOR_Y + 1, rz + CELL - 2, BlockType.COUNTER);

        // Shelves on left (west) interior wall — two rows high
        world.setBlock(rx + 1, FLOOR_Y + 1, rz + 2, BlockType.SHELF);
        world.setBlock(rx + 1, FLOOR_Y + 1, rz + 3, BlockType.SHELF);
        world.setBlock(rx + 1, FLOOR_Y + 2, rz + 2, BlockType.SHELF);
        world.setBlock(rx + 1, FLOOR_Y + 2, rz + 3, BlockType.SHELF);

        // Coloured sign above counter (cycles through 3 colours)
        BlockType sign;
        int variant = (gx + gz) % 3;
        if (variant == 0)      sign = BlockType.SIGN_RED;
        else if (variant == 1) sign = BlockType.SIGN_BLUE;
        else                   sign = BlockType.SIGN_GREEN;
        world.setBlock(rx + 3, FLOOR_Y + 3, rz + CELL - 2, sign);
    }

    /** Mark all chunks overlapping the village volume as dirty for mesh rebuild. */
    private void markVillageChunksDirty() {
        int chunkMinX = Math.floorDiv(V_X0, 16);
        int chunkMinZ = Math.floorDiv(V_Z0, 16);
        int chunkMaxX = Math.floorDiv(V_X0 + V_SIZE - 1, 16);
        int chunkMaxZ = Math.floorDiv(V_Z0 + V_SIZE - 1, 16);
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                world.markBlockDirty(cx * 16 + 1, FLOOR_Y, cz * 16 + 1);
            }
        }
    }
}
