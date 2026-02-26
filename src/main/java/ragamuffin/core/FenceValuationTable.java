package ragamuffin.core;

import ragamuffin.building.Material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines how much FOOD the Fence pays per item of stolen goods.
 *
 * Stolen goods accepted: DIAMOND, COMPUTER, SCRAP_METAL, STAPLER, OFFICE_CHAIR.
 * Payment is in FOOD units (the Fence's local currency equivalent).
 */
public class FenceValuationTable {

    /** Maps a sellable material to the FOOD units the Fence will pay per item. */
    private static final Map<Material, Integer> VALUATIONS;

    static {
        Map<Material, Integer> m = new HashMap<>();
        m.put(Material.DIAMOND,      10);  // Top dollar — premium stolen good
        m.put(Material.COMPUTER,      5);  // Good haul from an office job
        m.put(Material.SCRAP_METAL,   2);  // Bulk commodity
        m.put(Material.STAPLER,       1);  // Barely worth the risk
        m.put(Material.OFFICE_CHAIR,  3);  // Awkward to carry, mid-value
        VALUATIONS = Collections.unmodifiableMap(m);
    }

    /**
     * Returns the FOOD units paid per item for the given material,
     * or 0 if the Fence does not buy this material.
     */
    public int getValueFor(Material material) {
        return VALUATIONS.getOrDefault(material, 0);
    }

    /**
     * Returns true if the Fence will buy this material.
     */
    public boolean accepts(Material material) {
        return VALUATIONS.containsKey(material);
    }

    /**
     * Returns an unmodifiable view of all accepted materials and their valuations.
     */
    public Map<Material, Integer> getAllValuations() {
        return VALUATIONS;
    }
}
