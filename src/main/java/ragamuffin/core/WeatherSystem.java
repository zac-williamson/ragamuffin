package ragamuffin.core;

import java.util.Random;

/**
 * Manages weather changes and effects.
 * Weather changes at most once per game day (every 12-24 game hours).
 */
public class WeatherSystem {
    private Weather currentWeather;
    private float timeSinceLastChange;
    private float nextChangeTime;
    private final Random random;

    private static final float MIN_WEATHER_DURATION = 12.0f * 3600.0f; // 12 game hours in seconds
    private static final float MAX_WEATHER_DURATION = 24.0f * 3600.0f; // 24 game hours in seconds

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
     * Change to a random new weather state.
     */
    private void changeWeather() {
        Weather[] weathers = Weather.values();
        Weather newWeather;

        // Pick a different weather than the current one
        do {
            newWeather = weathers[random.nextInt(weathers.length)];
        } while (newWeather == currentWeather && weathers.length > 1);

        currentWeather = newWeather;
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
}
