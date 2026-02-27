package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.BlockType;
import ragamuffin.world.Landmark;
import ragamuffin.world.LandmarkType;
import ragamuffin.world.World;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #728 Integration Tests — Shop doors must use DOOR_LOWER/DOOR_UPPER, not GARDEN_WALL.
 *
 * When {@code fillGapsBetweenBuildings} runs after shop generation it replaces
 * any AIR block at y=1 with a GARDEN_WALL block.  Previously {@code buildShop}
 * cleared the door position to AIR, which caused the gap-fill pass to block
 * the entrance with a GARDEN_WALL.
 *
 * The fix places {@code DOOR_LOWER} at y=1 and {@code DOOR_UPPER} at y=2 in
 * the door opening so the gap-fill pass leaves them untouched.
 */
class Issue728ShopDoorBlockTest {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        world.generate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: No shop landmark entrance is blocked by a GARDEN_WALL at y=1
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * For every shop-type landmark generated, the centre-front column
     * (width/2, z=front face) must NOT contain a GARDEN_WALL block at y=1.
     * A GARDEN_WALL there means the door was placed as AIR and then overwritten
     * by {@code fillGapsBetweenBuildings}.
     */
    @Test
    void test1_ShopEntranceIsNotBlockedByGardenWall() {
        Collection<Landmark> landmarks = world.getAllLandmarks();
        assertFalse(landmarks.isEmpty(), "Expected at least one landmark in the generated world.");

        int shopsChecked = 0;
        for (Landmark lm : landmarks) {
            LandmarkType type = lm.getType();
            if (!isShopType(type)) continue;
            shopsChecked++;

            int lx = (int) lm.getPosition().x;
            int lz = (int) lm.getPosition().z;
            int doorX = lx + lm.getWidth() / 2;
            int doorZ = lz; // front face z
            BlockType atY1 = world.getBlock(doorX, 1, doorZ);

            assertNotEquals(BlockType.GARDEN_WALL, atY1,
                "Shop " + type + " at (" + lx + "," + lz + "): "
                    + "door position (" + doorX + ",1," + doorZ + ") must NOT be GARDEN_WALL. "
                    + "Found: " + atY1);
        }

        assertTrue(shopsChecked >= 1,
            "Expected at least 1 shop-type landmark to be checked, but found none.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Shop entrances have DOOR_LOWER at y=1 and DOOR_UPPER at y=2
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The centre-front column of each shop landmark must have DOOR_LOWER at y=1
     * and DOOR_UPPER at y=2, confirming that proper door blocks (not AIR) are placed.
     */
    @Test
    void test2_ShopEntranceHasProperDoorBlocks() {
        Collection<Landmark> landmarks = world.getAllLandmarks();
        assertFalse(landmarks.isEmpty(), "Expected at least one landmark in the generated world.");

        int shopsChecked = 0;
        for (Landmark lm : landmarks) {
            LandmarkType type = lm.getType();
            if (!isShopType(type)) continue;
            shopsChecked++;

            int lx = (int) lm.getPosition().x;
            int lz = (int) lm.getPosition().z;
            int doorX = lx + lm.getWidth() / 2;
            int doorZ = lz;
            BlockType atY1 = world.getBlock(doorX, 1, doorZ);
            BlockType atY2 = world.getBlock(doorX, 2, doorZ);

            assertEquals(BlockType.DOOR_LOWER, atY1,
                "Shop " + type + " at (" + lx + "," + lz + "): "
                    + "expected DOOR_LOWER at (" + doorX + ",1," + doorZ + "), found: " + atY1);
            assertEquals(BlockType.DOOR_UPPER, atY2,
                "Shop " + type + " at (" + lx + "," + lz + "): "
                    + "expected DOOR_UPPER at (" + doorX + ",2," + doorZ + "), found: " + atY2);
        }

        assertTrue(shopsChecked >= 1,
            "Expected at least 1 shop-type landmark to be checked, but found none.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isShopType(LandmarkType type) {
        switch (type) {
            case GREGGS:
            case OFF_LICENCE:
            case CHARITY_SHOP:
            case JEWELLER:
            case BETTING_SHOP:
            case CORNER_SHOP:
            case NEWSAGENT:
            case CHIPPY:
            case NAIL_SALON:
            case BARBER:
            case NANDOS:
            case PHONE_REPAIR:
            case CASH_CONVERTER:
                return true;
            default:
                return false;
        }
    }
}
