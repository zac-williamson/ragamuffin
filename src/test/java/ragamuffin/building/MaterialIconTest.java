package ragamuffin.building;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Material icon classification used by the inventory UI.
 * Issue #139: non-block items must have a non-block icon shape distinct from block squares.
 */
class MaterialIconTest {

    // --- isBlockItem() ---

    @Test
    void blockMaterials_areClassifiedAsBlockItems() {
        assertTrue(Material.WOOD.isBlockItem());
        assertTrue(Material.BRICK.isBlockItem());
        assertTrue(Material.STONE.isBlockItem());
        assertTrue(Material.GLASS.isBlockItem());
        assertTrue(Material.GRASS_TURF.isBlockItem());
        assertTrue(Material.DIRT.isBlockItem());
        assertTrue(Material.CONCRETE.isBlockItem());
        assertTrue(Material.PLANKS.isBlockItem());
        assertTrue(Material.PAVEMENT_SLAB.isBlockItem());
        assertTrue(Material.ROAD_ASPHALT.isBlockItem());
    }

    @Test
    void tools_areNotBlockItems() {
        assertFalse(Material.IMPROVISED_TOOL.isBlockItem());
        assertFalse(Material.STONE_TOOL.isBlockItem());
    }

    @Test
    void foodItems_areNotBlockItems() {
        assertFalse(Material.SAUSAGE_ROLL.isBlockItem());
        assertFalse(Material.STEAK_BAKE.isBlockItem());
        assertFalse(Material.CHIPS.isBlockItem());
        assertFalse(Material.KEBAB.isBlockItem());
        assertFalse(Material.ENERGY_DRINK.isBlockItem());
        assertFalse(Material.CRISPS.isBlockItem());
        assertFalse(Material.TIN_OF_BEANS.isBlockItem());
        assertFalse(Material.PINT.isBlockItem());
        assertFalse(Material.PERI_PERI_CHICKEN.isBlockItem());
    }

    @Test
    void shopGoods_areNotBlockItems() {
        assertFalse(Material.NEWSPAPER.isBlockItem());
        assertFalse(Material.SCRATCH_CARD.isBlockItem());
        assertFalse(Material.WASHING_POWDER.isBlockItem());
        assertFalse(Material.PARACETAMOL.isBlockItem());
        assertFalse(Material.TEXTBOOK.isBlockItem());
        assertFalse(Material.HYMN_BOOK.isBlockItem());
        assertFalse(Material.PETROL_CAN.isBlockItem());
        assertFalse(Material.HAIR_CLIPPERS.isBlockItem());
        assertFalse(Material.NAIL_POLISH.isBlockItem());
        assertFalse(Material.BROKEN_PHONE.isBlockItem());
        assertFalse(Material.DODGY_DVD.isBlockItem());
        assertFalse(Material.FIRE_EXTINGUISHER.isBlockItem());
        assertFalse(Material.ANTIDEPRESSANTS.isBlockItem());
    }

    @Test
    void officeItems_areNotBlockItems() {
        assertFalse(Material.COMPUTER.isBlockItem());
        assertFalse(Material.OFFICE_CHAIR.isBlockItem());
        assertFalse(Material.STAPLER.isBlockItem());
    }

    @Test
    void diamond_isNotABlockItem() {
        assertFalse(Material.DIAMOND.isBlockItem());
    }

    // --- getIconShape() ---

    @Test
    void tools_haveToolShape() {
        assertEquals(Material.IconShape.TOOL, Material.IMPROVISED_TOOL.getIconShape());
        assertEquals(Material.IconShape.TOOL, Material.STONE_TOOL.getIconShape());
    }

    @Test
    void newspapers_and_books_haveFlatPaperShape() {
        assertEquals(Material.IconShape.FLAT_PAPER, Material.NEWSPAPER.getIconShape());
        assertEquals(Material.IconShape.FLAT_PAPER, Material.TEXTBOOK.getIconShape());
        assertEquals(Material.IconShape.FLAT_PAPER, Material.HYMN_BOOK.getIconShape());
        assertEquals(Material.IconShape.FLAT_PAPER, Material.PARACETAMOL.getIconShape());
        assertEquals(Material.IconShape.FLAT_PAPER, Material.WASHING_POWDER.getIconShape());
        assertEquals(Material.IconShape.FLAT_PAPER, Material.ANTIDEPRESSANTS.getIconShape());
    }

    @Test
    void drinks_haveBottleShape() {
        assertEquals(Material.IconShape.BOTTLE, Material.ENERGY_DRINK.getIconShape());
        assertEquals(Material.IconShape.BOTTLE, Material.PINT.getIconShape());
        assertEquals(Material.IconShape.BOTTLE, Material.NAIL_POLISH.getIconShape());
    }

    @Test
    void food_hasFoodShape() {
        assertEquals(Material.IconShape.FOOD, Material.SAUSAGE_ROLL.getIconShape());
        assertEquals(Material.IconShape.FOOD, Material.CHIPS.getIconShape());
        assertEquals(Material.IconShape.FOOD, Material.KEBAB.getIconShape());
        assertEquals(Material.IconShape.FOOD, Material.CRISPS.getIconShape());
    }

    @Test
    void cards_and_phones_haveCardShape() {
        assertEquals(Material.IconShape.CARD, Material.SCRATCH_CARD.getIconShape());
        assertEquals(Material.IconShape.CARD, Material.BROKEN_PHONE.getIconShape());
        assertEquals(Material.IconShape.CARD, Material.DODGY_DVD.getIconShape());
    }

    @Test
    void diamond_hasGemShape() {
        assertEquals(Material.IconShape.GEM, Material.DIAMOND.getIconShape());
    }

    @Test
    void cylinders_haveCylinderShape() {
        assertEquals(Material.IconShape.CYLINDER, Material.PETROL_CAN.getIconShape());
        assertEquals(Material.IconShape.CYLINDER, Material.FIRE_EXTINGUISHER.getIconShape());
        assertEquals(Material.IconShape.CYLINDER, Material.HAIR_CLIPPERS.getIconShape());
    }

    // --- Icon colors are still defined ---

    @Test
    void allMaterials_haveAtLeastOneIconColor() {
        for (Material m : Material.values()) {
            assertNotNull(m.getIconColors(), "getIconColors() must not return null for " + m);
            assertTrue(m.getIconColors().length >= 1,
                "getIconColors() must return at least one color for " + m);
        }
    }

    @Test
    void allNonBlockMaterials_haveAnIconShape() {
        for (Material m : Material.values()) {
            if (!m.isBlockItem()) {
                assertNotNull(m.getIconShape(),
                    "getIconShape() must not return null for non-block item " + m);
            }
        }
    }

    @Test
    void distinctNonBlockShapes_areUsed() {
        // Verify the system actually produces variety — at least 5 distinct shapes are used
        long distinctShapes = java.util.Arrays.stream(Material.values())
            .filter(m -> !m.isBlockItem())
            .map(Material::getIconShape)
            .distinct()
            .count();
        assertTrue(distinctShapes >= 5,
            "Expected at least 5 distinct icon shapes for non-block items, got " + distinctShapes);
    }
}
