package ragamuffin.core;

/**
 * Weather states that affect gameplay.
 * Reflects the full spectrum of British weather.
 */
public enum Weather {
    CLEAR("Clear"),
    OVERCAST("Overcast"),
    DRIZZLE("Drizzle"),
    RAIN("Rain"),
    THUNDERSTORM("Thunderstorm"),
    FOG("Fog"),
    COLD_SNAP("Cold Snap"),
    FROST("Frost"),
    HEATWAVE("Heatwave");

    private final String displayName;

    Weather(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the energy drain multiplier for this weather.
     * Wet weather increases energy drain.
     */
    public float getEnergyDrainMultiplier() {
        switch (this) {
            case DRIZZLE:      return 1.2f;
            case RAIN:         return 1.5f;
            case THUNDERSTORM: return 1.8f;
            default:           return 1.0f;
        }
    }

    /**
     * Whether this weather causes rain (wetness accumulation).
     */
    public boolean isRaining() {
        switch (this) {
            case DRIZZLE:
            case RAIN:
            case THUNDERSTORM:
                return true;
            default:
                return false;
        }
    }

    /**
     * Rate at which wetness accumulates per second (when unsheltered).
     * 0 means no wetness.
     */
    public float getWetnessAccumulationRate() {
        switch (this) {
            case DRIZZLE:      return 2.0f;
            case RAIN:         return 5.0f;
            case THUNDERSTORM: return 10.0f;
            default:           return 0.0f;
        }
    }

    /**
     * Rate at which warmth drains per second (when outdoors).
     * Negative values represent warmth drain.
     */
    public float getWarmthDrainRate() {
        switch (this) {
            case COLD_SNAP:    return 3.0f;
            case FROST:        return 4.0f;
            case DRIZZLE:      return 1.0f;
            case RAIN:         return 2.0f;
            case THUNDERSTORM: return 3.0f;
            case OVERCAST:     return 0.5f;
            case FOG:          return 0.5f;
            case HEATWAVE:     return 0.0f; // No warmth drain — Hydration matters instead
            default:           return 0.0f;
        }
    }

    /**
     * Check if this weather drains health when unsheltered at night.
     * COLD_SNAP drains 2 HP/s at night if unsheltered.
     */
    public boolean drainsHealthAtNight() {
        return this == COLD_SNAP || this == FROST;
    }

    /**
     * Get the health drain rate per second for unsheltered players at night.
     */
    public float getHealthDrainRate() {
        switch (this) {
            case COLD_SNAP: return 2.0f;
            case FROST:     return 3.0f;
            default:        return 0.0f;
        }
    }

    /**
     * Whether this weather causes frost black ice patches on roads.
     */
    public boolean causesFrost() {
        return this == FROST;
    }

    /**
     * Whether police line-of-sight is halved (fog stealth bonus).
     */
    public boolean halvesPoliceLoS() {
        return this == FOG;
    }

    /**
     * Whether this weather drives NPC traffic to pub/pond (heatwave).
     */
    public boolean isHeatwave() {
        return this == HEATWAVE;
    }

    /**
     * Whether this weather causes non-hostile NPCs to flee indoors (thunderstorm).
     */
    public boolean causesEvacuation() {
        return this == THUNDERSTORM;
    }

    /**
     * Whether youth NPCs become more aggressive in this weather.
     */
    public boolean increasesYouthAggression() {
        return this == THUNDERSTORM || this == COLD_SNAP || this == FROST;
    }

    /**
     * Whether police patrols thin out in this weather.
     */
    public boolean thinsPolicePatrols() {
        return this == THUNDERSTORM || this == FROST;
    }
}
