package ragamuffin.core;

/**
 * Parallel 2-D ownership map over the world grid (Phase 8d / Issue #702).
 *
 * <p>Each cell tracks which {@link Faction} owns that (X, Z) column.  The map is
 * initialised to {@code null} (uncontested / neutral) for every cell.  Faction
 * indices (0 = Marchetti Crew, 1 = Street Lads, 2 = The Council, −1 = neutral) are
 * stored as {@code byte} values for memory efficiency.
 *
 * <p>Coordinates outside [0, width) × [0, depth) are silently ignored / return neutral.
 */
public class TurfMap {

    public static final byte NEUTRAL = -1;

    private final int width;
    private final int depth;
    private final byte[] ownership; // flat [x + z * width]

    public TurfMap(int width, int depth) {
        this.width    = width;
        this.depth    = depth;
        this.ownership = new byte[width * depth];
        java.util.Arrays.fill(ownership, NEUTRAL);
    }

    // ── Read / write ───────────────────────────────────────────────────────

    /** Set owner for (x, z). Pass {@code null} faction to reset to neutral. */
    public void setOwner(int x, int z, Faction faction) {
        if (!inBounds(x, z)) return;
        ownership[x + z * width] = (faction == null) ? NEUTRAL : (byte) faction.index();
    }

    /**
     * Returns the owning faction at (x, z), or {@code null} if neutral/uncontested.
     */
    public Faction getOwner(int x, int z) {
        if (!inBounds(x, z)) return null;
        byte idx = ownership[x + z * width];
        if (idx == NEUTRAL) return null;
        return Faction.values()[idx];
    }

    /** Returns true if the cell is neutral (no faction owns it). */
    public boolean isNeutral(int x, int z) {
        return getOwner(x, z) == null;
    }

    // ── Statistics ─────────────────────────────────────────────────────────

    /** Total number of cells owned by the given faction. */
    public int countOwned(Faction faction) {
        byte target = (byte) faction.index();
        int count = 0;
        for (byte b : ownership) {
            if (b == target) count++;
        }
        return count;
    }

    /** Total non-neutral cells. */
    public int countContested() {
        int count = 0;
        for (byte b : ownership) {
            if (b != NEUTRAL) count++;
        }
        return count;
    }

    /** Total cells in this map. */
    public int totalCells() {
        return width * depth;
    }

    /**
     * Fraction of all cells owned by the given faction (0.0 – 1.0).
     * Uses total cells (including neutral) as denominator to match the spec's
     * "60% turf control" victory condition.
     */
    public float ownershipFraction(Faction faction) {
        int total = totalCells();
        if (total == 0) return 0f;
        return (float) countOwned(faction) / total;
    }

    // ── Turf transfer ──────────────────────────────────────────────────────

    /**
     * Transfer 10 % of cells currently owned by {@code loser} to {@code winner}.
     * Called by FactionSystem when the Respect gap exceeds 30.
     *
     * @return the number of cells transferred
     */
    public int transferTurf(Faction loser, Faction winner) {
        byte loserIdx  = (byte) loser.index();
        byte winnerIdx = (byte) winner.index();

        // Collect all cells owned by loser
        java.util.List<Integer> loserCells = new java.util.ArrayList<>();
        for (int i = 0; i < ownership.length; i++) {
            if (ownership[i] == loserIdx) {
                loserCells.add(i);
            }
        }

        // Transfer 10 % (minimum 1 if any exist)
        int transferCount = Math.max(1, loserCells.size() / 10);
        transferCount = Math.min(transferCount, loserCells.size());

        // Pick cells from the start of the list (deterministic, no RNG needed here)
        for (int i = 0; i < transferCount; i++) {
            ownership[loserCells.get(i)] = winnerIdx;
        }
        return transferCount;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    public int getWidth()  { return width; }
    public int getDepth()  { return depth; }

    private boolean inBounds(int x, int z) {
        return x >= 0 && x < width && z >= 0 && z < depth;
    }
}
