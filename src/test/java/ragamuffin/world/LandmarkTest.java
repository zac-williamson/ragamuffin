package ragamuffin.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Landmark.
 */
public class LandmarkTest {

    @Test
    public void testLandmarkCreation() {
        Landmark landmark = new Landmark(LandmarkType.PARK, 10, 5, 15, 20, 10, 25);

        assertEquals(LandmarkType.PARK, landmark.getType());
        assertEquals(10, landmark.getPosition().x);
        assertEquals(5, landmark.getPosition().y);
        assertEquals(15, landmark.getPosition().z);
        assertEquals(20, landmark.getWidth());
        assertEquals(10, landmark.getHeight());
        assertEquals(25, landmark.getDepth());
    }

    @Test
    public void testLandmarkContainsPoint() {
        Landmark landmark = new Landmark(LandmarkType.GREGGS, 0, 0, 0, 10, 5, 10);

        // Inside bounds
        assertTrue(landmark.contains(5, 2, 5));
        assertTrue(landmark.contains(0, 0, 0));
        assertTrue(landmark.contains(9, 4, 9));

        // Outside bounds
        assertFalse(landmark.contains(10, 0, 0)); // At edge (exclusive)
        assertFalse(landmark.contains(-1, 0, 0));
        assertFalse(landmark.contains(5, 5, 5)); // Above
        assertFalse(landmark.contains(5, -1, 5)); // Below
    }

    @Test
    public void testLandmarkWithVectorConstructor() {
        Vector3 pos = new Vector3(15, 3, 20);
        Landmark landmark = new Landmark(LandmarkType.OFFICE_BUILDING, pos, 12, 8, 12);

        assertEquals(15, landmark.getPosition().x);
        assertEquals(3, landmark.getPosition().y);
        assertEquals(20, landmark.getPosition().z);
    }
}
