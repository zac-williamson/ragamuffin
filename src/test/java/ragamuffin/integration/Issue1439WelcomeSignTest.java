package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.test.HeadlessTestHelper;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;
import ragamuffin.world.WorldGenerator;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #1439 — Welcome sign as 3D physical object in town.
 *
 * Verifies:
 * - A WELCOME_SIGN prop is placed in the world after generation
 * - The sign is positioned near the main park entrance (south of park, near x=0, z=-18)
 * - A WELCOME_SIGN landmark exists at the correct location
 * - The WELCOME_SIGN PropType has appropriate dimensions for a large sign
 */
class Issue1439WelcomeSignTest {

    private World world;
    private WorldGenerator generator;

    @BeforeEach
    void setUp() {
        HeadlessTestHelper.initHeadless();
        world = new World(12345);
        generator = new WorldGenerator(12345);
        generator.generateWorld(world);
    }

    @Test
    void welcomeSignPropIsPlacedInWorld() {
        List<PropPosition> allProps = world.getPropPositions();
        List<PropPosition> welcomeSigns = allProps.stream()
                .filter(p -> p.getType() == PropType.WELCOME_SIGN)
                .collect(Collectors.toList());

        assertFalse(welcomeSigns.isEmpty(),
                "World should contain at least one WELCOME_SIGN prop after generation");
    }

    @Test
    void welcomeSignIsNearParkEntrance() {
        // The park south fence is at z=-15; the main entrance gap is at x=0/-1.
        // The sign should be placed just south of the entrance, around z=-18.
        List<PropPosition> welcomeSigns = world.getPropPositions().stream()
                .filter(p -> p.getType() == PropType.WELCOME_SIGN)
                .collect(Collectors.toList());

        assertFalse(welcomeSigns.isEmpty(), "WELCOME_SIGN prop must exist");

        PropPosition sign = welcomeSigns.get(0);

        // Sign should be near the park entrance: within 10 blocks of x=0
        assertTrue(Math.abs(sign.getWorldX()) <= 10f,
                "Welcome sign X should be near the park entrance (x≈0), got: " + sign.getWorldX());

        // Sign should be just south of the park (z in range -22 to -14)
        assertTrue(sign.getWorldZ() >= -22f && sign.getWorldZ() <= -14f,
                "Welcome sign Z should be just south of the park entrance (z≈-18), got: " + sign.getWorldZ());
    }

    @Test
    void welcomeSignLandmarkExists() {
        Landmark landmark = world.getLandmark(LandmarkType.WELCOME_SIGN);

        assertNotNull(landmark,
                "A WELCOME_SIGN landmark should be registered in the world after generation");
    }

    @Test
    void welcomeSignLandmarkIsNearParkEntrance() {
        Landmark landmark = world.getLandmark(LandmarkType.WELCOME_SIGN);
        assertNotNull(landmark, "WELCOME_SIGN landmark must exist");

        Vector3 pos = landmark.getPosition();

        // Landmark should be near the park south entrance
        assertTrue(Math.abs(pos.x) <= 10f,
                "Welcome sign landmark X should be near x=0, got: " + pos.x);
        assertTrue(pos.z >= -22f && pos.z <= -14f,
                "Welcome sign landmark Z should be south of park entrance, got: " + pos.z);
    }

    @Test
    void welcomeSignPropTypeHasReasonableDimensions() {
        // The sign should be wide enough to be a landmark (at least 2m wide)
        assertTrue(PropType.WELCOME_SIGN.getCollisionWidth() >= 2.0f,
                "Welcome sign should be at least 2 m wide, got: " + PropType.WELCOME_SIGN.getCollisionWidth());

        // And tall enough to be visible (at least 1m tall)
        assertTrue(PropType.WELCOME_SIGN.getCollisionHeight() >= 1.0f,
                "Welcome sign should be at least 1 m tall, got: " + PropType.WELCOME_SIGN.getCollisionHeight());
    }

    @Test
    void welcomeSignHasDisplayName() {
        String name = LandmarkType.WELCOME_SIGN.getDisplayName();
        assertNotNull(name, "WELCOME_SIGN landmark should have a display name");
        assertTrue(name.toLowerCase().contains("northfield") || name.toLowerCase().contains("welcome"),
                "Display name should reference Northfield or Welcome, got: " + name);
    }
}
