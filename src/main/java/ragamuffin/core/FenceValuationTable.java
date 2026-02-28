package ragamuffin.core;

import ragamuffin.building.Material;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines how much FOOD/COIN the Fence pays per item.
 *
 * Stolen goods: DIAMOND, COMPUTER, SCRAP_METAL, STAPLER, OFFICE_CHAIR.
 * Skip salvage (Issue #936): OLD_SOFA, BROKEN_TELLY, WONKY_CHAIR, CARPET_ROLL,
 *   OLD_MATTRESS, FILING_CABINET, EXERCISE_BIKE, BOX_OF_RECORDS, MICROWAVE,
 *   SHOPPING_TROLLEY_GOLD, ANTIQUE_CLOCK.
 * Skip salvage items do NOT add to Notoriety when sold.
 */
public class FenceValuationTable {

    /** Materials that are skip salvage — legitimate items, no Notoriety gain on sale. */
    private static final Set<Material> SKIP_SALVAGE;

    static {
        SKIP_SALVAGE = Collections.unmodifiableSet(EnumSet.of(
            Material.OLD_SOFA,
            Material.BROKEN_TELLY,
            Material.WONKY_CHAIR,
            Material.CARPET_ROLL,
            Material.OLD_MATTRESS,
            Material.FILING_CABINET,
            Material.EXERCISE_BIKE,
            Material.BOX_OF_RECORDS,
            Material.MICROWAVE,
            Material.SHOPPING_TROLLEY_GOLD,
            Material.ANTIQUE_CLOCK
        ));
    }

    /** Maps a sellable material to the FOOD/COIN units the Fence will pay per item. */
    private static final Map<Material, Integer> VALUATIONS;

    static {
        Map<Material, Integer> m = new HashMap<>();
        m.put(Material.DIAMOND,      10);  // Top dollar — premium stolen good
        m.put(Material.COMPUTER,      5);  // Good haul from an office job
        m.put(Material.SCRAP_METAL,   2);  // Bulk commodity
        m.put(Material.STAPLER,       1);  // Barely worth the risk
        m.put(Material.OFFICE_CHAIR,  3);  // Awkward to carry, mid-value

        // ── Issue #936: Council Skip & Bulky Item Day — legitimate salvage ───────
        // These are NOT stolen goods; selling them does NOT add to Notoriety.
        m.put(Material.OLD_SOFA,               3);   // Common — 3 WOOD equivalent
        m.put(Material.BROKEN_TELLY,           4);   // Common — scrap electronics
        m.put(Material.WONKY_CHAIR,            2);   // Common — 2 WOOD equivalent
        m.put(Material.CARPET_ROLL,            3);   // Common — textile value
        m.put(Material.OLD_MATTRESS,           5);   // Uncommon — decent bulk
        m.put(Material.FILING_CABINET,         6);   // Uncommon — metal scrap
        m.put(Material.EXERCISE_BIKE,          7);   // Uncommon — mechanical parts
        m.put(Material.BOX_OF_RECORDS,         8);   // Uncommon — vinyl collectables
        m.put(Material.MICROWAVE,             10);   // Rare — working appliance
        m.put(Material.SHOPPING_TROLLEY_GOLD, 12);   // Rare — golden trolley curiosity
        m.put(Material.ANTIQUE_CLOCK,         20);   // Very Rare — triggers special dialogue

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

    /**
     * Returns true if this material is skip salvage (legal — no Notoriety gain on sale).
     */
    public boolean isSkipSalvage(Material material) {
        return SKIP_SALVAGE.contains(material);
    }

    /**
     * Returns an unmodifiable view of all skip salvage materials.
     */
    public Set<Material> getSkipSalvageMaterials() {
        return SKIP_SALVAGE;
    }
}
