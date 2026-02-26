package ragamuffin.core;

import java.util.Random;

/**
 * Manages weather changes and effects.
 * Weather changes every 5-10 game minutes (2-6 transitions per in-game day).
 * Transition probabilities reflect British weather patterns.
 */
public class WeatherSystem {
    private Weather currentWeather;
    private float timeSinceLastChange;
    private float nextChangeTime;
    private final Random random;

    private static final float MIN_WEATHER_DURATION = 5.0f * 60.0f;  // 5 game minutes
    private static final float MAX_WEATHER_DURATION = 10.0f * 60.0f; // 10 game minutes

    /**
     * British weather Markov transition matrix.
     * Each row represents the current weather state (by ordinal).
     * Each column represents the probability of transitioning to that new state.
     * Row order: CLEAR, OVERCAST, DRIZZLE, RAIN, THUNDERSTORM, FOG, COLD_SNAP, FROST, HEATWAVE
     */
    private static final float[][] TRANSITION_MATRIX = {
        // From CLEAR:       CLEAR   OCAST   DRIZ    RAIN    TSTORM  FOG     COLD    FROST   HEAT
                            {0.00f,  0.35f,  0.20f,  0.15f,  0.05f,  0.10f,  0.05f,  0.05f,  0.05f},
        // From OVERCAST:
                            {0.15f,  0.00f,  0.25f,  0.30f,  0.10f,  0.10f,  0.05f,  0.05f,  0.00f},
        // From DRIZZLE:
                            {0.10f,  0.20f,  0.00f,  0.35f,  0.15f,  0.10f,  0.05f,  0.05f,  0.00f},
        // From RAIN:
                            {0.10f,  0.25f,  0.20f,  0.00f,  0.20f,  0.10f,  0.10f,  0.05f,  0.00f},
        // From THUNDERSTORM:
                            {0.05f,  0.20f,  0.30f,  0.30f,  0.00f,  0.05f,  0.05f,  0.05f,  0.00f},
        // From FOG:
                            {0.20f,  0.35f,  0.20f,  0.15f,  0.05f,  0.00f,  0.05f,  0.00f,  0.00f},
        // From COLD_SNAP:
                            {0.10f,  0.15f,  0.10f,  0.20f,  0.10f,  0.10f,  0.00f,  0.25f,  0.00f},
        // From FROST:
                            {0.10f,  0.20f,  0.10f,  0.15f,  0.05f,  0.15f,  0.25f,  0.00f,  0.00f},
        // From HEATWAVE:
                            {0.20f,  0.30f,  0.20f,  0.15f,  0.05f,  0.05f,  0.05f,  0.00f,  0.00f},
    };

    public WeatherSystem() {
        this.random = new Random();
        this.currentWeather = Weather.CLEAR;
        this.timeSinceLastChange = 0.0f;
        this.nextChangeTime = getRandomWeatherDuration();
    }

    /**
     * Update the weather system.
     * @param deltaTime time since last update in seconds (game time, not real time)
     */
    public void update(float deltaTime) {
        timeSinceLastChange += deltaTime;

        if (timeSinceLastChange >= nextChangeTime) {
            changeWeather();
            timeSinceLastChange = 0.0f;
            nextChangeTime = getRandomWeatherDuration();
        }
    }

    /**
     * Change to a new weather state using British weather transition probabilities.
     */
    private void changeWeather() {
        Weather[] weathers = Weather.values();
        int currentOrdinal = currentWeather.ordinal();
        float[] transitions = TRANSITION_MATRIX[currentOrdinal];

        // Weighted random selection
        float roll = random.nextFloat();
        float cumulative = 0.0f;
        for (int i = 0; i < transitions.length; i++) {
            cumulative += transitions[i];
            if (roll < cumulative) {
                currentWeather = weathers[i];
                return;
            }
        }
        // Fallback in case of floating point rounding: pick last non-zero option
        for (int i = transitions.length - 1; i >= 0; i--) {
            if (transitions[i] > 0.0f) {
                currentWeather = weathers[i];
                return;
            }
        }
    }

    /**
     * Get a random duration for the next weather period.
     */
    private float getRandomWeatherDuration() {
        return MIN_WEATHER_DURATION + random.nextFloat() * (MAX_WEATHER_DURATION - MIN_WEATHER_DURATION);
    }

    /**
     * Get the current weather.
     */
    public Weather getCurrentWeather() {
        return currentWeather;
    }

    /**
     * Set the weather directly (for testing).
     */
    public void setWeather(Weather weather) {
        this.currentWeather = weather;
    }

    /**
     * Get the time since the last weather change (seconds).
     */
    public float getTimeSinceLastChange() {
        return timeSinceLastChange;
    }
}
