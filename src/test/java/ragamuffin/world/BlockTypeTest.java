package ragamuffin.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockTypeTest {

    @Test
    void allBlockTypesExist() {
        assertNotNull(BlockType.AIR);
        assertNotNull(BlockType.GRASS);
        assertNotNull(BlockType.DIRT);
        assertNotNull(BlockType.STONE);
        assertNotNull(BlockType.PAVEMENT);
        assertNotNull(BlockType.ROAD);
        assertNotNull(BlockType.BRICK);
        assertNotNull(BlockType.GLASS);
        assertNotNull(BlockType.WOOD);
        assertNotNull(BlockType.WATER);
        assertNotNull(BlockType.TREE_TRUNK);
        assertNotNull(BlockType.LEAVES);
    }

    @Test
    void airIsNotSolid() {
        assertFalse(BlockType.AIR.isSolid());
    }

    @Test
    void grassIsSolid() {
        assertTrue(BlockType.GRASS.isSolid());
    }

    @Test
    void waterIsNotSolid() {
        assertFalse(BlockType.WATER.isSolid());
    }

    @Test
    void stoneIsSolid() {
        assertTrue(BlockType.STONE.isSolid());
    }

    @Test
    void blockTypesHaveUniqueIds() {
        BlockType[] types = BlockType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getId(), types[j].getId(),
                    "BlockType " + types[i] + " and " + types[j] + " have the same ID");
            }
        }
    }
}
