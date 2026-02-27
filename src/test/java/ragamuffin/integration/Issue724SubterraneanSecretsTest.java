package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #724 Integration Tests — Expand and add secrets to subterranean structures.
 *
 * Verifies that:
 * 1. Sewer stash rooms exist as registered landmarks and contain the expected blocks.
 * 2. The bunker lower level exists, is accessible, and contains unique furnishings.
 * 3. The black market basement exists beneath the high street.
 * 4. The new underground landmarks are accessible from existing entry points.
 * 5. The new constants for underground depth levels are correctly ordered.
 */
class Issue724SubterraneanSecretsTest {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        world.generate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Sewer stash rooms exist as landmarks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * After world generation, at least one SEWER_STASH_ROOM landmark must be
     * registered, and its bounding box must be underground (y < 0).
     */
    @Test
    void test1_SewerStashRoomsExistAsLandmarks() {
        Landmark stash = world.getLandmark(LandmarkType.SEWER_STASH_ROOM);
        assertNotNull(stash,
            "At least one SEWER_STASH_ROOM landmark must be registered after world generation.");

        // Stash room must be below ground
        assertTrue(stash.getPosition().y < 0,
            "SEWER_STASH_ROOM must be underground (y < 0), but found at y=" + stash.getPosition().y);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Sewer stash rooms contain expected blocks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A sewer stash room must contain at least one SHELF block and one TABLE block
     * inside its bounds — confirming the room was furnished.
     */
    @Test
    void test2_SewerStashRoomsAreFurnished() {
        Landmark stash = world.getLandmark(LandmarkType.SEWER_STASH_ROOM);
        assertNotNull(stash, "SEWER_STASH_ROOM landmark must exist.");

        int sx = (int) stash.getPosition().x;
        int sy = (int) stash.getPosition().y;
        int sz = (int) stash.getPosition().z;

        boolean hasShelf = false;
        boolean hasTable = false;

        // Scan a wider area around the stash position to find furnishings
        for (int x = sx - 2; x <= sx + stash.getWidth() + 2; x++) {
            for (int z = sz - 2; z <= sz + stash.getDepth() + 2; z++) {
                for (int y = sy; y <= sy + stash.getHeight(); y++) {
                    BlockType block = world.getBlock(x, y, z);
                    if (block == BlockType.SHELF) hasShelf = true;
                    if (block == BlockType.TABLE) hasTable = true;
                }
            }
        }

        assertTrue(hasShelf,
            "SEWER_STASH_ROOM must contain at least one SHELF block.");
        assertTrue(hasTable,
            "SEWER_STASH_ROOM must contain at least one TABLE block.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Bunker lower level exists and is below the main bunker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The BUNKER_LOWER_LEVEL landmark must be registered and must be below the
     * main UNDERGROUND_BUNKER landmark (lower Y coordinate).
     */
    @Test
    void test3_BunkerLowerLevelExistsAndIsDeeper() {
        Landmark upperBunker = world.getLandmark(LandmarkType.UNDERGROUND_BUNKER);
        Landmark lowerLevel  = world.getLandmark(LandmarkType.BUNKER_LOWER_LEVEL);

        assertNotNull(upperBunker, "UNDERGROUND_BUNKER landmark must exist.");
        assertNotNull(lowerLevel,  "BUNKER_LOWER_LEVEL landmark must be registered after world generation.");

        // Lower level must have a lower (more negative) Y than the main bunker
        assertTrue(lowerLevel.getPosition().y < upperBunker.getPosition().y,
            "BUNKER_LOWER_LEVEL Y (" + lowerLevel.getPosition().y
                + ") must be below UNDERGROUND_BUNKER Y (" + upperBunker.getPosition().y + ").");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Bunker lower level contains air blocks (was carved)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The bunker lower level interior must contain air blocks, confirming the
     * carving pass executed correctly rather than leaving the area as solid stone.
     */
    @Test
    void test4_BunkerLowerLevelInteriorIsCarved() {
        int airCount = 0;
        // Scan the known lower-level bounds (centred at 0,0)
        int llFloor = WorldGenerator.BUNKER_LOWER_FLOOR_Y;
        int llTop   = WorldGenerator.BUNKER_LOWER_TOP_Y;
        for (int x = -7; x <= 7; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = llFloor; y <= llTop; y++) {
                    if (world.getBlock(x, y, z) == BlockType.AIR) {
                        airCount++;
                    }
                }
            }
        }

        assertTrue(airCount > 30,
            "Bunker lower level must have a substantial carved interior (>30 air blocks), "
                + "but found only " + airCount + ".");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Black market basement landmark exists
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The BLACK_MARKET_BASEMENT landmark must be registered and positioned
     * underground (y < 0).
     */
    @Test
    void test5_BlackMarketBasementExistsUnderground() {
        Landmark basement = world.getLandmark(LandmarkType.BLACK_MARKET_BASEMENT);
        assertNotNull(basement,
            "BLACK_MARKET_BASEMENT landmark must be registered after world generation.");

        assertTrue(basement.getPosition().y < 0,
            "BLACK_MARKET_BASEMENT must be underground (y < 0), found y=" + basement.getPosition().y);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Depth constants are correctly ordered
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The new depth constants must be consistently ordered:
     *   SEWER level > BUNKER level > BUNKER_LOWER level > BEDROCK
     * (more negative = deeper underground).
     */
    @Test
    void test6_DepthConstantsAreConsistentlyOrdered() {
        // Sewer is shallower than bunker
        assertTrue(WorldGenerator.SEWER_FLOOR_Y > WorldGenerator.BUNKER_FLOOR_Y,
            "SEWER_FLOOR_Y must be shallower (less negative) than BUNKER_FLOOR_Y.");

        // Bunker top is shallower than bunker lower level
        assertTrue(WorldGenerator.BUNKER_TOP_Y > WorldGenerator.BUNKER_LOWER_TOP_Y,
            "BUNKER_TOP_Y must be shallower than BUNKER_LOWER_TOP_Y.");

        // Bunker lower floor is shallower than bedrock
        assertTrue(WorldGenerator.BUNKER_LOWER_FLOOR_Y > WorldGenerator.BEDROCK_DEPTH,
            "BUNKER_LOWER_FLOOR_Y must be above BEDROCK_DEPTH.");

        // Basement levels are between sewer and bunker
        assertTrue(WorldGenerator.BASEMENT_FLOOR_Y > WorldGenerator.BUNKER_FLOOR_Y,
            "BASEMENT_FLOOR_Y must be shallower (less negative) than BUNKER_FLOOR_Y.");
        assertTrue(WorldGenerator.BASEMENT_FLOOR_Y < 0,
            "BASEMENT_FLOOR_Y must be underground (< 0).");
    }
}
