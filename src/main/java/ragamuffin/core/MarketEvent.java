package ragamuffin.core;

import ragamuffin.building.Material;

/**
 * Issue #769: Market disruption events that spike prices and alter NPC behaviour.
 *
 * <p>Six events are defined, each with a duration (real seconds), affected commodities,
 * a price multiplier, and behavioural effects on NPCs. Events are triggered by the
 * {@link StreetEconomySystem} and broadcast via the {@link RumourNetwork}.
 */
public enum MarketEvent {

    /**
     * Greggs workers go on strike — pastry supply collapses.
     * GREGGS_PASTRY and DODGY_PASTY prices ×3.
     * HUNGRY need accumulation rate ×2.5 for all NPCs.
     * Greggs shopkeeper NPC switches to IDLE_AT_LANDMARK state.
     */
    GREGGS_STRIKE(
        "Greggs Strike",
        "The bakers are out. Not a sausage roll to be found.",
        180f, // 3 game minutes
        new Material[]{Material.GREGGS_PASTRY, Material.DODGY_PASTY, Material.SAUSAGE_ROLL, Material.STEAK_BAKE},
        3.0f
    ),

    /**
     * Lager shortage — off-licence runs dry.
     * CAN_OF_LAGER price ×4; PINT price ×2.
     * BORED need accumulation rate ×2 for all NPCs.
     * Barman NPC issues LOOT_TIP rumour about alternative suppliers.
     */
    LAGER_SHORTAGE(
        "Lager Shortage",
        "The off-licence is out. The Swan hasn't got a barrel left.",
        240f, // 4 game minutes
        new Material[]{Material.CAN_OF_LAGER, Material.PINT, Material.ENERGY_DRINK},
        4.0f
    ),

    /**
     * Sudden cold snap — demand for warm items surges.
     * WOOLLY_HAT_ECONOMY and COAT prices ×2.5.
     * COLD need accumulation rate ×3 for all NPCs.
     * WeatherSystem forced to COLD_SNAP state while event is active.
     */
    COLD_SNAP(
        "Cold Snap",
        "Proper bitter out. Everyone's looking for something warm.",
        300f, // 5 game minutes
        new Material[]{Material.WOOLLY_HAT_ECONOMY, Material.WOOLLY_HAT, Material.COAT, Material.FLASK_OF_TEA},
        2.5f
    ),

    /**
     * Benefit day — NPCs receive payments, BROKE need drops to 0 for all.
     * Prices on luxury goods (CAN_OF_LAGER, CIGARETTE) ×1.5 due to demand surge.
     * BENEFIT_FRAUD achievement unlockable during this window.
     * Player selling COUNTERFEIT_NOTE during this event triggers achievement.
     */
    BENEFIT_DAY(
        "Benefit Day",
        "Everyone's flush today. Don't let it go to waste.",
        120f, // 2 game minutes
        new Material[]{Material.CAN_OF_LAGER, Material.CIGARETTE, Material.TOBACCO_POUCH, Material.SCRATCH_CARD},
        1.5f
    ),

    /**
     * Council crackdown — enforcement presence increases.
     * CIGARETTE supply halved (price ×2); STOLEN_PHONE transactions risky.
     * Police patrols doubled in frequency (NPC spawner hint).
     * Wearing COUNCIL_JACKET grants 20% price bonus during this event.
     */
    COUNCIL_CRACKDOWN(
        "Council Crackdown",
        "The council are out in force. Keep it low-key.",
        200f, // 3.3 game minutes
        new Material[]{Material.CIGARETTE, Material.STOLEN_PHONE, Material.PRESCRIPTION_MEDS},
        2.0f
    ),

    /**
     * Marchetti shipment arrives — premium goods flood the black market.
     * PRESCRIPTION_MEDS price drops ×0.5 (supply glut).
     * DESPERATE need spikes for all NPCs (sudden availability triggers craving).
     * Triggers MARCHETTI_CREW faction rumour via RumourNetwork.
     */
    MARCHETTI_SHIPMENT(
        "Marchetti Shipment",
        "Something came in last night. The usual lot are sorted.",
        150f, // 2.5 game minutes
        new Material[]{Material.PRESCRIPTION_MEDS, Material.TOBACCO_POUCH, Material.STOLEN_PHONE},
        0.5f // price drop — flood market
    );

    private final String displayName;
    private final String description;
    /** How long this event lasts in real seconds. */
    private final float durationSeconds;
    /** Materials whose prices are affected. */
    private final Material[] affectedMaterials;
    /** Price multiplier applied to affected materials' base prices. */
    private final float priceMultiplier;

    MarketEvent(String displayName, String description, float durationSeconds,
                Material[] affectedMaterials, float priceMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.affectedMaterials = affectedMaterials;
        this.priceMultiplier = priceMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public Material[] getAffectedMaterials() {
        return affectedMaterials;
    }

    public float getPriceMultiplier() {
        return priceMultiplier;
    }

    /**
     * Returns true if this event affects the given material.
     */
    public boolean affects(Material material) {
        for (Material m : affectedMaterials) {
            if (m == material) return true;
        }
        return false;
    }
}
