package ragamuffin.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.render.SignageRenderer;
import ragamuffin.test.HeadlessTestHelper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #10: Building signage.
 *
 * Verifies that:
 * - Key store landmark types have display names set
 * - Non-commercial landmarks (park, houses, etc.) have no display name
 * - The SignageRenderer builds sign objects from landmark data
 * - Sign positions are above the front face of the building
 */
public class BuildingSignageTest {

    @BeforeEach
    public void setUp() {
        HeadlessTestHelper.initHeadless();
    }

    // ---- LandmarkType.getDisplayName() tests -----------------------------------

    @Test
    public void testGreggHasDisplayName() {
        assertNotNull(LandmarkType.GREGGS.getDisplayName(),
            "Greggs should have a display name for its sign");
        assertFalse(LandmarkType.GREGGS.getDisplayName().isBlank(),
            "Greggs display name should not be blank");
    }

    @Test
    public void testJewellerHasAndresDiamondsName() {
        // Issue #10 specifically mentions "Andre's Diamonds" as an example
        String name = LandmarkType.JEWELLER.getDisplayName();
        assertNotNull(name, "Jeweller should have a display name");
        assertTrue(name.toLowerCase().contains("diamond") || name.toLowerCase().contains("andre"),
            "Jeweller sign should reference diamonds or 'Andre', got: " + name);
    }

    @Test
    public void testAllCommercialLandmarksHaveDisplayNames() {
        LandmarkType[] commercialTypes = {
            LandmarkType.GREGGS,
            LandmarkType.OFF_LICENCE,
            LandmarkType.CHARITY_SHOP,
            LandmarkType.JEWELLER,
            LandmarkType.BOOKIES,
            LandmarkType.KEBAB_SHOP,
            LandmarkType.LAUNDERETTE,
            LandmarkType.TESCO_EXPRESS,
            LandmarkType.PUB,
            LandmarkType.PAWN_SHOP,
            LandmarkType.BUILDERS_MERCHANT,
            LandmarkType.CHIPPY,
            LandmarkType.NEWSAGENT,
            LandmarkType.GP_SURGERY,
            LandmarkType.JOB_CENTRE,
            LandmarkType.OFFICE_BUILDING,
            LandmarkType.NANDOS,
            LandmarkType.BARBER,
            LandmarkType.NAIL_SALON,
            LandmarkType.WETHERSPOONS,
            LandmarkType.CORNER_SHOP,
            LandmarkType.BETTING_SHOP,
            LandmarkType.PHONE_REPAIR,
            LandmarkType.CASH_CONVERTER,
            LandmarkType.LIBRARY,
            LandmarkType.FIRE_STATION,
        };

        for (LandmarkType type : commercialTypes) {
            String name = type.getDisplayName();
            assertNotNull(name,
                type + " should have a display name for its sign");
            assertFalse(name.isBlank(),
                type + " display name should not be blank");
        }
    }

    @Test
    public void testNonCommercialLandmarksHaveNoDisplayName() {
        // These are areas / residential buildings that don't need named signs
        assertNull(LandmarkType.PARK.getDisplayName(),
            "Park should not have a named sign");
        assertNull(LandmarkType.TERRACED_HOUSE.getDisplayName(),
            "Terraced houses should not have named signs");
        assertNull(LandmarkType.COUNCIL_FLATS.getDisplayName(),
            "Council flats should not have named signs");
        assertNull(LandmarkType.CANAL.getDisplayName(),
            "Canal should not have a named sign");
        assertNull(LandmarkType.ALLOTMENTS.getDisplayName(),
            "Allotments should not have a named sign");
        assertNull(LandmarkType.CEMETERY.getDisplayName(),
            "Cemetery should not have a named sign");
        assertNull(LandmarkType.SKATE_PARK.getDisplayName(),
            "Skate park should not have a named sign");
    }

    @Test
    public void testDisplayNamesAreUnique() {
        // All commercial landmarks should have distinct sign text (no copy-paste errors)
        List<String> seen = new ArrayList<>();
        for (LandmarkType type : LandmarkType.values()) {
            String name = type.getDisplayName();
            if (name == null) continue;
            assertFalse(seen.contains(name),
                "Duplicate sign name '" + name + "' on " + type);
            seen.add(name);
        }
    }

    // ---- SignageRenderer tests -------------------------------------------------

    @Test
    public void testSignageRendererBuildsSignsFromLandmarks() {
        List<Landmark> landmarks = new ArrayList<>();
        landmarks.add(new Landmark(LandmarkType.GREGGS,     20, 0, 25, 7, 4, 8));
        landmarks.add(new Landmark(LandmarkType.JEWELLER,   43, 0, 25, 6, 4, 8));
        landmarks.add(new Landmark(LandmarkType.PARK,      -15, 0,-15,30, 1, 30)); // No sign
        landmarks.add(new Landmark(LandmarkType.COUNCIL_FLATS, -95, 0, 50, 12, 18, 12)); // No sign

        SignageRenderer renderer = new SignageRenderer();
        renderer.buildFromLandmarks(landmarks);

        List<ragamuffin.world.BuildingSign> signs = renderer.getSigns();

        // Park and Council Flats have no display name, so only 2 signs expected
        assertEquals(2, signs.size(),
            "Should create signs only for landmarks with display names");
    }

    @Test
    public void testSignPositionedAboveBuildingFrontFace() {
        // Building at x=20, z=25, width=7, height=4, depth=8
        List<Landmark> landmarks = new ArrayList<>();
        landmarks.add(new Landmark(LandmarkType.GREGGS, 20, 0, 25, 7, 4, 8));

        SignageRenderer renderer = new SignageRenderer();
        renderer.buildFromLandmarks(landmarks);

        List<ragamuffin.world.BuildingSign> signs = renderer.getSigns();
        assertEquals(1, signs.size(), "Should produce exactly one sign for Greggs");

        ragamuffin.world.BuildingSign sign = signs.get(0);

        // Sign X should be roughly centred on the building (x + width/2 = 20 + 3.5 = 23.5)
        assertEquals(23.5f, sign.getWorldX(), 0.5f,
            "Sign should be horizontally centred over the entrance");

        // Sign Y should be above the building height (height=4, so Y >= 4)
        assertTrue(sign.getWorldY() >= 4.0f,
            "Sign Y should be at or above the building top, got " + sign.getWorldY());

        // Sign Z should be at the front face of the building (z=25)
        assertEquals(25f, sign.getWorldZ(), 1.0f,
            "Sign should be at the front face of the building");
    }

    @Test
    public void testSignTextMatchesLandmarkDisplayName() {
        List<Landmark> landmarks = new ArrayList<>();
        landmarks.add(new Landmark(LandmarkType.JEWELLER, 43, 0, 25, 6, 4, 8));

        SignageRenderer renderer = new SignageRenderer();
        renderer.buildFromLandmarks(landmarks);

        List<ragamuffin.world.BuildingSign> signs = renderer.getSigns();
        assertEquals(1, signs.size());

        String expectedName = LandmarkType.JEWELLER.getDisplayName();
        assertEquals(expectedName, signs.get(0).getText(),
            "Sign text should match the landmark's display name");
    }

    @Test
    public void testSignsArePresentInGeneratedWorld() {
        // Integration: generate the world and verify signs are created for key buildings
        World world = new World(12345L);
        WorldGenerator generator = new WorldGenerator(12345L);
        generator.generateWorld(world);

        SignageRenderer renderer = new SignageRenderer();
        renderer.buildFromLandmarks(world.getAllLandmarks());

        List<ragamuffin.world.BuildingSign> signs = renderer.getSigns();

        // Should have a meaningful number of signs (at least the high-street shops)
        assertTrue(signs.size() >= 10,
            "World should produce at least 10 building signs, got " + signs.size());

        // Verify the Jeweller sign specifically (mentioned in the issue)
        boolean foundJeweller = signs.stream()
            .anyMatch(s -> s.getText().equals(LandmarkType.JEWELLER.getDisplayName()));
        assertTrue(foundJeweller, "Should find the jeweller sign in the generated world");

        // Verify Greggs sign
        boolean foundGreggs = signs.stream()
            .anyMatch(s -> s.getText().equals(LandmarkType.GREGGS.getDisplayName()));
        assertTrue(foundGreggs, "Should find the Greggs sign in the generated world");
    }
}
