package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.render.SkyRenderer;
import ragamuffin.render.SkyRenderer.ConstellationStar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #790: Add realistic constellation positioning to star map.
 *
 * Verifies that Orion, the Plough (Ursa Major), and Cassiopeia are defined with
 * catalogue-accurate positions, shift correctly with the day of year (sidereal
 * drift), and that their constituent stars have distinct positions so the
 * patterns are recognisable.
 *
 * No LibGDX rendering backend is required — only the logic/data methods are tested.
 */
class Issue790ConstellationPositioningTest {

    // -----------------------------------------------------------------------
    // Constellation data existence
    // -----------------------------------------------------------------------

    @Test
    void constellationStars_arrayIsNotEmpty() {
        assertTrue(SkyRenderer.CONSTELLATION_STARS.length > 0,
                "CONSTELLATION_STARS must contain at least one entry");
    }

    @Test
    void constellationNames_includesOrionPloughCassiopeia() {
        Set<String> names = new HashSet<>(Arrays.asList(SkyRenderer.CONSTELLATION_NAMES));
        assertTrue(names.contains("Orion"),
                "CONSTELLATION_NAMES must include Orion");
        assertTrue(names.contains("Ursa Major"),
                "CONSTELLATION_NAMES must include Ursa Major (the Plough)");
        assertTrue(names.contains("Cassiopeia"),
                "CONSTELLATION_NAMES must include Cassiopeia");
    }

    @Test
    void orion_hasAtLeastSevenStars() {
        long orionCount = Arrays.stream(SkyRenderer.CONSTELLATION_STARS)
                .filter(s -> "Orion".equals(s.constellation))
                .count();
        assertTrue(orionCount >= 7,
                "Orion must have at least 7 stars (belt + shoulders + feet + head), got " + orionCount);
    }

    @Test
    void plough_hasSevenStars() {
        long ploughCount = Arrays.stream(SkyRenderer.CONSTELLATION_STARS)
                .filter(s -> "Ursa Major".equals(s.constellation))
                .count();
        assertEquals(7, ploughCount,
                "Ursa Major / Plough must have exactly 7 stars (the classic asterism), got " + ploughCount);
    }

    @Test
    void cassiopeia_hasFiveStars() {
        long casCount = Arrays.stream(SkyRenderer.CONSTELLATION_STARS)
                .filter(s -> "Cassiopeia".equals(s.constellation))
                .count();
        assertEquals(5, casCount,
                "Cassiopeia must have exactly 5 stars (the W/M pattern), got " + casCount);
    }

    // -----------------------------------------------------------------------
    // Coordinate validity
    // -----------------------------------------------------------------------

    @Test
    void allConstellationStars_worldYawAtEpochInRange() {
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            assertTrue(cs.worldYawAtEpoch >= 0f && cs.worldYawAtEpoch < 360f,
                    cs.constellation + "/" + cs.starName
                    + " worldYawAtEpoch must be in [0, 360), got " + cs.worldYawAtEpoch);
        }
    }

    @Test
    void allConstellationStars_elevationFractionInRange() {
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            assertTrue(cs.elevationFraction >= 0f && cs.elevationFraction <= 1f,
                    cs.constellation + "/" + cs.starName
                    + " elevationFraction must be in [0, 1], got " + cs.elevationFraction);
        }
    }

    @Test
    void allConstellationStars_magnitudeIsReasonable() {
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            assertTrue(cs.magnitude >= 0f && cs.magnitude <= 6f,
                    cs.constellation + "/" + cs.starName
                    + " magnitude should be in naked-eye range [0, 6], got " + cs.magnitude);
        }
    }

    // -----------------------------------------------------------------------
    // Orion geometric correctness
    // -----------------------------------------------------------------------

    @Test
    void orionBelt_threeStarsCloseInRightAscension() {
        // The belt (Alnitak, Alnilam, Mintaka) spans ~2° in RA (~0.13h) and
        // sits near the celestial equator.  Their epoch yaws should be within
        // 10° of each other.
        double[] beltYaws = Arrays.stream(SkyRenderer.CONSTELLATION_STARS)
                .filter(s -> "Orion".equals(s.constellation)
                          && (s.starName.equals("Alnitak")
                           || s.starName.equals("Alnilam")
                           || s.starName.equals("Mintaka")))
                .mapToDouble(s -> s.worldYawAtEpoch)
                .toArray();

        assertEquals(3, beltYaws.length,
                "Should find exactly 3 Orion belt stars (Alnitak, Alnilam, Mintaka)");

        // All three should be within 10° of each other
        double minYaw = Arrays.stream(beltYaws).min().getAsDouble();
        double maxYaw = Arrays.stream(beltYaws).max().getAsDouble();
        assertTrue(maxYaw - minYaw < 10.0,
                "Orion belt stars should span < 10° in world yaw; span was " + (maxYaw - minYaw));
    }

    @Test
    void orionBelt_nearCelestialEquator() {
        // Belt stars should have low declination → low elevationFraction from 52°N.
        // Dec ≈ -2° to 0° → elevation fraction ≈ (dec+38)/128 ≈ 0.28–0.30
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            if ("Orion".equals(cs.constellation)
                    && (cs.starName.equals("Alnitak")
                     || cs.starName.equals("Alnilam")
                     || cs.starName.equals("Mintaka"))) {
                assertTrue(cs.elevationFraction < 0.40f,
                        "Orion belt star " + cs.starName
                        + " should be in lower sky (elevationFraction < 0.40), got "
                        + cs.elevationFraction);
            }
        }
    }

    @Test
    void orionBetelgeuse_higherThanRigel() {
        // Betelgeuse (dec ~+7°) should have higher elevationFraction than Rigel (dec ~-8°)
        ConstellationStar betelgeuse = null, rigel = null;
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            if ("Orion".equals(cs.constellation)) {
                if ("Betelgeuse".equals(cs.starName)) betelgeuse = cs;
                if ("Rigel".equals(cs.starName))      rigel      = cs;
            }
        }
        assertNotNull(betelgeuse, "Betelgeuse must be present in constellation data");
        assertNotNull(rigel,      "Rigel must be present in constellation data");
        assertTrue(betelgeuse.elevationFraction > rigel.elevationFraction,
                "Betelgeuse (dec ~+7°) should be higher in the sky than Rigel (dec ~-8°)");
    }

    // -----------------------------------------------------------------------
    // Plough / Ursa Major correctness
    // -----------------------------------------------------------------------

    @Test
    void ploughStars_areHighInSkyFromBritain() {
        // All Plough stars have dec ~49–62°, so elevationFraction should be > 0.65
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            if ("Ursa Major".equals(cs.constellation)) {
                assertTrue(cs.elevationFraction > 0.65f,
                        "Plough star " + cs.starName
                        + " should be high in the sky from Britain (elevationFraction > 0.65), got "
                        + cs.elevationFraction);
            }
        }
    }

    @Test
    void ploughStars_spreadAcrossRightAscension() {
        // The Plough spans RA ~11h to ~14h (yaw ~165° to ~207°), a spread of ~42°.
        double minYaw = Double.MAX_VALUE, maxYaw = Double.MIN_VALUE;
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            if ("Ursa Major".equals(cs.constellation)) {
                if (cs.worldYawAtEpoch < minYaw) minYaw = cs.worldYawAtEpoch;
                if (cs.worldYawAtEpoch > maxYaw) maxYaw = cs.worldYawAtEpoch;
            }
        }
        assertTrue(maxYaw - minYaw > 20.0,
                "Plough stars should span > 20° in world yaw (pattern extends across the sky); span was "
                + (maxYaw - minYaw));
    }

    // -----------------------------------------------------------------------
    // Seasonal drift
    // -----------------------------------------------------------------------

    @Test
    void constellationStars_yawChangesWithSeason() {
        SkyRenderer sky = new SkyRenderer();
        // All constellation stars should move measurably over 90 days
        for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
            float yawDay0  = sky.getConstellationStarWorldYaw(cs, 0);
            float yawDay90 = sky.getConstellationStarWorldYaw(cs, 90);
            assertNotEquals(yawDay0, yawDay90, 0.5f,
                    cs.constellation + "/" + cs.starName
                    + " should move in the sky between day 0 and day 90");
        }
    }

    @Test
    void constellationStarWorldYaw_isInRange() {
        SkyRenderer sky = new SkyRenderer();
        for (int day = 0; day < 365; day++) {
            for (ConstellationStar cs : SkyRenderer.CONSTELLATION_STARS) {
                float yaw = sky.getConstellationStarWorldYaw(cs, day);
                assertTrue(yaw >= 0f && yaw < 360f,
                        cs.constellation + "/" + cs.starName
                        + " world yaw must be in [0, 360) on day " + day + ", got " + yaw);
            }
        }
    }

    @Test
    void constellationStars_siderealDriftMatchesBackgroundStars() {
        // Verify the drift rate is ~0.9856°/day (same as background stars).
        // Over 365 days a star should return to nearly its original position
        // (365 * 0.9856 ≈ 359.7° ≈ 0° wrap).
        SkyRenderer sky = new SkyRenderer();
        ConstellationStar first = SkyRenderer.CONSTELLATION_STARS[0];
        float yawDay0   = sky.getConstellationStarWorldYaw(first, 0);
        float yawDay365 = sky.getConstellationStarWorldYaw(first, 365);
        // After 365 days the star should be within ~1° of its original position
        float diff = Math.abs(yawDay0 - yawDay365);
        if (diff > 180f) diff = 360f - diff; // handle wrap-around
        assertTrue(diff < 2.0f,
                "After 365 days a constellation star should be within 2° of its start position; diff was " + diff);
    }

    // -----------------------------------------------------------------------
    // Star uniqueness
    // -----------------------------------------------------------------------

    @Test
    void constellationStars_allHaveUniquePositions() {
        // No two constellation stars should have identical epoch world yaw
        // (that would indicate a data entry error).
        for (int i = 0; i < SkyRenderer.CONSTELLATION_STARS.length; i++) {
            for (int j = i + 1; j < SkyRenderer.CONSTELLATION_STARS.length; j++) {
                ConstellationStar a = SkyRenderer.CONSTELLATION_STARS[i];
                ConstellationStar b = SkyRenderer.CONSTELLATION_STARS[j];
                assertNotEquals(a.worldYawAtEpoch, b.worldYawAtEpoch, 0.01f,
                        "Stars " + a.constellation + "/" + a.starName
                        + " and " + b.constellation + "/" + b.starName
                        + " must not share the same world yaw");
            }
        }
    }
}
