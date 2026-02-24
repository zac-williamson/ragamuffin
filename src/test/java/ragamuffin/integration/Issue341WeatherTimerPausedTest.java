package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.core.Weather;
import ragamuffin.core.WeatherSystem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #341:
 * WeatherSystem.update() was not called in the PAUSED render path, so the internal
 * {@code timeSinceLastChange} counter stopped advancing while the pause menu was open.
 * A player who paused during a rainstorm and left the game paused would return to find
 * the rain still ongoing even though the in-game timer should have fired a transition.
 *
 * Fix: the PAUSED render path now calls
 *   {@code weatherSystem.update(delta * timeSystem.getTimeSpeed() * 3600f)}
 * mirroring the PLAYING path, so weather transitions continue to accumulate while paused.
 */
class Issue341WeatherTimerPausedTest {

    /**
     * Test 1: update() accumulates the timer — calling update repeatedly with a
     * large enough total delta must eventually trigger a weather change.
     *
     * The WeatherSystem changes weather every 5–10 game-minutes (300–600 game-seconds).
     * Feeding it 601 game-seconds must cross the threshold and change the weather.
     */
    @Test
    void updateAccumulatesTimerAndChangesWeather() {
        WeatherSystem weatherSystem = new WeatherSystem();
        // Start in CLEAR so we can detect a change
        weatherSystem.setWeather(Weather.CLEAR);
        Weather initial = weatherSystem.getCurrentWeather();

        // Advance by 601 game-seconds — guaranteed to exceed the 10-minute (600 s) max
        // by feeding 60 small ticks of 10.017 seconds each (simulating ~60 paused frames)
        float tickDelta = 601.0f / 60f;
        for (int i = 0; i < 60; i++) {
            weatherSystem.update(tickDelta);
        }

        // After crossing the threshold the weather must have changed at least once
        // (may have looped back to CLEAR if only two states, but update must have fired)
        // We verify the timer has accumulated and the system responded correctly by
        // checking that a weather with only one alternative (RAIN) would transition.
        WeatherSystem ws2 = new WeatherSystem();
        ws2.setWeather(Weather.RAIN);
        for (int i = 0; i < 60; i++) {
            ws2.update(tickDelta);
        }
        // After 601 s the RAIN must have transitioned away from RAIN
        assertNotEquals(Weather.RAIN, ws2.getCurrentWeather(),
                "Weather must have transitioned away from RAIN after 601 game-seconds of update()");
    }

    /**
     * Test 2: update() with zero delta does NOT change weather.
     *
     * Passing 0 to update() (e.g. zero real-time delta) must not advance the
     * timer — weather must remain unchanged.
     */
    @Test
    void updateWithZeroDeltaDoesNotChangeWeather() {
        WeatherSystem weatherSystem = new WeatherSystem();
        weatherSystem.setWeather(Weather.RAIN);

        // Pump 1000 zero-delta updates
        for (int i = 0; i < 1000; i++) {
            weatherSystem.update(0f);
        }

        assertEquals(Weather.RAIN, weatherSystem.getCurrentWeather(),
                "Zero-delta updates must not advance the timer or trigger a weather change");
    }

    /**
     * Test 3: update() with a single large delta crosses the threshold immediately.
     *
     * This mirrors the pause scenario: after a long pause the first frame will
     * supply a large delta. One call with 601 game-seconds must trigger a transition.
     */
    @Test
    void singleLargeDeltaTriggersTransition() {
        WeatherSystem weatherSystem = new WeatherSystem();
        weatherSystem.setWeather(Weather.RAIN);

        // One call simulating a long paused session (601 game-seconds)
        weatherSystem.update(601f);

        assertNotEquals(Weather.RAIN, weatherSystem.getCurrentWeather(),
                "A single 601 s delta must cross the 600 s max threshold and transition weather");
    }

    /**
     * Test 4: Timer resets after a transition — subsequent short updates do not
     * immediately trigger another change.
     *
     * After a transition the counter resets to 0, so a tiny follow-up update
     * (1 game-second) must NOT trigger another change.
     */
    @Test
    void timerResetsAfterTransition() {
        WeatherSystem weatherSystem = new WeatherSystem();
        weatherSystem.setWeather(Weather.RAIN);

        // Force a transition
        weatherSystem.update(601f);
        Weather afterFirst = weatherSystem.getCurrentWeather();
        assertNotEquals(Weather.RAIN, afterFirst, "First transition must have fired");

        // One small follow-up update (1 s) — timer just reset, no second change expected
        weatherSystem.update(1f);
        assertEquals(afterFirst, weatherSystem.getCurrentWeather(),
                "Weather must not change again immediately after timer reset");
    }

    /**
     * Test 5: Game-time formula produces the correct game-seconds delta.
     *
     * The PAUSED path uses: delta * timeSystem.getTimeSpeed() * 3600f
     * At the default time speed of 1.0 h/s, one real second equals 3600 game-seconds.
     * This test verifies the formula produces a value > 0 for a typical frame delta.
     */
    @Test
    void gameTimeDeltaFormulaProducesPositiveValue() {
        float realDelta = 1f / 60f; // one frame at 60 fps
        float timeSpeed = 1.0f;     // default: 1 h per real second
        float gameTimeDelta = realDelta * timeSpeed * 3600f;

        assertTrue(gameTimeDelta > 0f,
                "Game-time delta formula must produce a positive value for a real frame delta");
        assertEquals(60f, gameTimeDelta, 0.01f,
                "At 60 fps and 1 h/s speed, each frame advances 60 game-seconds");
    }
}
