package ragamuffin.core;

/**
 * Weather states that affect gameplay.
 */
public enum Weather {
    CLEAR("Clear"),
    OVERCAST("Overcast"),
    RAIN("Rain"),
    COLD_SNAP("Cold Snap");

    private final String displayName;

    Weather(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the energy drain multiplier for this weather.
     * RAIN increases energy drain by 50% (1.5x).
     */
    public float getEnergyDrainMultiplier() {
        if (this == RAIN) {
            return 1.5f;
        }
        return 1.0f;
    }

    /**
     * Check if this weather drains health when unsheltered at night.
     * COLD_SNAP drains 2 HP/s at night if unsheltered.
     */
    public boolean drainsHealthAtNight() {
        return this == COLD_SNAP;
    }

    /**
     * Get the health drain rate per second for unsheltered players at night.
     */
    public float getHealthDrainRate() {
        if (this == COLD_SNAP) {
            return 2.0f;
        }
        return 0.0f;
    }
}
