package ragamuffin.core;

/**
 * Manages the day/night cycle and game time.
 * Time is measured in hours (0.0 to 24.0).
 * Seasons affect sunrise/sunset times based on realistic British daylight hours.
 * Year = 365 days with real month lengths.
 * Game starts on Day 1 = 1st June (summer, long days).
 */
public class TimeSystem {

    private float time; // Current time in hours (0-24)
    private float timeSpeed; // How fast time passes (hours per real second)
    private int dayCount; // Number of days survived

    // Time constants
    private static final float DEFAULT_TIME_SPEED = 0.1f; // 1 real second = 6 in-game minutes
    private static final float HOURS_PER_DAY = 24.0f;
    private static final int DAYS_PER_YEAR = 365;
    private static final int START_DAY_OF_YEAR = 152; // June 1st (Jan=31, Feb=28, Mar=31, Apr=30, May=31 = 151, so June 1 = day 152, 0-indexed = 151)

    // Cumulative days at start of each month (0-indexed: Jan=0..30, Feb=31..58, etc.)
    private static final int[] MONTH_START_DAY = {
        0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334
    };
    private static final int[] MONTH_LENGTHS = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    // British sunrise/sunset extremes (approximate, London latitude ~51.5°N)
    // Summer solstice (June 21): sunrise ~04:43, sunset ~21:21
    // Winter solstice (Dec 21): sunrise ~08:04, sunset ~15:53
    private static final float SUMMER_SUNRISE = 4.72f;   // ~04:43
    private static final float WINTER_SUNRISE = 8.07f;   // ~08:04
    private static final float SUMMER_SUNSET = 21.35f;   // ~21:21
    private static final float WINTER_SUNSET = 15.88f;   // ~15:53

    // Month names for display
    private static final String[] MONTH_NAMES = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    private static final String[] SEASON_NAMES = {
        "Winter", "Winter", "Spring", "Spring", "Spring", "Summer",
        "Summer", "Summer", "Autumn", "Autumn", "Autumn", "Winter"
    };

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
     * Get the day of the year (0-364) accounting for game start date.
     */
    public int getDayOfYear() {
        return (START_DAY_OF_YEAR + dayCount - 1) % DAYS_PER_YEAR;
    }

    /**
     * Get seasonal factor (0.0 = winter solstice, 1.0 = summer solstice).
     * Uses cosine curve peaking at summer solstice (June 21 = day 172).
     */
    public float getSeasonalFactor() {
        int dayOfYear = getDayOfYear();
        // Summer solstice at day 172 (June 21), winter solstice at day 355 (Dec 21)
        // Cosine curve: 1.0 at summer solstice, 0.0 at winter solstice
        double angle = 2.0 * Math.PI * (dayOfYear - 172.0) / DAYS_PER_YEAR;
        return (float) (0.5 + 0.5 * Math.cos(angle));
    }

    /**
     * Get today's sunrise time in hours, based on season.
     */
    public float getSunriseTime() {
        float factor = getSeasonalFactor();
        // Lerp between winter and summer sunrise
        return WINTER_SUNRISE + (SUMMER_SUNRISE - WINTER_SUNRISE) * factor;
    }

    /**
     * Get today's sunset time in hours, based on season.
     */
    public float getSunsetTime() {
        float factor = getSeasonalFactor();
        // Lerp between winter and summer sunset
        return WINTER_SUNSET + (SUMMER_SUNSET - WINTER_SUNSET) * factor;
    }

    /**
     * Check if it's currently night time (before sunrise or after sunset).
     */
    public boolean isNight() {
        return time >= getSunsetTime() || time < getSunriseTime();
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
        return time / HOURS_PER_DAY;
    }

    /**
     * Get sun position factor (0.0 = below horizon, 1.0 = highest/noon).
     * Accounts for seasonal sunrise/sunset.
     */
    public float getSunPosition() {
        float sunrise = getSunriseTime();
        float sunset = getSunsetTime();
        float noon = (sunrise + sunset) / 2.0f;

        if (time < sunrise || time > sunset) {
            return 0.0f; // Below horizon
        }

        float distanceFromNoon = Math.abs(time - noon);
        float halfDay = (sunset - sunrise) / 2.0f;
        return 1.0f - (distanceFromNoon / halfDay);
    }

    /**
     * Get the current month (0-11, January=0).
     */
    public int getMonth() {
        int day = getDayOfYear();
        for (int m = 11; m >= 0; m--) {
            if (day >= MONTH_START_DAY[m]) {
                return m;
            }
        }
        return 0;
    }

    /**
     * Get the current month name.
     */
    public String getMonthName() {
        return MONTH_NAMES[getMonth()];
    }

    /**
     * Get the current season name.
     */
    public String getSeasonName() {
        return SEASON_NAMES[getMonth()];
    }

    /**
     * Get the day of the current month (1-28/29/30/31).
     */
    public int getDayOfMonth() {
        int day = getDayOfYear();
        int month = getMonth();
        return day - MONTH_START_DAY[month] + 1;
    }

    /**
     * Get daylight hours for today.
     */
    public float getDaylightHours() {
        return getSunsetTime() - getSunriseTime();
    }

    /**
     * Get the number of days survived.
     */
    public int getDayCount() {
        return dayCount;
    }

    /**
     * Get the current day index (alias for getDayCount, used by SquatSystem daily tick).
     */
    public int getDayIndex() {
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
            dayCount++;
        }
    }
}
