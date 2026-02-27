package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Inventory;
import ragamuffin.building.Material;
import ragamuffin.building.PropBreaker;
import ragamuffin.entity.AABB;
import ragamuffin.entity.Player;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #719 Integration Tests — Prop Collision and Destructibility
 *
 * Tests that non-block 3D props:
 * 1. Block player movement via AABB collision
 * 2. Can be punched and broken into materials
 * 3. Disappear from the world when broken
 * 4. Partially-hit props do not yield materials
 * 5. PropType AABB dimensions are present for every prop type
 */
class Issue719PropCollisionDestructibilityTest {

    private World world;
    private Player player;
    private PropBreaker propBreaker;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        world      = new World(99L);
        player     = new Player(0, 1, 0);
        propBreaker = new PropBreaker();
        inventory  = new Inventory(36);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Prop blocks player movement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a BOLLARD directly in front of the player and verify the player
     * cannot walk through it.
     */
    @Test
    void test1_PropBlocksPlayerMovement() {
        // Player at (0, 1, 0), bollard at (0, 1, 2) — 2 blocks in front on Z+
        world.addPropPosition(new PropPosition(0f, 1f, 2f, PropType.BOLLARD, 0f));

        // Record starting Z
        float startZ = player.getPosition().z;

        // Simulate walking forward (positive Z) for 60 frames at ~1/60 s each
        for (int frame = 0; frame < 60; frame++) {
            world.moveWithCollision(player, 0, 0, 1, 1f / 60f);
        }

        // Player should NOT have passed through the bollard.
        // Bollard centre is at Z=2, half-depth = 0.14 → front face at Z=1.86
        // Player half-depth = 0.3, so player front can reach Z = propMinZ - playerHalfDepth
        float bollardMinZ = 2f - PropType.BOLLARD.getCollisionDepth() / 2f;
        assertTrue(player.getPosition().z < bollardMinZ + Player.DEPTH,
                "Player should be blocked by bollard; actual Z=" + player.getPosition().z
                        + " but bollard starts at Z=" + bollardMinZ);

        // Bollard should still be in the world
        assertEquals(1, world.getPropPositions().size(), "Bollard should still be present");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Punching a prop the required number of times destroys it
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Punch a PARK_BENCH (3 hits to break) exactly 3 times and verify it is
     * removed from the world and drops WOOD.
     */
    @Test
    void test2_PunchingPropToDestructionDropsMaterial() {
        world.addPropPosition(new PropPosition(1f, 1f, 1f, PropType.PARK_BENCH, 0f));
        assertEquals(1, world.getPropPositions().size());

        int hitsNeeded = PropType.PARK_BENCH.getHitsToBreak(); // 3
        Material lastDrop = null;
        for (int i = 0; i < hitsNeeded; i++) {
            lastDrop = propBreaker.punchProp(world, 0);
            if (i < hitsNeeded - 1) {
                assertNull(lastDrop, "Should not drop material before final hit (hit " + (i + 1) + ")");
            }
        }

        // Prop should now be removed
        assertEquals(0, world.getPropPositions().size(), "Prop should be removed after being broken");

        // Final punch should have returned WOOD
        assertEquals(Material.WOOD, lastDrop, "PARK_BENCH should drop WOOD");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Partial hits do not break or remove the prop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Punch a POST_BOX (5 hits to break) only 4 times; verify it is still
     * present and no material has been dropped.
     */
    @Test
    void test3_PartialPunchesDoNotBreakProp() {
        world.addPropPosition(new PropPosition(3f, 1f, 3f, PropType.POST_BOX, 0f));

        int hitsShy = PropType.POST_BOX.getHitsToBreak() - 1; // 4
        for (int i = 0; i < hitsShy; i++) {
            Material drop = propBreaker.punchProp(world, 0);
            assertNull(drop, "Should not drop material on hit " + (i + 1));
        }

        // Prop must still be present
        assertEquals(1, world.getPropPositions().size(), "Prop should still be present after partial damage");
        assertEquals(PropType.POST_BOX, world.getPropPositions().get(0).getType());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: PropPosition.getAABB() returns correct bounds
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verify that the AABB returned by a PropPosition is correctly sized and
     * positioned relative to the prop's world coordinates.
     */
    @Test
    void test4_PropAABBBoundsAreCorrect() {
        float px = 5f, py = 0f, pz = 8f;
        PropPosition pos = new PropPosition(px, py, pz, PropType.PHONE_BOX, 0f);
        AABB box = pos.getAABB();

        float halfW = PropType.PHONE_BOX.getCollisionWidth()  / 2f;
        float halfD = PropType.PHONE_BOX.getCollisionDepth()  / 2f;
        float h     = PropType.PHONE_BOX.getCollisionHeight();

        assertEquals(px - halfW, box.getMinX(), 1e-4f, "AABB minX");
        assertEquals(px + halfW, box.getMaxX(), 1e-4f, "AABB maxX");
        assertEquals(py,         box.getMinY(), 1e-4f, "AABB minY (base)");
        assertEquals(py + h,     box.getMaxY(), 1e-4f, "AABB maxY");
        assertEquals(pz - halfD, box.getMinZ(), 1e-4f, "AABB minZ");
        assertEquals(pz + halfD, box.getMaxZ(), 1e-4f, "AABB maxZ");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Every PropType has positive collision dimensions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ensure no PropType has a zero or negative collision dimension (which would
     * silently disable collision or produce broken AABBs).
     */
    @Test
    void test5_AllPropTypesHavePositiveCollisionDimensions() {
        for (PropType type : PropType.values()) {
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
    // Test 6: After breaking a prop, the gap is passable
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a BOLLARD in front of the player, break it completely, then verify
     * the player can now walk through its former position.
     */
    @Test
    void test6_AfterBreakingPropPlayerCanPassThrough() {
        world.addPropPosition(new PropPosition(0f, 1f, 2f, PropType.BOLLARD, 0f));

        // Break the bollard completely
        int hits = PropType.BOLLARD.getHitsToBreak();
        for (int i = 0; i < hits; i++) {
            propBreaker.punchProp(world, 0);
        }

        assertEquals(0, world.getPropPositions().size(), "Bollard should be removed");

        // Reset player position
        player.teleport(0, 1, 0);
        float startZ = player.getPosition().z;

        // Walk forward for 60 frames
        for (int frame = 0; frame < 60; frame++) {
            world.moveWithCollision(player, 0, 0, 1, 1f / 60f);
        }

        // Player should have moved forward past where the bollard was
        assertTrue(player.getPosition().z > 2f,
                "Player should be able to walk through where the bollard was; actual Z="
                        + player.getPosition().z);
    }
}
