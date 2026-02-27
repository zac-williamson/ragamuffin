package ragamuffin.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #721 Integration Tests — Small 3D objects on shop shelves.
 *
 * Verifies that:
 * 1. After world generation, shelf-item props (SHELF_CAN, SHELF_BOTTLE, SHELF_BOX)
 *    are present in the world.
 * 2. Shelf-item props are positioned at appropriate heights above shelf blocks.
 * 3. All three shelf item types have valid (positive) collision dimensions.
 * 4. At least a minimum number of shelf items appear on shelves to improve
 *    visual detail.
 */
class Issue721ShopShelfObjectsTest {

    private static final PropType[] SHELF_ITEM_TYPES = {
        PropType.SHELF_CAN, PropType.SHELF_BOTTLE, PropType.SHELF_BOX
    };

    private World world;

    @BeforeEach
    void setUp() {
        world = new World(42L);
        world.generate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Shelf item props exist in the generated world
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * After generating the world, at least one SHELF_CAN, SHELF_BOTTLE, or
     * SHELF_BOX prop must be present — confirming that shop shelves are
     * populated with small 3D objects.
     */
    @Test
    void test1_ShelfItemPropsExistAfterWorldGeneration() {
        List<PropPosition> shelfItems = getShelfItemProps();
        assertFalse(shelfItems.isEmpty(),
            "Expected at least one shelf item prop (SHELF_CAN / SHELF_BOTTLE / SHELF_BOX) "
                + "to be placed in the world after generation, but found none.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: A meaningful number of shelf items are placed
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Several shops have shelves; each shelf should have items on both
     * shelf levels.  We expect at least 8 shelf-item props across the world
     * (there are 5 shelved shops × 2 shelf levels × at least 2 positions each).
     */
    @Test
    void test2_SufficientShelfItemsArePlaced() {
        List<PropPosition> shelfItems = getShelfItemProps();
        assertTrue(shelfItems.size() >= 8,
            "Expected at least 8 shelf-item props to be placed across all shelved shops, "
                + "but found only " + shelfItems.size() + ".");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Shelf items are placed at shelf-surface heights (y >= 2)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shelf blocks in shops are placed at y=1 and y=2.  Items sitting on top
     * of the shelves must therefore have a Y position of 2.0 or 3.0.  We
     * verify that every shelf-item prop is at y >= 2.0, i.e. not sunk into
     * the floor.
     */
    @Test
    void test3_ShelfItemsAreAtShelfHeight() {
        List<PropPosition> shelfItems = getShelfItemProps();
        assertFalse(shelfItems.isEmpty(), "No shelf items found — cannot verify height.");

        for (PropPosition prop : shelfItems) {
            assertTrue(prop.getWorldY() >= 2.0f,
                "Shelf item " + prop.getType() + " at (" + prop.getWorldX() + ", "
                    + prop.getWorldY() + ", " + prop.getWorldZ()
                    + ") should be at y >= 2.0 (on top of a shelf block).");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: All three shelf item types are represented
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * With multiple shelved shops each using a deterministic RNG, all three
     * item types (can, bottle, box) should appear somewhere in the world.
     */
    @Test
    void test4_AllThreeShelfItemTypesAreRepresented() {
        List<PropPosition> shelfItems = getShelfItemProps();
        assertFalse(shelfItems.isEmpty(), "No shelf items found.");

        boolean hasCan    = shelfItems.stream().anyMatch(p -> p.getType() == PropType.SHELF_CAN);
        boolean hasBottle = shelfItems.stream().anyMatch(p -> p.getType() == PropType.SHELF_BOTTLE);
        boolean hasBox    = shelfItems.stream().anyMatch(p -> p.getType() == PropType.SHELF_BOX);

        assertTrue(hasCan,    "Expected at least one SHELF_CAN in the world.");
        assertTrue(hasBottle, "Expected at least one SHELF_BOTTLE in the world.");
        assertTrue(hasBox,    "Expected at least one SHELF_BOX in the world.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Shelf item PropType collision dimensions are valid
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Every shelf item type must have positive collision dimensions and require
     * at least 1 hit to break, so the prop system handles them correctly.
     */
    @Test
    void test5_ShelfItemPropTypesHaveValidDimensions() {
        for (PropType type : SHELF_ITEM_TYPES) {
            assertTrue(type.getCollisionWidth()  > 0f,
                type.name() + " must have positive collisionWidth");
            assertTrue(type.getCollisionHeight() > 0f,
                type.name() + " must have positive collisionHeight");
            assertTrue(type.getCollisionDepth()  > 0f,
                type.name() + " must have positive collisionDepth");
            assertTrue(type.getHitsToBreak()     >= 1,
                type.name() + " must require at least 1 hit to break");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private List<PropPosition> getShelfItemProps() {
        return world.getPropPositions().stream()
            .filter(p -> p.getType() == PropType.SHELF_CAN
                      || p.getType() == PropType.SHELF_BOTTLE
                      || p.getType() == PropType.SHELF_BOX)
            .collect(Collectors.toList());
    }
}
