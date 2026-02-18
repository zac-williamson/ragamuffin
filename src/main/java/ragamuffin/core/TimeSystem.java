package ragamuffin.core;

/**
 * Manages the day/night cycle and game time.
 * Time is measured in hours (0.0 to 24.0).
 */
public class TimeSystem {

    private float time; // Current time in hours (0-24)
    private float timeSpeed; // How fast time passes (hours per real second)
    private int dayCount; // Number of days survived

    // Time constants
    private static final float DEFAULT_TIME_SPEED = 0.1f; // 1 real second = 6 in-game minutes
    private static final float HOURS_PER_DAY = 24.0f;

    // Day/night thresholds
    public static final float NIGHT_START = 20.0f; // 20:00 (8 PM)
    public static final float NIGHT_END = 6.0f;    // 06:00 (6 AM)

    public TimeSystem() {
        this.time = 8.0f; // Start at 8:00 AM
        this.timeSpeed = DEFAULT_TIME_SPEED;
        this.dayCount = 1;
    }

    public TimeSystem(float startTime) {
        this.time = startTime % HOURS_PER_DAY;
        this.timeSpeed = DEFAULT_TIME_SPEED;
        this.dayCount = 1;
    }

    /**
     * Update time based on delta time.
     */
    public void update(float delta) {
        time += timeSpeed * delta;

        // Wrap around at 24 hours
        if (time >= HOURS_PER_DAY) {
            time -= HOURS_PER_DAY;
            dayCount++;
        }
    }

    /**
     * Get current time in hours (0-24).
     */
    public float getTime() {
        return time;
    }

    /**
     * Set the current time in hours (0-24).
     */
    public void setTime(float hours) {
        this.time = hours % HOURS_PER_DAY;
        if (this.time < 0) {
            this.time += HOURS_PER_DAY;
        }
    }

    /**
     * Get time as hours and minutes (e.g., 13.5 -> 13, 30).
     */
    public int getHours() {
        return (int) time;
    }

    public int getMinutes() {
        return (int) ((time - getHours()) * 60);
    }

    /**
     * Get formatted time string (e.g., "13:05").
     */
    public String getTimeString() {
        int hours = getHours();
        int minutes = getMinutes();
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Check if it's currently night time.
     */
    public boolean isNight() {
        return time >= NIGHT_START || time < NIGHT_END;
    }

    /**
     * Check if it's currently day time.
     */
    public boolean isDay() {
        return !isNight();
    }

    /**
     * Get normalized time of day (0.0 = midnight, 0.5 = noon, 1.0 = midnight).
     * Useful for lighting calculations.
     */
    public float getNormalizedTime() {
        // Map 0-24 hours to 0-1, where 0/1 = midnight, 0.5 = noon
        return time / HOURS_PER_DAY;
    }

    /**
     * Get sun position factor (0.0 = lowest/midnight, 1.0 = highest/noon).
     */
    public float getSunPosition() {
        // Calculate sun height based on time
        // Noon (12:00) = 1.0 (highest)
        // Midnight (0:00/24:00) = 0.0 (lowest)

        float noon = 12.0f;
        float distanceFromNoon = Math.abs(time - noon);

        // Map distance from noon (0-12) to sun height (1.0-0.0)
        return 1.0f - (distanceFromNoon / 12.0f);
    }

    /**
     * Get the number of days survived.
     */
    public int getDayCount() {
        return dayCount;
    }

    /**
     * Set time speed multiplier.
     */
    public void setTimeSpeed(float speed) {
        this.timeSpeed = speed;
    }

    public float getTimeSpeed() {
        return timeSpeed;
    }

    /**
     * Advance time by a specific number of hours.
     */
    public void advanceTime(float hours) {
        time += hours;
        if (time >= HOURS_PER_DAY) {
            time -= HOURS_PER_DAY;
        }
    }
}
