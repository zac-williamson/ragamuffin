package ragamuffin.integration;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ragamuffin.building.Material;
import ragamuffin.building.PropBreaker;
import ragamuffin.entity.AABB;
import ragamuffin.world.PropPosition;
import ragamuffin.world.PropType;
import ragamuffin.world.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Issue #752: Non-block 3D objects cannot be targeted or destroyed.
 *
 * <p>Verifies the ray-AABB intersection logic that identifies which prop (if any) the
 * player is aiming at, and confirms that the PropBreaker correctly removes broken props
 * and yields material drops.
 *
 * <p>The ray-AABB slab-method is exercised directly here (mirroring the private
 * {@code rayAABBIntersect} helper in RagamuffinGame) so the tests can run headlessly
 * without a full LibGDX context.
 */
class Issue752PropTargetingTest {

    private World world;
    private PropBreaker propBreaker;

    @BeforeEach
    void setUp() {
        world      = new World(99L);
        propBreaker = new PropBreaker();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ray-AABB helper (mirrors RagamuffinGame.rayAABBIntersect)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the entry distance along the ray, or -1 if no intersection.
     */
    private float rayAABBIntersect(Vector3 origin, Vector3 dir, AABB box) {
        float tmin = 0f;
        float tmax = Float.MAX_VALUE;

        // X slab
        if (Math.abs(dir.x) < 1e-8f) {
            if (origin.x < box.getMinX() || origin.x > box.getMaxX()) return -1f;
        } else {
            float ood = 1f / dir.x;
            float t1 = (box.getMinX() - origin.x) * ood;
            float t2 = (box.getMaxX() - origin.x) * ood;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1f;
        }

        // Y slab
        if (Math.abs(dir.y) < 1e-8f) {
            if (origin.y < box.getMinY() || origin.y > box.getMaxY()) return -1f;
        } else {
            float ood = 1f / dir.y;
            float t1 = (box.getMinY() - origin.y) * ood;
            float t2 = (box.getMaxY() - origin.y) * ood;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1f;
        }

        // Z slab
        if (Math.abs(dir.z) < 1e-8f) {
            if (origin.z < box.getMinZ() || origin.z > box.getMaxZ()) return -1f;
        } else {
            float ood = 1f / dir.z;
            float t1 = (box.getMinZ() - origin.z) * ood;
            float t2 = (box.getMaxZ() - origin.z) * ood;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return -1f;
        }

        return tmin >= 0f ? tmin : (tmax >= 0f ? 0f : -1f);
    }

    /**
     * Finds the nearest prop index whose AABB is hit by the ray, or -1 if none.
     */
    private int findPropInReach(Vector3 origin, Vector3 direction, float reach) {
        List<PropPosition> props = world.getPropPositions();
        int bestIndex = -1;
        float bestDist = reach + 1f;
        for (int i = 0; i < props.size(); i++) {
            PropPosition prop = props.get(i);
            AABB box = prop.getAABB();
            float t = rayAABBIntersect(origin, direction, box);
            if (t >= 0f && t < bestDist) {
                bestDist = t;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Ray aimed directly at a prop AABB returns that prop's index
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a BOLLARD at (0, 0, 5). Fire a ray from (0, 0.5, 0) toward +Z.
     * The ray must intersect the bollard's AABB and return index 0.
     */
    @Test
    void test1_RayHitsPropAABBWhenAimed() {
        world.addPropPosition(new PropPosition(0f, 0f, 5f, PropType.BOLLARD, 0f));

        Vector3 origin    = new Vector3(0f, 0.5f, 0f);
        Vector3 direction = new Vector3(0f, 0f, 1f); // looking toward +Z

        int hit = findPropInReach(origin, direction, 10f);
        assertEquals(0, hit, "Ray aimed at the bollard must return index 0");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Ray aimed beside a prop does NOT hit it
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a BOLLARD at (0, 0, 5). Fire a ray from (5, 0.5, 0) toward +Z —
     * completely to the side. No hit should be detected.
     */
    @Test
    void test2_RayMissesPropReturnsNegativeOne() {
        world.addPropPosition(new PropPosition(0f, 0f, 5f, PropType.BOLLARD, 0f));

        Vector3 origin    = new Vector3(5f, 0.5f, 0f); // 5 units to the right
        Vector3 direction = new Vector3(0f, 0f, 1f);

        int hit = findPropInReach(origin, direction, 10f);
        assertEquals(-1, hit, "Ray beside the bollard must return -1 (no hit)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Punching a prop the required number of times destroys it via PropBreaker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a PARK_BENCH (3 hits to break) in the world. Simulate 3 punches
     * via PropBreaker and verify the bench is removed and drops WOOD.
     */
    @Test
    void test3_PropDestroyedAfterRequiredHitsViaRaycast() {
        world.addPropPosition(new PropPosition(0f, 0f, 3f, PropType.PARK_BENCH, 0f));

        Vector3 origin    = new Vector3(0f, 0.5f, 0f);
        Vector3 direction = new Vector3(0f, 0f, 1f);

        int hitsRequired = PropType.PARK_BENCH.getHitsToBreak(); // 3
        Material lastDrop = null;

        for (int i = 0; i < hitsRequired; i++) {
            int propIndex = findPropInReach(origin, direction, 10f);
            assertNotEquals(-1, propIndex,
                    "Prop must still be reachable on hit " + (i + 1));
            lastDrop = propBreaker.punchProp(world, propIndex);
        }

        // After all hits, prop must be gone
        assertEquals(0, world.getPropPositions().size(),
                "PARK_BENCH must be removed after " + hitsRequired + " hits");
        assertEquals(Material.WOOD, lastDrop,
                "Breaking a PARK_BENCH must yield WOOD");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Partial hits show increasing break progress
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a POST_BOX (5 hits to break). Punch it 4 times and verify the
     * break progress increases monotonically and the prop is not yet removed.
     */
    @Test
    void test4_PartialHitsShowIncreasingBreakProgress() {
        world.addPropPosition(new PropPosition(0f, 0f, 3f, PropType.POST_BOX, 0f));

        Vector3 origin    = new Vector3(0f, 0.5f, 0f);
        Vector3 direction = new Vector3(0f, 0f, 1f);

        int total = PropType.POST_BOX.getHitsToBreak(); // 5
        float prevProgress = 0f;

        for (int i = 1; i <= total - 1; i++) {
            int propIndex = findPropInReach(origin, direction, 10f);
            propBreaker.punchProp(world, propIndex);
            float progress = propBreaker.getBreakProgress(world, propIndex);
            assertTrue(progress > prevProgress,
                    "Break progress must increase after hit " + i);
            prevProgress = progress;
        }

        // Prop must still exist after (total-1) hits
        assertEquals(1, world.getPropPositions().size(),
                "POST_BOX must still be present after " + (total - 1) + " hits");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: When two props are in line, the nearer one is selected
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a BOLLARD at Z=3 and a PHONE_BOX at Z=7. Fire a ray toward +Z.
     * The BOLLARD (closer) must be selected first; after it is destroyed the
     * PHONE_BOX must become targetable.
     */
    @Test
    void test5_NearerPropSelectedWhenTwoInLine() {
        world.addPropPosition(new PropPosition(0f, 0f, 3f, PropType.BOLLARD,   0f)); // index 0
        world.addPropPosition(new PropPosition(0f, 0f, 7f, PropType.PHONE_BOX, 0f)); // index 1

        Vector3 origin    = new Vector3(0f, 0.5f, 0f);
        Vector3 direction = new Vector3(0f, 0f, 1f);

        // First target must be the bollard (closer)
        int firstHit = findPropInReach(origin, direction, 10f);
        assertEquals(0, firstHit, "Nearer BOLLARD (index 0) must be targeted first");

        // Destroy the bollard
        int bollardHits = PropType.BOLLARD.getHitsToBreak();
        for (int i = 0; i < bollardHits; i++) {
            propBreaker.punchProp(world, 0);
        }
        assertEquals(1, world.getPropPositions().size(), "Bollard should be removed");

        // Now the PHONE_BOX (formerly index 1, now index 0 after shift) must be reachable
        int secondHit = findPropInReach(origin, direction, 10f);
        assertEquals(0, secondHit,
                "After bollard is destroyed, PHONE_BOX (new index 0) must be targeted");
        assertEquals(PropType.PHONE_BOX, world.getPropPositions().get(0).getType(),
                "Remaining prop must be PHONE_BOX");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Ray beyond reach does not hit a prop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Place a BOLLARD at Z=10. With a reach of 5.0 the ray must not reach it.
     */
    @Test
    void test6_PropBeyondReachIsNotTargeted() {
        world.addPropPosition(new PropPosition(0f, 0f, 10f, PropType.BOLLARD, 0f));

        Vector3 origin    = new Vector3(0f, 0.5f, 0f);
        Vector3 direction = new Vector3(0f, 0f, 1f);

        int hit = findPropInReach(origin, direction, 5.0f); // reach is only 5
        assertEquals(-1, hit,
                "Prop at Z=10 must be out of reach when reach=5.0");
    }
}
