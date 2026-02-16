package ragamuffin.building;

/**
 * Represents a tool with durability that reduces the hits needed to break blocks.
 */
public class Tool {
    private final Material material;
    private int durability;
    private final int maxDurability;

    public Tool(Material material, int maxDurability) {
        this.material = material;
        this.maxDurability = maxDurability;
        this.durability = maxDurability;
    }

    public Material getMaterial() {
        return material;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    /**
     * Use the tool, reducing durability by 1.
     * @return true if the tool broke (durability reached 0)
     */
    public boolean use() {
        if (durability > 0) {
            durability--;
        }
        return durability == 0;
    }

    /**
     * Check if the tool is broken (durability is 0).
     */
    public boolean isBroken() {
        return durability <= 0;
    }

    /**
     * Get the hits multiplier for this tool type.
     * Bare fist = 1.0 (5 hits for soft blocks)
     * Improvised tool = 0.6 (3 hits for soft blocks)
     * Stone tool = 0.4 (2 hits for soft blocks)
     */
    public static float getHitsMultiplier(Material toolMaterial) {
        if (toolMaterial == Material.STONE_TOOL) {
            return 0.4f;
        } else if (toolMaterial == Material.IMPROVISED_TOOL) {
            return 0.6f;
        }
        return 1.0f; // bare fist or non-tool
    }

    /**
     * Get the maximum durability for a tool type.
     */
    public static int getMaxDurability(Material toolMaterial) {
        if (toolMaterial == Material.IMPROVISED_TOOL) {
            return 20;
        } else if (toolMaterial == Material.STONE_TOOL) {
            return 50;
        }
        return 0; // not a tool
    }

    /**
     * Check if a material is a tool.
     */
    public static boolean isTool(Material material) {
        return material == Material.IMPROVISED_TOOL || material == Material.STONE_TOOL;
    }
}
