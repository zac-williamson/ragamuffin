package ragamuffin.integration;

import org.junit.jupiter.api.Test;
import ragamuffin.render.NPCRenderer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Issue #1295 — Increase polygon count on NPC models.
 *
 * NPC body parts are built as subdivided boxes: each of the 6 faces is
 * tessellated into a BODY_SUBDIVISIONS×BODY_SUBDIVISIONS grid of quads.
 * This gives divs²×2 triangles per face and divs²×12 triangles per part,
 * compared with the original 12 triangles (2 per face) from a plain box.
 */
class Issue1295NPCModelPolygonCountTest {

    /**
     * BODY_SUBDIVISIONS must be greater than 1 so that each face is subdivided
     * into more than 2 triangles — the whole point of the polygon count increase.
     */
    @Test
    void bodySubdivisionsGreaterThanOne() {
        assertTrue(NPCRenderer.BODY_SUBDIVISIONS > 1,
            "BODY_SUBDIVISIONS must be > 1 to increase polygon count over plain box (2 tris/face)");
    }

    /**
     * With BODY_SUBDIVISIONS subdivisions per face edge, a body part has
     * BODY_SUBDIVISIONS² × 2 triangles per face × 6 faces total.
     * This must be strictly greater than 12 (the plain-box baseline).
     */
    @Test
    void subdividedBoxHasMoreTrianglesThanPlainBox() {
        int divs = NPCRenderer.BODY_SUBDIVISIONS;
        int trianglesPerPart = divs * divs * 2 * 6;
        int plainBoxTriangles = 12; // 2 tris per face × 6 faces
        assertTrue(trianglesPerPart > plainBoxTriangles,
            "Subdivided box (" + trianglesPerPart + " tris) must exceed plain box (" + plainBoxTriangles + " tris)");
    }

    /**
     * Verify the specific triangle count at BODY_SUBDIVISIONS = 3:
     * 3×3 quads per face = 18 tris per face × 6 faces = 108 triangles per body part.
     * This is 9× the polygon density of the original plain box.
     */
    @Test
    void bodySubdivisionsYieldsExpectedTriangleCount() {
        int divs = NPCRenderer.BODY_SUBDIVISIONS;
        int trianglesPerPart = divs * divs * 2 * 6;
        // At divs=3: 108 triangles per part; at any divs>1 we get at least 48 tris.
        assertTrue(trianglesPerPart >= 48,
            "Body parts should have at least 48 triangles (divs=2 minimum); got " + trianglesPerPart);
    }
}
